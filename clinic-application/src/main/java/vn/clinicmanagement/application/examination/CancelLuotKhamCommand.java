package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

public record CancelLuotKhamCommand(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `cancellationReason`: `cancellation` lý do. */
        String cancellationReason,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Command<CancelLuotKhamBienNhan> {
}
