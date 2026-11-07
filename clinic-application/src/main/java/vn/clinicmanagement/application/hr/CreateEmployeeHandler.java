package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.util.Locale;

public final class CreateEmployeeHandler
        implements CommandHandler<CreateEmployeeCommand, CreateEmployeeReceipt> {
    private final INhanVienRepository employees;
    private final EmployeePasswordHasher passwordHasher;
    private final HrAuthorization authorization;
    private final HrAuditLog auditLog;
    private final TransactionBoundary transactions;

    public CreateEmployeeHandler(INhanVienRepository employees, EmployeePasswordHasher passwordHasher,
                                 HrAuthorization authorization, HrAuditLog auditLog,
                                 TransactionBoundary transactions) {
        this.employees = employees;
        this.passwordHasher = passwordHasher;
        this.authorization = authorization;
        this.auditLog = auditLog;
        this.transactions = transactions;
    }

    @Override
    public Result<CreateEmployeeReceipt> handle(CreateEmployeeCommand command) {
        if (command == null) return Result.failure("HR_INVALID", "Thông tin nhân viên không hợp lệ.");
        char[] password = command.passwordCopy();
        try {
            if (command.actorId() <= 0 || command.maNhanVien().isBlank() || command.ho().isBlank()
                    || command.ten().isBlank() || command.username().isBlank() || password.length == 0) {
                return Result.failure("HR_INVALID", "Thông tin bắt buộc của nhân viên chưa đầy đủ.");
            }
            return transactions.inTransaction(() -> {
                if (!authorization.canManageEmployees(command.actorId())) {
                    return Result.failure("HR_FORBIDDEN", "Không có quyền quản lý nhân viên.");
                }
                if (employees.getByMaNhanVien(command.maNhanVien()).isPresent()) {
                    return Result.failure("HR_DUPLICATE_EMPLOYEE_CODE", "Mã nhân viên đã tồn tại.");
                }
                String normalizedUsername = command.username().toLowerCase(Locale.ROOT);
                if (employees.usernameExists(normalizedUsername)) {
                    return Result.failure("HR_DUPLICATE_USERNAME", "Tên đăng nhập đã tồn tại.");
                }

                NhanVien employee = new NhanVien();
                employee.setMaNhanVien(command.maNhanVien());
                employee.setHo(command.ho());
                employee.setTen(command.ten());
                employee.setSoDienThoai(command.soDienThoai());
                employee.setEmail(command.email().isBlank() ? null : command.email());
                employee.setMaChucVu(command.maChucVu());
                employee.setUsername(normalizedUsername);
                employee.setPasswordHash(passwordHasher.hash(password));
                employee.setHoatDong(true);
                employee.setCreatedBy(command.actorId());

                employees.add(employee);
                if (employee.getId() == null) {
                    throw new IllegalStateException("Kho lưu trữ không cấp ID cho nhân viên mới.");
                }
                auditLog.append(HrAuditAction.USER_CREATED, command.actorId(), employee.getId());
                return Result.success(new CreateEmployeeReceipt(
                        employee.getId(), employee.getMaNhanVien(), employee.getUsername()));
            });
        } finally {
            java.util.Arrays.fill(password, '\0');
            command.close();
        }
    }
}
