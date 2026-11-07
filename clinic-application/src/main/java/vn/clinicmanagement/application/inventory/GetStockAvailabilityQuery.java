package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;

import java.util.UUID;

public record GetStockAvailabilityQuery(
        int warehouseId,
        int itemId,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Query<StockAvailabilityDto> {

    public GetStockAvailabilityQuery(int warehouseId, int itemId, AuthorizationContext authorizationContext) {
        this(warehouseId, itemId, authorizationContext, createAuditActor(authorizationContext));
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
