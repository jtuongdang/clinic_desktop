package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record CancelPrescriptionCommand(
        int prescriptionId,
        int doctorId,
        String lyDoHuy,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<CancelPrescriptionReceipt> {

    public CancelPrescriptionCommand(
            int prescriptionId,
            int doctorId,
            String lyDoHuy,
            AuthorizationContext authorizationContext) {
        this(prescriptionId, doctorId, lyDoHuy, authorizationContext, createAuditActor(authorizationContext));
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
