package vn.clinicmanagement.application.cls;

import java.time.LocalDateTime;

public record RecordServiceResultReceipt(
        int orderId,
        int visitId,
        String status,
        int performedBy,
        int resultItemCount,
        LocalDateTime performedAt,
        LocalDateTime returnedAt
) {}
