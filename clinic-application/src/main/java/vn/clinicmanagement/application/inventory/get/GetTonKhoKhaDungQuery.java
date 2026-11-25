package vn.clinicmanagement.application.inventory.get;

import vn.clinicmanagement.application.inventory.TonKhoKhaDungDuLieu;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.util.UUID;

public record GetTonKhoKhaDungQuery(
        int warehouseId,
        int itemId,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Query<TonKhoKhaDungDuLieu> {

    public GetTonKhoKhaDungQuery(int warehouseId, int itemId, PhanQuyenNguCanh authorizationContext) {
        this(warehouseId, itemId, authorizationContext, createAuditActor(authorizationContext));
    }

    private static KiemToanTacNhanNguCanh createAuditActor(PhanQuyenNguCanh context) {
        return context != null && context.isValid()
                ? new KiemToanTacNhanNguCanh(
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
