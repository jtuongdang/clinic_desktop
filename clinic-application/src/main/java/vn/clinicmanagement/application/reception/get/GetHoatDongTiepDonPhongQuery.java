package vn.clinicmanagement.application.reception.get;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.common.Query;
import vn.clinicmanagement.domain.facility.PhongKham;

import java.util.List;

/**
 * Yêu cầu chọn phòng đang hoạt động đã được bảo vệ quyền truy cập.
 */
public record GetHoatDongTiepDonPhongQuery(PhanQuyenNguCanh authorizationContext,
                                           KiemToanTacNhanNguCanh auditActorContext)
        implements Query<List<PhongKham>> {}
