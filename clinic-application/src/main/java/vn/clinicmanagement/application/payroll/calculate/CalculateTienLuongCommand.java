package vn.clinicmanagement.application.payroll.calculate;

import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;

public record CalculateTienLuongCommand(
        int periodId,
        int employeeId,
        BigDecimal baseSalary,
        BigDecimal salaryCoefficient,
        BigDecimal totalAllowances,
        BigDecimal totalExaminationFees,
        BigDecimal totalDeductions
) implements Command<CalculateTienLuongBienNhan> {}
