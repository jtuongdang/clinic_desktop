package vn.clinicmanagement.application.authorization;

import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;

/** Immutable permission request; null and invalid fields are rejected by the evaluator. */
public record PhanQuyenYeuCau(
        /** Trường `context`: ngữ cảnh. */
        PhanQuyenNguCanh context,
        /** Trường `moduleCode`: `module` mã. */
        String moduleCode,
        /** Trường `action`: hành động. */
        HanhDongQuyen action,
        /** Trường `targetFacilityId`: `target` cơ sở mã. */
        int targetFacilityId,
        /** Trường `targetDepartmentId`: `target` khoa mã. */
        int targetDepartmentId) {

    /** Hàm `toString`: den `String`. Gốc khai báo tại `Object#toString`. */
    @Override
    public String toString() {
        return "AuthorizationRequest[redacted]";
    }
}
