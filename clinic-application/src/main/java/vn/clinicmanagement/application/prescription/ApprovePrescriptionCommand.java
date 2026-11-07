package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record ApprovePrescriptionCommand(
        int prescriptionId,
        int doctorId,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<ApprovePrescriptionReceipt> {

    public ApprovePrescriptionCommand(
            int prescriptionId, int doctorId, AuthorizationContext authorizationContext) {
        this(prescriptionId, doctorId, authorizationContext, createAuditActor(authorizationContext));
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
