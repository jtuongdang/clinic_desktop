package vn.clinicmanagement.application.authorization;

import java.util.Locale;
import java.util.Set;

/** Immutable allowlist of role codes recognized by the authorization boundary. */
public final class ApprovedRoleCodes {
    public static final Set<String> ALLOWED = Set.of(
            "ADMIN", "DOCTOR", "RECEPTIONIST", "PHARMACIST", "CASHIER", "LAB_TECH");

    private ApprovedRoleCodes() {}

    public static boolean isAllowed(String roleCode) {
        String normalized = normalize(roleCode);
        return normalized != null && ALLOWED.contains(normalized);
    }

    public static String normalize(String roleCode) {
        return roleCode == null || roleCode.isBlank() ? null : roleCode.trim().toUpperCase(Locale.ROOT);
    }
}
