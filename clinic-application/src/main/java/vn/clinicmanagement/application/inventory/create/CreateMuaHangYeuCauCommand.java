package vn.clinicmanagement.application.inventory.create;

import vn.clinicmanagement.application.inventory.MuaHangYeuCauMatHangDauVao;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.List;
import java.util.UUID;

public record CreateMuaHangYeuCauCommand(
        String soDonDatHang,
        int supplierId,
        List<MuaHangYeuCauMatHangDauVao> items,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<CreateMuaHangYeuCauBienNhan> {

    public CreateMuaHangYeuCauCommand(
            String soDonDatHang,
            int supplierId,
            List<MuaHangYeuCauMatHangDauVao> items,
            PhanQuyenNguCanh authorizationContext) {
        this(soDonDatHang, supplierId, items, authorizationContext, createAuditActor(authorizationContext));
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
