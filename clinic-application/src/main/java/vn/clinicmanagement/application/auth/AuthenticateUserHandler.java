package vn.clinicmanagement.application.auth;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class AuthenticateUserHandler implements CommandHandler<AuthenticateUserCommand, AuthenticatedUser> {
    private static final String INVALID_CREDENTIALS = "Tên đăng nhập hoặc mật khẩu không đúng.";
    private static final String UNAVAILABLE_MESSAGE =
            "Không thể xác thực lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.";
    private final INhanVienRepository employees;
    private final PasswordHasher passwordHasher;
    private final AuditWritePort auditWritePort;
    private final Clock clock;

    public AuthenticateUserHandler(INhanVienRepository employees, PasswordHasher passwordHasher,
                                   AuditWritePort auditWritePort) {
        this(employees, passwordHasher, auditWritePort, Clock.systemUTC());
    }

    public AuthenticateUserHandler(INhanVienRepository employees, PasswordHasher passwordHasher,
                                   TransactionBoundary transactions, AuditWritePort auditWritePort) {
        this(employees, passwordHasher, auditWritePort, Clock.systemUTC());
    }

    public AuthenticateUserHandler(INhanVienRepository employees, PasswordHasher passwordHasher,
                                   TransactionBoundary transactions, AuditWritePort auditWritePort, Clock clock) {
        this(employees, passwordHasher, auditWritePort, clock);
    }

    public AuthenticateUserHandler(INhanVienRepository employees, PasswordHasher passwordHasher,
                                   AuditWritePort auditWritePort, Clock clock) {
        this.employees = Objects.requireNonNull(employees, "employees");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Result<AuthenticatedUser> handle(AuthenticateUserCommand command) {
        if (command == null || command.username().isBlank()) {
            if (command != null) command.close();
            try {
                appendIndependentAudit(null, AuditActionCode.AUTH_LOGIN_FAILED,
                        AuditOutcome.FAILURE, AuditReasonCode.AUTH_INVALID);
                return Result.failure("AUTH_INVALID", INVALID_CREDENTIALS);
            } catch (RuntimeException ex) {
                return Result.failure("AUTH_UNAVAILABLE", UNAVAILABLE_MESSAGE);
            }
        }

        char[] password = command.passwordCopy();
        try {
            NhanVien employee = employees.getByUsername(command.username()).orElse(null);
            Integer employeeId = (employee != null && employee.getId() != null && employee.getId() > 0)
                    ? employee.getId()
                    : null;
            if (employee == null || !employee.isHoatDong() || employee.isDeleted()
                    || !passwordHasher.verify(password, employee.getPasswordHash())) {
                appendIndependentAudit(employeeId, AuditActionCode.AUTH_LOGIN_FAILED,
                        AuditOutcome.FAILURE, AuditReasonCode.AUTH_INVALID);
                return Result.failure("AUTH_INVALID", INVALID_CREDENTIALS);
            }

            List<AuthenticatedUser.RoleChoice> roles = employees.getAssignedRoles(employee.getId()).stream()
                    .filter(role -> role != null && role.getId() != null && role.getId() > 0
                            && role.getTenVaiTro() != null && !role.getTenVaiTro().isBlank())
                    .map(AuthenticateUserHandler::toRoleChoice)
                    .toList();
            if (roles.isEmpty()) {
                appendIndependentAudit(employeeId, AuditActionCode.AUTH_LOGIN_DENIED_NO_ROLE,
                        AuditOutcome.DENIED, AuditReasonCode.AUTH_NO_ROLE);
                return Result.failure("AUTH_NO_ROLE", "Tài khoản chưa được gán vai trò hoạt động.");
            }

            List<AuthenticatedUser.WorkScopeChoice> scopes = employees.getAssignedWorkScopes(employee.getId()).stream()
                    .filter(scope -> scope != null && scope.maCoSo() > 0 && scope.maKhoa() > 0
                            && scope.tenCoSo() != null && !scope.tenCoSo().isBlank()
                            && scope.tenKhoa() != null && !scope.tenKhoa().isBlank())
                    .map(AuthenticateUserHandler::toWorkScopeChoice)
                    .toList();
            if (scopes.isEmpty()) {
                appendIndependentAudit(employeeId, AuditActionCode.AUTH_LOGIN_DENIED_NO_SCOPE,
                        AuditOutcome.DENIED, AuditReasonCode.AUTH_NO_SCOPE);
                return Result.failure("AUTH_NO_SCOPE", "Tài khoản chưa được gán cơ sở và khoa làm việc hợp lệ.");
            }

            return Result.success(new AuthenticatedUser(employee.getId(), employee.getUsername(),
                    employee.getHoTen(), roles, scopes));
        } catch (RuntimeException ex) {
            return Result.failure("AUTH_UNAVAILABLE", UNAVAILABLE_MESSAGE);
        } finally {
            java.util.Arrays.fill(password, '\0');
            command.close();
        }
    }

    private void appendIndependentAudit(
            Integer employeeId,
            AuditActionCode actionCode,
            AuditOutcome outcome,
            AuditReasonCode reasonCode) {
        Integer validEmployeeId = (employeeId != null && employeeId > 0) ? employeeId : null;
        AuditResourceType resourceType = validEmployeeId != null ? AuditResourceType.EMPLOYEE : null;
        auditWritePort.appendIndependently(new SystemAuditEntry(
                validEmployeeId,
                null,
                null,
                null,
                null,
                actionCode,
                resourceType,
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
}
