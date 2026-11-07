package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecordPaymentReceipt(
        int receiptId,
        int invoiceId,
        BigDecimal amountPaidThisTime,
        BigDecimal totalPaidSoFar,
        BigDecimal remainingDebt,
        TrangThaiHoaDon invoiceStatus,
        TrangThaiCongNo debtStatus,
        LocalDateTime paymentTime
) {}
