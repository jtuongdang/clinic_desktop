package vn.clinicmanagement.application.billing.refund;

import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.billing.ClsVienPhiDongBo;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuHoanTra;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class RecordHoanTraHandler implements CommandHandler<RecordHoanTraCommand, RecordHoanTraBienNhan> {

    private static final Set<String> ALLOWED_ROLES = Set.of("CASHIER", "ADMIN");

    private final IHoaDonRepository invoiceRepository;
    private final KiemToanWriteCongKetNoi auditWritePort;
    private final GiaoDichRanhGioi transactions;

    public RecordHoanTraHandler(IHoaDonRepository invoiceRepository, KiemToanWriteCongKetNoi auditWritePort) {
        this(invoiceRepository, auditWritePort, new GiaoDichRanhGioi() {
            @Override
            public <T> KetQua<T> inTransaction(java.util.function.Supplier<KetQua<T>> operation) {
                return operation.get();
            }
        });
    }

    public RecordHoanTraHandler(
            IHoaDonRepository invoiceRepository,
            KiemToanWriteCongKetNoi auditWritePort,
            GiaoDichRanhGioi transactions
    ) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public KetQua<RecordHoanTraBienNhan> handle(RecordHoanTraCommand command) {
        return transactions.inTransaction(() -> handleInTransaction(command));
    }

    private KetQua<RecordHoanTraBienNhan> handleInTransaction(RecordHoanTraCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Lệnh hoàn trả viện phí không được để trống.");
        }
        PhanQuyenNguCanh auth = command.authorizationContext();
        if (auth == null || !auth.isValid() || !isRoleAllowed(auth.roleCode())) {
            logAudit(command.auditActorContext(), command.invoiceId(), KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN);
            return KetQua.failure("AUTHORIZATION_DENIED", "Tài khoản không có quyền chi hoàn tiền viện phí.");
        }
        if (command.invoiceId() <= 0) {
            return KetQua.failure("INVALID_INVOICE", "Mã hóa đơn không hợp lệ.");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return KetQua.failure("INVALID_AMOUNT", "Số tiền hoàn trả phải lớn hơn 0.");
        }
        if (command.amount().stripTrailingZeros().scale() > 2 || command.requestId() == null) {
            return KetQua.failure("INVALID_AMOUNT", "Số tiền chỉ được có tối đa 2 chữ số thập phân và yêu cầu phải có mã giao dịch.");
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

        PhuongThucThanhToan method = command.paymentMethod() != null ? command.paymentMethod() : PhuongThucThanhToan.TIEN_MAT;
        List<PhieuHoanTra> recordedRefunds = invoiceRepository.getPhieuHoanTraByHoaDon(command.invoiceId());
        var prior = recordedRefunds.stream()
                .filter(item -> command.requestId().equals(item.getRequestId()))
                .findFirst();
        if (prior.isPresent()) {
            PhieuHoanTra refund = prior.get();
            if (refund.getSoTien().compareTo(command.amount()) != 0 || refund.getPhuongThuc() != method
                    || refund.getNguoiHoan() != auth.employeeId()) {
                return KetQua.failure("REFUND_REQUEST_CONFLICT", "Mã giao dịch đã được sử dụng cho một phiếu hoàn trả khác.");
            }
            BigDecimal remaining = hoaDon.getRemainingAmount();
            BigDecimal pending = hoaDon.getPendingRefundAmount();
            return KetQua.success(new RecordHoanTraBienNhan(
                    refund.getId() != null ? refund.getId() : 0,
                    hoaDon.getId(),
                    refund.getSoTien(),
                    hoaDon.getDaThanhToan(),
                    hoaDon.getSoTienHoanTra(),
                    pending,
                    remaining,
                    hoaDon.getTrangThai(),
                    remaining.signum() > 0 ? TrangThaiCongNo.DANG_NO : TrangThaiCongNo.DA_TAT_TOAN,
                    refund.getNgayHoan()
            ));
        }

        if (hoaDon.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
            return KetQua.failure("INVOICE_CANCELLED", "Hóa đơn đã bị hủy, không thể chi hoàn tiền.");
        }

        BigDecimal receiptTotal = invoiceRepository.getPhieuThuByHoaDon(command.invoiceId()).stream()
                .map(PhieuThu::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundTotal = recordedRefunds.stream()
                .map(PhieuHoanTra::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (receiptTotal.subtract(refundTotal).compareTo(hoaDon.getDaThanhToan()) != 0) {
            return KetQua.failure("BILLING_BALANCE_MISMATCH", "Số đã thu chưa khớp lịch sử phiếu thu/hoàn trả. Vui lòng đối soát trước khi chi hoàn.");
        }

        BigDecimal pendingBefore = hoaDon.getPendingRefundAmount();
        if (pendingBefore.signum() <= 0) {
            return KetQua.failure("NO_REFUND_DUE", "Hóa đơn không có khoản tiền dư cần hoàn trả.");
        }
        if (command.amount().compareTo(pendingBefore) > 0) {
            return KetQua.failure("EXCESS_REFUND",
                    String.format("Số tiền hoàn trả (%s) vượt quá số tiền cần hoàn (%s).",
                            command.amount().toPlainString(), pendingBefore.toPlainString()));
        }

        String reason = command.reason() != null && !command.reason().isBlank()
                ? command.reason().trim()
                : "Hoàn tiền chênh lệch viện phí tại quầy thu ngân";
        PhieuHoanTra latestRefund = hoaDon.recordRefund(command.amount(), method, auth.employeeId(), reason);

        hoaDon.markAsUpdated(auth.employeeId());
        invoiceRepository.save(hoaDon);

        latestRefund.setMaHoaDon(hoaDon.getId());
        latestRefund.setRequestId(command.requestId());
        latestRefund.setCreatedBy(auth.employeeId());
        invoiceRepository.savePhieuHoanTra(latestRefund);

        TrangThaiCongNo debtStatus = ClsVienPhiDongBo.synchronizeDebt(invoiceRepository, hoaDon, auth.employeeId(), null);
        logAudit(command.auditActorContext(), command.invoiceId(), KiemToanKetQua.SUCCESS, null);

        int refundId = latestRefund.getId() != null ? latestRefund.getId() : 0;
        return KetQua.success(new RecordHoanTraBienNhan(
                refundId,
                hoaDon.getId(),
                command.amount(),
                hoaDon.getDaThanhToan(),
                hoaDon.getSoTienHoanTra(),
                hoaDon.getPendingRefundAmount(),
                hoaDon.getRemainingAmount(),
                hoaDon.getTrangThai(),
                debtStatus,
                latestRefund.getNgayHoan() != null ? latestRefund.getNgayHoan() : LocalDateTime.now()
        ));
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
                KiemToanHanhDongMa.BILL_REFUND_ISSUED,
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
