package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;

public record GetVisitDetailQuery(
        int visitId,
        AuthorizationContext sessionContext,
        AuditActorContext auditActorContext) implements Query<VisitDetailDto> {

    public GetVisitDetailQuery(int visitId, AuthorizationContext sessionContext) {
        this(visitId, sessionContext, null);
    }

    public GetVisitDetailQuery(int visitId) {
        this(visitId, null, null);
    }
}
