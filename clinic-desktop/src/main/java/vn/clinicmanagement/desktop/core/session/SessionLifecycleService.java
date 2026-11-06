package vn.clinicmanagement.desktop.core.session;

import vn.clinicmanagement.application.authorization.ActiveSessionPort;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
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
    private static final AuditWritePort NO_OP_AUDIT = new AuditWritePort() {
        @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {}
        @Override public void appendIndependently(SystemAuditEntry entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}
        @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
    };
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

    public SessionLifecycleService(UserSession session, SessionTimeoutPolicy policy, Clock clock) {
        this(session, policy, clock, NO_OP_AUDIT, DEFAULT_AUDIT_EXECUTOR, null);
    }

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

    public static SessionLifecycleService forCurrentSession(SessionTimeoutPolicy policy) {
        return new SessionLifecycleService(UserSession.getInstance(), policy, Clock.systemUTC());
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
                LOGGER.log(java.util.logging.Level.WARNING, "Error notifying SessionListener", ex);
            }
        }
    }

    public synchronized void startSession(
            int userId,
            String username,
            String fullName,
            RoleChoice role,
            WorkScopeChoice scope) {
        startSession(userId, username, fullName, role, scope, UUID.randomUUID().toString());
    }

    public void startSession(
            int userId,
            String username,
            String fullName,
            RoleChoice role,
            WorkScopeChoice scope,
            String sessionId) {
        String resolvedSessionId = (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString()
                : sessionId.trim();
        Instant now = clock.instant();
        synchronized (this) {
            session.setSession(userId, username, fullName, role, scope);
            session.setLifecycleMetadata(resolvedSessionId, now, now, now.plus(policy.absoluteTimeout()));
        }
        notifyListeners();
    }

    public void startSession(CompletedLoginSession completed) {
        Objects.requireNonNull(completed, "completed");
        startSession(
                completed.userId(),
                completed.username(),
                completed.fullName(),
                completed.role(),
                completed.workScope(),
                completed.correlationId().toString());
    }

    public State evaluate() {
        boolean transitioned = false;
        State result;
        SystemAuditEntry auditEntry = null;
        synchronized (this) {
            if (!session.isLoggedOn()) return State.LOGGED_OUT;
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
        synchronized (this) {
            if (state == State.ACTIVE) session.setLastActivityAt(clock.instant());
        }
        return state;
    }

    /** Unlocks or restarts only when the authenticated identity and live assignments match exactly. */
    public boolean reauthenticate(AuthenticatedUser authenticated) {
        final String originalSessionId;
        final RoleChoice selectedRole;
        final WorkScopeChoice selectedScope;
        final SystemAuditEntry successEntry;
        final Instant now;
        synchronized (this) {
            State lockState = evaluate();
            if (lockState != State.LOCKED_IDLE && lockState != State.LOCKED_ABSOLUTE) return false;
            now = clock.instant();
            if (authenticated == null) {
                dispatchAudit(buildSessionAuditEntry(AuditActionCode.AUTH_REAUTH_FAILED,
                        AuditOutcome.FAILURE, AuditReasonCode.AUTH_INVALID, now));
                return false;
            }
            if (authenticated.userId() != session.getUserId()) {
                dispatchAudit(buildSessionAuditEntry(AuditActionCode.AUTH_REAUTH_FAILED,
                        AuditOutcome.DENIED, AuditReasonCode.AUTH_FORBIDDEN, now));
                return false;
            }
            selectedRole = authenticated.roles().stream()
                    .filter(r -> r.id() == session.getRoleId()
                            && (session.getRoleCode() == null || session.getRoleCode().isBlank()
                            || r.code().equalsIgnoreCase(session.getRoleCode())))
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
        synchronized (this) {
            if (!Objects.equals(originalSessionId, session.getSessionId())) return false;
            State currentLockState = evaluate();
            if (currentLockState != State.LOCKED_IDLE && currentLockState != State.LOCKED_ABSOLUTE) return false;
            String retainedStationId = session.getStationId();
            String retainedWorkstationId = session.getWorkstationId();
            if (currentLockState == State.LOCKED_ABSOLUTE) {
                startSession(authenticated.userId(), authenticated.username(), authenticated.fullName(),
                        selectedRole, selectedScope);
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
        synchronized (this) {
            hadSession = hasValidBoundedSession() || session.isLoggedOn();
            if (hasValidBoundedSession()) {
                logoutEntry = buildSessionAuditEntry(
                        AuditActionCode.AUTH_LOGOUT,
                        AuditOutcome.SUCCESS,
                        null,
                        clock.instant());
            }
            session.clearSession();
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
        State currentState = evaluate();
        synchronized (this) {
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
        if (evaluate() != State.ACTIVE) return Optional.empty();
        synchronized (this) {
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
        if (evaluate() != State.ACTIVE) return Optional.empty();
        synchronized (this) {
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

    private boolean hasValidBoundedSession() {
        return session.isLoggedOn()
                && session.getSessionId() != null
                && !session.getSessionId().isBlank()
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
        Runnable writeTask = () -> {
            try {
                auditWritePort.appendIndependently(entry);
            } catch (RuntimeException ex) {
                reportAuditFailure(entry.actionCode());
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
        }
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
