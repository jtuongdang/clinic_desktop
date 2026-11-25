package vn.clinicmanagement.application.payroll.calculate;

import java.math.BigDecimal;

public record CalculateTienLuongBienNhan(
        int payrollId,
        int periodId,
        int employeeId,
        BigDecimal baseSalary,
        BigDecimal salaryCoefficient,
        BigDecimal totalAllowances,
        BigDecimal totalExaminationFees,
        BigDecimal totalDeductions,
        BigDecimal netSalary) {}
