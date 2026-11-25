package vn.clinicmanagement.application.billing.payer;

import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.billing.ClsVienPhiDongBo;
import vn.clinicmanagement.application.billing.ThanhToanDichVuReadCongKetNoi;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuHoanTra;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ChangeChiTietHoaDonDoiTuongHandler
        implements CommandHandler<ChangeChiTietHoaDonDoiTuongCommand, ChangeChiTietHoaDonDoiTuongBienNhan> {

    private static final Set<String> ALLOWED_ROLES = Set.of("CASHIER", "ADMIN", "DOCTOR", "RECEPTIONIST");
    private static final Set<String> SUPPORTED_PAYERS = Set.of("BHYT", "BHYT_DICH_VU", "THU_PHI", "MIEN_PHI");

    private final IHoaDonRepository invoiceRepository;
    private final ThanhToanDichVuReadCongKetNoi serviceReadPort;
    private final KiemToanWriteCongKetNoi auditWritePort;
    private final GiaoDichRanhGioi transactions;

    public ChangeChiTietHoaDonDoiTuongHandler(
            IHoaDonRepository invoiceRepository,
            ThanhToanDichVuReadCongKetNoi serviceReadPort,
            KiemToanWriteCongKetNoi auditWritePort
    ) {
        this(invoiceRepository, serviceReadPort, auditWritePort, new GiaoDichRanhGioi() {
            @Override
            public <T> KetQua<T> inTransaction(java.util.function.Supplier<KetQua<T>> operation) {
                return operation.get();
            }
        });
    }

    public ChangeChiTietHoaDonDoiTuongHandler(
            IHoaDonRepository invoiceRepository,
            ThanhToanDichVuReadCongKetNoi serviceReadPort,
            KiemToanWriteCongKetNoi auditWritePort,
            GiaoDichRanhGioi transactions
    ) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        this.serviceReadPort = Objects.requireNonNull(serviceReadPort, "serviceReadPort");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public KetQua<ChangeChiTietHoaDonDoiTuongBienNhan> handle(ChangeChiTietHoaDonDoiTuongCommand command) {
        return transactions.inTransaction(() -> handleInTransaction(command));
    }

    private KetQua<ChangeChiTietHoaDonDoiTuongBienNhan> handleInTransaction(ChangeChiTietHoaDonDoiTuongCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Lệnh chuyển đối tượng thanh toán không được để trống.");
        }
        PhanQuyenNguCanh auth = command.authorizationContext();
        if (auth == null || !auth.isValid() || !isRoleAllowed(auth.roleCode())) {
            logAudit(command.auditActorContext(), command.invoiceId(), KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN);
            return KetQua.failure("AUTHORIZATION_DENIED", "Tài khoản không có quyền chuyển đối tượng thanh toán dịch vụ.");
        }
        if (command.invoiceId() <= 0 || command.lineId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Mã hóa đơn và mã dòng dịch vụ không hợp lệ.");
        }
        String targetPayer = command.targetPayerType() == null
                ? ""
                : command.targetPayerType().trim().toUpperCase(Locale.ROOT);
        if ("DICH_VU".equals(targetPayer) || "VIEN_PHI".equals(targetPayer)) {
            targetPayer = "THU_PHI";
        }
        if (!SUPPORTED_PAYERS.contains(targetPayer)) {
            return KetQua.failure("VALIDATION_ERROR", "Đối tượng thanh toán không hợp lệ: " + command.targetPayerType());
        }

        Optional<HoaDon> invoiceOpt = invoiceRepository.getById(command.invoiceId());
        if (invoiceOpt.isEmpty()) {
            return KetQua.failure("INVOICE_NOT_FOUND", "Không tìm thấy hóa đơn mã: " + command.invoiceId());
        }
        HoaDon hoaDon = invoiceOpt.get();
        if (hoaDon.getMaCoSo() != auth.facilityId()) {
            logAudit(command.auditActorContext(), command.invoiceId(), KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN);
            return KetQua.failure("AUTHORIZATION_DENIED", "Hóa đơn không thuộc cơ sở đang làm việc.");
        }
        if (hoaDon.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
            return KetQua.failure("INVOICE_CANCELLED", "Hóa đơn đã hủy, không thể chuyển đối tượng dịch vụ.");
        }
        if (hoaDon.getSoHoaDonDienTu() != null && !hoaDon.getSoHoaDonDienTu().isBlank()) {
            return KetQua.failure("INVOICE_FINALIZED", "Hóa đơn đã phát hành điện tử, không thể chỉnh sửa đối tượng dịch vụ.");
        }

        BigDecimal receiptTotal = invoiceRepository.getPhieuThuByHoaDon(hoaDon.getId()).stream()
                .map(PhieuThu::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundTotal = invoiceRepository.getPhieuHoanTraByHoaDon(hoaDon.getId()).stream()
                .map(PhieuHoanTra::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (receiptTotal.subtract(refundTotal).compareTo(hoaDon.getDaThanhToan()) != 0) {
            return KetQua.failure("BILLING_BALANCE_MISMATCH", "Số đã thu chưa khớp lịch sử phiếu thu/hoàn trả. Vui lòng đối soát tại Thu ngân.");
        }

        ChiTietHoaDon line = hoaDon.getChiTietHoaDons().stream()
                .filter(item -> item != null && !item.isDeleted() && item.getId() != null && item.getId() == command.lineId())
                .findFirst()
                .orElse(null);
        if (line == null) {
            return KetQua.failure("LINE_NOT_FOUND", "Không tìm thấy dòng dịch vụ cần chuyển đối tượng.");
        }

        Map<Integer, ThanhToanDichVuReadCongKetNoi.NguonDong> sources =
                serviceReadPort.getByInvoice(hoaDon.getId(), hoaDon.getMaLuotKham(), auth.facilityId());
        ThanhToanDichVuReadCongKetNoi.NguonDong source = sources != null ? sources.get(line.getId()) : null;

        BigDecimal servicePrice = firstPositive(
                source != null ? source.catalogServicePrice() : null,
                command.fallbackServicePrice(),
                line.getDonGia());
        BigDecimal bhytPrice = firstPositive(
                source != null ? source.catalogInsurancePrice() : null,
                command.fallbackInsurancePrice(),
                line.getGiaBHYT());

        int coverageRate = 0;
        if ("BHYT".equals(targetPayer) || "BHYT_DICH_VU".equals(targetPayer)) {
            if (bhytPrice.signum() <= 0) {
                return KetQua.failure("BHYT_PRICE_MISSING",
                        "Dịch vụ/thuốc \"" + line.getMoTa() + "\" không có đơn giá BHYT trong danh mục.");
            }
            LocalDate refDate = hoaDon.getNgayLap() != null ? hoaDon.getNgayLap().toLocalDate() : LocalDate.now();
            Optional<ThanhToanDichVuReadCongKetNoi.ThongTinBaoHiemBenhNhan> cardOpt =
                    serviceReadPort.getActiveInsuranceByPatient(hoaDon.getMaBenhNhan());
            if (cardOpt.isPresent()) {
                var card = cardOpt.get();
                if (!card.isValidOn(refDate) || card.mucHuong() <= 0) {
                    return KetQua.failure("BHYT_CARD_EXPIRED",
                            "Thẻ BHYT của bệnh nhân đã hết hạn hoặc chưa đến ngày hiệu lực.");
                }
                coverageRate = card.mucHuong();
            } else if (command.overrideCoverageRate() != null && command.overrideCoverageRate() > 0) {
                coverageRate = command.overrideCoverageRate();
            } else {
                return KetQua.failure("BHYT_CARD_MISSING",
                        "Bệnh nhân chưa có thông tin thẻ BHYT hợp lệ để chuyển sang đối tượng Bảo hiểm.");
            }
        }

        String previousPayer = line.getDoiTuongThanhToan();
        line.applyPayerRule(targetPayer, servicePrice, bhytPrice, coverageRate);
        hoaDon.recalculateAfterPayerChange();
        hoaDon.markAsUpdated(auth.employeeId());
        invoiceRepository.save(hoaDon);
        ClsVienPhiDongBo.synchronizeDebt(invoiceRepository, hoaDon, auth.employeeId(), null);

        logAudit(command.auditActorContext(), hoaDon.getId(), KiemToanKetQua.SUCCESS, null);

        return KetQua.success(new ChangeChiTietHoaDonDoiTuongBienNhan(
                hoaDon.getId(),
                line.getId(),
                previousPayer,
                line.getDoiTuongThanhToan(),
                hoaDon.getTongTien(),
                hoaDon.getSoTienBaoHiemChiTra(),
                hoaDon.getTotalServiceDiscountAmount(),
                hoaDon.getSoTienBenhNhanChiTra(),
                hoaDon.getDaThanhToan(),
                hoaDon.getRemainingAmount(),
                hoaDon.getPendingRefundAmount(),
                hoaDon.getTrangThai()
        ));
    }

    private static BigDecimal firstPositive(BigDecimal... candidates) {
        if (candidates != null) {
            for (BigDecimal c : candidates) {
                if (c != null && c.signum() > 0) return c;
            }
        }
        return BigDecimal.ZERO;
    }

    private boolean isRoleAllowed(String roleCode) {
        if (roleCode == null) return false;
        return ALLOWED_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    private void logAudit(KiemToanTacNhanNguCanh actor, int invoiceId, KiemToanKetQua outcome, KiemToanLyDoMa reason) {
        if (actor == null) return;
        KiemToanTaiNguyenLoai resourceType = invoiceId > 0 ? KiemToanTaiNguyenLoai.INVOICE : null;
        Integer resourceId = invoiceId > 0 ? invoiceId : null;
        HeThongKiemToanNhapLieu entry = new HeThongKiemToanNhapLieu(
                actor.employeeId(),
                actor.roleId(),
                actor.roleCode() != null ? actor.roleCode().trim().toUpperCase(Locale.ROOT) : null,
                actor.facilityId(),
                actor.departmentId(),
                KiemToanHanhDongMa.BILL_DISCOUNT_APPLIED,
                resourceType,
                resourceId,
                outcome,
                reason,
                Instant.now(),
                actor.workstationId(),
                actor.correlationId()
        );
        if (outcome == KiemToanKetQua.SUCCESS) auditWritePort.appendInCurrentTransaction(entry);
        else auditWritePort.appendIndependently(entry);
    }
}
