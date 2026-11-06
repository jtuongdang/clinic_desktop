package vn.clinicmanagement.application.audit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditActionCodeTransferTest {
    @Test
    void resolvesTransferAsAnActionDistinctFromRegistration() {
        AuditActionCode action = AuditActionCode.valueOf("VISIT_TRANSFERRED");

        assertEquals(AuditActionCode.VISIT_TRANSFERRED, action);
        assertNotEquals(AuditActionCode.VISIT_REGISTERED, action);
    }

    @Test
    void preservesTheTransferActionNameUsedByAuditSerialization() {
        assertEquals("VISIT_TRANSFERRED", AuditActionCode.VISIT_TRANSFERRED.name());
    }

    @Test
    void keepsTransferBusinessReasonsOutOfAuditReasonCodes() {
        for (String businessReason : List.of(
                "RECEPTION_TRIAGE_CORRECTION",
                "PATIENT_SPECIALTY_PREFERENCE",
                "CLINIC_LOAD_BALANCING",
                "CLINICAL_SPECIALTY_REFERRAL",
                "DIAGNOSTIC_EQUIPMENT_NEED",
                "CLINICIAN_EMERGENCY_HANDOFF")) {
            assertThrows(IllegalArgumentException.class,
                    () -> AuditReasonCode.valueOf(businessReason), businessReason);
        }
    }
}
