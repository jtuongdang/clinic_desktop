package vn.clinicmanagement.domain.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThamSoHeThongTest {

    @Test
    void initializesDefaultsAndBaseEntityMetadata() {
        ThamSoHeThong param = new ThamSoHeThong();

        assertNull(param.getId());
        assertEquals(0L, param.getVersion());
        assertEquals("", param.getMaThamSo());
        assertEquals("", param.getTenThamSo());
        assertEquals("", param.getGiaTri());
        assertNull(param.getMoTa());
        assertFalse(param.isDeleted());
        assertNotNull(param.getCreatedAt());
        assertNull(param.getCreatedBy());
        assertNull(param.getUpdatedAt());
        assertNull(param.getUpdatedBy());
        assertNull(param.getDeletedAt());
        assertNull(param.getDeletedBy());
        assertNotNull(param.getDomainEvents());
        assertTrue(param.getDomainEvents().isEmpty());
    }

    @Test
    void exposesPropertiesThroughGettersAndSetters() {
        ThamSoHeThong param = new ThamSoHeThong();

        param.setId(10);
        param.setVersion(3L);
        param.setMaThamSo("CLINIC_NAME");
        param.setTenThamSo("Tên Phòng Khám");
        param.setGiaTri("Phòng Khám Đa Khoa Quốc Tế EHC");
        param.setMoTa("Tên đơn vị sử dụng hiển thị trên đầu phiếu in");

        assertEquals(10, param.getId());
        assertEquals(3L, param.getVersion());
        assertEquals("CLINIC_NAME", param.getMaThamSo());
        assertEquals("Tên Phòng Khám", param.getTenThamSo());
        assertEquals("Phòng Khám Đa Khoa Quốc Tế EHC", param.getGiaTri());
        assertEquals("Tên đơn vị sử dụng hiển thị trên đầu phiếu in", param.getMoTa());

        param.setMoTa(null);
        param.setGiaTri(null);
        param.setMaThamSo(null);
        param.setTenThamSo(null);

        assertNull(param.getMoTa());
        assertNull(param.getGiaTri());
        assertNull(param.getMaThamSo());
        assertNull(param.getTenThamSo());
    }

    @Test
    void supportsAuditLifecycleSoftDeleteAndRestore() {
        ThamSoHeThong param = new ThamSoHeThong();
        LocalDateTime beforeUpdate = LocalDateTime.now();

        param.markAsUpdated(41);
        assertEquals(41, param.getUpdatedBy());
        assertNotNull(param.getUpdatedAt());
        assertFalse(param.getUpdatedAt().isBefore(beforeUpdate));

        param.softDelete(42);
        assertTrue(param.isDeleted());
        assertEquals(42, param.getDeletedBy());
        assertNotNull(param.getDeletedAt());

        // Repeated soft delete is idempotent
        param.softDelete(43);
        assertEquals(42, param.getDeletedBy());

        param.restore();
        assertFalse(param.isDeleted());
        assertNull(param.getDeletedAt());
        assertNull(param.getDeletedBy());

        // Repeated restore is idempotent
        param.restore();
        assertFalse(param.isDeleted());

        param.setDeleted(true);
        assertTrue(param.isDeleted());
        param.setDeleted(false);
        assertFalse(param.isDeleted());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void rejectsInvalidLifecycleActors(int invalidActorId) {
        ThamSoHeThong param = new ThamSoHeThong();

        assertThrows(IllegalArgumentException.class, () -> param.setCreatedBy(invalidActorId));
        assertThrows(IllegalArgumentException.class, () -> param.setUpdatedBy(invalidActorId));
        assertThrows(IllegalArgumentException.class, () -> param.setDeletedBy(invalidActorId));
        assertThrows(IllegalArgumentException.class, () -> param.markAsUpdated(invalidActorId));
        assertThrows(IllegalArgumentException.class, () -> param.softDelete(invalidActorId));
    }

    @Test
    void verifiesEntityIdentityAndEquality() {
        ThamSoHeThong param1 = new ThamSoHeThong();
        param1.setId(10);

        ThamSoHeThong param2 = new ThamSoHeThong();
        param2.setId(10);

        ThamSoHeThong param3 = new ThamSoHeThong();
        param3.setId(20);

        ThamSoHeThong nullId1 = new ThamSoHeThong();
        ThamSoHeThong nullId2 = new ThamSoHeThong();

        assertEquals(param1, param1);
        assertEquals(param1, param2);
        assertEquals(param1.hashCode(), param2.hashCode());

        assertNotEquals(param1, param3);
        assertNotEquals(param1, nullId1);
        assertNotEquals(nullId1, nullId2);
        assertNotEquals(param1, null);
        assertNotEquals(param1, "unrelated");
    }

    @Test
    void parsesBooleanTypedParameterHelper() {
        ThamSoHeThong param = new ThamSoHeThong();

        param.setGiaTri("true");
        assertTrue(asBoolean(param, false));

        param.setGiaTri("TRUE");
        assertTrue(asBoolean(param, false));

        param.setGiaTri("false");
        assertFalse(asBoolean(param, true));

        param.setGiaTri(null);
        assertTrue(asBoolean(param, true));
        assertFalse(asBoolean(param, false));

        param.setGiaTri("");
        assertTrue(asBoolean(param, true));
        assertFalse(asBoolean(param, false));

        param.setGiaTri("   ");
        assertTrue(asBoolean(param, true));
    }

    @Test
    void parsesIntegerTypedParameterHelper() {
        ThamSoHeThong param = new ThamSoHeThong();

        param.setGiaTri("50");
        assertEquals(50, asInt(param, 10));

        param.setGiaTri("  100  ");
        assertEquals(100, asInt(param, 10));

        param.setGiaTri(null);
        assertEquals(10, asInt(param, 10));

        param.setGiaTri("");
        assertEquals(10, asInt(param, 10));

        param.setGiaTri("not-a-number");
        assertEquals(10, asInt(param, 10));
    }

    @Test
    void parsesLongTypedParameterHelper() {
        ThamSoHeThong param = new ThamSoHeThong();

        param.setGiaTri("86400000");
        assertEquals(86400000L, asLong(param, 1000L));

        param.setGiaTri(null);
        assertEquals(1000L, asLong(param, 1000L));

        param.setGiaTri("invalid");
        assertEquals(1000L, asLong(param, 1000L));
    }

    @Test
    void parsesDecimalTypedParameterHelper() {
        ThamSoHeThong param = new ThamSoHeThong();

        param.setGiaTri("0.08");
        assertEquals(new BigDecimal("0.08"), asBigDecimal(param, BigDecimal.ZERO));

        param.setGiaTri(null);
        assertEquals(BigDecimal.ZERO, asBigDecimal(param, BigDecimal.ZERO));

        param.setGiaTri("invalid-decimal");
        assertEquals(BigDecimal.ZERO, asBigDecimal(param, BigDecimal.ZERO));
    }

    @Test
    void parsesStringTypedParameterHelperWithDefault() {
        ThamSoHeThong param = new ThamSoHeThong();

        param.setGiaTri("Custom Value");
        assertEquals("Custom Value", asString(param, "Default Value"));

        param.setGiaTri(null);
        assertEquals("Default Value", asString(param, "Default Value"));

        param.setGiaTri("   ");
        assertEquals("Default Value", asString(param, "Default Value"));
    }

    private static boolean asBoolean(ThamSoHeThong param, boolean defaultValue) {
        if (param == null || param.getGiaTri() == null || param.getGiaTri().trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(param.getGiaTri().trim());
    }

    private static int asInt(ThamSoHeThong param, int defaultValue) {
        if (param == null || param.getGiaTri() == null || param.getGiaTri().trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(param.getGiaTri().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long asLong(ThamSoHeThong param, long defaultValue) {
        if (param == null || param.getGiaTri() == null || param.getGiaTri().trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(param.getGiaTri().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static BigDecimal asBigDecimal(ThamSoHeThong param, BigDecimal defaultValue) {
        if (param == null || param.getGiaTri() == null || param.getGiaTri().trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(param.getGiaTri().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String asString(ThamSoHeThong param, String defaultValue) {
        if (param == null || param.getGiaTri() == null || param.getGiaTri().trim().isEmpty()) {
            return defaultValue;
        }
        return param.getGiaTri().trim();
    }
}
