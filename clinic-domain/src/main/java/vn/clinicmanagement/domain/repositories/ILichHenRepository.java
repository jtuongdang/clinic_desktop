package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.appointment.LichHen;
import java.time.LocalDateTime;
import java.util.List;

public interface ILichHenRepository extends IRepository<LichHen> {
    /** Hàm `getUpcomingAppointments`: lấy saptoi lịch hẹn. */
    List<LichHen> getUpcomingAppointments(int patientId);
    /** Hàm `countAppointmentsInSlot`: dem lịch hẹn trong `Slot`. */
    int countAppointmentsInSlot(int doctorId, LocalDateTime start, LocalDateTime end);
}
