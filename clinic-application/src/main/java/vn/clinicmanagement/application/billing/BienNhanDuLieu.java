package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BienNhanDuLieu(
        /** Trường `id`: mã. */
        int id,
        /** Trường `invoiceId`: hóa đơn mã. */
        int invoiceId,
        BigDecimal amount,
        /** Trường `paymentTime`: thanhtoan thời gian. */
        LocalDateTime paymentTime,
        /** Trường `paymentMethod`: thanhtoan `Method`. */
        PhuongThucThanhToan paymentMethod,
        /** Trường `cashierId`: `cashier` mã. */
        int cashierId,
        String notes
) {}
