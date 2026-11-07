package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;

import java.util.List;

public record GetPatientDebtQuery(
        int patientId,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext
) implements Query<List<PatientDebtDto>> {}
