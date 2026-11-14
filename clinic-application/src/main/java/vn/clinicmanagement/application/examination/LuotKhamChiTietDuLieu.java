package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LuotKhamChiTietDuLieu(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `patientId`: bệnh nhân mã. */
        int patientId,
        /** Trường `doctorId`: bacsi mã. */
        Integer doctorId,
        /** Trường `facilityId`: cơ sở mã. */
        int facilityId,
        /** Trường `departmentId`: khoa mã. */
        int departmentId,
        /** Trường `roomId`: phòng mã. */
        Integer roomId,
        /** Trường `queueDisplay`: hangdoi `Display`. */
        String queueDisplay,
        /** Trường `queueNumber`: hangdoi so. */
        Integer queueNumber,
        /** Trường `status`: trạng thái. */
        TrangThaiLuotKham status,
        /** Trường `reasonForVisit`: lý do cho luotkham. */
        String reasonForVisit,
        /** Trường `clinicalProcess`: lamsang xử lý. */
        String clinicalProcess,
        /** Trường `personalHistory`: `personal` tiensu. */
        String personalHistory,
        /** Trường `familyHistory`: `family` tiensu. */
        String familyHistory,
        /** Trường `generalExam`: `general` kham. */
        String generalExam,
        /** Trường `organExam`: `organ` kham. */
        String organExam,
        /** Trường `summary`: tonghop. */
        String summary,
        /** Trường `treatmentPlan`: dieutri `Plan`. */
        String treatmentPlan,
        /** Trường `doctorAdvice`: bacsi `Advice`. */
        String doctorAdvice,
        /** Trường `treatmentOutcome`: dieutri kết quả. */
        KetQuaDieuTri treatmentOutcome,
        HuongXuTri disposal,
        /** Trường `followUpDate`: theo tiep ngày. */
        LocalDate followUpDate,
        /** Trường `emrLocked`: emr dakhoa. */
        boolean emrLocked,
        /** Trường `emrLockedAt`: emr dakhoa luc. */
        LocalDateTime emrLockedAt,
        /** Trường `emrLockedBy`: emr dakhoa theo. */
        Integer emrLockedBy,
        /** Trường `emrSignature`: emr `Signature`. */
        String emrSignature,
        /** Trường `startedAt`: dabatdau luc. */
        LocalDateTime startedAt,
        /** Trường `completedAt`: dahoantat luc. */
        LocalDateTime completedAt) {
}
