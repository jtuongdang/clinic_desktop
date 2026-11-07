package vn.clinicmanagement.domain.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NhanVienWorkScopeTest {

    @Test
    void constructsValidWorkScopeAndExposesRecordComponents() {
        NhanVienWorkScope scope = new NhanVienWorkScope(3, "Synthetic facility", 8, "Synthetic department");

        assertEquals(3, scope.maCoSo());
        assertEquals("Synthetic facility", scope.tenCoSo());
        assertEquals(8, scope.maKhoa());
        assertEquals("Synthetic department", scope.tenKhoa());
        assertEquals(
                new NhanVienWorkScope(3, "Synthetic facility", 8, "Synthetic department"),
                scope);
    }

    @Test
    void rejectsNonPositiveFacilityOrDepartmentIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new NhanVienWorkScope(0, "Facility", 8, "Department"));
        assertThrows(IllegalArgumentException.class,
                () -> new NhanVienWorkScope(-1, "Facility", 8, "Department"));
        assertThrows(IllegalArgumentException.class,
                () -> new NhanVienWorkScope(3, "Facility", 0, "Department"));
        assertThrows(IllegalArgumentException.class,
                () -> new NhanVienWorkScope(3, "Facility", -1, "Department"));
    }

    @Test
    void rejectsNullOrBlankFacilityAndDepartmentNames() {
        assertThrows(IllegalArgumentException.class,
                () -> new NhanVienWorkScope(3, null, 8, "Department"));
        assertThrows(IllegalArgumentException.class,
                () -> new NhanVienWorkScope(3, " ", 8, "Department"));
        assertThrows(IllegalArgumentException.class,
                () -> new NhanVienWorkScope(3, "Facility", 8, null));
        assertThrows(IllegalArgumentException.class,
                () -> new NhanVienWorkScope(3, "Facility", 8, "\t"));
    }
}
