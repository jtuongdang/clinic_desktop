package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateHoaDonBienNhan(
        /** Trường `invoiceId`: hóa đơn mã. */
        int invoiceId,
        /** Trường `invoiceNumber`: hóa đơn so. */
        String invoiceNumber,
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `patientId`: bệnh nhân mã. */
        int patientId,
        /** Trường `facilityId`: cơ sở mã. */
        int facilityId,
        /** Trường `invoiceDate`: hóa đơn ngày. */
        LocalDateTime invoiceDate,
        BigDecimal totalAmount,
        BigDecimal insurancePaidAmount,
        BigDecimal advanceAmount,
        BigDecimal discountAmount,
        /** Trường `patientPaidAmount`: bệnh nhân `Paid` `Amount`. */
        BigDecimal patientPaidAmount,
        BigDecimal amountPaid,
        /** Trường `status`: trạng thái. */
        TrangThaiHoaDon status
) {}
