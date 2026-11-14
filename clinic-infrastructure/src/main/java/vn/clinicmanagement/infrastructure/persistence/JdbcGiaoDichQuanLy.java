package vn.clinicmanagement.infrastructure.persistence;

import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/** JDBC transaction boundary shared by repository calls on the current thread. */
public final class JdbcGiaoDichQuanLy implements GiaoDichRanhGioi {
    /** Trường `unitOfWork`: đơn vị của làm việc. */
    private final JdbcUnitOfWork unitOfWork;
    /** Trường `connectionSupplier`: kết nối `Supplier`. */
    private final ConnectionSupplier connectionSupplier;
    /** Trường `currentConnection`: hientai kết nối. */
    private final ThreadLocal<Connection> currentConnection = new ThreadLocal<>();
    private final ThreadLocal<Boolean> rollbackOnly = ThreadLocal.withInitial(() -> false);

    public JdbcGiaoDichQuanLy(JdbcUnitOfWork unitOfWork) {
        this(unitOfWork, CoSoDuLieuKetNoi::getConnection);
    }

    JdbcGiaoDichQuanLy(JdbcUnitOfWork unitOfWork, ConnectionSupplier connectionSupplier) {
        this.unitOfWork = unitOfWork;
        this.connectionSupplier = connectionSupplier;
    }

    /** Hàm `inTransaction`: trong giao dịch. Gốc khai báo tại `vn.clinicmanagement.application.common.GiaoDichRanhGioi#inTransaction`. */
    @Override
    public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
        if (currentConnection.get() != null) {
            try {
                KetQua<T> nested = operation.get();
                if (nested == null || nested.isFailure()) rollbackOnly.set(true);
                return nested;
            } catch (RuntimeException ex) {
                rollbackOnly.set(true);
                return KetQua.failure("TRANSACTION_FAILED", "Giao dịch nghiệp vụ không thành công.");
            }
        }

        try (Connection connection = connectionSupplier.get()) {
            connection.setAutoCommit(false);
            currentConnection.set(connection);
            rollbackOnly.set(false);
            unitOfWork.beginTransaction();
            KetQua<T> result;
            try {
                result = operation.get();
            } catch (RuntimeException ex) {
                rollbackOnly.set(true);
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    // Keep the operation failure generic even when rollback also fails.
                }
                return KetQua.failure("TRANSACTION_FAILED", "Không thể hoàn tất giao dịch cơ sở dữ liệu.");
            }
            if (result == null || result.isFailure() || rollbackOnly.get()) {
                connection.rollback();
                if (result == null) return KetQua.failure("TRANSACTION_FAILED", "Giao dịch không trả về kết quả.");
                if (rollbackOnly.get() && result.isSuccess()) {
                    return KetQua.failure("TRANSACTION_ROLLBACK_ONLY", "Giao dịch đã bị đánh dấu rollback.");
                }
                return result;
            }
            unitOfWork.saveChanges(); // JDBC statements are already executed; this flushes the UoW change count.
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException ex) {
            return KetQua.failure("TRANSACTION_FAILED", "Không thể hoàn tất giao dịch cơ sở dữ liệu.");
        } finally {
            currentConnection.remove();
            rollbackOnly.remove();
            unitOfWork.clear();
        }
    }

    /** Hàm `currentConnection`: hientai kết nối. */
    Connection currentConnection() { return currentConnection.get(); }

    @FunctionalInterface
    interface ConnectionSupplier {
        /** Hàm `get`: lấy. */
        Connection get() throws SQLException;
    }
}
