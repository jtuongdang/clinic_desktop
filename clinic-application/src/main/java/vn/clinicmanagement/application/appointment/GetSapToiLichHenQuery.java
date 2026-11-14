package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.util.List;

/**
 * Query to retrieve upcoming appointments for a patient.
 */
public record GetSapToiLichHenQuery(
        /** Trường `patientId`: bệnh nhân mã. */
        int patientId,
        /** Trường `sessionContext`: phien ngữ cảnh. */
        PhanQuyenNguCanh sessionContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Query<List<LichHenTongHopDuLieu>> {

    public GetSapToiLichHenQuery(int patientId, PhanQuyenNguCanh sessionContext) {
        this(patientId, sessionContext, null);
    }

    public GetSapToiLichHenQuery(int patientId) {
        this(patientId, null, null);
    }

    /** Hàm `authorizationContext`: phanquyen ngữ cảnh. */
    public PhanQuyenNguCanh authorizationContext() {
        return sessionContext;
    }

    /** Hàm `auditContext`: kiểm toán ngữ cảnh. */
    public KiemToanTacNhanNguCanh auditContext() {
        return auditActorContext;
    }
}
