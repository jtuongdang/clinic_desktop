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
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;

public final class CreateMatHangHandler implements CommandHandler<CreateMatHangCommand, CreateMatHangBienNhan> {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "PHARMACIST");

    /** Trường `inventoryRepository`: kho kho dữ liệu. */
    private final IKhoRepository inventoryRepository;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public CreateMatHangHandler(IKhoRepository inventoryRepository, GiaoDichRanhGioi transactions,
                             KiemToanWriteCongKetNoi audit) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<CreateMatHangBienNhan> handle(CreateMatHangCommand command) {
        if (command == null || isBlank(command.maItem()) || isBlank(command.tenItem())
                || isBlank(command.donViCoBan())) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin danh mục không hợp lệ.");
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        if (!isAuthorized(context)) {
            deny(context, command.auditActorContext());
            return KetQua.failure("AUTHORIZATION_DENIED", "Không được phép tạo danh mục hàng hóa trong phiên hiện tại.");
        }

        String itemCode = command.maItem().trim();
        if (inventoryRepository.getItemByCode(itemCode).isPresent()) {
            return KetQua.failure("DUPLICATE_CODE", "Mã hàng đã tồn tại trên hệ thống.");
        }

        MatHang item = new MatHang();
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

        KetQua<MatHang> savedResult = transactions.inTransaction(
                () -> KetQua.success(inventoryRepository.saveItem(item)));
        if (savedResult.isFailure()) {
            KetQua.ApplicationError error = savedResult.getError().orElseThrow();
            return KetQua.failure(error.code(), error.message());
        }

        MatHang savedItem = savedResult.valueOrThrow();
        appendSuccessAudit(context, command.auditActorContext(), savedItem.getId());
        return KetQua.success(new CreateMatHangBienNhan(savedItem.getId(), savedItem.getMaItem(), savedItem.getTenItem()));
    }

    /** Hàm `isBlank`: là `Blank`. */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
                KiemToanHanhDongMa.AUTH_ACCESS_DENIED, null, null, KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN,
                Instant.now(), actor == null ? null : actor.workstationId(), correlationId(context, actor)));
    }

    /** Hàm `appendSuccessAudit`: thêm vào thành công kiểm toán. */
    private void appendSuccessAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, int itemId) {
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                context.departmentId(), KiemToanHanhDongMa.INV_ITEM_CREATED, KiemToanTaiNguyenLoai.ITEM, itemId,
                KiemToanKetQua.SUCCESS, null, Instant.now(), actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    /** Hàm `correlationId`: `correlation` mã. */
    private static UUID correlationId(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        if (actor != null) return actor.correlationId();
        return context != null && context.sessionId() != null ? context.sessionId() : UUID.randomUUID();
    }
}
