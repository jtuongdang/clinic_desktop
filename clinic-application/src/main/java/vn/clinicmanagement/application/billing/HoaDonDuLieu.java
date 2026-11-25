package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record HoaDonDuLieu(
        int id,
        String invoiceNumber,
        int visitId,
        int patientId,
        int facilityId,
        LocalDateTime invoiceDate,
        BigDecimal totalAmount,
        BigDecimal insurancePaidAmount,
        BigDecimal advanceAmount,
        BigDecimal discountAmount,
        BigDecimal discountPercentage,
        String discountReason,
        BigDecimal patientPaidAmount,
        BigDecimal amountPaid,
        BigDecimal remainingAmount,
        TrangThaiHoaDon status,
        String notes,
        List<HoaDonDongDuLieu> lines,
        List<BienNhanDuLieu> receipts,
        BigDecimal serviceDiscountAmount,
        BigDecimal refundAmount,
        BigDecimal pendingRefundAmount,
        List<BienNhanHoanTraDuLieu> refunds
) {
    public HoaDonDuLieu(
            int id,
            String invoiceNumber,
            int visitId,
            int patientId,
            int facilityId,
            LocalDateTime invoiceDate,
            BigDecimal totalAmount,
            BigDecimal insurancePaidAmount,
            BigDecimal advanceAmount,
            BigDecimal discountAmount,
            BigDecimal discountPercentage,
            String discountReason,
            BigDecimal patientPaidAmount,
            BigDecimal amountPaid,
            BigDecimal remainingAmount,
            TrangThaiHoaDon status,
            String notes,
            List<HoaDonDongDuLieu> lines,
            List<BienNhanDuLieu> receipts
    ) {
        this(id, invoiceNumber, visitId, patientId, facilityId, invoiceDate,
                totalAmount, insurancePaidAmount, advanceAmount, discountAmount, discountPercentage, discountReason,
                patientPaidAmount, amountPaid, remainingAmount, status, notes, lines, receipts,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                (amountPaid != null && patientPaidAmount != null)
                        ? amountPaid.subtract(patientPaidAmount).max(BigDecimal.ZERO)
                        : BigDecimal.ZERO,
                List.of());
    }
}
