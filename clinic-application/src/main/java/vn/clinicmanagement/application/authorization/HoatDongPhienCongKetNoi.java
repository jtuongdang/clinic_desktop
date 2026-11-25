package vn.clinicmanagement.application.authorization;

import java.util.Optional;
import java.util.UUID;

/** Đọc hiệu lực phiên hiện tại cùng các trường tác nhân/phạm vi được gắn với phiên. */
public interface HoatDongPhienCongKetNoi {
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
