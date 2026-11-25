package vn.clinicmanagement.domain.billing;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PhieuThuTest {

    @Test
    void defaultsToCashReceiptWithZeroAmount() {
        PhieuThu receipt = new PhieuThu();

        assertEquals(0, receipt.getMaHoaDon());
        assertEquals(BigDecimal.ZERO, receipt.getSoTien());
        assertNotNull(receipt.getNgayThu());
        assertEquals(PhuongThucThanhToan.TIEN_MAT, receipt.getPhuongThuc());
        assertEquals(0, receipt.getNguoiThu());
        assertNull(receipt.getGhiChu());
        assertNull(receipt.getHoaDon());
    }

    @Test
    void storesReceiptPropertiesAndInvoiceAssociation() {
        PhieuThu receipt = new PhieuThu();
        HoaDon invoice = new HoaDon();
        LocalDateTime receivedAt = LocalDateTime.of(2026, 11, 7, 10, 15);

        receipt.setMaHoaDon(42);
        receipt.setSoTien(new BigDecimal("275.50"));
        receipt.setNgayThu(receivedAt);
        receipt.setPhuongThuc(PhuongThucThanhToan.THE);
        receipt.setNguoiThu(9);
        receipt.setGhiChu("Synthetic receipt");
        receipt.setHoaDon(invoice);

        assertEquals(42, receipt.getMaHoaDon());
        assertEquals(new BigDecimal("275.50"), receipt.getSoTien());
        assertEquals(receivedAt, receipt.getNgayThu());
        assertEquals(PhuongThucThanhToan.THE, receipt.getPhuongThuc());
        assertEquals(9, receipt.getNguoiThu());
        assertEquals("Synthetic receipt", receipt.getGhiChu());
        assertEquals(invoice, receipt.getHoaDon());
    }
}
