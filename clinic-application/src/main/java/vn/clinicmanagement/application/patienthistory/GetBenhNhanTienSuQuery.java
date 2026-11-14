package vn.clinicmanagement.application.patienthistory;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;
import vn.clinicmanagement.domain.patient.TienSuBenh;

import java.util.List;

/** Patient history read request bound to the authenticated operation contexts. */
public record GetBenhNhanTienSuQuery(
        /** Trường `patientId`: bệnh nhân mã. */
        int patientId,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Query<List<TienSuBenh>> {
}
