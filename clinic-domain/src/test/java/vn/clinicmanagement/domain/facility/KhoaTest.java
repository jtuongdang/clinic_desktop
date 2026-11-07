package vn.clinicmanagement.domain.facility;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KhoaTest {

    @Test
    void initializesDefaultsAndEmptyRoomCollection() {
        Khoa department = new Khoa();

        assertEquals(0, department.getMaCoSo());
        assertEquals("", department.getTenKhoa());
        assertNull(department.getMoTa());
        assertTrue(department.isHoatDong());
        assertNull(department.getCoSo());
        assertNotNull(department.getPhongKhams());
        assertTrue(department.getPhongKhams().isEmpty());
        assertFalse(department.isDeleted());
        assertNotNull(department.getCreatedAt());
    }

    @Test
    void exposesDepartmentPropertiesAndRoomCollectionThroughGettersAndSetters() {
        Khoa department = new Khoa();
        CoSo facility = new CoSo();
        PhongKham room = new PhongKham();
        List<PhongKham> rooms = new ArrayList<>(List.of(room));

        department.setId(23);
        department.setMaCoSo(12);
        department.setTenKhoa("Nội tổng quát");
        department.setMoTa("Khoa khám bệnh tổng hợp");
        department.setHoatDong(false);
        department.setCoSo(facility);
        department.setPhongKhams(rooms);

        assertEquals(23, department.getId());
        assertEquals(12, department.getMaCoSo());
        assertEquals("Nội tổng quát", department.getTenKhoa());
        assertEquals("Khoa khám bệnh tổng hợp", department.getMoTa());
        assertFalse(department.isHoatDong());
        assertEquals(facility, department.getCoSo());
        assertEquals(rooms, department.getPhongKhams());
        assertEquals(room, department.getPhongKhams().get(0));

        department.setMoTa(null);
        department.setCoSo(null);
        department.setPhongKhams(null);
        assertNull(department.getMoTa());
        assertNull(department.getCoSo());
        assertNull(department.getPhongKhams());
    }

    @Test
    void supportsUpdateSoftDeleteAndRestoreLifecycleAndValidatesActors() {
        Khoa department = new Khoa();
        LocalDateTime beforeUpdate = LocalDateTime.now();

        department.markAsUpdated(51);

        assertEquals(51, department.getUpdatedBy());
        assertNotNull(department.getUpdatedAt());
        assertFalse(department.getUpdatedAt().isBefore(beforeUpdate));
        assertThrows(IllegalArgumentException.class, () -> department.markAsUpdated(0));
        assertThrows(IllegalArgumentException.class, () -> department.softDelete(-1));

        department.softDelete(52);
        assertTrue(department.isDeleted());
        assertEquals(52, department.getDeletedBy());
        assertNotNull(department.getDeletedAt());

        department.restore();
        assertFalse(department.isDeleted());
        assertNull(department.getDeletedAt());
        assertNull(department.getDeletedBy());
    }
}
