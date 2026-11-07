package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.application.common.Query;

public record GetEmployeesQuery(Boolean active) implements Query<java.util.List<EmployeeSummaryDto>> {}
