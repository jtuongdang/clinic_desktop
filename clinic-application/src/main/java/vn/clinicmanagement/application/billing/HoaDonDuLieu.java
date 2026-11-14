package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record HoaDonDuLieu(
        /** Trường `id`: mã. */
        int id,
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
        BigDecimal discountPercentage,
        /** Trường `discountReason`: `discount` lý do. */
        String discountReason,
        /** Trường `patientPaidAmount`: bệnh nhân `Paid` `Amount`. */
        BigDecimal patientPaidAmount,
        BigDecimal amountPaid,
        BigDecimal remainingAmount,
        /** Trường `status`: trạng thái. */
        TrangThaiHoaDon status,
        String notes,
        List<HoaDonDongDuLieu> lines,
        List<BienNhanDuLieu> receipts
) {}
