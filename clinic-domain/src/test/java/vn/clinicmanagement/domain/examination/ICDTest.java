package vn.clinicmanagement.domain.examination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ICDTest {

    @Test
    void initializesCatalogDefaultsAndBaseEntityDefaults() {
        ICD icd = new ICD();

        assertNull(icd.getId());
        assertEquals("", icd.getMaICD());
        assertEquals("", icd.getTenBenh());
        assertNull(icd.getMoTa());
        assertTrue(icd.isHoatDong());
        assertEquals(0L, icd.getVersion());
        assertNotNull(icd.getCreatedAt());
        assertFalse(icd.isDeleted());
    }

    @Test
    void exposesCatalogFieldsThroughGettersAndSetters() {
        ICD icd = new ICD();

        icd.setId(81);
        icd.setMaICD("SYN-01");
        icd.setTenBenh("Synthetic catalog entry");
        icd.setMoTa("Synthetic description");
        icd.setHoatDong(false);

        assertEquals(81, icd.getId());
        assertEquals("SYN-01", icd.getMaICD());
        assertEquals("Synthetic catalog entry", icd.getTenBenh());
        assertEquals("Synthetic description", icd.getMoTa());
        assertFalse(icd.isHoatDong());

        icd.setMaICD(null);
        icd.setTenBenh(null);
        icd.setMoTa(null);
        assertNull(icd.getMaICD());
        assertNull(icd.getTenBenh());
        assertNull(icd.getMoTa());
    }

    @Test
    void supportsBaseEntityUpdateDeleteAndRestoreLifecycle() {
        ICD icd = new ICD();

        icd.markAsUpdated(37);

        assertEquals(37, icd.getUpdatedBy());
        assertNotNull(icd.getUpdatedAt());
        assertThrows(IllegalArgumentException.class, () -> icd.markAsUpdated(0));

        icd.softDelete(38);
        assertTrue(icd.isDeleted());
        assertEquals(38, icd.getDeletedBy());
        assertNotNull(icd.getDeletedAt());
        assertThrows(IllegalArgumentException.class, () -> icd.softDelete(-1));

        icd.restore();
        assertFalse(icd.isDeleted());
        assertNull(icd.getDeletedAt());
        assertNull(icd.getDeletedBy());
    }
}
