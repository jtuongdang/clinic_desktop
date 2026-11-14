package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Query;

import java.util.List;

public record GetDichVuTheoLuotKhamQuery(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `sessionContext`: phien ngữ cảnh. */
        PhanQuyenNguCanh sessionContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext
) implements Query<List<DichVuYeuCauDuLieu>> {}
