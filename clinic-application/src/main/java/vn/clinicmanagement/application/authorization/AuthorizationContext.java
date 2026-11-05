package vn.clinicmanagement.application.authorization;

import java.util.UUID;

/** Minimal immutable actor and selected work scope for one authorization check. */
public record AuthorizationContext(
        int employeeId,
        int roleId,
        String roleCode,
        int facilityId,
        int departmentId,
        UUID sessionId) {

    public boolean isValid() {
        return employeeId > 0 && roleId > 0 && roleCode != null && !roleCode.isBlank()
                && facilityId > 0 && departmentId > 0 && sessionId != null;
    }

    @Override
    public String toString() {
        return "AuthorizationContext[redacted]";
    }
}
