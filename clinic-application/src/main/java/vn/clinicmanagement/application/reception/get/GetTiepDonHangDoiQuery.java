package vn.clinicmanagement.application.reception.get;

import vn.clinicmanagement.application.reception.TiepDonHangDoiAnhChup;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;

/**
 * Yêu cầu xem hàng đợi và phần tổng hợp phòng đã được bảo vệ.
 */
public record GetTiepDonHangDoiQuery(LocalDate date, PhanQuyenNguCanh authorizationContext,
                                     KiemToanTacNhanNguCanh auditActorContext) implements Query<TiepDonHangDoiAnhChup> {
    /** Các điểm gọi cũ bị từ chối an toàn vì không có ngữ cảnh thao tác đã xác thực. */
    @Deprecated
    public GetTiepDonHangDoiQuery(LocalDate date) { this(date, null, null); }
}
