package vn.clinicmanagement.application.authorization;

import java.util.UUID;

/** Thông tin bất biến tối thiểu về tác nhân và phạm vi làm việc được chọn cho một lần kiểm tra quyền. */
public record PhanQuyenNguCanh(
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
