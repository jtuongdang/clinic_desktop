package vn.clinicmanagement.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;

/** Cho repository mượn connection của transaction hiện tại hoặc một connection độc lập. */
public final class JdbcConnectionProvider {
    private final JdbcTransactionManager transactions;

    public JdbcConnectionProvider(JdbcTransactionManager transactions) {
        this.transactions = transactions;
    }

    public Lease acquire() throws SQLException {
        Connection transactionConnection = transactions.currentConnection();
        if (transactionConnection != null) return new Lease(transactionConnection, false);
        return new Lease(DatabaseConnection.getConnection(), true);
    }

    public boolean isTransactionActive() { return transactions.currentConnection() != null; }

    public static final class Lease implements AutoCloseable {
        private final Connection connection;
        private final boolean owned;

        private Lease(Connection connection, boolean owned) {
            this.connection = connection;
            this.owned = owned;
        }

        public Connection connection() { return connection; }

        @Override
        public void close() throws SQLException {
            if (owned) connection.close();
        }
    }
}
