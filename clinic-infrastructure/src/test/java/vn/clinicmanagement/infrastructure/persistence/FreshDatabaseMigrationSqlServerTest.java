package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        runMigrationsThroughV8();
        assertLegacyDuplicatePreflightAndRetry();

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
                            "V8__Seed_Approved_Permission_Grants.sql",
                            "V9__Enforce_Active_Patient_Facility_Uniqueness.sql",
                            "V10__Add_TienSuBenh_Facility_Department_Scope.sql"
                    ),
                    queryMigrationRecords(connection),
                    "All V1-V10 migrations must be recorded as successful"
            );

            assertEquals(1, queryInt(
                    connection,
                    "SELECT COUNT(*) FROM sys.tables WHERE name = 'CoSo'"
            ), "V1 must create the representative CoSo table");
            assertEquals(1, queryInt(
                    connection,
                    "SELECT COUNT(*) FROM sys.indexes WHERE object_id = OBJECT_ID('BenhNhanCoSo') " +
                            "AND name = 'UX_BenhNhanCoSo_Active_Patient_Facility' AND is_unique = 1 AND has_filter = 1"
            ), "V9 must enforce unique active patient-facility links");
            assertV9ActivePatientFacilityUniqueness(connection);
            assertV10ScopeSchema(connection);
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
            assertSeedCount(connection, "MaTranPhanQuyen", 57);
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

            Set<ApprovedPermissionGrantsMigrationTest.GrantKey> grantsBeforeReplay =
                    queryActiveGrants(connection);
            assertEquals(ApprovedPermissionGrantsMigrationTest.approvedGrants(), grantsBeforeReplay,
                    "V8 must persist exactly the approved grant allowlist");
            assertEquals(0, queryInt(connection,
                    "SELECT COUNT(*) FROM MaTranPhanQuyen WHERE isDeleted = 0 AND duocPhep = 1 "
                            + "AND (phamViDuLieu IS NULL OR phamViDuLieu <> 'KHOA_PHONG')"),
                    "Every seeded ALLOW must be scoped to KHOA_PHONG");
            assertEquals(0, queryInt(connection,
                    "SELECT COUNT(*) FROM MaTranPhanQuyen WHERE isDeleted = 0 "
                            + "AND ((phanHe = 'AUDIT' AND hanhDong = 'AUDIT_READ') "
                            + "OR (phanHe = 'CLS' AND hanhDong = 'DUYET') "
                            + "OR (phanHe = 'PRESCRIPTION' AND hanhDong = 'DUYET'))"),
                    "AUDIT_READ and unresolved approval actions must remain default-deny");
            assertEquals(0, queryInt(connection,
                    "SELECT COUNT(*) FROM MaTranPhanQuyen WHERE isDeleted = 0 "
                            + "AND hanhDong IN ('CHO_KHAM', 'TAM', 'DA_DUYET')"),
                    "Workflow statuses must not be seeded as authorization actions");

            DatabaseMigrationRunner.executeSqlScript(connection,
                    readMigrationScript("migrations/V8__Seed_Approved_Permission_Grants.sql"));
            assertEquals(grantsBeforeReplay, queryActiveGrants(connection),
                    "Replaying V8 must leave existing permission rows unchanged");

            assertAppendOnlyTriggersRejectPayloadChangesAndDeletion(connection);
        } catch (SQLException exception) {
            throw new AssertionError(
                    "Could not verify the SQL Server migration results; connection details were suppressed"
            );
        }
    }

    private static void runMigrationsThroughV8() {
        List<String> migrationNames = List.of(
                "V1__Initial_Clinic_Schema.sql",
                "V2__Seed_Initial_Data.sql",
                "V3__Repair_Demo_Account_Passwords.sql",
                "V4__Enforce_Active_Patient_CCCD_Uniqueness.sql",
                "V5__Seed_Session_Timeout_Parameters.sql",
                "V6__Enhance_Audit_Tables_Append_Only.sql",
                "V7__Harden_Permission_Matrix_Defaults.sql",
                "V8__Seed_Approved_Permission_Grants.sql"
        );

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("CREATE TABLE __schema_migrations ("
                    + "id INT IDENTITY(1,1) PRIMARY KEY, "
                    + "script_name NVARCHAR(255) NOT NULL UNIQUE, "
                    + "installed_on DATETIME2 NOT NULL DEFAULT GETDATE(), "
                    + "execution_time_ms BIGINT NOT NULL, "
                    + "success BIT NOT NULL DEFAULT 1)");
            connection.commit();

            for (String migrationName : migrationNames) {
                DatabaseMigrationRunner.executeMigration(
                        connection,
                        migrationName,
                        readMigrationScript("migrations/" + migrationName),
                        0L
                );
            }
        } catch (SQLException | IOException exception) {
            throw new AssertionError("Could not apply V1-V8 to the selected SQL Server test database");
        }
    }

    private static void assertLegacyDuplicatePreflightAndRetry() throws SQLException {
        String syntheticSuffix = UUID.randomUUID().toString();
        int facilityId = -1;
        int patientId = -1;
        int firstLinkId = -1;
        int secondLinkId = -1;
        int legacyHistoryId = -1;
        try {
            try (Connection connection = DatabaseConnection.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO CoSo (tenCoSo) OUTPUT INSERTED.id VALUES (?)")) {
                    statement.setString(1, "Synthetic PAT-V9 preflight " + syntheticSuffix);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Synthetic facility insert returned no identity.");
                        }
                        facilityId = resultSet.getInt(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO BenhNhan (ho, ten, ngaySinh) OUTPUT INSERTED.id "
                                + "VALUES (?, ?, '2000-01-01')")) {
                    statement.setString(1, "Synthetic");
                    statement.setString(2, "PATV9" + syntheticSuffix.replace("-", ""));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Synthetic patient insert returned no identity.");
                        }
                        patientId = resultSet.getInt(1);
                    }
                }

                firstLinkId = insertPatientFacilityLink(connection, patientId, facilityId);
                secondLinkId = insertPatientFacilityLink(connection, patientId, facilityId);
                assertEquals(2, countSyntheticActiveLinks(connection, firstLinkId, secondLinkId),
                        "The generated legacy fixture must contain two active duplicate links before V9");
                assertEquals(List.of(
                                "V1__Initial_Clinic_Schema.sql",
                                "V2__Seed_Initial_Data.sql",
                                "V3__Repair_Demo_Account_Passwords.sql",
                                "V4__Enforce_Active_Patient_CCCD_Uniqueness.sql",
                                "V5__Seed_Session_Timeout_Parameters.sql",
                                "V6__Enhance_Audit_Tables_Append_Only.sql",
                                "V7__Harden_Permission_Matrix_Defaults.sql",
                                "V8__Seed_Approved_Permission_Grants.sql"),
                        queryMigrationRecords(connection), "Only V1-V8 must be successful before the V9 attempt");
            }

            assertFalse(runMigrationsWithoutConsoleOutput(),
                    "The real migration runner must fail V9 when active legacy duplicates exist");
            try (Connection connection = DatabaseConnection.getConnection()) {
                assertEquals(0, queryInt(connection,
                                "SELECT COUNT(*) FROM __schema_migrations "
                                        + "WHERE script_name = 'V9__Enforce_Active_Patient_Facility_Uniqueness.sql' "
                                        + "AND success = 1"),
                        "A failed V9 attempt must not create a successful tracking record");
                assertEquals(0, queryInt(connection,
                                "SELECT COUNT(*) FROM sys.indexes WHERE object_id = OBJECT_ID('BenhNhanCoSo') "
                                        + "AND name = 'UX_BenhNhanCoSo_Active_Patient_Facility'"),
                        "A failed V9 attempt must leave no filtered unique index behind");
                assertEquals(2, countSyntheticActiveLinks(connection, firstLinkId, secondLinkId),
                        "A failed V9 preflight must preserve both generated duplicate rows");
            }

            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE BenhNhanCoSo SET isDeleted = 1 WHERE id = ? AND isDeleted = 0")) {
                statement.setInt(1, secondLinkId);
                assertEquals(1, statement.executeUpdate(),
                        "Only the second generated duplicate link should be soft-deleted for retry");
            }
            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO TienSuBenh (maBenhNhan, loaiTienSu, moTa) "
                                 + "OUTPUT INSERTED.id VALUES (?, 'BENH_NEN', 'Synthetic legacy history')")) {
                statement.setInt(1, patientId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) throw new SQLException("Synthetic legacy history insert returned no identity.");
                    legacyHistoryId = resultSet.getInt(1);
                }
            }
            assertTrue(runMigrationsWithoutConsoleOutput(),
                    "The real migration runner must apply V9 and V10 after the synthetic duplicate is resolved");
            try (Connection connection = DatabaseConnection.getConnection()) {
                assertEquals(0, queryInt(connection,
                                "SELECT COUNT(*) FROM TienSuBenh WHERE id = " + legacyHistoryId
                                        + " AND (maCoSo IS NOT NULL OR maKhoa IS NOT NULL)"),
                        "V10 must leave legacy history ownership NULL rather than infer a scope");
                assertV10RejectsMismatchedFacilityDepartment(connection, patientId, facilityId);
            }
        } finally {
            if (legacyHistoryId > 0) {
                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "DELETE FROM TienSuBenh WHERE id = ?")) {
                    statement.setInt(1, legacyHistoryId);
                    statement.executeUpdate();
                }
            }
            cleanupSyntheticPatientFacilityFixture(patientId, facilityId, firstLinkId, secondLinkId);
        }
    }

    private static void assertV10ScopeSchema(Connection connection) throws SQLException {
        assertEquals(2, queryInt(connection,
                "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.TienSuBenh') "
                        + "AND name IN ('maCoSo', 'maKhoa') AND is_nullable = 1"),
                "V10 must add both nullable ownership columns");
        assertEquals(1, queryInt(connection,
                "SELECT COUNT(*) FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.TienSuBenh') "
                        + "AND name = 'CK_TienSuBenh_Facility_Department_Pair' AND is_disabled = 0 AND is_not_trusted = 0"),
                "V10 must enforce the all-null or both-present pair");
        assertEquals(1, queryInt(connection,
                "SELECT COUNT(*) FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('dbo.TienSuBenh') "
                        + "AND name = 'FK_TienSuBenh_Khoa_Facility' AND is_disabled = 0 AND is_not_trusted = 0"),
                "V10 must enforce that the department belongs to the stored facility");
        assertEquals(1, queryInt(connection,
                "SELECT COUNT(*) FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.TienSuBenh') "
                        + "AND name = 'IX_TienSuBenh_Facility_Department_Patient'"),
                "V10 must add a history scope lookup index");
    }

    private static void assertV10RejectsMismatchedFacilityDepartment(
            Connection connection, int patientId, int firstFacilityId) throws SQLException {
        int secondFacilityId = -1;
        int secondDepartmentId = -1;
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO CoSo (tenCoSo) OUTPUT INSERTED.id VALUES ('Synthetic PAT-V10 pair fixture')")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) throw new SQLException("Synthetic facility insert returned no identity.");
                    secondFacilityId = resultSet.getInt(1);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO Khoa (maCoSo, tenKhoa) OUTPUT INSERTED.id VALUES (?, 'Synthetic PAT-V10 department')")) {
                statement.setInt(1, secondFacilityId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) throw new SQLException("Synthetic department insert returned no identity.");
                    secondDepartmentId = resultSet.getInt(1);
                }
            }
            int mismatchingDepartmentId = secondDepartmentId;
            SQLException mismatch = assertThrows(SQLException.class, () -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO TienSuBenh (maBenhNhan, loaiTienSu, moTa, maCoSo, maKhoa) "
                                + "VALUES (?, 'BENH_NEN', 'Synthetic mismatched scope', ?, ?)")) {
                    statement.setInt(1, patientId);
                    statement.setInt(2, firstFacilityId);
                    statement.setInt(3, mismatchingDepartmentId);
                    statement.executeUpdate();
                }
            }, "V10 must reject a department owned by a different facility");
            assertTrue(mismatch.getErrorCode() == 547 || mismatch.getErrorCode() == 23000,
                    "The mismatch must fail as a SQL Server integrity violation");
        } finally {
            if (secondDepartmentId > 0) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM Khoa WHERE id = ?")) {
                    statement.setInt(1, secondDepartmentId);
                    statement.executeUpdate();
                }
            }
            if (secondFacilityId > 0) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM CoSo WHERE id = ?")) {
                    statement.setInt(1, secondFacilityId);
                    statement.executeUpdate();
                }
            }
        }
    }

    private static int countSyntheticActiveLinks(Connection connection, int firstLinkId, int secondLinkId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM BenhNhanCoSo WHERE id IN (?, ?) AND isDeleted = 0")) {
            statement.setInt(1, firstLinkId);
            statement.setInt(2, secondLinkId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Synthetic duplicate count returned no result.");
                return resultSet.getInt(1);
            }
        }
    }

    private static void cleanupSyntheticPatientFacilityFixture(
            int patientId, int facilityId, int firstLinkId, int secondLinkId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM BenhNhanCoSo WHERE id IN (?, ?)")) {
                if (firstLinkId > 0) statement.setInt(1, firstLinkId);
                else statement.setNull(1, java.sql.Types.INTEGER);
                if (secondLinkId > 0) statement.setInt(2, secondLinkId);
                else statement.setNull(2, java.sql.Types.INTEGER);
                statement.executeUpdate();
            }
            if (patientId > 0) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM BenhNhan WHERE id = ?")) {
                    statement.setInt(1, patientId);
                    statement.executeUpdate();
                }
            }
            if (facilityId > 0) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM CoSo WHERE id = ?")) {
                    statement.setInt(1, facilityId);
                    statement.executeUpdate();
                }
            }
        }
    }

    private static void assertV9ActivePatientFacilityUniqueness(Connection connection) throws SQLException {
        String syntheticSuffix = UUID.randomUUID().toString();
        int facilityId = -1;
        int patientId = -1;
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO CoSo (tenCoSo) OUTPUT INSERTED.id VALUES (?)")) {
                statement.setString(1, "Synthetic PAT-02 " + syntheticSuffix);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) throw new SQLException("Synthetic facility insert returned no identity.");
                    facilityId = resultSet.getInt(1);
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO BenhNhan (ho, ten, ngaySinh) OUTPUT INSERTED.id VALUES (?, ?, '2000-01-01')")) {
                statement.setString(1, "Synthetic");
                statement.setString(2, "PAT02" + syntheticSuffix.replace("-", ""));
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) throw new SQLException("Synthetic patient insert returned no identity.");
                    patientId = resultSet.getInt(1);
                }
            }

            int syntheticPatientId = patientId;
            int syntheticFacilityId = facilityId;
            int firstLinkId = insertPatientFacilityLink(connection, syntheticPatientId, syntheticFacilityId);
            SQLException duplicateFailure = assertThrows(SQLException.class,
                    () -> insertPatientFacilityLink(connection, syntheticPatientId, syntheticFacilityId),
                    "V9 must reject a second active link for the same patient-facility pair");
            assertEquals(2601, duplicateFailure.getErrorCode(),
                    "The duplicate must fail with SQL Server's unique-index violation code");
            assertEquals(1, queryInt(connection,
                    "SELECT COUNT(*) FROM BenhNhanCoSo WHERE maBenhNhan = " + patientId
                            + " AND maCoSo = " + facilityId + " AND isDeleted = 0"),
                    "The rejected duplicate must not create another active link");

            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE BenhNhanCoSo SET isDeleted = 1 WHERE id = ? AND isDeleted = 0")) {
                statement.setInt(1, firstLinkId);
                assertEquals(1, statement.executeUpdate(), "The synthetic link must be soft-deleted");
            }

            int replacementLinkId = insertPatientFacilityLink(connection, syntheticPatientId, syntheticFacilityId);
            assertTrue(replacementLinkId != firstLinkId,
                    "A new active link must be inserted after the prior link is soft-deleted");
            assertEquals(1, queryInt(connection,
                    "SELECT COUNT(*) FROM BenhNhanCoSo WHERE maBenhNhan = " + patientId
                            + " AND maCoSo = " + facilityId + " AND isDeleted = 0"),
                    "A soft-deleted link must not block one replacement active link");
            assertEquals(2, queryInt(connection,
                    "SELECT COUNT(*) FROM BenhNhanCoSo WHERE maBenhNhan = " + patientId
                            + " AND maCoSo = " + facilityId),
                    "The soft-deleted link and its active replacement must both remain recorded");
        } finally {
            if (patientId > 0 && facilityId > 0) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM BenhNhanCoSo WHERE maBenhNhan = ? AND maCoSo = ?")) {
                    statement.setInt(1, patientId);
                    statement.setInt(2, facilityId);
                    statement.executeUpdate();
                }
            }
            if (patientId > 0) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM BenhNhan WHERE id = ?")) {
                    statement.setInt(1, patientId);
                    statement.executeUpdate();
                }
            }
            if (facilityId > 0) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM CoSo WHERE id = ?")) {
                    statement.setInt(1, facilityId);
                    statement.executeUpdate();
                }
            }
        }
    }

    private static int insertPatientFacilityLink(Connection connection, int patientId, int facilityId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO BenhNhanCoSo (maBenhNhan, maCoSo) OUTPUT INSERTED.id VALUES (?, ?)")) {
            statement.setInt(1, patientId);
            statement.setInt(2, facilityId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Synthetic patient-facility insert returned no identity.");
                return resultSet.getInt(1);
            }
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

    private static Set<ApprovedPermissionGrantsMigrationTest.GrantKey> queryActiveGrants(Connection connection)
            throws SQLException {
        Set<ApprovedPermissionGrantsMigrationTest.GrantKey> grants = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT role.tenVaiTro, permission.phanHe, permission.hanhDong, "
                             + "permission.phamViDuLieu, permission.duocPhep "
                             + "FROM MaTranPhanQuyen permission "
                             + "JOIN VaiTro role ON role.id = permission.maVaiTro "
                             + "WHERE permission.isDeleted = 0")) {
            while (resultSet.next()) {
                assertEquals(PhamViDuLieu.KHOA_PHONG,
                        PhamViDuLieu.valueOf(resultSet.getString("phamViDuLieu")));
                assertTrue(resultSet.getBoolean("duocPhep"),
                        "Fresh database allowlist should contain only approved ALLOW rows");
                grants.add(new ApprovedPermissionGrantsMigrationTest.GrantKey(
                        resultSet.getString("tenVaiTro"),
                        resultSet.getString("phanHe"),
                        resultSet.getString("hanhDong")));
            }
        }
        return grants;
    }

    private static String readMigrationScript(String resourcePath) throws IOException {
        try (var input = FreshDatabaseMigrationSqlServerTest.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (input == null) throw new AssertionError("Missing migration script: " + resourcePath);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
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
