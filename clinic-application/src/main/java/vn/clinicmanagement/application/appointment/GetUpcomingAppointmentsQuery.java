package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;

import java.util.List;

/**
 * Query to retrieve upcoming appointments for a patient.
 */
public record GetUpcomingAppointmentsQuery(
        int patientId,
        AuthorizationContext sessionContext,
        AuditActorContext auditActorContext) implements Query<List<AppointmentSummaryDto>> {

    public GetUpcomingAppointmentsQuery(int patientId, AuthorizationContext sessionContext) {
        this(patientId, sessionContext, null);
    }

    public GetUpcomingAppointmentsQuery(int patientId) {
        this(patientId, null, null);
    }

    public AuthorizationContext authorizationContext() {
        return sessionContext;
    }

    public AuditActorContext auditContext() {
        return auditActorContext;
    }
}
