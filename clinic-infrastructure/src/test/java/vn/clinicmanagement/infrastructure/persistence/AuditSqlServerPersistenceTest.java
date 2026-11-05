package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.reception.GetReceptionQueueHandler;
import vn.clinicmanagement.application.reception.GetReceptionQueueQuery;
import vn.clinicmanagement.application.reception.PatientSearchItem;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.application.reception.RegisterPatientCommand;
import vn.clinicmanagement.application.reception.RegisterPatientHandler;
import vn.clinicmanagement.application.reception.RegistrationReceipt;
import vn.clinicmanagement.application.reception.SearchPatientsHandler;
import vn.clinicmanagement.application.reception.SearchPatientsQuery;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.infrastructure.persistence.facility.PhongKhamRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.patient.BaoHiemRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.patient.BenhNhanRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.reception.ReceptionQueueRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcAuditRepositoryImpl;

import java.io.IOException;
import java.io.InputStream;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditSqlServerPersistenceTest {

    private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile("clinic_test_[A-Za-z0-9_]+");
    private static final Pattern DATABASE_OPTION_PATTERN =
            Pattern.compile("(?i);\\s*(?:databaseName|database)\\s*=[^;]*");
    private static final Pattern EMBEDDED_CREDENTIAL_PATTERN =
            Pattern.compile("(?i);\\s*(?:user|username|" + "pass" + "word)\\s*=");
    private static final String TEMPORARY_REJECT_TRIGGER = "TR_NhatKyHeThong_RejectSyntheticPatientCreated";

    @Test
    void verifiesV6AppendOnlyAndRollsBackSyntheticReceptionRegistrationWhenPatientCreatedAuditInsertIsRejected()
            throws Exception {
        Assumptions.assumeTrue(
                isOptInEnabled(),
                "Set clinic.sqlserver.audit.enabled=true or clinic.test.sqlserver=true to opt in"
        );

        String databaseName = resolveDatabaseName();
        Assumptions.assumeTrue(
                databaseName != null && DATABASE_NAME_PATTERN.matcher(databaseName).matches(),
                "Set clinic.sqlserver.test.database or clinic.test.sqlserver.databaseName to a clinic_test_* database"
        );

        configureSyntheticDatabase(databaseName);

        try (Connection connection = DatabaseConnection.getConnection()) {
            assertEquals(
                    databaseName,
                    queryString(connection, "SELECT DB_NAME()"),
                    "The SQL Server connection must select only the explicitly named clinic_test_* database"
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
            assertV6MigrationAndAppendOnlyBehavior(connection);
        } catch (SQLException exception) {
            throw new AssertionError(
                    "Could not verify V6 append-only migration behavior; connection details were suppressed"
            );
        }

        String generatedSuffix = Long.toString(ThreadLocalRandom.current().nextLong(10_000_000L, 100_000_000L));
        String syntheticCccd = "0000" + generatedSuffix;
        String syntheticPhone = "09" + generatedSuffix;
        String syntheticCardNumber = "SYNTH" + generatedSuffix;
        UUID correlationId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        ReceptionServices services = ReceptionServices.create();
        PhongKham room = services.rooms().getActiveRooms().stream().findFirst()
                .orElseThrow(() -> new AssertionError("No active seeded clinic room is available"));
        AuditActorContext actorContext = resolveSeededActorContext(room, correlationId);

        BenhNhan patient = new BenhNhan();
        patient.setHo("Synthetic");
        patient.setTen("AuditRollback" + generatedSuffix);
        patient.setNgaySinh(LocalDate.of(1990, 1, 2));
        patient.setGioiTinh(GioiTinh.M);
        patient.setDienThoai(syntheticPhone);
        patient.setSoCCCD(syntheticCccd);
        patient.setDiaChi("Synthetic audit rollback address");
        patient.setCreatedBy(actorContext.employeeId());

        BaoHiem insurance = new BaoHiem();
        insurance.setLoaiBaoHiem(LoaiBaoHiem.BHYT);
        insurance.setSoBaoHiem(syntheticCardNumber);
        insurance.setNgayBatDau(today.minusDays(1));
        insurance.setNgayHetHan(today.plusYears(1));
        insurance.setMucHuong(80);
        insurance.setCreatedBy(actorContext.employeeId());

        Integer generatedPatientIdBeforeRollback;
        try {
            installTemporaryPatientCreatedRejectTrigger(correlationId);

            Result<RegistrationReceipt> registrationResult = services.registerPatient().handle(
                    new RegisterPatientCommand(
                            patient,
                            insurance,
                            room.getId(),
                            "WALK_IN",
                            "Synthetic audit rollback test",
                            DoiTuongKham.BHYT,
                            false,
                            actorContext.employeeId(),
                            actorContext
                    )
            );

            assertTrue(
                    registrationResult.isFailure(),
                    "Reception registration must fail when PATIENT_CREATED audit insert is rejected"
            );
            assertEquals(
                    "PATIENT_REGISTRATION_FAILED",
                    registrationResult.getError().orElseThrow().code(),
                    "Rejected audit insert must surface the safe registration failure code"
            );
            String errorMessage = registrationResult.getError().orElseThrow().message();
            assertFalse(errorMessage.contains(syntheticCccd), "Failure message must not expose CCCD");
            assertFalse(errorMessage.contains(syntheticCardNumber), "Failure message must not expose BHYT");
            assertFalse(
                    errorMessage.contains("Synthetic audit rollback test"),
                    "Failure message must not expose free-text reason"
            );

            generatedPatientIdBeforeRollback = patient.getId();
            assertNotNull(
                    generatedPatientIdBeforeRollback,
                    "Patient insert must assign a synthetic ID before the PATIENT_CREATED audit insert is rejected"
            );
            assertTrue(
                    generatedPatientIdBeforeRollback > 0,
                    "Generated patient ID prior to transaction rollback must be positive"
            );

            try (Connection connection = DatabaseConnection.getConnection()) {
                assertEquals(
                        0,
                        queryCountByIntOrString(
                                connection,
                                "SELECT COUNT(*) FROM BenhNhan WHERE id = ? OR soCCCD = ?",
                                generatedPatientIdBeforeRollback,
                                syntheticCccd
                        ),
                        "Patient row must be rolled back when PATIENT_CREATED audit insert fails"
                );
                assertEquals(
                        0,
                        queryCountByIntOrString(
                                connection,
                                "SELECT COUNT(*) FROM BaoHiem WHERE maBenhNhan = ? OR soBaoHiem = ?",
                                generatedPatientIdBeforeRollback,
                                syntheticCardNumber
                        ),
                        "Insurance row must remain absent when PATIENT_CREATED audit insert fails"
                );
                assertEquals(
                        0,
                        queryCountByInt(
                                connection,
                                "SELECT COUNT(*) FROM LuotKham WHERE maBenhNhan = ?",
                                generatedPatientIdBeforeRollback
                        ),
                        "Visit row must remain absent when PATIENT_CREATED audit insert fails"
                );
                assertEquals(
                        0,
                        queryCountByString(
                                connection,
                                "SELECT COUNT(*) FROM NhatKyHeThong WHERE correlationId = ?",
                                correlationId.toString()
                        ),
                        "Rejected PATIENT_CREATED audit row must not persist"
                );
            }

            ReceptionServices freshServices = ReceptionServices.create();
            List<PatientSearchItem> matches = freshServices.searchPatients()
                    .handle(new SearchPatientsQuery(syntheticCccd))
                    .valueOrThrow();
            assertTrue(matches.isEmpty(), "Rolled-back synthetic patient must not appear in reception search");

            List<ReceptionQueueItem> queue = freshServices.getQueue()
                    .handle(new GetReceptionQueueQuery(today))
                    .valueOrThrow()
                    .queue();
            assertFalse(
                    queue.stream().anyMatch(item -> item.patientId() == generatedPatientIdBeforeRollback),
                    "Rolled-back synthetic registration must not appear in the reception queue"
            );
        } finally {
            dropTemporaryPatientCreatedRejectTrigger();
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            assertEquals(
                    0,
                    queryCountByString(
                            connection,
                            "SELECT COUNT(*) FROM sys.triggers WHERE name = ?",
                            TEMPORARY_REJECT_TRIGGER
                    ),
                    "Temporary rejecting trigger must be removed in finally"
            );
        }
    }

    private static boolean isOptInEnabled() {
        return Boolean.parseBoolean(System.getProperty("clinic.sqlserver.audit.enabled"))
                || Boolean.parseBoolean(System.getProperty("clinic.test.sqlserver"))
                || Boolean.parseBoolean(System.getProperty("clinic.sqlserver.migration.enabled"));
    }

    private static String resolveDatabaseName() {
        for (String key : List.of(
                "clinic.sqlserver.test.database",
                "clinic.test.sqlserver.databaseName",
                "clinic.sqlserver.audit.database")) {
            String value = System.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static void configureSyntheticDatabase(String databaseName) {
        if (databaseName == null || !DATABASE_NAME_PATTERN.matcher(databaseName).matches()) {
            throw new IllegalArgumentException(
                    "SQL Server test database must match clinic_test_[A-Za-z0-9_]+"
            );
        }
        Properties properties = loadLocalProperties();
        String baseUrl = requiredProperty(properties, "db.url").trim();
        String username = requiredProperty(properties, "db.username").trim();
        String credentialValue = requiredProperty(properties, "db." + "password");
        String selectedUrl = selectDatabase(baseUrl, databaseName);

        DatabaseConnection.setConnectionString("");
        DatabaseConnection.configure(selectedUrl, username, credentialValue);
    }

    private static Properties loadLocalProperties() {
        Properties properties = new Properties();
        try (InputStream classpathInput = AuditSqlServerPersistenceTest.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (classpathInput != null) {
                properties.load(classpathInput);
                return properties;
            }
        } catch (IOException exception) {
            throw new AssertionError(
                    "Could not load the ignored local SQL Server configuration after opt-in"
            );
        }

        for (Path candidate : List.of(
                Path.of("src", "main", "resources", "db.properties"),
                Path.of("clinic-infrastructure", "src", "main", "resources", "db.properties"))) {
            if (Files.isRegularFile(candidate)) {
                try (InputStream fileInput = Files.newInputStream(candidate)) {
                    properties.load(fileInput);
                    return properties;
                } catch (IOException exception) {
                    throw new AssertionError(
                            "Could not load the ignored local SQL Server configuration after opt-in"
                    );
                }
            }
        }
        throw new AssertionError(
                "Could not load the ignored local SQL Server configuration after opt-in"
        );
    }

    private static String requiredProperty(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("The ignored local SQL Server configuration is incomplete");
        }
        return value;
    }

    private static String selectDatabase(String baseUrl, String databaseName) {
        if (!baseUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:sqlserver:")
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

    private static void assertV6MigrationAndAppendOnlyBehavior(Connection connection) throws SQLException {
        assertEquals(
                List.of(
                        "V1__Initial_Clinic_Schema.sql",
                        "V2__Seed_Initial_Data.sql",
                        "V3__Repair_Demo_Account_Passwords.sql",
                        "V4__Enforce_Active_Patient_CCCD_Uniqueness.sql",
                        "V5__Seed_Session_Timeout_Parameters.sql",
                        "V6__Enhance_Audit_Tables_Append_Only.sql"
                ),
                queryMigrationRecords(connection),
                "All V1-V6 migrations must be recorded as successful"
        );
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
                "V6 must add metadata columns to both audit tables"
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

        int auditId;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "INSERT INTO NhatKyHeThong (maNhanVien, hanhDong, chiTiet) "
                             + "OUTPUT INSERTED.id VALUES (NULL, 'SYNTHETIC_APPEND_ONLY_TEST', 'synthetic payload')")) {
            if (!resultSet.next()) {
                throw new SQLException("Synthetic audit insert returned no identity");
            }
            auditId = resultSet.getInt(1);
        }

        try (Statement statement = connection.createStatement()) {
            assertThrows(
                    SQLException.class,
                    () -> statement.executeUpdate(
                            "UPDATE NhatKyHeThong SET chiTiet = 'tampered synthetic payload' WHERE id = " + auditId
                    )
            );
            assertThrows(
                    SQLException.class,
                    () -> statement.executeUpdate(
                            "UPDATE NhatKyHeThong SET isDeleted = 1, deletedAt = SYSDATETIME() WHERE id = " + auditId
                    )
            );
            assertThrows(
                    SQLException.class,
                    () -> statement.executeUpdate(
                            "DELETE FROM NhatKyHeThong WHERE id = " + auditId
                    )
            );
        }

        assertEquals(
                1,
                queryCountByInt(
                        connection,
                        "SELECT COUNT(*) FROM NhatKyHeThong WHERE id = ? AND chiTiet = 'synthetic payload' AND isDeleted = 0",
                        auditId
                ),
                "Append-only trigger must preserve the stored synthetic audit row after rejected UPDATE and DELETE"
        );
    }

    private static AuditActorContext resolveSeededActorContext(PhongKham room, UUID correlationId)
            throws SQLException {
        int facilityId = room.getKhoa().getCoSo().getId();
        int departmentId = room.getKhoa().getId();
        try (Connection connection = DatabaseConnection.getConnection()) {
            int employeeId = queryInt(
                    connection,
                    "SELECT TOP 1 id FROM NhanVien WHERE hoatDong = 1 AND isDeleted = 0 ORDER BY id"
            );
            int roleId = queryInt(
                    connection,
                    "SELECT TOP 1 id FROM VaiTro WHERE isDeleted = 0 "
                            + "ORDER BY CASE WHEN tenVaiTro = N'RECEPTIONIST' THEN 0 ELSE 1 END, id"
            );
            return new AuditActorContext(
                    employeeId,
                    roleId,
                    "RECEPTIONIST",
                    facilityId,
                    departmentId,
                    "synthetic-workstation",
                    correlationId
            );
        }
    }

    private static void installTemporaryPatientCreatedRejectTrigger(UUID correlationId) throws SQLException {
        String correlationLiteral = correlationId.toString();
        String triggerSql = "CREATE OR ALTER TRIGGER dbo." + TEMPORARY_REJECT_TRIGGER + "\n"
                + "ON dbo.NhatKyHeThong\n"
                + "AFTER INSERT\n"
                + "AS\n"
                + "BEGIN\n"
                + "    SET NOCOUNT ON;\n"
                + "    IF EXISTS (\n"
                + "        SELECT 1\n"
                + "        FROM inserted\n"
                + "        WHERE hanhDong = N'PATIENT_CREATED'\n"
                + "          AND correlationId = '" + correlationLiteral + "'\n"
                + "    )\n"
                + "    BEGIN\n"
                + "        THROW 51001, 'Synthetic PATIENT_CREATED audit insert rejected.', 1;\n"
                + "    END;\n"
                + "END;";
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(triggerSql);
        }
    }

    private static void dropTemporaryPatientCreatedRejectTrigger() throws SQLException {
        String dropSql = "IF OBJECT_ID(N'dbo." + TEMPORARY_REJECT_TRIGGER + "', N'TR') IS NOT NULL "
                + "DROP TRIGGER dbo." + TEMPORARY_REJECT_TRIGGER + ";";
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(dropSql);
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

    private static int queryInt(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                throw new SQLException("Scalar int query returned no result");
            }
            return resultSet.getInt(1);
        }
    }

    private static String queryString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                throw new SQLException("Scalar string query returned no result");
            }
            return resultSet.getString(1);
        }
    }

    private static int queryCountByInt(Connection connection, String sql, int value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Count query returned no result");
                }
                return resultSet.getInt(1);
            }
        }
    }

    private static int queryCountByString(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Count query returned no result");
                }
                return resultSet.getInt(1);
            }
        }
    }

    private static int queryCountByIntOrString(Connection connection, String sql, int id, String code)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Count query returned no result");
                }
                return resultSet.getInt(1);
            }
        }
    }

    private record ReceptionServices(
            PhongKhamRepositoryImpl rooms,
            RegisterPatientHandler registerPatient,
            SearchPatientsHandler searchPatients,
            GetReceptionQueueHandler getQueue) {

        private static ReceptionServices create() {
            JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
            JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
            JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
            BenhNhanRepositoryImpl patients = new BenhNhanRepositoryImpl(connections, unitOfWork);
            BaoHiemRepositoryImpl insurance = new BaoHiemRepositoryImpl(connections, unitOfWork);
            PhongKhamRepositoryImpl rooms = new PhongKhamRepositoryImpl(connections);
            ReceptionQueueRepositoryImpl queue = new ReceptionQueueRepositoryImpl(connections, unitOfWork);
            JdbcAuditRepositoryImpl audit = new JdbcAuditRepositoryImpl(
                    connections, unitOfWork, DatabaseConnection::getConnection);
            return new ReceptionServices(
                    rooms,
                    new RegisterPatientHandler(patients, insurance, rooms, queue, transactions, audit),
                    new SearchPatientsHandler(patients, insurance),
                    new GetReceptionQueueHandler(queue)
            );
        }
    }
}
