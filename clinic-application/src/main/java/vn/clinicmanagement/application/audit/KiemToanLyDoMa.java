package vn.clinicmanagement.application.audit;

public enum KiemToanLyDoMa {
    AUTH_INVALID,
    AUTH_THROTTLED,
    AUTH_NO_ROLE,
    AUTH_NO_SCOPE,
    AUTH_FORBIDDEN,
    SCOPE_MISMATCH,
    ACCOUNT_REVOKED,
    EMR_LOCKED,
    REOPEN_CLINICAL_ADDENDUM
}
