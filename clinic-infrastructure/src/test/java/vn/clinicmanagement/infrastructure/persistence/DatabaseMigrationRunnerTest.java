package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationRunnerTest {

    @Test
    void executeSqlScriptExecutesSplitNonEmptyBatchesInOrderWithoutDatabaseConnection() throws SQLException {
        List<String> executedBatches = new ArrayList<>();
        AtomicBoolean statementClosed = new AtomicBoolean(false);

        Statement fakeStatement = (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("execute".equals(name) && args != null && args.length == 1 && args[0] instanceof String sql) {
                        executedBatches.add(sql);
                        return false;
                    }
                    if ("close".equals(name)) {
                        statementClosed.set(true);
                        return null;
                    }
                    throw new UnsupportedOperationException("Unexpected Statement method: " + name);
                }
        );

        Connection fakeConnection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("createStatement".equals(name)) {
                        return fakeStatement;
                    }
                    throw new UnsupportedOperationException("Unexpected Connection method: " + name);
                }
        );

        String syntheticScript = "\n"
                + "GO\n"
                + "CREATE TABLE SyntheticRoom (id INT PRIMARY KEY);\n"
                + "  go \r\n"
                + "\n"
                + "  Go\n"
                + "INSERT INTO SyntheticRoom (id) VALUES (101);\n"
                + "   gO   \n"
                + "UPDATE SyntheticRoom SET id = 102 WHERE id = 101;\n"
                + "GO\n";

        DatabaseMigrationRunner.executeSqlScript(fakeConnection, syntheticScript);

        assertEquals(
                List.of(
                        "CREATE TABLE SyntheticRoom (id INT PRIMARY KEY);",
                        "INSERT INTO SyntheticRoom (id) VALUES (101);",
                        "UPDATE SyntheticRoom SET id = 102 WHERE id = 101;"
                ),
                executedBatches
        );
        assertTrue(statementClosed.get());
    }
}
