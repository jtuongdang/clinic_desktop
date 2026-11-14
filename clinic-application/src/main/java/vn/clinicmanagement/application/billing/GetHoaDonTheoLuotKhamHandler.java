package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetHoaDonTheoLuotKhamHandler implements QueryHandler<GetHoaDonTheoLuotKhamQuery, HoaDonDuLieu> {

    private static final Set<String> ALLOWED_ROLES = Set.of("CASHIER", "ADMIN", "DOCTOR", "RECEPTIONIST");

    /** Trường `invoiceRepository`: hóa đơn kho dữ liệu. */
    private final IHoaDonRepository invoiceRepository;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public GetHoaDonTheoLuotKhamHandler(IHoaDonRepository invoiceRepository, KiemToanWriteCongKetNoi auditWritePort) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public KetQua<HoaDonDuLieu> handle(GetHoaDonTheoLuotKhamQuery query) {
        if (query == null) {
            return KetQua.failure("VALIDATION_ERROR", "Truy vấn hóa đơn không được để trống.");
        }

        // 1. Phân quyền
        PhanQuyenNguCanh auth = query.authorizationContext();
        if (auth == null || !isRoleAllowed(auth.roleCode())) {
            logAudit(query.auditActorContext(), 0, KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN);
            return KetQua.failure("AUTHORIZATION_DENIED", "Tài khoản không có quyền xem thông tin hóa đơn.");
        }

        if (query.visitId() <= 0) {
            return KetQua.failure("INVALID_VISIT", "Mã lượt khám không hợp lệ.");
        }

        // 2. Tra cứu hóa đơn
        Optional<HoaDon> invoiceOpt = invoiceRepository.getByLuotKham(query.visitId());
        if (invoiceOpt.isEmpty()) {
            return KetQua.failure("INVOICE_NOT_FOUND", "Không tìm thấy hóa đơn cho lượt khám: " + query.visitId());
        }

        HoaDon hoaDon = invoiceOpt.get();
        int invoiceId = hoaDon.getId() != null ? hoaDon.getId() : 0;

        // 3. Tra cứu danh sách phiếu thu
        List<PhieuThu> receipts = invoiceRepository.getPhieuThuByHoaDon(invoiceId);
        List<BienNhanDuLieu> receiptDtos = new ArrayList<>();
        if (receipts != null) {
            for (PhieuThu pt : receipts) {
                receiptDtos.add(new BienNhanDuLieu(
                        pt.getId() != null ? pt.getId() : 0,
                        pt.getMaHoaDon(),
                        pt.getSoTien(),
                        pt.getNgayThu(),
                        pt.getPhuongThuc(),
                        pt.getNguoiThu(),
                        pt.getGhiChu()
                ));
            }
        }

        // 4. Chi tiết các dòng hóa đơn
        List<HoaDonDongDuLieu> lineDtos = new ArrayList<>();
        if (hoaDon.getChiTietHoaDons() != null) {
            for (ChiTietHoaDon ct : hoaDon.getChiTietHoaDons()) {
                BigDecimal total = (ct.getSoLuong() != null && ct.getDonGia() != null)
                        ? ct.getSoLuong().multiply(ct.getDonGia())
                        : BigDecimal.ZERO;
                lineDtos.add(new HoaDonDongDuLieu(
                        ct.getId() != null ? ct.getId() : 0,
                        ct.getLoaiDoiTuong(),
                        ct.getMaDoiTuong(),
                        ct.getMoTa(),
                        ct.getSoLuong(),
                        ct.getDonGia(),
                        total
                ));
            }
        }

        BigDecimal remaining = hoaDon.getSoTienBenhNhanChiTra().subtract(hoaDon.getDaThanhToan());
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        HoaDonDuLieu dto = new HoaDonDuLieu(
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
                hoaDon.getChietKhauPhanTram(),
                hoaDon.getLyDoGiamGia(),
                hoaDon.getSoTienBenhNhanChiTra(),
                hoaDon.getDaThanhToan(),
                remaining,
                hoaDon.getTrangThai(),
                hoaDon.getGhiChu(),
                lineDtos,
                receiptDtos
        );

        logAudit(query.auditActorContext(), invoiceId, KiemToanKetQua.SUCCESS, null);
        return KetQua.success(dto);
    }

    /** Hàm `isRoleAllowed`: là vai trò `Allowed`. */
    private boolean isRoleAllowed(String roleCode) {
        if (roleCode == null) return false;
        return ALLOWED_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    /** Hàm `logAudit`: nhatky kiểm toán. */
    private void logAudit(KiemToanTacNhanNguCanh actor, int invoiceId, KiemToanKetQua outcome, KiemToanLyDoMa reason) {
        if (actor == null) return;
        KiemToanTaiNguyenLoai resourceType = invoiceId > 0 ? KiemToanTaiNguyenLoai.INVOICE : null;
        Integer resourceId = invoiceId > 0 ? invoiceId : null;
        auditWritePort.appendIndependently(new HeThongKiemToanNhapLieu(
                actor.employeeId(),
                actor.roleId(),
                actor.roleCode() != null ? actor.roleCode().trim().toUpperCase(Locale.ROOT) : null,
                actor.facilityId(),
                actor.departmentId(),
                KiemToanHanhDongMa.CATALOG_VIEW,
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
