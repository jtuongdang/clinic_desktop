package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BenhNhanCongNoDuLieu(
        /** Trường `id`: mã. */
        int id,
        /** Trường `invoiceId`: hóa đơn mã. */
        int invoiceId,
        /** Trường `invoiceNumber`: hóa đơn so. */
        String invoiceNumber,
        /** Trường `patientId`: bệnh nhân mã. */
        int patientId,
        /** Trường `totalDebtAmount`: `total` congno `Amount`. */
        BigDecimal totalDebtAmount,
        BigDecimal paidAmount,
        /** Trường `remainingDebt`: `remaining` congno. */
        BigDecimal remainingDebt,
        /** Trường `dueDate`: `due` ngày. */
        LocalDate dueDate,
        /** Trường `status`: trạng thái. */
        TrangThaiCongNo status
) {}
