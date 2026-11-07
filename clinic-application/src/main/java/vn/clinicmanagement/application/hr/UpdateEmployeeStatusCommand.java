package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.Command;

public record UpdateEmployeeStatusCommand(int actorId, int employeeId, boolean active)
        implements Command<UpdateEmployeeStatusReceipt> {}
