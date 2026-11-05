package vn.clinicmanagement.application.authorization;

import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;

/** Immutable permission request; null and invalid fields are rejected by the evaluator. */
public record AuthorizationRequest(
        AuthorizationContext context,
        String moduleCode,
        HanhDongQuyen action,
        int targetFacilityId,
        int targetDepartmentId) {

    @Override
    public String toString() {
        return "AuthorizationRequest[redacted]";
    }
}
