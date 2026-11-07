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

class ApprovedRoleCodesTest {

    @Test
    @DisplayName("Private constructor should be private and instantiable via reflection")
    void constructor_isPrivate() throws Exception {
        Constructor<ApprovedRoleCodes> constructor = ApprovedRoleCodes.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        ApprovedRoleCodes instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("ALLOWED constant contains exactly the 6 expected approved roles and is immutable")
    void allowedConstant_containsApprovedRolesAndIsImmutable() {
        Set<String> expected = Set.of(
                "ADMIN", "DOCTOR", "RECEPTIONIST", "PHARMACIST", "CASHIER", "LAB_TECH"
        );
        assertEquals(expected, ApprovedRoleCodes.ALLOWED);
        assertEquals(6, ApprovedRoleCodes.ALLOWED.size());
        assertThrows(UnsupportedOperationException.class, () -> ApprovedRoleCodes.ALLOWED.add("UNAPPROVED_ROLE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "  \t  \n  "})
    @DisplayName("normalize returns null for null, empty, or blank strings")
    void normalize_nullEmptyOrBlank_returnsNull(String input) {
        assertNull(ApprovedRoleCodes.normalize(input));
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
        assertEquals("ADMIN", ApprovedRoleCodes.normalize(input));
    }

    @Test
    @DisplayName("normalize properly trims and uppercases arbitrary role codes using Locale.ROOT")
    void normalize_variousRoleCodes_normalizesCorrectly() {
        assertEquals("DOCTOR", ApprovedRoleCodes.normalize("  doctor  "));
        assertEquals("RECEPTIONIST", ApprovedRoleCodes.normalize("\treceptionist\n"));
        assertEquals("PHARMACIST", ApprovedRoleCodes.normalize("pharmacist"));
        assertEquals("CASHIER", ApprovedRoleCodes.normalize("  Cashier  "));
        assertEquals("LAB_TECH", ApprovedRoleCodes.normalize("lab_tech"));
        assertEquals("CUSTOM_ROLE", ApprovedRoleCodes.normalize("  custom_role  "));
        assertEquals("TITLE", ApprovedRoleCodes.normalize("title"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ADMIN", "DOCTOR", "RECEPTIONIST", "PHARMACIST", "CASHIER", "LAB_TECH"
    })
    @DisplayName("isAllowed returns true for canonical uppercase approved roles")
    void isAllowed_canonicalUppercaseRoles_returnsTrue(String roleCode) {
        assertTrue(ApprovedRoleCodes.isAllowed(roleCode));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "admin", "doctor", "receptionist", "pharmacist", "cashier", "lab_tech"
    })
    @DisplayName("isAllowed returns true for lowercase approved roles")
    void isAllowed_lowercaseRoles_returnsTrue(String roleCode) {
        assertTrue(ApprovedRoleCodes.isAllowed(roleCode));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "  ADMIN  ", "  doctor  ", "\tRECEPTIONIST\n", "  Pharmacist  ", " cashier ", " Lab_Tech "
    })
    @DisplayName("isAllowed returns true for approved roles with whitespace and mixed casing")
    void isAllowed_whitespaceAndMixedCasing_returnsTrue(String roleCode) {
        assertTrue(ApprovedRoleCodes.isAllowed(roleCode));
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
        assertFalse(ApprovedRoleCodes.isAllowed(roleCode));
    }
}
