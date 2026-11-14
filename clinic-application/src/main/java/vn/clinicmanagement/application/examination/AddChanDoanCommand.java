package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record AddChanDoanCommand(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `doctorId`: bacsi mã. */
        int doctorId,
        /** Trường `icdId`: icd mã. */
        Integer icdId,
        /** Trường `chanDoanText`: `chan` doan văn bản. */
        String chanDoanText,
        boolean laChinh,
        String loaiChanDoan,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Command<AddChanDoanBienNhan> {

    public AddChanDoanCommand(int visitId, int doctorId, Integer icdId, String chanDoanText,
                               boolean laChinh, String loaiChanDoan, PhanQuyenNguCanh authorizationContext) {
        this(visitId, doctorId, icdId, chanDoanText, laChinh, loaiChanDoan, authorizationContext,
                createAuditActor(authorizationContext));
    }

    public AddChanDoanCommand(int visitId, int doctorId, String chanDoanText, boolean laChinh,
                               PhanQuyenNguCanh authorizationContext) {
        this(visitId, doctorId, null, chanDoanText, laChinh, null, authorizationContext);
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
