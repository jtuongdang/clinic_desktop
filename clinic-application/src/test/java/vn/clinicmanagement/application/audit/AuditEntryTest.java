package vn.clinicmanagement.application.audit;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditEntryTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-01-02T03:04:05Z");
    private static final UUID CORRELATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void acceptsOnlyWhitelistedTypedMetadataAndPreservesUtcInstant() {
        SystemAuditEntry entry = new SystemAuditEntry(
                null, null, null, null, null, AuditActionCode.AUTH_LOGIN_FAILED,
                null, null, AuditOutcome.FAILURE, AuditReasonCode.AUTH_INVALID,
                OCCURRED_AT, "station-01", CORRELATION_ID);

        assertEquals(AuditActionCode.AUTH_LOGIN_FAILED, entry.actionCode());
        assertEquals(AuditOutcome.FAILURE, entry.outcome());
        assertEquals(OCCURRED_AT, entry.occurredAtUtc());
        assertEquals(CORRELATION_ID, entry.correlationId());
    }

    @Test
    void rejectsUnpairedRoleAndResourceMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new SystemAuditEntry(
                null, 2, null, null, null, AuditActionCode.AUTH_LOGIN_SUCCESS,
                null, null, AuditOutcome.SUCCESS, null, OCCURRED_AT, null, CORRELATION_ID));
        assertThrows(IllegalArgumentException.class, () -> new SystemAuditEntry(
                null, null, null, null, null, AuditActionCode.PATIENT_VIEW,
                AuditResourceType.PATIENT, null, AuditOutcome.SUCCESS, null,
                OCCURRED_AT, null, CORRELATION_ID));
    }

    @Test
    void rejectsFreeTextLikeRoleAndWorkstationValues() {
        assertThrows(IllegalArgumentException.class, () -> new SystemAuditEntry(
                null, 2, "Doctor account", null, null, AuditActionCode.AUTH_LOGIN_SUCCESS,
                null, null, AuditOutcome.SUCCESS, null, OCCURRED_AT, null, CORRELATION_ID));
        assertThrows(IllegalArgumentException.class, () -> new SystemAuditEntry(
                null, null, null, null, null, AuditActionCode.AUTH_LOGIN_SUCCESS,
                null, null, AuditOutcome.SUCCESS, null, OCCURRED_AT, "station 01", CORRELATION_ID));
    }

    @Test
    void requiresAnAuthenticatedActorAndVisitForMedicalRecordEntries() {
        assertThrows(IllegalArgumentException.class, () -> new MedicalRecordAuditEntry(
                17, 0, null, null, null, null, AuditActionCode.EMR_VIEW,
                AuditOutcome.SUCCESS, null, OCCURRED_AT, null, CORRELATION_ID));
        assertThrows(IllegalArgumentException.class, () -> new MedicalRecordAuditEntry(
                0, 7, null, null, null, null, AuditActionCode.EMR_VIEW,
                AuditOutcome.SUCCESS, null, OCCURRED_AT, null, CORRELATION_ID));

        MedicalRecordAuditEntry entry = new MedicalRecordAuditEntry(
                17, 7, 2, "DOCTOR", 3, 5, AuditActionCode.EMR_VIEW,
                AuditOutcome.SUCCESS, null, OCCURRED_AT, "room-02", CORRELATION_ID);

        assertEquals(AuditResourceType.VISIT, entry.resourceType());
        assertEquals(17, entry.resourceId());
        assertEquals(7, entry.actorEmployeeId());
    }
}
