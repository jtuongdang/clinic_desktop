package vn.clinicmanagement.domain.appointment;
public interface IThongBaoDichVu {
    /** Hàm `sendAppointmentConfirmation`: `send` lịch hẹn `Confirmation`. */
    void sendAppointmentConfirmation(LichHen appointment);
    /** Hàm `sendAppointmentReminder`: `send` lịch hẹn `Reminder`. */
    void sendAppointmentReminder(LichHen appointment);
    void sendCancellationNotice(LichHen appointment, String reason);
}
