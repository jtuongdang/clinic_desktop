package vn.clinicmanagement.desktop.modules.auth;

import vn.clinicmanagement.application.auth.AuthenticateUserHandler;
import vn.clinicmanagement.application.auth.Pbkdf2PasswordHasher;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepositoryImpl;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.infrastructure.persistence.system.SystemRepositoryImpl;

/** Builds the authentication use case and its JDBC adapters for the desktop application. */
public final class AuthenticationCompositionRoot {
    private AuthenticationCompositionRoot() {}

    public static AuthenticateUserHandler createHandler() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        NhanVienRepositoryImpl employees = new NhanVienRepositoryImpl(connections, unitOfWork);
        return new AuthenticateUserHandler(employees, new Pbkdf2PasswordHasher(), transactions);
    }

    public static SessionTimeoutPolicy createSessionTimeoutPolicy() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        return SessionTimeoutPolicy.from(new SystemRepositoryImpl(connections, unitOfWork));
    }

    public static SessionLifecycleService createSessionLifecycleService(UserSession session) {
        return new SessionLifecycleService(session, createSessionTimeoutPolicy(), java.time.Clock.systemUTC());
    }
}
