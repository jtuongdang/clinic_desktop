package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.application.authorization.ActiveSessionPort;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.reception.GetActiveReceptionRoomsHandler;
import vn.clinicmanagement.application.reception.GetReceptionQueueHandler;
import vn.clinicmanagement.application.reception.RegisterPatientHandler;
import vn.clinicmanagement.application.reception.SearchPatientsHandler;
import vn.clinicmanagement.infrastructure.persistence.DatabaseConnection;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.facility.PhongKhamRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.patient.BaoHiemRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.patient.BenhNhanCoSoRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.patient.BenhNhanRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.reception.ReceptionQueueRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcAuditRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.SystemRepositoryImpl;

import java.util.Objects;
import java.util.Optional;

/**
 * Assembles reception use cases and their shared JDBC persistence adapters,
 * transaction boundaries, audit writer, and authorization service.
 */
public final class ReceptionCompositionRoot {

    private final JdbcUnitOfWork unitOfWork;
    private final JdbcTransactionManager transactionManager;
    private final JdbcConnectionProvider connectionProvider;
    private final JdbcAuditRepositoryImpl auditRepository;
    private final AuthorizationService authorizationService;

    private final RegisterPatientHandler registerPatientHandler;
    private final SearchPatientsHandler searchPatientsHandler;
    private final GetReceptionQueueHandler getReceptionQueueHandler;
    private final GetActiveReceptionRoomsHandler getActiveReceptionRoomsHandler;

    public ReceptionCompositionRoot(
            JdbcUnitOfWork unitOfWork,
            JdbcTransactionManager transactionManager,
            JdbcConnectionProvider connectionProvider,
            JdbcAuditRepositoryImpl auditRepository,
            AuthorizationService authorizationService) {
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork");
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager");
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");

        BenhNhanRepositoryImpl patientRepo = new BenhNhanRepositoryImpl(connectionProvider, unitOfWork);
        BenhNhanCoSoRepositoryImpl patientFacilityRepo = new BenhNhanCoSoRepositoryImpl(connectionProvider, unitOfWork);
        BaoHiemRepositoryImpl insuranceRepo = new BaoHiemRepositoryImpl(connectionProvider, unitOfWork);
        PhongKhamRepositoryImpl roomRepo = new PhongKhamRepositoryImpl(connectionProvider);
        ReceptionQueueRepositoryImpl queueRepo = new ReceptionQueueRepositoryImpl(connectionProvider, unitOfWork);

        this.registerPatientHandler = new RegisterPatientHandler(
                patientRepo, patientFacilityRepo, insuranceRepo, roomRepo, queueRepo,
                transactionManager, auditRepository, authorizationService);
        this.searchPatientsHandler = new SearchPatientsHandler(
                patientRepo, insuranceRepo, authorizationService, auditRepository);
        this.getReceptionQueueHandler = new GetReceptionQueueHandler(
                queueRepo, authorizationService, auditRepository);
        this.getActiveReceptionRoomsHandler = new GetActiveReceptionRoomsHandler(
                roomRepo, authorizationService, auditRepository);
    }

    public ReceptionCompositionRoot(AuthorizationService authorizationService) {
        this(createBundleWithAuthz(authorizationService));
    }

    public ReceptionCompositionRoot(ActiveSessionPort sessionPort) {
        this(createBundleWithSession(sessionPort));
    }

    public ReceptionCompositionRoot() {
        this((ActiveSessionPort) null);
    }

    private ReceptionCompositionRoot(Bundle bundle) {
        this(bundle.uow, bundle.tx, bundle.conn, bundle.audit, bundle.authz);
    }

    private record Bundle(JdbcUnitOfWork uow, JdbcTransactionManager tx, JdbcConnectionProvider conn,
                          JdbcAuditRepositoryImpl audit, AuthorizationService authz) {}

    private static Bundle createBundleWithAuthz(AuthorizationService authorizationService) {
        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcTransactionManager tx = new JdbcTransactionManager(uow);
        JdbcConnectionProvider conn = new JdbcConnectionProvider(tx);
        JdbcAuditRepositoryImpl audit = new JdbcAuditRepositoryImpl(conn, uow, DatabaseConnection::getConnection);
        return new Bundle(uow, tx, conn, audit,
                authorizationService != null ? authorizationService : createDefaultAuthz(null, conn, uow));
    }

    private static Bundle createBundleWithSession(ActiveSessionPort sessionPort) {
        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcTransactionManager tx = new JdbcTransactionManager(uow);
        JdbcConnectionProvider conn = new JdbcConnectionProvider(tx);
        JdbcAuditRepositoryImpl audit = new JdbcAuditRepositoryImpl(conn, uow, DatabaseConnection::getConnection);
        AuthorizationService authz = createDefaultAuthz(sessionPort, conn, uow);
        return new Bundle(uow, tx, conn, audit, authz);
    }

    private static AuthorizationService createDefaultAuthz(
            ActiveSessionPort sessionPort,
            JdbcConnectionProvider conn,
            JdbcUnitOfWork uow) {
        NhanVienRepositoryImpl employees = new NhanVienRepositoryImpl(conn, uow);
        SystemRepositoryImpl system = new SystemRepositoryImpl(conn, uow);
        ActiveSessionPort port = sessionPort != null ? sessionPort : (sessionId -> Optional.empty());
        return new AuthorizationService(port, employees, system);
    }

    public static ReceptionCompositionRoot create(
            JdbcUnitOfWork unitOfWork,
            JdbcTransactionManager transactionManager,
            JdbcConnectionProvider connectionProvider,
            JdbcAuditRepositoryImpl auditRepository,
            AuthorizationService authorizationService) {
        return new ReceptionCompositionRoot(unitOfWork, transactionManager, connectionProvider, auditRepository, authorizationService);
    }

    public static ReceptionCompositionRoot create(AuthorizationService authorizationService) {
        return new ReceptionCompositionRoot(authorizationService);
    }

    public RegisterPatientHandler getRegisterPatientHandler() {
        return registerPatientHandler;
    }

    public RegisterPatientHandler registerPatientHandler() {
        return registerPatientHandler;
    }

    public SearchPatientsHandler getSearchPatientsHandler() {
        return searchPatientsHandler;
    }

    public SearchPatientsHandler searchPatientsHandler() {
        return searchPatientsHandler;
    }

    public GetReceptionQueueHandler getGetReceptionQueueHandler() {
        return getReceptionQueueHandler;
    }

    public GetReceptionQueueHandler getReceptionQueueHandler() {
        return getReceptionQueueHandler;
    }

    public GetActiveReceptionRoomsHandler getGetActiveReceptionRoomsHandler() {
        return getActiveReceptionRoomsHandler;
    }

    public GetActiveReceptionRoomsHandler getActiveReceptionRoomsHandler() {
        return getActiveReceptionRoomsHandler;
    }

    public JdbcUnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    public JdbcUnitOfWork unitOfWork() {
        return unitOfWork;
    }

    public JdbcTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public JdbcTransactionManager transactionManager() {
        return transactionManager;
    }

    public JdbcConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    public JdbcConnectionProvider connectionProvider() {
        return connectionProvider;
    }

    public JdbcAuditRepositoryImpl getAuditRepository() {
        return auditRepository;
    }

    public JdbcAuditRepositoryImpl auditRepository() {
        return auditRepository;
    }

    public AuthorizationService getAuthorizationService() {
        return authorizationService;
    }

    public AuthorizationService authorizationService() {
        return authorizationService;
    }
}
