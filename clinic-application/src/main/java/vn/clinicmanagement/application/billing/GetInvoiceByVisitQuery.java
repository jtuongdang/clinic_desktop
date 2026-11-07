package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;

public record GetInvoiceByVisitQuery(
        int visitId,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext
) implements Query<InvoiceDto> {}
