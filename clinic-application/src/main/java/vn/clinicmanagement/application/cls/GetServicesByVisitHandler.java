package vn.clinicmanagement.application.cls;

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
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetServicesByVisitHandler implements QueryHandler<GetServicesByVisitQuery, List<ServiceOrderDto>> {

    private static final Set<String> ALLOWED_ROLES = Set.of("DOCTOR", "LAB_TECH", "RECEPTIONIST", "ADMIN");

    private final IChiDinhDichVuRepository serviceRepository;
    private final AuditWritePort auditWritePort;

    public GetServicesByVisitHandler(
            IChiDinhDichVuRepository serviceRepository,
            AuditWritePort auditWritePort) {
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public Result<List<ServiceOrderDto>> handle(GetServicesByVisitQuery query) {
        if (query == null) {
            return Result.failure("VALIDATION_ERROR", "Truy vấn chỉ định dịch vụ không được để trống.");
        }
        if (query.visitId() <= 0) {
            return Result.failure("INVALID_VISIT", "Mã lượt khám không hợp lệ.");
        }

        AuthorizationContext context = query.sessionContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, query.auditActorContext(), "Quyền không được phép xem chỉ định dịch vụ.");
            return Result.failure("AUTH_ACCESS_DENIED", "Không có quyền tra cứu dịch vụ kỹ thuật/cận lâm sàng trong phiên hiện tại.");
        }

        List<ChiDinhDichVu> orders = serviceRepository.getByLuotKham(query.visitId());
        List<ServiceOrderDto> dtos = new ArrayList<>();

        for (ChiDinhDichVu order : orders) {
            if (order == null) continue;
            int orderId = order.getId() != null ? order.getId() : 0;
            Optional<DanhMucDichVu> catalogOpt = serviceRepository.getDichVuById(order.getMaDichVu());
            String serviceCode = catalogOpt.map(DanhMucDichVu::getMaDichVu).orElse("");
            String serviceName = catalogOpt.map(DanhMucDichVu::getTenDichVu).orElse("");
            String serviceType = catalogOpt.map(d -> d.getLoaiDichVu() != null ? d.getLoaiDichVu().name() : "").orElse("");

            List<ChiTietKetQuaXetNghiem> results = serviceRepository.getKetQuaByChiDinh(orderId);
            List<LabResultDto> resultDtos = new ArrayList<>();
            for (ChiTietKetQuaXetNghiem res : results) {
                if (res == null) continue;
                resultDtos.add(new LabResultDto(
                        res.getId() != null ? res.getId() : 0,
                        orderId,
                        res.getTenChiSo(),
                        res.getGiaTri(),
                        res.getDonViDo(),
                        res.getKhoangThamChieu(),
                        res.getDanhGia()));
            }

            dtos.add(new ServiceOrderDto(
                    orderId,
                    order.getMaLuotKham(),
                    order.getMaDichVu(),
                    serviceCode,
                    serviceName,
                    serviceType,
                    order.getMaBacSiChiDinh(),
                    order.getSoLuong(),
                    order.getDonGia(),
                    order.getTrangThai() != null ? order.getTrangThai().name() : "",
                    order.getKetQua(),
                    order.getKetLuan(),
                    order.getDanhSachHinhAnhUrl(),
                    order.getMaPhongThucHien(),
                    order.getBacSiThucHien(),
                    order.getThoiGianChiDinh(),
                    order.getThoiGianThucHien(),
                    order.getThoiGianTraKetQua(),
                    resultDtos));
        }

        emitSuccessAudit(context, query.auditActorContext(), query.visitId());
        return Result.success(dtos);
    }

    private boolean isAuthorized(AuthorizationContext context) {
        if (context == null || !context.isValid()) {
            return false;
        }
        String role = context.roleCode().trim().toUpperCase(Locale.ROOT);
        return ALLOWED_ROLES.contains(role);
    }

    private void emitDeniedAudit(AuthorizationContext context, AuditActorContext actor, String reason) {
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
                AuditActionCode.PATIENT_VIEW,
                AuditResourceType.SERVICE_ORDER,
                visitId,
                AuditOutcome.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                correlationId(context, actor)));
    }

    private UUID correlationId(AuthorizationContext context, AuditActorContext actor) {
        if (actor != null && actor.correlationId() != null) {
            return actor.correlationId();
        }
        if (context != null && context.sessionId() != null) {
            return context.sessionId();
        }
        return UUID.randomUUID();
    }
}
