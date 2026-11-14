package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;

/** Trường `appointmentId`: lịch hẹn mã. */
/** Trường `status`: trạng thái. */
public record LichHenBienNhan(Integer appointmentId, TrangThaiLichHen status) {
}
