package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

public record CancelLichHenCommand(
        /** Trường `appointmentId`: lịch hẹn mã. */
        int appointmentId,
        /** Trường `cancellationReason`: `cancellation` lý do. */
        String cancellationReason,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Command<CancelLichHenBienNhan> {
}
