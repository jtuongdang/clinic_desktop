package vn.clinicmanagement.infrastructure.persistence.identity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.infrastructure.persistence.DatabaseConnection;
import vn.clinicmanagement.infrastructure.persistence.DatabaseMigrationRunner;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused opt-in SQL Server persistence coverage for {@link NhanVienRepositoryImpl}.
 *
 * <p>Validates actual SQL Server behavior for {@code getByUsername}, {@code getRoleNames},
 * and {@code recordSuccessfulLogin}. This test requires explicit opt-in via
 * {@code -Dclinic.test.sqlserver=true} and a dedicated test database name matching
 * {@code clinic_test_*} or {@code clinic_orchestrator_review_*}. It will never run against
 * default {@code clinicDb} and fails closed when an unsafe or non-test target is supplied.
 */
class NhanVienRepositorySqlServerTest {

    private static final String OPT_IN_PROPERTY = "clinic.test.sqlserver";
    private static final String DATABASE_PROPERTY = "clinic.test.sqlserver.databaseName";
    private static final Pattern SAFE_DATABASE_NAME =
            Pattern.compile("(?:clinic_orchestrator_review_|clinic_test_)[A-Za-z0-9_]+");
    private static final Pattern URL_PARAMETER =
            Pattern.compile(";\\s*([^=;{}\\s]+)\\s*=\\s*(\\{(?:[^}]|}})*\\}|[^;{}]*)\\s*(?=;|$)");

    private final List<Integer> generatedEmployeeIds = new ArrayList<>();
    private final List<Integer> generatedRoleIds = new ArrayList<>();

    private JdbcUnitOfWork unitOfWork;
    private JdbcTransactionManager transactions;
    private JdbcConnectionProvider connections;
    private NhanVienRepositoryImpl repository;

