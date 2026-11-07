package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;

public record OrderServiceCommand(
        int visitId,
        int serviceId,
        BigDecimal quantity,
        Integer roomId,
        String doctorNotes,
        AuthorizationContext sessionContext,
        AuditActorContext auditActorContext
) implements Command<OrderServiceReceipt> {}
