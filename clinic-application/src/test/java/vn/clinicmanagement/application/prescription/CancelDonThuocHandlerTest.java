package vn.clinicmanagement.application.prescription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
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

class CancelDonThuocHandlerTest {
    private static final int PRESCRIPTION_ID = 901;
    private static final int VISIT_ID = 502;
    private static final int DOCTOR_ID = 21;
    private static final int FACILITY_ID = 10;
    private static final int DEPARTMENT_ID = 30;
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final PhanQuyenNguCanh DOCTOR_CONTEXT =
            new PhanQuyenNguCanh(2, 20, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    private static final PhanQuyenNguCanh ADMIN_CONTEXT =
            new PhanQuyenNguCanh(1, 1, "ADMIN", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

    private final List<HeThongKiemToanNhapLieu> transactionalAudits = new ArrayList<>();
    private final List<HeThongKiemToanNhapLieu> independentAudits = new ArrayList<>();
    private final AtomicInteger transactionCalls = new AtomicInteger();
    private DonThuoc prescription;
    private LuotKham visit;
    private DonThuoc updatedPrescription;
    private CancelDonThuocHandler handler;

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
        GiaoDichRanhGioi transactions = new GiaoDichRanhGioi() {
            @Override
            public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
                transactionCalls.incrementAndGet();
                return operation.get();
            }
        };
        KiemToanWriteCongKetNoi audit = new KiemToanWriteCongKetNoi() {
            @Override
            public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {
                transactionalAudits.add(entry);
            }

            @Override
            public void appendIndependently(HeThongKiemToanNhapLieu entry) {
                independentAudits.add(entry);
            }

            @Override
            public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {
            }

            @Override
            public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {
            }
        };
        handler = new CancelDonThuocHandler(prescriptionRepository, visitRepository, transactions, audit);
    }

    @Test
    void cancelsPrescriptionByPrescribingDoctorAndAuditsSuccess() {
        KetQua<CancelDonThuocBienNhan> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "  Change plan  ", DOCTOR_CONTEXT));

        assertTrue(result.isSuccess());
        CancelDonThuocBienNhan receipt = result.getValue().orElseThrow();
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

        KetQua<CancelDonThuocBienNhan> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", ADMIN_CONTEXT));

        assertTrue(result.isSuccess());
        assertEquals(TrangThaiDonThuoc.HUY, result.getValue().orElseThrow().trangThai());
        assertSame(prescription, updatedPrescription);
        assertEquals(1, transactionCalls.get());
        assertSuccessAudit(transactionalAudits.get(0));
    }

    @Test
    void rejectsNonpositivePrescriptionOrDoctorIds() {
        KetQua<CancelDonThuocBienNhan> invalidPrescription =
                handler.handle(command(0, DOCTOR_ID, "Change plan", DOCTOR_CONTEXT));
        KetQua<CancelDonThuocBienNhan> invalidDoctor =
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
            KetQua<CancelDonThuocBienNhan> result =
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

        KetQua<CancelDonThuocBienNhan> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", DOCTOR_CONTEXT));

        assertFailure(result, "NOT_FOUND");
        assertEquals("Không tìm thấy đơn thuốc.", result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void rejectsDoctorMismatch() {
        prescription.setMaBacSi(DOCTOR_ID + 1);

        KetQua<CancelDonThuocBienNhan> result =
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

        KetQua<CancelDonThuocBienNhan> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", DOCTOR_CONTEXT));

        assertFailure(result, "ALREADY_DISPENSED");
        assertEquals("Không thể hủy đơn thuốc đã xuất hết.", result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void rejectsPrescriptionAlreadyCancelled() {
        prescription.setTrangThai(TrangThaiDonThuoc.HUY);

        KetQua<CancelDonThuocBienNhan> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", DOCTOR_CONTEXT));

        assertFailure(result, "ALREADY_CANCELLED");
        assertEquals("Đơn thuốc đã bị hủy trước đó.", result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void rejectsPrescriptionWhenVisitEmrIsLocked() {
        visit.setEmrLocked(true);

        KetQua<CancelDonThuocBienNhan> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", DOCTOR_CONTEXT));

        assertFailure(result, "EMR_LOCKED");
        assertEquals("Hồ sơ bệnh án đã bị khóa không thể hủy đơn thuốc.",
                result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
    }

    @Test
    void deniesUnauthorizedRoleAndAuditsForbiddenAccess() {
        PhanQuyenNguCanh receptionist =
                new PhanQuyenNguCanh(3, 10, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

        KetQua<CancelDonThuocBienNhan> result =
                handler.handle(command(PRESCRIPTION_ID, DOCTOR_ID, "Change plan", receptionist));

        assertFailure(result, "AUTH_ACCESS_DENIED");
        assertEquals("Không được phép hủy đơn thuốc trong phiên hiện tại.",
                result.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
        assertTrue(transactionalAudits.isEmpty());
        assertEquals(1, independentAudits.size());
        HeThongKiemToanNhapLieu audit = independentAudits.get(0);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, audit.actionCode());
        assertEquals(KiemToanKetQua.DENIED, audit.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, audit.reasonCode());
        assertEquals(receptionist.employeeId(), audit.actorEmployeeId());
        assertEquals(receptionist.roleCode(), audit.roleCode());
    }

    private static CancelDonThuocCommand command(
            int prescriptionId, int doctorId, String reason, PhanQuyenNguCanh context) {
        return new CancelDonThuocCommand(
                prescriptionId,
                doctorId,
                reason,
                context,
                new KiemToanTacNhanNguCanh(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        context.departmentId(),
                        "WS-SYN-01",
                        CORRELATION_ID));
    }

    private static void assertSuccessAudit(HeThongKiemToanNhapLieu audit) {
        assertEquals(KiemToanHanhDongMa.RX_CANCELLED, audit.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.PRESCRIPTION, audit.resourceType());
        assertEquals(PRESCRIPTION_ID, audit.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, audit.outcome());
    }

    private static void assertFailure(KetQua<CancelDonThuocBienNhan> result, String code) {
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
