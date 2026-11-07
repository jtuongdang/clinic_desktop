package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;

import java.util.List;

public record GetServicesByVisitQuery(
        int visitId,
        AuthorizationContext sessionContext,
        AuditActorContext auditActorContext
) implements Query<List<ServiceOrderDto>> {}
