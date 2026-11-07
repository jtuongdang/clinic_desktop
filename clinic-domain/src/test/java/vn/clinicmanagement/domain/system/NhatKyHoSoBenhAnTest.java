package vn.clinicmanagement.domain.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NhatKyHoSoBenhAnTest {

    @Test
    void initializesDefaultsAndBaseEntityMetadata() {
        NhatKyHoSoBenhAn log = new NhatKyHoSoBenhAn();

        assertNull(log.getId());
        assertEquals(0L, log.getVersion());
        assertEquals(0, log.getMaLuotKham());
        assertEquals(0, log.getMaNhanVien());
        assertEquals("", log.getHanhDong());
        assertNull(log.getLyDo());
        assertNotNull(log.getThoiGian());
        assertFalse(log.getThoiGian().isAfter(LocalDateTime.now().plusSeconds(1)));
        assertFalse(log.isDeleted());
        assertNotNull(log.getCreatedAt());
        assertNull(log.getCreatedBy());
        assertNull(log.getUpdatedAt());
        assertNull(log.getUpdatedBy());
        assertNull(log.getDeletedAt());
        assertNull(log.getDeletedBy());
        assertNotNull(log.getDomainEvents());
        assertTrue(log.getDomainEvents().isEmpty());
    }

    @Test
    void exposesPropertiesThroughGettersAndSetters() {
        NhatKyHoSoBenhAn log = new NhatKyHoSoBenhAn();
        LocalDateTime eventTime = LocalDateTime.of(2026, 11, 7, 11, 15, 0);
        LocalDateTime createdTime = LocalDateTime.of(2026, 11, 7, 11, 0, 0);

        log.setId(205);
        log.setVersion(2L);
        log.setMaLuotKham(501);
        log.setMaNhanVien(88);
        log.setHanhDong("XEM_HO_SO");
        log.setLyDo("Hội chẩn chuyên khoa Nội tim mạch");
        log.setThoiGian(eventTime);
        log.setCreatedBy(88);
        log.setCreatedAt(createdTime);

        assertEquals(205, log.getId());
        assertEquals(2L, log.getVersion());
        assertEquals(501, log.getMaLuotKham());
        assertEquals(88, log.getMaNhanVien());
        assertEquals("XEM_HO_SO", log.getHanhDong());
        assertEquals("Hội chẩn chuyên khoa Nội tim mạch", log.getLyDo());
        assertEquals(eventTime, log.getThoiGian());
        assertEquals(88, log.getCreatedBy());
        assertEquals(createdTime, log.getCreatedAt());

        log.setLyDo(null);
        log.setHanhDong(null);

        assertNull(log.getLyDo());
        assertNull(log.getHanhDong());
    }

    @Test
    void enforcesEmrAuditImmutabilityInvariants() {
        NhatKyHoSoBenhAn log = new NhatKyHoSoBenhAn();

        UnsupportedOperationException updateEx = assertThrows(
                UnsupportedOperationException.class,
                () -> log.markAsUpdated(1));
        assertEquals("Nhật ký hồ sơ bệnh án không thể cập nhật vòng đời.", updateEx.getMessage());

        UnsupportedOperationException softDeleteEx = assertThrows(
                UnsupportedOperationException.class,
                () -> log.softDelete(1));
        assertEquals("Nhật ký hồ sơ bệnh án không thể xóa mềm.", softDeleteEx.getMessage());

        UnsupportedOperationException restoreEx = assertThrows(
                UnsupportedOperationException.class,
                log::restore);
        assertEquals("Nhật ký hồ sơ bệnh án không thể khôi phục.", restoreEx.getMessage());

        UnsupportedOperationException setDeletedTrueEx = assertThrows(
                UnsupportedOperationException.class,
                () -> log.setDeleted(true));
        assertEquals("Không thể thay đổi trạng thái xóa của nhật ký hồ sơ bệnh án.", setDeletedTrueEx.getMessage());

        UnsupportedOperationException setDeletedFalseEx = assertThrows(
                UnsupportedOperationException.class,
                () -> log.setDeleted(false));
        assertEquals("Không thể thay đổi trạng thái xóa của nhật ký hồ sơ bệnh án.", setDeletedFalseEx.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -50})
    void rejectsInvalidCreatorId(int invalidCreatorId) {
        NhatKyHoSoBenhAn log = new NhatKyHoSoBenhAn();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> log.setCreatedBy(invalidCreatorId));
        assertEquals("Người tạo bản ghi phải là người dùng hợp lệ.", ex.getMessage());
    }

    @Test
    void allowsValidOrNullCreatorId() {
        NhatKyHoSoBenhAn log = new NhatKyHoSoBenhAn();

        log.setCreatedBy(35);
        assertEquals(35, log.getCreatedBy());

        log.setCreatedBy(null);
        assertNull(log.getCreatedBy());
    }

    @Test
    void verifiesEntityIdentityAndEquality() {
        NhatKyHoSoBenhAn log1 = new NhatKyHoSoBenhAn();
        log1.setId(301);

        NhatKyHoSoBenhAn log2 = new NhatKyHoSoBenhAn();
        log2.setId(301);

        NhatKyHoSoBenhAn log3 = new NhatKyHoSoBenhAn();
        log3.setId(401);

        NhatKyHoSoBenhAn logNullId1 = new NhatKyHoSoBenhAn();
        NhatKyHoSoBenhAn logNullId2 = new NhatKyHoSoBenhAn();

        assertEquals(log1, log1);
        assertEquals(log1, log2);
        assertEquals(log1.hashCode(), log2.hashCode());

        assertNotEquals(log1, log3);
        assertNotEquals(log1, logNullId1);
        assertNotEquals(logNullId1, logNullId2);
        assertNotEquals(log1, null);
        assertNotEquals(log1, "different-type");
    }
}
