package vn.clinicmanagement.application.cls;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ServiceOrderDto(
        int orderId,
        int visitId,
        int serviceId,
        String serviceCode,
        String serviceName,
        String serviceType,
        int doctorId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String status,
        String resultSummary,
        String conclusion,
        String imageAttachmentUrls,
        Integer roomId,
        Integer technicianId,
        LocalDateTime orderedAt,
        LocalDateTime performedAt,
        LocalDateTime returnedAt,
        List<LabResultDto> testResults
) {}
