package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;

public record AppointmentReceipt(Integer appointmentId, TrangThaiLichHen status) {
}
