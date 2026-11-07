package vn.clinicmanagement.application.cls;

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
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RecordServiceResultHandler implements CommandHandler<RecordServiceResultCommand, RecordServiceResultReceipt> {

    private static final Set<String> ALLOWED_ROLES = Set.of("LAB_TECH", "DOCTOR", "ADMIN");

    private final IChiDinhDichVuRepository serviceRepository;
    private final AuditWritePort auditWritePort;

    public RecordServiceResultHandler(
            IChiDinhDichVuRepository serviceRepository,
            AuditWritePort auditWritePort) {
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public Result<RecordServiceResultReceipt> handle(RecordServiceResultCommand command) {
        if (command == null) {
            return Result.failure("VALIDATION_ERROR", "Lệnh nhập kết quả dịch vụ không được để trống.");
        }
        if (command.orderId() <= 0) {
            return Result.failure("INVALID_ORDER", "Mã chỉ định dịch vụ không hợp lệ.");
        }

        AuthorizationContext context = command.sessionContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, command.auditActorContext(), "Quyền không được phép nhập kết quả dịch vụ.");
            return Result.failure("AUTH_ACCESS_DENIED", "Không có quyền nhập kết quả kỹ thuật/cận lâm sàng trong phiên hiện tại.");
        }

        Optional<ChiDinhDichVu> orderOpt = serviceRepository.getById(command.orderId());
        if (orderOpt.isEmpty()) {
            return Result.failure("ORDER_NOT_FOUND", "Không tìm thấy chỉ định dịch vụ.");
        }
        ChiDinhDichVu order = orderOpt.get();
        if (order.getTrangThai() == TrangThaiChiDinhDichVu.DA_HUY) {
            return Result.failure("ORDER_CANCELLED", "Chỉ định dịch vụ đã bị hủy, không thể nhập kết quả.");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CO_KET_QUA);
        order.setKetQua(command.resultSummary() != null ? command.resultSummary() : "");
        order.setKetLuan(command.conclusion() != null ? command.conclusion() : "");
        order.setDanhSachHinhAnhUrl(command.imageAttachmentUrls() != null ? command.imageAttachmentUrls() : "");
        order.setBacSiThucHien(context.employeeId());
        order.setThoiGianThucHien(now);
        order.setThoiGianTraKetQua(now);
        order.markAsUpdated(context.employeeId());

        serviceRepository.save(order);

        int itemCount = 0;
        if (command.resultItems() != null) {
            for (LabResultItemInput item : command.resultItems()) {
                if (item == null) continue;
                ChiTietKetQuaXetNghiem line = new ChiTietKetQuaXetNghiem();
                line.setMaChiDinhDichVu(order.getId());
                line.setTenChiSo(item.indicatorName() != null ? item.indicatorName() : "");
                line.setGiaTri(item.value() != null ? item.value() : "");
                line.setDonViDo(item.unit());
                line.setKhoangThamChieu(item.referenceRange());
                line.setDanhGia(item.evaluation());
                line.setCreatedBy(context.employeeId());
                serviceRepository.saveKetQua(line);
                itemCount++;
            }
        }

        emitSuccessAudit(context, command.auditActorContext(), order.getId());

        RecordServiceResultReceipt receipt = new RecordServiceResultReceipt(
                order.getId(),
                order.getMaLuotKham(),
                order.getTrangThai().name(),
                context.employeeId(),
                itemCount,
                now,
                now);

        return Result.success(receipt);
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

    private void emitSuccessAudit(AuthorizationContext context, AuditActorContext actor, int orderId) {
        auditWritePort.appendIndependently(new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                context.roleCode().trim().toUpperCase(Locale.ROOT),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.SERVICE_RESULT_RECORDED,
                AuditResourceType.SERVICE_ORDER,
                orderId,
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
