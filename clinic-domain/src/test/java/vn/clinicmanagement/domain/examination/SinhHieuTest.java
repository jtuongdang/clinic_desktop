package vn.clinicmanagement.domain.examination;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SinhHieuTest {

    @Test
    void initializesEmptyMeasurementsAndBaseEntityDefaults() {
        SinhHieu vitals = new SinhHieu();

        assertNull(vitals.getId());
        assertEquals(0, vitals.getMaLuotKham());
        assertNull(vitals.getMach());
        assertNull(vitals.getNhietDo());
        assertNull(vitals.getHuyetApTamThu());
        assertNull(vitals.getHuyetApTamTruong());
        assertNull(vitals.getNhipTho());
        assertNull(vitals.getCanNang());
        assertNull(vitals.getChieuCao());
        assertNull(vitals.getBmi());
        assertNull(vitals.getSpo2());
        assertNull(vitals.getLuotKham());
        assertEquals(0L, vitals.getVersion());
        assertNotNull(vitals.getCreatedAt());
        assertFalse(vitals.isDeleted());
    }

    @Test
    void exposesVisitAndVitalMeasurementsThroughGettersAndSetters() {
        SinhHieu vitals = new SinhHieu();
        LuotKham visit = new LuotKham();
        BigDecimal temperature = new BigDecimal("36.7");
        BigDecimal weight = new BigDecimal("62.5");
        BigDecimal height = new BigDecimal("168.0");
        BigDecimal bmi = new BigDecimal("22.1");
        BigDecimal oxygenSaturation = new BigDecimal("98.0");

        vitals.setId(31);
        vitals.setMaLuotKham(42);
        vitals.setMach(72);
        vitals.setNhietDo(temperature);
        vitals.setHuyetApTamThu(118);
        vitals.setHuyetApTamTruong(76);
        vitals.setNhipTho(16);
        vitals.setCanNang(weight);
        vitals.setChieuCao(height);
        vitals.setBmi(bmi);
        vitals.setSpo2(oxygenSaturation);
        vitals.setLuotKham(visit);

        assertEquals(31, vitals.getId());
        assertEquals(42, vitals.getMaLuotKham());
        assertEquals(72, vitals.getMach());
        assertEquals(temperature, vitals.getNhietDo());
        assertEquals(118, vitals.getHuyetApTamThu());
        assertEquals(76, vitals.getHuyetApTamTruong());
        assertEquals(16, vitals.getNhipTho());
        assertEquals(weight, vitals.getCanNang());
        assertEquals(height, vitals.getChieuCao());
        assertEquals(bmi, vitals.getBmi());
        assertEquals(oxygenSaturation, vitals.getSpo2());
        assertEquals(visit, vitals.getLuotKham());

        vitals.setMach(null);
        vitals.setNhietDo(null);
        vitals.setHuyetApTamThu(null);
        vitals.setHuyetApTamTruong(null);
        vitals.setNhipTho(null);
        vitals.setCanNang(null);
        vitals.setChieuCao(null);
        vitals.setBmi(null);
        vitals.setSpo2(null);
        vitals.setLuotKham(null);
        assertNull(vitals.getMach());
        assertNull(vitals.getNhietDo());
        assertNull(vitals.getHuyetApTamThu());
        assertNull(vitals.getHuyetApTamTruong());
        assertNull(vitals.getNhipTho());
        assertNull(vitals.getCanNang());
        assertNull(vitals.getChieuCao());
        assertNull(vitals.getBmi());
        assertNull(vitals.getSpo2());
        assertNull(vitals.getLuotKham());
    }

    @Test
    void supportsBaseEntityUpdateDeleteAndRestoreLifecycle() {
        SinhHieu vitals = new SinhHieu();
        LocalDateTime beforeUpdate = LocalDateTime.now();

        vitals.markAsUpdated(17);

        assertEquals(17, vitals.getUpdatedBy());
        assertNotNull(vitals.getUpdatedAt());
        assertFalse(vitals.getUpdatedAt().isBefore(beforeUpdate));
        assertThrows(IllegalArgumentException.class, () -> vitals.markAsUpdated(0));

        vitals.softDelete(18);
        LocalDateTime deletedAt = vitals.getDeletedAt();
        assertTrue(vitals.isDeleted());
        assertEquals(18, vitals.getDeletedBy());
        assertNotNull(deletedAt);
        assertThrows(IllegalArgumentException.class, () -> vitals.softDelete(-1));

        vitals.restore();
        assertFalse(vitals.isDeleted());
        assertNull(vitals.getDeletedAt());
        assertNull(vitals.getDeletedBy());
    }
}
