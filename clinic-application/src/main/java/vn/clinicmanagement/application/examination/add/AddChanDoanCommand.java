package vn.clinicmanagement.application.examination.add;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record AddChanDoanCommand(
        int visitId,
        int doctorId,
        Integer icdId,
        String chanDoanText,
        boolean laChinh,
        String loaiChanDoan,
        PhanQuyenNguCanh authorizationContext,
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
