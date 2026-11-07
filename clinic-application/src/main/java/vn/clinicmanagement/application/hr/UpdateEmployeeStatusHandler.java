package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

public final class UpdateEmployeeStatusHandler implements
        CommandHandler<UpdateEmployeeStatusCommand, UpdateEmployeeStatusReceipt> {
    private final INhanVienRepository employees;
    private final HrAuthorization authorization;
    private final HrAuditLog auditLog;
    private final TransactionBoundary transactions;

    public UpdateEmployeeStatusHandler(INhanVienRepository employees, HrAuthorization authorization,
                                       HrAuditLog auditLog, TransactionBoundary transactions) {
        this.employees = employees;
        this.authorization = authorization;
        this.auditLog = auditLog;
        this.transactions = transactions;
    }

    @Override
    public Result<UpdateEmployeeStatusReceipt> handle(UpdateEmployeeStatusCommand command) {
        if (command == null || command.actorId() <= 0 || command.employeeId() <= 0) {
            return Result.failure("HR_INVALID", "Thông tin cập nhật nhân viên không hợp lệ.");
        }
        return transactions.inTransaction(() -> {
            if (!authorization.canManageEmployees(command.actorId())) {
                return Result.failure("HR_FORBIDDEN", "Không có quyền quản lý nhân viên.");
            }
            NhanVien employee = employees.getById(command.employeeId()).orElse(null);
            if (employee == null || employee.isDeleted()) {
                return Result.failure("HR_EMPLOYEE_NOT_FOUND", "Không tìm thấy nhân viên.");
            }
            employee.setHoatDong(command.active());
            employee.markAsUpdated(command.actorId());
            employees.update(employee);
            HrAuditAction action = command.active()
                    ? HrAuditAction.USER_UPDATED : HrAuditAction.USER_DEACTIVATED;
            auditLog.append(action, command.actorId(), employee.getId());
            return Result.success(new UpdateEmployeeStatusReceipt(employee.getId(), employee.isHoatDong()));
        });
    }
}
