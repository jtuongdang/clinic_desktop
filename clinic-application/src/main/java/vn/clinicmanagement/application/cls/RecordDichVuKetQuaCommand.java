package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.util.List;

public record RecordDichVuKetQuaCommand(
        /** Trường `orderId`: yêu cầu mã. */
        int orderId,
        /** Trường `resultSummary`: kết quả tonghop. */
        String resultSummary,
        String conclusion,
        String imageAttachmentUrls,
        /** Trường `resultItems`: kết quả `Items`. */
        List<XetNghiemKetQuaMatHangDauVao> resultItems,
        /** Trường `sessionContext`: phien ngữ cảnh. */
        PhanQuyenNguCanh sessionContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<RecordDichVuKetQuaBienNhan> {}
