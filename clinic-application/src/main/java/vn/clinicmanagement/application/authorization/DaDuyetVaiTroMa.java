package vn.clinicmanagement.application.authorization;

import java.util.Locale;
import java.util.Set;

/** Danh sách cho phép bất biến gồm các mã vai trò được cổng phân quyền nhận diện. */
public final class DaDuyetVaiTroMa {
    public static final Set<String> ALLOWED = Set.of(
            "ADMIN", "DOCTOR", "RECEPTIONIST", "PHARMACIST", "CASHIER", "LAB_TECH");

    private DaDuyetVaiTroMa() {}

    public static boolean isAllowed(String roleCode) {
        String normalized = normalize(roleCode);
        return normalized != null && ALLOWED.contains(normalized);
    }

    public static String normalize(String roleCode) {
        return roleCode == null || roleCode.isBlank() ? null : roleCode.trim().toUpperCase(Locale.ROOT);
    }
}
