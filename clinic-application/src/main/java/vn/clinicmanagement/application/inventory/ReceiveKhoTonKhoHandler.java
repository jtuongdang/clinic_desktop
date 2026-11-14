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
import vn.clinicmanagement.domain.common.enums.TrangThaiPhieuNhap;
import vn.clinicmanagement.domain.inventory.ChiTietNhapKho;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ReceiveKhoTonKhoHandler
        implements CommandHandler<ReceiveKhoTonKhoCommand, ReceiveKhoTonKhoBienNhan> {

    /** Trường `inventoryRepository`: kho kho dữ liệu. */
    private final IKhoRepository inventoryRepository;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public ReceiveKhoTonKhoHandler(
            IKhoRepository inventoryRepository,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<ReceiveKhoTonKhoBienNhan> handle(ReceiveKhoTonKhoCommand command) {
        KetQua<ReceiveKhoTonKhoBienNhan> validationError = validate(command);
        if (validationError != null) {
            return validationError;
        }

        if (!isAuthorized(command.authorizationContext())) {
            return deny(command, KiemToanLyDoMa.AUTH_FORBIDDEN);
        }

        Optional<Kho> warehouse = inventoryRepository.getKhoById(command.warehouseId());
        if (warehouse.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy kho hàng.");
        }

        KetQua<ReceiveKhoTonKhoBienNhan> txResult;
        try {
            txResult = transactions.inTransaction(() -> {
                List<ChiTietNhapKho> chiTiets = new ArrayList<>();
                BigDecimal tongTien = BigDecimal.ZERO;

                for (TonKhoBienNhanMatHangDauVao item : command.items()) {
                    BigDecimal soLuong = item.soLuong() != null ? item.soLuong() : BigDecimal.ZERO;
                    BigDecimal giaNhap = item.giaNhap() != null ? item.giaNhap() : BigDecimal.ZERO;
                    BigDecimal giaBan = item.giaBan() != null ? item.giaBan() : BigDecimal.ZERO;

                    ChiTietNhapKho ct = new ChiTietNhapKho();
                    ct.setMaItem(item.itemId());
                    ct.setSoLo(item.soLo());
                    ct.setHanSuDung(item.hanSuDung());
                    ct.setSoLuong(soLuong);
                    ct.setGiaNhap(giaNhap);
                    ct.setGiaBan(giaBan);
                    if (command.authorizationContext() != null) {
                        ct.setCreatedBy(command.authorizationContext().employeeId());
                    }
                    chiTiets.add(ct);

                    BigDecimal lineTotal = soLuong.multiply(giaNhap);
                    tongTien = tongTien.add(lineTotal);
                }

                PhieuNhapKho phieu = new PhieuNhapKho();
                phieu.setSoPhieuNhap(command.soPhieuNhap());
                phieu.setMaKho(command.warehouseId());
                phieu.setMaNhaCungCap(command.supplierId());
                phieu.setNgayNhap(LocalDateTime.now());
                phieu.setTrangThai(TrangThaiPhieuNhap.DA_NHAP);
                phieu.setTongTien(tongTien);
                phieu.setChiTiets(chiTiets);
                if (command.authorizationContext() != null) {
                    phieu.setCreatedBy(command.authorizationContext().employeeId());
                }

                PhieuNhapKho savedPhieu = inventoryRepository.savePhieuNhapKho(phieu);
                int receiptId = savedPhieu != null && savedPhieu.getId() != null ? savedPhieu.getId() : 0;

                for (ChiTietNhapKho ct : chiTiets) {
                    ct.setMaPhieuNhap(receiptId);
                }

                for (TonKhoBienNhanMatHangDauVao item : command.items()) {
                    BigDecimal soLuong = item.soLuong() != null ? item.soLuong() : BigDecimal.ZERO;
                    BigDecimal giaNhap = item.giaNhap() != null ? item.giaNhap() : BigDecimal.ZERO;
                    BigDecimal giaBan = item.giaBan() != null ? item.giaBan() : BigDecimal.ZERO;

                    TonKho batch = new TonKho();
                    batch.setMaKho(command.warehouseId());
                    batch.setMaItem(item.itemId());
                    batch.setSoLo(item.soLo());
                    batch.setHanSuDung(item.hanSuDung());
                    batch.setSoLuongTon(soLuong);
                    batch.setGiaNhap(giaNhap);
                    batch.setGiaBan(giaBan);
                    if (command.authorizationContext() != null) {
                        batch.setCreatedBy(command.authorizationContext().employeeId());
                    }
                    inventoryRepository.saveTonKho(batch);
                }

                return KetQua.success(new ReceiveKhoTonKhoBienNhan(
                        receiptId,
                        command.soPhieuNhap(),
                        tongTien,
                        command.items().size()));
            });
        } catch (RuntimeException ex) {
            return KetQua.failure("TRANSACTION_FAILED", "Không thể hoàn tất nhập kho. Lỗi giao dịch.");
        }

        if (txResult != null && txResult.isSuccess()) {
            ReceiveKhoTonKhoBienNhan receipt = txResult.valueOrThrow();
            logSuccessAudit(command, receipt.receiptId());
        }

        return txResult;
    }

    /** Hàm `validate`: kiểm tra hợp lệ. */
    private static KetQua<ReceiveKhoTonKhoBienNhan> validate(ReceiveKhoTonKhoCommand command) {
        if (command == null) {
            return KetQua.failure("INVALID_INPUT", "Lệnh nhập kho không được để trống.");
        }
        if (command.warehouseId() <= 0) {
            return KetQua.failure("INVALID_INPUT", "Mã kho không hợp lệ.");
        }
        if (command.supplierId() <= 0) {
            return KetQua.failure("INVALID_INPUT", "Mã nhà cung cấp không hợp lệ.");
        }
        if (command.soPhieuNhap() == null || command.soPhieuNhap().isBlank()) {
            return KetQua.failure("INVALID_INPUT", "Số phiếu nhập không được để trống.");
        }
        if (command.items() == null || command.items().isEmpty()) {
            return KetQua.failure("INVALID_INPUT", "Danh sách mặt hàng nhập kho không được để trống.");
        }
        for (TonKhoBienNhanMatHangDauVao item : command.items()) {
            if (item == null) {
                return KetQua.failure("INVALID_INPUT", "Mặt hàng nhập kho không hợp lệ.");
            }
            if (item.itemId() <= 0) {
                return KetQua.failure("INVALID_INPUT", "Mã mặt hàng không hợp lệ.");
            }
            if (item.soLo() == null || item.soLo().isBlank()) {
                return KetQua.failure("INVALID_INPUT", "Số lô không được để trống.");
            }
            if (item.hanSuDung() == null) {
                return KetQua.failure("INVALID_INPUT", "Hạn sử dụng không được để trống.");
            }
            if (item.soLuong() == null || item.soLuong().compareTo(BigDecimal.ZERO) <= 0) {
                return KetQua.failure("INVALID_INPUT", "Số lượng nhập phải lớn hơn 0.");
            }
            if (item.hanSuDung().isBefore(LocalDate.now())) {
                return KetQua.failure("EXPIRED_BATCH", "Không thể nhập lô hàng đã hết hạn sử dụng.");
            }
        }
        return null;
    }

    /** Hàm `isAuthorized`: là `Authorized`. */
    private static boolean isAuthorized(PhanQuyenNguCanh context) {
        if (context == null || context.roleCode() == null) {
            return false;
        }
        String role = context.roleCode().trim();
        return "PHARMACIST".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    private KetQua<ReceiveKhoTonKhoBienNhan> deny(
            ReceiveKhoTonKhoCommand command,
            KiemToanLyDoMa reasonCode) {
        PhanQuyenNguCanh context = command != null ? command.authorizationContext() : null;
        KiemToanTacNhanNguCanh auditActor = command != null ? command.auditActorContext() : null;

        Integer actorEmployeeId = context != null && context.employeeId() > 0 ? context.employeeId()
                : (auditActor != null && auditActor.employeeId() > 0 ? auditActor.employeeId() : null);
        Integer roleId = context != null && context.roleId() > 0 ? context.roleId()
                : (auditActor != null && auditActor.roleId() > 0 ? auditActor.roleId() : null);
        String roleCode = context != null && context.roleCode() != null && !context.roleCode().isBlank()
                ? context.roleCode()
                : (auditActor != null ? auditActor.roleCode() : null);
        if ((roleId == null) != (roleCode == null)) {
            roleId = null;
            roleCode = null;
        }
        Integer facilityId = context != null && context.facilityId() > 0 ? context.facilityId()
                : (auditActor != null && auditActor.facilityId() > 0 ? auditActor.facilityId() : null);
        Integer departmentId = context != null && context.departmentId() > 0 ? context.departmentId()
                : (auditActor != null && auditActor.departmentId() > 0 ? auditActor.departmentId() : null);
        String workstationId = auditActor != null ? auditActor.workstationId() : null;
        UUID correlationId = auditActor != null && auditActor.correlationId() != null ? auditActor.correlationId()
                : (context != null && context.sessionId() != null ? context.sessionId() : UUID.randomUUID());

        try {
            audit.appendIndependently(new HeThongKiemToanNhapLieu(
                    actorEmployeeId,
                    roleId,
                    roleCode,
                    facilityId,
                    departmentId,
                    KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                    null,
                    null,
                    KiemToanKetQua.DENIED,
                    reasonCode,
                    Instant.now(),
                    workstationId,
                    correlationId));
        } catch (RuntimeException ignored) {
            // A denial remains a denial even when audit write fails.
        }
        return KetQua.failure("AUTHORIZATION_DENIED", "Không có quyền thực hiện nhập kho.");
    }

    /** Hàm `logSuccessAudit`: nhatky thành công kiểm toán. */
    private void logSuccessAudit(ReceiveKhoTonKhoCommand command, int receiptId) {
        PhanQuyenNguCanh context = command.authorizationContext();
        KiemToanTacNhanNguCanh auditActor = command.auditActorContext();

        Integer actorEmployeeId = context != null && context.employeeId() > 0 ? context.employeeId()
                : (auditActor != null && auditActor.employeeId() > 0 ? auditActor.employeeId() : null);
        Integer roleId = context != null && context.roleId() > 0 ? context.roleId()
                : (auditActor != null && auditActor.roleId() > 0 ? auditActor.roleId() : null);
        String roleCode = context != null && context.roleCode() != null && !context.roleCode().isBlank()
                ? context.roleCode()
                : (auditActor != null ? auditActor.roleCode() : null);
        if ((roleId == null) != (roleCode == null)) {
            roleId = null;
            roleCode = null;
        }
        Integer facilityId = context != null && context.facilityId() > 0 ? context.facilityId()
                : (auditActor != null && auditActor.facilityId() > 0 ? auditActor.facilityId() : null);
        Integer departmentId = context != null && context.departmentId() > 0 ? context.departmentId()
                : (auditActor != null && auditActor.departmentId() > 0 ? auditActor.departmentId() : null);
        String workstationId = auditActor != null ? auditActor.workstationId() : null;
        UUID correlationId = auditActor != null && auditActor.correlationId() != null ? auditActor.correlationId()
                : (context != null && context.sessionId() != null ? context.sessionId() : UUID.randomUUID());

        Integer resourceId = receiptId > 0 ? receiptId : 1;

        try {
            audit.appendIndependently(new HeThongKiemToanNhapLieu(
                    actorEmployeeId,
                    roleId,
                    roleCode,
                    facilityId,
                    departmentId,
                    KiemToanHanhDongMa.INV_RECEIPT_CREATED,
                    KiemToanTaiNguyenLoai.INVENTORY_RECEIPT,
                    resourceId,
                    KiemToanKetQua.SUCCESS,
                    null,
                    Instant.now(),
                    workstationId,
                    correlationId));
        } catch (RuntimeException ignored) {
            // Independent audit failure does not prevent returning the successful receipt.
        }
    }
}
