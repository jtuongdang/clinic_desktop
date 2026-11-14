package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordSinhHieuDauHieuCommand(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        Integer mach,
        BigDecimal nhietDo,
        Integer huyetApTamThu,
        Integer huyetApTamTruong,
        Integer nhipTho,
        BigDecimal canNang,
        BigDecimal chieuCao,
        BigDecimal spo2,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Command<RecordSinhHieuDauHieuBienNhan> {

    public RecordSinhHieuDauHieuCommand(
            int visitId,
            Integer mach,
            BigDecimal nhietDo,
            Integer huyetApTamThu,
            Integer huyetApTamTruong,
            Integer nhipTho,
            BigDecimal canNang,
            BigDecimal chieuCao,
            BigDecimal spo2,
            PhanQuyenNguCanh authorizationContext) {
        this(visitId, mach, nhietDo, huyetApTamThu, huyetApTamTruong, nhipTho, canNang, chieuCao, spo2,
                authorizationContext, auditActor(authorizationContext));
    }

    public RecordSinhHieuDauHieuCommand(int visitId, PhanQuyenNguCanh authorizationContext) {
        this(visitId, null, null, null, null, null, null, null, null, authorizationContext);
    }

    /** Hàm `auditActor`: kiểm toán tacnhan. */
    private static KiemToanTacNhanNguCanh auditActor(PhanQuyenNguCanh context) {
        return context != null && context.isValid()
                ? new KiemToanTacNhanNguCanh(context.employeeId(), context.roleId(), context.roleCode(),
                        context.facilityId(), context.departmentId(), null, UUID.randomUUID())
                : null;
    }
}
