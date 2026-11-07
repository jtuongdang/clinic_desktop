package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.application.common.Query;

public record GetPayrollByPeriodQuery(Integer periodId, Integer month, Integer year)
        implements Query<PayrollSummaryDto> {}
