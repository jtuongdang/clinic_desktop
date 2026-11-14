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
import vn.clinicmanagement.domain.common.enums.LoaiXuatKho;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.inventory.ChiTietXuatKho;
import vn.clinicmanagement.domain.inventory.FEFOPhanBoDichVu;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DispenseDonThuocHandler
        implements CommandHandler<DispenseDonThuocCommand, DispenseDonThuocBienNhan> {

    private static final Set<String> ALLOWED_ROLES = Set.of("PHARMACIST", "ADMIN");

    /** Trường `prescriptionRepository`: đơn thuốc kho dữ liệu. */
    private final IDonThuocRepository prescriptionRepository;
    /** Trường `inventoryRepository`: kho kho dữ liệu. */
    private final IKhoRepository inventoryRepository;
    /** Trường `fefoService`: fefo dịch vụ. */
    private final FEFOPhanBoDichVu fefoService;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public DispenseDonThuocHandler(
            IDonThuocRepository prescriptionRepository,
            IKhoRepository inventoryRepository,
            FEFOPhanBoDichVu fefoService,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit) {
        this.prescriptionRepository = Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.fefoService = Objects.requireNonNull(fefoService, "fefoService");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<DispenseDonThuocBienNhan> handle(DispenseDonThuocCommand command) {
        if (command == null
                || command.prescriptionId() <= 0
                || command.pharmacistId() <= 0
                || command.warehouseId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin cấp phát thuốc không hợp lệ.");
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        if (!isAuthorized(context)) {
            deny(context, command.auditActorContext());
            return KetQua.failure("AUTH_ACCESS_DENIED", "Không có quyền cấp phát đơn thuốc trong phiên hiện tại.");
        }

        Optional<DonThuoc> prescriptionOpt = prescriptionRepository.getById(command.prescriptionId());
        if (prescriptionOpt.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy đơn thuốc.");
        }

        DonThuoc prescription = prescriptionOpt.get();
        if (prescription.getTrangThai() != TrangThaiDonThuoc.DA_DUYET) {
            return KetQua.failure("INVALID_STATE", "Chỉ có thể cấp phát đơn thuốc đã được duyệt.");
        }

        Optional<Kho> warehouseOpt = inventoryRepository.getKhoById(command.warehouseId());
        if (warehouseOpt.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy kho dược.");
        }

        KetQua<DispenseDonThuocBienNhan> txResult = transactions.inTransaction(() -> {
            List<DaCapPhatMatHangPhanBoDuLieu> allocations = new ArrayList<>();
            List<ChiTietXuatKho> chiTietXuatKhos = new ArrayList<>();
            BigDecimal[] tongTienHolder = new BigDecimal[]{BigDecimal.ZERO};

            List<ChiTietDonThuoc> items = prescription.getChiTietDonThuocs() != null
                    ? prescription.getChiTietDonThuocs()
                    : List.of();

            for (ChiTietDonThuoc item : items) {
                BigDecimal soLuongKe = item.getSoLuongKe() != null ? item.getSoLuongKe() : BigDecimal.ZERO;
                BigDecimal soLuongDaXuat = item.getSoLuongDaXuat() != null ? item.getSoLuongDaXuat() : BigDecimal.ZERO;
                BigDecimal neededQty = soLuongKe.subtract(soLuongDaXuat);

                if (neededQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                List<TonKho> batches = inventoryRepository.getAvailableBatches(command.warehouseId(), item.getMaItem());
                List<FEFOPhanBoDichVu.AllocationLine> allocatedLines;
                try {
                    allocatedLines = fefoService.allocateFefo(batches, neededQty);
                } catch (IllegalStateException ex) {
                    return KetQua.failure("INSUFFICIENT_STOCK", ex.getMessage());
                }

                for (FEFOPhanBoDichVu.AllocationLine alloc : allocatedLines) {
                    TonKho tonKho = alloc.batch();
                    BigDecimal takeQty = alloc.quantityToTake();
                    tonKho.setSoLuongTon(tonKho.getSoLuongTon().subtract(takeQty));
                    inventoryRepository.saveTonKho(tonKho);

                    int tonKhoId = tonKho.getId() != null ? tonKho.getId() : 0;
                    allocations.add(new DaCapPhatMatHangPhanBoDuLieu(
                            item.getMaItem(),
                            tonKhoId,
                            tonKho.getSoLo(),
                            takeQty));

                    BigDecimal unitPrice = item.getDonGiaKe() != null && item.getDonGiaKe().compareTo(BigDecimal.ZERO) > 0
                            ? item.getDonGiaKe()
                            : (tonKho.getGiaBan() != null ? tonKho.getGiaBan() : BigDecimal.ZERO);
                    tongTienHolder[0] = tongTienHolder[0].add(unitPrice.multiply(takeQty));

                    ChiTietXuatKho ctxk = new ChiTietXuatKho();
                    ctxk.setMaItem(item.getMaItem());
                    ctxk.setMaTonKho(tonKhoId);
                    ctxk.setSoLuong(takeQty);
                    ctxk.setDonGia(unitPrice);
                    chiTietXuatKhos.add(ctxk);
                }

                item.setSoLuongDaXuat(item.getSoLuongKe());
            }

            PhieuXuatKho phieuXuat = new PhieuXuatKho();
            phieuXuat.setMaKhoXuat(command.warehouseId());
            phieuXuat.setLoaiXuat(LoaiXuatKho.BAN_THUOC);
            phieuXuat.setMaDonThuoc(command.prescriptionId());
            phieuXuat.setTongTien(tongTienHolder[0]);
            phieuXuat.setChiTiets(chiTietXuatKhos);
            phieuXuat.setSoPhieuXuat("PXK-" + command.prescriptionId());

            PhieuXuatKho savedPhieuXuat = inventoryRepository.savePhieuXuatKho(phieuXuat);
            int issueReceiptId = (savedPhieuXuat != null && savedPhieuXuat.getId() != null)
                    ? savedPhieuXuat.getId()
                    : 0;
            String soPhieuXuat = (savedPhieuXuat != null && savedPhieuXuat.getSoPhieuXuat() != null && !savedPhieuXuat.getSoPhieuXuat().isBlank())
                    ? savedPhieuXuat.getSoPhieuXuat()
                    : phieuXuat.getSoPhieuXuat();

            prescription.completeDispense(command.pharmacistId());
            prescriptionRepository.update(prescription);

            return KetQua.success(new DispenseDonThuocBienNhan(
                    command.prescriptionId(),
                    issueReceiptId,
                    soPhieuXuat,
                    tongTienHolder[0],
                    allocations));
        });

        if (txResult.isFailure()) {
            return txResult;
        }

        DispenseDonThuocBienNhan receipt = txResult.valueOrThrow();
        int prescriptionId = prescription.getId() != null ? prescription.getId() : command.prescriptionId();
        appendSuccessAudit(context, command.auditActorContext(), prescriptionId);
        return KetQua.success(receipt);
    }

    /** Hàm `isAuthorized`: là `Authorized`. */
    private static boolean isAuthorized(PhanQuyenNguCanh context) {
        if (context == null || !context.isValid() || context.roleCode() == null) {
            return false;
        }
        return ALLOWED_ROLES.contains(context.roleCode().trim().toUpperCase(Locale.ROOT));
    }

    private void deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                validContext ? context.employeeId() : null,
                validContext ? context.roleId() : null,
                validContext ? context.roleCode().trim().toUpperCase(Locale.ROOT) : null,
                validContext ? context.facilityId() : null,
                validContext ? context.departmentId() : null,
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
    private void appendSuccessAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, int prescriptionId) {
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode().trim().toUpperCase(Locale.ROOT),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.RX_DISPENSED,
                KiemToanTaiNguyenLoai.PRESCRIPTION,
                prescriptionId,
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    /** Hàm `correlationId`: `correlation` mã. */
    private static UUID correlationId(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        if (actor != null && actor.correlationId() != null) {
            return actor.correlationId();
        }
        if (context != null && context.sessionId() != null) {
            return context.sessionId();
        }
        return UUID.randomUUID();
    }
}
