package vn.clinicmanagement.desktop.modules.auth;

import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.authorization.NavigationPermissionsHandler;
import vn.clinicmanagement.application.auth.AuthenticateUserHandler;
import vn.clinicmanagement.application.auth.CompleteLoginSessionHandler;
import vn.clinicmanagement.application.auth.Pbkdf2PasswordHasher;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.infrastructure.persistence.DatabaseConnection;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcAuditRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.SystemRepositoryImpl;

import java.time.Clock;

/** Builds the authentication use case and its JDBC adapters for the desktop application. */
public final class AuthenticationCompositionRoot {
    private AuthenticationCompositionRoot() {}

    public static AuthenticateUserHandler createHandler() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        NhanVienRepositoryImpl employees = new NhanVienRepositoryImpl(connections, unitOfWork);
        JdbcAuditRepositoryImpl auditWritePort = new JdbcAuditRepositoryImpl(
                connections, unitOfWork, DatabaseConnection::getConnection);
        return new AuthenticateUserHandler(
                employees, new Pbkdf2PasswordHasher(), transactions, auditWritePort, Clock.systemUTC());
    }

    public static CompleteLoginSessionHandler createCompleteLoginSessionHandler() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        NhanVienRepositoryImpl employees = new NhanVienRepositoryImpl(connections, unitOfWork);
        JdbcAuditRepositoryImpl auditWritePort = new JdbcAuditRepositoryImpl(
                connections, unitOfWork, DatabaseConnection::getConnection);
        return new CompleteLoginSessionHandler(employees, transactions, auditWritePort, Clock.systemUTC());
    }

    public static NavigationPermissionsHandler createNavigationPermissionsHandler() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        return new NavigationPermissionsHandler(new SystemRepositoryImpl(connections, unitOfWork));
    }

    public static AuditWritePort createAuditWritePort() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        return new JdbcAuditRepositoryImpl(connections, unitOfWork, DatabaseConnection::getConnection);
    }

    public static SessionTimeoutPolicy createSessionTimeoutPolicy() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        return SessionTimeoutPolicy.from(new SystemRepositoryImpl(connections, unitOfWork));
    }

    public static SessionLifecycleService createSessionLifecycleService(UserSession session) {
        return createSessionLifecycleService(session, createSessionTimeoutPolicy());
    }

    public static SessionLifecycleService createSessionLifecycleService(
            UserSession session,
            SessionTimeoutPolicy policy) {
        return new SessionLifecycleService(session, policy, Clock.systemUTC(), createAuditWritePort());
    }
}
