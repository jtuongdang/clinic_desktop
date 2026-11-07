package vn.clinicmanagement.domain.patient;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.facility.CoSo;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenhNhanCoSoTest {

    @Test
    void initializesRegistrationTimeAndUnassignedRelationships() {
        LocalDateTime beforeCreation = LocalDateTime.now();
        BenhNhanCoSo registration = new BenhNhanCoSo();
        LocalDateTime afterCreation = LocalDateTime.now();

        assertEquals(0, registration.getMaBenhNhan());
        assertEquals(0, registration.getMaCoSo());
        assertNotNull(registration.getNgayDangKy());
        assertFalse(registration.getNgayDangKy().isBefore(beforeCreation));
        assertFalse(registration.getNgayDangKy().isAfter(afterCreation));
        assertNull(registration.getBenhNhan());
        assertNull(registration.getCoSo());
        assertFalse(registration.isDeleted());
        assertNotNull(registration.getCreatedAt());
    }

    @Test
    void exposesPatientFacilityAndRegistrationPropertiesThroughGettersAndSetters() {
        BenhNhanCoSo registration = new BenhNhanCoSo();
        BenhNhan patient = new BenhNhan();
        CoSo facility = new CoSo();
        LocalDateTime registeredAt = LocalDateTime.of(2025, 6, 15, 9, 30);

        registration.setMaBenhNhan(73);
        registration.setMaCoSo(19);
        registration.setNgayDangKy(registeredAt);
        registration.setBenhNhan(patient);
        registration.setCoSo(facility);

        assertEquals(73, registration.getMaBenhNhan());
        assertEquals(19, registration.getMaCoSo());
        assertEquals(registeredAt, registration.getNgayDangKy());
        assertEquals(patient, registration.getBenhNhan());
        assertEquals(facility, registration.getCoSo());

        registration.setNgayDangKy(null);
        assertNull(registration.getNgayDangKy());
    }

    @Test
    void supportsUpdateSoftDeleteAndRestoreLifecycleAndValidatesActors() {
        BenhNhanCoSo registration = new BenhNhanCoSo();

        registration.markAsUpdated(41);
        assertEquals(41, registration.getUpdatedBy());
        assertNotNull(registration.getUpdatedAt());
        assertThrows(IllegalArgumentException.class, () -> registration.markAsUpdated(0));
        assertThrows(IllegalArgumentException.class, () -> registration.softDelete(-1));

        registration.softDelete(42);
        assertTrue(registration.isDeleted());
        assertEquals(42, registration.getDeletedBy());
        assertNotNull(registration.getDeletedAt());

        registration.restore();
        assertFalse(registration.isDeleted());
        assertNull(registration.getDeletedAt());
        assertNull(registration.getDeletedBy());
    }
}
