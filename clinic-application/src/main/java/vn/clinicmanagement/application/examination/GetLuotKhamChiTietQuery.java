package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

public record GetLuotKhamChiTietQuery(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `sessionContext`: phien ngữ cảnh. */
        PhanQuyenNguCanh sessionContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Query<LuotKhamChiTietDuLieu> {

    public GetLuotKhamChiTietQuery(int visitId, PhanQuyenNguCanh sessionContext) {
        this(visitId, sessionContext, null);
    }

    public GetLuotKhamChiTietQuery(int visitId) {
        this(visitId, null, null);
    }
}
