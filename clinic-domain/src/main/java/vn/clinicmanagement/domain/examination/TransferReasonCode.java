package vn.clinicmanagement.domain.examination;

import java.util.Set;

/**
 * Approved business taxonomy for outpatient visit transfers (VIS-TRANSFER-OWNER-DECISION-RECON-05).
 * <p>
 * This enum provides classification data and role eligibility matrix only.
 * Application guards and authorization boundaries remain mandatory.
 */
public enum TransferReasonCode {

    /**
     * Front desk misrouted patient during reception; corrected before consultation begins.
     * Role eligibility: RECEPTIONIST only.
     */
    RECEPTION_TRIAGE_CORRECTION(Set.of("RECEPTIONIST")),

    /**
     * Patient requests another room while waiting in CHO_KHAM.
     * Role eligibility: RECEPTIONIST and DOCTOR.
     */
    PATIENT_SPECIALTY_PREFERENCE(Set.of("RECEPTIONIST", "DOCTOR")),

    /**
     * Shift patient between parallel rooms in the same department to balance wait times.
     * Role eligibility: RECEPTIONIST and DOCTOR.
     */
    CLINIC_LOAD_BALANCING(Set.of("RECEPTIONIST", "DOCTOR")),

    /**
     * Examining doctor requests another suitable specialty room within the session-selected department.
     * Role eligibility: DOCTOR only.
     */
    CLINICAL_SPECIALTY_REFERRAL(Set.of("DOCTOR")),

    /**
     * Room lacks specific diagnostic instrument available in target room.
     * Role eligibility: DOCTOR only.
     */
    DIAGNOSTIC_EQUIPMENT_NEED(Set.of("DOCTOR")),

    /**
     * Doctor is called to an emergency, hospital round, or sudden absence; handoff of active queue.
     * Role eligibility: DOCTOR only.
     */
    CLINICIAN_EMERGENCY_HANDOFF(Set.of("DOCTOR"));

    public static final String ROLE_RECEPTIONIST = "RECEPTIONIST";
    public static final String ROLE_DOCTOR = "DOCTOR";

    private final Set<String> allowedRoleCodes;

    TransferReasonCode(Set<String> allowedRoleCodes) {
        this.allowedRoleCodes = allowedRoleCodes;
    }

    /**
     * Returns an unmodifiable set of allowed role codes for this transfer reason.
     *
     * @return immutable set of allowed role codes
     */
    public Set<String> getAllowedRoleCodes() {
        return allowedRoleCodes;
    }

    /**
     * Fluent alias for {@link #getAllowedRoleCodes()}.
     *
     * @return immutable set of allowed role codes
     */
    public Set<String> allowedRoleCodes() {
        return allowedRoleCodes;
    }

    /**
     * Determines whether the given role code is eligible to use this transfer reason.
     * Exact string match is required; null, empty, whitespace-padded, or unknown role codes deny.
     *
     * @param roleCode the role code to test (e.g. "RECEPTIONIST", "DOCTOR")
     * @return true if eligible; false if null, unknown, or not allowed
     */
    public boolean isEligibleRole(String roleCode) {
        if (roleCode == null) {
            return false;
        }
        return allowedRoleCodes.contains(roleCode);
    }

    /**
     * Checks if this transfer reason is allowed for the specified role code.
     *
     * @param roleCode the role code to test
     * @return true if allowed; false otherwise
     */
    public boolean isAllowedForRole(String roleCode) {
        return isEligibleRole(roleCode);
    }

    /**
     * Checks if the specified role code is an allowed role for this transfer reason.
     *
     * @param roleCode the role code to test
     * @return true if allowed; false otherwise
     */
    public boolean isAllowedRole(String roleCode) {
        return isEligibleRole(roleCode);
    }

    /**
     * Checks if this transfer reason allows the specified role code.
     *
     * @param roleCode the role code to test
     * @return true if allowed; false otherwise
     */
    public boolean allowsRole(String roleCode) {
        return isEligibleRole(roleCode);
    }
}
