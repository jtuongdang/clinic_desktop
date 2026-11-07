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
import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompleteExamHandlerTest {
    private static final int VISIT_ID = 501;
    private static final int DOCTOR_ID = 20;
    private static final int FACILITY_ID = 10;
    private static final int DEPARTMENT_ID = 30;
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private LuotKham visit;
    private final AtomicInteger visitReads = new AtomicInteger();
    private final AtomicInteger visitUpdates = new AtomicInteger();
    private final AtomicInteger transactionCalls = new AtomicInteger();
    private final List<SystemAuditEntry> transactionalAudits = new ArrayList<>();
    private final List<SystemAuditEntry> independentAudits = new ArrayList<>();
    private ILuotKhamRepository visitRepository;
    private CompleteExamHandler handler;

    @BeforeEach
    void setUp() {
        visit = createVisit(TrangThaiLuotKham.DANG_KHAM);
        visit.setMaBacSi(DOCTOR_ID);
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
            public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {
            }

            @Override
            public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {
            }
        };
        handler = new CompleteExamHandler(visitRepository, transactionBoundary, audit);
    }

    @Test
    void completesExamAndAuditsEmrCompletion() {
        LocalDate followUp = LocalDate.of(2026, 12, 1);
        CompleteExamCommand command = new CompleteExamCommand(
                VISIT_ID, DOCTOR_ID, KetQuaDieuTri.DO_GIAM, HuongXuTri.HEN_TAI_KHAM,
                "Tái khám theo lịch.", followUp, DOCTOR_CONTEXT, actor(DOCTOR_CONTEXT));

        Result<CompleteExamReceipt> result = handler.handle(command);

        assertTrue(result.isSuccess());
        CompleteExamReceipt receipt = result.getValue().orElseThrow();
        assertEquals(VISIT_ID, receipt.visitId());
        assertEquals(TrangThaiLuotKham.HOAN_THANH, receipt.status());
        assertNotNull(receipt.completedAt());
        assertEquals(TrangThaiLuotKham.HOAN_THANH, visit.getTrangThai());
        assertEquals(receipt.completedAt(), visit.getThoiGianKetThucKham());
        assertEquals(KetQuaDieuTri.DO_GIAM, visit.getKetQuaDieuTri());
        assertEquals(HuongXuTri.HEN_TAI_KHAM, visit.getHuongXuTri());
        assertEquals("Tái khám theo lịch.", visit.getLoiDanBacSi());
        assertEquals(followUp, visit.getNgayHenTaiKham());
        assertEquals(1, visitReads.get());
        assertEquals(1, visitUpdates.get());
        assertEquals(1, transactionCalls.get());
        assertEquals(1, transactionalAudits.size());
        assertTrue(independentAudits.isEmpty());

        SystemAuditEntry entry = transactionalAudits.get(0);
        assertEquals(AuditActionCode.EMR_COMPLETED, entry.actionCode());
        assertEquals(AuditResourceType.VISIT, entry.resourceType());
        assertEquals(VISIT_ID, entry.resourceId());
        assertEquals(AuditOutcome.SUCCESS, entry.outcome());
        assertEquals("DOCTOR", entry.roleCode());
    }

    @Test
    void rejectsMissingVisit() {
        visit = null;

        Result<CompleteExamReceipt> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "NOT_FOUND");
        assertEquals("Không tìm thấy lượt khám.", result.getError().orElseThrow().message());
        assertEquals(1, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsVisitNotInProgress() {
        visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);

        Result<CompleteExamReceipt> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "INVALID_STATE");
        assertEquals("Lượt khám không ở trạng thái đang khám.", result.getError().orElseThrow().message());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsEmrLockedVisit() {
        visit.lockEmr("synthetic-signature", DOCTOR_ID);

        Result<CompleteExamReceipt> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "INVALID_STATE");
        assertTrue(result.getError().orElseThrow().message().contains("đã được ký số và khóa an toàn"));
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsDoctorMismatch() {
        visit.setMaBacSi(DOCTOR_ID + 1);

        Result<CompleteExamReceipt> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "DOCTOR_MISMATCH");
        assertEquals("Chỉ bác sĩ phụ trách mới có quyền kết thúc lượt khám.",
                result.getError().orElseThrow().message());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void deniesReceptionistAndWritesForbiddenAudit() {
        assertDenial(command(RECEPTIONIST_CONTEXT), "RECEPTIONIST");
    }

    @Test
    void deniesOtherAuthenticatedRoleAndWritesForbiddenAudit() {
        AuthorizationContext otherRole = new AuthorizationContext(
                3, 30, "NURSE", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

        assertDenial(command(otherRole), "NURSE");
    }

    @Test
    void deniesMissingSessionAndWritesAnonymousForbiddenAudit() {
        assertDenial(new CompleteExamCommand(
                VISIT_ID, DOCTOR_ID, null, null, null, null, null, null), null);
    }

    @Test
    void rejectsInvalidIdentifiersBeforeAuthorizationOrTransaction() {
        Result<CompleteExamReceipt> invalidVisit = handler.handle(
                new CompleteExamCommand(0, DOCTOR_ID, DOCTOR_CONTEXT));
        Result<CompleteExamReceipt> invalidDoctor = handler.handle(
                new CompleteExamCommand(VISIT_ID, 0, DOCTOR_CONTEXT));

        assertFailure(invalidVisit, "VALIDATION_ERROR");
        assertFailure(invalidDoctor, "VALIDATION_ERROR");
        assertEquals("Thông tin kết thúc khám không hợp lệ.", invalidVisit.getError().orElseThrow().message());
        assertEquals("Thông tin kết thúc khám không hợp lệ.", invalidDoctor.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitReads.get());
    }

    private void assertDenial(CompleteExamCommand command, String roleCode) {
        Result<CompleteExamReceipt> result = handler.handle(command);

        assertFailure(result, "AUTHORIZATION_DENIED");
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitReads.get());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
        assertEquals(1, independentAudits.size());

        SystemAuditEntry entry = independentAudits.get(0);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, entry.actionCode());
        assertEquals(AuditOutcome.DENIED, entry.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, entry.reasonCode());
        assertEquals(roleCode, entry.roleCode());
        if (roleCode == null) {
            assertNull(entry.actorEmployeeId());
        }
        assertNull(entry.resourceType());
        assertNull(entry.resourceId());
    }

    private static void assertFailure(Result<CompleteExamReceipt> result, String code) {
        assertFalse(result.isSuccess());
        assertEquals(code, result.getErrorCode().orElse(null));
    }

    private static CompleteExamCommand command(AuthorizationContext context) {
        return new CompleteExamCommand(
                VISIT_ID, DOCTOR_ID, null, null, null, null, context, actor(context));
    }

    private static AuditActorContext actor(AuthorizationContext context) {
        return new AuditActorContext(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                "WS-01",
                CORRELATION_ID);
    }

    private static LuotKham createVisit(TrangThaiLuotKham state) {
        LuotKham result = new LuotKham();
        result.setId(VISIT_ID);
        result.setTrangThai(state);
        return result;
    }

    private static final AuthorizationContext DOCTOR_CONTEXT =
            new AuthorizationContext(2, 20, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    private static final AuthorizationContext RECEPTIONIST_CONTEXT =
            new AuthorizationContext(1, 10, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> instance == args[0];
                    case "hashCode" -> System.identityHashCode(instance);
                    case "toString" -> type.getSimpleName() + " test proxy";
                    default -> null;
                };
            }
            return invocation.invoke(method.getName(), args);
        });
        return type.cast(proxy);
    }
}
