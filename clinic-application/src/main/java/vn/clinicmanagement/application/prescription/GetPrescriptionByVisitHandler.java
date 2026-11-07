package vn.clinicmanagement.application.prescription;

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
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetPrescriptionByVisitHandler
        implements QueryHandler<GetPrescriptionByVisitQuery, List<PrescriptionDto>> {
    private static final Set<String> ALLOWED_ROLES = Set.of("DOCTOR", "RECEPTIONIST", "ADMIN");

    private final IDonThuocRepository prescriptionRepository;
    private final ILuotKhamRepository visitRepository;
    private final AuditWritePort audit;

    public GetPrescriptionByVisitHandler(
            IDonThuocRepository prescriptionRepository,
            ILuotKhamRepository visitRepository,
            AuditWritePort audit) {
        this.prescriptionRepository = Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<List<PrescriptionDto>> handle(GetPrescriptionByVisitQuery query) {
        if (query == null || query.visitId() <= 0) {
            return Result.failure("VALIDATION_ERROR", "Mã lượt khám không hợp lệ.");
        }

        AuthorizationContext context = query.authorizationContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, query.auditActorContext());
            return Result.failure("AUTH_ACCESS_DENIED", "Không có quyền xem đơn thuốc trong phiên hiện tại.");
        }

        Optional<LuotKham> visitResult = visitRepository.getById(query.visitId());
        if (visitResult.isEmpty()) {
            return Result.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
        }

        List<PrescriptionDto> prescriptions = prescriptionRepository.getByLuotKhamId(query.visitId()).stream()
                .map(this::toDto)
                .toList();
        emitSuccessAudit(context, query.auditActorContext(), query.visitId());
        return Result.success(prescriptions);
    }

    private boolean isAuthorized(AuthorizationContext context) {
        return context != null && context.isValid() && ALLOWED_ROLES.contains(context.roleCode());
    }

    private void emitDeniedAudit(AuthorizationContext context, AuditActorContext actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new SystemAuditEntry(
                validContext ? context.employeeId() : null,
                validContext ? context.roleId() : null,
                validContext ? context.roleCode() : null,
                validContext ? context.facilityId() : null,
                validContext ? context.departmentId() : null,
                AuditActionCode.AUTH_ACCESS_DENIED,
                null,
                null,
                AuditOutcome.DENIED,
                AuditReasonCode.AUTH_FORBIDDEN,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    private void emitSuccessAudit(AuthorizationContext context, AuditActorContext actor, int visitId) {
        audit.appendIndependently(new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.EMR_VIEW,
                AuditResourceType.PRESCRIPTION,
                visitId,
                AuditOutcome.SUCCESS,
                null,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
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

    private PrescriptionDto toDto(DonThuoc prescription) {
        List<PrescriptionItemDto> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        if (prescription.getChiTietDonThuocs() != null) {
            for (ChiTietDonThuoc detail : prescription.getChiTietDonThuocs()) {
                BigDecimal quantity = detail.getSoLuongKe() == null ? BigDecimal.ZERO : detail.getSoLuongKe();
                BigDecimal unitPrice = detail.getDonGiaKe() == null ? BigDecimal.ZERO : detail.getDonGiaKe();
                BigDecimal lineTotal = quantity.multiply(unitPrice);
                total = total.add(lineTotal);
                items.add(toItemDto(detail, lineTotal));
            }
        }

        return new PrescriptionDto(
                prescription.getId(),
                prescription.getMaLuotKham(),
                prescription.getMaBacSi(),
                prescription.getBacSi() == null ? null : prescription.getBacSi().getHoTen(),
                prescription.getMaCoSo(),
                prescription.getMaKhoXuat(),
                prescription.getTrangThai(),
                prescription.getNgayKe(),
                prescription.getGhiChu(),
                prescription.getLoiDanDonThuoc(),
                total,
                List.copyOf(items));
    }

    private PrescriptionItemDto toItemDto(ChiTietDonThuoc detail, BigDecimal lineTotal) {
        Item item = detail.getItem();
        return new PrescriptionItemDto(
                detail.getId(),
                detail.getMaItem(),
                item == null ? null : item.getMaItem(),
                item == null ? null : item.getTenItem(),
                item == null ? null : item.getDonViCoBan(),
                detail.getSoLuongKe(),
                detail.getSoLuongDaXuat(),
                detail.getDonGiaKe(),
                lineTotal,
                detail.getDuongDung(),
                detail.getLieuDung(),
                detail.getTanSuat(),
                detail.getSoNgay(),
                detail.getSang(),
                detail.getTrua(),
                detail.getChieu(),
                detail.getToi(),
                detail.getHuongDan());
    }
}
