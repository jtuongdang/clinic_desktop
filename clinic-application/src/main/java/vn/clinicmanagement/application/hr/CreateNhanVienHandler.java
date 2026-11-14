package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.util.Locale;

public final class CreateNhanVienHandler
        implements CommandHandler<CreateNhanVienCommand, CreateNhanVienBienNhan> {
    /** Trường `employees`: nhân viên. */
    private final INhanVienRepository employees;
    /** Trường `passwordHasher`: matkhau bobam. */
    private final NhanVienMatKhauBoBam passwordHasher;
    /** Trường `authorization`: phanquyen. */
    private final NhanSuPhanQuyen authorization;
    /** Trường `auditLog`: kiểm toán nhatky. */
    private final NhanSuKiemToanNhatKy auditLog;
    private final GiaoDichRanhGioi transactions;

    public CreateNhanVienHandler(INhanVienRepository employees, NhanVienMatKhauBoBam passwordHasher,
                                 NhanSuPhanQuyen authorization, NhanSuKiemToanNhatKy auditLog,
                                 GiaoDichRanhGioi transactions) {
        this.employees = employees;
        this.passwordHasher = passwordHasher;
        this.authorization = authorization;
        this.auditLog = auditLog;
        this.transactions = transactions;
    }

    @Override
    public KetQua<CreateNhanVienBienNhan> handle(CreateNhanVienCommand command) {
        if (command == null) return KetQua.failure("HR_INVALID", "Thông tin nhân viên không hợp lệ.");
        char[] password = command.passwordCopy();
        try {
            if (command.actorId() <= 0 || command.maNhanVien().isBlank() || command.ho().isBlank()
                    || command.ten().isBlank() || command.username().isBlank() || password.length == 0) {
                return KetQua.failure("HR_INVALID", "Thông tin bắt buộc của nhân viên chưa đầy đủ.");
            }
            return transactions.inTransaction(() -> {
                if (!authorization.canManageEmployees(command.actorId())) {
                    return KetQua.failure("HR_FORBIDDEN", "Không có quyền quản lý nhân viên.");
                }
                if (employees.getByMaNhanVien(command.maNhanVien()).isPresent()) {
                    return KetQua.failure("HR_DUPLICATE_EMPLOYEE_CODE", "Mã nhân viên đã tồn tại.");
                }
                String normalizedUsername = command.username().toLowerCase(Locale.ROOT);
                if (employees.usernameExists(normalizedUsername)) {
                    return KetQua.failure("HR_DUPLICATE_USERNAME", "Tên đăng nhập đã tồn tại.");
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
                auditLog.append(NhanSuKiemToanHanhDong.USER_CREATED, command.actorId(), employee.getId());
                return KetQua.success(new CreateNhanVienBienNhan(
                        employee.getId(), employee.getMaNhanVien(), employee.getUsername()));
            });
        } finally {
            java.util.Arrays.fill(password, '\0');
            command.close();
        }
    }
}
