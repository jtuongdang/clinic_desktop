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
import vn.clinicmanagement.domain.common.enums.TrangThaiKiemKe;
import vn.clinicmanagement.domain.inventory.Stocktake;
import vn.clinicmanagement.domain.inventory.StocktakeCount;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ApproveStocktakeHandler
        implements CommandHandler<ApproveStocktakeCommand, ApproveStocktakeReceipt> {
    private final IKhoRepository inventoryRepository;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public ApproveStocktakeHandler(
            IKhoRepository inventoryRepository,
            TransactionBoundary transactions,
            AuditWritePort audit) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<ApproveStocktakeReceipt> handle(ApproveStocktakeCommand command) {
        if (command == null || command.stocktakeId() <= 0 || command.approverId() <= 0) {
            return Result.failure("VALIDATION_ERROR", "Thông tin phê duyệt kiểm kê không hợp lệ.");
        }

        AuthorizationContext context = command.authorizationContext();
        if (context == null || !context.isValid()
                || !("ADMIN".equals(context.roleCode()) || "PHARMACIST".equals(context.roleCode()))) {
            return deny(context, command.auditActorContext());
        }

        Optional<Stocktake> stocktakeOpt = inventoryRepository.getStocktakeById(command.stocktakeId());
        if (stocktakeOpt.isEmpty()) {
            return Result.failure("NOT_FOUND", "Không tìm thấy phiếu kiểm kê.");
        }

        Stocktake stocktake = stocktakeOpt.get();
        if (stocktake.getTrangThai() != TrangThaiKiemKe.MOI_TAO && stocktake.getTrangThai() != TrangThaiKiemKe.DA_NOP) {
            return Result.failure("INVALID_STATE", "Phiếu kiểm kê đã được duyệt hoặc bị hủy trước đó.");
        }

        Result<ApproveStocktakeReceipt> txResult = transactions.inTransaction(() -> {
            stocktake.approve();
            stocktake.markAsUpdated(command.approverId());
            List<StocktakeAdjustmentLineDto> adjustments = new ArrayList<>();
            if (stocktake.getCounts() != null) {
                for (StocktakeCount count : stocktake.getCounts()) {
                    if (count == null) continue;
                    Optional<TonKho> batchOpt = inventoryRepository.getBatchById(count.getMaTonKho());
                    if (batchOpt.isPresent()) {
                        TonKho batch = batchOpt.get();
                        batch.setSoLuongTon(count.getSoLuongThucTe());
                        batch.markAsUpdated(command.approverId());
                        inventoryRepository.saveTonKho(batch);
                        adjustments.add(new StocktakeAdjustmentLineDto(
                                batch.getId() == null ? 0 : batch.getId(),
                                count.getSoLuongSoSach(),
                                count.getSoLuongThucTe(),
                                count.getChenhLech()));
                    }
                }
            }
            inventoryRepository.saveStocktake(stocktake);
            return Result.success(new ApproveStocktakeReceipt(
                    stocktake.getId() == null ? command.stocktakeId() : stocktake.getId(),
                    adjustments.size(),
                    adjustments));
        });

        if (txResult.isFailure()) {
            return txResult;
        }

        audit.appendIndependently(new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.INV_STOCKTAKE_APPROVED,
                AuditResourceType.STOCKTAKE,
                stocktake.getId(),
                AuditOutcome.SUCCESS,
                null,
                Instant.now(),
                command.auditActorContext() == null ? null : command.auditActorContext().workstationId(),
                command.auditActorContext() == null
                        ? context.sessionId()
                        : command.auditActorContext().correlationId()));

        return txResult;
    }

    private Result<ApproveStocktakeReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
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
                actor == null ? UUID.randomUUID() : actor.correlationId()));
        return Result.failure("AUTH_ACCESS_DENIED", "Không có quyền phê duyệt kiểm kê kho.");
    }
}
