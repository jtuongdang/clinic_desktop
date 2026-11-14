package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

public record CancelDichVuYeuCauCommand(
        /** Trường `orderId`: yêu cầu mã. */
        int orderId,
        /** Trường `reason`: lý do. */
        String reason,
        /** Trường `sessionContext`: phien ngữ cảnh. */
        PhanQuyenNguCanh sessionContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<CancelDichVuYeuCauBienNhan> {}
