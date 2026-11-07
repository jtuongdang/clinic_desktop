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
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class OrderServiceHandler implements CommandHandler<OrderServiceCommand, OrderServiceReceipt> {

    private static final Set<String> ALLOWED_ROLES = Set.of("DOCTOR", "ADMIN");

    private final IChiDinhDichVuRepository serviceRepository;
    private final ILuotKhamRepository visitRepository;
    private final AuditWritePort auditWritePort;

    public OrderServiceHandler(
            IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository,
            AuditWritePort auditWritePort) {
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public Result<OrderServiceReceipt> handle(OrderServiceCommand command) {
        if (command == null) {
            return Result.failure("VALIDATION_ERROR", "Lệnh chỉ định dịch vụ không được để trống.");
        }
        if (command.visitId() <= 0) {
            return Result.failure("INVALID_VISIT", "Mã lượt khám không hợp lệ.");
        }
        if (command.serviceId() <= 0) {
            return Result.failure("INVALID_SERVICE", "Mã dịch vụ không hợp lệ.");
        }
        if (command.quantity() == null || command.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.failure("INVALID_QUANTITY", "Số lượng chỉ định phải lớn hơn 0.");
        }

        AuthorizationContext context = command.sessionContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, command.auditActorContext(), "Quyền không được phép chỉ định dịch vụ.");
            return Result.failure("AUTH_ACCESS_DENIED", "Không có quyền chỉ định dịch vụ kỹ thuật trong phiên hiện tại.");
        }

        Optional<LuotKham> visitOpt = visitRepository.getById(command.visitId());
        if (visitOpt.isEmpty()) {
            return Result.failure("VISIT_NOT_FOUND", "Không tìm thấy lượt khám.");
        }
        LuotKham visit = visitOpt.get();
        if (visit.getTrangThai() == TrangThaiLuotKham.DA_HUY) {
            return Result.failure("VISIT_CANCELLED", "Lượt khám đã bị hủy, không thể chỉ định dịch vụ.");
        }

        Optional<DanhMucDichVu> serviceOpt = serviceRepository.getDichVuById(command.serviceId());
        if (serviceOpt.isEmpty()) {
            return Result.failure("SERVICE_NOT_FOUND", "Không tìm thấy dịch vụ kỹ thuật trong danh mục.");
        }
        DanhMucDichVu service = serviceOpt.get();
        if (!service.isHoatDong()) {
            return Result.failure("SERVICE_INACTIVE", "Dịch vụ đã ngừng hoạt động.");
        }

        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setMaLuotKham(command.visitId());
        order.setMaDichVu(command.serviceId());
        order.setMaBacSiChiDinh(context.employeeId());
        order.setSoLuong(command.quantity());
        order.setDonGia(service.getDonGia() != null ? service.getDonGia() : BigDecimal.ZERO);
        order.setMaPhongThucHien(command.roomId());
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        order.setThoiGianChiDinh(LocalDateTime.now());
        order.setCreatedBy(context.employeeId());

        serviceRepository.save(order);
        int orderId = order.getId() != null ? order.getId() : 0;

        emitSuccessAudit(context, command.auditActorContext(), orderId);

        OrderServiceReceipt receipt = new OrderServiceReceipt(
                orderId,
                command.visitId(),
                command.serviceId(),
                service.getTenDichVu(),
                order.getSoLuong(),
                order.getDonGia(),
                order.getTrangThai().name(),
                order.getThoiGianChiDinh());

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
                AuditActionCode.SERVICE_ORDERED,
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
