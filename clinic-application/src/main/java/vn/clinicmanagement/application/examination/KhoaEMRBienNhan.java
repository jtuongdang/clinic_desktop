package vn.clinicmanagement.application.examination;

import java.time.LocalDateTime;

public record KhoaEMRBienNhan(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `emrLocked`: emr dakhoa. */
        boolean emrLocked,
        /** Trường `lockedAt`: dakhoa luc. */
        LocalDateTime lockedAt,
        /** Trường `lockedBy`: dakhoa theo. */
        Integer lockedBy,
        String signature) {
}
