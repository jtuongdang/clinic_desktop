package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateInvoiceReceipt(
        int invoiceId,
        String invoiceNumber,
        int visitId,
        int patientId,
        int facilityId,
        LocalDateTime invoiceDate,
        BigDecimal totalAmount,
        BigDecimal insurancePaidAmount,
        BigDecimal advanceAmount,
        BigDecimal discountAmount,
        BigDecimal patientPaidAmount,
        BigDecimal amountPaid,
        TrangThaiHoaDon status
) {}
