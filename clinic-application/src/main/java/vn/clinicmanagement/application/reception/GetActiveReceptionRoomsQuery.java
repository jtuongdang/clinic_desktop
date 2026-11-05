package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.common.Query;
import vn.clinicmanagement.domain.facility.PhongKham;

import java.util.List;

/** Protected active-room picker request. */
public record GetActiveReceptionRoomsQuery(AuthorizationContext authorizationContext,
                                           AuditActorContext auditActorContext)
        implements Query<List<PhongKham>> {}
