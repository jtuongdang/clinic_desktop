package vn.clinicmanagement.application.examination;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Cổng đọc/ghi các thao tác ngữ cảnh trên hàng đợi Khám Bệnh & Quản lý giường nội trú. */
public interface KhamBenhQueueActionsPort {

    record GuardianInfo(String contactName, String contactPhone) {}

    record ReexamHistoryRow(
            int visitId,
            String visitCode,
            LocalDateTime visitedAt,
            String roomName,
            String diagnosis,
            String status
    ) {}

    record PatientCardData(
            int visitId,
            String visitCode,
            String patientCode,
            String patientName,
            LocalDate birthDate,
            String gender,
            String phone,
            String address,
            String departmentName,
            String roomName,
            String subjectType,
            String bhytNumber,
            LocalDateTime admittedAt
    ) {}

    record EmrSyncSummary(
            int visitId,
            int vitalsCount,
            int diagnosisCount,
            int serviceOrderCount,
            int prescriptionCount,
            LocalDateTime syncedAt
    ) {}

    record InpatientAdmissionRow(
            int visitId,
            String visitCode,
            String patientCode,
            String patientName,
            String gender,
            String departmentName,
            String treatmentDirection,
            String bedAssignment,
            LocalDateTime admittedAt
    ) {}

    void callPatientToRoom(int visitId, int facilityId, int actorId);

    void bookFollowUpAppointment(int visitId, int patientId, int doctorId, int facilityId, LocalDateTime date, String reason);

    GuardianInfo getGuardianInfo(int patientId);

    void updateGuardianInfo(int patientId, GuardianInfo info, int actorId);

    String getVisitNote(int visitId);

    void updateVisitNote(int visitId, String note, int actorId);

    List<ReexamHistoryRow> listReexamHistory(int patientId, int facilityId);

    PatientCardData getPatientCardData(int visitId, int facilityId);

    void changeVisitSubject(int visitId, String subjectType, int actorId);

    EmrSyncSummary syncEmrSummary(int visitId, int facilityId, int actorId);

    List<InpatientAdmissionRow> listInpatientAdmissions(int facilityId);

    void assignInpatientBed(int visitId, String wardName, String bedCode, String note, int actorId);
}
