package vn.clinicmanagement.application.payroll;

import java.math.BigDecimal;

public record TienLuongDongDuLieu(
        int payrollId,
        int employeeId,
        BigDecimal baseSalary,
        BigDecimal salaryCoefficient,
        BigDecimal totalAllowances,
        BigDecimal totalExaminationFees,
        BigDecimal totalDeductions,
        BigDecimal netSalary) {}
