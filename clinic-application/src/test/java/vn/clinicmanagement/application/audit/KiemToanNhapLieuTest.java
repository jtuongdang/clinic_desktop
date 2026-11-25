package vn.clinicmanagement.application.audit;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KiemToanNhapLieuTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-01-02T03:04:05Z");
    private static final UUID CORRELATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void acceptsOnlyWhitelistedTypedMetadataAndPreservesUtcInstant() {
        HeThongKiemToanNhapLieu entry = new HeThongKiemToanNhapLieu(
                null, null, null, null, null, KiemToanHanhDongMa.AUTH_LOGIN_FAILED,
                null, null, KiemToanKetQua.FAILURE, KiemToanLyDoMa.AUTH_INVALID,
                OCCURRED_AT, "station-01", CORRELATION_ID);

        assertEquals(KiemToanHanhDongMa.AUTH_LOGIN_FAILED, entry.actionCode());
        assertEquals(KiemToanKetQua.FAILURE, entry.outcome());
        assertEquals(OCCURRED_AT, entry.occurredAtUtc());
        assertEquals(CORRELATION_ID, entry.correlationId());
    }

    @Test
    void rejectsUnpairedRoleAndResourceMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new HeThongKiemToanNhapLieu(
                null, 2, null, null, null, KiemToanHanhDongMa.AUTH_LOGIN_SUCCESS,
                null, null, KiemToanKetQua.SUCCESS, null, OCCURRED_AT, null, CORRELATION_ID));
        assertThrows(IllegalArgumentException.class, () -> new HeThongKiemToanNhapLieu(
                null, null, null, null, null, KiemToanHanhDongMa.PATIENT_VIEW,
                KiemToanTaiNguyenLoai.PATIENT, null, KiemToanKetQua.SUCCESS, null,
                OCCURRED_AT, null, CORRELATION_ID));
    }

    @Test
    void rejectsFreeTextLikeRoleAndWorkstationValues() {
        assertThrows(IllegalArgumentException.class, () -> new HeThongKiemToanNhapLieu(
                null, 2, "Doctor account", null, null, KiemToanHanhDongMa.AUTH_LOGIN_SUCCESS,
                null, null, KiemToanKetQua.SUCCESS, null, OCCURRED_AT, null, CORRELATION_ID));
        assertThrows(IllegalArgumentException.class, () -> new HeThongKiemToanNhapLieu(
                null, null, null, null, null, KiemToanHanhDongMa.AUTH_LOGIN_SUCCESS,
                null, null, KiemToanKetQua.SUCCESS, null, OCCURRED_AT, "station 01", CORRELATION_ID));
    }

    @Test
    void requiresAnAuthenticatedActorAndVisitForMedicalRecordEntries() {
        assertThrows(IllegalArgumentException.class, () -> new YTeRecordKiemToanNhapLieu(
                17, 0, null, null, null, null, KiemToanHanhDongMa.EMR_VIEW,
                KiemToanKetQua.SUCCESS, null, OCCURRED_AT, null, CORRELATION_ID));
        assertThrows(IllegalArgumentException.class, () -> new YTeRecordKiemToanNhapLieu(
                0, 7, null, null, null, null, KiemToanHanhDongMa.EMR_VIEW,
                KiemToanKetQua.SUCCESS, null, OCCURRED_AT, null, CORRELATION_ID));

        YTeRecordKiemToanNhapLieu entry = new YTeRecordKiemToanNhapLieu(
                17, 7, 2, "DOCTOR", 3, 5, KiemToanHanhDongMa.EMR_VIEW,
                KiemToanKetQua.SUCCESS, null, OCCURRED_AT, "room-02", CORRELATION_ID);

        assertEquals(KiemToanTaiNguyenLoai.VISIT, entry.resourceType());
        assertEquals(17, entry.resourceId());
        assertEquals(7, entry.actorEmployeeId());
    }
}
