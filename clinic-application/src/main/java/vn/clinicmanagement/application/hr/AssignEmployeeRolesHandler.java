package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.util.HashSet;

public final class AssignEmployeeRolesHandler implements
        CommandHandler<AssignEmployeeRolesCommand, AssignEmployeeRolesReceipt> {
    private final INhanVienRepository employees;
    private final EmployeeRoleAssignments roleAssignments;
    private final HrAuthorization authorization;
    private final HrAuditLog auditLog;
    private final TransactionBoundary transactions;

    public AssignEmployeeRolesHandler(INhanVienRepository employees, EmployeeRoleAssignments roleAssignments,
                                      HrAuthorization authorization, HrAuditLog auditLog,
                                      TransactionBoundary transactions) {
        this.employees = employees;
        this.roleAssignments = roleAssignments;
        this.authorization = authorization;
        this.auditLog = auditLog;
        this.transactions = transactions;
    }

    @Override
    public Result<AssignEmployeeRolesReceipt> handle(AssignEmployeeRolesCommand command) {
        if (command == null || command.actorId() <= 0 || command.employeeId() <= 0
                || command.roleIds().stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(command.roleIds()).size() != command.roleIds().size()) {
            return Result.failure("HR_INVALID", "Thông tin phân quyền nhân viên không hợp lệ.");
        }
        return transactions.inTransaction(() -> {
            if (!authorization.canManageEmployees(command.actorId())) {
                return Result.failure("HR_FORBIDDEN", "Không có quyền quản lý nhân viên.");
            }
            NhanVien employee = employees.getById(command.employeeId()).orElse(null);
            if (employee == null || employee.isDeleted()) {
                return Result.failure("HR_EMPLOYEE_NOT_FOUND", "Không tìm thấy nhân viên.");
            }
            roleAssignments.replaceRoleIds(employee.getId(), command.roleIds());
            employee.markAsUpdated(command.actorId());
            employees.update(employee);
            auditLog.append(HrAuditAction.ROLE_ASSIGNMENT_CHANGED, command.actorId(), employee.getId());
            return Result.success(new AssignEmployeeRolesReceipt(employee.getId(), command.roleIds()));
        });
    }
}
