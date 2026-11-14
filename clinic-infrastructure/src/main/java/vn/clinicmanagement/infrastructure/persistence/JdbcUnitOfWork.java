package vn.clinicmanagement.infrastructure.persistence;

import vn.clinicmanagement.domain.common.IUnitOfWork;

/**
 * Unit of Work nhận số dòng đã ghi bởi JDBC repository trong transaction hiện tại.
 * Commit/rollback thuộc về JdbcTransactionManager để toàn bộ use case có một ranh giới duy nhất.
 */
public final class JdbcUnitOfWork implements IUnitOfWork {
    /** Trường `changedRows`: thaydoi đóng. */
    private final ThreadLocal<Integer> changedRows = ThreadLocal.withInitial(() -> 0);

    /** Hàm `recordChanges`: ghi nhận thaydoi. */
    public void recordChanges(int rows) {
        if (rows > 0) changedRows.set(changedRows.get() + rows);
    }

    /** Hàm `beginTransaction`: `begin` giao dịch. */
    void beginTransaction() { changedRows.remove(); }
    /** Hàm `clear`: xóa. */
    void clear() { changedRows.remove(); }

    /** Hàm `saveChanges`: lưu thaydoi. Gốc khai báo tại `vn.clinicmanagement.domain.common.IUnitOfWork#saveChanges`. */
    @Override
    public int saveChanges() {
        int count = changedRows.get();
        changedRows.remove();
        return count;
    }

    /** Hàm `saveEntities`: lưu thực thể. Gốc khai báo tại `vn.clinicmanagement.domain.common.IUnitOfWork#saveEntities`. */
    @Override
    public boolean saveEntities() {
        return saveChanges() > 0;
    }
}
