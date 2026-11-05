package vn.clinicmanagement.desktop.modules.dashboard;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardNavigationPolicyTest {
    @Test
    void mapsApprovedRoleXemGrantsToOnlyExistingPanels() {
        assertNavigation("ADMIN", Set.of("HOME", "CATALOG", "SYSTEM"),
                Set.of("home", "catalog", "system"));
        assertNavigation("DOCTOR", Set.of("HOME", "PATIENT", "RECEPTION", "APPOINTMENT", "EMR",
                        "CLS", "PRESCRIPTION", "INVENTORY", "BILLING", "CATALOG"),
                Set.of("home", "examination", "reception", "treatment", "paraclinical", "pharmacy", "billing", "catalog"));
        assertNavigation("RECEPTIONIST", Set.of("HOME", "PATIENT", "RECEPTION", "APPOINTMENT", "CATALOG"),
                Set.of("home", "examination", "reception", "catalog"));
        assertNavigation("PHARMACIST", Set.of("HOME", "PATIENT", "PRESCRIPTION", "INVENTORY", "CATALOG"),
                Set.of("home", "examination", "pharmacy", "catalog"));
        assertNavigation("CASHIER", Set.of("HOME", "PATIENT", "APPOINTMENT", "CLS", "BILLING", "CATALOG"),
                Set.of("home", "examination", "paraclinical", "billing", "catalog"));
        assertNavigation("LAB_TECH", Set.of("HOME", "PATIENT", "CLS", "CATALOG"),
                Set.of("home", "examination", "paraclinical", "catalog"));
    }

    @Test
    void missingUnknownAndUnsupportedModulesFailClosed() {
        assertTrue(DashboardNavigationPolicy.visibleNavigationKeys(0, "ADMIN", Set.of("HOME")).isEmpty());
        assertTrue(DashboardNavigationPolicy.visibleNavigationKeys(1, "ADMIN", Set.of("HOME")).contains("home"));
        assertTrue(DashboardNavigationPolicy.visibleNavigationKeys(1, "Quản trị viên", Set.of("HOME")).isEmpty());
        assertTrue(DashboardNavigationPolicy.visibleNavigationKeys(1, "ADMIN", null).isEmpty());
        assertTrue(DashboardNavigationPolicy.visibleNavigationKeys(1, "ADMIN",
                Set.of("UNKNOWN", "APPOINTMENT", "REPORTS", "AUDIT")).isEmpty());
        assertTrue(DashboardNavigationPolicy.visibleNavigationKeys(1, "ADMIN", Set.of("INVENTORY")).isEmpty(),
                "A combined pharmacy panel requires both of its approved module grants.");
    }

    @Test
    void dashboardHasNoIdentityFabricatingDefaultConstructor() {
        for (Constructor<?> constructor : MainDashboardFrame.class.getConstructors()) {
            assertTrue(constructor.getParameterCount() > 0);
        }
    }

    private static void assertNavigation(String roleCode, Set<String> grants, Set<String> expectedKeys) {
        assertEquals(expectedKeys,
                DashboardNavigationPolicy.visibleNavigationKeys(9, roleCode, grants), roleCode);
    }
}
