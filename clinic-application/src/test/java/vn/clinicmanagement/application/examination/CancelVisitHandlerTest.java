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

class CancelVisitHandlerTest {
    private static final int VISIT_ID = 73;
    private static final int EMPLOYEE_ID = 17;
    private static final int ROLE_ID = 4;
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void cancelsWaitingVisitAndAuditsWithinTransaction() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.CHO_KHAM);

        Result<CancelVisitReceipt> result = fixture.handler().handle(fixture.command("Patient request"));

        assertTrue(result.isSuccess());
        assertEquals(VISIT_ID, result.valueOrThrow().visitId());
        assertEquals(TrangThaiLuotKham.DA_HUY, fixture.visit.getTrangThai());
        assertSame(fixture.visit, fixture.updated);
        assertEquals(1, fixture.transactions.calls);
        assertEquals(1, fixture.repositoryReads);
        assertTrue(fixture.audit.inTransaction);
        assertEquals(AuditActionCode.VISIT_TRANSFERRED, fixture.audit.entry.actionCode());
        assertEquals(AuditResourceType.VISIT, fixture.audit.entry.resourceType());
        assertEquals(VISIT_ID, fixture.audit.entry.resourceId());
        assertEquals(AuditOutcome.SUCCESS, fixture.audit.entry.outcome());
        assertEquals(CORRELATION_ID, fixture.audit.entry.correlationId());
    }

    @Test
    void deniesDoctorAndOtherRolesWithIndependentForbiddenAudit() {
        for (String roleCode : new String[]{"DOCTOR", "ADMIN"}) {
            Fixture fixture = new Fixture(TrangThaiLuotKham.CHO_KHAM);

            Result<CancelVisitReceipt> result = fixture.handler().handle(fixture.command("Synthetic reason", roleCode));

            assertTrue(result.isFailure());
            assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
            assertEquals(0, fixture.repositoryReads);
            assertNull(fixture.updated);
            assertEquals(1, fixture.audit.independentWrites);
            assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
            assertEquals(AuditOutcome.DENIED, fixture.audit.entry.outcome());
            assertEquals(AuditReasonCode.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
        }
    }

    @Test
    void rejectsVisitThatDoesNotExist() {
        Fixture fixture = new Fixture(null);

        Result<CancelVisitReceipt> result = fixture.handler().handle(fixture.command("Synthetic reason"));

        assertFailure(result, "NOT_FOUND", "Không tìm thấy lượt khám.");
        assertNull(fixture.updated);
        assertNull(fixture.audit.entry);
    }

    @Test
    void rejectsVisitsNotWaitingForExamination() {
        for (TrangThaiLuotKham state : new TrangThaiLuotKham[]{
                TrangThaiLuotKham.DANG_KHAM, TrangThaiLuotKham.HOAN_THANH, TrangThaiLuotKham.DA_HUY}) {
            Fixture fixture = new Fixture(state);

            Result<CancelVisitReceipt> result = fixture.handler().handle(fixture.command("Synthetic reason"));

            assertFailure(result, "INVALID_STATE", "Chỉ có thể hủy lượt khám ở trạng thái chờ khám.");
            assertEquals(state, fixture.visit.getTrangThai());
            assertNull(fixture.updated);
            assertNull(fixture.audit.entry);
        }
    }

    @Test
    void rejectsNullEmptyAndBlankCancellationReasons() {
        for (String reason : new String[]{null, "", "  \t"}) {
            Fixture fixture = new Fixture(TrangThaiLuotKham.CHO_KHAM);

            Result<CancelVisitReceipt> result = fixture.handler().handle(fixture.command(reason));

            assertFailure(result, "VALIDATION_ERROR", "Lý do hủy không được để trống.");
            assertEquals(0, fixture.repositoryReads);
            assertNull(fixture.updated);
        }
    }

    private static void assertFailure(Result<CancelVisitReceipt> result, String code, String message) {
        assertTrue(result.isFailure());
        assertEquals(code, result.getError().orElseThrow().code());
        assertEquals(message, result.getError().orElseThrow().message());
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
                            transactions.mutationWasStaged = true;
                            yield null;
                        }
                        default -> null;
                    });
        }

        CancelVisitHandler handler() {
            return new CancelVisitHandler(visits, transactions, audit);
        }

        CancelVisitCommand command(String reason) {
            return command(reason, "RECEPTIONIST");
        }

        CancelVisitCommand command(String reason, String roleCode) {
            return new CancelVisitCommand(VISIT_ID, reason, context(roleCode), actor(roleCode));
        }
    }

    private static final class TransactionFixture implements TransactionBoundary {
        boolean current;
        boolean mutationWasStaged;
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
