package vn.clinicmanagement.application.examination.update;

import java.time.LocalDateTime;

public record UpdateEMRLamSangRecordBienNhan(
        int visitId,
        boolean emrLocked,
        LocalDateTime updatedAt
) {}
