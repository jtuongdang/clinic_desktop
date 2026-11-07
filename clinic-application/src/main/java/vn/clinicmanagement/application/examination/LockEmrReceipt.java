package vn.clinicmanagement.application.examination;

import java.time.LocalDateTime;

public record LockEmrReceipt(
        int visitId,
        boolean emrLocked,
        LocalDateTime lockedAt,
        Integer lockedBy,
        String signature) {
}
