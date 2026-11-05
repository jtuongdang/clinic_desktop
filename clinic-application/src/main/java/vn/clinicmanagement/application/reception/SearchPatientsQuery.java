package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.common.Query;

public record SearchPatientsQuery(String keyword) implements Query<java.util.List<PatientSearchItem>> {}
