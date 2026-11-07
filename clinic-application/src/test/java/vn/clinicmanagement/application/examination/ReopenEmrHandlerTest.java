package vn.clinicmanagement.application.examination;

import org.junit.jupiter.api.BeforeEach;
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
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReopenEmrHandlerTest {
    private static final int VISIT_ID = 601;
    private static final int ADMIN_ID = 99;
    private static final int CHIEF_DOCTOR_ID = 88;
    private static final int DOCTOR_ID = 20;
    private static final int FACILITY_ID = 10;
    private static final int DEPARTMENT_ID = 30;
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final AuthorizationContext ADMIN_CONTEXT =
            new AuthorizationContext(1, ADMIN_ID, "ADMIN", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    private static final AuthorizationContext CHIEF_DOCTOR_CONTEXT =
            new AuthorizationContext(2, CHIEF_DOCTOR_ID, "CHIEF_DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    private static final AuthorizationContext DOCTOR_CONTEXT =
            new AuthorizationContext(3, DOCTOR_ID, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

    private LuotKham visit;
    private final AtomicInteger visitReads = new AtomicInteger();
    private final AtomicInteger visitUpdates = new AtomicInteger();
    private final AtomicInteger transactionCalls = new AtomicInteger();
    private final List<SystemAuditEntry> transactionalAudits = new ArrayList<>();
    private final List<SystemAuditEntry> independentAudits = new ArrayList<>();
    private ILuotKhamRepository visitRepository;
    private ReopenEmrHandler handler;

    @BeforeEach
    void setUp() {
        visit = createLockedVisit();
        visitReads.set(0);
        visitUpdates.set(0);
        transactionCalls.set(0);
        transactionalAudits.clear();
        independentAudits.clear();

        visitRepository = proxy(ILuotKhamRepository.class, (method, args) -> {
            if ("getById".equals(method)) {
                visitReads.incrementAndGet();
                return visit != null && visit.getId() == (int) args[0]
                        ? Optional.of(visit)
                        : Optional.empty();
            }
            if ("update".equals(method)) {
                visitUpdates.incrementAndGet();
            }
            return null;
        });

        TransactionBoundary transactionBoundary = new TransactionBoundary() {
            @Override
            public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
                transactionCalls.incrementAndGet();
                return operation.get();
            }
        };

        AuditWritePort audit = new AuditWritePort() {
            @Override
            public void appendInCurrentTransaction(SystemAuditEntry entry) {
                transactionalAudits.add(entry);
            }

            @Override
            public void appendIndependently(SystemAuditEntry entry) {
                independentAudits.add(entry);
            }

            @Override
            public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}

            @Override
            public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
        };

        handler = new ReopenEmrHandler(visitRepository, transactionBoundary, audit);
    }

    @Test
    void handle_shouldSucceed_whenAdminReopensLockedEmr() {
        ReopenEmrCommand command = new ReopenEmrCommand(
                VISIT_ID,
                ADMIN_ID,
                "Bo sung ket qua chup X-Quang theo bien ban hoi chan",
                ADMIN_CONTEXT,
                actorContext(ADMIN_CONTEXT)
        );

        Result<ReopenEmrReceipt> result = handler.handle(command);

        assertTrue(result.isSuccess());
        ReopenEmrReceipt receipt = result.getValue().orElseThrow();
        assertNotNull(receipt);
        assertEquals(VISIT_ID, receipt.visitId());
        assertFalse(receipt.emrLocked());
        assertEquals(TrangThaiLuotKham.DANG_KHAM, receipt.trangThai());
        assertEquals(ADMIN_ID, receipt.approverId());
        assertNotNull(receipt.reopenedAt());

        assertFalse(visit.isEmrLocked());
        assertEquals(TrangThaiLuotKham.DANG_KHAM, visit.getTrangThai());
        assertEquals(1, visitUpdates.get());
        assertEquals(1, transactionCalls.get());
        assertEquals(1, transactionalAudits.size());

        SystemAuditEntry auditEntry = transactionalAudits.get(0);
        assertEquals(AuditActionCode.EMR_REOPENED, auditEntry.actionCode());
        assertEquals(AuditResourceType.VISIT, auditEntry.resourceType());
        assertEquals(VISIT_ID, auditEntry.resourceId());
        assertEquals(AuditOutcome.SUCCESS, auditEntry.outcome());
    }

    @Test
    void handle_shouldSucceed_whenChiefDoctorReopensLockedEmr() {
        ReopenEmrCommand command = new ReopenEmrCommand(
                VISIT_ID,
                CHIEF_DOCTOR_ID,
                "Dieu chinh chan doan theo hoi chan khoa",
                CHIEF_DOCTOR_CONTEXT,
                actorContext(CHIEF_DOCTOR_CONTEXT)
        );

        Result<ReopenEmrReceipt> result = handler.handle(command);

        assertTrue(result.isSuccess());
        ReopenEmrReceipt receipt = result.getValue().orElseThrow();
        assertFalse(receipt.emrLocked());
        assertEquals(TrangThaiLuotKham.DANG_KHAM, receipt.trangThai());
    }

    @Test
    void handle_shouldDeny_whenCallerIsDoctor() {
        ReopenEmrCommand command = new ReopenEmrCommand(
                VISIT_ID,
                DOCTOR_ID,
                "Tu y mo khoa benh an",
                DOCTOR_CONTEXT,
                actorContext(DOCTOR_CONTEXT)
        );

        Result<ReopenEmrReceipt> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElse(""));
        assertEquals(0, transactionCalls.get());
        assertEquals(1, independentAudits.size());

        SystemAuditEntry auditEntry = independentAudits.get(0);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, auditEntry.actionCode());
        assertEquals(AuditOutcome.DENIED, auditEntry.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, auditEntry.reasonCode());
    }

    @Test
    void handle_shouldDeny_whenSessionContextNull() {
        ReopenEmrCommand command = new ReopenEmrCommand(
                VISIT_ID,
                ADMIN_ID,
                "Mo khoa khong co phien lam viec",
                null,
                actorContext(ADMIN_CONTEXT)
        );

        Result<ReopenEmrReceipt> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElse(""));
        assertEquals(1, independentAudits.size());
    }

    @Test
    void handle_shouldFail_whenValidationFails() {
        ReopenEmrCommand command = new ReopenEmrCommand(
                0,
                ADMIN_ID,
                "Ngắn",
                ADMIN_CONTEXT,
                actorContext(ADMIN_CONTEXT)
        );

        Result<ReopenEmrReceipt> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("VALIDATION_ERROR", result.getErrorCode().orElse(""));
        assertEquals(0, visitReads.get());
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void handle_shouldFail_whenVisitNotFound() {
        ReopenEmrCommand command = new ReopenEmrCommand(
                9999,
                ADMIN_ID,
                "Bo sung ket qua chup X-Quang",
                ADMIN_CONTEXT,
                actorContext(ADMIN_CONTEXT)
        );

        Result<ReopenEmrReceipt> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("NOT_FOUND", result.getErrorCode().orElse(""));
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void handle_shouldFail_whenEmrNotLocked() {
        visit.setEmrLocked(false);
        ReopenEmrCommand command = new ReopenEmrCommand(
                VISIT_ID,
                ADMIN_ID,
                "Mo khoa khi benh an chua khoa",
                ADMIN_CONTEXT,
                actorContext(ADMIN_CONTEXT)
        );

        Result<ReopenEmrReceipt> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("EMR_NOT_LOCKED", result.getErrorCode().orElse(""));
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void handle_shouldFail_whenVisitNotHoanThanh() {
        visit.setTrangThai(TrangThaiLuotKham.DANG_KHAM);
        ReopenEmrCommand command = new ReopenEmrCommand(
                VISIT_ID,
                ADMIN_ID,
                "Mo khoa khi luot kham dang dien ra",
                ADMIN_CONTEXT,
                actorContext(ADMIN_CONTEXT)
        );

        Result<ReopenEmrReceipt> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("INVALID_STATE", result.getErrorCode().orElse(""));
        assertEquals(0, visitUpdates.get());
    }

    private static LuotKham createLockedVisit() {
        LuotKham v = new LuotKham();
        v.setId(VISIT_ID);
        v.setMaBenhNhan(101);
        v.setMaBacSi(DOCTOR_ID);
        v.setMaCoSo(FACILITY_ID);
        v.setMaKhoa(DEPARTMENT_ID);
        v.setTrangThai(TrangThaiLuotKham.DANG_KHAM);
        v.completeExam();
        v.lockEmr("SYNTHETIC-SIG-123", DOCTOR_ID);
        return v;
    }

    private static AuditActorContext actorContext(AuthorizationContext context) {
        return new AuditActorContext(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                "WS-TEST-01",
                CORRELATION_ID);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> handler.invoke(method.getName(), args == null ? new Object[0] : args)
        );
    }

    @FunctionalInterface
    private interface InvocationHandler {
        Object invoke(String method, Object[] args) throws Throwable;
    }
}
