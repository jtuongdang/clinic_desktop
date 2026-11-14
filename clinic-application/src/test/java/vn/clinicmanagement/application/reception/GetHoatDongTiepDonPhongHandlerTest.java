package vn.clinicmanagement.application.reception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GetHoatDongTiepDonPhongHandlerTest {
    private static final int EMPLOYEE_ID = 42;
    private static final int ROLE_ID = 5;
    private static final String ROLE_CODE = "RECEPTIONIST";
    private static final int FACILITY_ID = 10;
    private static final int DEPARTMENT_ID = 20;
    private static final String WORKSTATION_ID = "ws-reception-01";
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private FakePhongKhamRepository roomsRepository;
    private FakeAuditWritePort auditPort;
    private AuthorizationFixture authFixture;
    private GetHoatDongTiepDonPhongHandler handler;

    @BeforeEach
    void setUp() {
        roomsRepository = new FakePhongKhamRepository();
        auditPort = new FakeAuditWritePort();
        authFixture = new AuthorizationFixture();
        handler = new GetHoatDongTiepDonPhongHandler(roomsRepository, authFixture.authorizationService, auditPort);
    }

    @Test
    void handleNullQueryReturnsRoomCatalogFailedWithoutInvokingDependencies() {
        KetQua<List<PhongKham>> result = handler.handle(null);

        assertTrue(result.isFailure());
        assertFalse(result.isSuccess());
        assertTrue(result.getError().isPresent());
        assertEquals("ROOM_CATALOG_FAILED", result.getError().get().code());
        assertEquals("Không thể tải danh sách phòng khám.", result.getError().get().message());

        assertEquals(0, roomsRepository.callCount);
        assertTrue(auditPort.independentEntries.isEmpty());
        assertTrue(auditPort.currentTransactionEntries.isEmpty());
    }

    @Test
    void handleMismatchedActorContextRecordsDeniedAuditAndReturnsAuthorizationDenied() {
        PhanQuyenNguCanh authContext = new PhanQuyenNguCanh(
                999, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
        KiemToanTacNhanNguCanh actorContext = new KiemToanTacNhanNguCanh(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, WORKSTATION_ID, CORRELATION_ID);
        GetHoatDongTiepDonPhongQuery query = new GetHoatDongTiepDonPhongQuery(authContext, actorContext);

        KetQua<List<PhongKham>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
        assertEquals("Không được phép xem danh mục phòng trong phiên hiện tại.", result.getError().get().message());

        assertEquals(0, roomsRepository.callCount);
        assertEquals(1, auditPort.independentEntries.size());

        HeThongKiemToanNhapLieu auditEntry = auditPort.independentEntries.get(0);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, auditEntry.actionCode());
        assertEquals(KiemToanKetQua.DENIED, auditEntry.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, auditEntry.reasonCode());
        assertEquals(EMPLOYEE_ID, auditEntry.actorEmployeeId());
        assertEquals(ROLE_CODE, auditEntry.roleCode());
        assertEquals(FACILITY_ID, auditEntry.facilityId());
        assertEquals(DEPARTMENT_ID, auditEntry.departmentId());
        assertEquals(CORRELATION_ID, auditEntry.correlationId());
        assertEquals(WORKSTATION_ID, auditEntry.workstationId());
    }

    @Test
    void handleMismatchedRoleCodeRecordsDeniedAuditAndReturnsAuthorizationDenied() {
        PhanQuyenNguCanh authContext = new PhanQuyenNguCanh(
                EMPLOYEE_ID, ROLE_ID, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
        KiemToanTacNhanNguCanh actorContext = new KiemToanTacNhanNguCanh(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, WORKSTATION_ID, CORRELATION_ID);
        GetHoatDongTiepDonPhongQuery query = new GetHoatDongTiepDonPhongQuery(authContext, actorContext);

        KetQua<List<PhongKham>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, roomsRepository.callCount);
        assertEquals(1, auditPort.independentEntries.size());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, auditPort.independentEntries.get(0).reasonCode());
    }

    @Test
    void handleMismatchedFacilityRecordsDeniedAuditAndReturnsAuthorizationDenied() {
        PhanQuyenNguCanh authContext = new PhanQuyenNguCanh(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, 999, DEPARTMENT_ID, SESSION_ID);
        KiemToanTacNhanNguCanh actorContext = new KiemToanTacNhanNguCanh(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, WORKSTATION_ID, CORRELATION_ID);
        GetHoatDongTiepDonPhongQuery query = new GetHoatDongTiepDonPhongQuery(authContext, actorContext);

        KetQua<List<PhongKham>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, roomsRepository.callCount);
        assertEquals(1, auditPort.independentEntries.size());
    }

    @Test
    void handleMismatchedDepartmentRecordsDeniedAuditAndReturnsAuthorizationDenied() {
        PhanQuyenNguCanh authContext = new PhanQuyenNguCanh(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, 999, SESSION_ID);
        KiemToanTacNhanNguCanh actorContext = new KiemToanTacNhanNguCanh(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, WORKSTATION_ID, CORRELATION_ID);
        GetHoatDongTiepDonPhongQuery query = new GetHoatDongTiepDonPhongQuery(authContext, actorContext);

        KetQua<List<PhongKham>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, roomsRepository.callCount);
        assertEquals(1, auditPort.independentEntries.size());
    }

    @Test
    void handleUnauthorizedDecisionRecordsDeniedAuditAndReturnsAuthorizationDenied() {
        authFixture.permissionGranted = false;
        GetHoatDongTiepDonPhongQuery query = validQuery();

        KetQua<List<PhongKham>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, roomsRepository.callCount);
        assertEquals(1, auditPort.independentEntries.size());

        HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, entry.actionCode());
        assertEquals(KiemToanKetQua.DENIED, entry.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, entry.reasonCode());
    }

    @Test
    void handleScopeMismatchDecisionRecordsScopeMismatchReasonCode() {
        authFixture.scopes = List.of(new NhanVienWorkPhamVi(FACILITY_ID + 1, "Other Facility", DEPARTMENT_ID, "Dept"));
        GetHoatDongTiepDonPhongQuery query = validQuery();

        KetQua<List<PhongKham>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, roomsRepository.callCount);
        assertEquals(1, auditPort.independentEntries.size());

        HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, entry.actionCode());
        assertEquals(KiemToanKetQua.DENIED, entry.outcome());
        assertEquals(KiemToanLyDoMa.SCOPE_MISMATCH, entry.reasonCode());
    }

    @Test
    void handleAuthorizedReceptionistReturnsScopedRoomsAndRecordsCatalogViewSuccessAudit() {
        PhongKham room1 = new PhongKham();
        room1.setId(101);
        room1.setTenPhong("Phòng Khám Nội 1");
        room1.setMaKhoa(DEPARTMENT_ID);
        room1.setHoatDong(true);

        PhongKham room2 = new PhongKham();
        room2.setId(102);
        room2.setTenPhong("Phòng Khám Ngoại 1");
        room2.setMaKhoa(DEPARTMENT_ID);
        room2.setHoatDong(true);

        roomsRepository.rooms = List.of(room1, room2);
        GetHoatDongTiepDonPhongQuery query = validQuery();

        KetQua<List<PhongKham>> result = handler.handle(query);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertTrue(result.getValue().isPresent());
        List<PhongKham> returnedRooms = result.getValue().get();
        assertEquals(2, returnedRooms.size());
        assertEquals(101, returnedRooms.get(0).getId());
        assertEquals(102, returnedRooms.get(1).getId());

        assertEquals(1, roomsRepository.callCount);
        assertEquals(FACILITY_ID, roomsRepository.capturedFacilityId);
        assertEquals(DEPARTMENT_ID, roomsRepository.capturedDepartmentId);

        assertEquals(1, auditPort.independentEntries.size());
        HeThongKiemToanNhapLieu entry = auditPort.independentEntries.get(0);
        assertEquals(KiemToanHanhDongMa.CATALOG_VIEW, entry.actionCode());
        assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
        assertNull(entry.reasonCode());
        assertEquals(EMPLOYEE_ID, entry.actorEmployeeId());
        assertEquals(ROLE_ID, entry.roleId());
        assertEquals(ROLE_CODE, entry.roleCode());
        assertEquals(FACILITY_ID, entry.facilityId());
        assertEquals(DEPARTMENT_ID, entry.departmentId());
        assertEquals(WORKSTATION_ID, entry.workstationId());
        assertEquals(CORRELATION_ID, entry.correlationId());
        assertNotNull(entry.occurredAtUtc());
    }

    @Test
    void handleRepositoryRuntimeExceptionCaughtAndReturnsSafeRoomCatalogFailedWithoutLeakingRawMessage() {
        roomsRepository.exceptionToThrow = new RuntimeException(
                "SQL Server deadlocked / table locked: connection string jdbc:sqlserver://secret-host:1433;user=sa;pass=secretPass123");
        GetHoatDongTiepDonPhongQuery query = validQuery();

        KetQua<List<PhongKham>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertFalse(result.isSuccess());
        assertTrue(result.getError().isPresent());
        assertEquals("ROOM_CATALOG_FAILED", result.getError().get().code());
        assertEquals("Không thể tải danh sách phòng khám.", result.getError().get().message());

        assertFalse(result.getError().get().message().contains("SQL"));
        assertFalse(result.getError().get().message().contains("secret-host"));
        assertFalse(result.getError().get().message().contains("secretPass123"));
        assertFalse(result.getError().get().message().contains("deadlocked"));

        assertEquals(1, roomsRepository.callCount);
        assertTrue(auditPort.independentEntries.isEmpty(), "No success audit must be written when repository fails");
    }

    @Test
    void handleAuditFailureDuringDenialStillReturnsAuthorizationDeniedSafely() {
        auditPort.appendException = new RuntimeException("synthetic audit store connection drop");
        authFixture.permissionGranted = false;
        GetHoatDongTiepDonPhongQuery query = validQuery();

        KetQua<List<PhongKham>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
        assertEquals("Không được phép xem danh mục phòng trong phiên hiện tại.", result.getError().get().message());
    }

    private static GetHoatDongTiepDonPhongQuery validQuery() {
        PhanQuyenNguCanh authContext = new PhanQuyenNguCanh(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
        KiemToanTacNhanNguCanh actorContext = new KiemToanTacNhanNguCanh(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, WORKSTATION_ID, CORRELATION_ID);
        return new GetHoatDongTiepDonPhongQuery(authContext, actorContext);
    }

    private static final class FakePhongKhamRepository implements IPhongKhamRepository {
        List<PhongKham> rooms = List.of();
        RuntimeException exceptionToThrow;
        int callCount;
        int capturedFacilityId = -1;
        int capturedDepartmentId = -1;

        @Override
        public Optional<PhongKham> getById(int id) {
            return Optional.empty();
        }

        @Override
        public List<PhongKham> getActiveRooms() {
            return rooms;
        }

        @Override
        public List<PhongKham> getActiveRoomsInScope(int facilityId, int departmentId) {
            callCount++;
            capturedFacilityId = facilityId;
            capturedDepartmentId = departmentId;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return rooms;
        }
    }

    private static final class FakeAuditWritePort implements KiemToanWriteCongKetNoi {
        final List<HeThongKiemToanNhapLieu> currentTransactionEntries = new ArrayList<>();
        final List<HeThongKiemToanNhapLieu> independentEntries = new ArrayList<>();
        RuntimeException appendException;

        @Override
        public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {
            if (appendException != null) throw appendException;
            currentTransactionEntries.add(entry);
        }

        @Override
        public void appendIndependently(HeThongKiemToanNhapLieu entry) {
            if (appendException != null) throw appendException;
            independentEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {
            if (appendException != null) throw appendException;
        }

        @Override
        public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {
            if (appendException != null) throw appendException;
        }
    }

    private static final class AuthorizationFixture {
        private final PhanQuyenDichVu authorizationService;
        private boolean permissionGranted = true;
        private List<NhanVienWorkPhamVi> scopes = List.of(
                new NhanVienWorkPhamVi(FACILITY_ID, "Synthetic Facility", DEPARTMENT_ID, "Synthetic Dept"));

        private AuthorizationFixture() {
            NhanVien employee = new NhanVien();
            employee.setId(EMPLOYEE_ID);
            employee.setHoatDong(true);

            VaiTro role = new VaiTro();
            role.setId(ROLE_ID);
            role.setTenVaiTro(ROLE_CODE);

            HoatDongPhienCongKetNoi sessionPort = sessionId -> Optional.of(
                    new HoatDongPhienCongKetNoi.ActiveSession(
                            SESSION_ID, EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID,
                            true, true, true));

            INhanVienRepository employeeRepo = (INhanVienRepository) Proxy.newProxyInstance(
                    INhanVienRepository.class.getClassLoader(),
                    new Class<?>[]{INhanVienRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getById" -> Optional.of(employee);
                        case "getAssignedRoles" -> List.of(role);
                        case "getAssignedWorkScopes" -> scopes;
                        default -> null;
                    });

            IHeThongRepository systemRepo = new IHeThongRepository() {
                @Override
                public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
                    MaTranPhanQuyen grant = new MaTranPhanQuyen();
                    grant.setMaVaiTro(ROLE_ID);
                    grant.setPhanHe("CATALOG");
                    grant.setHanhDong(HanhDongQuyen.XEM);
                    grant.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);
                    grant.setDuocPhep(permissionGranted);
                    return List.of(grant);
                }

                @Override
                public Optional<String> getSystemParameterValue(String key) {
                    return Optional.empty();
                }
            };

            this.authorizationService = new PhanQuyenDichVu(sessionPort, employeeRepo, systemRepo);
        }
    }
}
