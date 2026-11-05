package vn.clinicmanagement.infrastructure.persistence.patient;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.patient.PatientIdentityConflictException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenhNhanRepositoryImplTest {
    @Test
    void mapsSqlServerDuplicateKeyErrorsWithoutLeakingTheDuplicateValue() {
        for (int errorCode : new int[]{2601, 2627}) {
            SQLException sqlException = new SQLException("Duplicate key: synthetic-cccd-value", "23000", errorCode);

            RuntimeException failure = BenhNhanRepositoryImpl.persistenceFailure(sqlException);

            assertInstanceOf(PatientIdentityConflictException.class, failure);
            assertNull(failure.getCause());
            assertTrue(failure.getMessage().contains("Định danh"));
            assertFalse(failure.getMessage().contains("synthetic-cccd-value"));
        }
    }

    @Test
    void keepsNonDuplicateSqlErrorsAsPersistenceFailures() {
        SQLException sqlException = new SQLException("Synthetic database failure", "42000", 102);

        RuntimeException failure = BenhNhanRepositoryImpl.persistenceFailure(sqlException);

        assertInstanceOf(IllegalStateException.class, failure);
        assertTrue(failure.getCause() == sqlException);
    }
}
