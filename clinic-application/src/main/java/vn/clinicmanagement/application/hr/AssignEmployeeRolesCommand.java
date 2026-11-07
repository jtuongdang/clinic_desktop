package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.Command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AssignEmployeeRolesCommand(int actorId, int employeeId, List<Integer> roleIds)
        implements Command<AssignEmployeeRolesReceipt> {
    public AssignEmployeeRolesCommand {
        roleIds = roleIds == null ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(roleIds));
    }
}
