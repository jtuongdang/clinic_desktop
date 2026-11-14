package vn.clinicmanagement.application.billing;

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

    /** Trường `invoiceRepository`: hóa đơn kho dữ liệu. */
    private final IHoaDonRepository invoiceRepository;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public RecordThanhToanHandler(IHoaDonRepository invoiceRepository, KiemToanWriteCongKetNoi auditWritePort) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public KetQua<RecordThanhToanBienNhan> handle(RecordThanhToanCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Lệnh thu tiền viện phí không được để trống.");
        }

        // 1. Phân quyền vai trò
        PhanQuyenNguCanh auth = command.authorizationContext();
        if (auth == null || !isRoleAllowed(auth.roleCode())) {
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

        // 3. Tìm hóa đơn
        Optional<HoaDon> invoiceOpt = invoiceRepository.getById(command.invoiceId());
        if (invoiceOpt.isEmpty()) {
            return KetQua.failure("INVOICE_NOT_FOUND", "Không tìm thấy hóa đơn mã: " + command.invoiceId());
        }

        HoaDon hoaDon = invoiceOpt.get();

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

        PhuongThucThanhToan method = command.paymentMethod() != null ? command.paymentMethod() : PhuongThucThanhToan.TIEN_MAT;
        String notes = command.notes() != null ? command.notes() : "";

        // 5. Ghi nhận thanh toán vào domain aggregate
        hoaDon.recordPayment(command.amount(), method, auth.employeeId(), notes);

        // Lưu phiếu thu
        List<PhieuThu> receipts = hoaDon.getPhieuThus();
        PhieuThu latestReceipt = !receipts.isEmpty() ? receipts.get(receipts.size() - 1) : null;
        if (latestReceipt != null) {
            invoiceRepository.savePhieuThu(latestReceipt);
        }

        // 6. Xử lý công nợ bệnh nhân
        BigDecimal remainingAfter = hoaDon.getSoTienBenhNhanChiTra().subtract(hoaDon.getDaThanhToan());
        TrangThaiCongNo debtStatus;

        Optional<CongNoBenhNhan> debtOpt = invoiceRepository.getCongNoByHoaDon(command.invoiceId());
        if (remainingAfter.compareTo(BigDecimal.ZERO) > 0) {
            debtStatus = TrangThaiCongNo.DANG_NO;
            CongNoBenhNhan congNo;
            if (debtOpt.isPresent()) {
                congNo = debtOpt.get();
                congNo.setSoTienDaTra(hoaDon.getDaThanhToan());
                congNo.setTrangThai(TrangThaiCongNo.DANG_NO);
            } else {
                congNo = new CongNoBenhNhan();
                congNo.setMaHoaDon(command.invoiceId());
                congNo.setMaBenhNhan(hoaDon.getMaBenhNhan());
                congNo.setSoTienNo(hoaDon.getSoTienBenhNhanChiTra());
                congNo.setSoTienDaTra(hoaDon.getDaThanhToan());
                congNo.setHanThanhToan(command.debtDueDate() != null ? command.debtDueDate() : LocalDate.now().plusDays(30));
                congNo.setTrangThai(TrangThaiCongNo.DANG_NO);
            }
            invoiceRepository.saveCongNo(congNo);
        } else {
            debtStatus = TrangThaiCongNo.DA_TAT_TOAN;
            if (debtOpt.isPresent()) {
                CongNoBenhNhan congNo = debtOpt.get();
                congNo.setSoTienDaTra(hoaDon.getDaThanhToan());
                congNo.setTrangThai(TrangThaiCongNo.DA_TAT_TOAN);
                invoiceRepository.saveCongNo(congNo);
            }
        }

        // Lưu trạng thái hóa đơn cập nhật
        invoiceRepository.save(hoaDon);

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

    /** Hàm `isRoleAllowed`: là vai trò `Allowed`. */
    private boolean isRoleAllowed(String roleCode) {
        if (roleCode == null) return false;
        return ALLOWED_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    /** Hàm `logAudit`: nhatky kiểm toán. */
    private void logAudit(KiemToanTacNhanNguCanh actor, int invoiceId, KiemToanKetQua outcome, KiemToanLyDoMa reason, String note) {
        if (actor == null) return;
        KiemToanTaiNguyenLoai resourceType = invoiceId > 0 ? KiemToanTaiNguyenLoai.INVOICE : null;
        Integer resourceId = invoiceId > 0 ? invoiceId : null;
        auditWritePort.appendIndependently(new HeThongKiemToanNhapLieu(
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
        ));
    }
}
