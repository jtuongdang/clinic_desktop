package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PatientDebtDto(
        int id,
        int invoiceId,
        String invoiceNumber,
        int patientId,
        BigDecimal totalDebtAmount,
        BigDecimal paidAmount,
        BigDecimal remainingDebt,
        LocalDate dueDate,
        TrangThaiCongNo status
) {}
