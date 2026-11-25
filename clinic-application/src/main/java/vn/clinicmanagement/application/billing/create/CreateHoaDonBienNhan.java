package vn.clinicmanagement.application.billing.create;

import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateHoaDonBienNhan(
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
