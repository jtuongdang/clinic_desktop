package vn.clinicmanagement.application.cls;

import java.time.LocalDateTime;

public record CancelServiceOrderReceipt(
        int orderId,
        int visitId,
        String status,
        int cancelledBy,
        String reason,
        LocalDateTime cancelledAt
) {}
