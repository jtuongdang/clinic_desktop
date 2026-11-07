package vn.clinicmanagement.application.hr;

import java.util.List;

public interface EmployeeRoleAssignments {
    void replaceRoleIds(int employeeId, List<Integer> roleIds);
}
