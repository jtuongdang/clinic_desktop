package vn.clinicmanagement.application.payroll;

import java.math.BigDecimal;

public record TienLuongDongDuLieu(
        /** Trường `payrollId`: tienluong mã. */
        int payrollId,
        /** Trường `employeeId`: nhân viên mã. */
        int employeeId,
        /** Trường `baseSalary`: cơ sở `Salary`. */
        BigDecimal baseSalary,
        BigDecimal salaryCoefficient,
        BigDecimal totalAllowances,
        /** Trường `totalExaminationFees`: `total` khám bệnh `Fees`. */
        BigDecimal totalExaminationFees,
        BigDecimal totalDeductions,
        BigDecimal netSalary) {}
