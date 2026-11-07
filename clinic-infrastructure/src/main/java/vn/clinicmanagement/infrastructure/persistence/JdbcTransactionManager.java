package vn.clinicmanagement.infrastructure.persistence;

import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/** JDBC transaction boundary shared by repository calls on the current thread. */
public final class JdbcTransactionManager implements TransactionBoundary {
    private final JdbcUnitOfWork unitOfWork;
    private final ConnectionSupplier connectionSupplier;
    private final ThreadLocal<Connection> currentConnection = new ThreadLocal<>();
    private final ThreadLocal<Boolean> rollbackOnly = ThreadLocal.withInitial(() -> false);

    public JdbcTransactionManager(JdbcUnitOfWork unitOfWork) {
        this(unitOfWork, DatabaseConnection::getConnection);
    }

    JdbcTransactionManager(JdbcUnitOfWork unitOfWork, ConnectionSupplier connectionSupplier) {
        this.unitOfWork = unitOfWork;
        this.connectionSupplier = connectionSupplier;
    }

    @Override
    public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
        if (currentConnection.get() != null) {
            try {
                Result<T> nested = operation.get();
                if (nested == null || nested.isFailure()) rollbackOnly.set(true);
                return nested;
            } catch (RuntimeException ex) {
                rollbackOnly.set(true);
                return Result.failure("TRANSACTION_FAILED", "Giao dịch nghiệp vụ không thành công.");
            }
        }

        try (Connection connection = connectionSupplier.get()) {
            connection.setAutoCommit(false);
            currentConnection.set(connection);
            rollbackOnly.set(false);
            unitOfWork.beginTransaction();
            Result<T> result;
            try {
                result = operation.get();
            } catch (RuntimeException ex) {
                rollbackOnly.set(true);
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    // Keep the operation failure generic even when rollback also fails.
                }
                return Result.failure("TRANSACTION_FAILED", "Không thể hoàn tất giao dịch cơ sở dữ liệu.");
            }
            if (result == null || result.isFailure() || rollbackOnly.get()) {
                connection.rollback();
                if (result == null) return Result.failure("TRANSACTION_FAILED", "Giao dịch không trả về kết quả.");
                if (rollbackOnly.get() && result.isSuccess()) {
                    return Result.failure("TRANSACTION_ROLLBACK_ONLY", "Giao dịch đã bị đánh dấu rollback.");
                }
                return result;
            }
            unitOfWork.saveChanges(); // JDBC statements are already executed; this flushes the UoW change count.
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException ex) {
            return Result.failure("TRANSACTION_FAILED", "Không thể hoàn tất giao dịch cơ sở dữ liệu.");
        } finally {
            currentConnection.remove();
            rollbackOnly.remove();
            unitOfWork.clear();
        }
    }

    Connection currentConnection() { return currentConnection.get(); }

    @FunctionalInterface
    interface ConnectionSupplier {
        Connection get() throws SQLException;
    }
}
