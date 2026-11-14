package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateMatHangCommand(
        /** Trường `maItem`: `ma` mathang. */
        String maItem,
        /** Trường `tenItem`: `ten` mathang. */
        String tenItem,
        /** Trường `loaiItem`: `loai` mathang. */
        LoaiMatHang loaiItem,
        String hoatChat,
        String hamLuong,
        String donViCoBan,
        /** Trường `quyCachDongGoi`: quy `Cach` đóng `Goi`. */
        String quyCachDongGoi,
        BigDecimal giaNhapMacDinh,
        BigDecimal giaBanMacDinh,
        BigDecimal tonToiThieu,
        BigDecimal tonToiDa,
        String dangBaoChe,
        String nuocSanXuat,
        String hangSanXuat,
        String soDangKy,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
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

    /** Hàm `createAuditActor`: tạo kiểm toán tacnhan. */
    private static KiemToanTacNhanNguCanh createAuditActor(PhanQuyenNguCanh context) {
        return context != null && context.isValid()
                ? new KiemToanTacNhanNguCanh(context.employeeId(), context.roleId(), context.roleCode(),
                        context.facilityId(), context.departmentId(), null, UUID.randomUUID())
                : null;
    }
}
