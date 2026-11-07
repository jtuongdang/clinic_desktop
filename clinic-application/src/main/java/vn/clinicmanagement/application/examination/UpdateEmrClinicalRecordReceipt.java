package vn.clinicmanagement.application.examination;

import java.time.LocalDateTime;

public record UpdateEmrClinicalRecordReceipt(
        int visitId,
        boolean emrLocked,
        LocalDateTime updatedAt
) {}
