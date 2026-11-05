package vn.clinicmanagement.application.auth;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditOutcome;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Finalizes an authenticated login only after one assigned role and one assigned facility/department
 * scope are selected, atomically updating last-login and appending AUTH_LOGIN_SUCCESS in one transaction.
 */
public final class CompleteLoginSessionHandler
        implements CommandHandler<CompleteLoginSessionCommand, CompletedLoginSession> {
    private static final String SELECTION_REQUIRED_MESSAGE =
            "Vui lòng chọn một vai trò và một cơ sở/khoa làm việc.";
    private static final String INVALID_SELECTION_MESSAGE =
            "Vai trò hoặc phạm vi làm việc đã chọn không thuộc danh sách được phân quyền.";
    private static final String UNAVAILABLE_MESSAGE =
            "Không thể hoàn tất đăng nhập lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.";
    private static final Pattern WORKSTATION_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}");

    private final INhanVienRepository employees;
    private final TransactionBoundary transactions;
    private final AuditWritePort auditWritePort;
    private final Clock clock;

    public CompleteLoginSessionHandler(
            INhanVienRepository employees,
            TransactionBoundary transactions,
            AuditWritePort auditWritePort) {
        this(employees, transactions, auditWritePort, Clock.systemUTC());
    }

    public CompleteLoginSessionHandler(
            INhanVienRepository employees,
            TransactionBoundary transactions,
            AuditWritePort auditWritePort,
            Clock clock) {
        this.employees = Objects.requireNonNull(employees, "employees");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Result<CompletedLoginSession> handle(CompleteLoginSessionCommand command) {
        if (command == null
                || command.authenticatedUser() == null
                || command.selectedRole() == null
                || command.selectedWorkScope() == null) {
            return Result.failure("AUTH_SELECTION_REQUIRED", SELECTION_REQUIRED_MESSAGE);
        }

        AuthenticatedUser user = command.authenticatedUser();
        if (user.userId() <= 0 || user.username() == null || user.username().isBlank()
                || user.fullName() == null || user.fullName().isBlank()) {
            return Result.failure("AUTH_INVALID_SELECTION", INVALID_SELECTION_MESSAGE);
        }

        AuthenticatedUser.RoleChoice matchedRole = user.roles().stream()
                .filter(role -> role.equals(command.selectedRole()))
                .findFirst()
                .orElse(null);
        if (matchedRole == null) {
            return Result.failure("AUTH_INVALID_ROLE_SELECTION", INVALID_SELECTION_MESSAGE);
        }

        AuthenticatedUser.WorkScopeChoice matchedScope = user.workScopes().stream()
                .filter(scope -> scope.equals(command.selectedWorkScope()))
                .findFirst()
                .orElse(null);
        if (matchedScope == null) {
            return Result.failure("AUTH_INVALID_SCOPE_SELECTION", INVALID_SELECTION_MESSAGE);
        }

        Instant occurredAtUtc = clock.instant();
        LocalDateTime loginAtUtc = LocalDateTime.ofInstant(occurredAtUtc, ZoneOffset.UTC);
        UUID correlationId = command.correlationId() != null ? command.correlationId() : UUID.randomUUID();
        String workstationId = normalizeWorkstationId(command.workstationId());
        String normalizedRoleCode = matchedRole.code().trim().toUpperCase(Locale.ROOT);

        try {
            Result<CompletedLoginSession> txResult = transactions.inTransaction(() -> {
                try {
                    NhanVien currentEmployee = employees.getById(user.userId()).orElse(null);
                    if (currentEmployee == null || !currentEmployee.isHoatDong() || currentEmployee.isDeleted()) {
                        return Result.failure("AUTH_INVALID_SELECTION", INVALID_SELECTION_MESSAGE);
                    }
                    boolean roleStillAssigned = employees.getAssignedRoles(user.userId()).stream()
                            .anyMatch(role -> role.getId() != null
                                    && role.getId() == matchedRole.id()
                                    && role.getTenVaiTro() != null
                                    && role.getTenVaiTro().trim().equalsIgnoreCase(matchedRole.code()));
                    boolean scopeStillAssigned = employees.getAssignedWorkScopes(user.userId()).stream()
                            .map(CompleteLoginSessionHandler::toWorkScopeChoice)
                            .anyMatch(matchedScope::equals);
                    if (!roleStillAssigned || !scopeStillAssigned) {
                        return Result.failure("AUTH_INVALID_SELECTION", INVALID_SELECTION_MESSAGE);
                    }
                    employees.recordSuccessfulLogin(user.userId(), loginAtUtc);
                    auditWritePort.appendInCurrentTransaction(new SystemAuditEntry(
                            user.userId(),
                            matchedRole.id(),
                            normalizedRoleCode,
                            matchedScope.facilityId(),
                            matchedScope.departmentId(),
                            AuditActionCode.AUTH_LOGIN_SUCCESS,
                            AuditResourceType.EMPLOYEE,
                            user.userId(),
                            AuditOutcome.SUCCESS,
                            null,
                            occurredAtUtc,
                            workstationId,
                            correlationId));
                    return Result.success(new CompletedLoginSession(
                            user.userId(),
                            user.username(),
                            user.fullName(),
                            matchedRole,
                            matchedScope,
                            correlationId,
                            occurredAtUtc));
                } catch (RuntimeException ex) {
                    ApplicationFailureDiagnostics.logUnexpectedFailure(
                            "COMPLETE_LOGIN_SESSION", "AUTH_UNAVAILABLE", correlationId.toString(), ex);
                    return Result.failure("AUTH_UNAVAILABLE", UNAVAILABLE_MESSAGE);
                }
            });
            if (txResult == null) {
                return Result.failure("AUTH_UNAVAILABLE", UNAVAILABLE_MESSAGE);
            }
            return txResult;
        } catch (RuntimeException ex) {
            ApplicationFailureDiagnostics.logUnexpectedFailure(
                    "COMPLETE_LOGIN_SESSION", "AUTH_UNAVAILABLE", correlationId.toString(), ex);
            return Result.failure("AUTH_UNAVAILABLE", UNAVAILABLE_MESSAGE);
        }
    }

    private static AuthenticatedUser.WorkScopeChoice toWorkScopeChoice(NhanVienWorkScope scope) {
        return new AuthenticatedUser.WorkScopeChoice(
                scope.maCoSo(), scope.tenCoSo(), scope.maKhoa(), scope.tenKhoa());
    }

    private static String normalizeWorkstationId(String workstationId) {
        if (workstationId == null || workstationId.isBlank()) return null;
        String trimmed = workstationId.trim();
        return WORKSTATION_ID_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }
}
