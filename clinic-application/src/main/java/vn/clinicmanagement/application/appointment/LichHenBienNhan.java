package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;

public record LichHenBienNhan(Integer appointmentId, TrangThaiLichHen status) {
}
