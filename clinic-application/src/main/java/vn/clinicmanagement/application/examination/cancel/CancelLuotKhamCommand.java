package vn.clinicmanagement.application.examination.cancel;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

public record CancelLuotKhamCommand(
        int visitId,
        String cancellationReason,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<CancelLuotKhamBienNhan> {
}
