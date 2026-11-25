package vn.clinicmanagement.application.appointment.book;

import vn.clinicmanagement.application.appointment.LichHenBienNhan;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.time.LocalDateTime;

public record BookLichHenCommand(
        int patientId,
        int doctorId,
        int facilityId,
        int departmentId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason,
        String bookingSource,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext) implements Command<LichHenBienNhan> {
}
