package vn.clinicmanagement.application.inventory;

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
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetStockAvailabilityHandler
        implements QueryHandler<GetStockAvailabilityQuery, StockAvailabilityDto> {
    private static final Set<String> ALLOWED_ROLES = Set.of("PHARMACIST", "DOCTOR", "ADMIN");

    private final IKhoRepository inventoryRepository;
    private final AuditWritePort audit;

    public GetStockAvailabilityHandler(IKhoRepository inventoryRepository, AuditWritePort audit) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<StockAvailabilityDto> handle(GetStockAvailabilityQuery query) {
        if (query == null || query.warehouseId() <= 0 || query.itemId() <= 0) {
            return Result.failure("VALIDATION_ERROR", "Thông tin tra cứu kho hoặc mặt hàng không hợp lệ.");
        }

        AuthorizationContext context = query.authorizationContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, query.auditActorContext());
            return Result.failure("AUTH_ACCESS_DENIED", "Không có quyền tra cứu tồn kho trong phiên hiện tại.");
        }

        Optional<Kho> warehouse = inventoryRepository.getKhoById(query.warehouseId());
        if (warehouse.isEmpty()) {
            return Result.failure("NOT_FOUND", "Không tìm thấy kho dược.");
        }

        Optional<Item> item = inventoryRepository.getItemById(query.itemId());
        if (item.isEmpty()) {
            return Result.failure("NOT_FOUND", "Không tìm thấy mặt hàng.");
        }

        LocalDate today = LocalDate.now();
        List<TonKho> batches = inventoryRepository.getAvailableBatches(query.warehouseId(), query.itemId()).stream()
                .sorted(Comparator.comparing(TonKho::getHanSuDung, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        BigDecimal availableQuantity = BigDecimal.ZERO;
        List<StockBatchDto> batchDtos = batches.stream()
                .map(batch -> toDto(batch, today))
                .toList();
        for (StockBatchDto batch : batchDtos) {
            if (!batch.expired() && batch.soLuongTon() != null
                    && batch.soLuongTon().compareTo(BigDecimal.ZERO) > 0) {
                availableQuantity = availableQuantity.add(batch.soLuongTon());
            }
        }

        StockAvailabilityDto dto = new StockAvailabilityDto(
                query.warehouseId(),
                query.itemId(),
                item.get().getMaItem(),
                item.get().getTenItem(),
                availableQuantity,
                batchDtos);
        emitSuccessAudit(context, query.auditActorContext(), query.itemId());
        return Result.success(dto);
    }

    private boolean isAuthorized(AuthorizationContext context) {
        return context != null && context.isValid()
                && ALLOWED_ROLES.contains(context.roleCode().trim().toUpperCase(Locale.ROOT));
    }

    private StockBatchDto toDto(TonKho batch, LocalDate today) {
        LocalDate expiryDate = batch.getHanSuDung();
        return new StockBatchDto(
                batch.getId(),
                batch.getSoLo(),
                expiryDate,
                batch.getSoLuongTon(),
                batch.getGiaNhap(),
                batch.getGiaBan(),
                expiryDate != null && expiryDate.isBefore(today));
    }

    private void emitDeniedAudit(AuthorizationContext context, AuditActorContext actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new SystemAuditEntry(
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
                actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    private void emitSuccessAudit(AuthorizationContext context, AuditActorContext actor, int itemId) {
        audit.appendIndependently(new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                context.roleCode().trim().toUpperCase(Locale.ROOT),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.CATALOG_VIEW,
                AuditResourceType.ITEM,
                itemId,
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
}
