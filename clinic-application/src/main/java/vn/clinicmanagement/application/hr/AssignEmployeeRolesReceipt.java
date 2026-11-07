package vn.clinicmanagement.application.hr;

import java.util.List;

public record AssignEmployeeRolesReceipt(int employeeId, List<Integer> roleIds) {
    public AssignEmployeeRolesReceipt {
        roleIds = List.copyOf(roleIds);
    }
}
