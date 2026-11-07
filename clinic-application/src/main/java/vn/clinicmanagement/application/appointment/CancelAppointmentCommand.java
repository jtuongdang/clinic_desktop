package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

public record CancelAppointmentCommand(
        int appointmentId,
        String cancellationReason,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<CancelAppointmentReceipt> {
}
