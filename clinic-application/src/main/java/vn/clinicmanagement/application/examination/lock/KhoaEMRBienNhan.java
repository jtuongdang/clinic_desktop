package vn.clinicmanagement.application.examination.lock;

import java.time.LocalDateTime;

public record KhoaEMRBienNhan(
        int visitId,
        boolean emrLocked,
        LocalDateTime lockedAt,
        Integer lockedBy,
        String signature) {
}
