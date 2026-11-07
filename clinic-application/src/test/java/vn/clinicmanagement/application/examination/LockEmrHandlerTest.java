package vn.clinicmanagement.application.examination;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class LockEmrHandlerTest {
    private static final int VISIT_ID = 73;
    private static final int DOCTOR_ID = 17;
    private static final int EMPLOYEE_ID = 17;
    private static final int ROLE_ID = 4;
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");
    private static final String SIGNATURE = "synthetic-signature";

    @Test
    void locksCompletedVisitAsDoctorAndAuditsWithinTransaction() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.HOAN_THANH);

        Result<LockEmrReceipt> result = fixture.handler().handle(fixture.command("DOCTOR", SIGNATURE));

        assertTrue(result.isSuccess());
        LockEmrReceipt receipt = result.valueOrThrow();
        assertEquals(VISIT_ID, receipt.visitId());
        assertTrue(receipt.emrLocked());
        assertNotNull(receipt.lockedAt());
        assertEquals(DOCTOR_ID, receipt.lockedBy());
        assertEquals(SIGNATURE, receipt.signature());
        assertSame(fixture.visit, fixture.updated);
        assertEquals(1, fixture.transactions.calls);
        assertEquals(1, fixture.repositoryReads);
        assertTrue(fixture.audit.inTransaction);
        assertSuccessAudit(fixture.audit.entry);
    }

    @Test
    void locksCompletedVisitAsAdmin() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.HOAN_THANH);

        Result<LockEmrReceipt> result = fixture.handler().handle(fixture.command("ADMIN", SIGNATURE));

        assertTrue(result.isSuccess());
        assertTrue(result.valueOrThrow().emrLocked());
        assertEquals(AuditActionCode.EMR_LOCKED, fixture.audit.entry.actionCode());
        assertEquals(AuditOutcome.SUCCESS, fixture.audit.entry.outcome());
    }

    @Test
    void rejectsVisitThatDoesNotExist() {
        Fixture fixture = new Fixture(null);

        Result<LockEmrReceipt> result = fixture.handler().handle(fixture.command("DOCTOR", SIGNATURE));

        assertFailure(result, "NOT_FOUND", "Không tìm thấy lượt khám.");
        assertNull(fixture.updated);
        assertNull(fixture.audit.entry);
    }

    @Test
    void rejectsVisitThatIsNotCompleted() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.DANG_KHAM);

        Result<LockEmrReceipt> result = fixture.handler().handle(fixture.command("DOCTOR", SIGNATURE));

        assertFailure(result, "INVALID_STATE", "Chỉ có thể khóa bệnh án khi lượt khám đã hoàn thành.");
        assertEquals(TrangThaiLuotKham.DANG_KHAM, fixture.visit.getTrangThai());
        assertNull(fixture.updated);
        assertNull(fixture.audit.entry);
    }

    @Test
    void rejectsAlreadyLockedEmr() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.HOAN_THANH);
        fixture.visit.lockEmr("prior-synthetic-signature", DOCTOR_ID);

        Result<LockEmrReceipt> result = fixture.handler().handle(fixture.command("DOCTOR", SIGNATURE));

        assertFailure(result, "EMR_ALREADY_LOCKED", "Hồ sơ bệnh án đã bị khóa trước đó.");
        assertNull(fixture.updated);
        assertNull(fixture.audit.entry);
    }

    @Test
    void rejectsEmptyOrBlankSignatures() {
        for (String signature : new String[]{"", "  \t"}) {
            Fixture fixture = new Fixture(TrangThaiLuotKham.HOAN_THANH);

            Result<LockEmrReceipt> result = fixture.handler().handle(fixture.command("DOCTOR", signature));

            assertFailure(result, "VALIDATION_ERROR", "Thông tin khóa bệnh án không hợp lệ.");
            assertEquals(0, fixture.repositoryReads);
            assertEquals(0, fixture.transactions.calls);
            assertNull(fixture.updated);
        }
    }

    @Test
    void deniesReceptionistAndOtherRolesWithIndependentForbiddenAudit() {
        for (String roleCode : new String[]{"RECEPTIONIST", "OTHER"}) {
            Fixture fixture = new Fixture(TrangThaiLuotKham.HOAN_THANH);

            Result<LockEmrReceipt> result = fixture.handler().handle(fixture.command(roleCode, SIGNATURE));

            assertTrue(result.isFailure());
            assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
            assertEquals(0, fixture.repositoryReads);
            assertEquals(0, fixture.transactions.calls);
            assertNull(fixture.updated);
            assertEquals(1, fixture.audit.independentWrites);
            assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
            assertEquals(AuditOutcome.DENIED, fixture.audit.entry.outcome());
            assertEquals(AuditReasonCode.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
        }
    }

    @Test
    void deniesDoctorWithoutValidSessionContext() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.HOAN_THANH);
        AuthorizationContext invalidContext = new AuthorizationContext(
                EMPLOYEE_ID, ROLE_ID, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, null);
        LockEmrCommand command = new LockEmrCommand(
                VISIT_ID, DOCTOR_ID, SIGNATURE, invalidContext, actor("DOCTOR"));

        Result<LockEmrReceipt> result = fixture.handler().handle(command);

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, fixture.repositoryReads);
        assertEquals(0, fixture.transactions.calls);
        assertEquals(1, fixture.audit.independentWrites);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
    }

    private static void assertFailure(Result<LockEmrReceipt> result, String code, String message) {
        assertTrue(result.isFailure());
        assertEquals(code, result.getError().orElseThrow().code());
        assertEquals(message, result.getError().orElseThrow().message());
    }

    private static void assertSuccessAudit(SystemAuditEntry entry) {
        assertEquals(AuditActionCode.EMR_LOCKED, entry.actionCode());
        assertEquals(AuditResourceType.VISIT, entry.resourceType());
        assertEquals(VISIT_ID, entry.resourceId());
        assertEquals(AuditOutcome.SUCCESS, entry.outcome());
        assertEquals(CORRELATION_ID, entry.correlationId());
    }

    private static AuthorizationContext context(String roleCode) {
        return new AuthorizationContext(EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    }

    private static AuditActorContext actor(String roleCode) {
        return new AuditActorContext(EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, "station-1",
                CORRELATION_ID);
    }

    private static final class Fixture {
        final LuotKham visit;
        int repositoryReads;
        LuotKham updated;
        final TransactionFixture transactions = new TransactionFixture();
        final AuditFixture audit = new AuditFixture(transactions);
        final ILuotKhamRepository visits;

        Fixture(TrangThaiLuotKham state) {
            if (state == null) {
                visit = null;
            } else {
                visit = new LuotKham();
                visit.setId(VISIT_ID);
                visit.setTrangThai(state);
            }
            visits = (ILuotKhamRepository) Proxy.newProxyInstance(
                    ILuotKhamRepository.class.getClassLoader(), new Class<?>[]{ILuotKhamRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getById" -> {
                            repositoryReads++;
                            yield Optional.ofNullable(visit);
                        }
                        case "update" -> {
                            updated = (LuotKham) args[0];
                            yield null;
                        }
                        default -> null;
                    });
        }

        LockEmrHandler handler() {
            return new LockEmrHandler(visits, transactions, audit);
        }

        LockEmrCommand command(String roleCode, String signature) {
            AuthorizationContext context = context(roleCode);
            return new LockEmrCommand(VISIT_ID, DOCTOR_ID, signature, context, actor(roleCode));
        }
    }

    private static final class TransactionFixture implements TransactionBoundary {
        boolean current;
        int calls;

        @Override
        public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
            calls++;
            current = true;
            try {
                return operation.get();
            } finally {
                current = false;
            }
        }
    }

    private static final class AuditFixture implements AuditWritePort {
        private final TransactionFixture transactions;
        boolean inTransaction;
        int independentWrites;
        SystemAuditEntry entry;

        AuditFixture(TransactionFixture transactions) {
            this.transactions = transactions;
        }

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry auditEntry) {
            inTransaction = transactions.current;
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
