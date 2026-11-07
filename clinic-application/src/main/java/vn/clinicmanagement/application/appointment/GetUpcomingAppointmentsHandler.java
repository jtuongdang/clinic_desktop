package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.ApplicationFailureDiagnostics;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.appointment.LichHen;
import vn.clinicmanagement.domain.repositories.ILichHenRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Handles GetUpcomingAppointmentsQuery with authorization validation,
 * patient input validation, mapping to AppointmentSummaryDto, and audit logging.
 */
public final class GetUpcomingAppointmentsHandler
        implements QueryHandler<GetUpcomingAppointmentsQuery, List<AppointmentSummaryDto>> {

    private static final Set<String> ALLOWED_ROLES = Set.of("RECEPTIONIST", "DOCTOR", "ADMIN");

    private final ILichHenRepository appointmentRepository;
    private final AuditWritePort auditWritePort;

    public GetUpcomingAppointmentsHandler(
            ILichHenRepository appointmentRepository,
            AuditWritePort auditWritePort) {
        this.appointmentRepository = Objects.requireNonNull(appointmentRepository, "appointmentRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public Result<List<AppointmentSummaryDto>> handle(GetUpcomingAppointmentsQuery query) {
        if (query == null || query.patientId() <= 0) {
            return Result.failure("VALIDATION_ERROR", "Mã bệnh nhân không hợp lệ.");
        }

        AuthorizationContext context = query.sessionContext() != null
                ? query.sessionContext()
                : query.authorizationContext();

        if (!isAuthorized(context, query.auditActorContext())) {
            emitDeniedAudit(context, query.auditActorContext());
            return Result.failure("AUTH_ACCESS_DENIED", "Không có quyền xem danh sách lịch hẹn.");
        }

        try {
            List<LichHen> appointments = appointmentRepository.getUpcomingAppointments(query.patientId());
            List<AppointmentSummaryDto> dtos = (appointments != null ? appointments : List.<LichHen>of())
                    .stream()
                    .map(this::toSummaryDto)
                    .toList();

            emitSuccessAudit(context, query.auditActorContext(), query.patientId());
            return Result.success(dtos);
        } catch (RuntimeException ex) {
            ApplicationFailureDiagnostics.logUnexpectedFailure(
                    "GET_UPCOMING_APPOINTMENTS",
                    "REPOSITORY_FAILURE",
                    correlationId(query),
                    ex);
            return Result.failure("GET_UPCOMING_APPOINTMENTS_FAILED", "Không thể tải danh sách lịch hẹn.");
        }
    }

    private boolean isAuthorized(AuthorizationContext context, AuditActorContext actor) {
        if (context == null || !context.isValid()) {
            return false;
        }
        String normalizedRole = normalizeRoleCode(context.roleCode());
        if (normalizedRole == null || !ALLOWED_ROLES.contains(normalizedRole)) {
            return false;
        }
        if (actor != null) {
            if (actor.employeeId() != context.employeeId()
                    || actor.roleId() != context.roleId()
                    || !Objects.equals(normalizeRoleCode(actor.roleCode()), normalizedRole)
                    || actor.facilityId() != context.facilityId()
                    || actor.departmentId() != context.departmentId()) {
                return false;
            }
        }
        return true;
    }

    private void emitDeniedAudit(AuthorizationContext context, AuditActorContext actor) {
        if (auditWritePort == null) return;
        try {
            Integer actorId = null;
            if (context != null && context.employeeId() > 0) {
                actorId = context.employeeId();
            } else if (actor != null && actor.employeeId() > 0) {
                actorId = actor.employeeId();
            }

            Integer roleId = null;
            if (context != null && context.roleId() > 0) {
                roleId = context.roleId();
            } else if (actor != null && actor.roleId() > 0) {
                roleId = actor.roleId();
            }

            String roleCode = (context != null) ? normalizeRoleCode(context.roleCode())
                    : (actor != null ? normalizeRoleCode(actor.roleCode()) : null);
            if (roleId == null || roleCode == null) {
                roleId = null;
                roleCode = null;
            }

            Integer facilityId = null;
            if (context != null && context.facilityId() > 0) {
                facilityId = context.facilityId();
            } else if (actor != null && actor.facilityId() > 0) {
                facilityId = actor.facilityId();
            }

            Integer departmentId = null;
            if (context != null && context.departmentId() > 0) {
                departmentId = context.departmentId();
            } else if (actor != null && actor.departmentId() > 0) {
                departmentId = actor.departmentId();
            }
            String workstationId = (actor != null) ? actor.workstationId() : null;
            UUID correlationId = (actor != null && actor.correlationId() != null) ? actor.correlationId()
                    : (context != null && context.sessionId() != null ? context.sessionId() : UUID.randomUUID());

            SystemAuditEntry entry = new SystemAuditEntry(
                    actorId,
                    roleId,
                    roleCode,
                    facilityId,
                    departmentId,
                    AuditActionCode.AUTH_ACCESS_DENIED,
                    null,
                    null,
                    AuditOutcome.DENIED,
                    AuditReasonCode.AUTH_FORBIDDEN,
                    Instant.now(),
                    workstationId,
                    correlationId
            );
            auditWritePort.appendIndependently(entry);
        } catch (RuntimeException ignored) { 
            // Safe failure if audit emission fails
        }
    }

    private void emitSuccessAudit(AuthorizationContext context, AuditActorContext actor, int patientId) {
        if (auditWritePort == null || context == null) return;
        String workstationId = (actor != null) ? actor.workstationId() : null;
        UUID correlationId = (actor != null && actor.correlationId() != null) ? actor.correlationId()
                : (context.sessionId() != null ? context.sessionId() : UUID.randomUUID());

        SystemAuditEntry entry = new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                normalizeRoleCode(context.roleCode()),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.PATIENT_VIEW,
                AuditResourceType.PATIENT,
                patientId,
                AuditOutcome.SUCCESS,
                null,
                Instant.now(),
                workstationId,
                correlationId
        );
        auditWritePort.appendIndependently(entry);
    }

    private AppointmentSummaryDto toSummaryDto(LichHen entity) {
        int appointmentId = (entity.getId() != null) ? entity.getId() : 0;
        int doctorId = entity.getMaBacSi();
        int facilityId = entity.getMaCoSo();
        Integer departmentId = entity.getMaKhoa();
        LocalDateTime startTime = entity.getThoiGianBatDau();
        LocalDateTime endTime = entity.getThoiGianKetThuc();
        String status = (entity.getTrangThai() != null) ? entity.getTrangThai().name() : null;
        String reason = entity.getLyDo();
        String bookingSource = entity.getNguonDat();

        return new AppointmentSummaryDto(
                appointmentId,
                doctorId,
                facilityId,
                departmentId,
                startTime,
                endTime,
                status,
                reason,
                bookingSource
        );
    }

    private static String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) return null;
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }

    private static String correlationId(GetUpcomingAppointmentsQuery query) {
        if (query == null) return null;
        if (query.auditActorContext() != null && query.auditActorContext().correlationId() != null) {
            return query.auditActorContext().correlationId().toString();
        }
        if (query.sessionContext() != null && query.sessionContext().sessionId() != null) {
            return query.sessionContext().sessionId().toString();
        }
        return null;
    }
}
