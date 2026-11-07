package vn.clinicmanagement.desktop.modules.dashboard;

import java.util.Objects;
import java.util.Set;

/**
 * Điều hướng phân hệ trên giao diện làm việc theo quyền và ma trận phân quyền.
 */
public final class DashboardNavigationPolicy {
    private final Set<String> grants;

    public DashboardNavigationPolicy(Set<String> grants) {
        this.grants = Set.copyOf(Objects.requireNonNull(grants, "grants"));
    }

    public boolean canOpen(String key) {
        if (key == null) return false;
        String normalizedKey = key.toLowerCase(java.util.Locale.ROOT);

        // Quyền quản trị hệ thống có thể mở toàn bộ các phân hệ để kiểm tra và vận hành
        boolean isSystemAdmin = grants.contains("SYSTEM") || grants.contains("ADMIN");

        return switch (normalizedKey) {
            case "home" -> grants.contains("HOME") || isSystemAdmin;
            case "reception" -> isSystemAdmin || grants.contains("RECEPTION") || grants.contains("PATIENT");
            case "treatment", "doctor" -> isSystemAdmin || grants.contains("EMR");
            case "examination" -> isSystemAdmin || grants.contains("PATIENT") || grants.contains("EMR");
            case "billing" -> isSystemAdmin || grants.contains("BILLING");
            case "paraclinical", "services" -> isSystemAdmin || grants.contains("CLS");
            case "pharmacy" -> isSystemAdmin || grants.contains("INVENTORY") || grants.contains("PRESCRIPTION");
            case "reports", "report-mgmt" -> isSystemAdmin || grants.contains("BILLING");
            case "system", "admin" -> isSystemAdmin;
            case "catalog" -> isSystemAdmin || grants.contains("CATALOG");
            case "user", "help", "exit" -> true;
            default -> false;
        };
    }
}
