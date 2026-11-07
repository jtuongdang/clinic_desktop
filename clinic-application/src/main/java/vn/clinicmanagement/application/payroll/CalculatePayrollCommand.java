package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;

public record CalculatePayrollCommand(
        int periodId,
        int employeeId,
        BigDecimal baseSalary,
        BigDecimal salaryCoefficient,
        BigDecimal totalAllowances,
        BigDecimal totalExaminationFees,
        BigDecimal totalDeductions
) implements Command<CalculatePayrollReceipt> {}
