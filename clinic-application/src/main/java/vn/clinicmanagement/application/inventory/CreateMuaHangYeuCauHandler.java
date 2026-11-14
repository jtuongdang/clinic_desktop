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

public final class CreateMuaHangYeuCauHandler
        implements CommandHandler<CreateMuaHangYeuCauCommand, CreateMuaHangYeuCauBienNhan> {
    private static final Set<String> ALLOWED_ROLES = Set.of("PHARMACIST", "ADMIN");
    private static final String VALIDATION_MESSAGE = "Thông tin đơn đặt hàng không hợp lệ.";

    /** Trường `inventoryRepository`: kho kho dữ liệu. */
    private final IKhoRepository inventoryRepository;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public CreateMuaHangYeuCauHandler(
            IKhoRepository inventoryRepository,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<CreateMuaHangYeuCauBienNhan> handle(CreateMuaHangYeuCauCommand command) {
        if (command == null || command.soDonDatHang() == null || command.soDonDatHang().isBlank()
                || command.supplierId() <= 0 || command.items() == null || command.items().isEmpty()
                || command.items().stream().anyMatch(item -> item == null || item.itemId() <= 0
                        || item.soLuong() == null || item.soLuong().compareTo(BigDecimal.ZERO) <= 0
                        || item.donGia() == null)) {
            return KetQua.failure("VALIDATION_ERROR", VALIDATION_MESSAGE);
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        if (!isAuthorized(context)) {
            deny(context, command.auditActorContext());
            return KetQua.failure("AUTH_ACCESS_DENIED",
                    "Không được phép tạo đơn đặt hàng trong phiên hiện tại.");
        }

        DonDatHang purchaseOrder = new DonDatHang();
        purchaseOrder.setSoDonDatHang(command.soDonDatHang().trim());
        purchaseOrder.setMaNhaCungCap(command.supplierId());
        purchaseOrder.setTrangThai(TrangThaiDonDatHang.CHO_DUYET);

        ArrayList<ChiTietDonDatHang> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (MuaHangYeuCauMatHangDauVao itemInput : command.items()) {
            ChiTietDonDatHang item = new ChiTietDonDatHang();
            item.setMaItem(itemInput.itemId());
            item.setSoLuongDat(itemInput.soLuong());
            item.setDonGia(itemInput.donGia());
            items.add(item);
            total = total.add(itemInput.soLuong().multiply(itemInput.donGia()));
        }
        purchaseOrder.setChiTiets(items);
        purchaseOrder.setTongTien(total);

        KetQua<DonDatHang> savedResult =
                transactions.inTransaction(() -> KetQua.success(inventoryRepository.savePurchaseOrder(purchaseOrder)));
        if (savedResult.isFailure()) {
            KetQua.ApplicationError error = savedResult.getError().orElseThrow();
            return KetQua.failure(error.code(), error.message());
        }

        DonDatHang savedOrder = savedResult.valueOrThrow();
        appendSuccessAudit(context, command.auditActorContext(), savedOrder.getId());
        return KetQua.success(new CreateMuaHangYeuCauBienNhan(
                savedOrder.getId(), savedOrder.getSoDonDatHang(), savedOrder.getTongTien()));
    }

    /** Hàm `isAuthorized`: là `Authorized`. */
    private static boolean isAuthorized(PhanQuyenNguCanh context) {
        return context != null && context.isValid() && ALLOWED_ROLES.contains(context.roleCode());
    }

    private void deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                validContext ? context.employeeId() : actor == null ? null : actor.employeeId(),
                validContext ? context.roleId() : actor == null ? null : actor.roleId(),
                validContext ? context.roleCode() : actor == null ? null : actor.roleCode(),
                validContext ? context.facilityId() : actor == null ? null : actor.facilityId(),
                validContext ? context.departmentId() : actor == null ? null : actor.departmentId(),
                KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                null,
                null,
                KiemToanKetQua.DENIED,
                KiemToanLyDoMa.AUTH_FORBIDDEN,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    /** Hàm `appendSuccessAudit`: thêm vào thành công kiểm toán. */
    private void appendSuccessAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, int purchaseOrderId) {
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.INV_RECEIPT_CREATED,
                KiemToanTaiNguyenLoai.INVENTORY_RECEIPT,
                purchaseOrderId,
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    /** Hàm `correlationId`: `correlation` mã. */
    private static UUID correlationId(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        if (actor != null) {
            return actor.correlationId();
        }
        return context != null && context.sessionId() != null ? context.sessionId() : UUID.randomUUID();
    }
}
