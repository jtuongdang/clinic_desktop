package vn.clinicmanagement.domain.examination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChanDoanTest {

    @Test
    void initializesDiagnosisDefaultsAndBaseEntityDefaults() {
        ChanDoan diagnosis = new ChanDoan();

        assertNull(diagnosis.getId());
        assertEquals(0, diagnosis.getMaLuotKham());
        assertNull(diagnosis.getMaICD());
        assertEquals("", diagnosis.getChanDoanText());
        assertFalse(diagnosis.isLaChinh());
        assertEquals("XAC_DINH", diagnosis.getLoaiChanDoan());
        assertNull(diagnosis.getLuotKham());
        assertNull(diagnosis.getIcd());
        assertEquals(0L, diagnosis.getVersion());
        assertNotNull(diagnosis.getCreatedAt());
        assertFalse(diagnosis.isDeleted());
    }

    @Test
    void exposesDiagnosisFieldsAndRelatedEntitiesThroughGettersAndSetters() {
        ChanDoan diagnosis = new ChanDoan();
        LuotKham visit = new LuotKham();
        ICD icd = new ICD();

        diagnosis.setId(24);
        diagnosis.setMaLuotKham(42);
        diagnosis.setMaICD(81);
        diagnosis.setChanDoanText("Synthetic clinical finding");
        diagnosis.setLaChinh(true);
        diagnosis.setLoaiChanDoan("SO_BO");
        diagnosis.setLuotKham(visit);
        diagnosis.setIcd(icd);

        assertEquals(24, diagnosis.getId());
        assertEquals(42, diagnosis.getMaLuotKham());
        assertEquals(81, diagnosis.getMaICD());
        assertEquals("Synthetic clinical finding", diagnosis.getChanDoanText());
        assertTrue(diagnosis.isLaChinh());
        assertEquals("SO_BO", diagnosis.getLoaiChanDoan());
        assertEquals(visit, diagnosis.getLuotKham());
        assertEquals(icd, diagnosis.getIcd());

        diagnosis.setMaICD(null);
        diagnosis.setChanDoanText(null);
        diagnosis.setLoaiChanDoan(null);
        diagnosis.setLuotKham(null);
        diagnosis.setIcd(null);
        assertNull(diagnosis.getMaICD());
        assertNull(diagnosis.getChanDoanText());
        assertNull(diagnosis.getLoaiChanDoan());
        assertNull(diagnosis.getLuotKham());
        assertNull(diagnosis.getIcd());
    }

    @Test
    void supportsBaseEntityUpdateDeleteAndRestoreLifecycle() {
        ChanDoan diagnosis = new ChanDoan();

        diagnosis.markAsUpdated(27);

        assertEquals(27, diagnosis.getUpdatedBy());
        assertNotNull(diagnosis.getUpdatedAt());
        assertThrows(IllegalArgumentException.class, () -> diagnosis.markAsUpdated(-1));

        diagnosis.softDelete(28);
        assertTrue(diagnosis.isDeleted());
        assertEquals(28, diagnosis.getDeletedBy());
        assertNotNull(diagnosis.getDeletedAt());
        assertThrows(IllegalArgumentException.class, () -> diagnosis.softDelete(0));

        diagnosis.restore();
        assertFalse(diagnosis.isDeleted());
        assertNull(diagnosis.getDeletedAt());
        assertNull(diagnosis.getDeletedBy());
    }
}
