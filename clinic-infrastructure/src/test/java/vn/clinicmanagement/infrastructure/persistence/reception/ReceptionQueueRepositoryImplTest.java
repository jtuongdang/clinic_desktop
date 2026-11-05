package vn.clinicmanagement.infrastructure.persistence.reception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceptionQueueRepositoryImplTest {
    @Test
    void queueSqlFiltersExactFacilityAndDepartment() {
        String sql = ReceptionQueueRepositoryImpl.scopedQueueSql();

        assertTrue(sql.contains("l.maCoSo=?"));
        assertTrue(sql.contains("l.maKhoa=?"));
        assertTrue(sql.contains("l.isDeleted=0"));
        assertTrue(sql.contains("p.isDeleted=0"));
        assertTrue(sql.contains("l.trangThai<>'DA_HUY'"));
    }

    @Test
    void roomSummarySqlFiltersRoomsAndJoinedVisitsByBothScopeKeys() {
        String sql = ReceptionQueueRepositoryImpl.scopedRoomSummariesSql();

        assertTrue(sql.contains("l.maCoSo=?"));
        assertTrue(sql.contains("l.maKhoa=?"));
        assertTrue(sql.contains("k.maCoSo=?"));
        assertTrue(sql.contains("k.id=?"));
        assertTrue(sql.contains("l.isDeleted=0"));
        assertTrue(sql.contains("k.isDeleted=0"));
        assertTrue(sql.contains("p.isDeleted=0"));
    }
}
