package vn.clinicmanagement.domain.facility;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.LoaiCoSo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoSoTest {

    @Test
    void initializesDefaultsAndEmptyDepartmentCollection() {
        CoSo facility = new CoSo();

        assertInstanceOf(IAggregateRoot.class, facility);
        assertEquals("", facility.getTenCoSo());
        assertEquals("", facility.getDiaChi());
        assertEquals("", facility.getDienThoai());
        assertNull(facility.getEmail());
        assertEquals(LoaiCoSo.PHONG_KHAM_DA_KHOA, facility.getLoaiCoSo());
        assertTrue(facility.isHoatDong());
        assertNotNull(facility.getKhoas());
        assertTrue(facility.getKhoas().isEmpty());
        assertFalse(facility.isDeleted());
        assertNotNull(facility.getCreatedAt());
    }

    @Test
    void exposesFacilityPropertiesAndDepartmentCollectionThroughGettersAndSetters() {
        CoSo facility = new CoSo();
        Khoa department = new Khoa();
        List<Khoa> departments = new ArrayList<>(List.of(department));

        facility.setId(31);
        facility.setTenCoSo("Phòng khám mẫu");
        facility.setDiaChi("Đường Tổng hợp");
        facility.setDienThoai("0281234567");
        facility.setEmail("clinic@example.test");
        facility.setLoaiCoSo(LoaiCoSo.PHONG_KHAM_DA_KHOA);
        facility.setHoatDong(false);
        facility.setKhoas(departments);

        assertEquals(31, facility.getId());
        assertEquals("Phòng khám mẫu", facility.getTenCoSo());
        assertEquals("Đường Tổng hợp", facility.getDiaChi());
        assertEquals("0281234567", facility.getDienThoai());
        assertEquals("clinic@example.test", facility.getEmail());
        assertEquals(LoaiCoSo.PHONG_KHAM_DA_KHOA, facility.getLoaiCoSo());
        assertFalse(facility.isHoatDong());
        assertEquals(departments, facility.getKhoas());
        assertEquals(department, facility.getKhoas().get(0));

        facility.setEmail(null);
        facility.setKhoas(null);
        assertNull(facility.getEmail());
        assertNull(facility.getKhoas());
    }

    @Test
    void supportsUpdateSoftDeleteAndRestoreLifecycleAndValidatesActors() {
        CoSo facility = new CoSo();
        LocalDateTime beforeUpdate = LocalDateTime.now();

        facility.markAsUpdated(61);

        assertEquals(61, facility.getUpdatedBy());
        assertNotNull(facility.getUpdatedAt());
        assertFalse(facility.getUpdatedAt().isBefore(beforeUpdate));
        assertThrows(IllegalArgumentException.class, () -> facility.markAsUpdated(0));
        assertThrows(IllegalArgumentException.class, () -> facility.softDelete(-1));

        facility.softDelete(62);
        assertTrue(facility.isDeleted());
        assertEquals(62, facility.getDeletedBy());
        assertNotNull(facility.getDeletedAt());

        facility.restore();
        assertFalse(facility.isDeleted());
        assertNull(facility.getDeletedAt());
        assertNull(facility.getDeletedBy());
    }
}
