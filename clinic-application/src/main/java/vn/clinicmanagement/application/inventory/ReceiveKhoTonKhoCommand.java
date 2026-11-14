package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.List;
import java.util.UUID;

public record ReceiveKhoTonKhoCommand(
        String soPhieuNhap,
        /** Trường `warehouseId`: kho mã. */
        int warehouseId,
        /** Trường `supplierId`: `supplier` mã. */
        int supplierId,
        List<TonKhoBienNhanMatHangDauVao> items,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Command<ReceiveKhoTonKhoBienNhan> {

    public ReceiveKhoTonKhoCommand {
        items = items != null ? List.copyOf(items) : null;
    }

    public ReceiveKhoTonKhoCommand(
            String soPhieuNhap,
            int warehouseId,
            int supplierId,
            List<TonKhoBienNhanMatHangDauVao> items,
            PhanQuyenNguCanh authorizationContext) {
        this(soPhieuNhap, warehouseId, supplierId, items, authorizationContext, createAuditActor(authorizationContext));
    }

    /** Hàm `createAuditActor`: tạo kiểm toán tacnhan. */
    public static KiemToanTacNhanNguCanh createAuditActor(PhanQuyenNguCanh authorizationContext) {
        if (authorizationContext == null || !authorizationContext.isValid()) {
            return null;
        }
        return new KiemToanTacNhanNguCanh(
                authorizationContext.employeeId(),
                authorizationContext.roleId(),
                authorizationContext.roleCode(),
                authorizationContext.facilityId(),
                authorizationContext.departmentId(),
                null,
                authorizationContext.sessionId() != null ? authorizationContext.sessionId() : UUID.randomUUID());
    }
}
