package vn.clinicmanagement.domain.billing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChiTietHoaDonTest {

    @Test
    void defaultsToOneUnitWithZeroPriceAndNoInvoice() {
        ChiTietHoaDon detail = new ChiTietHoaDon();

        assertEquals(0, detail.getMaHoaDon());
        assertEquals("", detail.getLoaiDoiTuong());
        assertEquals(0, detail.getMaDoiTuong());
        assertEquals("", detail.getMoTa());
        assertEquals(BigDecimal.ONE, detail.getSoLuong());
        assertEquals(BigDecimal.ZERO, detail.getDonGia());
        assertNull(detail.getHoaDon());
    }

    @Test
    void storesInvoiceLinePropertiesAndAssociation() {
        ChiTietHoaDon detail = new ChiTietHoaDon();
        HoaDon invoice = new HoaDon();

        detail.setMaHoaDon(42);
        detail.setLoaiDoiTuong("DICH_VU");
        detail.setMaDoiTuong(17);
        detail.setMoTa("Synthetic consultation service");
        detail.setSoLuong(new BigDecimal("2.50"));
        detail.setDonGia(new BigDecimal("125.00"));
        detail.setHoaDon(invoice);

        assertEquals(42, detail.getMaHoaDon());
        assertEquals("DICH_VU", detail.getLoaiDoiTuong());
        assertEquals(17, detail.getMaDoiTuong());
        assertEquals("Synthetic consultation service", detail.getMoTa());
        assertEquals(new BigDecimal("2.50"), detail.getSoLuong());
        assertEquals(new BigDecimal("125.00"), detail.getDonGia());
        assertEquals(invoice, detail.getHoaDon());
    }
}
