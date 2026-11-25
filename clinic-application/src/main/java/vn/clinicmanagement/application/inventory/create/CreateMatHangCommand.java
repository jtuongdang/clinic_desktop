package vn.clinicmanagement.application.inventory.create;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateMatHangCommand(
        String maItem,
        String tenItem,
        LoaiMatHang loaiItem,
        String hoatChat,
        String hamLuong,
        String donViCoBan,
        String quyCachDongGoi,
        BigDecimal giaNhapMacDinh,
        BigDecimal giaBanMacDinh,
        BigDecimal tonToiThieu,
        BigDecimal tonToiDa,
        String dangBaoChe,
        String nuocSanXuat,
        String hangSanXuat,
        String soDangKy,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<CreateMatHangBienNhan> {

    public CreateMatHangCommand(
            String maItem,
            String tenItem,
            LoaiMatHang loaiItem,
            String hoatChat,
            String hamLuong,
            String donViCoBan,
            String quyCachDongGoi,
            BigDecimal giaNhapMacDinh,
            BigDecimal giaBanMacDinh,
            BigDecimal tonToiThieu,
            BigDecimal tonToiDa,
            String dangBaoChe,
            String nuocSanXuat,
            String hangSanXuat,
            String soDangKy,
            PhanQuyenNguCanh authorizationContext) {
        this(maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi,
                giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat,
                hangSanXuat, soDangKy, authorizationContext, createAuditActor(authorizationContext));
    }

    private static KiemToanTacNhanNguCanh createAuditActor(PhanQuyenNguCanh context) {
        return context != null && context.isValid()
                ? new KiemToanTacNhanNguCanh(context.employeeId(), context.roleId(), context.roleCode(),
                        context.facilityId(), context.departmentId(), null, UUID.randomUUID())
                : null;
    }
}
