package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;
import java.util.List;

/** Protected active doctor team-queue request for one selected work scope. */
public record GetDoctorQueueQuery(LocalDate date, AuthorizationContext authorizationContext,
                                  AuditActorContext auditActorContext) implements Query<List<ReceptionQueueItem>> {}
