package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record ReopenEmrCommand(
        int visitId,
        int approverId,
        String lyDoMoKhoa,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext
) implements Command<ReopenEmrReceipt> {

    public ReopenEmrCommand(int visitId, int approverId, String lyDoMoKhoa, AuthorizationContext authorizationContext) {
        this(visitId, approverId, lyDoMoKhoa, authorizationContext, createAuditActor(authorizationContext));
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
