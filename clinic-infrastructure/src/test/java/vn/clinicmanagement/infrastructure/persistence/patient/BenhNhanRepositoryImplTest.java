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
    void scopedCccdLookupRequiresBothFacilityAndDepartmentOnLinkedVisit() {
        String sql = BenhNhanRepositoryImpl.scopedCccdSql();

        assertTrue(sql.contains("EXISTS (SELECT 1 FROM LuotKham lk"));
        assertTrue(sql.contains("lk.maBenhNhan=BenhNhan.id"));
        assertTrue(sql.contains("lk.maCoSo=?"));
        assertTrue(sql.contains("lk.maKhoa=?"));
        assertTrue(sql.contains("lk.isDeleted=0"));
        assertFalse(sql.contains("OR lk.maCoSo"));
    }

    @Test
    void mapsOnlyTheActiveCccdIndexViolationWithoutLeakingTheDuplicateValue() {
        for (int errorCode : new int[]{2601, 2627}) {
            SQLException sqlException = new SQLException(
                    "Cannot insert duplicate key row with unique index 'UX_BenhNhan_Active_CCCD'. "
                            + "The duplicate key value is (CCCD:synthetic-cccd-value).",
                    "23000", errorCode);

            RuntimeException failure = BenhNhanRepositoryImpl.persistenceFailure(sqlException);

            assertInstanceOf(PatientIdentityConflictException.class, failure);
            assertNull(failure.getCause());
            assertTrue(failure.getMessage().contains("Định danh"));
            assertFalse(failure.getMessage().contains("synthetic-cccd-value"));
        }
    }

    @Test
    void doesNotMapOtherSqlServerUniqueKeysToDuplicateCccd() {
        for (int errorCode : new int[]{2601, 2627}) {
            SQLException sqlException = new SQLException(
                    "Cannot insert duplicate key row with unique index 'UX_BenhNhan_MaBenhNhanFormat'. "
                            + "The duplicate key value is (synthetic-patient-code).",
                    "23000", errorCode);

            RuntimeException failure = BenhNhanRepositoryImpl.persistenceFailure(sqlException);

            assertFalse(failure instanceof PatientIdentityConflictException);
            assertInstanceOf(IllegalStateException.class, failure);
            assertTrue(failure.getCause() == sqlException);
        }
    }

    @Test
    void doesNotMapDuplicateCodesWithoutTheCccdIndexName() {
        SQLException sqlException = new SQLException("Duplicate key: synthetic-value", "23000", 2601);

        RuntimeException failure = BenhNhanRepositoryImpl.persistenceFailure(sqlException);

        assertFalse(failure instanceof PatientIdentityConflictException);
        assertInstanceOf(IllegalStateException.class, failure);
    }

    @Test
    void keepsNonDuplicateSqlErrorsAsPersistenceFailures() {
        SQLException sqlException = new SQLException("Synthetic database failure", "42000", 102);

        RuntimeException failure = BenhNhanRepositoryImpl.persistenceFailure(sqlException);

        assertInstanceOf(IllegalStateException.class, failure);
        assertTrue(failure.getCause() == sqlException);
    }
}
