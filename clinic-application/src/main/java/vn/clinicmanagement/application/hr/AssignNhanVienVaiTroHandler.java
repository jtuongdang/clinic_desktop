package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.util.HashSet;

public final class AssignNhanVienVaiTroHandler implements
        CommandHandler<AssignNhanVienVaiTroCommand, AssignNhanVienVaiTroBienNhan> {
    /** Trường `employees`: nhân viên. */
    private final INhanVienRepository employees;
    /** Trường `roleAssignments`: vai trò phân công. */
    private final NhanVienVaiTroPhanCong roleAssignments;
    /** Trường `authorization`: phanquyen. */
    private final NhanSuPhanQuyen authorization;
    /** Trường `auditLog`: kiểm toán nhatky. */
    private final NhanSuKiemToanNhatKy auditLog;
    private final GiaoDichRanhGioi transactions;

    public AssignNhanVienVaiTroHandler(INhanVienRepository employees, NhanVienVaiTroPhanCong roleAssignments,
                                      NhanSuPhanQuyen authorization, NhanSuKiemToanNhatKy auditLog,
                                      GiaoDichRanhGioi transactions) {
        this.employees = employees;
        this.roleAssignments = roleAssignments;
        this.authorization = authorization;
        this.auditLog = auditLog;
        this.transactions = transactions;
    }

    @Override
    public KetQua<AssignNhanVienVaiTroBienNhan> handle(AssignNhanVienVaiTroCommand command) {
        if (command == null || command.actorId() <= 0 || command.employeeId() <= 0
                || command.roleIds().stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(command.roleIds()).size() != command.roleIds().size()) {
            return KetQua.failure("HR_INVALID", "Thông tin phân quyền nhân viên không hợp lệ.");
        }
        return transactions.inTransaction(() -> {
            if (!authorization.canManageEmployees(command.actorId())) {
                return KetQua.failure("HR_FORBIDDEN", "Không có quyền quản lý nhân viên.");
            }
            NhanVien employee = employees.getById(command.employeeId()).orElse(null);
            if (employee == null || employee.isDeleted()) {
                return KetQua.failure("HR_EMPLOYEE_NOT_FOUND", "Không tìm thấy nhân viên.");
            }
            roleAssignments.replaceRoleIds(employee.getId(), command.roleIds());
            employee.markAsUpdated(command.actorId());
            employees.update(employee);
            auditLog.append(NhanSuKiemToanHanhDong.ROLE_ASSIGNMENT_CHANGED, command.actorId(), employee.getId());
            return KetQua.success(new AssignNhanVienVaiTroBienNhan(employee.getId(), command.roleIds()));
        });
    }
}
