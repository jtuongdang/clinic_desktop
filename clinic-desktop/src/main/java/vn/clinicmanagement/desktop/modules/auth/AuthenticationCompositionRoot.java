package vn.clinicmanagement.desktop.modules.auth;

import vn.clinicmanagement.application.auth.AuthenticateUserHandler;
import vn.clinicmanagement.application.auth.Pbkdf2PasswordHasher;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepositoryImpl;

/** Builds the authentication use case and its JDBC adapters for the desktop application. */
final class AuthenticationCompositionRoot {
    private AuthenticationCompositionRoot() {}

    static AuthenticateUserHandler createHandler() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        NhanVienRepositoryImpl employees = new NhanVienRepositoryImpl(connections, unitOfWork);
        return new AuthenticateUserHandler(employees, new Pbkdf2PasswordHasher(), transactions);
    }
}
