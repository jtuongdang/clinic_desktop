package vn.clinicmanagement.application.reception.get;

import vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;
import java.util.List;

/**
 * Yêu cầu xem hàng đợi nhóm bác sĩ đang hoạt động trong phạm vi làm việc đã chọn.
 */
public record GetBacSiHangDoiQuery(LocalDate date, PhanQuyenNguCanh authorizationContext,
                                  KiemToanTacNhanNguCanh auditActorContext) implements Query<List<TiepDonHangDoiMatHang>> {}
