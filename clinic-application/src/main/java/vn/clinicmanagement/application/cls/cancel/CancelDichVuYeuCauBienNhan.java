package vn.clinicmanagement.application.cls.cancel;

import java.time.LocalDateTime;

public record CancelDichVuYeuCauBienNhan(
        int orderId,
        int visitId,
        String status,
        int cancelledBy,
        String reason,
        LocalDateTime cancelledAt
) {}
