package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record DispensePrescriptionCommand(
        int prescriptionId,
        int pharmacistId,
        int warehouseId,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<DispensePrescriptionReceipt> {

    public DispensePrescriptionCommand(
            int prescriptionId,
            int pharmacistId,
            int warehouseId,
            AuthorizationContext authorizationContext) {
        this(prescriptionId, pharmacistId, warehouseId, authorizationContext, createAuditActor(authorizationContext));
    }

    private static AuditActorContext createAuditActor(AuthorizationContext context) {
        return context != null && context.isValid()
                ? new AuditActorContext(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        context.departmentId(),
                        null,
                        UUID.randomUUID())
                : null;
    }
}
