package vn.clinicmanagement.application.audit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KiemToanHanhDongMaTransferTest {
    @Test
    void resolvesTransferAsAnActionDistinctFromRegistration() {
        KiemToanHanhDongMa action = KiemToanHanhDongMa.valueOf("VISIT_TRANSFERRED");

        assertEquals(KiemToanHanhDongMa.VISIT_TRANSFERRED, action);
        assertNotEquals(KiemToanHanhDongMa.VISIT_REGISTERED, action);
    }

    @Test
    void preservesTheTransferActionNameUsedByAuditSerialization() {
        assertEquals("VISIT_TRANSFERRED", KiemToanHanhDongMa.VISIT_TRANSFERRED.name());
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
                    () -> KiemToanLyDoMa.valueOf(businessReason), businessReason);
        }
    }
}
