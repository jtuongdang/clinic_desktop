package vn.clinicmanagement.application.authorization;

import java.util.Optional;
import java.util.UUID;

/** Reads current session validity and its bound actor/scope fields. */
public interface HoatDongPhienCongKetNoi {
    /** Hàm `findActiveSession`: tìm hoạt động phien. */
    Optional<ActiveSession> findActiveSession(UUID sessionId);

    record ActiveSession(
            /** Trường `sessionId`: phien mã. */
            UUID sessionId,
            /** Trường `employeeId`: nhân viên mã. */
            int employeeId,
            /** Trường `roleId`: vai trò mã. */
            int roleId,
            /** Trường `roleCode`: vai trò mã. */
            String roleCode,
            /** Trường `facilityId`: cơ sở mã. */
            int facilityId,
            /** Trường `departmentId`: khoa mã. */
            int departmentId,
            /** Trường `active`: hoạt động. */
            boolean active,
            boolean unlocked,
            /** Trường `notExpired`: không `Expired`. */
            boolean notExpired) {
        /** Hàm `toString`: den `String`. Gốc khai báo tại `Object#toString`. */
        @Override
        public String toString() {
            return "ActiveSession[redacted]";
        }
    }
}
