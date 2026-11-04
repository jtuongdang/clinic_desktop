package vn.clinicmanagement.application.auth;

import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.identity.NhanVien;
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

            List<String> roles = employees.getRoleNames(employee.getId());
            String roleCode = roles.stream().filter(role -> role != null && !role.isBlank()).findFirst().orElse("");
            if (roleCode.isBlank()) {
                return Result.failure("AUTH_NO_ROLE", "Tài khoản chưa được gán vai trò hoạt động.");
            }

            String normalizedRole = roleCode.toUpperCase(Locale.ROOT);
            String roleName = switch (normalizedRole) {
                case "ADMIN" -> "Quản trị viên (Admin)";
                case "DOCTOR" -> "Bác sĩ điều trị (Doctor Station)";
                case "RECEPTIONIST" -> "Phòng Tiếp Đón - Lễ tân tiếp nhận";
                case "CASHIER" -> "Phòng Thu Ngân - Viện phí & Hóa đơn";
                case "PHARMACIST" -> "Kho Dược Trung Tâm - Dược sĩ cấp phát";
                case "LAB_TECH" -> "Khoa Chẩn Đoán Hình Ảnh & Thăm Dò";
                default -> roleCode;
            };

            AuthenticatedUser user = new AuthenticatedUser(employee.getId(), employee.getUsername(),
                    employee.getHoTen(), normalizedRole, roleName);
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
}
