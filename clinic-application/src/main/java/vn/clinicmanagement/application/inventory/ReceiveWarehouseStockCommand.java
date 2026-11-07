package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.List;
import java.util.UUID;

public record ReceiveWarehouseStockCommand(
        String soPhieuNhap,
        int warehouseId,
        int supplierId,
        List<StockReceiptItemInput> items,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<ReceiveWarehouseStockReceipt> {

    public ReceiveWarehouseStockCommand {
        items = items != null ? List.copyOf(items) : null;
    }

    public ReceiveWarehouseStockCommand(
            String soPhieuNhap,
            int warehouseId,
            int supplierId,
            List<StockReceiptItemInput> items,
            AuthorizationContext authorizationContext) {
        this(soPhieuNhap, warehouseId, supplierId, items, authorizationContext, createAuditActor(authorizationContext));
    }

    public static AuditActorContext createAuditActor(AuthorizationContext authorizationContext) {
        if (authorizationContext == null || !authorizationContext.isValid()) {
            return null;
        }
        return new AuditActorContext(
                authorizationContext.employeeId(),
                authorizationContext.roleId(),
                authorizationContext.roleCode(),
                authorizationContext.facilityId(),
                authorizationContext.departmentId(),
                null,
                authorizationContext.sessionId() != null ? authorizationContext.sessionId() : UUID.randomUUID());
    }
}
