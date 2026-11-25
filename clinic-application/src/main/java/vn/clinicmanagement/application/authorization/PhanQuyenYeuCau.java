package vn.clinicmanagement.application.authorization;

import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;

/**
 * Yêu cầu phân quyền bất biến; bộ đánh giá từ chối trường `null` hoặc không hợp lệ.
 */
public record PhanQuyenYeuCau(
        PhanQuyenNguCanh context,
        String moduleCode,
        HanhDongQuyen action,
        int targetFacilityId,
        int targetDepartmentId) {

    @Override
    public String toString() {
        return "AuthorizationRequest[redacted]";
    }
}
