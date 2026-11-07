package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceDto(
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
        List<InvoiceLineDto> lines,
        List<ReceiptDto> receipts
) {}
