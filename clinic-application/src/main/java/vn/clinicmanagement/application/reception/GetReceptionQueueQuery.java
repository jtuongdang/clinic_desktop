package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;

/** Protected queue and room-summary request. */
public record GetReceptionQueueQuery(LocalDate date, AuthorizationContext authorizationContext,
                                     AuditActorContext auditActorContext) implements Query<ReceptionQueueSnapshot> {
    /** Legacy call sites fail closed because they carry no authenticated operation contexts. */
    @Deprecated
    public GetReceptionQueueQuery(LocalDate date) { this(date, null, null); }
}
