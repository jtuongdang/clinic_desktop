package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptDto(
        int id,
        int invoiceId,
        BigDecimal amount,
        LocalDateTime paymentTime,
        PhuongThucThanhToan paymentMethod,
        int cashierId,
        String notes
) {}
