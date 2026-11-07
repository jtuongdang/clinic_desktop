package vn.clinicmanagement.infrastructure.persistence.identity;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NhanVienLoginAuditAtomicityTest {
    @Test
    void recordSuccessfulLoginRequiresAnActiveSharedTransaction() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        NhanVienRepositoryImpl employees = new NhanVienRepositoryImpl(
                new JdbcConnectionProvider(transactions), unitOfWork);

        assertThrows(IllegalStateException.class,
                () -> employees.recordSuccessfulLogin(701, LocalDateTime.of(2026, 1, 15, 8, 30)));
    }
}
