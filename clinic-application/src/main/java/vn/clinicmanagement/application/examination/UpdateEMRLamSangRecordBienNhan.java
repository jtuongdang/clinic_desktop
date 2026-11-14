package vn.clinicmanagement.application.examination;

import java.time.LocalDateTime;

public record UpdateEMRLamSangRecordBienNhan(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `emrLocked`: emr dakhoa. */
        boolean emrLocked,
        /** Trường `updatedAt`: `updated` luc. */
        LocalDateTime updatedAt
) {}
