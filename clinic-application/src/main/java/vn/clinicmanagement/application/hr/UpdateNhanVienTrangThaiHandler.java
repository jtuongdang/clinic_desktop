package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

public final class UpdateNhanVienTrangThaiHandler implements
        CommandHandler<UpdateNhanVienTrangThaiCommand, UpdateNhanVienTrangThaiBienNhan> {
    /** Trường `employees`: nhân viên. */
    private final INhanVienRepository employees;
    /** Trường `authorization`: phanquyen. */
    private final NhanSuPhanQuyen authorization;
    /** Trường `auditLog`: kiểm toán nhatky. */
    private final NhanSuKiemToanNhatKy auditLog;
    private final GiaoDichRanhGioi transactions;

    public UpdateNhanVienTrangThaiHandler(INhanVienRepository employees, NhanSuPhanQuyen authorization,
                                       NhanSuKiemToanNhatKy auditLog, GiaoDichRanhGioi transactions) {
        this.employees = employees;
        this.authorization = authorization;
        this.auditLog = auditLog;
        this.transactions = transactions;
    }

    @Override
    public KetQua<UpdateNhanVienTrangThaiBienNhan> handle(UpdateNhanVienTrangThaiCommand command) {
        if (command == null || command.actorId() <= 0 || command.employeeId() <= 0) {
            return KetQua.failure("HR_INVALID", "Thông tin cập nhật nhân viên không hợp lệ.");
        }
        return transactions.inTransaction(() -> {
            if (!authorization.canManageEmployees(command.actorId())) {
                return KetQua.failure("HR_FORBIDDEN", "Không có quyền quản lý nhân viên.");
            }
            NhanVien employee = employees.getById(command.employeeId()).orElse(null);
            if (employee == null || employee.isDeleted()) {
                return KetQua.failure("HR_EMPLOYEE_NOT_FOUND", "Không tìm thấy nhân viên.");
            }
            employee.setHoatDong(command.active());
            employee.markAsUpdated(command.actorId());
            employees.update(employee);
            NhanSuKiemToanHanhDong action = command.active()
                    ? NhanSuKiemToanHanhDong.USER_UPDATED : NhanSuKiemToanHanhDong.USER_DEACTIVATED;
            auditLog.append(action, command.actorId(), employee.getId());
            return KetQua.success(new UpdateNhanVienTrangThaiBienNhan(employee.getId(), employee.isHoatDong()));
        });
    }
}
