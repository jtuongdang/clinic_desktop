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
import vn.clinicmanagement.domain.examination.TransferLyDoMa;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferLuotKhamHandlerTest {
    private static final int FACILITY_ID = 10;
    private static final int DEPARTMENT_ID = 20;
    private static final int CURRENT_ROOM_ID = 101;
    private static final int TARGET_ROOM_ID = 102;
    private static final int VISIT_ID = 501;

    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final PhanQuyenNguCanh RECEPTIONIST_CONTEXT =
            new PhanQuyenNguCanh(1, 10, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

    private static final PhanQuyenNguCanh DOCTOR_CONTEXT =
            new PhanQuyenNguCanh(2, 20, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);

    private static final KiemToanTacNhanNguCanh AUDIT_ACTOR =
            new KiemToanTacNhanNguCanh(1, 10, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, "WS-01", CORRELATION_ID);

    private LuotKham visit;
    private PhongKham destinationRoom;
    private final AtomicInteger visitUpdates = new AtomicInteger();
    private final AtomicInteger transactionCalls = new AtomicInteger();
    private final List<HeThongKiemToanNhapLieu> transactionalAudits = new ArrayList<>();
    private final List<HeThongKiemToanNhapLieu> independentAudits = new ArrayList<>();

    private ILuotKhamRepository visitRepository;
    private IPhongKhamRepository roomRepository;
    private GiaoDichRanhGioi transactionBoundary;
    private KiemToanWriteCongKetNoi auditWritePort;
    private TransferLuotKhamHandler handler;

    @BeforeEach
    void setUp() {
        visitUpdates.set(0);
        transactionCalls.set(0);
        transactionalAudits.clear();
        independentAudits.clear();

        visit = createVisit(VISIT_ID, CURRENT_ROOM_ID, TrangThaiLuotKham.CHO_KHAM);
        destinationRoom = createRoom(TARGET_ROOM_ID, FACILITY_ID, DEPARTMENT_ID, true);

        visitRepository = proxy(ILuotKhamRepository.class, (method, args) -> {
            if ("getById".equals(method)) {
                int id = (int) args[0];
                if (visit != null && visit.getId() != null && visit.getId() == id) {
                    return Optional.of(visit);
                }
                return Optional.empty();
            }
            if ("update".equals(method)) {
                visitUpdates.incrementAndGet();
                return null;
            }
            return null;
        });

        roomRepository = proxy(IPhongKhamRepository.class, (method, args) -> {
            if ("getById".equals(method)) {
                int id = (int) args[0];
                if (destinationRoom != null && destinationRoom.getId() != null && destinationRoom.getId() == id) {
                    return Optional.of(destinationRoom);
                }
                return Optional.empty();
            }
            return null;
        });

        transactionBoundary = new GiaoDichRanhGioi() {
            @Override
            public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
                transactionCalls.incrementAndGet();
                return operation.get();
            }
        };

        auditWritePort = new KiemToanWriteCongKetNoi() {
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

        handler = new TransferLuotKhamHandler(visitRepository, roomRepository, transactionBoundary, auditWritePort);
    }

    @Test
    void successfulTransferByReceptionistWithTriageCorrection() {
        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, TransferLyDoMa.RECEPTION_TRIAGE_CORRECTION,
                "Dieu chinh sai phong", RECEPTIONIST_CONTEXT, AUDIT_ACTOR);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        TransferLuotKhamBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(VISIT_ID, receipt.visitId());
        assertEquals(CURRENT_ROOM_ID, receipt.previousRoomId());
        assertEquals(CURRENT_ROOM_ID, receipt.sourceRoomId());
        assertEquals(TARGET_ROOM_ID, receipt.destinationRoomId());
        assertEquals(TARGET_ROOM_ID, receipt.targetRoomId());
        assertEquals(TransferLyDoMa.RECEPTION_TRIAGE_CORRECTION, receipt.reasonCode());
        assertEquals("Dieu chinh sai phong", receipt.transferNote());

        assertEquals(TARGET_ROOM_ID, visit.getMaPhongKham());
        assertTrue(visit.getLyDoChuyenTuyen().contains("RECEPTION_TRIAGE_CORRECTION"));
        assertTrue(visit.getLyDoChuyenTuyen().contains("Dieu chinh sai phong"));
        assertEquals(1, visitUpdates.get());
        assertEquals(1, transactionCalls.get());
        assertEquals(1, transactionalAudits.size());

        HeThongKiemToanNhapLieu auditEntry = transactionalAudits.get(0);
        assertEquals(KiemToanHanhDongMa.VISIT_TRANSFERRED, auditEntry.actionCode());
        assertEquals(KiemToanKetQua.SUCCESS, auditEntry.outcome());
        assertEquals(KiemToanTaiNguyenLoai.VISIT, auditEntry.resourceType());
        assertEquals(TARGET_ROOM_ID, auditEntry.resourceId());
    }

    @Test
    void successfulTransferByDoctorWithSpecialtyReferral() {
        visit.setTrangThai(TrangThaiLuotKham.DANG_KHAM);
        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, TransferLyDoMa.CLINICAL_SPECIALTY_REFERRAL,
                "Chuyen kham chuyen khoa", DOCTOR_CONTEXT);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        TransferLuotKhamBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(TARGET_ROOM_ID, receipt.destinationRoomId());
        assertEquals(TransferLyDoMa.CLINICAL_SPECIALTY_REFERRAL, receipt.reasonCode());

        assertEquals(TARGET_ROOM_ID, visit.getMaPhongKham());
        assertEquals(1, visitUpdates.get());
        assertEquals(1, transactionCalls.get());
        assertEquals(1, transactionalAudits.size());

        HeThongKiemToanNhapLieu auditEntry = transactionalAudits.get(0);
        assertEquals(KiemToanHanhDongMa.VISIT_TRANSFERRED, auditEntry.actionCode());
        assertEquals(KiemToanKetQua.SUCCESS, auditEntry.outcome());
        assertEquals(TARGET_ROOM_ID, auditEntry.resourceId());
    }

    @Test
    void denialWhenReceptionistAttemptsClinicalSpecialtyReferral() {
        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, TransferLyDoMa.CLINICAL_SPECIALTY_REFERRAL,
                "Can kham chuyen khoa", RECEPTIONIST_CONTEXT, AUDIT_ACTOR);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElse(null));
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
        assertEquals(0, transactionalAudits.size());

        assertEquals(1, independentAudits.size());
        HeThongKiemToanNhapLieu deniedAudit = independentAudits.get(0);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, deniedAudit.actionCode());
        assertEquals(KiemToanKetQua.DENIED, deniedAudit.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, deniedAudit.reasonCode());
        assertEquals("RECEPTIONIST", deniedAudit.roleCode());
    }

    @Test
    void denialWhenDestinationRoomIsInactive() {
        destinationRoom.setHoatDong(false);

        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, TransferLyDoMa.RECEPTION_TRIAGE_CORRECTION,
                null, RECEPTIONIST_CONTEXT);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("ROOM_UNAVAILABLE", result.getErrorCode().orElse(null));
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void denialWhenDestinationRoomIsAcrossFacilities() {
        PhongKham otherFacilityRoom = createRoom(TARGET_ROOM_ID, FACILITY_ID + 999, DEPARTMENT_ID, true);
        destinationRoom = otherFacilityRoom;

        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, TransferLyDoMa.RECEPTION_TRIAGE_CORRECTION,
                null, RECEPTIONIST_CONTEXT);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("ROOM_UNAVAILABLE", result.getErrorCode().orElse(null));
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void denialWhenDestinationRoomNotFound() {
        destinationRoom = null;

        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, TransferLyDoMa.RECEPTION_TRIAGE_CORRECTION,
                null, RECEPTIONIST_CONTEXT);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("ROOM_UNAVAILABLE", result.getErrorCode().orElse(null));
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void denialWhenDestinationRoomIsIdenticalToCurrentRoom() {
        destinationRoom = createRoom(CURRENT_ROOM_ID, FACILITY_ID, DEPARTMENT_ID, true);

        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, CURRENT_ROOM_ID, TransferLyDoMa.RECEPTION_TRIAGE_CORRECTION,
                null, RECEPTIONIST_CONTEXT);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("SAME_ROOM", result.getErrorCode().orElse(null));
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void denialWhenVisitIsCompleted() {
        visit.setTrangThai(TrangThaiLuotKham.HOAN_THANH);

        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, TransferLyDoMa.RECEPTION_TRIAGE_CORRECTION,
                null, RECEPTIONIST_CONTEXT);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("INVALID_STATE", result.getErrorCode().orElse(null));
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void denialWhenVisitIsCancelled() {
        visit.setTrangThai(TrangThaiLuotKham.DA_HUY);

        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, TransferLyDoMa.RECEPTION_TRIAGE_CORRECTION,
                null, RECEPTIONIST_CONTEXT);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("INVALID_STATE", result.getErrorCode().orElse(null));
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void verificationThatVisitTransferredIsRecordedWithDestinationRoomId() {
        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, TransferLyDoMa.RECEPTION_TRIAGE_CORRECTION,
                "Transfer note", RECEPTIONIST_CONTEXT, AUDIT_ACTOR);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        assertEquals(1, transactionalAudits.size());

        HeThongKiemToanNhapLieu auditEntry = transactionalAudits.get(0);
        assertEquals(KiemToanHanhDongMa.VISIT_TRANSFERRED, auditEntry.actionCode());
        assertEquals(KiemToanKetQua.SUCCESS, auditEntry.outcome());
        assertEquals(KiemToanTaiNguyenLoai.VISIT, auditEntry.resourceType());
        assertEquals(TARGET_ROOM_ID, auditEntry.resourceId(), "resourceId must record destination roomId");
        assertEquals(FACILITY_ID, auditEntry.facilityId());
        assertEquals(DEPARTMENT_ID, auditEntry.departmentId());
        assertEquals("WS-01", auditEntry.workstationId());
        assertEquals(CORRELATION_ID, auditEntry.correlationId());
    }

    @Test
    void denialWhenVisitNotFound() {
        visit = null;

        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, TransferLyDoMa.RECEPTION_TRIAGE_CORRECTION,
                null, RECEPTIONIST_CONTEXT);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("NOT_FOUND", result.getErrorCode().orElse(null));
        assertEquals(0, transactionCalls.get());
        assertEquals(0, visitUpdates.get());
    }

    @Test
    void existingReasonIsAppendedCorrectly() {
        visit.setLyDoChuyenTuyen("Ly do cu");

        TransferLuotKhamCommand command = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, TransferLyDoMa.RECEPTION_TRIAGE_CORRECTION,
                "Ghi chu moi", RECEPTIONIST_CONTEXT);

        KetQua<TransferLuotKhamBienNhan> result = handler.handle(command);

        assertTrue(result.isSuccess());
        assertEquals("Ly do cu; RECEPTION_TRIAGE_CORRECTION: Ghi chu moi", visit.getLyDoChuyenTuyen());
    }

    @Test
    void nullCommandOrNullReasonCodeIsRejected() {
        KetQua<TransferLuotKhamBienNhan> nullCmdResult = handler.handle(null);
        assertTrue(nullCmdResult.isFailure());
        assertEquals("COMMAND_INVALID", nullCmdResult.getErrorCode().orElse(null));

        TransferLuotKhamCommand nullReasonCmd = new TransferLuotKhamCommand(
                VISIT_ID, TARGET_ROOM_ID, null, null, RECEPTIONIST_CONTEXT);
        KetQua<TransferLuotKhamBienNhan> nullReasonResult = handler.handle(nullReasonCmd);
        assertTrue(nullReasonResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", nullReasonResult.getErrorCode().orElse(null));
    }

    private static PhongKham createRoom(int roomId, int facilityId, int departmentId, boolean active) {
        CoSo coSo = new CoSo();
        coSo.setId(facilityId);

        Khoa khoa = new Khoa();
        khoa.setId(departmentId);
        khoa.setMaCoSo(facilityId);
        khoa.setCoSo(coSo);

        PhongKham room = new PhongKham();
        room.setId(roomId);
        room.setMaKhoa(departmentId);
        room.setKhoa(khoa);
        room.setHoatDong(active);
        return room;
    }

    private static LuotKham createVisit(int visitId, int roomId, TrangThaiLuotKham state) {
        LuotKham v = new LuotKham();
        v.setId(visitId);
        v.setMaPhongKham(roomId);
        v.setTrangThai(state);
        v.setLyDoChuyenTuyen("");
        return v;
    }

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