    @BeforeAll
    static void setUpDatabaseFixture() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty(OPT_IN_PROPERTY)),
                "SQL Server employee repository persistence test requires explicit opt-in via -D"
                        + OPT_IN_PROPERTY + "=true");

        String databaseName = System.getProperty(DATABASE_PROPERTY);
        assertSyntheticDatabaseName(databaseName);

        configureSyntheticDatabase(databaseName);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT DB_NAME()");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next(), "SQL Server did not return its current database name");
            assertDatabaseIdentity(databaseName, rs.getString(1));
        }
        assertTrue(runMigrationsWithoutLogging(), "Database migrations did not complete");
    }

    private static void assertSyntheticDatabaseName(String databaseName) {
        assertNotNull(databaseName, "An explicit synthetic SQL Server database name is required via -D"
                + DATABASE_PROPERTY);
        assertFalse("clinicDb".equalsIgnoreCase(databaseName.trim()),
                "Must never connect to default clinicDb");
        assertTrue(SAFE_DATABASE_NAME.matcher(databaseName).matches(),
                "SQL Server target must use an approved synthetic database name starting with clinic_test_ or clinic_orchestrator_review_");
    }

    static void assertDatabaseIdentity(String expectedDatabaseName, String actualDatabaseName) {
        assertSyntheticDatabaseName(actualDatabaseName);
        assertEquals(expectedDatabaseName, actualDatabaseName,
                "Connected SQL Server catalog must exactly match -D" + DATABASE_PROPERTY);
    }

    @BeforeEach
    void setUpRepository() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty(OPT_IN_PROPERTY)),
                "SQL Server employee repository persistence test requires explicit opt-in");

        unitOfWork = new JdbcUnitOfWork();
        transactions = new JdbcTransactionManager(unitOfWork);
        connections = new JdbcConnectionProvider(transactions);
        repository = new NhanVienRepositoryImpl(connections, unitOfWork);
    }

    @AfterEach
    void tearDownSyntheticData() {
        if (!Boolean.parseBoolean(System.getProperty(OPT_IN_PROPERTY))) {
            return;
        }
        cleanupGeneratedRecords();
    }

    @Test
    void getByUsername_returnsActiveEmployee_andExcludesMissingOrSoftDeleted() throws SQLException {
        String suffix = uniqueSuffix();
        String activeUsername = "synth_act_" + suffix;
        String deletedUsername = "synth_del_" + suffix;
        String inactiveUsername = "synth_inact_" + suffix;
        String missingUsername = "synth_missing_" + suffix;

        int activeId;
        int deletedId;
        int inactiveId;

        try (Connection conn = DatabaseConnection.getConnection()) {
            activeId = insertSyntheticEmployee(conn, "EMP_ACT_" + suffix, activeUsername, true, false);
            deletedId = insertSyntheticEmployee(conn, "EMP_DEL_" + suffix, deletedUsername, true, true);
            inactiveId = insertSyntheticEmployee(conn, "EMP_INACT_" + suffix, inactiveUsername, false, false);
        }

        // 1. Active account with exact username match is found
        Optional<NhanVien> activeOpt = repository.getByUsername(activeUsername);
        assertTrue(activeOpt.isPresent(), "Expected active synthetic employee to be returned by exact username");
        NhanVien active = activeOpt.get();
        assertEquals(activeId, active.getId(), "Employee identifier mismatch");
        assertEquals(activeUsername, active.getUsername(), "Username mismatch");
        assertEquals("Synthetic", active.getHo(), "Family name mismatch");
        assertEquals("TestUser", active.getTen(), "Given name mismatch");
        assertTrue(active.isHoatDong(), "Active employee must have hoatDong=true");
        assertFalse(active.isDeleted(), "Active employee must have isDeleted=false");

        // 2. Missing username returns empty Optional
        Optional<NhanVien> missingOpt = repository.getByUsername(missingUsername);
        assertTrue(missingOpt.isEmpty(), "Missing username must return Optional.empty()");

        // 3. Soft-deleted account is excluded by isDeleted=0 query predicate
        Optional<NhanVien> deletedOpt = repository.getByUsername(deletedUsername);
        assertTrue(deletedOpt.isEmpty(), "Soft-deleted employee must not be returned by getByUsername");

        // 4. Inactive account (hoatDong=0, isDeleted=0) is returned with hoatDong=false
        Optional<NhanVien> inactiveOpt = repository.getByUsername(inactiveUsername);
        assertTrue(inactiveOpt.isPresent(), "Inactive but not-deleted employee should be queryable by repository");
        assertEquals(inactiveId, inactiveOpt.get().getId());
        assertFalse(inactiveOpt.get().isHoatDong(), "Inactive employee must reflect hoatDong=false");

        // 5. Blank/null username handling
        assertTrue(repository.getByUsername(null).isEmpty(), "Null username must return Optional.empty()");
        assertTrue(repository.getByUsername("   ").isEmpty(), "Blank username must return Optional.empty()");
    }

    @Test
    void getRoleNames_returnsActiveRolesAsSet_andExcludesSoftDeletedAssignmentsAndRoles() throws SQLException {
        String suffix = uniqueSuffix();
        String roleA = "SYNTH_ROLE_A_" + suffix;
        String roleB = "SYNTH_ROLE_B_" + suffix;
        String roleCSoftDeletedLink = "SYNTH_ROLE_C_" + suffix;
        String roleDSoftDeletedRole = "SYNTH_ROLE_D_" + suffix;

        int employeeId;
        try (Connection conn = DatabaseConnection.getConnection()) {
            employeeId = insertSyntheticEmployee(conn, "EMP_ROLES_" + suffix, "synth_roles_" + suffix, true, false);

            int roleAId = insertSyntheticRole(conn, roleA, false);
            int roleBId = insertSyntheticRole(conn, roleB, false);
            int roleCId = insertSyntheticRole(conn, roleCSoftDeletedLink, false);
            int roleDId = insertSyntheticRole(conn, roleDSoftDeletedRole, true); // Soft-deleted role

            // Link active roles
            insertSyntheticEmployeeRole(conn, employeeId, roleAId, false);
            insertSyntheticEmployeeRole(conn, employeeId, roleBId, false);

            // Link with soft-deleted junction record (NhanVienVaiTro.isDeleted = 1)
            insertSyntheticEmployeeRole(conn, employeeId, roleCId, true);

            // Link with active junction record to a soft-deleted role (VaiTro.isDeleted = 1)
            insertSyntheticEmployeeRole(conn, employeeId, roleDId, false);
        }

        List<String> actualRoleNames = repository.getRoleNames(employeeId);
        Set<String> actualRoleSet = new HashSet<>(actualRoleNames);

        // Assert role membership as a set without asserting arbitrary ordering
        assertEquals(Set.of(roleA, roleB), actualRoleSet,
                "getRoleNames must return exactly active roles with active assignments as a set");

        // Verify soft-deleted assignment is excluded
        assertFalse(actualRoleSet.contains(roleCSoftDeletedLink),
                "Soft-deleted role membership assignment must be excluded");

        // Verify soft-deleted role itself is excluded
        assertFalse(actualRoleSet.contains(roleDSoftDeletedRole),
                "Soft-deleted role entity must be excluded from role names");

        // Missing employee returns empty list
        List<String> unassignedRoles = repository.getRoleNames(999_999_999);
        assertTrue(unassignedRoles.isEmpty(), "Non-existent employee must return empty role list");
    }

    @Test
    void recordSuccessfulLogin_persistsTimestamp_andEnforcesActiveAccountSemantics() throws SQLException {
        String suffix = uniqueSuffix();
        int activeEmployeeId;
        int inactiveEmployeeId;
        int deletedEmployeeId;

        try (Connection conn = DatabaseConnection.getConnection()) {
            activeEmployeeId = insertSyntheticEmployee(conn, "EMP_LOG1_" + suffix, "synth_log1_" + suffix, true, false);
            inactiveEmployeeId = insertSyntheticEmployee(conn, "EMP_LOG2_" + suffix, "synth_log2_" + suffix, false, false);
            deletedEmployeeId = insertSyntheticEmployee(conn, "EMP_LOG3_" + suffix, "synth_log3_" + suffix, true, true);
        }

        LocalDateTime loginTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        // 1. Calling recordSuccessfulLogin without an active transaction throws IllegalStateException
        assertThrows(IllegalStateException.class,
                () -> repository.recordSuccessfulLogin(activeEmployeeId, loginTime),
                "recordSuccessfulLogin must require an active transaction boundary");

        // 2. Within a transaction, recordSuccessfulLogin commits and persists the timestamp
        Result<Boolean> commitResult = transactions.inTransaction(() -> {
            repository.recordSuccessfulLogin(activeEmployeeId, loginTime);
            return Result.success(true);
        });
        assertTrue(commitResult.isSuccess(), "Login timestamp update transaction failed");

        // Read back after commit to verify persistence
        NhanVien reloaded = repository.getById(activeEmployeeId)
                .orElseThrow(() -> new AssertionError("Synthetic employee not found after login update"));
        assertNotNull(reloaded.getDangNhapCuoi(), "dangNhapCuoi timestamp was not persisted");
        assertEquals(Timestamp.valueOf(loginTime).toLocalDateTime(), reloaded.getDangNhapCuoi(),
                "Persisted login timestamp did not match expected value");

        // 3. Inactive account throws IllegalStateException ("Tài khoản không còn hoạt động.")
        Result<Boolean> inactiveResult = transactions.inTransaction(() -> {
            assertThrows(IllegalStateException.class,
                    () -> repository.recordSuccessfulLogin(inactiveEmployeeId, loginTime),
                    "Inactive account must be rejected on recordSuccessfulLogin");
            return Result.success(true);
        });
        assertTrue(inactiveResult.isSuccess());

        // 4. Soft-deleted account throws IllegalStateException
        Result<Boolean> deletedResult = transactions.inTransaction(() -> {
            assertThrows(IllegalStateException.class,
                    () -> repository.recordSuccessfulLogin(deletedEmployeeId, loginTime),
                    "Soft-deleted account must be rejected on recordSuccessfulLogin");
            return Result.success(true);
        });
        assertTrue(deletedResult.isSuccess());

        // 5. Non-existent employee throws IllegalStateException
        Result<Boolean> missingResult = transactions.inTransaction(() -> {
            assertThrows(IllegalStateException.class,
                    () -> repository.recordSuccessfulLogin(999_999_999, loginTime),
                    "Non-existent employee must be rejected on recordSuccessfulLogin");
            return Result.success(true);
        });
        assertTrue(missingResult.isSuccess());

        // 6. Invalid parameters
        assertThrows(IllegalArgumentException.class, () -> repository.recordSuccessfulLogin(0, loginTime));
        assertThrows(IllegalArgumentException.class, () -> repository.recordSuccessfulLogin(-1, loginTime));
        assertThrows(IllegalArgumentException.class, () -> repository.recordSuccessfulLogin(activeEmployeeId, null));
    }

    @Test
    void recordSuccessfulLogin_sharesTransactionConnection_andRollsBackOnFailure() throws SQLException {
        String suffix = uniqueSuffix();
        int employeeId;

        try (Connection conn = DatabaseConnection.getConnection()) {
            employeeId = insertSyntheticEmployee(conn, "EMP_TX_" + suffix, "synth_tx_" + suffix, true, false);
        }

        LocalDateTime initialTimestamp = LocalDateTime.now().minusHours(2).truncatedTo(ChronoUnit.MILLIS);

        // Baseline commit
        Result<Boolean> initialCommit = transactions.inTransaction(() -> {
            repository.recordSuccessfulLogin(employeeId, initialTimestamp);
            return Result.success(true);
        });
        assertTrue(initialCommit.isSuccess());
        assertEquals(Timestamp.valueOf(initialTimestamp).toLocalDateTime(),
                repository.getById(employeeId).orElseThrow().getDangNhapCuoi());

        // 1. Transaction connection sharing: verify uncommitted write is visible on the same connection
        LocalDateTime updatedTimestamp = LocalDateTime.now().minusMinutes(10).truncatedTo(ChronoUnit.MILLIS);
        Result<Boolean> sharedConnectionResult = transactions.inTransaction(() -> {
            repository.recordSuccessfulLogin(employeeId, updatedTimestamp);
            // Read back within uncommitted transaction via repository
            NhanVien visibleInsideTx = repository.getById(employeeId).orElseThrow();
            assertEquals(Timestamp.valueOf(updatedTimestamp).toLocalDateTime(), visibleInsideTx.getDangNhapCuoi(),
                    "Repository call within transaction must observe uncommitted write on shared connection");
            return Result.success(true);
        });
        assertTrue(sharedConnectionResult.isSuccess());
        assertEquals(Timestamp.valueOf(updatedTimestamp).toLocalDateTime(),
                repository.getById(employeeId).orElseThrow().getDangNhapCuoi());

        // 2. Transaction rollback: verify failed transaction reverts the login timestamp
        LocalDateTime rollbackTimestamp = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MILLIS);
        Result<Boolean> failedResult = transactions.inTransaction(() -> {
            repository.recordSuccessfulLogin(employeeId, rollbackTimestamp);
            return Result.failure("SIMULATED_FAILURE", "Simulated business transaction failure");
        });
        assertTrue(failedResult.isFailure(), "Transaction should report failure");

        // Read back after rollback: should retain updatedTimestamp, NOT rollbackTimestamp
        NhanVien afterRollback = repository.getById(employeeId).orElseThrow();
        assertEquals(Timestamp.valueOf(updatedTimestamp).toLocalDateTime(), afterRollback.getDangNhapCuoi(),
                "Rolled-back transaction must not persist login timestamp update");
    }

    private static String uniqueSuffix() {
        return Long.toString(ThreadLocalRandom.current().nextLong(10_000_000L, 100_000_000L));
    }

    private int insertSyntheticEmployee(Connection conn, String code, String username,
                                        boolean active, boolean isDeleted) throws SQLException {
        String sql = "INSERT INTO NhanVien (maNhanVien, ho, ten, username, hoatDong, isDeleted, ngayVaoLam, createdAt) "
                + "OUTPUT INSERTED.id VALUES (?, N'Synthetic', N'TestUser', ?, ?, ?, CAST(GETDATE() AS DATE), SYSDATETIME())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, username);
            ps.setBoolean(3, active);
            ps.setBoolean(4, isDeleted);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Failed to insert synthetic employee fixture");
                int id = rs.getInt(1);
                generatedEmployeeIds.add(id);
                return id;
            }
        }
    }

    private int insertSyntheticRole(Connection conn, String roleName, boolean isDeleted) throws SQLException {
        String sql = "INSERT INTO VaiTro (tenVaiTro, moTa, isDeleted, createdAt) "
                + "OUTPUT INSERTED.id VALUES (?, N'Synthetic test role', ?, SYSDATETIME())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleName);
            ps.setBoolean(2, isDeleted);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Failed to insert synthetic role fixture");
                int id = rs.getInt(1);
                generatedRoleIds.add(id);
                return id;
            }
        }
    }

    private int insertSyntheticEmployeeRole(Connection conn, int employeeId, int roleId,
                                            boolean isDeleted) throws SQLException {
        String sql = "INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro, isDeleted, createdAt) "
                + "OUTPUT INSERTED.id VALUES (?, ?, ?, SYSDATETIME())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, roleId);
            ps.setBoolean(3, isDeleted);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Failed to link synthetic employee role fixture");
                return rs.getInt(1);
            }
        }
    }

    private void cleanupGeneratedRecords() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!generatedRoleIds.isEmpty()) {
                    for (int roleId : generatedRoleIds) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "DELETE FROM NhanVienVaiTro WHERE maVaiTro=?")) {
                            ps.setInt(1, roleId);
                            ps.executeUpdate();
                        }
                        try (PreparedStatement ps = conn.prepareStatement(
                                "DELETE FROM VaiTro WHERE id=?")) {
                            ps.setInt(1, roleId);
                            ps.executeUpdate();
                        }
                    }
                }
                if (!generatedEmployeeIds.isEmpty()) {
                    for (int empId : generatedEmployeeIds) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "DELETE FROM NhanVienVaiTro WHERE maNhanVien=?")) {
                            ps.setInt(1, empId);
                            ps.executeUpdate();
                        }
                        try (PreparedStatement ps = conn.prepareStatement(
                                "DELETE FROM NhanVien WHERE id=?")) {
                            ps.setInt(1, empId);
                            ps.executeUpdate();
                        }
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
            }
        } catch (Exception ignored) {
            // Teardown should not mask test failures
        } finally {
            generatedEmployeeIds.clear();
            generatedRoleIds.clear();
        }
    }

    private static void configureSyntheticDatabase(String databaseName) throws Exception {
        Properties properties = new Properties();
        try (InputStream input = NhanVienRepositorySqlServerTest.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
            }
        }

        String configuredUrl = properties.getProperty("db.url");
        String username = properties.getProperty("db.username");
        String credentialValue = properties.getProperty("db." + "password");

        if (configuredUrl == null || configuredUrl.isBlank()) {
            configuredUrl = System.getenv("DB_URL");
        }
        if (username == null || username.isBlank()) {
            username = System.getenv("DB_USERNAME");
        }
        if (credentialValue == null || credentialValue.isBlank()) {
            credentialValue = System.getenv("DB_PASSWORD");
        }

        if (configuredUrl == null || configuredUrl.isBlank()) {
            String connStr = System.getenv("DB_CONNECTION_STRING");
            if (connStr != null && !connStr.isBlank()) {
                configuredUrl = connStr;
            }
        }

        if (configuredUrl == null || !configuredUrl.regionMatches(true, 0, "jdbc:sqlserver://", 0, 17)
                || username == null || username.isBlank() || credentialValue == null || credentialValue.isBlank()) {
            throw new IllegalStateException("SQL Server credentials (db.properties or DB_URL/DB_USERNAME/DB_PASSWORD) "
                    + "are required for the opt-in test");
        }

        String targetUrl = syntheticDatabaseUrl(configuredUrl, databaseName);

        DatabaseConnection.setConnectionString("");
        DatabaseConnection.configure(targetUrl, username, credentialValue);
    }

    static String syntheticDatabaseUrl(String configuredUrl, String databaseName) {
        assertSyntheticDatabaseName(databaseName);
        int parameterStart = configuredUrl.indexOf(';');
        if (parameterStart < 0) {
            throw new IllegalStateException("SQL Server URL must explicitly select the synthetic database");
        }

        StringBuilder targetUrl = new StringBuilder(configuredUrl.substring(0, parameterStart));
        Matcher parameters = URL_PARAMETER.matcher(configuredUrl);
        int databaseSelectors = 0;
        int position = parameterStart;
        while (position < configuredUrl.length()) {
            if (position == configuredUrl.length() - 1 && configuredUrl.charAt(position) == ';') {
                break;
            }
            parameters.region(position, configuredUrl.length());
            if (!parameters.lookingAt()) {
                throw new IllegalStateException("SQL Server URL has ambiguous or malformed properties");
            }
            String name = parameters.group(1);
            String value = parameters.group(2).trim();
            if (name.equalsIgnoreCase("databaseName") || name.equalsIgnoreCase("database")) {
                databaseSelectors++;
                if (value.startsWith("{")) {
                    value = value.substring(1, value.length() - 1).replace("}}", "}");
                }
                if (databaseSelectors > 1 || !databaseName.equals(value)) {
                    throw new IllegalStateException("SQL Server URL must contain exactly one database selector "
                            + "matching -D" + DATABASE_PROPERTY);
                }
            }
            // Parse braced values before removing credentials so embedded semicolons cannot create properties.
            if (!name.equalsIgnoreCase("user") && !name.equalsIgnoreCase("username")
                    && !name.equalsIgnoreCase("password")) {
                targetUrl.append(parameters.group());
            }
            position = parameters.end();
        }
        if (databaseSelectors != 1) {
            throw new IllegalStateException("SQL Server URL must explicitly select the synthetic database");
        }
        return targetUrl.toString();
    }

    private static boolean runMigrationsWithoutLogging() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try (PrintStream sink = new PrintStream(OutputStream.nullOutputStream())) {
            System.setOut(sink);
            System.setErr(sink);
            return DatabaseMigrationRunner.runMigrations();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
}

