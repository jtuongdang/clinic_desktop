package vn.clinicmanagement.domain.billing;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.common.events.ThanhToanDaGhiNhanSuKien;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.patient.BenhNhan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoaDonTest {

    @Test
    void defaultsToUnpaidInvoiceWithEmptyCollections() {
        HoaDon invoice = new HoaDon();

        assertEquals("", invoice.getSoHoaDon());
        assertEquals(0, invoice.getMaLuotKham());
        assertEquals(0, invoice.getMaBenhNhan());
        assertEquals(0, invoice.getMaCoSo());
        assertNotNull(invoice.getNgayLap());
        assertEquals(BigDecimal.ZERO, invoice.getTongTien());
        assertEquals(BigDecimal.ZERO, invoice.getSoTienBaoHiemChiTra());
        assertEquals(BigDecimal.ZERO, invoice.getSoTienBenhNhanChiTra());
        assertEquals(BigDecimal.ZERO, invoice.getDaThanhToan());
        assertEquals(TrangThaiHoaDon.CHO_THANH_TOAN, invoice.getTrangThai());
        assertEquals("", invoice.getGhiChu());
        assertEquals(BigDecimal.ZERO, invoice.getSoTienTamUng());
        assertEquals(BigDecimal.ZERO, invoice.getSoTienGiamGia());
        assertEquals(BigDecimal.ZERO, invoice.getChietKhauPhanTram());
        assertEquals("", invoice.getLyDoGiamGia());
        assertEquals(BigDecimal.ZERO, invoice.getSoTienHoanTra());
        assertEquals("", invoice.getSoHoaDonDienTu());
        assertEquals("", invoice.getMaTraCuuHddt());
        assertNull(invoice.getLuotKham());
        assertNull(invoice.getBenhNhan());
        assertNull(invoice.getCoSo());
        assertTrue(invoice.getChiTietHoaDons().isEmpty());
        assertTrue(invoice.getPhieuThus().isEmpty());
        assertNull(invoice.getCongNoBenhNhan());
    }

    @Test
    void exposesInvoicePropertiesAndAssociationsThroughGettersAndSetters() {
        HoaDon invoice = new HoaDon();
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 7, 9, 30);
        LuotKham visit = new LuotKham();
        BenhNhan patient = new BenhNhan();
        CoSo facility = new CoSo();
        List<ChiTietHoaDon> details = new ArrayList<>(List.of(new ChiTietHoaDon()));
        List<PhieuThu> receipts = new ArrayList<>(List.of(new PhieuThu()));
        CongNoBenhNhan debt = new CongNoBenhNhan();

        invoice.setSoHoaDon("HD-SYNTHETIC-001");
        invoice.setMaLuotKham(12);
        invoice.setMaBenhNhan(34);
        invoice.setMaCoSo(56);
        invoice.setNgayLap(createdAt);
        invoice.setTongTien(new BigDecimal("1250.00"));
        invoice.setSoTienBaoHiemChiTra(new BigDecimal("250.00"));
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("1000.00"));
        invoice.setDaThanhToan(new BigDecimal("300.00"));
        invoice.setTrangThai(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN);
        invoice.setGhiChu("Synthetic invoice");
        invoice.setSoTienTamUng(new BigDecimal("100.00"));
        invoice.setSoTienGiamGia(new BigDecimal("50.00"));
        invoice.setChietKhauPhanTram(new BigDecimal("5.00"));
        invoice.setLyDoGiamGia("Synthetic discount");
        invoice.setSoTienHoanTra(new BigDecimal("25.00"));
        invoice.setSoHoaDonDienTu("E-SYNTHETIC-001");
        invoice.setMaTraCuuHddt("LOOKUP-SYNTHETIC-001");
        invoice.setLuotKham(visit);
        invoice.setBenhNhan(patient);
        invoice.setCoSo(facility);
        invoice.setChiTietHoaDons(details);
        invoice.setPhieuThu(receipts);
        invoice.setCongNoBenhNhan(debt);

        assertEquals("HD-SYNTHETIC-001", invoice.getSoHoaDon());
        assertEquals(12, invoice.getMaLuotKham());
        assertEquals(34, invoice.getMaBenhNhan());
        assertEquals(56, invoice.getMaCoSo());
        assertEquals(createdAt, invoice.getNgayLap());
        assertEquals(new BigDecimal("1250.00"), invoice.getTongTien());
        assertEquals(new BigDecimal("250.00"), invoice.getSoTienBaoHiemChiTra());
        assertEquals(new BigDecimal("1000.00"), invoice.getSoTienBenhNhanChiTra());
        assertEquals(new BigDecimal("300.00"), invoice.getDaThanhToan());
        assertEquals(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN, invoice.getTrangThai());
        assertEquals("Synthetic invoice", invoice.getGhiChu());
        assertEquals(new BigDecimal("100.00"), invoice.getSoTienTamUng());
        assertEquals(new BigDecimal("50.00"), invoice.getSoTienGiamGia());
        assertEquals(new BigDecimal("5.00"), invoice.getChietKhauPhanTram());
        assertEquals("Synthetic discount", invoice.getLyDoGiamGia());
        assertEquals(new BigDecimal("25.00"), invoice.getSoTienHoanTra());
        assertEquals("E-SYNTHETIC-001", invoice.getSoHoaDonDienTu());
        assertEquals("LOOKUP-SYNTHETIC-001", invoice.getMaTraCuuHddt());
        assertEquals(visit, invoice.getLuotKham());
        assertEquals(patient, invoice.getBenhNhan());
        assertEquals(facility, invoice.getCoSo());
        assertEquals(details, invoice.getChiTietHoaDons());
        assertEquals(receipts, invoice.getPhieuThus());
        assertEquals(debt, invoice.getCongNoBenhNhan());
    }

    @Test
    void recordsPartialThenFullPaymentsAndPublishesPaymentEvents() {
        HoaDon invoice = new HoaDon();
        invoice.setId(42);
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("1000.00"));

        invoice.recordPayment(new BigDecimal("400.00"), PhuongThucThanhToan.TIEN_MAT, 7, "First payment");

        assertEquals(new BigDecimal("400.00"), invoice.getDaThanhToan());
        assertEquals(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN, invoice.getTrangThai());
        assertEquals(1, invoice.getPhieuThus().size());
        PhieuThu firstReceipt = invoice.getPhieuThus().get(0);
        assertEquals(42, firstReceipt.getMaHoaDon());
        assertEquals(new BigDecimal("400.00"), firstReceipt.getSoTien());
        assertEquals(PhuongThucThanhToan.TIEN_MAT, firstReceipt.getPhuongThuc());
        assertEquals(7, firstReceipt.getNguoiThu());
        assertEquals("First payment", firstReceipt.getGhiChu());

        invoice.recordPayment(new BigDecimal("600.00"), PhuongThucThanhToan.CHUYEN_KHOAN, 8, "Final payment");

        assertEquals(new BigDecimal("1000.00"), invoice.getDaThanhToan());
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, invoice.getTrangThai());
        assertEquals(2, invoice.getPhieuThus().size());
        assertEquals(2, invoice.getDomainEvents().size());
        ThanhToanDaGhiNhanSuKien firstEvent = assertInstanceOf(
                ThanhToanDaGhiNhanSuKien.class, invoice.getDomainEvents().get(0));
        assertEquals(42, firstEvent.getInvoiceId());
        assertEquals(new BigDecimal("400.00"), firstEvent.getAmountPaid());
        assertEquals(PhuongThucThanhToan.TIEN_MAT.name(), firstEvent.getPaymentMethod());
        ThanhToanDaGhiNhanSuKien secondEvent = assertInstanceOf(
                ThanhToanDaGhiNhanSuKien.class, invoice.getDomainEvents().get(1));
        assertEquals(42, secondEvent.getInvoiceId());
        assertEquals(new BigDecimal("600.00"), secondEvent.getAmountPaid());
        assertEquals(PhuongThucThanhToan.CHUYEN_KHOAN.name(), secondEvent.getPaymentMethod());
    }

    @Test
    void rejectsNullZeroAndNegativePaymentsWithoutChangingInvoice() {
        HoaDon invoice = new HoaDon();
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("100.00"));

        assertThrows(IllegalArgumentException.class,
                () -> invoice.recordPayment(null, PhuongThucThanhToan.TIEN_MAT, 7, ""));
        assertThrows(IllegalArgumentException.class,
                () -> invoice.recordPayment(BigDecimal.ZERO, PhuongThucThanhToan.TIEN_MAT, 7, ""));
        assertThrows(IllegalArgumentException.class,
                () -> invoice.recordPayment(new BigDecimal("-0.01"), PhuongThucThanhToan.TIEN_MAT, 7, ""));

        assertEquals(BigDecimal.ZERO, invoice.getDaThanhToan());
        assertEquals(TrangThaiHoaDon.CHO_THANH_TOAN, invoice.getTrangThai());
        assertTrue(invoice.getPhieuThus().isEmpty());
        assertTrue(invoice.getDomainEvents().isEmpty());
    }
}
