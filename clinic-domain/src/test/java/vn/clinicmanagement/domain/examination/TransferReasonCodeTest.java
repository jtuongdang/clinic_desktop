package vn.clinicmanagement.domain.examination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransferReasonCode domain taxonomy and role eligibility tests")
class TransferReasonCodeTest {

    private static final String ROLE_RECEPTIONIST = "RECEPTIONIST";
    private static final String ROLE_DOCTOR = "DOCTOR";

    @Test
    @DisplayName("Taxonomy defines exactly six approved transfer reason codes")
    void taxonomyContainsExactlySixApprovedCodes() {
        TransferReasonCode[] values = TransferReasonCode.values();
        assertEquals(6, values.length, "Must define exactly 6 approved transfer reason codes");

        List<String> names = Arrays.stream(values).map(Enum::name).toList();
        assertTrue(names.contains("RECEPTION_TRIAGE_CORRECTION"));
        assertTrue(names.contains("PATIENT_SPECIALTY_PREFERENCE"));
        assertTrue(names.contains("CLINIC_LOAD_BALANCING"));
        assertTrue(names.contains("CLINICAL_SPECIALTY_REFERRAL"));
        assertTrue(names.contains("DIAGNOSTIC_EQUIPMENT_NEED"));
        assertTrue(names.contains("CLINICIAN_EMERGENCY_HANDOFF"));
    }

    @Test
    @DisplayName("RECEPTION_TRIAGE_CORRECTION is eligible for RECEPTIONIST only")
    void receptionTriageCorrectionAllowedOnlyForReceptionist() {
        TransferReasonCode code = TransferReasonCode.RECEPTION_TRIAGE_CORRECTION;

        assertEquals(Set.of(ROLE_RECEPTIONIST), code.getAllowedRoleCodes());
        assertTrue(code.isEligibleRole(ROLE_RECEPTIONIST));
        assertTrue(code.isAllowedForRole(ROLE_RECEPTIONIST));
        assertTrue(code.isAllowedRole(ROLE_RECEPTIONIST));
        assertTrue(code.allowsRole(ROLE_RECEPTIONIST));

        assertFalse(code.isEligibleRole(ROLE_DOCTOR));
        assertFalse(code.isAllowedForRole(ROLE_DOCTOR));
        assertFalse(code.isAllowedRole(ROLE_DOCTOR));
        assertFalse(code.allowsRole(ROLE_DOCTOR));
    }

    @Test
    @DisplayName("PATIENT_SPECIALTY_PREFERENCE is eligible for RECEPTIONIST and DOCTOR")
    void patientSpecialtyPreferenceAllowedForReceptionistAndDoctor() {
        TransferReasonCode code = TransferReasonCode.PATIENT_SPECIALTY_PREFERENCE;

        assertEquals(Set.of(ROLE_RECEPTIONIST, ROLE_DOCTOR), code.getAllowedRoleCodes());
        assertTrue(code.isEligibleRole(ROLE_RECEPTIONIST));
        assertTrue(code.isEligibleRole(ROLE_DOCTOR));
        assertTrue(code.isAllowedForRole(ROLE_RECEPTIONIST));
        assertTrue(code.isAllowedForRole(ROLE_DOCTOR));
    }

    @Test
    @DisplayName("CLINIC_LOAD_BALANCING is eligible for RECEPTIONIST and DOCTOR")
    void clinicLoadBalancingAllowedForReceptionistAndDoctor() {
        TransferReasonCode code = TransferReasonCode.CLINIC_LOAD_BALANCING;

        assertEquals(Set.of(ROLE_RECEPTIONIST, ROLE_DOCTOR), code.getAllowedRoleCodes());
        assertTrue(code.isEligibleRole(ROLE_RECEPTIONIST));
        assertTrue(code.isEligibleRole(ROLE_DOCTOR));
        assertTrue(code.isAllowedForRole(ROLE_RECEPTIONIST));
        assertTrue(code.isAllowedForRole(ROLE_DOCTOR));
    }

    @Test
    @DisplayName("CLINICAL_SPECIALTY_REFERRAL is eligible for DOCTOR only")
    void clinicalSpecialtyReferralAllowedOnlyForDoctor() {
        TransferReasonCode code = TransferReasonCode.CLINICAL_SPECIALTY_REFERRAL;

        assertEquals(Set.of(ROLE_DOCTOR), code.getAllowedRoleCodes());
        assertTrue(code.isEligibleRole(ROLE_DOCTOR));
        assertTrue(code.isAllowedForRole(ROLE_DOCTOR));

        assertFalse(code.isEligibleRole(ROLE_RECEPTIONIST));
        assertFalse(code.isAllowedForRole(ROLE_RECEPTIONIST));
    }

