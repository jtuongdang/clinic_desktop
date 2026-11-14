package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;

import java.time.LocalDateTime;

/**
 * Summary DTO of an upcoming appointment.
 */
public record LichHenTongHopDuLieu(
        /** Trường `appointmentId`: lịch hẹn mã. */
        int appointmentId,
        /** Trường `doctorId`: bacsi mã. */
        int doctorId,
        /** Trường `facilityId`: cơ sở mã. */
        int facilityId,
        /** Trường `departmentId`: khoa mã. */
        Integer departmentId,
        /** Trường `startTime`: bắt đầu thời gian. */
        LocalDateTime startTime,
        /** Trường `endTime`: `end` thời gian. */
        LocalDateTime endTime,
        /** Trường `status`: trạng thái. */
        String status,
        /** Trường `reason`: lý do. */
        String reason,
        /** Trường `bookingSource`: `booking` nguồn. */
        String bookingSource) {

    public LichHenTongHopDuLieu(
            int appointmentId,
            int doctorId,
            int facilityId,
            Integer departmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            TrangThaiLichHen status,
            String reason,
            String bookingSource) {
        this(
                appointmentId,
                doctorId,
                facilityId,
                departmentId,
                startTime,
                endTime,
                status != null ? status.name() : null,
                reason,
                bookingSource);
    }
}
