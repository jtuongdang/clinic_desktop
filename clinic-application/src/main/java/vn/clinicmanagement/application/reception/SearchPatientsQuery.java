package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.common.Query;

import java.util.List;

/** Protected patient search request. Contexts are captured for this operation. */
public record SearchPatientsQuery(String keyword, AuthorizationContext authorizationContext,
                                  AuditActorContext auditActorContext) implements Query<List<PatientSearchItem>> {
    /** Legacy call sites fail closed because they carry no authenticated operation contexts. */
    @Deprecated
    public SearchPatientsQuery(String keyword) { this(keyword, null, null); }
}
