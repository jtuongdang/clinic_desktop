package vn.clinicmanagement.application.cls;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderServiceReceipt(
        int orderId,
        int visitId,
        int serviceId,
        String serviceName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String status,
        LocalDateTime orderedAt
) {}
