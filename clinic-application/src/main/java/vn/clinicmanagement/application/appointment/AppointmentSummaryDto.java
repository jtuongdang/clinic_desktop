package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;

import java.time.LocalDateTime;

/**
 * Summary DTO of an upcoming appointment.
 */
public record AppointmentSummaryDto(
        int appointmentId,
        int doctorId,
        int facilityId,
        Integer departmentId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        String reason,
        String bookingSource) {

    public AppointmentSummaryDto(
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
