package vn.clinicmanagement.application.authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaDuyetVaiTroMaTest {

    @Test
    @DisplayName("Private constructor should be private and instantiable via reflection")
    void constructor_isPrivate() throws Exception {
        Constructor<DaDuyetVaiTroMa> constructor = DaDuyetVaiTroMa.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        DaDuyetVaiTroMa instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("ALLOWED constant contains exactly the 6 expected approved roles and is immutable")
    void allowedConstant_containsApprovedRolesAndIsImmutable() {
        Set<String> expected = Set.of(
                "ADMIN", "DOCTOR", "RECEPTIONIST", "PHARMACIST", "CASHIER", "LAB_TECH"
        );
        assertEquals(expected, DaDuyetVaiTroMa.ALLOWED);
        assertEquals(6, DaDuyetVaiTroMa.ALLOWED.size());
        assertThrows(UnsupportedOperationException.class, () -> DaDuyetVaiTroMa.ALLOWED.add("UNAPPROVED_ROLE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "  \t  \n  "})
    @DisplayName("normalize returns null for null, empty, or blank strings")
    void normalize_nullEmptyOrBlank_returnsNull(String input) {
        assertNull(DaDuyetVaiTroMa.normalize(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ADMIN",
            "admin",
            "  admin  ",
            "Admin",
            "aDmIn"
    })
    @DisplayName("normalize trims and converts ADMIN role variations to uppercase")
    void normalize_adminVariations_normalizesToUppercase(String input) {
        assertEquals("ADMIN", DaDuyetVaiTroMa.normalize(input));
    }

    @Test
    @DisplayName("normalize properly trims and uppercases arbitrary role codes using Locale.ROOT")
    void normalize_variousRoleCodes_normalizesCorrectly() {
        assertEquals("DOCTOR", DaDuyetVaiTroMa.normalize("  doctor  "));
        assertEquals("RECEPTIONIST", DaDuyetVaiTroMa.normalize("\treceptionist\n"));
        assertEquals("PHARMACIST", DaDuyetVaiTroMa.normalize("pharmacist"));
        assertEquals("CASHIER", DaDuyetVaiTroMa.normalize("  Cashier  "));
        assertEquals("LAB_TECH", DaDuyetVaiTroMa.normalize("lab_tech"));
        assertEquals("CUSTOM_ROLE", DaDuyetVaiTroMa.normalize("  custom_role  "));
        assertEquals("TITLE", DaDuyetVaiTroMa.normalize("title"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ADMIN", "DOCTOR", "RECEPTIONIST", "PHARMACIST", "CASHIER", "LAB_TECH"
    })
    @DisplayName("isAllowed returns true for canonical uppercase approved roles")
    void isAllowed_canonicalUppercaseRoles_returnsTrue(String roleCode) {
        assertTrue(DaDuyetVaiTroMa.isAllowed(roleCode));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "admin", "doctor", "receptionist", "pharmacist", "cashier", "lab_tech"
    })
    @DisplayName("isAllowed returns true for lowercase approved roles")
    void isAllowed_lowercaseRoles_returnsTrue(String roleCode) {
        assertTrue(DaDuyetVaiTroMa.isAllowed(roleCode));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "  ADMIN  ", "  doctor  ", "\tRECEPTIONIST\n", "  Pharmacist  ", " cashier ", " Lab_Tech "
    })
    @DisplayName("isAllowed returns true for approved roles with whitespace and mixed casing")
    void isAllowed_whitespaceAndMixedCasing_returnsTrue(String roleCode) {
        assertTrue(DaDuyetVaiTroMa.isAllowed(roleCode));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "\t\n",
            "NURSE",
            "PATIENT",
            "ACCOUNTANT",
            "SUPERUSER",
            "DOC",
            "DOCTORS",
            "LABTECH",
            "ADMIN_USER",
            "UNKNOWN"
    })
    @DisplayName("isAllowed returns false for null, blank, and unauthorized role codes")
    void isAllowed_unauthorizedOrInvalidRoles_returnsFalse(String roleCode) {
        assertFalse(DaDuyetVaiTroMa.isAllowed(roleCode));
    }
}
