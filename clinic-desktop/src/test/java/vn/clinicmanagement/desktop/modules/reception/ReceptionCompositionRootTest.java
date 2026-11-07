package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.ActiveSessionPort;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.reception.GetActiveReceptionRoomsQuery;
import vn.clinicmanagement.application.reception.GetReceptionQueueQuery;
import vn.clinicmanagement.application.reception.SearchPatientsQuery;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcAuditRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.SystemRepositoryImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReceptionCompositionRootTest {

    @BeforeAll
    static void setUpHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void constructsAllHandlersWithSharedInfrastructure() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager tx = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(tx);
        JdbcAuditRepositoryImpl audit = new JdbcAuditRepositoryImpl(connections, unitOfWork, () -> {
            throw new UnsupportedOperationException("Synthetic in-memory test; clinicDb connection forbidden");
        });

        ActiveSessionPort sessionPort = sessionId -> Optional.empty();
        NhanVienRepositoryImpl employees = new NhanVienRepositoryImpl(connections, unitOfWork);
        SystemRepositoryImpl system = new SystemRepositoryImpl(connections, unitOfWork);
        AuthorizationService authz = new AuthorizationService(sessionPort, employees, system);

        ReceptionCompositionRoot root = new ReceptionCompositionRoot(
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
        ReceptionCompositionRoot root = new ReceptionCompositionRoot();

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
        ActiveSessionPort sessionPort = sessionId -> Optional.empty();
        ReceptionCompositionRoot root = new ReceptionCompositionRoot(sessionPort);

        assertNotNull(root.registerPatientHandler());
        assertNotNull(root.searchPatientsHandler());
        assertNotNull(root.getReceptionQueueHandler());
        assertNotNull(root.getActiveReceptionRoomsHandler());
    }

    @Test
    void handlersEnforceDefaultDenyAuthorizationWithoutSession() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager tx = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(tx);
        JdbcAuditRepositoryImpl audit = new JdbcAuditRepositoryImpl(connections, unitOfWork, () -> {
            throw new UnsupportedOperationException("Synthetic in-memory test; clinicDb connection forbidden");
        });

        // Default-deny session port (no active session found)
        ActiveSessionPort sessionPort = sessionId -> Optional.empty();
        NhanVienRepositoryImpl employees = new NhanVienRepositoryImpl(connections, unitOfWork);
        SystemRepositoryImpl system = new SystemRepositoryImpl(connections, unitOfWork);
        AuthorizationService authz = new AuthorizationService(sessionPort, employees, system);

        ReceptionCompositionRoot root = new ReceptionCompositionRoot(unitOfWork, tx, connections, audit, authz);

        // Evaluation without valid session must fail closed
        AuthorizationContext invalidContext = new AuthorizationContext(
                10, 2, "RECEPTIONIST", 1, 1, UUID.randomUUID());

        Result<?> roomsResult = root.getActiveReceptionRoomsHandler().handle(
                new GetActiveReceptionRoomsQuery(invalidContext, null));
        assertTrue(roomsResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", roomsResult.getError().orElseThrow().code());

        Result<?> queueResult = root.getReceptionQueueHandler().handle(
                new GetReceptionQueueQuery(LocalDate.now(), invalidContext, null));
        assertTrue(queueResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", queueResult.getError().orElseThrow().code());

        Result<?> searchResult = root.searchPatientsHandler().handle(
                new SearchPatientsQuery("0123456789", invalidContext, null));
        assertTrue(searchResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", searchResult.getError().orElseThrow().code());
    }

    @Test
    void defaultRootWithoutActiveSessionPortDeniesProtectedHandlersWithMatchingActorContexts() {
        ReceptionCompositionRoot root = new ReceptionCompositionRoot();

        UUID sessionId = UUID.randomUUID();
        AuthorizationContext authContext = new AuthorizationContext(
                10, 2, "RECEPTIONIST", 1, 1, sessionId);

        Result<?> roomsResult = root.getActiveReceptionRoomsHandler().handle(
                new GetActiveReceptionRoomsQuery(authContext, null));
        assertTrue(roomsResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", roomsResult.getError().orElseThrow().code());

        Result<?> queueResult = root.getReceptionQueueHandler().handle(
                new GetReceptionQueueQuery(LocalDate.now(), authContext, null));
        assertTrue(queueResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", queueResult.getError().orElseThrow().code());

        Result<?> searchResult = root.searchPatientsHandler().handle(
                new SearchPatientsQuery("0123456789", authContext, null));
        assertTrue(searchResult.isFailure());
        assertEquals("AUTHORIZATION_DENIED", searchResult.getError().orElseThrow().code());
    }
}