/** Runs without SQL Server opt-in; the actual catalog is verified by the fixture's DB_NAME() query. */
class NhanVienSqlServerTargetGuardTest {

    private static final String DATABASE_NAME = "clinic_test_employee_guard";
    private static final String URL = "jdbc:sqlserver://synthetic-host:1433";

    @Test
    void acceptsOneMatchingSelector_preservesTls_andRemovesWholeBracedCredentials() {
        String tls = ";encrypt=true;trustServerCertificate=false;hostNameInCertificate=synthetic-host";
        assertEquals(URL + ";databaseName=" + DATABASE_NAME + tls,
                NhanVienRepositorySqlServerTest.syntheticDatabaseUrl(URL + ";databaseName=" + DATABASE_NAME
                        + ";user=synthetic;password={synthetic;database=clinicDb;escaped}}value}" + tls,
                        DATABASE_NAME));
        assertEquals(URL + "; DaTaBaSe ={" + DATABASE_NAME + "}" + tls,
                NhanVienRepositorySqlServerTest.syntheticDatabaseUrl(
                        URL + "; DaTaBaSe ={" + DATABASE_NAME + "}" + tls, DATABASE_NAME));
    }

    @Test
    void rejectsMissingConflictingDuplicateOrMalformedDatabaseSelectors() {
        for (String properties : List.of("", ";encrypt=true", ";databaseName=clinicDb", ";database=clinicDb",
                ";databaseName=clinic_test_other", ";databaseName=" + DATABASE_NAME + ";database=clinicDb",
                ";database=clinicDb;databaseName=" + DATABASE_NAME,
                ";databaseName=" + DATABASE_NAME + "; DATABASEname =" + DATABASE_NAME,
                ";database=" + DATABASE_NAME + ";databaseName=" + DATABASE_NAME,
                ";password={synthetic;databaseName=" + DATABASE_NAME + "}",
                ";databaseName=" + DATABASE_NAME + ";password={unterminated")) {
            assertThrows(IllegalStateException.class,
                    () -> NhanVienRepositorySqlServerTest.syntheticDatabaseUrl(URL + properties, DATABASE_NAME));
        }
        assertThrows(AssertionError.class,
                () -> NhanVienRepositorySqlServerTest.syntheticDatabaseUrl(URL + ";databaseName=clinicDb", "clinicDb"));
    }

    @Test
    void requiresExactActualCatalogMatch_andTestOnlyPrefix() {
        NhanVienRepositorySqlServerTest.assertDatabaseIdentity(DATABASE_NAME, DATABASE_NAME);
        for (String actual : List.of("clinicDb", "master", "clinic_test_other", "CLINIC_TEST_EMPLOYEE_GUARD", "")) {
            assertThrows(AssertionError.class,
                    () -> NhanVienRepositorySqlServerTest.assertDatabaseIdentity(DATABASE_NAME, actual));
        }
        assertThrows(AssertionError.class,
                () -> NhanVienRepositorySqlServerTest.assertDatabaseIdentity(DATABASE_NAME, null));
    }
}
