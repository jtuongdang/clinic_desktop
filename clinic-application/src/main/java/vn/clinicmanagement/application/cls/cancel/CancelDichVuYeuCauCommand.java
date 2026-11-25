package vn.clinicmanagement.application.cls.cancel;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

public record CancelDichVuYeuCauCommand(
        int orderId,
        String reason,
        PhanQuyenNguCanh sessionContext,
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<CancelDichVuYeuCauBienNhan> {}
