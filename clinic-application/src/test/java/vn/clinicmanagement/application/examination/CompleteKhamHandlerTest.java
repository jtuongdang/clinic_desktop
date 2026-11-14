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

class CompleteKhamHandlerTest {
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
    private final List<HeThongKiemToanNhapLieu> transactionalAudits = new ArrayList<>();
    private final List<HeThongKiemToanNhapLieu> independentAudits = new ArrayList<>();
    private ILuotKhamRepository visitRepository;
    private CompleteKhamHandler handler;

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
            public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {
            }

            @Override
            public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {
            }
        };
        handler = new CompleteKhamHandler(visitRepository, transactionBoundary, audit);
    }

    @Test
    void completesExamAndAuditsEmrCompletion() {
        LocalDate followUp = LocalDate.of(2026, 12, 1);
        CompleteKhamCommand command = new CompleteKhamCommand(
                VISIT_ID, DOCTOR_ID, KetQuaDieuTri.DO_GIAM, HuongXuTri.HEN_TAI_KHAM,
                "Tái khám theo lịch.", followUp, DOCTOR_CONTEXT, actor(DOCTOR_CONTEXT));

        KetQua<CompleteKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        CompleteKhamBienNhan receipt = result.getValue().orElseThrow();
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

        HeThongKiemToanNhapLieu entry = transactionalAudits.get(0);
        assertEquals(KiemToanHanhDongMa.EMR_COMPLETED, entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.VISIT, entry.resourceType());
        assertEquals(VISIT_ID, entry.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
        assertEquals("DOCTOR", entry.roleCode());
    }

    @Test
    void rejectsMissingVisit() {
        visit = null;

        KetQua<CompleteKhamBienNhan> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "NOT_FOUND");
        assertEquals("Không tìm thấy lượt khám.", result.getError().orElseThrow().message());
        assertEquals(1, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsVisitNotInProgress() {
        visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);

        KetQua<CompleteKhamBienNhan> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "INVALID_STATE");
        assertEquals("Lượt khám không ở trạng thái đang khám.", result.getError().orElseThrow().message());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsEmrLockedVisit() {
        visit.lockEmr("synthetic-signature", DOCTOR_ID);

        KetQua<CompleteKhamBienNhan> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "INVALID_STATE");
        assertTrue(result.getError().orElseThrow().message().contains("đã được ký số và khóa an toàn"));
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsDoctorMismatch() {
        visit.setMaBacSi(DOCTOR_ID + 1);

        KetQua<CompleteKhamBienNhan> result = handler.handle(command(DOCTOR_CONTEXT));

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
        PhanQuyenNguCanh otherRole = new PhanQuyenNguCanh(
                3, 30, "NURSE", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

        assertDenial(command(otherRole), "NURSE");
    }

    @Test
    void deniesMissingSessionAndWritesAnonymousForbiddenAudit() {
        assertDenial(new CompleteKhamCommand(
                VISIT_ID, DOCTOR_ID, null, null, null, null, null, null), null);
    }

    @Test
    void rejectsInvalidIdentifiersBeforeAuthorizationOrTransaction() {
        KetQua<CompleteKhamBienNhan> invalidVisit = handler.handle(
                new CompleteKhamCommand(0, DOCTOR_ID, DOCTOR_CONTEXT));
        KetQua<CompleteKhamBienNhan> invalidDoctor = handler.handle(
                new CompleteKhamCommand(VISIT_ID, 0, DOCTOR_CONTEXT));

        assertFailure(invalidVisit, "VALIDATION_ERROR");
        assertFailure(invalidDoctor, "VALIDATION_ERROR");
        assertEquals("Thông tin kết thúc khám không hợp lệ.", invalidVisit.getError().orElseThrow().message());
        assertEquals("Thông tin kết thúc khám không hợp lệ.", invalidDoctor.getError().orElseThrow().message());
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitReads.get());
    }

    private void assertDenial(CompleteKhamCommand command, String roleCode) {
        KetQua<CompleteKhamBienNhan> result = handler.handle(command);

        assertFailure(result, "AUTHORIZATION_DENIED");
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitReads.get());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
        assertEquals(1, independentAudits.size());

        HeThongKiemToanNhapLieu entry = independentAudits.get(0);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, entry.actionCode());
        assertEquals(KiemToanKetQua.DENIED, entry.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, entry.reasonCode());
        assertEquals(roleCode, entry.roleCode());
        if (roleCode == null) {
            assertNull(entry.actorEmployeeId());
        }
        assertNull(entry.resourceType());
        assertNull(entry.resourceId());
    }

    private static void assertFailure(KetQua<CompleteKhamBienNhan> result, String code) {
        assertFalse(result.isSuccess());
        assertEquals(code, result.getErrorCode().orElse(null));
    }

    private static CompleteKhamCommand command(PhanQuyenNguCanh context) {
        return new CompleteKhamCommand(
                VISIT_ID, DOCTOR_ID, null, null, null, null, context, actor(context));
    }

    private static KiemToanTacNhanNguCanh actor(PhanQuyenNguCanh context) {
        return new KiemToanTacNhanNguCanh(
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

    private static final PhanQuyenNguCanh DOCTOR_CONTEXT =
            new PhanQuyenNguCanh(2, 20, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    private static final PhanQuyenNguCanh RECEPTIONIST_CONTEXT =
            new PhanQuyenNguCanh(1, 10, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

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
