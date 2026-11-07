package vn.clinicmanagement.domain.patient;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaoHiemTest {

    @Test
    void defaultsToHealthInsuranceAndStandardEligibilityFlags() {
        BaoHiem insurance = new BaoHiem();

        assertEquals(0, insurance.getMaBenhNhan());
        assertEquals(LoaiBaoHiem.BHYT, insurance.getLoaiBaoHiem());
        assertEquals("", insurance.getSoBaoHiem());
        assertNull(insurance.getNgayBatDau());
        assertNull(insurance.getNgayHetHan());
        assertEquals(0, insurance.getMucHuong());
        assertEquals("", insurance.getNoiDangKyKCB());
        assertEquals("", insurance.getMaNoiSong());
        assertEquals("", insurance.getMaNoiDangKyKCB());
        assertEquals("", insurance.getDiaChiThe());
        assertTrue(insurance.isDungTuyen());
        assertFalse(insurance.isCoGiayKhaiSinh());
        assertFalse(insurance.isKhongGiuThe());
        assertFalse(insurance.isMienCungChiTra());
        assertNull(insurance.getNgayMienCungChiTra());
        assertNull(insurance.getNgayDu5NamLienTuc());
        assertNull(insurance.getBenhNhan());
        assertFalse(insurance.isDeleted());
        assertNotNull(insurance.getCreatedAt());
    }

    @Test
    void exposesInsuranceAndPatientPropertiesThroughGettersAndSetters() {
        BaoHiem insurance = new BaoHiem();
        BenhNhan patient = new BenhNhan();
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        LocalDate copaymentExemption = LocalDate.of(2024, 3, 1);
        LocalDate continuousCoverage = LocalDate.of(2025, 1, 1);

        insurance.setMaBenhNhan(73);
        insurance.setLoaiBaoHiem(LoaiBaoHiem.BAO_HIEM_SUC_KHOE_TU_NHAN);
        insurance.setSoBaoHiem("SYNTHETIC-POLICY-001");
        insurance.setNgayBatDau(start);
        insurance.setNgayHetHan(end);
        insurance.setMucHuong(80);
        insurance.setNoiDangKyKCB("Synthetic facility");
        insurance.setMaNoiSong("K2");
        insurance.setMaNoiDangKyKCB("01-025");
        insurance.setDiaChiThe("Synthetic card address");
        insurance.setDungTuyen(false);
        insurance.setCoGiayKhaiSinh(true);
        insurance.setKhongGiuThe(true);
        insurance.setMienCungChiTra(true);
        insurance.setNgayMienCungChiTra(copaymentExemption);
        insurance.setNgayDu5NamLienTuc(continuousCoverage);
        insurance.setBenhNhan(patient);

        assertEquals(73, insurance.getMaBenhNhan());
        assertEquals(LoaiBaoHiem.BAO_HIEM_SUC_KHOE_TU_NHAN, insurance.getLoaiBaoHiem());
        assertEquals("SYNTHETIC-POLICY-001", insurance.getSoBaoHiem());
        assertEquals(start, insurance.getNgayBatDau());
        assertEquals(end, insurance.getNgayHetHan());
        assertEquals(80, insurance.getMucHuong());
        assertEquals("Synthetic facility", insurance.getNoiDangKyKCB());
        assertEquals("K2", insurance.getMaNoiSong());
        assertEquals("01-025", insurance.getMaNoiDangKyKCB());
        assertEquals("Synthetic card address", insurance.getDiaChiThe());
        assertFalse(insurance.isDungTuyen());
        assertTrue(insurance.isCoGiayKhaiSinh());
        assertTrue(insurance.isKhongGiuThe());
        assertTrue(insurance.isMienCungChiTra());
        assertEquals(copaymentExemption, insurance.getNgayMienCungChiTra());
        assertEquals(continuousCoverage, insurance.getNgayDu5NamLienTuc());
        assertEquals(patient, insurance.getBenhNhan());
    }

    @Test
    void supportsUpdateSoftDeleteAndRestoreLifecycleAndValidatesActors() {
        BaoHiem insurance = new BaoHiem();

        insurance.markAsUpdated(31);
        assertEquals(31, insurance.getUpdatedBy());
        assertNotNull(insurance.getUpdatedAt());
        assertThrows(IllegalArgumentException.class, () -> insurance.markAsUpdated(0));
        assertThrows(IllegalArgumentException.class, () -> insurance.softDelete(-1));

        insurance.softDelete(32);
        assertTrue(insurance.isDeleted());
        assertEquals(32, insurance.getDeletedBy());
        assertNotNull(insurance.getDeletedAt());

        insurance.restore();
        assertFalse(insurance.isDeleted());
        assertNull(insurance.getDeletedAt());
        assertNull(insurance.getDeletedBy());
    }
}
