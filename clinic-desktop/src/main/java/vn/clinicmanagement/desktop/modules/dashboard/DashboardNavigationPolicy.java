package vn.clinicmanagement.desktop.modules.dashboard;

import java.util.Set;

/** Only the non-sensitive welcome shell is wired in this candidate. Grants never enable demo routes. */
public final class DashboardNavigationPolicy {
    private final Set<String> grants;

    public DashboardNavigationPolicy(Set<String> grants) {
        this.grants = Set.copyOf(java.util.Objects.requireNonNull(grants, "grants"));
    }

    public boolean canOpen(String key) {
        if (key == null) return false;
        // Preserve slots for every original control; sensitive workflows await protected composition.
        return switch (key.toLowerCase(java.util.Locale.ROOT)) {
            case "home" -> grants.contains("HOME");
            case "user", "help" -> true;
            default -> false;
        };
    }
}
