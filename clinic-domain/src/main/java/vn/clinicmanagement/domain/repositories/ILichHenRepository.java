package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.appointment.LichHen;
import java.time.LocalDateTime;
import java.util.List;

public interface ILichHenRepository extends IRepository<LichHen> {
    List<LichHen> getUpcomingAppointments(int patientId);
    int countAppointmentsInSlot(int doctorId, LocalDateTime start, LocalDateTime end);
}
