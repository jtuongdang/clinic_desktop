package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.common.enums.TrangThaiKiemKe;
import vn.clinicmanagement.domain.inventory.KiemKe;
import vn.clinicmanagement.domain.inventory.KiemKeDem;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ApproveKiemKeHandler
        implements CommandHandler<ApproveKiemKeCommand, ApproveKiemKeBienNhan> {
    /** Trường `inventoryRepository`: kho kho dữ liệu. */
    private final IKhoRepository inventoryRepository;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public ApproveKiemKeHandler(
            IKhoRepository inventoryRepository,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<ApproveKiemKeBienNhan> handle(ApproveKiemKeCommand command) {
        if (command == null || command.stocktakeId() <= 0 || command.approverId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin phê duyệt kiểm kê không hợp lệ.");
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        if (context == null || !context.isValid()
                || !("ADMIN".equals(context.roleCode()) || "PHARMACIST".equals(context.roleCode()))) {
            return deny(context, command.auditActorContext());
        }

        Optional<KiemKe> stocktakeOpt = inventoryRepository.getStocktakeById(command.stocktakeId());
        if (stocktakeOpt.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy phiếu kiểm kê.");
        }

        KiemKe stocktake = stocktakeOpt.get();
        if (stocktake.getTrangThai() != TrangThaiKiemKe.MOI_TAO && stocktake.getTrangThai() != TrangThaiKiemKe.DA_NOP) {
            return KetQua.failure("INVALID_STATE", "Phiếu kiểm kê đã được duyệt hoặc bị hủy trước đó.");
        }

        KetQua<ApproveKiemKeBienNhan> txResult = transactions.inTransaction(() -> {
            stocktake.approve();
            stocktake.markAsUpdated(command.approverId());
            List<KiemKeDieuChinhDongDuLieu> adjustments = new ArrayList<>();
            if (stocktake.getCounts() != null) {
                for (KiemKeDem count : stocktake.getCounts()) {
                    if (count == null) continue;
                    Optional<TonKho> batchOpt = inventoryRepository.getBatchById(count.getMaTonKho());
                    if (batchOpt.isPresent()) {
                        TonKho batch = batchOpt.get();
                        batch.setSoLuongTon(count.getSoLuongThucTe());
                        batch.markAsUpdated(command.approverId());
                        inventoryRepository.saveTonKho(batch);
                        adjustments.add(new KiemKeDieuChinhDongDuLieu(
                                batch.getId() == null ? 0 : batch.getId(),
                                count.getSoLuongSoSach(),
                                count.getSoLuongThucTe(),
                                count.getChenhLech()));
                    }
                }
            }
            inventoryRepository.saveStocktake(stocktake);
            return KetQua.success(new ApproveKiemKeBienNhan(
                    stocktake.getId() == null ? command.stocktakeId() : stocktake.getId(),
                    adjustments.size(),
                    adjustments));
        });

        if (txResult.isFailure()) {
            return txResult;
        }

        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.INV_STOCKTAKE_APPROVED,
                KiemToanTaiNguyenLoai.STOCKTAKE,
                stocktake.getId(),
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                command.auditActorContext() == null ? null : command.auditActorContext().workstationId(),
                command.auditActorContext() == null
                        ? context.sessionId()
                        : command.auditActorContext().correlationId()));

        return txResult;
    }

    private KetQua<ApproveKiemKeBienNhan> deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                validContext ? context.employeeId() : null,
                validContext ? context.roleId() : null,
                validContext ? context.roleCode() : null,
                validContext ? context.facilityId() : null,
                validContext ? context.departmentId() : null,
                KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                null,
                null,
                KiemToanKetQua.DENIED,
                KiemToanLyDoMa.AUTH_FORBIDDEN,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                actor == null ? UUID.randomUUID() : actor.correlationId()));
        return KetQua.failure("AUTH_ACCESS_DENIED", "Không có quyền phê duyệt kiểm kê kho.");
    }
}
