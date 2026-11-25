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

class NhatKyHeThongTest {

    @Test
    void initializesDefaultsAndBaseEntityMetadata() {
        NhatKyHeThong log = new NhatKyHeThong();

        assertNull(log.getId());
        assertEquals(0L, log.getVersion());
        assertNull(log.getMaNhanVien());
        assertEquals("", log.getHanhDong());
        assertEquals("", log.getChiTiet());
        assertNull(log.getIpAddress());
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
        NhatKyHeThong log = new NhatKyHeThong();
        LocalDateTime eventTime = LocalDateTime.of(2026, 11, 7, 10, 30, 0);
        LocalDateTime createdTime = LocalDateTime.of(2026, 11, 7, 10, 0, 0);

        log.setId(101);
        log.setVersion(5L);
        log.setMaNhanVien(15);
        log.setHanhDong("LOGIN_SUCCESS");
        log.setChiTiet("Người dùng đăng nhập thành công từ máy trạm");
        log.setIpAddress("192.168.1.100");
        log.setThoiGian(eventTime);
        log.setCreatedBy(15);
        log.setCreatedAt(createdTime);

        assertEquals(101, log.getId());
        assertEquals(5L, log.getVersion());
        assertEquals(15, log.getMaNhanVien());
        assertEquals("LOGIN_SUCCESS", log.getHanhDong());
        assertEquals("Người dùng đăng nhập thành công từ máy trạm", log.getChiTiet());
        assertEquals("192.168.1.100", log.getIpAddress());
        assertEquals(eventTime, log.getThoiGian());
        assertEquals(15, log.getCreatedBy());
        assertEquals(createdTime, log.getCreatedAt());

        log.setMaNhanVien(null);
        log.setIpAddress(null);
        log.setHanhDong(null);
        log.setChiTiet(null);

        assertNull(log.getMaNhanVien());
        assertNull(log.getIpAddress());
        assertNull(log.getHanhDong());
        assertNull(log.getChiTiet());
    }

    @Test
    void enforcesAuditImmutabilityInvariantsForLifecycleMethods() {
        NhatKyHeThong log = new NhatKyHeThong();

        UnsupportedOperationException updateEx = assertThrows(
                UnsupportedOperationException.class,
                () -> log.markAsUpdated(1));
        assertEquals("Nhật ký hệ thống không thể cập nhật vòng đời.", updateEx.getMessage());

        UnsupportedOperationException softDeleteEx = assertThrows(
                UnsupportedOperationException.class,
                () -> log.softDelete(1));
        assertEquals("Nhật ký hệ thống không thể xóa mềm.", softDeleteEx.getMessage());

        UnsupportedOperationException restoreEx = assertThrows(
                UnsupportedOperationException.class,
                log::restore);
        assertEquals("Nhật ký hệ thống không thể khôi phục.", restoreEx.getMessage());

        UnsupportedOperationException setDeletedTrueEx = assertThrows(
                UnsupportedOperationException.class,
                () -> log.setDeleted(true));
        assertEquals("Không thể thay đổi trạng thái xóa của nhật ký hệ thống.", setDeletedTrueEx.getMessage());

        UnsupportedOperationException setDeletedFalseEx = assertThrows(
                UnsupportedOperationException.class,
                () -> log.setDeleted(false));
        assertEquals("Không thể thay đổi trạng thái xóa của nhật ký hệ thống.", setDeletedFalseEx.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -99})
    void rejectsInvalidCreatorId(int invalidCreatorId) {
        NhatKyHeThong log = new NhatKyHeThong();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> log.setCreatedBy(invalidCreatorId));
        assertEquals("Người tạo bản ghi phải là người dùng hợp lệ.", ex.getMessage());
    }

    @Test
    void allowsValidOrNullCreatorId() {
        NhatKyHeThong log = new NhatKyHeThong();

        log.setCreatedBy(12);
        assertEquals(12, log.getCreatedBy());

        log.setCreatedBy(null);
        assertNull(log.getCreatedBy());
    }

    @Test
    void verifiesEntityIdentityAndEquality() {
        NhatKyHeThong log1 = new NhatKyHeThong();
        log1.setId(100);

        NhatKyHeThong log2 = new NhatKyHeThong();
        log2.setId(100);

        NhatKyHeThong log3 = new NhatKyHeThong();
        log3.setId(200);

        NhatKyHeThong logNullId1 = new NhatKyHeThong();
        NhatKyHeThong logNullId2 = new NhatKyHeThong();

        assertEquals(log1, log1);
        assertEquals(log1, log2);
        assertEquals(log1.hashCode(), log2.hashCode());

        assertNotEquals(log1, log3);
        assertNotEquals(log1, logNullId1);
        assertNotEquals(logNullId1, logNullId2);
        assertNotEquals(log1, null);
        assertNotEquals(log1, "not-an-entity");
    }
}
