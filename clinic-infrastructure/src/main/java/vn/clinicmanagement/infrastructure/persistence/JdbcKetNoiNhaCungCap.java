package vn.clinicmanagement.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;

/** Cho repository mượn connection của transaction hiện tại hoặc một connection độc lập. */
public final class JdbcKetNoiNhaCungCap {
    private final JdbcGiaoDichQuanLy transactions;

    public JdbcKetNoiNhaCungCap(JdbcGiaoDichQuanLy transactions) {
        this.transactions = transactions;
    }

    public Lease acquire() throws SQLException {
        Connection transactionConnection = transactions.currentConnection();
        if (transactionConnection != null) return new Lease(transactionConnection, false);
        return new Lease(CoSoDuLieuKetNoi.getConnection(), true);
    }

    /** Hàm `isTransactionActive`: là giao dịch hoạt động. */
    public boolean isTransactionActive() { return transactions.currentConnection() != null; }

    public static final class Lease implements AutoCloseable {
        /** Trường `connection`: kết nối. */
        private final Connection connection;
        private final boolean owned;

        private Lease(Connection connection, boolean owned) {
            this.connection = connection;
            this.owned = owned;
        }

        /** Hàm `connection`: kết nối. */
        public Connection connection() { return connection; }

        /** Hàm `close`: đóng. Gốc khai báo tại `AutoCloseable#close`. */
        @Override
        public void close() throws SQLException {
            if (owned) connection.close();
        }
    }
}
