package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreshDatabaseMigrationSqlServerTest {

    private static final String V8_SCRIPT_PATH = "migrations/V8__Seed_Approved_Permission_Grants.sql";
    private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile("clinic_test_[A-Za-z0-9_]+");
    private static final Pattern DATABASE_OPTION_PATTERN =
            Pattern.compile("(?i);\\s*(?:databaseName|database)\\s*=[^;]*");
    private static final Pattern EMBEDDED_CREDENTIAL_PATTERN =
            Pattern.compile("(?i);\\s*(?:user|password)\\s*=");

    @Test
    void migratesOnlyAnExplicitClinicTestDatabaseAndVerifiesV8Grants() throws Exception {
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

        if (!DatabaseMigrationRunner.runMigrations()) {
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
                            "V7__Harden_Permission_Matrix_Defaults.sql",
                            "V8__Seed_Approved_Permission_Grants.sql"
                    ),
                    queryMigrationRecords(connection),
                    "Fresh database must record migrations V1-V8"
            );

            Set<ApprovedPermissionGrantsMigrationTest.GrantKey> grantsBeforeReplay =
                    queryActiveGrants(connection);
            assertEquals(ApprovedPermissionGrantsMigrationTest.approvedGrants(), grantsBeforeReplay);
            assertEquals(57, queryInt(connection,
                    "SELECT COUNT(*) FROM dbo.MaTranPhanQuyen WHERE isDeleted = 0"));
            assertEquals(
                    0,
                    queryInt(connection,
                            "SELECT COUNT(*) FROM dbo.MaTranPhanQuyen "
                                    + "WHERE isDeleted = 0 AND duocPhep = 1 "
                                    + "AND (phamViDuLieu IS NULL OR phamViDuLieu <> 'KHOA_PHONG')"),
                    "Every active ALLOW must use KHOA_PHONG"
            );
            assertEquals(
                    0,
                    queryInt(connection,
                            "SELECT COUNT(*) FROM ("
                                    + "SELECT maVaiTro, phanHe, hanhDong FROM dbo.MaTranPhanQuyen "
                                    + "WHERE isDeleted = 0 GROUP BY maVaiTro, phanHe, hanhDong "
                                    + "HAVING COUNT_BIG(*) > 1) duplicates"),
                    "There must be no duplicate active role/module/action tuple"
            );
            assertEquals(
                    1,
                    queryInt(connection,
                            "SELECT COUNT(*) FROM sys.check_constraints "
                                    + "WHERE parent_object_id = OBJECT_ID('dbo.MaTranPhanQuyen') "
                                    + "AND name = 'CK_MaTranPhanQuyen_ActiveAllowScope' "
                                    + "AND is_disabled = 0 AND is_not_trusted = 0"),
                    "The trusted active-ALLOW scope constraint must remain enabled"
            );
            assertEquals(
                    1,
                    queryInt(connection,
                            "SELECT COUNT(*) FROM sys.indexes "
                                    + "WHERE object_id = OBJECT_ID('dbo.MaTranPhanQuyen') "
                                    + "AND name = 'UX_MaTranPhanQuyen_Active_Role_Module_Action' "
                                    + "AND is_unique = 1 AND has_filter = 1 "
                                    + "AND filter_definition LIKE '%isDeleted%0%'"),
                    "The filtered unique active role/module/action index must exist"
            );
            assertEquals(
                    0,
                    queryInt(connection,
                            "SELECT COUNT(*) FROM dbo.MaTranPhanQuyen p "
                                    + "JOIN dbo.VaiTro r ON r.id = p.maVaiTro "
                                    + "WHERE p.isDeleted = 0 AND p.duocPhep = 1 AND ("
                                    + "(r.tenVaiTro IN ('ADMIN', 'DOCTOR', 'RECEPTIONIST', "
                                    + "'PHARMACIST', 'CASHIER', 'LAB_TECH') "
                                    + "AND p.phanHe = 'AUDIT' AND p.hanhDong = 'AUDIT_READ') "
                                    + "OR (p.phanHe = 'CLS' AND p.hanhDong = 'DUYET') "
                                    + "OR (p.phanHe = 'PRESCRIPTION' AND p.hanhDong = 'DUYET'))"),
                    "AUDIT_READ and unresolved CLS/PRESCRIPTION approvals must remain default-deny"
            );

            DatabaseMigrationRunner.executeSqlScript(connection, readV8Script());
            assertEquals(grantsBeforeReplay, queryActiveGrants(connection),
                    "Reapplying V8 must not change the active grants");
        } catch (SQLException exception) {
            throw new AssertionError(
                    "Could not verify the V8 migration results; connection details were suppressed"
            );
        }
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

    private static Set<ApprovedPermissionGrantsMigrationTest.GrantKey> queryActiveGrants(
            Connection connection) throws SQLException {
        Set<ApprovedPermissionGrantsMigrationTest.GrantKey> grants = new java.util.HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT r.tenVaiTro, p.phanHe, p.hanhDong, p.phamViDuLieu, p.duocPhep "
                             + "FROM dbo.MaTranPhanQuyen p "
                             + "JOIN dbo.VaiTro r ON r.id = p.maVaiTro "
                             + "WHERE p.isDeleted = 0 ORDER BY r.tenVaiTro, p.phanHe, p.hanhDong"
             )) {
            while (resultSet.next()) {
                assertEquals(PhamViDuLieu.KHOA_PHONG,
                        PhamViDuLieu.valueOf(resultSet.getString("phamViDuLieu")));
                assertTrue(resultSet.getBoolean("duocPhep"));
                grants.add(new ApprovedPermissionGrantsMigrationTest.GrantKey(
                        resultSet.getString("tenVaiTro"),
                        resultSet.getString("phanHe"),
                        resultSet.getString("hanhDong")
                ));
            }
        }
        return grants;
    }

    private static String readV8Script() throws IOException {
        try (InputStream input = FreshDatabaseMigrationSqlServerTest.class.getClassLoader()
                .getResourceAsStream(V8_SCRIPT_PATH)) {
            if (input == null) throw new AssertionError("Missing migration script: " + V8_SCRIPT_PATH);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
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
