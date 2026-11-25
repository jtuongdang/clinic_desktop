package vn.clinicmanagement.application.appointment.cancel;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

public record CancelLichHenCommand(
        int appointmentId,
        String cancellationReason,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<CancelLichHenBienNhan> {
}
