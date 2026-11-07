package vn.clinicmanagement.application.authorization;

import java.util.Optional;
import java.util.UUID;

/** Reads current session validity and its bound actor/scope fields. */
public interface ActiveSessionPort {
    Optional<ActiveSession> findActiveSession(UUID sessionId);

    record ActiveSession(
            UUID sessionId,
            int employeeId,
            int roleId,
            String roleCode,
            int facilityId,
            int departmentId,
            boolean active,
            boolean unlocked,
            boolean notExpired) {
        @Override
        public String toString() {
            return "ActiveSession[redacted]";
        }
    }
}
