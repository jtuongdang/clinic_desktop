package vn.clinicmanagement.desktop.core.session;

import vn.clinicmanagement.application.authorization.ActiveSessionPort;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.auth.AuthenticatedUser;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.auth.CompletedLoginSession;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;

import javax.swing.SwingUtilities;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/** Clock-driven idle and absolute timeout state transitions for the desktop session. */
public final class SessionLifecycleService implements ActiveSessionPort {
    public enum State { ACTIVE, LOCKED_IDLE, LOCKED_ABSOLUTE, LOGGED_OUT }

    @FunctionalInterface
    public interface SessionListener {
        void onSessionChanged();
    }

    private static final Logger LOGGER = Logger.getLogger(SessionLifecycleService.class.getName());
    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{0,49}");
    private static final Pattern WORKSTATION_ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}");
    private static final Executor DEFAULT_AUDIT_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "clinic-session-audit");
        thread.setDaemon(true);
        return thread;
    });

    private final UserSession session;
    private final SessionTimeoutPolicy policy;
    private final Clock clock;
    private final AuditWritePort auditWritePort;
    private final Executor auditExecutor;
    private final Consumer<String> diagnosticConsumer;
    private final java.util.concurrent.CopyOnWriteArrayList<SessionListener> listeners =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile String lastAuditDiagnostic = "";

    public SessionLifecycleService(
            UserSession session,
            SessionTimeoutPolicy policy,
            Clock clock,
            AuditWritePort auditWritePort) {
        this(session, policy, clock, auditWritePort, DEFAULT_AUDIT_EXECUTOR, null);
    }

    public SessionLifecycleService(
            UserSession session,
            SessionTimeoutPolicy policy,
            Clock clock,
            AuditWritePort auditWritePort,
            Executor auditExecutor) {
        this(session, policy, clock, auditWritePort, auditExecutor, null);
    }

    public SessionLifecycleService(
            UserSession session,
            SessionTimeoutPolicy policy,
            Clock clock,
            AuditWritePort auditWritePort,
            Executor auditExecutor,
            Consumer<String> diagnosticConsumer) {
        this.session = Objects.requireNonNull(session, "session");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
        this.auditExecutor = Objects.requireNonNull(auditExecutor, "auditExecutor");
        this.diagnosticConsumer = diagnosticConsumer;
    }

    public static SessionLifecycleService forCurrentSession(
            SessionTimeoutPolicy policy,
            AuditWritePort auditWritePort) {
        return new SessionLifecycleService(
                UserSession.getInstance(), policy, Clock.systemUTC(), auditWritePort);
    }

    public void addSessionListener(SessionListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.addIfAbsent(listener);
    }

    public void removeSessionListener(SessionListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        for (SessionListener listener : listeners) {
            try {
                listener.onSessionChanged();
            } catch (RuntimeException ex) {
                LOGGER.warning("Session listener failed; session authority remains unchanged.");
            }
        }
    }

    private String ownedSessionId;
    private final java.util.Set<java.util.concurrent.CompletableFuture<Void>> pendingAudits =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public void startSession(CompletedLoginSession completed) {
        Objects.requireNonNull(completed, "completed");
        synchronized (session) {
            Instant issued = completed.loginAtUtc();
            Instant now = clock.instant();
            if (issued.isAfter(now) || !now.isBefore(issued.plus(policy.absoluteTimeout()))) {
                throw new IllegalArgumentException("Completed session is outside its bounded lifetime.");
            }
            session.setSession(completed.userId(), completed.username(), completed.fullName(),
                    completed.role(), completed.workScope());
            ownedSessionId = completed.correlationId().toString();
            session.setLifecycleMetadata(ownedSessionId, issued, issued, issued.plus(policy.absoluteTimeout()));
        }
        notifyListeners();
        evaluate();
    }

    public State evaluate() {
        boolean transitioned = false;
        State result;
        SystemAuditEntry auditEntry = null;
        synchronized (session) {
            if (!ownsSession() || !session.isLoggedOn()) return State.LOGGED_OUT;
            if (!hasValidBoundedSession()) {
                session.clearSession();
                transitioned = true;
                result = State.LOGGED_OUT;
            } else {
                Instant now = clock.instant();
                if (session.isLocked()) {
                    if (!session.isAbsoluteExpiryPending() && !now.isBefore(session.getAbsoluteExpiresAt())) {
                        session.setLocked(true, true);
                        auditEntry = buildSessionAuditEntry(
                                AuditActionCode.AUTH_SESSION_EXPIRED_ABSOLUTE,
                                AuditOutcome.SUCCESS,
                                null,
                                now);
                        transitioned = true;
                        result = State.LOCKED_ABSOLUTE;
                    } else {
                        result = session.isAbsoluteExpiryPending() ? State.LOCKED_ABSOLUTE : State.LOCKED_IDLE;
                    }
                } else if (!now.isBefore(session.getAbsoluteExpiresAt())) {
                    session.setLocked(true, true);
                    auditEntry = buildSessionAuditEntry(
                            AuditActionCode.AUTH_SESSION_EXPIRED_ABSOLUTE,
                            AuditOutcome.SUCCESS,
                            null,
                            now);
                    transitioned = true;
                    result = State.LOCKED_ABSOLUTE;
                } else if (!now.isBefore(session.getLastActivityAt().plus(policy.idleTimeout()))) {
                    session.setLocked(true, false);
                    auditEntry = buildSessionAuditEntry(
                            AuditActionCode.AUTH_SESSION_LOCKED_IDLE,
                            AuditOutcome.SUCCESS,
                            null,
                            now);
                    transitioned = true;
                    result = State.LOCKED_IDLE;
                } else {
                    result = State.ACTIVE;
                }
            }
        }
        if (auditEntry != null) {
            dispatchAudit(auditEntry);
        }
        if (transitioned) {
            notifyListeners();
        }
        return result;
    }

    /** Activity while locked is intentionally ignored. */
    public State recordActivity() {
        State state = evaluate();
        synchronized (session) {
            if (evaluate() == State.ACTIVE) session.setLastActivityAt(clock.instant());
        }
        return state;
    }

    /** Unlocks or restarts only when the authenticated identity and live assignments match exactly. */
    public boolean reauthenticate(AuthenticatedUser authenticated) {
        if (SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Reauthentication requires a worker.");
        final String originalSessionId;
        final RoleChoice selectedRole;
        final WorkScopeChoice selectedScope;
        final SystemAuditEntry successEntry;
        final Instant now;
        final State originalLockState;
        synchronized (session) {
            State lockState = evaluate();
            if (lockState != State.LOCKED_IDLE && lockState != State.LOCKED_ABSOLUTE) return false;
            originalLockState = lockState;
            now = clock.instant();
            if (authenticated == null) {
                dispatchAudit(buildSessionAuditEntry(AuditActionCode.AUTH_REAUTH_FAILED,
                        AuditOutcome.FAILURE, AuditReasonCode.AUTH_INVALID, now));
                return false;
            }
            if (authenticated.userId() != session.getUserId()
                    || !authenticated.username().equals(session.getUsername())) {
                dispatchAudit(buildSessionAuditEntry(AuditActionCode.AUTH_REAUTH_FAILED,
                        AuditOutcome.DENIED, AuditReasonCode.AUTH_FORBIDDEN, now));
                return false;
            }
            selectedRole = authenticated.roles().stream()
                    .filter(r -> r.id() == session.getRoleId()
                            && r.code().equals(session.getRoleCode()))
                    .findFirst().orElse(null);
            if (selectedRole == null) {
                dispatchAudit(buildSessionAuditEntry(AuditActionCode.AUTH_REAUTH_FAILED,
                        AuditOutcome.DENIED, AuditReasonCode.ACCOUNT_REVOKED, now));
                return false;
            }
            selectedScope = authenticated.workScopes().stream()
                    .filter(s -> s.facilityId() == session.getFacilityId()
                            && s.departmentId() == session.getDepartmentId())
                    .findFirst().orElse(null);
            if (selectedScope == null) {
                dispatchAudit(buildSessionAuditEntry(AuditActionCode.AUTH_REAUTH_FAILED,
                        AuditOutcome.DENIED, AuditReasonCode.SCOPE_MISMATCH, now));
                return false;
            }
            originalSessionId = session.getSessionId();
            successEntry = buildSessionAuditEntry(AuditActionCode.AUTH_REAUTH_SUCCESS,
                    AuditOutcome.SUCCESS, null, now);
        }

        // The database write is outside the service monitor so a Swing timer cannot block the EDT.
        try {
            auditWritePort.appendIndependently(successEntry);
        } catch (RuntimeException ex) {
            reportAuditFailure(AuditActionCode.AUTH_REAUTH_SUCCESS);
            return false;
        }

        boolean notify = false;
        synchronized (session) {
            if (!Objects.equals(originalSessionId, session.getSessionId())) return false;
            State currentLockState = evaluate();
            if (currentLockState != originalLockState) return false;
            if (!clock.instant().isBefore(now.plus(policy.idleTimeout()))
                    || !clock.instant().isBefore(now.plus(policy.absoluteTimeout()))) return false;
            String retainedStationId = session.getStationId();
            String retainedWorkstationId = session.getWorkstationId();
            if (currentLockState == State.LOCKED_ABSOLUTE) {
                startSession(new CompletedLoginSession(authenticated.userId(), authenticated.username(),
                        authenticated.fullName(), selectedRole, selectedScope, UUID.randomUUID(), now));
                if (retainedStationId != null && !retainedStationId.isEmpty()) session.setStationId(retainedStationId);
                if (retainedWorkstationId != null && !retainedWorkstationId.isEmpty()) {
                    session.setWorkstationId(retainedWorkstationId);
                }
            } else {
                session.setLocked(false, false);
                session.setLastActivityAt(now);
                notify = true;
            }
        }
        if (notify) {
            notifyListeners();
        }
        return true;
    }

    public void logout() {
        clear();
    }

    public void clear() {
        boolean hadSession;
        SystemAuditEntry logoutEntry = null;
        synchronized (session) {
            if (!ownsSession()) return;
            hadSession = hasValidBoundedSession() || session.isLoggedOn();
            if (hasValidBoundedSession()) {
                logoutEntry = buildSessionAuditEntry(
                        AuditActionCode.AUTH_LOGOUT,
                        AuditOutcome.SUCCESS,
                        null,
                        clock.instant());
            }
            session.clearSession();
            ownedSessionId = null;
        }
        if (logoutEntry != null) {
            dispatchAudit(logoutEntry);
        }
        if (hadSession) {
            notifyListeners();
        }
    }

    public State state() {
        return evaluate();
    }

    @Override
    public Optional<ActiveSession> findActiveSession(UUID requestedSessionId) {
        synchronized (session) {
            State currentState = evaluate();
            if (!ownsSession()) return Optional.empty();
            UUID currentSessionId = parseSessionId(session.getSessionId());
            if (currentSessionId == null || !currentSessionId.equals(requestedSessionId)) {
                return Optional.empty();
            }
            Instant now = clock.instant();
            boolean notExpired = session.getAbsoluteExpiresAt() != null
                    && now.isBefore(session.getAbsoluteExpiresAt());
            return Optional.of(new ActiveSession(
                    currentSessionId,
                    session.getUserId(),
                    session.getRoleId(),
                    session.getRoleCode(),
                    session.getFacilityId(),
                    session.getDepartmentId(),
                    currentState == State.ACTIVE,
                    !session.isLocked(),
                    notExpired));
        }
    }

    public Optional<AuthorizationContext> currentAuthorizationContext() {
        synchronized (session) {
            if (evaluate() != State.ACTIVE) return Optional.empty();
            UUID currentSessionId = parseSessionId(session.getSessionId());
            if (currentSessionId == null) return Optional.empty();
            AuthorizationContext context = new AuthorizationContext(
                    session.getUserId(), session.getRoleId(), session.getRoleCode(),
                    session.getFacilityId(), session.getDepartmentId(), currentSessionId);
            return context.isValid() ? Optional.of(context) : Optional.empty();
        }
    }

    /** Creates per-operation audit metadata without reusing the session correlation ID. */
    public Optional<AuditActorContext> currentAuditActorContext() {
        synchronized (session) {
            if (evaluate() != State.ACTIVE) return Optional.empty();
            UUID sessionId = parseSessionId(session.getSessionId());
            if (sessionId == null || session.getUserId() <= 0 || session.getRoleId() <= 0
                    || session.getFacilityId() <= 0 || session.getDepartmentId() <= 0) return Optional.empty();
            String roleCode = normalizeRoleCode(session.getRoleCode());
            if (roleCode == null) return Optional.empty();
            return Optional.of(new AuditActorContext(session.getUserId(), session.getRoleId(), roleCode,
                    session.getFacilityId(), session.getDepartmentId(),
                    normalizeWorkstationId(session.getWorkstationId(), session.getStationId()), UUID.randomUUID()));
        }
    }

    public String lastAuditDiagnostic() {
        return lastAuditDiagnostic;
    }

    private boolean ownsSession() {
        return ownedSessionId != null && ownedSessionId.equals(session.getSessionId());
    }

    private boolean hasValidBoundedSession() {
        return ownsSession() && session.isLoggedOn()
                && session.getSessionId() != null
                && parseSessionId(session.getSessionId()) != null
                && session.getUserId() > 0 && session.getRoleId() > 0
                && normalizeRoleCode(session.getRoleCode()) != null
                && session.getFacilityId() > 0 && session.getDepartmentId() > 0
                && session.getIssuedAt() != null
                && session.getLastActivityAt() != null
                && session.getAbsoluteExpiresAt() != null;
    }

    private SystemAuditEntry buildSessionAuditEntry(
            AuditActionCode actionCode,
            AuditOutcome outcome,
            AuditReasonCode reasonCode,
            Instant occurredAtUtc) {
        Integer actorId = session.getUserId() > 0 ? session.getUserId() : null;
        Integer roleId = session.getRoleId() > 0 ? session.getRoleId() : null;
        String roleCode = normalizeRoleCode(session.getRoleCode());
        if (roleId == null || roleCode == null) {
            roleId = null;
            roleCode = null;
        }
        Integer facilityId = session.getFacilityId() > 0 ? session.getFacilityId() : null;
        Integer departmentId = session.getDepartmentId() > 0 ? session.getDepartmentId() : null;
        AuditResourceType resourceType = actorId != null ? AuditResourceType.EMPLOYEE : null;
        Integer resourceId = actorId;
        String workstationId = normalizeWorkstationId(session.getWorkstationId(), session.getStationId());
        UUID correlationId = parseOrGenerateCorrelationId(session.getSessionId());
        return new SystemAuditEntry(
                actorId,
                roleId,
                roleCode,
                facilityId,
                departmentId,
                actionCode,
                resourceType,
                resourceId,
                outcome,
                reasonCode,
                occurredAtUtc != null ? occurredAtUtc : clock.instant(),
                workstationId,
                correlationId);
    }

    private void dispatchAudit(SystemAuditEntry entry) {
        if (entry == null) return;
        java.util.concurrent.CompletableFuture<Void> pending = new java.util.concurrent.CompletableFuture<>();
        pendingAudits.add(pending);
        Runnable writeTask = () -> {
            try {
                auditWritePort.appendIndependently(entry);
            } catch (RuntimeException ex) {
                reportAuditFailure(entry.actionCode());
            } finally {
                pending.complete(null);
                pendingAudits.remove(pending);
            }
        };
        try {
            auditExecutor.execute(() -> {
                if (SwingUtilities.isEventDispatchThread()) {
                    DEFAULT_AUDIT_EXECUTOR.execute(writeTask);
                } else {
                    writeTask.run();
                }
            });
        } catch (RuntimeException ex) {
            reportAuditFailure(entry.actionCode());
            pending.complete(null);
            pendingAudits.remove(pending);
        }
    }

    /** Bounded shutdown drain; authority has already been cleared before waiting. */
    public boolean awaitPendingAudits(java.time.Duration timeout) {
        if (SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Audit drain requires a worker.");
        try {
            java.util.concurrent.CompletableFuture.allOf(
                    pendingAudits.toArray(java.util.concurrent.CompletableFuture[]::new))
                    .get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException ex) {
            reportAuditFailure(AuditActionCode.AUTH_LOGOUT);
        }
        return false;
    }

    private void reportAuditFailure(AuditActionCode actionCode) {
        String safeDiagnostic = "Không thể ghi nhật ký phiên làm việc cho sự kiện " + actionCode.name() + ".";
        this.lastAuditDiagnostic = safeDiagnostic;
        if (diagnosticConsumer != null) {
            try {
                diagnosticConsumer.accept(safeDiagnostic);
            } catch (RuntimeException ignored) {
                // Keep session transition unaffected even if diagnostic listener fails.
            }
        }
        LOGGER.warning(safeDiagnostic);
    }

    private static String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) return null;
        String candidate = roleCode.trim().toUpperCase(Locale.ROOT);
        return ROLE_CODE_PATTERN.matcher(candidate).matches() ? candidate : null;
    }

    private static String normalizeWorkstationId(String workstationId, String stationId) {
        String raw = (workstationId != null && !workstationId.isBlank()) ? workstationId : stationId;
        if (raw == null || raw.isBlank()) return null;
        String candidate = raw.trim();
        return WORKSTATION_ID_PATTERN.matcher(candidate).matches() ? candidate : null;
    }

    private static UUID parseOrGenerateCorrelationId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return UUID.randomUUID();
        try {
            return UUID.fromString(sessionId.trim());
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID();
        }
    }

    private static UUID parseSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return null;
        try {
            return UUID.fromString(sessionId.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
