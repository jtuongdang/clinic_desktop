package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class GetItemCatalogHandler
        implements QueryHandler<GetItemCatalogQuery, List<ItemSummaryDto>> {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "PHARMACIST", "DOCTOR", "RECEPTIONIST");

    private final IKhoRepository inventoryRepository;
    private final AuditWritePort audit;

    public GetItemCatalogHandler(IKhoRepository inventoryRepository, AuditWritePort audit) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<List<ItemSummaryDto>> handle(GetItemCatalogQuery query) {
        if (query == null) {
            return Result.failure("VALIDATION_ERROR", "Thông tin tra cứu danh mục không hợp lệ.");
        }

        AuthorizationContext context = query.authorizationContext();
        if (!isAuthorized(context)) {
            deny(context, query.auditActorContext());
            return Result.failure("AUTHORIZATION_DENIED", "Không được phép xem danh mục hàng hóa trong phiên hiện tại.");
        }

        List<ItemSummaryDto> items = inventoryRepository.searchItems(query.keyword(), query.activeOnly()).stream()
                .map(GetItemCatalogHandler::toSummary)
                .toList();
        appendSuccessAudit(context, query.auditActorContext());
        return Result.success(items);
    }

    private static boolean isAuthorized(AuthorizationContext context) {
        return context != null && context.isValid() && ALLOWED_ROLES.contains(context.roleCode());
    }

    private void deny(AuthorizationContext context, AuditActorContext actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new SystemAuditEntry(
                validContext ? context.employeeId() : actor == null ? null : actor.employeeId(),
                validContext ? context.roleId() : actor == null ? null : actor.roleId(),
                validContext ? context.roleCode() : actor == null ? null : actor.roleCode(),
                validContext ? context.facilityId() : actor == null ? null : actor.facilityId(),
                validContext ? context.departmentId() : actor == null ? null : actor.departmentId(),
                AuditActionCode.AUTH_ACCESS_DENIED, null, null, AuditOutcome.DENIED, AuditReasonCode.AUTH_FORBIDDEN,
                Instant.now(), actor == null ? null : actor.workstationId(), correlationId(context, actor)));
    }

    private void appendSuccessAudit(AuthorizationContext context, AuditActorContext actor) {
        audit.appendIndependently(new SystemAuditEntry(
                context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                context.departmentId(), AuditActionCode.CATALOG_VIEW, null, null,
                AuditOutcome.SUCCESS, null, Instant.now(), actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    private static UUID correlationId(AuthorizationContext context, AuditActorContext actor) {
        if (actor != null) return actor.correlationId();
        return context != null && context.sessionId() != null ? context.sessionId() : UUID.randomUUID();
    }

    private static ItemSummaryDto toSummary(Item item) {
        return new ItemSummaryDto(item.getId(), item.getMaItem(), item.getTenItem(), item.getLoaiItem(),
                item.getHoatChat(), item.getHamLuong(), item.getDonViCoBan(), item.getGiaBanMacDinh(),
                item.isHoatDong());
    }
}
