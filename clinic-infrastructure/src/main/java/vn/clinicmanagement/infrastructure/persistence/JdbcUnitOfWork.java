package vn.clinicmanagement.infrastructure.persistence;

import vn.clinicmanagement.domain.common.IUnitOfWork;

/**
 * Unit of Work nhận số dòng đã ghi bởi JDBC repository trong transaction hiện tại.
 * Commit/rollback thuộc về JdbcTransactionManager để toàn bộ use case có một ranh giới duy nhất.
 */
public final class JdbcUnitOfWork implements IUnitOfWork {
    private final ThreadLocal<Integer> changedRows = ThreadLocal.withInitial(() -> 0);

    public void recordChanges(int rows) {
        if (rows > 0) changedRows.set(changedRows.get() + rows);
    }

    void beginTransaction() { changedRows.remove(); }
    void clear() { changedRows.remove(); }

    @Override
    public int saveChanges() {
        int count = changedRows.get();
        changedRows.remove();
        return count;
    }

    @Override
    public boolean saveEntities() {
        return saveChanges() > 0;
    }
}
