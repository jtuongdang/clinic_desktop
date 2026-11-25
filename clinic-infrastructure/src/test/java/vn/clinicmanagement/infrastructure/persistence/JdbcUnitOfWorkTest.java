package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcUnitOfWorkTest {
    @Test
    void recordChangesAccumulatesOnlyPositiveRows() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();

        unitOfWork.recordChanges(3);
        unitOfWork.recordChanges(0);
        unitOfWork.recordChanges(-2);
        unitOfWork.recordChanges(4);

        assertEquals(7, unitOfWork.saveChanges());
    }

    @Test
    void beginTransactionClearsPreviouslyRecordedChanges() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        unitOfWork.recordChanges(2);

        unitOfWork.beginTransaction();

        assertEquals(0, unitOfWork.saveChanges());
    }

    @Test
    void clearDiscardsRecordedChanges() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        unitOfWork.recordChanges(2);

        unitOfWork.clear();

        assertEquals(0, unitOfWork.saveChanges());
    }

    @Test
    void saveChangesReturnsAndResetsRecordedCount() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        unitOfWork.recordChanges(5);

        assertEquals(5, unitOfWork.saveChanges());
        assertEquals(0, unitOfWork.saveChanges());
    }

    @Test
    void saveEntitiesReportsWhetherChangesWereRecordedAndResetsCount() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();

        assertFalse(unitOfWork.saveEntities());

        unitOfWork.recordChanges(1);

        assertTrue(unitOfWork.saveEntities());
        assertFalse(unitOfWork.saveEntities());
    }
}
