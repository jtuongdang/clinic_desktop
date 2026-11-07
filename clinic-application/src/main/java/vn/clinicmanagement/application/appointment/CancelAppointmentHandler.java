package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.appointment.LichHen;
import vn.clinicmanagement.domain.repositories.ILichHenRepository;

import java.time.Instant;

public final class CancelAppointmentHandler
        implements CommandHandler<CancelAppointmentCommand, CancelAppointmentReceipt> {
    private final ILichHenRepository appointments;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public CancelAppointmentHandler(ILichHenRepository appointments, TransactionBoundary transactions,
                                    AuditWritePort audit) {
        this.appointments = appointments;
        this.transactions = transactions;
        this.audit = audit;
    }

    @Override
    public Result<CancelAppointmentReceipt> handle(CancelAppointmentCommand command) {
        if (command == null || command.appointmentId() <= 0) {
            return Result.failure("VALIDATION_ERROR", "Thông tin hủy lịch hẹn không hợp lệ.");
        }
        if (command.cancellationReason() == null || command.cancellationReason().isBlank()) {
            return Result.failure("VALIDATION_ERROR", "Lý do hủy không được để trống.");
        }

        AuthorizationContext context = command.authorizationContext();
        AuditActorContext actor = command.auditActorContext();
        if (!isAuthorized(context)) {
            return deny(context, actor);
        }

        return transactions.inTransaction(() -> {
            var appointmentResult = appointments.getById(command.appointmentId());
            if (appointmentResult.isEmpty()) {
                return Result.failure("NOT_FOUND", "Không tìm thấy lịch hẹn.");
            }

            LichHen appointment = appointmentResult.get();
            try {
                appointment.huyLichHen(command.cancellationReason());
            } catch (IllegalStateException ex) {
                return Result.failure("INVALID_STATE", ex.getMessage());
            }

            appointments.update(appointment);
            appendSuccessAudit(actor, context, appointment);
            return Result.success(new CancelAppointmentReceipt(appointment.getId()));
        });
    }

    private static boolean isAuthorized(AuthorizationContext context) {
        return context != null && context.isValid()
                && ("RECEPTIONIST".equals(context.roleCode()) || "ADMIN".equals(context.roleCode()));
    }

    private Result<CancelAppointmentReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
        if (context != null && context.isValid()) {
            audit.appendIndependently(new SystemAuditEntry(
                    context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                    context.departmentId(), AuditActionCode.AUTH_ACCESS_DENIED, null, null,
                    AuditOutcome.DENIED, AuditReasonCode.AUTH_FORBIDDEN, Instant.now(),
                    actor == null ? null : actor.workstationId(),
                    actor == null ? context.sessionId() : actor.correlationId()));
        } else if (actor != null) {
            audit.appendIndependently(new SystemAuditEntry(
                    actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(),
                    actor.departmentId(), AuditActionCode.AUTH_ACCESS_DENIED, null, null,
                    AuditOutcome.DENIED, AuditReasonCode.AUTH_FORBIDDEN, Instant.now(),
                    actor.workstationId(), actor.correlationId()));
        }
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép hủy lịch hẹn trong phiên hiện tại.");
    }

    private void appendSuccessAudit(AuditActorContext actor, AuthorizationContext context, LichHen appointment) {
        audit.appendInCurrentTransaction(new SystemAuditEntry(
                context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                context.departmentId(), AuditActionCode.VISIT_REGISTERED, AuditResourceType.PATIENT,
                appointment.getMaBenhNhan(), AuditOutcome.SUCCESS, null, Instant.now(),
                actor == null ? null : actor.workstationId(),
                actor == null ? context.sessionId() : actor.correlationId()));
    }
}
