package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BienNhanHoanTraDuLieu(
        int id,
        int invoiceId,
        BigDecimal amount,
        LocalDateTime refundTime,
        PhuongThucThanhToan paymentMethod,
        int cashierId,
        String reason
) {}
