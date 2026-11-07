package vn.clinicmanagement.application.prescription;

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
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.common.events.DonThuocApprovedEvent;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovePrescriptionHandlerTest {
    private static final int PRESCRIPTION_ID = 901;
    private static final int VISIT_ID = 502;
    private static final int DOCTOR_ID = 21;
    private static final int FACILITY_ID = 10;
    private static final int DEPARTMENT_ID = 30;
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final AuthorizationContext DOCTOR_CONTEXT =
            new AuthorizationContext(2, 20, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    private static final AuthorizationContext ADMIN_CONTEXT =
            new AuthorizationContext(1, 1, "ADMIN", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

    private final List<SystemAuditEntry> transactionalAudits = new ArrayList<>();
    private final List<SystemAuditEntry> independentAudits = new ArrayList<>();
    private final AtomicInteger transactionCalls = new AtomicInteger();
    private DonThuoc prescription;
    private LuotKham visit;
    private DonThuoc updatedPrescription;
    private ApprovePrescriptionHandler handler;

    @BeforeEach
    void setUp() {
        prescription = new DonThuoc();
        prescription.setId(PRESCRIPTION_ID);
        prescription.setMaLuotKham(VISIT_ID);
        prescription.setMaBacSi(DOCTOR_ID);
        prescription.setTrangThai(TrangThaiDonThuoc.TAM);
        visit = new LuotKham();
        visit.setId(VISIT_ID);
        visit.setEmrLocked(false);
        updatedPrescription = null;
        transactionalAudits.clear();
        independentAudits.clear();
        transactionCalls.set(0);

        IDonThuocRepository prescriptionRepository = proxy(IDonThuocRepository.class, (method, args) -> {
            if ("getById".equals(method)) {
                return prescription != null && prescription.getId() == (int) args[0]
                        ? Optional.of(prescription)
                        : Optional.empty();
            }
            if ("update".equals(method)) {
                updatedPrescription = (DonThuoc) args[0];
            }
            return null;
        });
        ILuotKhamRepository visitRepository = proxy(ILuotKhamRepository.class, (method, args) ->
                "getById".equals(method) && visit != null && visit.getId() == (int) args[0]
                        ? Optional.of(visit)
                        : Optional.empty());
        TransactionBoundary transactions = new TransactionBoundary() {
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
        handler = new ApprovePrescriptionHandler(prescriptionRepository, visitRepository, transactions, audit);
    }

    @Test
    void approvesPrescriptionByPrescribingDoctorAndAuditsSuccess() {
        Result<ApprovePrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, DOCTOR_CONTEXT));

        assertTrue(result.isSuccess());
        ApprovePrescriptionReceipt receipt = result.getValue().orElseThrow();
        assertEquals(PRESCRIPTION_ID, receipt.prescriptionId());
        assertEquals(VISIT_ID, receipt.visitId());
        assertEquals(TrangThaiDonThuoc.DA_DUYET, receipt.trangThai());
        assertNotNull(receipt.approvedAt());
        assertSame(prescription, updatedPrescription);
        assertEquals(1, transactionCalls.get());
        assertEquals(1, transactionalAudits.size());
        assertTrue(independentAudits.isEmpty());
        assertTrue(prescription.getDomainEvents().get(0) instanceof DonThuocApprovedEvent);
        assertSuccessAudit(transactionalAudits.get(0));
    }

    @Test
    void approvesPrescriptionByAdminEvenWhenDoctorDoesNotMatch() {
        prescription.setMaBacSi(DOCTOR_ID + 1);

        Result<ApprovePrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, ADMIN_CONTEXT));

        assertTrue(result.isSuccess());
        assertEquals(TrangThaiDonThuoc.DA_DUYET, result.getValue().orElseThrow().trangThai());
        assertSame(prescription, updatedPrescription);
        assertEquals(1, transactionCalls.get());
        assertSuccessAudit(transactionalAudits.get(0));
    }

    @Test
    void rejectsNonpositivePrescriptionOrDoctorIds() {
        Result<ApprovePrescriptionReceipt> invalidPrescription =
                handler.handle(command(0, DOCTOR_ID, DOCTOR_CONTEXT));
        Result<ApprovePrescriptionReceipt> invalidDoctor =
                handler.handle(command(PRESCRIPTION_ID, -1, DOCTOR_CONTEXT));

        assertFailure(invalidPrescription, "VALIDATION_ERROR");
        assertFailure(invalidDoctor, "VALIDATION_ERROR");
        assertEquals("Thông tin duyệt đơn thuốc không hợp lệ.",
                invalidPrescription.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
        assertTrue(transactionalAudits.isEmpty());
        assertTrue(independentAudits.isEmpty());
    }

    @Test
    void rejectsMissingPrescription() {
        prescription = null;

        Result<ApprovePrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, DOCTOR_CONTEXT));

        assertFailure(result, "NOT_FOUND");
        assertEquals("Không tìm thấy đơn thuốc.", result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void rejectsDoctorMismatch() {
        prescription.setMaBacSi(DOCTOR_ID + 1);

        Result<ApprovePrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, DOCTOR_CONTEXT));

        assertFailure(result, "DOCTOR_MISMATCH");
        assertEquals("Chỉ bác sĩ kê đơn hoặc quản trị viên mới có quyền duyệt đơn.",
                result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsPrescriptionNotInTemporaryState() {
        prescription.setTrangThai(TrangThaiDonThuoc.DA_DUYET);

        Result<ApprovePrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, DOCTOR_CONTEXT));

        assertFailure(result, "INVALID_STATE");
        assertEquals("Chỉ có thể duyệt đơn thuốc đang ở trạng thái lưu tạm.",
                result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void rejectsPrescriptionWhenVisitEmrIsLocked() {
        visit.setEmrLocked(true);

        Result<ApprovePrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, DOCTOR_CONTEXT));

        assertFailure(result, "EMR_LOCKED");
        assertEquals("Hồ sơ bệnh án đã bị khóa không thể duyệt đơn thuốc.",
                result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void deniesUnauthorizedRoleAndAuditsForbiddenAccess() {
        AuthorizationContext receptionist =
                new AuthorizationContext(3, 10, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

        Result<ApprovePrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, receptionist));

        assertFailure(result, "AUTH_ACCESS_DENIED");
        assertEquals("Không được phép duyệt đơn thuốc trong phiên hiện tại.",
                result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
        assertTrue(transactionalAudits.isEmpty());
        assertEquals(1, independentAudits.size());
        SystemAuditEntry audit = independentAudits.get(0);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, audit.actionCode());
        assertEquals(AuditOutcome.DENIED, audit.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, audit.reasonCode());
        assertEquals(receptionist.employeeId(), audit.actorEmployeeId());
        assertEquals(receptionist.roleCode(), audit.roleCode());
    }

    private static ApprovePrescriptionCommand command(
            int prescriptionId, int doctorId, AuthorizationContext context) {
        return new ApprovePrescriptionCommand(
                prescriptionId,
                doctorId,
                context,
                new AuditActorContext(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        context.departmentId(),
                        "WS-SYN-01",
                        CORRELATION_ID));
    }

    private static void assertSuccessAudit(SystemAuditEntry audit) {
        assertEquals(AuditActionCode.RX_APPROVED, audit.actionCode());
        assertEquals(AuditResourceType.PRESCRIPTION, audit.resourceType());
        assertEquals(PRESCRIPTION_ID, audit.resourceId());
        assertEquals(AuditOutcome.SUCCESS, audit.outcome());
    }

    private static void assertFailure(Result<ApprovePrescriptionReceipt> result, String code) {
        assertFalse(result.isSuccess());
        assertEquals(code, result.getErrorCode().orElse(null));
    }

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
                    case "toString" -> type.getSimpleName() + " synthetic test proxy";
                    default -> null;
                };
            }
            return invocation.invoke(method.getName(), args);
        });
        return type.cast(instance);
    }
}
