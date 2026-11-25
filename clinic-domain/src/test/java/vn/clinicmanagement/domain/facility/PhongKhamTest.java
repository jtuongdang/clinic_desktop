package vn.clinicmanagement.domain.facility;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhongKhamTest {

    @Test
    void initializesDefaultsAndUnassignedDepartment() {
        PhongKham room = new PhongKham();

        assertEquals(0, room.getMaKhoa());
        assertEquals("", room.getTenPhong());
        assertNull(room.getViTri());
        assertTrue(room.isHoatDong());
        assertNull(room.getKhoa());
        assertEquals(0L, room.getVersion());
        assertNotNull(room.getCreatedAt());
        assertFalse(room.isDeleted());
    }

    @Test
    void exposesRoomPropertiesThroughGettersAndSetters() {
        PhongKham room = new PhongKham();
        Khoa department = new Khoa();

        room.setId(17);
        room.setMaKhoa(8);
        room.setTenPhong("Phòng khám tổng hợp");
        room.setViTri("Tầng 2");
        room.setHoatDong(false);
        room.setKhoa(department);

        assertEquals(17, room.getId());
        assertEquals(8, room.getMaKhoa());
        assertEquals("Phòng khám tổng hợp", room.getTenPhong());
        assertEquals("Tầng 2", room.getViTri());
        assertFalse(room.isHoatDong());
        assertEquals(department, room.getKhoa());

        room.setViTri(null);
        room.setKhoa(null);
        assertNull(room.getViTri());
        assertNull(room.getKhoa());
    }

    @Test
    void supportsUpdateSoftDeleteAndRestoreLifecycleAndValidatesActors() {
        PhongKham room = new PhongKham();
        LocalDateTime beforeUpdate = LocalDateTime.now();

        room.markAsUpdated(41);

        assertEquals(41, room.getUpdatedBy());
        assertNotNull(room.getUpdatedAt());
        assertFalse(room.getUpdatedAt().isBefore(beforeUpdate));
        assertThrows(IllegalArgumentException.class, () -> room.markAsUpdated(0));
        assertThrows(IllegalArgumentException.class, () -> room.softDelete(-1));

        room.softDelete(42);
        assertTrue(room.isDeleted());
        assertEquals(42, room.getDeletedBy());
        assertNotNull(room.getDeletedAt());

        room.restore();
        assertFalse(room.isDeleted());
        assertNull(room.getDeletedAt());
        assertNull(room.getDeletedBy());
    }
}
