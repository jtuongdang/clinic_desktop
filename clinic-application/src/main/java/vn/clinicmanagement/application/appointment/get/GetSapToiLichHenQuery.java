package vn.clinicmanagement.application.appointment.get;

import vn.clinicmanagement.application.appointment.LichHenTongHopDuLieu;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.util.List;

/**
 * Truy vấn lấy các lịch hẹn sắp tới của bệnh nhân.
 */
public record GetSapToiLichHenQuery(
        int patientId,
        PhanQuyenNguCanh sessionContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Query<List<LichHenTongHopDuLieu>> {

    public GetSapToiLichHenQuery(int patientId, PhanQuyenNguCanh sessionContext) {
        this(patientId, sessionContext, null);
    }

    public GetSapToiLichHenQuery(int patientId) {
        this(patientId, null, null);
    }

    public PhanQuyenNguCanh authorizationContext() {
        return sessionContext;
    }

    public KiemToanTacNhanNguCanh auditContext() {
        return auditActorContext;
    }
}
