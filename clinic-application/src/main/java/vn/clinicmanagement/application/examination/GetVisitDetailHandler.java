package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetVisitDetailHandler implements QueryHandler<GetVisitDetailQuery, VisitDetailDto> {
    private static final Set<String> ALLOWED_ROLES = Set.of("DOCTOR", "RECEPTIONIST", "ADMIN");

    private final ILuotKhamRepository visitRepository;
    private final AuditWritePort auditWritePort;

    public GetVisitDetailHandler(ILuotKhamRepository visitRepository, AuditWritePort auditWritePort) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public Result<VisitDetailDto> handle(GetVisitDetailQuery query) {
        if (query == null || query.visitId() <= 0) {
            return Result.failure("VALIDATION_ERROR", "Mã lượt khám không hợp lệ.");
        }

        AuthorizationContext context = query.sessionContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, query.auditActorContext());
            return Result.failure("AUTH_ACCESS_DENIED", "Không có quyền xem lượt khám trong phiên hiện tại.");
        }

        Optional<LuotKham> visitResult = visitRepository.getById(query.visitId());
        if (visitResult.isEmpty()) {
            return Result.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
        }

        VisitDetailDto detail = toDto(visitResult.get());
        emitSuccessAudit(context, query.auditActorContext(), query.visitId());
        return Result.success(detail);
    }

    private boolean isAuthorized(AuthorizationContext context) {
        if (context == null || !context.isValid()) {
            return false;
        }
        String roleCode = context.roleCode().trim().toUpperCase(Locale.ROOT);
        return ALLOWED_ROLES.contains(roleCode);
    }

    private void emitDeniedAudit(AuthorizationContext context, AuditActorContext actor) {
        boolean validContext = context != null && context.isValid();
        auditWritePort.appendIndependently(new SystemAuditEntry(
                validContext ? context.employeeId() : null,
                validContext ? context.roleId() : null,
                validContext ? context.roleCode().trim().toUpperCase(Locale.ROOT) : null,
                validContext ? context.facilityId() : null,
                validContext ? context.departmentId() : null,
                AuditActionCode.AUTH_ACCESS_DENIED,
                null,
                null,
                AuditOutcome.DENIED,
                AuditReasonCode.AUTH_FORBIDDEN,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                correlationId(context, actor)));
    }

    private void emitSuccessAudit(AuthorizationContext context, AuditActorContext actor, int visitId) {
        auditWritePort.appendIndependently(new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                context.roleCode().trim().toUpperCase(Locale.ROOT),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.EMR_VIEW,
                AuditResourceType.VISIT,
                visitId,
                AuditOutcome.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                correlationId(context, actor)));
    }

    private UUID correlationId(AuthorizationContext context, AuditActorContext actor) {
        if (actor != null) {
            return actor.correlationId();
        }
        if (context != null && context.sessionId() != null) {
            return context.sessionId();
        }
        return UUID.randomUUID();
    }

    private VisitDetailDto toDto(LuotKham visit) {
        return new VisitDetailDto(
                visit.getId(),
                visit.getMaBenhNhan(),
                visit.getMaBacSi(),
                visit.getMaCoSo(),
                visit.getMaKhoa(),
                visit.getMaPhongKham(),
                visit.getSoThuTuDisplay(),
                visit.getSoThuTu(),
                visit.getTrangThai(),
                visit.getLyDoKham(),
                visit.getQuaTrinhBenhLy(),
                visit.getTienSuBanThan(),
                visit.getTienSuGiaDinh(),
                visit.getKhamToanThan(),
                visit.getKhamBoPhan(),
                visit.getTomTatBenhAn(),
                visit.getHuongDieuTri(),
                visit.getLoiDanBacSi(),
                visit.getKetQuaDieuTri(),
                visit.getHuongXuTri(),
                visit.getNgayHenTaiKham(),
                visit.isEmrLocked(),
                visit.getEmrLockedAt(),
                visit.getEmrLockedBy(),
                visit.getEmrSignature(),
                visit.getThoiGianBatDauKham(),
                visit.getThoiGianKetThucKham());
    }
}
