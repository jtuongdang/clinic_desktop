package vn.clinicmanagement.domain.appointment;
public interface INotificationService {
    void sendAppointmentConfirmation(LichHen appointment);
    void sendAppointmentReminder(LichHen appointment);
    void sendCancellationNotice(LichHen appointment, String reason);
}
