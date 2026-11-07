package vn.clinicmanagement.domain.identity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaiTroTest {

    @Test
    void initializesRoleDefaultsAndBaseEntityMetadata() {
        VaiTro role = new VaiTro();

        assertEquals("", role.getTenVaiTro());
        assertNull(role.getMoTa());
        assertEquals(0L, role.getVersion());
        assertFalse(role.isDeleted());
        assertNotNull(role.getCreatedAt());
    }

    @Test
    void exposesRoleAndBaseEntityPropertiesThroughGettersAndSetters() {
        VaiTro role = new VaiTro();
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 2, 8, 30);

        role.setId(9);
        role.setTenVaiTro("Bac si");
        role.setMoTa("Synthetic role description");
        role.setVersion(3L);
        role.setCreatedAt(createdAt);
        role.setCreatedBy(12);

        assertEquals(9, role.getId());
        assertEquals("Bac si", role.getTenVaiTro());
        assertEquals("Synthetic role description", role.getMoTa());
        assertEquals(3L, role.getVersion());
        assertEquals(createdAt, role.getCreatedAt());
        assertEquals(12, role.getCreatedBy());

        role.setTenVaiTro(null);
        role.setMoTa(null);
        assertNull(role.getTenVaiTro());
        assertNull(role.getMoTa());
    }

    @Test
    void supportsUpdateSoftDeleteAndRestoreLifecycle() {
        VaiTro role = new VaiTro();

        role.markAsUpdated(31);
        assertEquals(31, role.getUpdatedBy());
        assertNotNull(role.getUpdatedAt());

        role.softDelete(32);
        assertTrue(role.isDeleted());
        assertEquals(32, role.getDeletedBy());
        assertNotNull(role.getDeletedAt());

        role.restore();
        assertFalse(role.isDeleted());
        assertNull(role.getDeletedAt());
        assertNull(role.getDeletedBy());
    }

    @Test
    void rejectsInvalidLifecycleActorsAndCreatorMetadata() {
        VaiTro role = new VaiTro();

        assertThrows(IllegalArgumentException.class, () -> role.markAsUpdated(0));
        assertThrows(IllegalArgumentException.class, () -> role.softDelete(-1));
        assertThrows(IllegalArgumentException.class, () -> role.setCreatedBy(0));
        assertThrows(IllegalArgumentException.class, () -> role.setCreatedBy(-1));
    }
}
