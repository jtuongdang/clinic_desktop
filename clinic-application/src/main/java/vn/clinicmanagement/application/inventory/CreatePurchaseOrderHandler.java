package vn.clinicmanagement.application.inventory;

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
import vn.clinicmanagement.domain.common.enums.TrangThaiDonDatHang;
import vn.clinicmanagement.domain.inventory.ChiTietDonDatHang;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CreatePurchaseOrderHandler
        implements CommandHandler<CreatePurchaseOrderCommand, CreatePurchaseOrderReceipt> {
    private static final Set<String> ALLOWED_ROLES = Set.of("PHARMACIST", "ADMIN");
    private static final String VALIDATION_MESSAGE = "Thông tin đơn đặt hàng không hợp lệ.";

    private final IKhoRepository inventoryRepository;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public CreatePurchaseOrderHandler(
            IKhoRepository inventoryRepository,
            TransactionBoundary transactions,
            AuditWritePort audit) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<CreatePurchaseOrderReceipt> handle(CreatePurchaseOrderCommand command) {
        if (command == null || command.soDonDatHang() == null || command.soDonDatHang().isBlank()
                || command.supplierId() <= 0 || command.items() == null || command.items().isEmpty()
                || command.items().stream().anyMatch(item -> item == null || item.itemId() <= 0
                        || item.soLuong() == null || item.soLuong().compareTo(BigDecimal.ZERO) <= 0
                        || item.donGia() == null)) {
            return Result.failure("VALIDATION_ERROR", VALIDATION_MESSAGE);
        }

        AuthorizationContext context = command.authorizationContext();
        if (!isAuthorized(context)) {
            deny(context, command.auditActorContext());
            return Result.failure("AUTH_ACCESS_DENIED",
                    "Không được phép tạo đơn đặt hàng trong phiên hiện tại.");
        }

        DonDatHang purchaseOrder = new DonDatHang();
        purchaseOrder.setSoDonDatHang(command.soDonDatHang().trim());
        purchaseOrder.setMaNhaCungCap(command.supplierId());
        purchaseOrder.setTrangThai(TrangThaiDonDatHang.CHO_DUYET);

        ArrayList<ChiTietDonDatHang> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItemInput itemInput : command.items()) {
            ChiTietDonDatHang item = new ChiTietDonDatHang();
            item.setMaItem(itemInput.itemId());
            item.setSoLuongDat(itemInput.soLuong());
            item.setDonGia(itemInput.donGia());
            items.add(item);
            total = total.add(itemInput.soLuong().multiply(itemInput.donGia()));
        }
        purchaseOrder.setChiTiets(items);
        purchaseOrder.setTongTien(total);

        Result<DonDatHang> savedResult =
                transactions.inTransaction(() -> Result.success(inventoryRepository.savePurchaseOrder(purchaseOrder)));
        if (savedResult.isFailure()) {
            Result.ApplicationError error = savedResult.getError().orElseThrow();
            return Result.failure(error.code(), error.message());
        }

        DonDatHang savedOrder = savedResult.valueOrThrow();
        appendSuccessAudit(context, command.auditActorContext(), savedOrder.getId());
        return Result.success(new CreatePurchaseOrderReceipt(
                savedOrder.getId(), savedOrder.getSoDonDatHang(), savedOrder.getTongTien()));
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
                AuditActionCode.AUTH_ACCESS_DENIED,
                null,
                null,
                AuditOutcome.DENIED,
                AuditReasonCode.AUTH_FORBIDDEN,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    private void appendSuccessAudit(AuthorizationContext context, AuditActorContext actor, int purchaseOrderId) {
        audit.appendIndependently(new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.INV_RECEIPT_CREATED,
                AuditResourceType.INVENTORY_RECEIPT,
                purchaseOrderId,
                AuditOutcome.SUCCESS,
                null,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    private static UUID correlationId(AuthorizationContext context, AuditActorContext actor) {
        if (actor != null) {
            return actor.correlationId();
        }
        return context != null && context.sessionId() != null ? context.sessionId() : UUID.randomUUID();
    }
}
