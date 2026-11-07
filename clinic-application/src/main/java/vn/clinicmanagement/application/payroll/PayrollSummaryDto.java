package vn.clinicmanagement.application.payroll;

import java.math.BigDecimal;
import java.util.List;

public record PayrollSummaryDto(
        int periodId,
        String periodName,
        int month,
        int year,
        boolean locked,
        List<PayrollLineDto> payrollLines,
        BigDecimal totalNetSalary) {
    public PayrollSummaryDto {
        payrollLines = List.copyOf(payrollLines);
    }
}
