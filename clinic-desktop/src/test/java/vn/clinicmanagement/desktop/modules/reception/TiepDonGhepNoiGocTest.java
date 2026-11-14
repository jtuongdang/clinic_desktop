package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.reception.GetHoatDongTiepDonPhongQuery;
import vn.clinicmanagement.application.reception.GetTiepDonHangDoiQuery;
import vn.clinicmanagement.application.reception.SearchBenhNhanQuery;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepository;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;
import vn.clinicmanagement.infrastructure.persistence.system.HeThongRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TiepDonGhepNoiGocTest {

    @BeforeAll
    static void setUpHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void constructsAllHandlersWithSharedInfrastructure() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(tx);
        JdbcKiemToanRepository audit = new JdbcKiemToanRepository(connections, unitOfWork, () -> {
            throw new UnsupportedOperationException("Synthetic in-memory test; clinicDb connection forbidden");
        });

        HoatDongPhienCongKetNoi sessionPort = sessionId -> Optional.empty();
        NhanVienRepository employees = new NhanVienRepository(connections, unitOfWork);
        HeThongRepository system = new HeThongRepository(connections, unitOfWork);
        PhanQuyenDichVu authz = new PhanQuyenDichVu(sessionPort, employees, system);

        TiepDonGhepNoiGoc root = new TiepDonGhepNoiGoc(
                unitOfWork, tx, connections, audit, authz);

        assertNotNull(root.registerPatientHandler());
        assertNotNull(root.getRegisterPatientHandler());
        assertNotNull(root.searchPatientsHandler());
        assertNotNull(root.getSearchPatientsHandler());
        assertNotNull(root.getReceptionQueueHandler());
        assertNotNull(root.getGetReceptionQueueHandler());
        assertNotNull(root.getActiveReceptionRoomsHandler());
        assertNotNull(root.getGetActiveReceptionRoomsHandler());

        assertSame(unitOfWork, root.unitOfWork());
        assertSame(unitOfWork, root.getUnitOfWork());
        assertSame(tx, root.transactionManager());
        assertSame(tx, root.getTransactionManager());
        assertSame(connections, root.connectionProvider());
        assertSame(connections, root.getConnectionProvider());
        assertSame(audit, root.auditRepository());
        assertSame(audit, root.getAuditRepository());
        assertSame(authz, root.authorizationService());
        assertSame(authz, root.getAuthorizationService());
    }

    @Test
    void defaultConstructorAssemblesAllHandlers() {
        TiepDonGhepNoiGoc root = new TiepDonGhepNoiGoc();

        assertNotNull(root.registerPatientHandler());
        assertNotNull(root.searchPatientsHandler());
        assertNotNull(root.getReceptionQueueHandler());
        assertNotNull(root.getActiveReceptionRoomsHandler());
        assertNotNull(root.unitOfWork());
        assertNotNull(root.transactionManager());
        assertNotNull(root.connectionProvider());
        assertNotNull(root.auditRepository());
        assertNotNull(root.authorizationService());
    }

    @Test
    void factoryMethodsAssembleAllHandlers() {
        HoatDongPhienCongKetNoi sessionPort = sessionId -> Optional.empty();
        TiepDonGhepNoiGoc root = new TiepDonGhepNoiGoc(sessionPort);

        assertNotNull(root.registerPatientHandler());
        assertNotNull(root.searchPatientsHandler());
        assertNotNull(root.getReceptionQueueHandler());
        assertNotNull(root.getActiveReceptionRoomsHandler());
    }

    @Test
    void handlersEnforceDefaultDenyAuthorizationWithoutSession() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(tx);
        JdbcKiemToanRepository audit = new JdbcKiemToanRepository(connections, unitOfWork, () -> {
            throw new UnsupportedOperationException("Synthetic in-memory test; clinicDb connection forbidden");
        });

        // Default-deny session port (no active session found)
        HoatDongPhienCongKetNoi sessionPort = sessionId -> Optional.empty();
        NhanVienRepository employees = new NhanVienRepository(connections, unitOfWork);
        HeThongRepository system = new HeThongRepository(connections, unitOfWork);
        PhanQuyenDichVu authz = new PhanQuyenDichVu(sessionPort, employees, system);

        TiepDonGhepNoiGoc root = new TiepDonGhepNoiGoc(unitOfWork, tx, connections, audit, authz);

        // Evaluation without valid session must fail closed
        PhanQuyenNguCanh invalidContext = new PhanQuyenNguCanh(
                10, 2, "RECEPTIONIST", 1, 1, UUID.randomUUID());

        KetQua<?> roomsResult = root.getActiveReceptionRoomsHandler().handle(
                new GetHoatDongTiepDonPhongQuery(invalidContext, null));
        assertTrue(roomsResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", roomsResult.getError().orElseThrow().code());

        KetQua<?> queueResult = root.getReceptionQueueHandler().handle(
                new GetTiepDonHangDoiQuery(LocalDate.now(), invalidContext, null));
        assertTrue(queueResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", queueResult.getError().orElseThrow().code());

        KetQua<?> searchResult = root.searchPatientsHandler().handle(
                new SearchBenhNhanQuery("0123456789", invalidContext, null));
        assertTrue(searchResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", searchResult.getError().orElseThrow().code());
    }

    @Test
    void defaultRootWithoutActiveSessionPortDeniesProtectedHandlersWithMatchingActorContexts() {
        TiepDonGhepNoiGoc root = new TiepDonGhepNoiGoc();

        UUID sessionId = UUID.randomUUID();
        PhanQuyenNguCanh authContext = new PhanQuyenNguCanh(
                10, 2, "RECEPTIONIST", 1, 1, sessionId);

        KetQua<?> roomsResult = root.getActiveReceptionRoomsHandler().handle(
                new GetHoatDongTiepDonPhongQuery(authContext, null));
        assertTrue(roomsResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", roomsResult.getError().orElseThrow().code());

        KetQua<?> queueResult = root.getReceptionQueueHandler().handle(
                new GetTiepDonHangDoiQuery(LocalDate.now(), authContext, null));
        assertTrue(queueResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", queueResult.getError().orElseThrow().code());

        KetQua<?> searchResult = root.searchPatientsHandler().handle(
                new SearchBenhNhanQuery("0123456789", authContext, null));
        assertTrue(searchResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", searchResult.getError().orElseThrow().code());
    }
}
