package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;

public record CalculateTienLuongCommand(
        /** Trường `periodId`: ky mã. */
        int periodId,
        /** Trường `employeeId`: nhân viên mã. */
        int employeeId,
        /** Trường `baseSalary`: cơ sở `Salary`. */
        BigDecimal baseSalary,
        BigDecimal salaryCoefficient,
        BigDecimal totalAllowances,
        /** Trường `totalExaminationFees`: `total` khám bệnh `Fees`. */
        BigDecimal totalExaminationFees,
        BigDecimal totalDeductions
) implements Command<CalculateTienLuongBienNhan> {}
