package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;

import java.util.List;
import java.util.UUID;

public record GetPrescriptionByVisitQuery(
        int visitId,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Query<List<PrescriptionDto>> {

    public GetPrescriptionByVisitQuery(int visitId, AuthorizationContext authorizationContext) {
        this(visitId, authorizationContext, createAuditActor(authorizationContext));
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
