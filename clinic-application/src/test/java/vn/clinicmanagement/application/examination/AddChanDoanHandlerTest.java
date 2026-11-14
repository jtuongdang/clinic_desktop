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

class AddChanDoanHandlerTest {
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
    private AddChanDoanHandler handler;

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
        handler = new AddChanDoanHandler(visitRepository, transactionBoundary, audit);
    }

    @Test
    void addsPrimaryDiagnosisAndDemotesPreviousPrimary() {
        ChanDoan previousPrimary = diagnosis("Chẩn đoán chính cũ", true);
        visit.getChanDoans().add(previousPrimary);

        KetQua<AddChanDoanBienNhan> result = handler.handle(new AddChanDoanCommand(
                VISIT_ID, DOCTOR_ID, 700, "  Viêm họng cấp  ", true, null,
                DOCTOR_CONTEXT, actor(DOCTOR_CONTEXT)));

        assertTrue(result.isSuccess());
        AddChanDoanBienNhan receipt = result.getValue().orElseThrow();
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

        HeThongKiemToanNhapLieu audit = transactionalAudits.get(0);
        assertEquals(KiemToanHanhDongMa.EMR_UPDATED, audit.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.VISIT, audit.resourceType());
        assertEquals(VISIT_ID, audit.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, audit.outcome());
    }

    @Test
    void addsSecondaryDiagnosisWithoutDemotingPrimary() {
        ChanDoan existingPrimary = diagnosis("Chẩn đoán chính", true);
        visit.getChanDoans().add(existingPrimary);

        KetQua<AddChanDoanBienNhan> result = handler.handle(new AddChanDoanCommand(
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

        KetQua<AddChanDoanBienNhan> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "NOT_FOUND");
        assertEquals("Không tìm thấy lượt khám.", result.getError().orElseThrow().message());
        assertEquals(1, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsBlankDiagnosisTextAndInvalidIdentifiers() {
        KetQua<AddChanDoanBienNhan> blank = handler.handle(new AddChanDoanCommand(
                VISIT_ID, DOCTOR_ID, "   ", false, DOCTOR_CONTEXT));
        KetQua<AddChanDoanBienNhan> invalidVisit = handler.handle(new AddChanDoanCommand(
                0, DOCTOR_ID, "Đau đầu", false, DOCTOR_CONTEXT));
        KetQua<AddChanDoanBienNhan> invalidDoctor = handler.handle(new AddChanDoanCommand(
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
        KetQua<AddChanDoanBienNhan> result = handler.handle(new AddChanDoanCommand(
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

        KetQua<AddChanDoanBienNhan> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "DOCTOR_MISMATCH");
        assertEquals("Chỉ bác sĩ phụ trách mới có quyền thêm chẩn đoán.",
                result.getError().orElseThrow().message());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsLockedEmr() {
        visit.lockEmr("synthetic-signature", DOCTOR_ID);

        KetQua<AddChanDoanBienNhan> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "EMR_LOCKED");
        assertEquals("Hồ sơ bệnh án đã bị khóa không thể thêm chẩn đoán.",
                result.getError().orElseThrow().message());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void rejectsVisitNotInProgress() {
        visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);

        KetQua<AddChanDoanBienNhan> result = handler.handle(command(DOCTOR_CONTEXT));

        assertFailure(result, "INVALID_STATE");
        assertEquals("Chỉ có thể thêm chẩn đoán khi lượt khám đang diễn ra.",
                result.getError().orElseThrow().message());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
    }

    @Test
    void deniesUnauthorizedCallerAndWritesForbiddenAudit() {
        PhanQuyenNguCanh receptionist = new PhanQuyenNguCanh(
                1, 10, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

        KetQua<AddChanDoanBienNhan> result = handler.handle(command(receptionist));

        assertFailure(result, "AUTHORIZATION_DENIED");
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitReads.get());
        assertEquals(0, visitUpdates.get());
        assertTrue(transactionalAudits.isEmpty());
        assertEquals(1, independentAudits.size());

        HeThongKiemToanNhapLieu audit = independentAudits.get(0);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, audit.actionCode());
        assertEquals(KiemToanKetQua.DENIED, audit.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, audit.reasonCode());
        assertEquals("RECEPTIONIST", audit.roleCode());
        assertNull(audit.resourceType());
        assertNull(audit.resourceId());
    }

    @Test
    void deniesMissingSessionAndWritesAnonymousForbiddenAudit() {
        KetQua<AddChanDoanBienNhan> result = handler.handle(new AddChanDoanCommand(
                VISIT_ID, DOCTOR_ID, null, "Đau đầu", false, "XAC_DINH", null, null));

        assertFailure(result, "AUTHORIZATION_DENIED");
        assertEquals(1, independentAudits.size());
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, independentAudits.get(0).actionCode());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, independentAudits.get(0).reasonCode());
        assertNull(independentAudits.get(0).actorEmployeeId());
    }

    private void assertFailure(KetQua<AddChanDoanBienNhan> result, String code) {
        assertFalse(result.isSuccess());
        assertEquals(code, result.getErrorCode().orElse(null));
    }

    private static AddChanDoanCommand command(PhanQuyenNguCanh context) {
        return new AddChanDoanCommand(
                VISIT_ID, DOCTOR_ID, null, "Đau đầu", false, "XAC_DINH", context, actor(context));
    }

    private static KiemToanTacNhanNguCanh actor(PhanQuyenNguCanh context) {
        return context != null && context.isValid()
                ? new KiemToanTacNhanNguCanh(
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

    private static final PhanQuyenNguCanh DOCTOR_CONTEXT =
            new PhanQuyenNguCanh(2, 20, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

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
