package vn.clinicmanagement.application.appointment;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
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
public final class GetSapToiLichHenHandler
        implements QueryHandler<GetSapToiLichHenQuery, List<LichHenTongHopDuLieu>> {

    private static final Set<String> ALLOWED_ROLES = Set.of("RECEPTIONIST", "DOCTOR", "ADMIN");

    /** Trường `appointmentRepository`: lịch hẹn kho dữ liệu. */
    private final ILichHenRepository appointmentRepository;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public GetSapToiLichHenHandler(
            ILichHenRepository appointmentRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        this.appointmentRepository = Objects.requireNonNull(appointmentRepository, "appointmentRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public KetQua<List<LichHenTongHopDuLieu>> handle(GetSapToiLichHenQuery query) {
        if (query == null || query.patientId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Mã bệnh nhân không hợp lệ.");
        }

        PhanQuyenNguCanh context = query.sessionContext() != null
                ? query.sessionContext()
                : query.authorizationContext();

        if (!isAuthorized(context, query.auditActorContext())) {
            emitDeniedAudit(context, query.auditActorContext());
            return KetQua.failure("AUTH_ACCESS_DENIED", "Không có quyền xem danh sách lịch hẹn.");
        }

        try {
            List<LichHen> appointments = appointmentRepository.getUpcomingAppointments(query.patientId());
            List<LichHenTongHopDuLieu> dtos = (appointments != null ? appointments : List.<LichHen>of())
                    .stream()
                    .map(this::toSummaryDto)
                    .toList();

            emitSuccessAudit(context, query.auditActorContext(), query.patientId());
            return KetQua.success(dtos);
        } catch (RuntimeException ex) {
            UngDungThatBaiChanDoanLoi.logUnexpectedFailure(
                    "GET_UPCOMING_APPOINTMENTS",
                    "REPOSITORY_FAILURE",
                    correlationId(query),
                    ex);
            return KetQua.failure("GET_UPCOMING_APPOINTMENTS_FAILED", "Không thể tải danh sách lịch hẹn.");
        }
    }

    /** Hàm `isAuthorized`: là `Authorized`. */
    private boolean isAuthorized(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
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

    /** Hàm `emitDeniedAudit`: `emit` tuchoi kiểm toán. */
    private void emitDeniedAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
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

            HeThongKiemToanNhapLieu entry = new HeThongKiemToanNhapLieu(
                    actorId,
                    roleId,
                    roleCode,
                    facilityId,
                    departmentId,
                    KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                    null,
                    null,
                    KiemToanKetQua.DENIED,
                    KiemToanLyDoMa.AUTH_FORBIDDEN,
                    Instant.now(),
                    workstationId,
                    correlationId
            );
            auditWritePort.appendIndependently(entry);
        } catch (RuntimeException ignored) { 
            // Safe failure if audit emission fails
        }
    }

    /** Hàm `emitSuccessAudit`: `emit` thành công kiểm toán. */
    private void emitSuccessAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, int patientId) {
        if (auditWritePort == null || context == null) return;
        String workstationId = (actor != null) ? actor.workstationId() : null;
        UUID correlationId = (actor != null && actor.correlationId() != null) ? actor.correlationId()
                : (context.sessionId() != null ? context.sessionId() : UUID.randomUUID());

        HeThongKiemToanNhapLieu entry = new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                normalizeRoleCode(context.roleCode()),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.PATIENT_VIEW,
                KiemToanTaiNguyenLoai.PATIENT,
                patientId,
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                workstationId,
                correlationId
        );
        auditWritePort.appendIndependently(entry);
    }

    /** Hàm `toSummaryDto`: den tonghop dữ liệu. */
    private LichHenTongHopDuLieu toSummaryDto(LichHen entity) {
        int appointmentId = (entity.getId() != null) ? entity.getId() : 0;
        int doctorId = entity.getMaBacSi();
        int facilityId = entity.getMaCoSo();
        Integer departmentId = entity.getMaKhoa();
        LocalDateTime startTime = entity.getThoiGianBatDau();
        LocalDateTime endTime = entity.getThoiGianKetThuc();
        String status = (entity.getTrangThai() != null) ? entity.getTrangThai().name() : null;
        String reason = entity.getLyDo();
        String bookingSource = entity.getNguonDat();

        return new LichHenTongHopDuLieu(
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

    /** Hàm `normalizeRoleCode`: `normalize` vai trò mã. */
    private static String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) return null;
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }

    /** Hàm `correlationId`: `correlation` mã. */
    private static String correlationId(GetSapToiLichHenQuery query) {
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
