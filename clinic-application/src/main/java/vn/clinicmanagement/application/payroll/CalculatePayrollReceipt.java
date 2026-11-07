package vn.clinicmanagement.application.payroll;

import java.math.BigDecimal;

public record CalculatePayrollReceipt(
        int payrollId,
        int periodId,
        int employeeId,
        BigDecimal baseSalary,
        BigDecimal salaryCoefficient,
        BigDecimal totalAllowances,
        BigDecimal totalExaminationFees,
        BigDecimal totalDeductions,
        BigDecimal netSalary) {}
