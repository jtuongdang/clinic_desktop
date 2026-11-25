package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Đồng bộ chi phí đơn thuốc vào hóa đơn lượt khám (Thu Ngân) và kiểm tra thanh toán trước khi xuất kho.
 * Được gọi bên trong giao dịch của kê đơn / duyệt đơn / hủy đơn, dùng chung connection với các repository.
 */
public final class DonThuocVienPhiDongBo {
    public static final String RX_LINE = "DON_THUOC";
    private final IHoaDonRepository invoices;

    public DonThuocVienPhiDongBo(IHoaDonRepository invoices) {
        this.invoices = Objects.requireNonNull(invoices, "invoices");
    }

    /** Đẩy các dòng thuốc trong đơn sang hóa đơn lượt khám (idempotent theo maDonThuoc). */
    public KetQua<HoaDon> syncPrescriptionCharges(DonThuoc prescription, int employeeId, int facilityId) {
        if (prescription == null || prescription.getId() == null || prescription.getId() <= 0) {
            throw new IllegalStateException("Đơn thuốc phải được lưu trước khi ghi nhận chi phí viện phí.");
        }
        var found = editableInvoice(prescription.getMaLuotKham(), facilityId);
        if (found.isFailure()) return found;
        HoaDon invoice = found.valueOrThrow();

        if (invoice.getChiTietHoaDons().stream().anyMatch(line -> matches(line, prescription))) {
            return KetQua.success(invoice);
        }

        List<ChiTietDonThuoc> details = prescription.getChiTietDonThuocs();
        if (details == null || details.isEmpty()) {
            return KetQua.success(invoice);
        }

        boolean added = false;
        for (ChiTietDonThuoc detail : details) {
            if (detail == null || detail.isDeleted()) continue;
            BigDecimal qty = detail.getSoLuongKe() != null ? detail.getSoLuongKe() : BigDecimal.ZERO;
            if (qty.signum() <= 0) continue;
            BigDecimal unitPrice = detail.getDonGiaKe();
            if ((unitPrice == null || unitPrice.signum() < 0) && detail.getItem() != null) {
                unitPrice = detail.getItem().getGiaBanMacDinh();
            }
            if (unitPrice == null || unitPrice.signum() < 0) {
                unitPrice = BigDecimal.ZERO;
            }
            String desc = buildDescription(detail);
            ChiTietHoaDon line = new ChiTietHoaDon();
            line.setLoaiDoiTuong(RX_LINE);
            line.setMaDoiTuong(prescription.getId());
            line.setMoTa(desc);
            line.setSoLuong(qty);
            line.setDonGia(unitPrice);
            line.setCreatedBy(employeeId);
            invoice.addChiTiet(line);
            added = true;
        }
        if (added) {
            persist(invoice, employeeId);
        }
        return KetQua.success(invoice);
    }

