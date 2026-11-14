package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecordThanhToanBienNhan(
        /** Trường `receiptId`: biên nhận mã. */
        int receiptId,
        /** Trường `invoiceId`: hóa đơn mã. */
        int invoiceId,
        /** Trường `amountPaidThisTime`: `amount` `Paid` `This` thời gian. */
        BigDecimal amountPaidThisTime,
        BigDecimal totalPaidSoFar,
        /** Trường `remainingDebt`: `remaining` congno. */
        BigDecimal remainingDebt,
        /** Trường `invoiceStatus`: hóa đơn trạng thái. */
        TrangThaiHoaDon invoiceStatus,
        /** Trường `debtStatus`: congno trạng thái. */
        TrangThaiCongNo debtStatus,
        /** Trường `paymentTime`: thanhtoan thời gian. */
        LocalDateTime paymentTime
) {}
