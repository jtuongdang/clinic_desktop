package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.time.LocalDateTime;

public record BookAppointmentCommand(
        int patientId,
        int doctorId,
        int facilityId,
        int departmentId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason,
        String bookingSource,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<AppointmentReceipt> {
}
