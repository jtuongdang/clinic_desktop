package vn.clinicmanagement.application.billing.refund;

import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecordHoanTraBienNhan(
        int refundId,
        int invoiceId,
        BigDecimal refundedAmount,
        BigDecimal netPaidAmount,
        BigDecimal totalRefundedAmount,
        BigDecimal pendingRefundAmount,
        BigDecimal remainingAmount,
        TrangThaiHoaDon invoiceStatus,
        TrangThaiCongNo debtStatus,
        LocalDateTime refundTime
) {}
