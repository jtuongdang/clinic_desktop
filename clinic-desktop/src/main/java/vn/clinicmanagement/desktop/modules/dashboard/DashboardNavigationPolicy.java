package vn.clinicmanagement.desktop.modules.dashboard;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Maps approved XEM module grants to the existing desktop panels; unknown modules stay hidden. */
final class DashboardNavigationPolicy {
    private static final Set<String> KNOWN_ROLE_CODES =
            Set.of("ADMIN", "DOCTOR", "RECEPTIONIST", "PHARMACIST", "CASHIER", "LAB_TECH");
    private static final Map<String, Set<String>> NAVIGATION_GRANTS = navigationGrants();

    private DashboardNavigationPolicy() {}

    static Set<String> visibleNavigationKeys(int roleId, String roleCode, Set<String> grantedModules) {
        String normalizedRoleCode = normalize(roleCode);
        if (roleId <= 0 || normalizedRoleCode == null || !KNOWN_ROLE_CODES.contains(normalizedRoleCode)
                || grantedModules == null) {
            return Set.of();
        }

        Set<String> normalizedGrants = grantedModules.stream()
                .map(DashboardNavigationPolicy::normalize)
                .filter(module -> module != null)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return NAVIGATION_GRANTS.entrySet().stream()
                .filter(entry -> normalizedGrants.containsAll(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Map<String, Set<String>> navigationGrants() {
        Map<String, Set<String>> grants = new LinkedHashMap<>();
        grants.put("home", Set.of("HOME"));
        grants.put("reception", Set.of("RECEPTION"));
        grants.put("treatment", Set.of("EMR"));
        grants.put("system", Set.of("SYSTEM"));
        grants.put("catalog", Set.of("CATALOG"));
        grants.put("billing", Set.of("BILLING"));
        grants.put("paraclinical", Set.of("CLS"));
        grants.put("pharmacy", Set.of("PRESCRIPTION", "INVENTORY"));
        return Map.copyOf(grants);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
