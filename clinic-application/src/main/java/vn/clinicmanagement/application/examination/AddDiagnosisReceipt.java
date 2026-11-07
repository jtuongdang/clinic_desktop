package vn.clinicmanagement.application.examination;

import java.time.LocalDateTime;

public record AddDiagnosisReceipt(
        int visitId,
        String chanDoanText,
        boolean laChinh,
        String loaiChanDoan,
        LocalDateTime addedAt) {
}
