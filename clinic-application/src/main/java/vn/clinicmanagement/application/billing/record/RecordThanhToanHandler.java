package vn.clinicmanagement.application.billing.record;

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
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RecordThanhToanHandler implements CommandHandler<RecordThanhToanCommand, RecordThanhToanBienNhan> {

    private static final Set<String> ALLOWED_ROLES = Set.of("CASHIER", "ADMIN");

    private final IHoaDonRepository invoiceRepository;
    private final KiemToanWriteCongKetNoi auditWritePort;
    private final GiaoDichRanhGioi transactions;

    public RecordThanhToanHandler(IHoaDonRepository invoiceRepository, KiemToanWriteCongKetNoi auditWritePort) {
        this(invoiceRepository, auditWritePort, new GiaoDichRanhGioi() {
            @Override
            public <T> KetQua<T> inTransaction(java.util.function.Supplier<KetQua<T>> operation) {
                return operation.get();
            }
        });
    }

    public RecordThanhToanHandler(IHoaDonRepository invoiceRepository, KiemToanWriteCongKetNoi auditWritePort,
                                 GiaoDichRanhGioi transactions) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public KetQua<RecordThanhToanBienNhan> handle(RecordThanhToanCommand command) {
        return transactions.inTransaction(() -> handleInTransaction(command));
    }

    private KetQua<RecordThanhToanBienNhan> handleInTransaction(RecordThanhToanCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Lệnh thu tiền viện phí không được để trống.");
        }

        // 1. Phân quyền vai trò
        PhanQuyenNguCanh auth = command.authorizationContext();
        if (auth == null || !auth.isValid() || !isRoleAllowed(auth.roleCode())) {
            logAudit(command.auditActorContext(), command.invoiceId(),
                    KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN, "Không có quyền thu viện phí.");
            return KetQua.failure("AUTHORIZATION_DENIED", "Tài khoản không có quyền thu viện phí.");
        }

        // 2. Kiểm tra dữ liệu đầu vào
        if (command.invoiceId() <= 0) {
            return KetQua.failure("INVALID_INVOICE", "Mã hóa đơn không hợp lệ.");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return KetQua.failure("INVALID_AMOUNT", "Số tiền thanh toán phải lớn hơn 0.");
        }

        if (command.amount().stripTrailingZeros().scale() > 2 || command.requestId() == null) {
            return KetQua.failure("INVALID_AMOUNT", "Số tiền chỉ được có tối đa 2 chữ số thập phân và yêu cầu phải có mã giao dịch.");
        }

        // 3. Tìm hóa đơn
        Optional<HoaDon> invoiceOpt = invoiceRepository.getById(command.invoiceId());
        if (invoiceOpt.isEmpty()) {
            return KetQua.failure("INVOICE_NOT_FOUND", "Không tìm thấy hóa đơn mã: " + command.invoiceId());
        }

        HoaDon hoaDon = invoiceOpt.get();

        if (hoaDon.getMaCoSo() != auth.facilityId()) {
            return KetQua.failure("AUTHORIZATION_DENIED", "Hóa đơn không thuộc cơ sở đang làm việc.");
        }
        PhuongThucThanhToan method = command.paymentMethod() != null ? command.paymentMethod() : PhuongThucThanhToan.TIEN_MAT;
        var recordedReceipts = invoiceRepository.getPhieuThuByHoaDon(command.invoiceId());
        var prior = recordedReceipts.stream()
                .filter(item -> command.requestId().equals(item.getRequestId())).findFirst();
        if (prior.isPresent()) {
            PhieuThu receipt = prior.get();
            if (receipt.getSoTien().compareTo(command.amount()) != 0 || receipt.getPhuongThuc() != method
                    || receipt.getNguoiThu() != auth.employeeId()) {
                return KetQua.failure("PAYMENT_REQUEST_CONFLICT", "Mã giao dịch đã được sử dụng cho một khoản thu khác.");
            }
            BigDecimal remaining = hoaDon.getSoTienBenhNhanChiTra().subtract(hoaDon.getDaThanhToan()).max(BigDecimal.ZERO);
            return KetQua.success(new RecordThanhToanBienNhan(receipt.getId(), hoaDon.getId(), receipt.getSoTien(),
                    hoaDon.getDaThanhToan(), remaining, hoaDon.getTrangThai(),
                    remaining.signum() > 0 ? TrangThaiCongNo.DANG_NO : TrangThaiCongNo.DA_TAT_TOAN, receipt.getNgayThu()));
        }

        // 4. Kiểm tra trạng thái hóa đơn
        if (hoaDon.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
            return KetQua.failure("INVOICE_CANCELLED", "Hóa đơn đã bị hủy, không thể thu tiền.");
        }
        if (hoaDon.getTrangThai() == TrangThaiHoaDon.DA_THANH_TOAN) {
            return KetQua.failure("ALREADY_PAID", "Hóa đơn đã được thanh toán toàn bộ trước đó.");
        }

        BigDecimal remainingBefore = hoaDon.getSoTienBenhNhanChiTra().subtract(hoaDon.getDaThanhToan());
        if (command.amount().compareTo(remainingBefore) > 0) {
            return KetQua.failure("OVERPAYMENT",
                    String.format("Số tiền thanh toán (%s) vượt quá số tiền còn nợ (%s).",
                            command.amount().toPlainString(), remainingBefore.toPlainString()));
        }

        BigDecimal receiptTotal = recordedReceipts.stream().map(PhieuThu::getSoTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundTotal = invoiceRepository.getPhieuHoanTraByHoaDon(command.invoiceId()).stream()
                .map(vn.clinicmanagement.domain.billing.PhieuHoanTra::getSoTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (receiptTotal.subtract(refundTotal).compareTo(hoaDon.getDaThanhToan()) != 0) {
            return KetQua.failure("BILLING_BALANCE_MISMATCH", "Số đã thu chưa khớp lịch sử phiếu thu. Vui lòng đối soát trước khi thu tiếp.");
        }
        String notes = command.notes() != null ? command.notes() : "";

        // 5. Ghi nhận thanh toán vào domain aggregate
        hoaDon.recordPayment(command.amount(), method, auth.employeeId(), notes);

        // Serialize all charge/payment writes through the invoice version before inserting cash records.
        hoaDon.markAsUpdated(auth.employeeId());
        invoiceRepository.save(hoaDon);
        List<PhieuThu> receipts = hoaDon.getPhieuThus();
        PhieuThu latestReceipt = receipts.get(receipts.size() - 1);
        latestReceipt.setRequestId(command.requestId());
        latestReceipt.setCreatedBy(auth.employeeId());
        invoiceRepository.savePhieuThu(latestReceipt);

        BigDecimal remainingAfter = hoaDon.getSoTienBenhNhanChiTra().subtract(hoaDon.getDaThanhToan()).max(BigDecimal.ZERO);
        TrangThaiCongNo debtStatus = vn.clinicmanagement.application.billing.ClsVienPhiDongBo.synchronizeDebt(
                invoiceRepository, hoaDon, auth.employeeId(), command.debtDueDate());

        // 7. Ghi nhật ký kiểm toán
        logAudit(command.auditActorContext(), command.invoiceId(),
                KiemToanKetQua.SUCCESS, null,
                String.format("Thu tiền viện phí %s VNĐ bằng %s thành công.", command.amount().toPlainString(), method.getDescription()));

        int receiptId = latestReceipt != null && latestReceipt.getId() != null ? latestReceipt.getId() : 0;
        RecordThanhToanBienNhan receipt = new RecordThanhToanBienNhan(
                receiptId,
                command.invoiceId(),
                command.amount(),
                hoaDon.getDaThanhToan(),
                remainingAfter,
                hoaDon.getTrangThai(),
                debtStatus,
                latestReceipt != null ? latestReceipt.getNgayThu() : LocalDateTime.now()
        );

        return KetQua.success(receipt);
    }

    private boolean isRoleAllowed(String roleCode) {
        if (roleCode == null) return false;
        return ALLOWED_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    private void logAudit(KiemToanTacNhanNguCanh actor, int invoiceId, KiemToanKetQua outcome, KiemToanLyDoMa reason, String note) {
        if (actor == null) return;
        KiemToanTaiNguyenLoai resourceType = invoiceId > 0 ? KiemToanTaiNguyenLoai.INVOICE : null;
        Integer resourceId = invoiceId > 0 ? invoiceId : null;
        HeThongKiemToanNhapLieu entry = new HeThongKiemToanNhapLieu(
                actor.employeeId(),
                actor.roleId(),
                actor.roleCode() != null ? actor.roleCode().trim().toUpperCase(Locale.ROOT) : null,
                actor.facilityId(),
                actor.departmentId(),
                KiemToanHanhDongMa.BILL_PAYMENT_COLLECTED,
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
