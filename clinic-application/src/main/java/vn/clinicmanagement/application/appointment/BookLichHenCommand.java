package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.time.LocalDateTime;

public record BookLichHenCommand(
        /** Trường `patientId`: bệnh nhân mã. */
        int patientId,
        /** Trường `doctorId`: bacsi mã. */
        int doctorId,
        /** Trường `facilityId`: cơ sở mã. */
        int facilityId,
        /** Trường `departmentId`: khoa mã. */
        int departmentId,
        /** Trường `startTime`: bắt đầu thời gian. */
        LocalDateTime startTime,
        /** Trường `endTime`: `end` thời gian. */
        LocalDateTime endTime,
        /** Trường `reason`: lý do. */
        String reason,
        /** Trường `bookingSource`: `booking` nguồn. */
        String bookingSource,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext) implements Command<LichHenBienNhan> {
}
