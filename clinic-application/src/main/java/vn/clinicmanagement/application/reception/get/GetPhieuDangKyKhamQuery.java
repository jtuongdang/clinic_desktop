package vn.clinicmanagement.application.reception.get;

import vn.clinicmanagement.application.reception.PhieuDangKyKhamDuLieu;
import vn.clinicmanagement.application.common.Query;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
/** Yêu cầu đọc phiếu, định danh bằng lượt khám thay vì nội dung đang gõ trên form. */
public record GetPhieuDangKyKhamQuery(int visitId, PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext,
        vn.clinicmanagement.domain.common.enums.HanhDongQuyen action) implements Query<PhieuDangKyKhamDuLieu> {
    public GetPhieuDangKyKhamQuery(int visitId, PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        this(visitId, context, actor, vn.clinicmanagement.domain.common.enums.HanhDongQuyen.XEM);
    }
}
