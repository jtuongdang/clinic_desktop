package vn.clinicmanagement.application.billing;

import java.math.BigDecimal;

public record InvoiceLineDto(
        int id,
        String itemType,
        int sourceId,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
