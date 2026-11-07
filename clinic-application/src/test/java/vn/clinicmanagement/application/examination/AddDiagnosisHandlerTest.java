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
import vn.clinicmanagement.domain.examination.ChanDoan;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddDiagnosisHandlerTest {
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
    private AddDiagnosisHandler handler;

    @BeforeEach
    void setUp() {
        visit = new LuotKham();
        visit.setId(VISIT_ID);
        visit.setMaBacSi(DOCTOR_ID);
        visit.setTrangThai(TrangThaiLuotKham.DANG_KHAM);
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
        handler = new AddDiagnosisHandler(visitRepository, transactionBoundary, audit);
    }

    @Test
    void addsPrimaryDiagnosisAndDemotesPreviousPrimary() {
        ChanDoan previousPrimary = diagnosis("Chẩn đoán chính cũ", true);
        visit.getChanDoans().add(previousPrimary);

        Result<AddDiagnosisReceipt> result = handler.handle(new AddDiagnosisCommand(
                VISIT_ID, DOCTOR_ID, 700, "  Viêm họng cấp  ", true, null,
                DOCTOR_CONTEXT, actor(DOCTOR_CONTEXT)));

        assertTrue(result.isSuccess());
        AddDiagnosisReceipt receipt = result.getValue().orElseThrow();
        assertEquals(VISIT_ID, receipt.visitId());
        assertEquals("Viêm họng cấp", receipt.chanDoanText());
        assertTrue(receipt.laChinh());
        assertEquals("XAC_DINH", receipt.loaiChanDoan());
        assertNotNull(receipt.addedAt());
        assertFalse(previousPrimary.isLaChinh());
        assertEquals(2, visit.getChanDoans().size());

        ChanDoan added = visit.getChanDoans().get(1);
        assertEquals(VISIT_ID, added.getMaLuotKham());
        assertEquals(700, added.getMaICD());
        assertEquals("Viêm họng cấp", added.getChanDoanText());
        assertTrue(added.isLaChinh());
        assertEquals("XAC_DINH", added.getLoaiChanDoan());
        assertSame(visit, added.getLuotKham());
        assertEquals(1, visitReads.get());
        assertEquals(1, visitUpdates.get());
        assertEquals(1, transactionCalls.get());
        assertEquals(1, transactionalAudits.size());
        assertTrue(independentAudits.isEmpty());

        SystemAuditEntry audit = transactionalAudits.get(0);
        assertEquals(AuditActionCode.EMR_UPDATED, audit.actionCode());
        assertEquals(AuditResourceType.VISIT, audit.resourceType());
        assertEquals(VISIT_ID, audit.resourceId());
        assertEquals(AuditOutcome.SUCCESS, audit.outcome());
    }

    @Test
    void addsSecondaryDiagnosisWithoutDemotingPrimary() {
        ChanDoan existingPrimary = diagnosis("Chẩn đoán chính", true);
        visit.getChanDoans().add(existingPrimary);

        Result<AddDiagnosisReceipt> result = handler.handle(new AddDiagnosisCommand(
                VISIT_ID, DOCTOR_ID, 701, "Rối loạn tiêu hóa", false, "KEM_THEO",
                DOCTOR_CONTEXT, actor(DOCTOR_CONTEXT)));

        assertTrue(result.isSuccess());
        assertTrue(existingPrimary.isLaChinh());
        assertFalse(result.getValue().orElseThrow().laChinh());
        assertEquals("KEM_THEO", result.getValue().orElseThrow().loaiChanDoan());
        assertEquals(2, visit.getChanDoans().size());
        assertFalse(visit.getChanDoans().get(1).isLaChinh());
        assertEquals(1, visitUpdates.get());
    }

    @Test
    void rejectsMissingVisit() {
        visit = null;

        Result<AddDiagnosisReceipt> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "NOT_FOUND");
        assertEquals("Không tìm thấy lượt khám.", result.getError().orElseThrow().message());
        assertEquals(1, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsBlankDiagnosisTextAndInvalidIdentifiers() {
        Result<AddDiagnosisReceipt> blank = handler.handle(new AddDiagnosisCommand(
                VISIT_ID, DOCTOR_ID, "   ", false, DOCTOR_CONTEXT));
        Result<AddDiagnosisReceipt> invalidVisit = handler.handle(new AddDiagnosisCommand(
                0, DOCTOR_ID, "Đau đầu", false, DOCTOR_CONTEXT));
        Result<AddDiagnosisReceipt> invalidDoctor = handler.handle(new AddDiagnosisCommand(
                VISIT_ID, 0, "Đau đầu", false, DOCTOR_CONTEXT));

        assertFailure(blank, "VALIDATION_ERROR");
        assertFailure(invalidVisit, "VALIDATION_ERROR");
        assertFailure(invalidDoctor, "VALIDATION_ERROR");
        assertEquals("Thông tin chẩn đoán không hợp lệ.", blank.getError().orElseThrow().message());
        assertEquals(0, visitReads.get());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void rejectsInvalidDiagnosisType() {
        Result<AddDiagnosisReceipt> result = handler.handle(new AddDiagnosisCommand(
                VISIT_ID, DOCTOR_ID, 700, "Đau đầu", false, "KHONG_HOP_LE",
                DOCTOR_CONTEXT, actor(DOCTOR_CONTEXT)));

        assertFailure(result, "VALIDATION_ERROR");
        assertEquals("Loại chẩn đoán không hợp lệ.", result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitReads.get());
    }

    @Test
    void rejectsDoctorMismatch() {
        visit.setMaBacSi(DOCTOR_ID + 1);

        Result<AddDiagnosisReceipt> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "DOCTOR_MISMATCH");
        assertEquals("Chỉ bác sĩ phụ trách mới có quyền thêm chẩn đoán.",
                result.getError().orElseThrow().message());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsLockedEmr() {
        visit.lockEmr("synthetic-signature", DOCTOR_ID);

        Result<AddDiagnosisReceipt> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "EMR_LOCKED");
        assertEquals("Hồ sơ bệnh án đã bị khóa không thể thêm chẩn đoán.",
                result.getError().orElseThrow().message());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsVisitNotInProgress() {
        visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);

        Result<AddDiagnosisReceipt> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "INVALID_STATE");
        assertEquals("Chỉ có thể thêm chẩn đoán khi lượt khám đang diễn ra.",
                result.getError().orElseThrow().message());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void deniesUnauthorizedCallerAndWritesForbiddenAudit() {
        AuthorizationContext receptionist = new AuthorizationContext(
                1, 10, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

        Result<AddDiagnosisReceipt> result = handler.handle(command(receptionist));

        assertFailure(result, "AUTHORIZATION_DENIED");
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitReads.get());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
        assertEquals(1, independentAudits.size());

        SystemAuditEntry audit = independentAudits.get(0);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, audit.actionCode());
        assertEquals(AuditOutcome.DENIED, audit.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, audit.reasonCode());
        assertEquals("RECEPTIONIST", audit.roleCode());
        assertNull(audit.resourceType());
        assertNull(audit.resourceId());
    }

    @Test
    void deniesMissingSessionAndWritesAnonymousForbiddenAudit() {
        Result<AddDiagnosisReceipt> result = handler.handle(new AddDiagnosisCommand(
                VISIT_ID, DOCTOR_ID, null, "Đau đầu", false, "XAC_DINH", null, null));

        assertFailure(result, "AUTHORIZATION_DENIED");
        assertEquals(1, independentAudits.size());
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, independentAudits.get(0).actionCode());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, independentAudits.get(0).reasonCode());
        assertNull(independentAudits.get(0).actorEmployeeId());
    }

    private void assertFailure(Result<AddDiagnosisReceipt> result, String code) {
        assertFalse(result.isSuccess());
        assertEquals(code, result.getErrorCode().orElse(null));
    }

    private static AddDiagnosisCommand command(AuthorizationContext context) {
        return new AddDiagnosisCommand(
                VISIT_ID, DOCTOR_ID, null, "Đau đầu", false, "XAC_DINH", context, actor(context));
    }

    private static AuditActorContext actor(AuthorizationContext context) {
        return context != null && context.isValid()
                ? new AuditActorContext(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        context.departmentId(),
                        "WS-01",
                        CORRELATION_ID)
                : null;
    }

    private static ChanDoan diagnosis(String text, boolean primary) {
        ChanDoan diagnosis = new ChanDoan();
        diagnosis.setChanDoanText(text);
        diagnosis.setLaChinh(primary);
        return diagnosis;
    }

    private static final AuthorizationContext DOCTOR_CONTEXT =
            new AuthorizationContext(2, 20, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        Object instance = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + " test proxy";
                    default -> null;
                };
            }
            return invocation.invoke(method.getName(), args);
        });
        return type.cast(instance);
    }
}
