package vn.clinicmanagement.application.authorization;

/** Safe evaluator result. Reason codes contain no actor or resource data. */
public record AuthorizationDecision(boolean allowed, ReasonCode reasonCode) {
    public enum ReasonCode {
        ALLOWED,
        INVALID_REQUEST,
        INVALID_CONTEXT,
        AUDIT_READ_DENIED,
        SESSION_INVALID,
        EMPLOYEE_INVALID,
        ROLE_INVALID,
        SCOPE_INVALID,
        TARGET_MISMATCH,
        MODULE_INVALID,
        PERMISSION_MISSING,
        PERMISSION_DENIED,
        PERMISSION_AMBIGUOUS,
        DEPENDENCY_FAILURE
    }

    public static AuthorizationDecision allow() {
        return new AuthorizationDecision(true, ReasonCode.ALLOWED);
    }

    public static AuthorizationDecision deny(ReasonCode reasonCode) {
        return new AuthorizationDecision(false, reasonCode);
    }
}
