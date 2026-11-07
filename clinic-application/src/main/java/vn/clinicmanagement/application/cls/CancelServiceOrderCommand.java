package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

public record CancelServiceOrderCommand(
        int orderId,
        String reason,
        AuthorizationContext sessionContext,
        AuditActorContext auditActorContext
) implements Command<CancelServiceOrderReceipt> {}
