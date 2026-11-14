package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.common.Query;
import vn.clinicmanagement.domain.facility.PhongKham;

import java.util.List;

/** Protected active-room picker request. */
public record GetHoatDongTiepDonPhongQuery(PhanQuyenNguCanh authorizationContext,
                                           /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
                                           KiemToanTacNhanNguCanh auditActorContext)
        implements Query<List<PhongKham>> {}
