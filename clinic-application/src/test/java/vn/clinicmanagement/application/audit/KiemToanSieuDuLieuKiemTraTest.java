package vn.clinicmanagement.application.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KiemToanSieuDuLieuKiemTraTest {

    private static final Integer VALID_ACTOR_ID = 1;
    private static final Integer VALID_ROLE_ID = 10;
    private static final String VALID_ROLE_CODE = "DOCTOR";
    private static final Integer VALID_FACILITY_ID = 2;
    private static final Integer VALID_DEPARTMENT_ID = 3;
    private static final KiemToanHanhDongMa VALID_ACTION = KiemToanHanhDongMa.PATIENT_VIEW;
    private static final KiemToanTaiNguyenLoai VALID_RESOURCE_TYPE = KiemToanTaiNguyenLoai.PATIENT;
    private static final Integer VALID_RESOURCE_ID = 100;
    private static final KiemToanKetQua VALID_OUTCOME = KiemToanKetQua.SUCCESS;
    private static final Instant VALID_OCCURRED_AT = Instant.now();
    private static final String VALID_WORKSTATION_ID = "WS-01";
    private static final UUID VALID_CORRELATION_ID = UUID.randomUUID();

    @Test
    @DisplayName("Private constructor should be private and instantiable via reflection")
    void constructor_isPrivate() throws Exception {
        Constructor<KiemToanSieuDuLieuKiemTra> constructor = KiemToanSieuDuLieuKiemTra.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        KiemToanSieuDuLieuKiemTra instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("Valid complete metadata passes validation")
    void validate_completeValidMetadata_succeeds() {
        assertDoesNotThrow(() -> KiemToanSieuDuLieuKiemTra.validate(
                VALID_ACTOR_ID,
                VALID_ROLE_ID,
                VALID_ROLE_CODE,
                VALID_FACILITY_ID,
                VALID_DEPARTMENT_ID,
                VALID_ACTION,
                VALID_RESOURCE_TYPE,
                VALID_RESOURCE_ID,
                VALID_OUTCOME,
                VALID_OCCURRED_AT,
                VALID_WORKSTATION_ID,
                VALID_CORRELATION_ID));
    }

    @Test
    @DisplayName("Valid anonymous metadata with null actor, role, and resource passes validation")
    void validate_anonymousMetadataWithNullActorRoleAndResource_succeeds() {
        assertDoesNotThrow(() -> KiemToanSieuDuLieuKiemTra.validate(
                null, // actorEmployeeId
                null, // roleId
                null, // roleCode
                null, // facilityId
                null, // departmentId
                KiemToanHanhDongMa.AUTH_LOGIN_FAILED,
                null, // resourceType
                null, // resourceId
                KiemToanKetQua.FAILURE,
                VALID_OCCURRED_AT,
                null, // workstationId
                VALID_CORRELATION_ID));
    }

    @Test
    @DisplayName("Valid anonymous metadata with facility and workstation passes validation")
    void validate_anonymousMetadataWithFacilityAndWorkstation_succeeds() {
        assertDoesNotThrow(() -> KiemToanSieuDuLieuKiemTra.validate(
                null,
                null,
                null,
                VALID_FACILITY_ID,
                VALID_DEPARTMENT_ID,
                KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                null,
                null,
                KiemToanKetQua.DENIED,
                VALID_OCCURRED_AT,
                VALID_WORKSTATION_ID,
                VALID_CORRELATION_ID));
    }

    @Test
    @DisplayName("Non-positive actorEmployeeId is rejected with IllegalArgumentException")
    void validate_nonPositiveActorEmployeeId_throwsException() {
        IllegalArgumentException ex0 = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        0, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("actorEmployeeId must be positive when present.", ex0.getMessage());

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        -1, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("actorEmployeeId must be positive when present.", exNeg.getMessage());
    }

    @Test
    @DisplayName("Non-positive roleId is rejected with IllegalArgumentException")
    void validate_nonPositiveRoleId_throwsException() {
        IllegalArgumentException ex0 = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, 0, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("roleId must be positive when present.", ex0.getMessage());

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, -1, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("roleId must be positive when present.", exNeg.getMessage());
    }

    @Test
    @DisplayName("Non-positive facilityId is rejected with IllegalArgumentException")
    void validate_nonPositiveFacilityId_throwsException() {
        IllegalArgumentException ex0 = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, 0, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("facilityId must be positive when present.", ex0.getMessage());

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, -5, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("facilityId must be positive when present.", exNeg.getMessage());
    }

    @Test
    @DisplayName("Non-positive departmentId is rejected with IllegalArgumentException")
    void validate_nonPositiveDepartmentId_throwsException() {
        IllegalArgumentException ex0 = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, 0,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("departmentId must be positive when present.", ex0.getMessage());

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, -10,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("departmentId must be positive when present.", exNeg.getMessage());
    }

    @Test
    @DisplayName("Non-positive resourceId is rejected with IllegalArgumentException")
    void validate_nonPositiveResourceId_throwsException() {
        IllegalArgumentException ex0 = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, 0, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("resourceId must be positive when present.", ex0.getMessage());

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, -1, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("resourceId must be positive when present.", exNeg.getMessage());
    }

    @Test
    @DisplayName("Mismatched roleId and roleCode presence is rejected with IllegalArgumentException")
    void validate_mismatchedRoleIdAndRoleCode_throwsException() {
        // roleId present without roleCode
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, null, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("Role ID and role code must both be present or both be absent.", ex1.getMessage());

        // roleCode present without roleId
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, null, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("Role ID and role code must both be present or both be absent.", ex2.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "doctor",
            "_DOCTOR",
            "1DOCTOR",
            "DOCTOR-ROLE",
            "DOC TOR",
            "DOCTOR@HOSPITAL",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" // 51 chars
    })
    @DisplayName("Invalid roleCode pattern is rejected with IllegalArgumentException")
    void validate_invalidRoleCodePattern_throwsException(String invalidRoleCode) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, invalidRoleCode, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("roleCode must be a normalized role code.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "D",
            "DOCTOR",
            "ROLE_123",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" // exactly 50 chars
    })
    @DisplayName("Valid roleCode pattern passes validation")
    void validate_validRoleCodePattern_succeeds(String validRoleCode) {
        assertDoesNotThrow(() -> KiemToanSieuDuLieuKiemTra.validate(
                VALID_ACTOR_ID, VALID_ROLE_ID, validRoleCode, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
    }

    @Test
    @DisplayName("Mismatched resourceType and resourceId presence is rejected with IllegalArgumentException")
    void validate_mismatchedResourceTypeAndResourceId_throwsException() {
        // resourceType present without resourceId
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, null, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("Resource type and resource ID must both be present or absent.", ex1.getMessage());

        // resourceId present without resourceType
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, null, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("Resource type and resource ID must both be present or absent.", ex2.getMessage());
    }

    @Test
    @DisplayName("Missing actionCode is rejected with IllegalArgumentException")
    void validate_missingActionCode_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        null, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("actionCode is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Missing outcome is rejected with IllegalArgumentException")
    void validate_missingOutcome_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, null,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("outcome is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Missing occurredAtUtc is rejected with IllegalArgumentException")
    void validate_missingOccurredAtUtc_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        null, VALID_WORKSTATION_ID, VALID_CORRELATION_ID));
        assertEquals("occurredAtUtc is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Missing correlationId is rejected with IllegalArgumentException")
    void validate_missingCorrelationId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, VALID_WORKSTATION_ID, null));
        assertEquals("correlationId is required.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "-WS01",
            ".WS01",
            ":WS01",
            "_WS01",
            "WS 01",
            "WS#01",
            "WS/01",
            "WS@01"
    })
    @DisplayName("Invalid workstationId pattern is rejected with IllegalArgumentException")
    void validate_invalidWorkstationIdPattern_throwsException(String invalidWorkstationId) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, invalidWorkstationId, VALID_CORRELATION_ID));
        assertEquals("workstationId must be a bounded identifier.", ex.getMessage());
    }

    @Test
    @DisplayName("WorkstationId exceeding 100 characters is rejected with IllegalArgumentException")
    void validate_workstationIdExceedsMaxLength_throwsException() {
        String tooLongWorkstationId = "W" + "x".repeat(100); // 101 characters
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                KiemToanSieuDuLieuKiemTra.validate(
                        VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                        VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                        VALID_OCCURRED_AT, tooLongWorkstationId, VALID_CORRELATION_ID));
        assertEquals("workstationId must be a bounded identifier.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "A",
            "WS-01",
            "PC_ROOM_101",
            "192.168.1.1:8080",
            "workstation.clinic.internal"
    })
    @DisplayName("Valid workstationId pattern passes validation")
    void validate_validWorkstationIdPattern_succeeds(String validWorkstationId) {
        assertDoesNotThrow(() -> KiemToanSieuDuLieuKiemTra.validate(
                VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                VALID_OCCURRED_AT, validWorkstationId, VALID_CORRELATION_ID));
    }

    @Test
    @DisplayName("WorkstationId of exactly 100 characters passes validation")
    void validate_workstationIdExactlyMaxLength_succeeds() {
        String maxWorkstationId = "W" + "x".repeat(99); // 100 characters
        assertDoesNotThrow(() -> KiemToanSieuDuLieuKiemTra.validate(
                VALID_ACTOR_ID, VALID_ROLE_ID, VALID_ROLE_CODE, VALID_FACILITY_ID, VALID_DEPARTMENT_ID,
                VALID_ACTION, VALID_RESOURCE_TYPE, VALID_RESOURCE_ID, VALID_OUTCOME,
                VALID_OCCURRED_AT, maxWorkstationId, VALID_CORRELATION_ID));
    }
}
