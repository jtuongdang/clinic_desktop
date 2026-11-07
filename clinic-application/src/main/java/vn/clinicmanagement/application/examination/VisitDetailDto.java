package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VisitDetailDto(
        int visitId,
        int patientId,
        Integer doctorId,
        int facilityId,
        int departmentId,
        Integer roomId,
        String queueDisplay,
        Integer queueNumber,
        TrangThaiLuotKham status,
        String reasonForVisit,
        String clinicalProcess,
        String personalHistory,
        String familyHistory,
        String generalExam,
        String organExam,
        String summary,
        String treatmentPlan,
        String doctorAdvice,
        KetQuaDieuTri treatmentOutcome,
        HuongXuTri disposal,
        LocalDate followUpDate,
        boolean emrLocked,
        LocalDateTime emrLockedAt,
        Integer emrLockedBy,
        String emrSignature,
        LocalDateTime startedAt,
        LocalDateTime completedAt) {
}
