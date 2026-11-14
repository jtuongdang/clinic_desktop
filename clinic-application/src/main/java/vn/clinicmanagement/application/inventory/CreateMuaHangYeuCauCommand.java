package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.List;
import java.util.UUID;

public record CreateMuaHangYeuCauCommand(
        String soDonDatHang,
        /** Trường `supplierId`: `supplier` mã. */
        int supplierId,
        List<MuaHangYeuCauMatHangDauVao> items,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Command<CreateMuaHangYeuCauBienNhan> {

    public CreateMuaHangYeuCauCommand(
            String soDonDatHang,
            int supplierId,
            List<MuaHangYeuCauMatHangDauVao> items,
            PhanQuyenNguCanh authorizationContext) {
        this(soDonDatHang, supplierId, items, authorizationContext, createAuditActor(authorizationContext));
    }

    /** Hàm `createAuditActor`: tạo kiểm toán tacnhan. */
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
