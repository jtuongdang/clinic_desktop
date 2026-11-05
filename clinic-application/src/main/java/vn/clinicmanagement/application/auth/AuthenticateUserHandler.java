package vn.clinicmanagement.application.auth;

import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public final class AuthenticateUserHandler implements CommandHandler<AuthenticateUserCommand, AuthenticatedUser> {
    private static final String INVALID_CREDENTIALS = "Tên đăng nhập hoặc mật khẩu không đúng.";

    private final INhanVienRepository employees;
    private final PasswordHasher passwordHasher;
    private final TransactionBoundary transactions;

    public AuthenticateUserHandler(INhanVienRepository employees, PasswordHasher passwordHasher,
                                   TransactionBoundary transactions) {
        this.employees = employees;
        this.passwordHasher = passwordHasher;
        this.transactions = transactions;
    }

    @Override
    public Result<AuthenticatedUser> handle(AuthenticateUserCommand command) {
        if (command == null || command.username().isBlank()) {
            if (command != null) command.close();
            return Result.failure("AUTH_INVALID", INVALID_CREDENTIALS);
        }

        char[] password = command.passwordCopy();
        try {
            NhanVien employee = employees.getByUsername(command.username()).orElse(null);
            if (employee == null || !employee.isHoatDong() || employee.isDeleted()
                    || !passwordHasher.verify(password, employee.getPasswordHash())) {
                return Result.failure("AUTH_INVALID", INVALID_CREDENTIALS);
            }

            List<AuthenticatedUser.RoleChoice> roles = employees.getAssignedRoles(employee.getId()).stream()
                    .filter(role -> role != null && role.getId() != null && role.getId() > 0
                            && role.getTenVaiTro() != null && !role.getTenVaiTro().isBlank())
                    .map(AuthenticateUserHandler::toRoleChoice)
                    .toList();
            if (roles.isEmpty()) {
                return Result.failure("AUTH_NO_ROLE", "Tài khoản chưa được gán vai trò hoạt động.");
            }

            List<AuthenticatedUser.WorkScopeChoice> scopes = employees.getAssignedWorkScopes(employee.getId()).stream()
                    .filter(scope -> scope != null && scope.maCoSo() > 0 && scope.maKhoa() > 0
                            && scope.tenCoSo() != null && !scope.tenCoSo().isBlank()
                            && scope.tenKhoa() != null && !scope.tenKhoa().isBlank())
                    .map(AuthenticateUserHandler::toWorkScopeChoice)
                    .toList();
            if (scopes.isEmpty()) {
                return Result.failure("AUTH_NO_SCOPE", "Tài khoản chưa được gán cơ sở và khoa làm việc hợp lệ.");
            }

            AuthenticatedUser user = new AuthenticatedUser(employee.getId(), employee.getUsername(),
                    employee.getHoTen(), roles, scopes);
            return transactions.inTransaction(() -> {
                employees.recordSuccessfulLogin(employee.getId(), LocalDateTime.now());
                return Result.success(user);
            });
        } catch (RuntimeException ex) {
            return Result.failure("AUTH_UNAVAILABLE", "Không thể xác thực lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.");
        } finally {
            java.util.Arrays.fill(password, '\0');
            command.close();
        }
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
