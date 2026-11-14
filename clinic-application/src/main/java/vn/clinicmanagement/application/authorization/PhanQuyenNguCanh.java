package vn.clinicmanagement.application.authorization;

import java.util.UUID;

/** Minimal immutable actor and selected work scope for one authorization check. */
public record PhanQuyenNguCanh(
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
        /** Trường `sessionId`: phien mã. */
        UUID sessionId) {

    /** Hàm `isValid`: là `Valid`. */
    public boolean isValid() {
        return employeeId > 0 && roleId > 0 && roleCode != null && !roleCode.isBlank()
                && facilityId > 0 && departmentId > 0 && sessionId != null;
    }

    /** Hàm `toString`: den `String`. Gốc khai báo tại `Object#toString`. */
    @Override
    public String toString() {
        return "AuthorizationContext[redacted]";
    }
}
