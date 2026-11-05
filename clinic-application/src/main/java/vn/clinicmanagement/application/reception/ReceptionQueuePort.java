package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;

import java.time.LocalDate;
import java.util.List;

/** Persistence port for creating a reception visit and reading the live reception queue. */
public interface ReceptionQueuePort {
    LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                           String registrationType, String reason, DoiTuongKham payerType,
                           boolean priority, int createdBy);
    List<ReceptionQueueItem> getQueue(LocalDate date);
    List<ClinicRoomQueueSummary> getRoomSummaries(LocalDate date);
}
