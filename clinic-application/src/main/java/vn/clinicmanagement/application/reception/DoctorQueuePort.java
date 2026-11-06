package vn.clinicmanagement.application.reception;

import java.time.LocalDate;
import java.util.List;

/** Read port for the active doctor team queue. */
public interface DoctorQueuePort {
    List<ReceptionQueueItem> getActiveDoctorQueue(LocalDate date, int facilityId, int departmentId);
}
