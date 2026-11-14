package vn.clinicmanagement.application.examination;

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

class UpdateEMRLamSangRecordHandlerTest {
    private static final int VISIT_ID = 501;
    private static final int DOCTOR_ID = 20;
    private static final int OTHER_DOCTOR_ID = 99;
    private static final int FACILITY_ID = 10;
    private static final int DEPARTMENT_ID = 30;
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final PhanQuyenNguCanh DOCTOR_CONTEXT =
            new PhanQuyenNguCanh(2, DOCTOR_ID, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    private static final PhanQuyenNguCanh RECEPTIONIST_CONTEXT =
            new PhanQuyenNguCanh(1, 33, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

    private LuotKham visit;
    private final AtomicInteger visitReads = new AtomicInteger();
    private final AtomicInteger visitUpdates = new AtomicInteger();
    private final AtomicInteger transactionCalls = new AtomicInteger();
    private final List<HeThongKiemToanNhapLieu> transactionalAudits = new ArrayList<>();
    private final List<HeThongKiemToanNhapLieu> independentAudits = new ArrayList<>();
    private ILuotKhamRepository visitRepository;
    private UpdateEMRLamSangRecordHandler handler;

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

        GiaoDichRanhGioi transactionBoundary = new GiaoDichRanhGioi() {
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
            public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}

            @Override
            public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
        };

        handler = new UpdateEMRLamSangRecordHandler(visitRepository, transactionBoundary, audit);
    }

    @Test
    void handle_shouldSucceed_whenDoctorUpdatesActiveEmr() {
        UpdateEMRLamSangRecordCommand command = new UpdateEMRLamSangRecordCommand(
                VISIT_ID,
                DOCTOR_ID,
                "Ho khan 3 ngay",
                "Khoe manh",
                "Khong ghi nhan",
                "The trang tot",
                "Hong do nhe, phoi trong",
                "Viem hong cap ngay thu 3",
                "Ke don thuoc ngoai tru",
                "Uong nhieu nuoc am",
                DOCTOR_CONTEXT,
                actorContext(DOCTOR_CONTEXT)
        );

        KetQua<UpdateEMRLamSangRecordBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        UpdateEMRLamSangRecordBienNhan receipt = result.getValue().orElseThrow();
        assertNotNull(receipt);
        assertEquals(VISIT_ID, receipt.visitId());
        assertFalse(receipt.emrLocked());
        assertNotNull(receipt.updatedAt());

        assertEquals("Ho khan 3 ngay", visit.getQuaTrinhBenhLy());
        assertEquals("Khoe manh", visit.getTienSuBanThan());
        assertEquals("Khong ghi nhan", visit.getTienSuGiaDinh());
        assertEquals("The trang tot", visit.getKhamToanThan());
        assertEquals("Hong do nhe, phoi trong", visit.getKhamBoPhan());
        assertEquals("Viem hong cap ngay thu 3", visit.getTomTatBenhAn());
        assertEquals("Ke don thuoc ngoai tru", visit.getHuongDieuTri());
        assertEquals("Uong nhieu nuoc am", visit.getLoiDanBacSi());

        assertEquals(1, visitUpdates.get());
        assertEquals(1, transactionCalls.get());
        assertEquals(1, transactionalAudits.size());

        HeThongKiemToanNhapLieu auditEntry = transactionalAudits.get(0);
        assertEquals(KiemToanHanhDongMa.EMR_UPDATED, auditEntry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.VISIT, auditEntry.resourceType());
        assertEquals(VISIT_ID, auditEntry.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, auditEntry.outcome());
    }