    /** Gỡ các dòng thuốc của đơn khỏi hóa đơn lượt khám khi bác sĩ hủy đơn. */
    public KetQua<HoaDon> cancelPrescriptionCharges(DonThuoc prescription, int employeeId, int facilityId) {
        if (prescription == null || prescription.getId() == null || prescription.getId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Đơn thuốc không hợp lệ.");
        }
        var found = editableInvoice(prescription.getMaLuotKham(), facilityId);
        if (found.isFailure()) return found;
        HoaDon invoice = found.valueOrThrow();

        List<ChiTietHoaDon> matched = invoice.getChiTietHoaDons().stream()
                .filter(line -> matches(line, prescription))
                .toList();
        if (matched.isEmpty()) {
            return KetQua.success(invoice);
        }

        BigDecimal nextTotal = invoice.getChiTietHoaDons().stream()
                .filter(item -> !item.isDeleted() && !matched.contains(item))
                .map(item -> item.getSoLuong().multiply(item.getDonGia()).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (invoice.patientAmountFor(nextTotal).compareTo(invoice.getDaThanhToan()) < 0) {
            return KetQua.failure("REFUND_REQUIRED",
                    "Không thể hủy đơn thuốc vì tiền đã thu vượt quá chi phí còn lại. Vui lòng liên hệ Thu Ngân để hoàn tiền.");
        }
        for (ChiTietHoaDon line : matched) {
            line.softDelete(employeeId);
        }
        persist(invoice, employeeId);
        return KetQua.success(invoice);
    }

    /** Kiểm tra bệnh nhân đã thanh toán đủ hóa đơn lượt khám (bao gồm đơn thuốc) tại Thu Ngân chưa. */
    public KetQua<Void> verifyPaidForDispense(DonThuoc prescription, int facilityId) {
        if (prescription == null || prescription.getMaLuotKham() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin lượt khám của đơn thuốc không hợp lệ.");
        }
        var found = invoices.getByLuotKham(prescription.getMaLuotKham());
        if (found.isEmpty()) {
            return KetQua.failure("UNPAID_PRESCRIPTION",
                    "Lượt khám chưa có hóa đơn thanh toán tại Thu Ngân. Vui lòng hướng dẫn bệnh nhân quay lại quầy Thu Ngân.");
        }
        HoaDon invoice = found.get();
        if (invoice.getMaCoSo() != facilityId) {
            return KetQua.failure("AUTH_ACCESS_DENIED", "Hóa đơn không thuộc cơ sở đang làm việc.");
        }
        if (invoice.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
            return KetQua.failure("INVOICE_CANCELLED", "Hóa đơn của lượt khám đã bị hủy, không thể xuất thuốc.");
        }
        BigDecimal copay = invoice.getSoTienBenhNhanChiTra() != null ? invoice.getSoTienBenhNhanChiTra() : BigDecimal.ZERO;
        BigDecimal paid = invoice.getDaThanhToan() != null ? invoice.getDaThanhToan() : BigDecimal.ZERO;
        BigDecimal remaining = copay.subtract(paid);
        if (remaining.signum() > 0) {
            String formatted = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(remaining);
            return KetQua.failure("UNPAID_PRESCRIPTION",
                    "Bệnh nhân chưa thanh toán đủ viện phí/tiền thuốc (còn nợ " + formatted
                            + "đ). Vui lòng hướng dẫn bệnh nhân quay lại Thu Ngân thanh toán trước khi xuất kho.");
        }
        return KetQua.success(null);
    }

    private KetQua<HoaDon> editableInvoice(int visitId, int facilityId) {
        var found = invoices.getByLuotKham(visitId);
        if (found.isEmpty()) {
            return KetQua.failure("INVOICE_NOT_FOUND",
                    "Lượt khám chưa có hóa đơn. Vui lòng lập hóa đơn tại Thu Ngân trước khi kê đơn thuốc.");
        }
        HoaDon invoice = found.get();
        if (invoice.getMaCoSo() != facilityId) {
            return KetQua.failure("AUTH_ACCESS_DENIED", "Hóa đơn không thuộc cơ sở đang làm việc.");
        }
        if (invoice.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
            return KetQua.failure("INVOICE_CANCELLED", "Hóa đơn lượt khám đã hủy.");
        }
        if (invoice.getSoHoaDonDienTu() != null && !invoice.getSoHoaDonDienTu().isBlank()) {
            return KetQua.failure("INVOICE_FINALIZED",
                    "Hóa đơn đã phát hành điện tử. Vui lòng liên hệ Thu Ngân để điều chỉnh.");
        }
        BigDecimal receiptTotal = invoices.getPhieuThuByHoaDon(invoice.getId()).stream()
                .map(vn.clinicmanagement.domain.billing.PhieuThu::getSoTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundTotal = invoices.getPhieuHoanTraByHoaDon(invoice.getId()).stream()
                .map(vn.clinicmanagement.domain.billing.PhieuHoanTra::getSoTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (receiptTotal.subtract(refundTotal).compareTo(invoice.getDaThanhToan()) != 0) {
            return KetQua.failure("BILLING_BALANCE_MISMATCH",
                    "Số đã thu chưa khớp lịch sử phiếu thu. Vui lòng đối soát tại Thu Ngân.");
        }
        return KetQua.success(invoice);
    }

    private static boolean matches(ChiTietHoaDon line, DonThuoc prescription) {
        return !line.isDeleted()
                && RX_LINE.equals(line.getLoaiDoiTuong())
                && Objects.equals(line.getMaDoiTuong(), prescription.getId());
    }

    private static String buildDescription(ChiTietDonThuoc detail) {
        if (detail.getItem() != null && detail.getItem().getTenItem() != null && !detail.getItem().getTenItem().isBlank()) {
            String unit = detail.getItem().getDonViCoBan();
            String name = detail.getItem().getTenItem().trim();
            return (unit != null && !unit.isBlank()) ? name + " (" + unit.trim() + ")" : name;
        }
        return "Thuốc #" + detail.getMaItem();
    }

    private void persist(HoaDon invoice, int employeeId) {
        invoice.recalculateCharges();
        invoice.markAsUpdated(employeeId);
        invoices.save(invoice);
        ClsVienPhiDongBo.synchronizeDebt(invoices, invoice, employeeId, null);
    }
}
