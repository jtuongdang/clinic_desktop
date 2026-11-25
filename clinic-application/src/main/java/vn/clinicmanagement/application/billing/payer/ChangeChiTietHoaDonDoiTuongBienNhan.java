package vn.clinicmanagement.application.billing.payer;

import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;

public record ChangeChiTietHoaDonDoiTuongBienNhan(
        int invoiceId,
        int lineId,
        String previousPayerType,
        String currentPayerType,
        BigDecimal totalAmount,
        BigDecimal insurancePaidAmount,
        BigDecimal serviceDiscountAmount,
        BigDecimal patientPaidAmount,
        BigDecimal amountPaid,
        BigDecimal remainingAmount,
        BigDecimal pendingRefundAmount,
        TrangThaiHoaDon invoiceStatus
) {}