    @Test
    void handle_shouldFail_whenValidationFails() {
        UpdateEMRLamSangRecordCommand command = new UpdateEMRLamSangRecordCommand(
                0,
                DOCTOR_ID,
                "Test", null, null, null, null, null, null, null,
                DOCTOR_CONTEXT,
                actorContext(DOCTOR_CONTEXT)
        );

        KetQua<UpdateEMRLamSangRecordBienNhan> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("VALIDATION_ERROR", result.getErrorCode().orElse(""));
        assertEquals(0, visitReads.get());
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void handle_shouldDeny_whenCallerIsReceptionist() {
        UpdateEMRLamSangRecordCommand command = new UpdateEMRLamSangRecordCommand(
                VISIT_ID,
                DOCTOR_ID,
                "Test", null, null, null, null, null, null, null,
                RECEPTIONIST_CONTEXT,
                actorContext(RECEPTIONIST_CONTEXT)
        );

        KetQua<UpdateEMRLamSangRecordBienNhan> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElse(""));
        assertEquals(0, transactionCalls.get());
        assertEquals(1, independentAudits.size());

        HeThongKiemToanNhapLieu auditEntry = independentAudits.get(0);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, auditEntry.actionCode());
        assertEquals(KiemToanKetQua.DENIED, auditEntry.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, auditEntry.reasonCode());
    }

    @Test
    void handle_shouldDeny_whenSessionContextNull() {
        UpdateEMRLamSangRecordCommand command = new UpdateEMRLamSangRecordCommand(
                VISIT_ID,
                DOCTOR_ID,
                "Test", null, null, null, null, null, null, null,
                null,
                actorContext(DOCTOR_CONTEXT)
        );

        KetQua<UpdateEMRLamSangRecordBienNhan> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElse(""));
        assertEquals(1, independentAudits.size());
    }

    @Test
    void handle_shouldFail_whenVisitNotFound() {
        UpdateEMRLamSangRecordCommand command = new UpdateEMRLamSangRecordCommand(
                9999,
                DOCTOR_ID,
                "Test", null, null, null, null, null, null, null,
                DOCTOR_CONTEXT,
                actorContext(DOCTOR_CONTEXT)
        );

        KetQua<UpdateEMRLamSangRecordBienNhan> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("NOT_FOUND", result.getErrorCode().orElse(""));
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void handle_shouldFail_whenDoctorMismatch() {
        UpdateEMRLamSangRecordCommand command = new UpdateEMRLamSangRecordCommand(
                VISIT_ID,
                OTHER_DOCTOR_ID,
                "Test", null, null, null, null, null, null, null,
                DOCTOR_CONTEXT,
                actorContext(DOCTOR_CONTEXT)
        );

        KetQua<UpdateEMRLamSangRecordBienNhan> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("DOCTOR_MISMATCH", result.getErrorCode().orElse(""));
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void handle_shouldFail_whenEmrAlreadyLocked() {
        visit.completeExam();
        visit.lockEmr("SIG-123", DOCTOR_ID);
        UpdateEMRLamSangRecordCommand command = new UpdateEMRLamSangRecordCommand(
                VISIT_ID,
                DOCTOR_ID,
                "Test", null, null, null, null, null, null, null,
                DOCTOR_CONTEXT,
                actorContext(DOCTOR_CONTEXT)
        );

        KetQua<UpdateEMRLamSangRecordBienNhan> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("EMR_ALREADY_LOCKED", result.getErrorCode().orElse(""));
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void handle_shouldFail_whenVisitNotDangKham() {
        visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);
        UpdateEMRLamSangRecordCommand command = new UpdateEMRLamSangRecordCommand(
                VISIT_ID,
                DOCTOR_ID,
                "Test", null, null, null, null, null, null, null,
                DOCTOR_CONTEXT,
                actorContext(DOCTOR_CONTEXT)
        );

        KetQua<UpdateEMRLamSangRecordBienNhan> result = handler.handle(command);

        assertFalse(result.isSuccess());
        assertEquals("INVALID_STATE", result.getErrorCode().orElse(""));
        assertEquals(0, visitUpdates.get());
    }

    private static LuotKham createVisit(TrangThaiLuotKham state) {
        LuotKham v = new LuotKham();
        v.setId(VISIT_ID);
        v.setMaBenhNhan(101);
        v.setMaCoSo(FACILITY_ID);
        v.setMaKhoa(DEPARTMENT_ID);
        v.setTrangThai(state);
        return v;
    }

    private static KiemToanTacNhanNguCanh actorContext(PhanQuyenNguCanh context) {
        return new KiemToanTacNhanNguCanh(
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
