package vn.clinicmanagement.application.examination;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GetVisitDetailHandlerTest {
    private static final int VISIT_ID = 73;
    private static final int PATIENT_ID = 29;
    private static final int EMPLOYEE_ID = 17;
    private static final int ROLE_ID = 4;
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");
    private static final LocalDate FOLLOW_UP = LocalDate.of(2026, 12, 4);
    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 11, 7, 9, 15);
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 11, 7, 9, 45);

    @Test
    void retrievesAndMapsVisitDetailsAndWritesEmrViewAudit() {
        LuotKham entity = visit();
        Fixture fixture = new Fixture(entity);

        Result<VisitDetailDto> result = fixture.handler.handle(query("DOCTOR"));

        assertTrue(result.isSuccess());
        assertEquals(new VisitDetailDto(
                VISIT_ID, PATIENT_ID, 51, FACILITY_ID, DEPARTMENT_ID, 12, "A-07", 7,
                TrangThaiLuotKham.HOAN_THANH, "Synthetic reason", "Synthetic clinical process",
                "Synthetic personal history", "Synthetic family history", "Synthetic general exam",
                "Synthetic organ exam", "Synthetic summary", "Synthetic treatment plan",
                "Synthetic doctor advice", KetQuaDieuTri.DO_GIAM, HuongXuTri.HEN_TAI_KHAM,
                FOLLOW_UP, true, entity.getEmrLockedAt(), 51, "synthetic-signature", STARTED_AT, COMPLETED_AT),
                result.valueOrThrow());
        assertEquals(1, fixture.repositoryReads);
        assertEquals(1, fixture.audit.independentWrites);
        SystemAuditEntry entry = fixture.audit.entry;
        assertEquals(AuditActionCode.EMR_VIEW, entry.actionCode());
        assertEquals(AuditResourceType.VISIT, entry.resourceType());
        assertEquals(VISIT_ID, entry.resourceId());
        assertEquals(AuditOutcome.SUCCESS, entry.outcome());
        assertEquals(CORRELATION_ID, entry.correlationId());
    }

    @Test
    void allowsDoctorReceptionistAndAdminRoles() {
        for (String roleCode : new String[]{"DOCTOR", "RECEPTIONIST", "ADMIN"}) {
            Fixture fixture = new Fixture(visit());

            Result<VisitDetailDto> result = fixture.handler.handle(query(roleCode));

            assertTrue(result.isSuccess(), roleCode);
            assertEquals(AuditActionCode.EMR_VIEW, fixture.audit.entry.actionCode());
            assertEquals(AuditOutcome.SUCCESS, fixture.audit.entry.outcome());
        }
    }

    @Test
    void rejectsNonPositiveVisitIdsBeforeRepositoryOrAuditAccess() {
        for (int visitId : new int[]{0, -1}) {
            Fixture fixture = new Fixture(visit());

            Result<VisitDetailDto> result = fixture.handler.handle(
                    new GetVisitDetailQuery(visitId, context("DOCTOR"), actor("DOCTOR")));

            assertFailure(result, "VALIDATION_ERROR", "Mã lượt khám không hợp lệ.");
            assertEquals(0, fixture.repositoryReads);
            assertEquals(0, fixture.audit.independentWrites);
        }
    }

    @Test
    void returnsNotFoundWhenVisitDoesNotExist() {
        Fixture fixture = new Fixture(null);

        Result<VisitDetailDto> result = fixture.handler.handle(query("DOCTOR"));

        assertFailure(result, "NOT_FOUND", "Không tìm thấy lượt khám.");
        assertEquals(1, fixture.repositoryReads);
        assertEquals(0, fixture.audit.independentWrites);
    }

    @Test
    void deniesUnauthorizedAndNullContextsWithForbiddenAudit() {
        Fixture unauthorized = new Fixture(visit());
        Result<VisitDetailDto> unauthorizedResult = unauthorized.handler.handle(query("OTHER"));

        assertTrue(unauthorizedResult.isFailure());
        assertEquals("AUTH_ACCESS_DENIED", unauthorizedResult.getError().orElseThrow().code());
        assertEquals(0, unauthorized.repositoryReads);
        assertDeniedAudit(unauthorized.audit.entry);

        Fixture missingContext = new Fixture(visit());
        Result<VisitDetailDto> missingContextResult = missingContext.handler.handle(
                new GetVisitDetailQuery(VISIT_ID, null, actor("DOCTOR")));

        assertTrue(missingContextResult.isFailure());
        assertEquals("AUTH_ACCESS_DENIED", missingContextResult.getError().orElseThrow().code());
        assertEquals(0, missingContext.repositoryReads);
        assertDeniedAudit(missingContext.audit.entry);
    }

    @Test
    void deniesInvalidSessionContextWithForbiddenAudit() {
        Fixture fixture = new Fixture(visit());
        AuthorizationContext invalidContext = new AuthorizationContext(
                EMPLOYEE_ID, ROLE_ID, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, null);

        Result<VisitDetailDto> result = fixture.handler.handle(
                new GetVisitDetailQuery(VISIT_ID, invalidContext, actor("DOCTOR")));

        assertTrue(result.isFailure());
        assertEquals("AUTH_ACCESS_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, fixture.repositoryReads);
        assertDeniedAudit(fixture.audit.entry);
    }

    private static void assertDeniedAudit(SystemAuditEntry entry) {
        assertNotNull(entry);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, entry.actionCode());
        assertNull(entry.resourceType());
        assertNull(entry.resourceId());
        assertEquals(AuditOutcome.DENIED, entry.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, entry.reasonCode());
        assertEquals(CORRELATION_ID, entry.correlationId());
    }

    private static void assertFailure(Result<VisitDetailDto> result, String code, String message) {
        assertTrue(result.isFailure());
        assertEquals(code, result.getError().orElseThrow().code());
        assertEquals(message, result.getError().orElseThrow().message());
    }

    private static GetVisitDetailQuery query(String roleCode) {
        return new GetVisitDetailQuery(VISIT_ID, context(roleCode), actor(roleCode));
    }

    private static AuthorizationContext context(String roleCode) {
        return new AuthorizationContext(
                EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    }

    private static AuditActorContext actor(String roleCode) {
        return new AuditActorContext(
                EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, "station-1", CORRELATION_ID);
    }

    private static LuotKham visit() {
        LuotKham visit = new LuotKham();
        visit.setId(VISIT_ID);
        visit.setMaBenhNhan(PATIENT_ID);
        visit.setMaBacSi(51);
        visit.setMaCoSo(FACILITY_ID);
        visit.setMaKhoa(DEPARTMENT_ID);
        visit.setMaPhongKham(12);
        visit.setSoThuTuDisplay("A-07");
        visit.setSoThuTu(7);
        visit.setTrangThai(TrangThaiLuotKham.HOAN_THANH);
        visit.setLyDoKham("Synthetic reason");
        visit.setQuaTrinhBenhLy("Synthetic clinical process");
        visit.setTienSuBanThan("Synthetic personal history");
        visit.setTienSuGiaDinh("Synthetic family history");
        visit.setKhamToanThan("Synthetic general exam");
        visit.setKhamBoPhan("Synthetic organ exam");
        visit.setTomTatBenhAn("Synthetic summary");
        visit.setHuongDieuTri("Synthetic treatment plan");
        visit.setLoiDanBacSi("Synthetic doctor advice");
        visit.setKetQuaDieuTri(KetQuaDieuTri.DO_GIAM);
        visit.setHuongXuTri(HuongXuTri.HEN_TAI_KHAM);
        visit.setNgayHenTaiKham(FOLLOW_UP);
        visit.setThoiGianBatDauKham(STARTED_AT);
        visit.setThoiGianKetThucKham(COMPLETED_AT);
        visit.lockEmr("synthetic-signature", 51);
        return visit;
    }

    private static final class Fixture {
        final GetVisitDetailHandler handler;
        final AuditFixture audit;
        int repositoryReads;

        Fixture(LuotKham visit) {
            ILuotKhamRepository repository = (ILuotKhamRepository) Proxy.newProxyInstance(
                    ILuotKhamRepository.class.getClassLoader(),
                    new Class<?>[]{ILuotKhamRepository.class},
                    (proxy, method, args) -> {
                        if ("getById".equals(method.getName())) {
                            repositoryReads++;
                            return Optional.ofNullable(visit);
                        }
                        return null;
                    });
            audit = new AuditFixture();
            handler = new GetVisitDetailHandler(repository, audit);
        }
    }

    private static final class AuditFixture implements AuditWritePort {
        int independentWrites;
        SystemAuditEntry entry;

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry auditEntry) {
            entry = auditEntry;
        }

        @Override
        public void appendIndependently(SystemAuditEntry auditEntry) {
            independentWrites++;
            entry = auditEntry;
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry auditEntry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void appendMedicalRecordIndependently(MedicalRecordAuditEntry auditEntry) {
            throw new UnsupportedOperationException();
        }
    }
}
