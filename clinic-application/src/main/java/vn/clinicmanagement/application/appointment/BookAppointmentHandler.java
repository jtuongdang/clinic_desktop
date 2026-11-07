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
import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;
import vn.clinicmanagement.domain.repositories.ILichHenRepository;

import java.time.Instant;

public final class BookAppointmentHandler implements CommandHandler<BookAppointmentCommand, AppointmentReceipt> {
    private final ILichHenRepository appointments;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public BookAppointmentHandler(ILichHenRepository appointments, TransactionBoundary transactions,
                                  AuditWritePort audit) {
        this.appointments = appointments;
        this.transactions = transactions;
        this.audit = audit;
    }

    @Override
    public Result<AppointmentReceipt> handle(BookAppointmentCommand command) {
        if (command == null || command.patientId() <= 0 || command.doctorId() <= 0 || command.facilityId() <= 0
                || command.startTime() == null || command.endTime() == null) {
            return Result.failure("VALIDATION_ERROR", "Thông tin đặt lịch không hợp lệ.");
        }
        if (!command.startTime().isBefore(command.endTime())) {
            return Result.failure("VALIDATION_ERROR", "Khung giờ hẹn không hợp lệ.");
        }

        AuthorizationContext context = command.authorizationContext();
        AuditActorContext actor = command.auditActorContext();
        if (!isAuthorized(context, actor, command.facilityId())) {
            return deny(context, actor);
        }

        return transactions.inTransaction(() -> {
            if (appointments.countAppointmentsInSlot(command.doctorId(), command.startTime(), command.endTime()) > 0) {
                return Result.failure("SLOT_UNAVAILABLE",
                        "Bác sĩ đã có lịch hẹn trong khung giờ này. Không được phép đặt trùng.");
            }

            LichHen appointment = new LichHen(command.patientId(), command.doctorId(), command.facilityId(),
                    command.departmentId(), command.startTime(), command.endTime(), command.reason(),
                    command.bookingSource(), context.employeeId());
            appointment.setTrangThai(TrangThaiLichHen.CHO_XAC_NHAN);
            appointments.add(appointment);
            appendSuccessAudit(actor, context, command.patientId());
            return Result.success(new AppointmentReceipt(appointment.getId(), appointment.getTrangThai()));
        });
    }

    private static boolean isAuthorized(AuthorizationContext context, AuditActorContext actor, int facilityId) {
        return context != null && context.isValid()
                && (context.roleCode().equals("RECEPTIONIST") || context.roleCode().equals("ADMIN"))
                && context.facilityId() == facilityId
                && actor != null
                && actor.employeeId() == context.employeeId()
                && actor.roleId() == context.roleId()
                && actor.roleCode().equals(context.roleCode())
                && actor.facilityId() == context.facilityId()
                && actor.departmentId() == context.departmentId();
    }

    private Result<AppointmentReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
        if (context != null && context.isValid() && actor != null) {
            audit.appendIndependently(new SystemAuditEntry(
                    context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                    context.departmentId(), AuditActionCode.AUTH_ACCESS_DENIED, null, null,
                    AuditOutcome.DENIED, AuditReasonCode.AUTH_FORBIDDEN, Instant.now(),
                    actor.workstationId(), actor.correlationId()));
        }
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép đặt lịch trong phiên hiện tại.");
    }

    private void appendSuccessAudit(AuditActorContext actor, AuthorizationContext context, int patientId) {
        audit.appendInCurrentTransaction(new SystemAuditEntry(
                context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                context.departmentId(), AuditActionCode.VISIT_REGISTERED, AuditResourceType.PATIENT, patientId,
                AuditOutcome.SUCCESS, null, Instant.now(), actor.workstationId(), actor.correlationId()));
    }
}
