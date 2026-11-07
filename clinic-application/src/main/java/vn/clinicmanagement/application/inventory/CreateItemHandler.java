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
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;

public final class CreateItemHandler implements CommandHandler<CreateItemCommand, CreateItemReceipt> {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "PHARMACIST");

    private final IKhoRepository inventoryRepository;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public CreateItemHandler(IKhoRepository inventoryRepository, TransactionBoundary transactions,
                             AuditWritePort audit) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<CreateItemReceipt> handle(CreateItemCommand command) {
        if (command == null || isBlank(command.maItem()) || isBlank(command.tenItem())
                || isBlank(command.donViCoBan())) {
            return Result.failure("VALIDATION_ERROR", "Thông tin danh mục không hợp lệ.");
        }

        AuthorizationContext context = command.authorizationContext();
        if (!isAuthorized(context)) {
            deny(context, command.auditActorContext());
            return Result.failure("AUTHORIZATION_DENIED", "Không được phép tạo danh mục hàng hóa trong phiên hiện tại.");
        }

        String itemCode = command.maItem().trim();
        if (inventoryRepository.getItemByCode(itemCode).isPresent()) {
            return Result.failure("DUPLICATE_CODE", "Mã hàng đã tồn tại trên hệ thống.");
        }

        Item item = new Item();
        item.setMaItem(itemCode);
        item.setTenItem(command.tenItem());
        item.setLoaiItem(command.loaiItem());
        item.setHoatChat(command.hoatChat());
        item.setHamLuong(command.hamLuong());
        item.setDonViCoBan(command.donViCoBan());
        item.setQuyCachDongGoi(command.quyCachDongGoi());
        item.setGiaNhapMacDinh(command.giaNhapMacDinh());
        item.setGiaBanMacDinh(command.giaBanMacDinh());
        item.setTonToiThieu(command.tonToiThieu());
        item.setTonToiDa(command.tonToiDa());
        item.setDangBaoChe(command.dangBaoChe());
        item.setNuocSanXuat(command.nuocSanXuat());
        item.setHangSanXuat(command.hangSanXuat());
        item.setSoDangKy(command.soDangKy());

        Result<Item> savedResult = transactions.inTransaction(
                () -> Result.success(inventoryRepository.saveItem(item)));
        if (savedResult.isFailure()) {
            Result.ApplicationError error = savedResult.getError().orElseThrow();
            return Result.failure(error.code(), error.message());
        }

        Item savedItem = savedResult.valueOrThrow();
        appendSuccessAudit(context, command.auditActorContext(), savedItem.getId());
        return Result.success(new CreateItemReceipt(savedItem.getId(), savedItem.getMaItem(), savedItem.getTenItem()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    private void appendSuccessAudit(AuthorizationContext context, AuditActorContext actor, int itemId) {
        audit.appendIndependently(new SystemAuditEntry(
                context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                context.departmentId(), AuditActionCode.INV_ITEM_CREATED, AuditResourceType.ITEM, itemId,
                AuditOutcome.SUCCESS, null, Instant.now(), actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    private static UUID correlationId(AuthorizationContext context, AuditActorContext actor) {
        if (actor != null) return actor.correlationId();
        return context != null && context.sessionId() != null ? context.sessionId() : UUID.randomUUID();
    }
}
