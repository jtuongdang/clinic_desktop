package vn.clinicmanagement.application.cls.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record YeuCauDichVuBienNhan(
        int orderId,
        int visitId,
        int serviceId,
        String serviceName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String status,
        LocalDateTime orderedAt
) {}
