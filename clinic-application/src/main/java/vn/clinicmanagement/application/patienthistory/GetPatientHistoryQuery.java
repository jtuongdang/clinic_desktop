package vn.clinicmanagement.application.patienthistory;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;
import vn.clinicmanagement.domain.patient.TienSuBenh;

import java.util.List;

/** Patient history read request bound to the authenticated operation contexts. */
public record GetPatientHistoryQuery(
        int patientId,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Query<List<TienSuBenh>> {
}
