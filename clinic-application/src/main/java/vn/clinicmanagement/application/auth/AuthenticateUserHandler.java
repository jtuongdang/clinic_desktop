package vn.clinicmanagement.application.auth;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.common.ApplicationFailureDiagnostics;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AuthenticateUserHandler implements CommandHandler<AuthenticateUserCommand, AuthenticatedUser> {
    private static final String INVALID_CREDENTIALS = "Tên đăng nhập hoặc mật khẩu không đúng.";
    private static final String UNAVAILABLE_MESSAGE =
            "Không thể xác thực lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.";
    private static final String NO_ROLE_MESSAGE = "Tài khoản chưa được gán vai trò hoạt động.";
    private static final String NO_SCOPE_MESSAGE = "Tài khoản chưa được gán cơ sở và khoa làm việc hợp lệ.";
    private static final int FAILURE_THRESHOLD = 5;
    private static final int WINDOW_SECONDS = 15 * 60;
    private static final int INITIAL_DELAY_SECONDS = 30;
    private static final int MAX_DELAY_SECONDS = 15 * 60;

    private final INhanVienRepository employees;
    private final AccountLoginThrottleStore throttleStore;
    private final PasswordHasher passwordHasher;
    private final TransactionBoundary transactions;
    private final AuditWritePort auditWritePort;
    private final Clock clock;

    public AuthenticateUserHandler(
            INhanVienRepository employees,
            AccountLoginThrottleStore throttleStore,
            PasswordHasher passwordHasher,
            TransactionBoundary transactions,
            AuditWritePort auditWritePort,
            Clock clock) {
        this.employees = Objects.requireNonNull(employees, "employees");
        this.throttleStore = Objects.requireNonNull(throttleStore, "throttleStore");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Result<AuthenticatedUser> handle(AuthenticateUserCommand command) {
        if (command == null || command.username().isBlank()) {
            if (command != null) command.close();
            return auditAnonymousFailure();
        }

        char[] password = command.passwordCopy();
        LoginAttempt loginAttempt;
        try {
            Result<LoginAttempt> transactionResult = transactions.inTransaction(
                    () -> Result.success(attempt(command.username(), password)));
            if (transactionResult == null || transactionResult.isFailure()) {
                return Result.failure("AUTH_UNAVAILABLE", UNAVAILABLE_MESSAGE);
            }
            loginAttempt = transactionResult.valueOrThrow();
        } catch (RuntimeException ex) {
            ApplicationFailureDiagnostics.logUnexpectedFailure(
                    "AUTHENTICATE_USER", "AUTH_UNAVAILABLE", UUID.randomUUID().toString(), ex);
            return Result.failure("AUTH_UNAVAILABLE", UNAVAILABLE_MESSAGE);
        } finally {
            Arrays.fill(password, '\0');
            command.close();
        }
        if (loginAttempt.failureCode() != null) {
            return Result.failure(loginAttempt.failureCode(), loginAttempt.failureMessage());
        }
        try {
            return authenticateAssignments(loginAttempt.employee());
        } catch (RuntimeException ex) {
            ApplicationFailureDiagnostics.logUnexpectedFailure(
                    "AUTHENTICATE_USER", "AUTH_UNAVAILABLE", UUID.randomUUID().toString(), ex);
            return Result.failure("AUTH_UNAVAILABLE", UNAVAILABLE_MESSAGE);
        }
    }

    private LoginAttempt attempt(String username, char[] password) {
        NhanVien candidate = employees.getByUsername(username).orElse(null);
        if (candidate == null || candidate.getId() == null || candidate.getId() <= 0) {
            appendTransactionalAudit(null, AuditActionCode.AUTH_LOGIN_FAILED,
                    AuditOutcome.FAILURE, AuditReasonCode.AUTH_INVALID);
            return LoginAttempt.failure("AUTH_INVALID", INVALID_CREDENTIALS);
        }

        int employeeId = candidate.getId();
        Optional<AccountLoginThrottleStore.Snapshot> snapshotOption = throttleStore.lockForUpdate(employeeId);
        NhanVien employee = employees.getById(employeeId).orElse(null);
        if (employee == null || !employee.isHoatDong() || employee.isDeleted() || snapshotOption.isEmpty()) {
            appendTransactionalAudit(employeeId, AuditActionCode.AUTH_LOGIN_FAILED,
                    AuditOutcome.FAILURE, AuditReasonCode.AUTH_INVALID);
            return LoginAttempt.failure("AUTH_INVALID", INVALID_CREDENTIALS);
        }

        AccountLoginThrottleStore.Snapshot snapshot = snapshotOption.orElseThrow();
        LocalDateTime databaseNow = snapshot.databaseNow();
        if (databaseNow == null) throw new IllegalStateException("Throttle store returned no database time.");

        boolean sequenceExpired = snapshot.lastFailureAt() == null
                || !snapshot.lastFailureAt().isAfter(databaseNow.minusHours(24));
        if (!sequenceExpired
                && snapshot.delaySeconds() > 0
                && (snapshot.nextAllowedAt() == null
                || databaseNow.isBefore(snapshot.nextAllowedAt()))) {
            appendTransactionalAudit(employeeId, AuditActionCode.AUTH_LOGIN_FAILED,
                    AuditOutcome.FAILURE, AuditReasonCode.AUTH_THROTTLED);
            return LoginAttempt.failure("AUTH_INVALID", INVALID_CREDENTIALS);
        }

        if (!passwordHasher.verify(password, employee.getPasswordHash())) {
            recordWrongPassword(employeeId, snapshot, databaseNow, sequenceExpired);
            appendTransactionalAudit(employeeId, AuditActionCode.AUTH_LOGIN_FAILED,
                    AuditOutcome.FAILURE, AuditReasonCode.AUTH_INVALID);
            return LoginAttempt.failure("AUTH_INVALID", INVALID_CREDENTIALS);
        }

        throttleStore.reset(employeeId);
        return LoginAttempt.verified(employee);
    }

    private Result<AuthenticatedUser> authenticateAssignments(NhanVien employee) {
        int employeeId = employee.getId();
        List<AuthenticatedUser.RoleChoice> roles = employees.getAssignedRoles(employeeId).stream()
                .filter(role -> role != null && role.getId() != null && role.getId() > 0
                        && role.getTenVaiTro() != null && !role.getTenVaiTro().isBlank())
                .map(AuthenticateUserHandler::toRoleChoice)
                .toList();
        if (roles.isEmpty()) {
            appendIndependentAudit(employeeId, AuditActionCode.AUTH_LOGIN_DENIED_NO_ROLE,
                    AuditOutcome.DENIED, AuditReasonCode.AUTH_NO_ROLE);
            return Result.failure("AUTH_NO_ROLE", NO_ROLE_MESSAGE);
        }

        List<AuthenticatedUser.WorkScopeChoice> scopes = employees.getAssignedWorkScopes(employeeId).stream()
                .filter(scope -> scope != null && scope.maCoSo() > 0 && scope.maKhoa() > 0
                        && scope.tenCoSo() != null && !scope.tenCoSo().isBlank()
                        && scope.tenKhoa() != null && !scope.tenKhoa().isBlank())
                .map(AuthenticateUserHandler::toWorkScopeChoice)
                .toList();
        if (scopes.isEmpty()) {
            appendIndependentAudit(employeeId, AuditActionCode.AUTH_LOGIN_DENIED_NO_SCOPE,
                    AuditOutcome.DENIED, AuditReasonCode.AUTH_NO_SCOPE);
            return Result.failure("AUTH_NO_SCOPE", NO_SCOPE_MESSAGE);
        }

        return Result.success(new AuthenticatedUser(employee.getId(), employee.getUsername(),
                employee.getHoTen(), roles, scopes));
    }

    private void recordWrongPassword(
            int employeeId,
            AccountLoginThrottleStore.Snapshot snapshot,
            LocalDateTime databaseNow,
            boolean sequenceExpired) {
        boolean escalating = !sequenceExpired && snapshot.delaySeconds() > 0;
        boolean windowExpired = snapshot.windowStartedAt() == null
                || !databaseNow.isBefore(snapshot.windowStartedAt().plusSeconds(WINDOW_SECONDS));
        LocalDateTime windowStart;
        int failureCount;
        int delaySeconds;

        if (sequenceExpired || (!escalating && windowExpired)) {
            windowStart = databaseNow;
            failureCount = 1;
            delaySeconds = 0;
        } else {
            windowStart = snapshot.windowStartedAt();
            failureCount = snapshot.failureCount() + 1;
            if (escalating) {
                delaySeconds = Math.min(snapshot.delaySeconds() * 2, MAX_DELAY_SECONDS);
            } else {
                delaySeconds = failureCount >= FAILURE_THRESHOLD ? INITIAL_DELAY_SECONDS : 0;
            }
        }
        throttleStore.recordFailure(employeeId, windowStart, failureCount, delaySeconds);
    }

    private Result<AuthenticatedUser> auditAnonymousFailure() {
        try {
            appendIndependentAudit(null, AuditActionCode.AUTH_LOGIN_FAILED,
                    AuditOutcome.FAILURE, AuditReasonCode.AUTH_INVALID);
            return Result.failure("AUTH_INVALID", INVALID_CREDENTIALS);
        } catch (RuntimeException ex) {
            ApplicationFailureDiagnostics.logUnexpectedFailure(
                    "AUTHENTICATE_USER", "AUTH_UNAVAILABLE", UUID.randomUUID().toString(), ex);
            return Result.failure("AUTH_UNAVAILABLE", UNAVAILABLE_MESSAGE);
        }
    }

    private void appendTransactionalAudit(
            Integer employeeId,
            AuditActionCode actionCode,
            AuditOutcome outcome,
            AuditReasonCode reasonCode) {
        Integer validEmployeeId = employeeId != null && employeeId > 0 ? employeeId : null;
        auditWritePort.appendInCurrentTransaction(new SystemAuditEntry(
                validEmployeeId,
                null,
                null,
                null,
                null,
                actionCode,
                validEmployeeId == null ? null : AuditResourceType.EMPLOYEE,
                validEmployeeId,
                outcome,
                reasonCode,
                clock.instant(),
                null,
                UUID.randomUUID()));
    }

    private void appendIndependentAudit(
            Integer employeeId,
            AuditActionCode actionCode,
            AuditOutcome outcome,
            AuditReasonCode reasonCode) {
        Integer validEmployeeId = employeeId != null && employeeId > 0 ? employeeId : null;
        auditWritePort.appendIndependently(new SystemAuditEntry(
                validEmployeeId,
                null,
                null,
                null,
                null,
                actionCode,
                validEmployeeId == null ? null : AuditResourceType.EMPLOYEE,
                validEmployeeId,
                outcome,
                reasonCode,
                clock.instant(),
                null,
                UUID.randomUUID()));
    }

    private static AuthenticatedUser.RoleChoice toRoleChoice(VaiTro role) {
        String code = role.getTenVaiTro().trim().toUpperCase(Locale.ROOT);
        String name = switch (code) {
            case "ADMIN" -> "Quản trị viên (Admin)";
            case "DOCTOR" -> "Bác sĩ điều trị (Doctor Station)";
            case "RECEPTIONIST" -> "Phòng Tiếp Đón - Lễ tân tiếp nhận";
            case "CASHIER" -> "Phòng Thu Ngân - Viện phí & Hóa đơn";
            case "PHARMACIST" -> "Kho Dược Trung Tâm - Dược sĩ cấp phát";
            case "LAB_TECH" -> "Khoa Chẩn Đoán Hình Ảnh & Thăm Dò";
            default -> role.getTenVaiTro().trim();
        };
        return new AuthenticatedUser.RoleChoice(role.getId(), code, name);
    }

    private static AuthenticatedUser.WorkScopeChoice toWorkScopeChoice(NhanVienWorkScope scope) {
        return new AuthenticatedUser.WorkScopeChoice(scope.maCoSo(), scope.tenCoSo(), scope.maKhoa(), scope.tenKhoa());
    }

    private record LoginAttempt(NhanVien employee, String failureCode, String failureMessage) {
        private static LoginAttempt verified(NhanVien employee) {
            return new LoginAttempt(employee, null, null);
        }

        private static LoginAttempt failure(String code, String message) {
            return new LoginAttempt(null, code, message);
        }
    }
}
