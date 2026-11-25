package vn.clinicmanagement.application.patienthistory.get;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;
import vn.clinicmanagement.domain.patient.TienSuBenh;

import java.util.List;

/**
 * Yêu cầu đọc tiền sử bệnh nhân, gắn với các ngữ cảnh của thao tác đã xác thực.
 */
public record GetBenhNhanTienSuQuery(
        int patientId,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Query<List<TienSuBenh>> {
}
