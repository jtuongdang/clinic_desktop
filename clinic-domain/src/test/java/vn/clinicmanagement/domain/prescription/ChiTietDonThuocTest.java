package vn.clinicmanagement.domain.prescription;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.inventory.MatHang;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChiTietDonThuocTest {

    @Test
    void defaultsQuantitiesAndDosageFieldsToSafeEmptyValues() {
        ChiTietDonThuoc lineItem = new ChiTietDonThuoc();

        assertEquals(BigDecimal.ZERO, lineItem.getSoLuongKe());
        assertEquals(BigDecimal.ZERO, lineItem.getSoLuongDaXuat());
        assertEquals(BigDecimal.ZERO, lineItem.getDonGiaKe());
        assertEquals("", lineItem.getLieuDung());
        assertEquals("", lineItem.getTanSuat());
        assertNull(lineItem.getSoNgay());
        assertEquals("", lineItem.getHuongDan());
        assertEquals("Uống", lineItem.getDuongDung());
        assertEquals(BigDecimal.ZERO, lineItem.getSang());
        assertEquals(BigDecimal.ZERO, lineItem.getTrua());
        assertEquals(BigDecimal.ZERO, lineItem.getChieu());
        assertEquals(BigDecimal.ZERO, lineItem.getToi());
        assertNull(lineItem.getDonThuoc());
        assertNull(lineItem.getItem());
    }

    @Test
    void storesPrescriptionItemQuantitiesAndCompleteDosageInstructions() {
        ChiTietDonThuoc lineItem = new ChiTietDonThuoc();
        DonThuoc prescription = new DonThuoc();
        MatHang item = new MatHang();
        BigDecimal prescribed = new BigDecimal("12.50");
        BigDecimal dispensed = new BigDecimal("4.25");
        BigDecimal price = new BigDecimal("1500.00");

        lineItem.setMaDonThuoc(42);
        lineItem.setMaItem(17);
        lineItem.setSoLuongKe(prescribed);
        lineItem.setSoLuongDaXuat(dispensed);
        lineItem.setDonGiaKe(price);
        lineItem.setLieuDung("1 viên");
        lineItem.setTanSuat("Ngày 2 lần");
        lineItem.setSoNgay(7);
        lineItem.setHuongDan("Dùng sau ăn");
        lineItem.setDuongDung("Uống");
        lineItem.setSang(new BigDecimal("1"));
        lineItem.setTrua(BigDecimal.ZERO);
        lineItem.setChieu(new BigDecimal("1"));
        lineItem.setToi(BigDecimal.ZERO);
        lineItem.setDonThuoc(prescription);
        lineItem.setItem(item);

        assertEquals(42, lineItem.getMaDonThuoc());
        assertEquals(17, lineItem.getMaItem());
        assertEquals(prescribed, lineItem.getSoLuongKe());
        assertEquals(dispensed, lineItem.getSoLuongDaXuat());
        assertEquals(price, lineItem.getDonGiaKe());
        assertEquals("1 viên", lineItem.getLieuDung());
        assertEquals("Ngày 2 lần", lineItem.getTanSuat());
        assertEquals(7, lineItem.getSoNgay());
        assertEquals("Dùng sau ăn", lineItem.getHuongDan());
        assertEquals("Uống", lineItem.getDuongDung());
        assertEquals(new BigDecimal("1"), lineItem.getSang());
        assertEquals(BigDecimal.ZERO, lineItem.getTrua());
        assertEquals(new BigDecimal("1"), lineItem.getChieu());
        assertEquals(BigDecimal.ZERO, lineItem.getToi());
        assertEquals(prescription, lineItem.getDonThuoc());
        assertEquals(item, lineItem.getItem());
    }
}
