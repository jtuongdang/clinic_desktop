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
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CreateHoaDonHandler implements CommandHandler<CreateHoaDonCommand, CreateHoaDonBienNhan> {

    private static final Set<String> ALLOWED_ROLES = Set.of("CASHIER", "ADMIN");

    /** Trường `invoiceRepository`: hóa đơn kho dữ liệu. */
    private final IHoaDonRepository invoiceRepository;
    /** Trường `visitRepository`: luotkham kho dữ liệu. */
    private final ILuotKhamRepository visitRepository;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public CreateHoaDonHandler(
            IHoaDonRepository invoiceRepository,
            ILuotKhamRepository visitRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public KetQua<CreateHoaDonBienNhan> handle(CreateHoaDonCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Lệnh tạo hóa đơn không được để trống.");
        }

        // 1. Phân quyền vai trò
        PhanQuyenNguCanh auth = command.authorizationContext();
        if (auth == null || !isRoleAllowed(auth.roleCode())) {
            logAudit(command.auditActorContext(), 0, KiemToanHanhDongMa.BILL_INVOICE_CREATED,
                    KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN, "Không có quyền lập hóa đơn viện phí.");
            return KetQua.failure("AUTHORIZATION_DENIED", "Tài khoản không có quyền lập hóa đơn viện phí.");
        }

        // 2. Kiểm tra dữ liệu đầu vào
        if (command.visitId() <= 0) {
            return KetQua.failure("INVALID_VISIT", "Mã lượt khám không hợp lệ.");
        }
        if (command.patientId() <= 0) {
            return KetQua.failure("INVALID_PATIENT", "Mã bệnh nhân không hợp lệ.");
        }
        if (command.facilityId() <= 0) {
            return KetQua.failure("INVALID_FACILITY", "Mã cơ sở không hợp lệ.");
        }
        if (command.items() == null || command.items().isEmpty()) {
            return KetQua.failure("EMPTY_ITEMS", "Danh sách chi tiết hóa đơn không được để trống.");
        }

        // 3. Kiểm tra sự tồn tại của lượt khám
        Optional<LuotKham> visitOpt = visitRepository.getById(command.visitId());
        if (visitOpt.isEmpty()) {
            return KetQua.failure("VISIT_NOT_FOUND", "Không tìm thấy lượt khám với mã: " + command.visitId());
        }

        // 4. Kiểm tra xem lượt khám đã có hóa đơn chưa (mỗi lượt khám 1 hóa đơn chính)
        Optional<HoaDon> existingInvoice = invoiceRepository.getByLuotKham(command.visitId());
        if (existingInvoice.isPresent() && existingInvoice.get().getTrangThai() != TrangThaiHoaDon.DA_HUY) {
            return KetQua.failure("INVOICE_ALREADY_EXISTS",
                    "Lượt khám đã có hóa đơn viện phí mã: " + existingInvoice.get().getSoHoaDon());
        }

        // 5. Tính toán chi phí từng dòng & tổng cộng
        BigDecimal tongTien = BigDecimal.ZERO;
        for (HoaDonMatHangDauVao item : command.items()) {
            if (item == null) {
                return KetQua.failure("INVALID_ITEM", "Dòng chi tiết chi phí không được null.");
            }
            if (item.quantity() == null || item.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                return KetQua.failure("INVALID_QUANTITY", "Số lượng trong chi tiết phải > 0: " + item.description());
            }
            if (item.unitPrice() == null || item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                return KetQua.failure("INVALID_PRICE", "Đơn giá không được âm: " + item.description());
            }
            BigDecimal lineTotal = item.quantity().multiply(item.unitPrice());
            tongTien = tongTien.add(lineTotal);
        }

        BigDecimal insurancePaid = command.insurancePaidAmount() != null && command.insurancePaidAmount().compareTo(BigDecimal.ZERO) > 0
                ? command.insurancePaidAmount()
                : BigDecimal.ZERO;
        if (insurancePaid.compareTo(tongTien) > 0) {
            insurancePaid = tongTien;
        }

        BigDecimal advance = command.advanceAmount() != null && command.advanceAmount().compareTo(BigDecimal.ZERO) > 0
                ? command.advanceAmount()
                : BigDecimal.ZERO;

        BigDecimal discount = command.discountAmount() != null && command.discountAmount().compareTo(BigDecimal.ZERO) > 0
                ? command.discountAmount()
                : BigDecimal.ZERO;

        // Tính số tiền bệnh nhân cần chi trả sau BHYT, tạm ứng và giảm giá
        BigDecimal patientCopay = tongTien.subtract(insurancePaid).subtract(advance).subtract(discount);
        if (patientCopay.compareTo(BigDecimal.ZERO) < 0) {
            patientCopay = BigDecimal.ZERO;
        }

        // 6. Khởi tạo đối tượng Hóa Đơn & chi tiết
        String invoiceNumber = generateInvoiceNumber(command.visitId());
        HoaDon hoaDon = new HoaDon();
        hoaDon.setSoHoaDon(invoiceNumber);
        hoaDon.setMaLuotKham(command.visitId());
        hoaDon.setMaBenhNhan(command.patientId());
        hoaDon.setMaCoSo(command.facilityId());
        hoaDon.setNgayLap(LocalDateTime.now());
        hoaDon.setTongTien(tongTien);
        hoaDon.setSoTienBaoHiemChiTra(insurancePaid);
        hoaDon.setSoTienTamUng(advance);
        hoaDon.setSoTienGiamGia(discount);
        hoaDon.setChietKhauPhanTram(command.discountPercentage() != null ? command.discountPercentage() : BigDecimal.ZERO);
        hoaDon.setLyDoGiamGia(command.discountReason() != null ? command.discountReason() : "");
        hoaDon.setSoTienBenhNhanChiTra(patientCopay);
        hoaDon.setDaThanhToan(BigDecimal.ZERO);
        hoaDon.setTrangThai(patientCopay.compareTo(BigDecimal.ZERO) == 0 ? TrangThaiHoaDon.DA_THANH_TOAN : TrangThaiHoaDon.CHO_THANH_TOAN);
        hoaDon.setGhiChu(command.notes() != null ? command.notes() : "");

        for (HoaDonMatHangDauVao item : command.items()) {
            ChiTietHoaDon detail = new ChiTietHoaDon();
            detail.setLoaiDoiTuong(item.itemType() != null ? item.itemType() : "DICH_VU");
            detail.setMaDoiTuong(item.sourceId());
            detail.setMoTa(item.description() != null ? item.description() : "");
            detail.setSoLuong(item.quantity());
            detail.setDonGia(item.unitPrice());
            hoaDon.addChiTiet(detail);
        }

        // 7. Lưu trữ
        HoaDon saved = invoiceRepository.save(hoaDon);
        int invoiceId = saved != null && saved.getId() != null ? saved.getId() : 0;

        // 8. Ghi nhật ký kiểm toán
        logAudit(command.auditActorContext(), invoiceId, KiemToanHanhDongMa.BILL_INVOICE_CREATED,
                KiemToanKetQua.SUCCESS, null, "Lập hóa đơn viện phí thành công: " + invoiceNumber);

        CreateHoaDonBienNhan receipt = new CreateHoaDonBienNhan(
                invoiceId,
                hoaDon.getSoHoaDon(),
                hoaDon.getMaLuotKham(),
                hoaDon.getMaBenhNhan(),
                hoaDon.getMaCoSo(),
                hoaDon.getNgayLap(),
                hoaDon.getTongTien(),
                hoaDon.getSoTienBaoHiemChiTra(),
                hoaDon.getSoTienTamUng(),
                hoaDon.getSoTienGiamGia(),
                hoaDon.getSoTienBenhNhanChiTra(),
                hoaDon.getDaThanhToan(),
                hoaDon.getTrangThai()
        );

        return KetQua.success(receipt);
    }

    /** Hàm `isRoleAllowed`: là vai trò `Allowed`. */
    private boolean isRoleAllowed(String roleCode) {
        if (roleCode == null) return false;
        return ALLOWED_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    /** Hàm `generateInvoiceNumber`: `generate` hóa đơn so. */
    private String generateInvoiceNumber(int visitId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        int suffix = 1000 + Math.abs(visitId % 9000);
        return "HD" + timestamp + "-" + suffix;
    }

    /** Hàm `logAudit`: nhatky kiểm toán. */
    private void logAudit(KiemToanTacNhanNguCanh actor, int invoiceId, KiemToanHanhDongMa action,
                          KiemToanKetQua outcome, KiemToanLyDoMa reason, String note) {
        if (actor == null) return;
        KiemToanTaiNguyenLoai resourceType = invoiceId > 0 ? KiemToanTaiNguyenLoai.INVOICE : null;
        Integer resourceId = invoiceId > 0 ? invoiceId : null;
        auditWritePort.appendIndependently(new HeThongKiemToanNhapLieu(
                actor.employeeId(),
                actor.roleId(),
                actor.roleCode() != null ? actor.roleCode().trim().toUpperCase(Locale.ROOT) : null,
                actor.facilityId(),
                actor.departmentId(),
                action,
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
