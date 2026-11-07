package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.List;
import java.util.UUID;

public record CreatePurchaseOrderCommand(
        String soDonDatHang,
        int supplierId,
        List<PurchaseOrderItemInput> items,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<CreatePurchaseOrderReceipt> {

    public CreatePurchaseOrderCommand(
            String soDonDatHang,
            int supplierId,
            List<PurchaseOrderItemInput> items,
            AuthorizationContext authorizationContext) {
        this(soDonDatHang, supplierId, items, authorizationContext, createAuditActor(authorizationContext));
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
