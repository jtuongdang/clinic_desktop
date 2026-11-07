package vn.clinicmanagement.application.auth;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.common.ApplicationFailureDiagnostics;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompleteLoginSessionHandlerTest {
    private static final int SYNTHETIC_EMPLOYEE_ID = 701;
    private static final RoleChoice ASSIGNED_ROLE_A = new RoleChoice(31, "ADMIN", "Quản trị viên (Admin)");
    private static final RoleChoice ASSIGNED_ROLE_B = new RoleChoice(42, "DOCTOR", "Bác sĩ điều trị (Doctor Station)");
    private static final WorkScopeChoice ASSIGNED_SCOPE_A = new WorkScopeChoice(11, "Cơ sở mẫu A", 21, "Khoa mẫu A");
    private static final WorkScopeChoice ASSIGNED_SCOPE_B = new WorkScopeChoice(12, "Cơ sở mẫu B", 22, "Khoa mẫu B");
    private static final UUID CORRELATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void recordsSuccessfulLoginAndAppendsAuthLoginSuccessInsideSameTransactionAfterRoleAndScopeSelection() {
        Fixture fixture = new Fixture();
        AuthenticatedUser user = authenticatedUser();

        Result<CompletedLoginSession> result = fixture.handler.handle(new CompleteLoginSessionCommand(
                user, ASSIGNED_ROLE_B, ASSIGNED_SCOPE_B, "synth-workstation-07", CORRELATION_ID));

        assertTrue(result.isSuccess());
        CompletedLoginSession completed = result.valueOrThrow();
        assertEquals(SYNTHETIC_EMPLOYEE_ID, completed.userId());
        assertEquals("synth.user.701", completed.username());
        assertEquals("Họ Mẫu Nhân Viên 701", completed.fullName());
        assertEquals(ASSIGNED_ROLE_B, completed.role());
        assertEquals(ASSIGNED_SCOPE_B, completed.workScope());
        assertEquals(CORRELATION_ID, completed.correlationId());
        assertEquals(Instant.parse("2026-01-15T08:30:00Z"), completed.loginAtUtc());

        assertEquals(1, fixture.transactions.calls.get());
        assertTrue(fixture.transactions.committed.get());
        assertFalse(fixture.transactions.rolledBack.get());
        assertEquals(1, fixture.recordLoginCalls.get());
        assertTrue(fixture.loginRecordedInTransaction.get());
        assertEquals(SYNTHETIC_EMPLOYEE_ID, fixture.recordedEmployeeId.get());
        assertNotNull(fixture.recordedLoginAt.get());

        assertEquals(1, fixture.audit.transactionalEntries.size());
        assertTrue(fixture.audit.appendedInTransaction.get());
        assertTrue(fixture.audit.independentEntries.isEmpty());
        SystemAuditEntry entry = fixture.audit.transactionalEntries.get(0);
        assertEquals(AuditActionCode.AUTH_LOGIN_SUCCESS, entry.actionCode());
        assertEquals(AuditResourceType.EMPLOYEE, entry.resourceType());
        assertEquals(AuditOutcome.SUCCESS, entry.outcome());
        assertNull(entry.reasonCode());
        assertEquals(SYNTHETIC_EMPLOYEE_ID, entry.actorEmployeeId());
        assertEquals("DOCTOR", entry.roleCode());
        assertEquals(12, entry.facilityId());
        assertEquals(22, entry.departmentId());
        assertEquals(SYNTHETIC_EMPLOYEE_ID, entry.resourceId());
        assertEquals("synth-workstation-07", entry.workstationId());
        assertEquals(CORRELATION_ID, entry.correlationId());
        assertEquals(Instant.parse("2026-01-15T08:30:00Z"), entry.occurredAtUtc());
    }

    @Test
    void rejectsNullOrUnassignedRoleAndScopeWithoutStartingTransactionOrRecordingLogin() {
        Fixture fixture = new Fixture();
        AuthenticatedUser user = authenticatedUser();
        RoleChoice unassignedRole = new RoleChoice(99, "RECEPTION", "Tiếp đón");
        WorkScopeChoice unassignedScope = new WorkScopeChoice(99, "Cơ sở khác", 88, "Khoa khác");

        List<CompleteLoginSessionCommand> invalidCommands = List.of(
                new CompleteLoginSessionCommand(user, unassignedRole, ASSIGNED_SCOPE_A),
                new CompleteLoginSessionCommand(user, ASSIGNED_ROLE_A, unassignedScope));

        assertTrue(fixture.handler.handle(null).isFailure());
        assertEquals("AUTH_SELECTION_REQUIRED", fixture.handler.handle(
                new CompleteLoginSessionCommand(null, ASSIGNED_ROLE_A, ASSIGNED_SCOPE_A))
                .getError().orElseThrow().code());
        assertEquals("AUTH_SELECTION_REQUIRED", fixture.handler.handle(
                new CompleteLoginSessionCommand(user, null, ASSIGNED_SCOPE_A))
                .getError().orElseThrow().code());
        assertEquals("AUTH_SELECTION_REQUIRED", fixture.handler.handle(
                new CompleteLoginSessionCommand(user, ASSIGNED_ROLE_A, null))
                .getError().orElseThrow().code());
        Result<CompletedLoginSession> invalidRoleResult = fixture.handler.handle(invalidCommands.get(0));
        Result<CompletedLoginSession> invalidScopeResult = fixture.handler.handle(invalidCommands.get(1));
        assertTrue(invalidRoleResult.isFailure());
        assertEquals("AUTH_INVALID_ROLE_SELECTION", invalidRoleResult.getError().orElseThrow().code());
        assertTrue(invalidScopeResult.isFailure());
        assertEquals("AUTH_INVALID_SCOPE_SELECTION", invalidScopeResult.getError().orElseThrow().code());
        assertEquals(0, fixture.transactions.calls.get());
        assertEquals(0, fixture.recordLoginCalls.get());
        assertTrue(fixture.audit.transactionalEntries.isEmpty());
        assertTrue(fixture.audit.independentEntries.isEmpty());
    }

    @Test
    void rejectsRoleOrScopeRevokedAfterPasswordAuthentication() {
        Fixture fixture = new Fixture();
        fixture.assignedScopes = List.of(new NhanVienWorkScope(11, "Cơ sở mẫu A", 21, "Khoa mẫu A"));

        Result<CompletedLoginSession> result = fixture.handler.handle(new CompleteLoginSessionCommand(
                authenticatedUser(), ASSIGNED_ROLE_A, ASSIGNED_SCOPE_B));

        assertTrue(result.isFailure());
        assertEquals("AUTH_INVALID_SELECTION", result.getError().orElseThrow().code());
        assertEquals(1, fixture.transactions.calls.get());
        assertEquals(0, fixture.recordLoginCalls.get());
        assertTrue(fixture.audit.transactionalEntries.isEmpty());
    }

    @Test
    void auditFailureRollsBackLastLoginAndPreventsSessionCreation() {
        Fixture fixture = new Fixture();
        String failureMarker = "synthetic audit exception marker synth.user.701 synthetic-password-marker";
        fixture.audit.failure = new IllegalStateException(failureMarker);

        Result<CompletedLoginSession> result;
        try (CapturedDiagnostics diagnostics = new CapturedDiagnostics()) {
            result = fixture.handler.handle(new CompleteLoginSessionCommand(
                    authenticatedUser(), ASSIGNED_ROLE_A, ASSIGNED_SCOPE_A, "synth-workstation-07", CORRELATION_ID));
            assertEquals(1, diagnostics.messages.size());
            String message = diagnostics.messages.get(0);
            assertTrue(message.contains("operation=COMPLETE_LOGIN_SESSION result=AUTH_UNAVAILABLE"));
            assertTrue(message.contains("correlationId=" + CORRELATION_ID));
            assertFalse(message.contains(failureMarker));
            assertFalse(message.contains("synth.user.701"));
            assertFalse(message.contains("synthetic-password-marker"));
        }

        assertTrue(result.isFailure());
        Result.ApplicationError error = result.getError().orElseThrow();
        assertEquals("AUTH_UNAVAILABLE", error.code());
        assertFalse(error.message().contains("synth.user.701"));
        assertFalse(error.message().contains("synthetic audit failure"));
        assertFalse(error.message().contains("synthetic-password-marker"));
        assertEquals(1, fixture.transactions.calls.get());
        assertTrue(fixture.transactions.rolledBack.get());
        assertFalse(fixture.transactions.committed.get());
        assertNull(fixture.committedLoginAt.get());
    }

    @Test
    void invalidRoleAndScopeSelectionsDoNotEmitUnexpectedDiagnostics() {
        Fixture fixture = new Fixture();
        try (CapturedDiagnostics diagnostics = new CapturedDiagnostics()) {
            fixture.handler.handle(new CompleteLoginSessionCommand(
                    authenticatedUser(), new RoleChoice(99, "OTHER", "Vai trò khác"), ASSIGNED_SCOPE_A));
            fixture.handler.handle(new CompleteLoginSessionCommand(
                    authenticatedUser(), ASSIGNED_ROLE_A,
                    new WorkScopeChoice(99, "Cơ sở khác", 88, "Khoa khác")));
            assertTrue(diagnostics.messages.isEmpty());
        }
    }

    private static final class CapturedDiagnostics implements AutoCloseable {
        private final Logger logger = Logger.getLogger(ApplicationFailureDiagnostics.class.getName());
        private final List<String> messages = new ArrayList<>();
        private final Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { messages.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        };

        private CapturedDiagnostics() {
            logger.addHandler(handler);
        }

        @Override public void close() {
            logger.removeHandler(handler);
        }
    }

    private static AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                SYNTHETIC_EMPLOYEE_ID,
                "synth.user.701",
                "Họ Mẫu Nhân Viên 701",
                List.of(ASSIGNED_ROLE_A, ASSIGNED_ROLE_B),
                List.of(ASSIGNED_SCOPE_A, ASSIGNED_SCOPE_B));
    }

    private static final class Fixture {
        private final AtomicInteger recordLoginCalls = new AtomicInteger();
        private final AtomicBoolean loginRecordedInTransaction = new AtomicBoolean(false);
        private final AtomicReference<Integer> recordedEmployeeId = new AtomicReference<>();
        private final AtomicReference<LocalDateTime> recordedLoginAt = new AtomicReference<>();
        private final AtomicReference<LocalDateTime> committedLoginAt = new AtomicReference<>();
        private List<NhanVienWorkScope> assignedScopes = List.of(
                new NhanVienWorkScope(11, "Cơ sở mẫu A", 21, "Khoa mẫu A"),
                new NhanVienWorkScope(12, "Cơ sở mẫu B", 22, "Khoa mẫu B"));
        private final RollbackAwareTransactionProbe transactions = new RollbackAwareTransactionProbe(
                () -> committedLoginAt.set(recordedLoginAt.get()),
                () -> committedLoginAt.set(null));
        private final TransactionalAuditProbe audit = new TransactionalAuditProbe(transactions.active);
        private final CompleteLoginSessionHandler handler;

        private Fixture() {
            INhanVienRepository employees = (INhanVienRepository) Proxy.newProxyInstance(
                    INhanVienRepository.class.getClassLoader(),
                    new Class<?>[]{INhanVienRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "recordSuccessfulLogin" -> {
                            recordLoginCalls.incrementAndGet();
                            loginRecordedInTransaction.set(transactions.active.get());
                            recordedEmployeeId.set((Integer) args[0]);
                            recordedLoginAt.set((LocalDateTime) args[1]);
                            yield null;
                        }
                        case "getById" -> {
                            NhanVien employee = new NhanVien();
                            employee.setId(SYNTHETIC_EMPLOYEE_ID);
                            employee.setUsername("synth.user.701");
                            employee.setHoatDong(true);
                            yield Optional.of(employee);
                        }
                        case "getByUsername" -> Optional.empty();
                        case "getAll", "getActiveDoctors", "find", "getRoleNames" -> List.of();
                        case "getAssignedRoles" -> List.of(role(31, "ADMIN"), role(42, "DOCTOR"));
                        case "getAssignedWorkScopes" -> assignedScopes;
                        case "exists" -> false;
                        case "count" -> 0;
                        default -> null;
                    });
            handler = new CompleteLoginSessionHandler(
                    employees,
                    transactions,
                    audit,
                    Clock.fixed(Instant.parse("2026-01-15T08:30:00Z"), ZoneOffset.UTC));
        }

        private static VaiTro role(int id, String code) {
            VaiTro role = new VaiTro();
            role.setId(id);
            role.setTenVaiTro(code);
            return role;
        }
    }

    private static final class TransactionalAuditProbe implements AuditWritePort {
        private final AtomicBoolean transactionActive;
        private final AtomicBoolean appendedInTransaction = new AtomicBoolean(false);
        private final List<SystemAuditEntry> transactionalEntries = new ArrayList<>();
        private final List<SystemAuditEntry> independentEntries = new ArrayList<>();
        private RuntimeException failure;

        private TransactionalAuditProbe(AtomicBoolean transactionActive) {
            this.transactionActive = transactionActive;
        }

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry entry) {
            appendedInTransaction.set(transactionActive.get());
            if (failure != null) throw failure;
            transactionalEntries.add(entry);
        }

        @Override
        public void appendIndependently(SystemAuditEntry entry) {
            if (failure != null) throw failure;
            independentEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {
            throw new UnsupportedOperationException("Medical record audit is not used by login tests.");
        }

        @Override
        public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {
            throw new UnsupportedOperationException("Medical record audit is not used by login tests.");
        }
    }

    private static final class RollbackAwareTransactionProbe implements TransactionBoundary {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicBoolean active = new AtomicBoolean(false);
        private final AtomicBoolean committed = new AtomicBoolean(false);
        private final AtomicBoolean rolledBack = new AtomicBoolean(false);
        private final Runnable onCommit;
        private final Runnable onRollback;

        private RollbackAwareTransactionProbe(Runnable onCommit, Runnable onRollback) {
            this.onCommit = onCommit;
            this.onRollback = onRollback;
        }

        @Override
        public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
            calls.incrementAndGet();
            active.set(true);
            try {
                Result<T> result = operation.get();
                if (result != null && result.isSuccess()) {
                    committed.set(true);
                    onCommit.run();
                } else {
                    rolledBack.set(true);
                    onRollback.run();
                }
                return result;
            } catch (RuntimeException ex) {
                rolledBack.set(true);
                onRollback.run();
                throw ex;
            } finally {
                active.set(false);
            }
        }
    }
}
