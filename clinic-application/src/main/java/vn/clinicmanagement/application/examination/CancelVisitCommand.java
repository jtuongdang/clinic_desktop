package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

public record CancelVisitCommand(
        int visitId,
        String cancellationReason,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<CancelVisitReceipt> {
}
