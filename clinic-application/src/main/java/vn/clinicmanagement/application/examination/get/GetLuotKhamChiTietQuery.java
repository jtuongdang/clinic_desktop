package vn.clinicmanagement.application.examination.get;

import vn.clinicmanagement.application.examination.LuotKhamChiTietDuLieu;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

public record GetLuotKhamChiTietQuery(
        int visitId,
        PhanQuyenNguCanh sessionContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Query<LuotKhamChiTietDuLieu> {

    public GetLuotKhamChiTietQuery(int visitId, PhanQuyenNguCanh sessionContext) {
        this(visitId, sessionContext, null);
    }

    public GetLuotKhamChiTietQuery(int visitId) {
        this(visitId, null, null);
    }
}
