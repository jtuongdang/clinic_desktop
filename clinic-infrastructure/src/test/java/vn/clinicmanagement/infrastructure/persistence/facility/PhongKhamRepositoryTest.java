package vn.clinicmanagement.infrastructure.persistence.facility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PhongKhamRepositoryTest {
    @Test
    void activeRoomPickerSqlFiltersExactFacilityAndDepartmentAndSoftDeleteState() {
        String sql = PhongKhamRepository.scopedActiveRoomsSql();

        assertTrue(sql.contains("k.maCoSo=?"));
        assertTrue(sql.contains("k.id=?"));
        assertTrue(sql.contains("p.hoatDong=1"));
        assertTrue(sql.contains("p.isDeleted=0"));
        assertTrue(sql.contains("k.isDeleted=0"));
        assertTrue(sql.contains("c.isDeleted=0"));
    }
}