    @Test
    @DisplayName("DIAGNOSTIC_EQUIPMENT_NEED is eligible for DOCTOR only")
    void diagnosticEquipmentNeedAllowedOnlyForDoctor() {
        TransferReasonCode code = TransferReasonCode.DIAGNOSTIC_EQUIPMENT_NEED;

        assertEquals(Set.of(ROLE_DOCTOR), code.getAllowedRoleCodes());
        assertTrue(code.isEligibleRole(ROLE_DOCTOR));
        assertTrue(code.isAllowedForRole(ROLE_DOCTOR));

        assertFalse(code.isEligibleRole(ROLE_RECEPTIONIST));
        assertFalse(code.isAllowedForRole(ROLE_RECEPTIONIST));
    }

    @Test
    @DisplayName("CLINICIAN_EMERGENCY_HANDOFF is eligible for DOCTOR only")
    void clinicianEmergencyHandoffAllowedOnlyForDoctor() {
        TransferReasonCode code = TransferReasonCode.CLINICIAN_EMERGENCY_HANDOFF;

        assertEquals(Set.of(ROLE_DOCTOR), code.getAllowedRoleCodes());
        assertTrue(code.isEligibleRole(ROLE_DOCTOR));
        assertTrue(code.isAllowedForRole(ROLE_DOCTOR));

        assertFalse(code.isEligibleRole(ROLE_RECEPTIONIST));
        assertFalse(code.isAllowedForRole(ROLE_RECEPTIONIST));
    }

    @ParameterizedTest
    @EnumSource(TransferReasonCode.class)
    @DisplayName("Null role code is safely denied across all query methods")
    void nullRoleCodeIsDeniedNullSafely(TransferReasonCode code) {
        assertFalse(code.isEligibleRole(null));
        assertFalse(code.isAllowedForRole(null));
        assertFalse(code.isAllowedRole(null));
        assertFalse(code.allowsRole(null));
    }

    @ParameterizedTest
    @EnumSource(TransferReasonCode.class)
    @DisplayName("Allowed role codes set is unmodifiable and prevents mutation")
    void allowedRoleCodesSetIsImmutable(TransferReasonCode code) {
        Set<String> set1 = code.getAllowedRoleCodes();
        Set<String> set2 = code.allowedRoleCodes();

        assertEquals(set1, set2);
        assertThrows(UnsupportedOperationException.class, () -> set1.add("ADMIN"));
        assertThrows(UnsupportedOperationException.class, () -> set1.remove(ROLE_DOCTOR));
        assertThrows(UnsupportedOperationException.class, () -> set1.remove(ROLE_RECEPTIONIST));
        assertThrows(UnsupportedOperationException.class, set1::clear);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ADMIN", "PHARMACIST", "CASHIER", "LAB_TECH", "NURSE", "HEAD_DOCTOR",
            "ACCOUNTANT", "UNKNOWN", "SUPERUSER", "", "   "
    })
    @DisplayName("Non-eligible and unknown roles are denied for all transfer reason codes")
    void nonEligibleAndUnknownRolesAreDenied(String unknownRole) {
        for (TransferReasonCode code : TransferReasonCode.values()) {
            assertFalse(code.isEligibleRole(unknownRole),
                    () -> code + " must deny unknown role: " + unknownRole);
            assertFalse(code.isAllowedForRole(unknownRole));
            assertFalse(code.isAllowedRole(unknownRole));
            assertFalse(code.allowsRole(unknownRole));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "receptionist", "doctor", "RECEPTIONIST ", " DOCTOR ", "Doctor", "Receptionist"
    })
    @DisplayName("Exact role code matching is enforced (no trimming or case folding)")
    void exactRoleCodeMatchingIsEnforced(String inexactRole) {
        for (TransferReasonCode code : TransferReasonCode.values()) {
            assertFalse(code.isEligibleRole(inexactRole),
                    () -> code + " must deny inexact role: '" + inexactRole + "'");
            assertFalse(code.isAllowedForRole(inexactRole));
        }
    }
}
