package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;

public record YeuCauDichVuCommand(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `serviceId`: dịch vụ mã. */
        int serviceId,
        BigDecimal quantity,
        /** Trường `roomId`: phòng mã. */
        Integer roomId,
        /** Trường `doctorNotes`: bacsi `Notes`. */
        String doctorNotes,
        /** Trường `sessionContext`: phien ngữ cảnh. */
        PhanQuyenNguCanh sessionContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<YeuCauDichVuBienNhan> {}
