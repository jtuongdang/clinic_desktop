package vn.clinicmanagement.desktop.core.session;

import vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.RoleChoice;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.WorkScopeChoice;
import vn.clinicmanagement.application.auth.DaHoanTatDangNhapPhien;
import vn.clinicmanagement.application.session.PhienHetGioChinhSach;

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

/**
 * Chuyển trạng thái phiên desktop theo thời gian nhàn rỗi và thời hạn tuyệt đối do đồng hồ điều khiển.
 */
public final class PhienVongDoiDichVu implements HoatDongPhienCongKetNoi {
    public enum State { ACTIVE, LOCKED_IDLE, LOCKED_ABSOLUTE, LOGGED_OUT }

    @FunctionalInterface
    public interface SessionListener {
        void onSessionChanged();
    }

    private static final Logger LOGGER = Logger.getLogger(PhienVongDoiDichVu.class.getName());
    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{0,49}");
    private static final Pattern WORKSTATION_ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}");
    private static final Executor DEFAULT_AUDIT_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "clinic-session-audit");
        thread.setDaemon(true);
        return thread;
    });

    private final NguoiDungPhien session;
    private final PhienHetGioChinhSach policy;
    private final Clock clock;
    private final KiemToanWriteCongKetNoi auditWritePort;
    private final Executor auditExecutor;
    private final Consumer<String> diagnosticConsumer;
    private final java.util.concurrent.CopyOnWriteArrayList<SessionListener> listeners =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile String lastAuditDiagnostic = "";

    public PhienVongDoiDichVu(
            NguoiDungPhien session,
            PhienHetGioChinhSach policy,
            Clock clock,
            KiemToanWriteCongKetNoi auditWritePort) {
        this(session, policy, clock, auditWritePort, DEFAULT_AUDIT_EXECUTOR, null);
    }

    public PhienVongDoiDichVu(
            NguoiDungPhien session,
            PhienHetGioChinhSach policy,
            Clock clock,
            KiemToanWriteCongKetNoi auditWritePort,
            Executor auditExecutor) {
        this(session, policy, clock, auditWritePort, auditExecutor, null);
    }

    public PhienVongDoiDichVu(
            NguoiDungPhien session,
            PhienHetGioChinhSach policy,
            Clock clock,
            KiemToanWriteCongKetNoi auditWritePort,
            Executor auditExecutor,
            Consumer<String> diagnosticConsumer) {
        this.session = Objects.requireNonNull(session, "session");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
        this.auditExecutor = Objects.requireNonNull(auditExecutor, "auditExecutor");
        this.diagnosticConsumer = diagnosticConsumer;
    }

    public static PhienVongDoiDichVu forCurrentSession(
            PhienHetGioChinhSach policy,
            KiemToanWriteCongKetNoi auditWritePort) {
        return new PhienVongDoiDichVu(
                NguoiDungPhien.getInstance(), policy, Clock.systemUTC(), auditWritePort);
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

    public void startSession(DaHoanTatDangNhapPhien completed) {
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
        HeThongKiemToanNhapLieu auditEntry = null;
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
                                KiemToanHanhDongMa.AUTH_SESSION_EXPIRED_ABSOLUTE,
                                KiemToanKetQua.SUCCESS,
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
                            KiemToanHanhDongMa.AUTH_SESSION_EXPIRED_ABSOLUTE,
                            KiemToanKetQua.SUCCESS,
                            null,
                            now);
                    transitioned = true;
                    result = State.LOCKED_ABSOLUTE;
                } else if (!now.isBefore(session.getLastActivityAt().plus(policy.idleTimeout()))) {
                    session.setLocked(true, false);
                    auditEntry = buildSessionAuditEntry(
                            KiemToanHanhDongMa.AUTH_SESSION_LOCKED_IDLE,
                            KiemToanKetQua.SUCCESS,
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

    /** Cố ý bỏ qua hoạt động khi phiên đang khóa. */
    public State recordActivity() {
        State state = evaluate();
        synchronized (session) {
            if (evaluate() == State.ACTIVE) session.setLastActivityAt(clock.instant());
        }
        return state;
    }

    /**
     * Chỉ mở khóa hoặc khởi động lại khi danh tính đã xác thực và các phân công đang hiệu lực khớp hoàn toàn.
     */
    public boolean reauthenticate(DaXacThucNguoiDung authenticated) {
        if (SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Reauthentication requires a worker.");
        final String originalSessionId;
        final RoleChoice selectedRole;
        final WorkScopeChoice selectedScope;
        final HeThongKiemToanNhapLieu successEntry;
        final Instant now;
        final State originalLockState;
        synchronized (session) {
            State lockState = evaluate();
            if (lockState != State.LOCKED_IDLE && lockState != State.LOCKED_ABSOLUTE) return false;
            originalLockState = lockState;
            now = clock.instant();
            if (authenticated == null) {
                dispatchAudit(buildSessionAuditEntry(KiemToanHanhDongMa.AUTH_REAUTH_FAILED,
                        KiemToanKetQua.FAILURE, KiemToanLyDoMa.AUTH_INVALID, now));
                return false;
            }
            if (authenticated.userId() != session.getUserId()
                    || !authenticated.username().equals(session.getUsername())) {
                dispatchAudit(buildSessionAuditEntry(KiemToanHanhDongMa.AUTH_REAUTH_FAILED,
                        KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN, now));
                return false;
            }
            selectedRole = authenticated.roles().stream()
                    .filter(r -> r.id() == session.getRoleId()
                            && r.code().equals(session.getRoleCode()))
                    .findFirst().orElse(null);
            if (selectedRole == null) {
                dispatchAudit(buildSessionAuditEntry(KiemToanHanhDongMa.AUTH_REAUTH_FAILED,
                        KiemToanKetQua.DENIED, KiemToanLyDoMa.ACCOUNT_REVOKED, now));
                return false;
            }
            selectedScope = authenticated.workScopes().stream()
                    .filter(s -> s.facilityId() == session.getFacilityId()
                            && s.departmentId() == session.getDepartmentId())
                    .findFirst().orElse(null);
            if (selectedScope == null) {
                dispatchAudit(buildSessionAuditEntry(KiemToanHanhDongMa.AUTH_REAUTH_FAILED,
                        KiemToanKetQua.DENIED, KiemToanLyDoMa.SCOPE_MISMATCH, now));
                return false;
            }
            originalSessionId = session.getSessionId();
            successEntry = buildSessionAuditEntry(KiemToanHanhDongMa.AUTH_REAUTH_SUCCESS,
                    KiemToanKetQua.SUCCESS, null, now);
        }

        // Ghi cơ sở dữ liệu bên ngoài monitor của dịch vụ để bộ hẹn giờ Swing không chặn EDT.
        try {
            auditWritePort.appendIndependently(successEntry);
        } catch (RuntimeException ex) {
            reportAuditFailure(KiemToanHanhDongMa.AUTH_REAUTH_SUCCESS);
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
                startSession(new DaHoanTatDangNhapPhien(authenticated.userId(), authenticated.username(),
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
        HeThongKiemToanNhapLieu logoutEntry = null;
        synchronized (session) {
            if (!ownsSession()) return;
            hadSession = hasValidBoundedSession() || session.isLoggedOn();
            if (hasValidBoundedSession()) {
                logoutEntry = buildSessionAuditEntry(
                        KiemToanHanhDongMa.AUTH_LOGOUT,
                        KiemToanKetQua.SUCCESS,
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

    public Optional<PhanQuyenNguCanh> currentAuthorizationContext() {
        synchronized (session) {
            if (evaluate() != State.ACTIVE) return Optional.empty();
            UUID currentSessionId = parseSessionId(session.getSessionId());
            if (currentSessionId == null) return Optional.empty();
            PhanQuyenNguCanh context = new PhanQuyenNguCanh(
                    session.getUserId(), session.getRoleId(), session.getRoleCode(),
                    session.getFacilityId(), session.getDepartmentId(), currentSessionId);
            return context.isValid() ? Optional.of(context) : Optional.empty();
        }
    }

    /**
     * Tạo siêu dữ liệu kiểm toán riêng cho từng thao tác, không dùng lại mã tương quan của phiên.
     */
    public Optional<KiemToanTacNhanNguCanh> currentAuditActorContext() {
        synchronized (session) {
            if (evaluate() != State.ACTIVE) return Optional.empty();
            UUID sessionId = parseSessionId(session.getSessionId());
            if (sessionId == null || session.getUserId() <= 0 || session.getRoleId() <= 0
                    || session.getFacilityId() <= 0 || session.getDepartmentId() <= 0) return Optional.empty();
            String roleCode = normalizeRoleCode(session.getRoleCode());
            if (roleCode == null) return Optional.empty();
            return Optional.of(new KiemToanTacNhanNguCanh(session.getUserId(), session.getRoleId(), roleCode,
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

    private HeThongKiemToanNhapLieu buildSessionAuditEntry(
            KiemToanHanhDongMa actionCode,
            KiemToanKetQua outcome,
            KiemToanLyDoMa reasonCode,
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
        KiemToanTaiNguyenLoai resourceType = actorId != null ? KiemToanTaiNguyenLoai.EMPLOYEE : null;
        Integer resourceId = actorId;
        String workstationId = normalizeWorkstationId(session.getWorkstationId(), session.getStationId());
        UUID correlationId = parseOrGenerateCorrelationId(session.getSessionId());
        return new HeThongKiemToanNhapLieu(
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

    private void dispatchAudit(HeThongKiemToanNhapLieu entry) {
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

    /** Chờ tác vụ kết thúc trong thời hạn giới hạn; quyền truy cập đã bị thu hồi trước khi chờ. */
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
            reportAuditFailure(KiemToanHanhDongMa.AUTH_LOGOUT);
        }
        return false;
    }

    private void reportAuditFailure(KiemToanHanhDongMa actionCode) {
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
