package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/** Được gọi bên trong giao dịch của chỉ định/hủy CLS, dùng chung connection với các repository. */
public final class ClsVienPhiDongBo {
    public static final String CLS_LINE = "DICH_VU_CLS";
    private final IHoaDonRepository invoices;

    public ClsVienPhiDongBo(IHoaDonRepository invoices) {
        this.invoices = Objects.requireNonNull(invoices);
    }

    public KetQua<HoaDon> addCharge(ChiDinhDichVu order, String description, int employeeId, int facilityId) {
        var found = editableInvoice(order.getMaLuotKham(), facilityId);
        if (found.isFailure()) return found;
        HoaDon invoice = found.valueOrThrow();
        if (order.getId() == null || order.getId() <= 0) {
            throw new IllegalStateException("Chỉ định phải được lưu trước khi ghi nhận chi phí.");
        }
        if (invoice.getChiTietHoaDons().stream().anyMatch(line -> matches(line, order))) {
            return KetQua.success(invoice);
        }
        ChiTietHoaDon line = new ChiTietHoaDon();
        line.setLoaiDoiTuong(CLS_LINE);
        line.setMaDoiTuong(order.getId());
        line.setMoTa(description);
        line.setSoLuong(order.getSoLuong());
        line.setDonGia(order.getDonGia());
        line.setCreatedBy(employeeId);
        invoice.addChiTiet(line);
        persist(invoice, employeeId);
        return KetQua.success(invoice);
    }

    public KetQua<HoaDon> cancelCharge(ChiDinhDichVu order, int employeeId, int facilityId) {
        var found = editableInvoice(order.getMaLuotKham(), facilityId);
        if (found.isFailure()) return found;
        HoaDon invoice = found.valueOrThrow();
        var line = invoice.getChiTietHoaDons().stream().filter(item -> matches(item, order)).findFirst();
        if (line.isEmpty()) {
            return KetQua.failure("CHARGE_NOT_FOUND", "Chỉ định chưa có khoản phí tương ứng. Vui lòng đối soát tại thu ngân trước khi hủy.");
        }
        BigDecimal nextTotal = invoice.getChiTietHoaDons().stream()
                .filter(item -> !item.isDeleted() && item != line.get())
                .map(item -> item.getSoLuong().multiply(item.getDonGia()).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (invoice.patientAmountFor(nextTotal).compareTo(invoice.getDaThanhToan()) < 0) {
            return KetQua.failure("REFUND_REQUIRED", "Không thể hủy vì chi phí sau hủy thấp hơn số đã thu. Vui lòng liên hệ thu ngân để hoàn tiền.");
        }
        line.get().softDelete(employeeId);
        persist(invoice, employeeId);
        return KetQua.success(invoice);
    }

    private KetQua<HoaDon> editableInvoice(int visitId, int facilityId) {
        var found = invoices.getByLuotKham(visitId);
        if (found.isEmpty()) {
            return KetQua.failure("INVOICE_NOT_FOUND", "Lượt khám chưa có hóa đơn. Vui lòng lập hóa đơn tại thu ngân trước khi cập nhật chi phí CLS.");
        }
        HoaDon invoice = found.get();
        if (invoice.getMaCoSo() != facilityId) {
            return KetQua.failure("AUTH_ACCESS_DENIED", "Hóa đơn không thuộc cơ sở đang làm việc.");
        }
        if (invoice.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
            return KetQua.failure("INVOICE_CANCELLED", "Hóa đơn lượt khám đã hủy.");
        }
        if (invoice.getSoHoaDonDienTu() != null && !invoice.getSoHoaDonDienTu().isBlank()) {
            return KetQua.failure("INVOICE_FINALIZED", "Hóa đơn đã phát hành điện tử. Vui lòng liên hệ thu ngân để điều chỉnh.");
        }
        BigDecimal receiptTotal = invoices.getPhieuThuByHoaDon(invoice.getId()).stream()
                .map(vn.clinicmanagement.domain.billing.PhieuThu::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundTotal = invoices.getPhieuHoanTraByHoaDon(invoice.getId()).stream()
                .map(vn.clinicmanagement.domain.billing.PhieuHoanTra::getSoTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (receiptTotal.subtract(refundTotal).compareTo(invoice.getDaThanhToan()) != 0) {
            return KetQua.failure("BILLING_BALANCE_MISMATCH", "Số đã thu chưa khớp lịch sử phiếu thu. Vui lòng đối soát tại thu ngân.");
        }
        return KetQua.success(invoice);
    }

    private static boolean matches(ChiTietHoaDon line, ChiDinhDichVu order) {
        return !line.isDeleted() && CLS_LINE.equals(line.getLoaiDoiTuong())
                && Objects.equals(line.getMaDoiTuong(), order.getId());
    }

    private void persist(HoaDon invoice, int employeeId) {
        invoice.recalculateCharges();
        invoice.markAsUpdated(employeeId);
        // Header version is the shared concurrency guard for payment and charge mutations.
        invoices.save(invoice);
        synchronizeDebt(invoices, invoice, employeeId, null);
    }

    public static TrangThaiCongNo synchronizeDebt(IHoaDonRepository invoices, HoaDon invoice,
                                                   int employeeId, LocalDate dueDate) {
        CongNoBenhNhan debt = invoices.getCongNoByHoaDon(invoice.getId()).orElseGet(() -> {
            CongNoBenhNhan created = new CongNoBenhNhan();
            created.setMaHoaDon(invoice.getId());
            created.setMaBenhNhan(invoice.getMaBenhNhan());
            created.setHanThanhToan(dueDate != null ? dueDate : LocalDate.now().plusDays(30));
            created.setCreatedBy(employeeId);
            return created;
        });
        debt.setSoTienNo(invoice.getSoTienBenhNhanChiTra());
        debt.setSoTienDaTra(invoice.getDaThanhToan());
        debt.setTrangThai(debt.getSoTienConLai().signum() > 0 ? TrangThaiCongNo.DANG_NO : TrangThaiCongNo.DA_TAT_TOAN);
        debt.markAsUpdated(employeeId);
        invoices.saveCongNo(debt);
        return debt.getTrangThai();
    }
}
