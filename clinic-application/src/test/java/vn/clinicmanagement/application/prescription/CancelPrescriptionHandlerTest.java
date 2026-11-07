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

class CancelPrescriptionHandlerTest {
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
    private CancelPrescriptionHandler handler;

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
        handler = new CancelPrescriptionHandler(prescriptionRepository, visitRepository, transactions, audit);
    }

    @Test
    void cancelsPrescriptionByPrescribingDoctorAndAuditsSuccess() {
        Result<CancelPrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "  Change plan  ", DOCTOR_CONTEXT));

        assertTrue(result.isSuccess());
        CancelPrescriptionReceipt receipt = result.getValue().orElseThrow();
        assertEquals(PRESCRIPTION_ID, receipt.prescriptionId());
        assertEquals(VISIT_ID, receipt.visitId());
        assertEquals("Change plan", receipt.lyDoHuy());
        assertEquals(TrangThaiDonThuoc.HUY, receipt.trangThai());
        assertNotNull(receipt.cancelledAt());
        assertSame(prescription, updatedPrescription);
        assertEquals(1, transactionCalls.get());
        assertEquals(1, transactionalAudits.size());
        assertTrue(independentAudits.isEmpty());
        assertSuccessAudit(transactionalAudits.get(0));
    }

    @Test
    void cancelsPrescriptionByAdminEvenWhenDoctorDoesNotMatch() {
        prescription.setMaBacSi(DOCTOR_ID + 1);

        Result<CancelPrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", ADMIN_CONTEXT));

        assertTrue(result.isSuccess());
        assertEquals(TrangThaiDonThuoc.HUY, result.getValue().orElseThrow().trangThai());
        assertSame(prescription, updatedPrescription);
        assertEquals(1, transactionCalls.get());
        assertSuccessAudit(transactionalAudits.get(0));
    }

    @Test
    void rejectsNonpositivePrescriptionOrDoctorIds() {
        Result<CancelPrescriptionReceipt> invalidPrescription =
                handler.handle(command(0, DOCTOR_ID, "Change plan", DOCTOR_CONTEXT));
        Result<CancelPrescriptionReceipt> invalidDoctor =
                handler.handle(command(PRESCRIPTION_ID, -1, "Change plan", DOCTOR_CONTEXT));

        assertFailure(invalidPrescription, "VALIDATION_ERROR");
        assertFailure(invalidDoctor, "VALIDATION_ERROR");
        assertEquals("Thông tin hủy đơn thuốc không hợp lệ.",
                invalidPrescription.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
        assertTrue(transactionalAudits.isEmpty());
        assertTrue(independentAudits.isEmpty());
    }

    @Test
    void rejectsNullEmptyBlankAndShortCancellationReasons() {
        for (String reason : new String[]{null, "", "   ", "abcd"}) {
            Result<CancelPrescriptionReceipt> result =
                    handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, reason, DOCTOR_CONTEXT));

            assertFailure(result, "VALIDATION_ERROR");
            assertEquals("Lý do hủy đơn thuốc phải có ít nhất 5 ký tự.",
                    result.getError().orElseThrow().message());
        }
        assertEquals(0, transactionCalls.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsMissingPrescription() {
        prescription = null;

        Result<CancelPrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", DOCTOR_CONTEXT));

        assertFailure(result, "NOT_FOUND");
        assertEquals("Không tìm thấy đơn thuốc.", result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void rejectsDoctorMismatch() {
        prescription.setMaBacSi(DOCTOR_ID + 1);

        Result<CancelPrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", DOCTOR_CONTEXT));

        assertFailure(result, "DOCTOR_MISMATCH");
        assertEquals("Chỉ bác sĩ kê đơn hoặc quản trị viên mới có quyền hủy đơn.",
                result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsPrescriptionAlreadyDispensed() {
        prescription.setTrangThai(TrangThaiDonThuoc.DA_XUAT_HET);

        Result<CancelPrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", DOCTOR_CONTEXT));

        assertFailure(result, "ALREADY_DISPENSED");
        assertEquals("Không thể hủy đơn thuốc đã xuất hết.", result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void rejectsPrescriptionAlreadyCancelled() {
        prescription.setTrangThai(TrangThaiDonThuoc.HUY);

        Result<CancelPrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", DOCTOR_CONTEXT));

        assertFailure(result, "ALREADY_CANCELLED");
        assertEquals("Đơn thuốc đã bị hủy trước đó.", result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void rejectsPrescriptionWhenVisitEmrIsLocked() {
        visit.setEmrLocked(true);

        Result<CancelPrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", DOCTOR_CONTEXT));

        assertFailure(result, "EMR_LOCKED");
        assertEquals("Hồ sơ bệnh án đã bị khóa không thể hủy đơn thuốc.",
                result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void deniesUnauthorizedRoleAndAuditsForbiddenAccess() {
        AuthorizationContext receptionist =
                new AuthorizationContext(3, 10, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

        Result<CancelPrescriptionReceipt> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", receptionist));

        assertFailure(result, "AUTH_ACCESS_DENIED");
        assertEquals("Không được phép hủy đơn thuốc trong phiên hiện tại.",
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

    private static CancelPrescriptionCommand command(
            int prescriptionId, int doctorId, String reason, AuthorizationContext context) {
        return new CancelPrescriptionCommand(
                prescriptionId,
                doctorId,
                reason,
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
        assertEquals(AuditActionCode.RX_CANCELLED, audit.actionCode());
        assertEquals(AuditResourceType.PRESCRIPTION, audit.resourceType());
        assertEquals(PRESCRIPTION_ID, audit.resourceId());
        assertEquals(AuditOutcome.SUCCESS, audit.outcome());
    }

    private static void assertFailure(Result<CancelPrescriptionReceipt> result, String code) {
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
