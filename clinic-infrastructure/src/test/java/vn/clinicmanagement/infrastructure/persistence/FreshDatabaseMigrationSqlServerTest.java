package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FreshDatabaseMigrationSqlServerTest {

    private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile("clinic_test_[A-Za-z0-9_]+");
    private static final Pattern DATABASE_OPTION_PATTERN =
            Pattern.compile("(?i);\\s*(?:databaseName|database)\\s*=[^;]*");
    private static final Pattern EMBEDDED_CREDENTIAL_PATTERN =
            Pattern.compile("(?i);\\s*(?:user|password)\\s*=");

    @Test
    void migratesAnExplicitlySelectedFreshSqlServerDatabase() throws Exception {
        Assumptions.assumeTrue(
                "true".equals(System.getProperty("clinic.sqlserver.migration.enabled")),
                "Set clinic.sqlserver.migration.enabled=true to opt in"
        );

        String databaseName = System.getProperty("clinic.sqlserver.test.database");
        if (databaseName == null || !DATABASE_NAME_PATTERN.matcher(databaseName).matches()) {
            throw new IllegalArgumentException(
                    "clinic.sqlserver.test.database must match clinic_test_[A-Za-z0-9_]+"
            );
        }

        Properties properties = loadLocalProperties();
        String baseUrl = requiredProperty(properties, "db.url").trim();
        String username = requiredProperty(properties, "db.username").trim();
        String credentialValue = requiredProperty(properties, "db." + "password");
        String selectedUrl = selectDatabase(baseUrl, databaseName);

        DatabaseConnection.setConnectionString("");
        DatabaseConnection.configure(selectedUrl, username, credentialValue);

        try (Connection connection = DatabaseConnection.getConnection()) {
            assertEquals(
                    databaseName,
                    queryString(connection, "SELECT DB_NAME()"),
                    "The SQL Server connection must select only the explicitly named test database"
            );
            assertEquals(
                    0,
                    queryInt(connection, "SELECT COUNT(*) FROM sys.objects WHERE is_ms_shipped = 0"),
                    "Refusing to run migrations because the selected database already has user objects"
            );
        } catch (SQLException exception) {
            throw new AssertionError(
                    "Could not inspect the selected SQL Server test database; connection details were suppressed"
            );
        }

        if (!runMigrationsWithoutConsoleOutput()) {
            throw new AssertionError("DatabaseMigrationRunner reported a migration failure");
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            assertEquals(
                    List.of(
                            "V1__Initial_Clinic_Schema.sql",
                            "V2__Seed_Initial_Data.sql",
                            "V3__Repair_Demo_Account_Passwords.sql",
                            "V4__Enforce_Active_Patient_CCCD_Uniqueness.sql",
                            "V5__Seed_Session_Timeout_Parameters.sql",
                            "V6__Enhance_Audit_Tables_Append_Only.sql",
                            "V7__Harden_Permission_Matrix_Defaults.sql"
                    ),
                    queryMigrationRecords(connection),
                    "All V1-V7 migrations must be recorded as successful"
            );

            assertEquals(1, queryInt(
                    connection,
                    "SELECT COUNT(*) FROM sys.tables WHERE name = 'CoSo'"
            ), "V1 must create the representative CoSo table");
            assertEquals(1, queryInt(
                    connection,
                    "SELECT COUNT(*) FROM sys.tables t "
                            + "JOIN sys.columns c ON c.object_id = t.object_id "
                            + "JOIN sys.indexes i ON i.object_id = t.object_id "
                            + "JOIN sys.index_columns ic ON ic.object_id = i.object_id "
                            + "AND ic.index_id = i.index_id AND ic.column_id = c.column_id "
                            + "WHERE t.name = 'BenhNhan' AND c.name = 'activeCccdUniqueKey' "
                            + "AND c.is_computed = 1 AND i.name = 'UX_BenhNhan_Active_CCCD' "
                            + "AND i.is_unique = 1"
            ), "V4 must create the unique index on the computed active CCCD key");

            assertSeedCount(connection, "CoSo", 1);
            assertSeedCount(connection, "Khoa", 5);
            assertSeedCount(connection, "PhongKham", 8);
            assertSeedCount(connection, "VaiTro", 6);
            assertSeedCount(connection, "ChucVu", 5);
            assertSeedCount(connection, "ICD", 7);
            assertSeedCount(connection, "DanhMucDichVu", 6);
            assertSeedCount(connection, "Kho", 3);
            assertSeedCount(connection, "Item", 3);
            assertSeedCount(connection, "TonKho", 3);
            assertSeedCount(connection, "MaTranPhanQuyen", 0);
            assertEquals(
                    2,
                    queryInt(connection,
                            "SELECT COUNT(*) FROM sys.default_constraints dc "
                                    + "JOIN sys.columns c ON c.object_id = dc.parent_object_id "
                                    + "AND c.column_id = dc.parent_column_id "
                                    + "WHERE dc.parent_object_id = OBJECT_ID('dbo.MaTranPhanQuyen') "
                                    + "AND ((c.name = 'phamViDuLieu' AND dc.definition LIKE '%KHOA_PHONG%') "
                                    + "OR (c.name = 'duocPhep' AND dc.definition LIKE '%0%'))"),
                    "V7 must persist KHOA_PHONG and false defaults"
            );
            assertEquals(
                    1,
                    queryInt(connection,
                            "SELECT COUNT(*) FROM sys.check_constraints "
                                    + "WHERE parent_object_id = OBJECT_ID('dbo.MaTranPhanQuyen') "
                                    + "AND name = 'CK_MaTranPhanQuyen_ActiveAllowScope' "
                                    + "AND is_disabled = 0 AND is_not_trusted = 0"),
                    "V7 must enable and trust the active ALLOW scope constraint"
            );
            assertEquals(
                    1,
                    queryInt(connection,
                            "SELECT COUNT(*) FROM sys.indexes "
                                    + "WHERE object_id = OBJECT_ID('dbo.MaTranPhanQuyen') "
                                    + "AND name = 'UX_MaTranPhanQuyen_Active_Role_Module_Action' "
                                    + "AND is_unique = 1 AND has_filter = 1 "
                                    + "AND filter_definition LIKE '%isDeleted%0%'"),
                    "V7 must create the filtered unique active assignment index"
            );
            assertEquals(
                    1,
                    queryInt(
                            connection,
                            "SELECT COUNT(*) FROM ThamSoHeThong "
                                    + "WHERE maThamSo = 'SESSION_IDLE_TIMEOUT_MINUTES'"
                    ),
                    "The idle timeout parameter must exist exactly once"
            );
            assertEquals(
                    "15",
                    queryString(
                            connection,
                            "SELECT giaTri FROM ThamSoHeThong "
                                    + "WHERE maThamSo = 'SESSION_IDLE_TIMEOUT_MINUTES'"
                    ),
                    "The idle timeout parameter must retain its approved default"
            );
            assertEquals(
                    1,
                    queryInt(
                            connection,
                            "SELECT COUNT(*) FROM ThamSoHeThong "
                                    + "WHERE maThamSo = 'SESSION_ABSOLUTE_TIMEOUT_HOURS'"
                    ),
                    "The absolute timeout parameter must exist exactly once"
            );
            assertEquals(
                    "12",
                    queryString(
                            connection,
                            "SELECT giaTri FROM ThamSoHeThong "
                                    + "WHERE maThamSo = 'SESSION_ABSOLUTE_TIMEOUT_HOURS'"
                    ),
                    "The absolute timeout parameter must retain its approved default"
            );
            assertSeedCount(connection, "ThamSoHeThong", 6);
            assertEquals(
                    22,
                    queryInt(
                            connection,
                            "SELECT COUNT(*) FROM sys.columns c "
                                    + "JOIN sys.tables t ON t.object_id = c.object_id "
                                    + "WHERE t.name IN ('NhatKyHeThong', 'NhatKyHoSoBenhAn') "
                                    + "AND c.name IN ('maVaiTro', 'maVaiTroCode', 'maCoSo', 'maKhoa', "
                                    + "'loaiDoiTuong', 'maDoiTuong', 'ketQua', 'maLyDo', 'workstationId', "
                                    + "'correlationId', 'occurredAtUtc')"
                    ),
                    "V6 must add nullable metadata columns to both audit tables"
            );
            assertEquals(
                    2,
                    queryInt(
                            connection,
                            "SELECT COUNT(*) FROM sys.triggers tr "
                                    + "JOIN sys.tables t ON t.object_id = tr.parent_id "
                                    + "WHERE t.name IN ('NhatKyHeThong', 'NhatKyHoSoBenhAn') "
                                    + "AND tr.name IN ('TR_NhatKyHeThong_AppendOnly', "
                                    + "'TR_NhatKyHoSoBenhAn_AppendOnly') AND tr.is_disabled = 0"
                    ),
                    "V6 must enable append-only triggers on both audit tables"
            );
            assertEquals(
                    4,
                    queryInt(
                            connection,
                            "SELECT COUNT(*) FROM sys.trigger_events e "
                                    + "JOIN sys.triggers tr ON tr.object_id = e.object_id "
                                    + "WHERE tr.name IN ('TR_NhatKyHeThong_AppendOnly', "
                                    + "'TR_NhatKyHoSoBenhAn_AppendOnly') "
                                    + "AND e.type_desc IN ('UPDATE', 'DELETE')"
                    ),
                    "Each append-only trigger must reject both UPDATE and DELETE"
            );

            assertAppendOnlyTriggersRejectPayloadChangesAndDeletion(connection);
        } catch (SQLException exception) {
            throw new AssertionError(
                    "Could not verify the SQL Server migration results; connection details were suppressed"
            );
        }
    }

    private static void assertAppendOnlyTriggersRejectPayloadChangesAndDeletion(Connection connection)
            throws SQLException {
        int auditId;
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                    "INSERT INTO NhatKyHeThong (maNhanVien, hanhDong, chiTiet) "
                            + "OUTPUT INSERTED.id VALUES (NULL, 'SYNTHETIC_APPEND_ONLY_TEST', 'synthetic payload')")) {
                if (!resultSet.next()) throw new SQLException("Synthetic audit insert returned no identity.");
                auditId = resultSet.getInt(1);
            }
        }

        try (Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "UPDATE NhatKyHeThong SET chiTiet = 'tampered synthetic payload' WHERE id = " + auditId));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "DELETE FROM NhatKyHeThong WHERE id = " + auditId));
        }

        assertEquals(1, queryInt(connection,
                "SELECT COUNT(*) FROM NhatKyHeThong WHERE id = " + auditId
                        + " AND chiTiet = 'synthetic payload'"),
                "Append-only trigger must preserve synthetic payload and row after rejected mutations");
    }

    private static Properties loadLocalProperties() {
        Path configPath = Path.of("src", "main", "resources", "db.properties");
        Properties properties = new Properties();
        try (var input = Files.newInputStream(configPath)) {
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new AssertionError(
                    "Could not load the ignored local SQL Server configuration after opt-in"
            );
        }
    }

    private static String requiredProperty(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("The ignored local SQL Server configuration is incomplete");
        }
        return value;
    }

    private static String selectDatabase(String baseUrl, String databaseName) {
        if (!baseUrl.startsWith("jdbc:sqlserver:")
                || EMBEDDED_CREDENTIAL_PATTERN.matcher(baseUrl).find()) {
            throw new IllegalArgumentException(
                    "db.url must be a SQL Server URL without embedded credentials"
            );
        }
        String serverUrl = DATABASE_OPTION_PATTERN.matcher(baseUrl).replaceAll("")
                .replaceFirst(";+$", "");
        return serverUrl + ";databaseName=" + databaseName;
    }

    private static boolean runMigrationsWithoutConsoleOutput() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try (PrintStream suppressed = new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8)) {
            System.setOut(suppressed);
            System.setErr(suppressed);
            try {
                return DatabaseMigrationRunner.runMigrations();
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
        }
    }

    private static List<String> queryMigrationRecords(Connection connection) throws SQLException {
        List<String> scriptNames = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT script_name FROM __schema_migrations WHERE success = 1 ORDER BY script_name"
             )) {
            while (resultSet.next()) {
                scriptNames.add(resultSet.getString(1));
            }
        }
        return scriptNames;
    }

    private static void assertSeedCount(Connection connection, String tableName, int expectedCount)
            throws SQLException {
        assertEquals(
                expectedCount,
                queryInt(connection, "SELECT COUNT(*) FROM " + tableName),
                "Unexpected deterministic seed count in " + tableName
        );
    }

    private static int queryInt(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                throw new SQLException("Count query returned no result");
            }
            return resultSet.getInt(1);
        }
    }

    private static String queryString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                throw new SQLException("Database selection query returned no result");
            }
            return resultSet.getString(1);
        }
    }
}
