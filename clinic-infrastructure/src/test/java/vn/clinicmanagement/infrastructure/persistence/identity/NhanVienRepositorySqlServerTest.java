package vn.clinicmanagement.infrastructure.persistence.identity;

import org.junit.jupiter.api.AfterAll;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Requires only {@code CLINIC_TEST_SQLSERVER_URL}, {@code CLINIC_TEST_SQLSERVER_USERNAME},
 * and {@code CLINIC_TEST_SQLSERVER_PASSWORD}; application configuration is never a source.
 * The URL must explicitly select the exact database, require encryption and certificate
 * validation via {@code encrypt=true;trustServerCertificate=false}, and contain no credentials.
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

        // A nonempty explicit override bypasses the application's lazy config loader.
        DatabaseConnection.setConnectionString(testConnectionString(databaseName, System::getenv));
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT DB_NAME()");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next(), "SQL Server did not return its current database name");
            assertDatabaseIdentity(databaseName, rs.getString(1));
        } catch (SQLException exception) {
            // Driver diagnostics can contain URL/user/secret material. Do not retain the cause.
            throw new IllegalStateException("Dedicated SQL Server test connection or catalog check failed");
        }
        assertTrue(runMigrationsWithoutLogging(), "Database migrations did not complete");
    }

    @AfterAll
    static void clearTestConnectionOverride() {
        DatabaseConnection.setConnectionString("");
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
        assertTrue(expectedDatabaseName.equals(actualDatabaseName),
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

    static String testConnectionString(String databaseName, Function<String, String> environment) {
        assertSyntheticDatabaseName(databaseName);
        String configuredUrl = requiredTestInput(environment, "CLINIC_TEST_SQLSERVER_URL");
        String username = requiredTestInput(environment, "CLINIC_TEST_SQLSERVER_USERNAME");
        String credentialValue = requiredTestInput(environment, "CLINIC_TEST_SQLSERVER_PASSWORD");
        String targetUrl = syntheticDatabaseUrl(configuredUrl, databaseName);
        // JDBC braced values preserve whitespace and escape semicolons/closing braces as data.
        return targetUrl + ";user={" + username.replace("}", "}}")
                + "};password={" + credentialValue.replace("}", "}}") + "}";
    }

    private static String requiredTestInput(Function<String, String> environment, String key) {
        String value = environment.apply(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing dedicated SQL Server test input: " + key);
        }
        return value;
    }

    static String syntheticDatabaseUrl(String configuredUrl, String databaseName) {
        assertSyntheticDatabaseName(databaseName);
        if (configuredUrl == null || !configuredUrl.startsWith("jdbc:sqlserver://")) {
            throw new IllegalStateException("Dedicated test URL must be a SQL Server JDBC URL");
        }
        int parameterStart = configuredUrl.indexOf(';');
        if (parameterStart <= "jdbc:sqlserver://".length()) {
            throw new IllegalStateException("SQL Server URL must explicitly select the synthetic database");
        }
        String server = configuredUrl.substring("jdbc:sqlserver://".length(), parameterStart);
        if (server.chars().anyMatch(c -> Character.isWhitespace(c) || "{}=@/?#".indexOf(c) >= 0)) {
            throw new IllegalStateException("SQL Server URL has an ambiguous server selector");
        }

        StringBuilder targetUrl = new StringBuilder(configuredUrl.substring(0, parameterStart));
        Matcher parameters = URL_PARAMETER.matcher(configuredUrl);
        Set<String> propertyNames = new HashSet<>();
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
            String normalizedName = name.toLowerCase(Locale.ROOT);
            if (!propertyNames.add(normalizedName)
                    || Set.of("user", "username", "password", "integratedsecurity", "authentication",
                            "authenticationscheme", "accesstoken", "accesstokencallbackclass").contains(normalizedName)) {
                throw new IllegalStateException("SQL Server test URL must not contain duplicate properties, "
                        + "credentials or alternate authentication");
            }
            String value = parameters.group(2).trim();
            if ((normalizedName.equals("encrypt") && !value.equalsIgnoreCase("true"))
                    || (normalizedName.equals("trustservercertificate") && !value.equalsIgnoreCase("false"))) {
                throw new IllegalStateException("SQL Server test URL requires encrypt=true and trustServerCertificate=false");
            }
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
            targetUrl.append(parameters.group());
            position = parameters.end();
        }
        if (databaseSelectors != 1) {
            throw new IllegalStateException("SQL Server URL must explicitly select the synthetic database");
        }
        if (!propertyNames.containsAll(Set.of("encrypt", "trustservercertificate"))) {
            throw new IllegalStateException("SQL Server test URL requires encrypt=true and trustServerCertificate=false");
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
    private static final String TLS = ";encrypt=true;trustServerCertificate=false";

    @Test
    void acceptsOneMatchingSelector_andPreservesTlsAndBracedValues() {
        String tls = TLS + ";hostNameInCertificate=synthetic-host";
        String braced = ";applicationName={synthetic;database=clinicDb;escaped}}value}";
        assertEquals(URL + ";databaseName=" + DATABASE_NAME + braced + tls,
                NhanVienRepositorySqlServerTest.syntheticDatabaseUrl(URL + ";databaseName=" + DATABASE_NAME
                        + braced + tls, DATABASE_NAME));
        assertEquals(URL + "; DaTaBaSe ={" + DATABASE_NAME + "}" + tls,
                NhanVienRepositorySqlServerTest.syntheticDatabaseUrl(
                        URL + "; DaTaBaSe ={" + DATABASE_NAME + "}" + tls, DATABASE_NAME));
    }

    @Test
    void acceptsExplicitTlsWithoutHostnameOverride_andPreservesSettings() {
        // Without hostNameInCertificate, JDBC validates the server name from the URL.
        for (String tls : List.of(TLS, "; TrUsTsErVeRcErTiFiCaTe = FALSE ; EnCrYpT = TRUE ")) {
            String configuredUrl = URL + ";databaseName=" + DATABASE_NAME + tls;
            assertEquals(configuredUrl,
                    NhanVienRepositorySqlServerTest.syntheticDatabaseUrl(configuredUrl, DATABASE_NAME));
        }
    }

    @Test
    void rejectsMissingDisabledMalformedOrBypassTls_withoutExposingInputs() {
        for (String tls : List.of("", ";encrypt=true", ";trustServerCertificate=false",
                ";encrypt=false;trustServerCertificate=false",
                ";encrypt=true;trustServerCertificate=true",
                ";encrypt=false;trustServerCertificate=true",
                ";encrypt=;trustServerCertificate=false",
                ";encrypt=invalid_encryption_marker;trustServerCertificate=false",
                ";encrypt=optional;trustServerCertificate=false",
                ";encrypt=true;trustServerCertificate=",
                ";encrypt=true;trustServerCertificate=invalid_certificate_marker",
                ";encrypt={true;trustServerCertificate=false}",
                TLS + ";ENCRYPT=false", TLS + ";TRUSTSERVERCERTIFICATE=true")) {
            Map<String, String> inputs = Map.of(
                    "CLINIC_TEST_SQLSERVER_URL", URL + ";databaseName=" + DATABASE_NAME
                            + ";applicationName=synthetic_input_marker" + tls,
                    "CLINIC_TEST_SQLSERVER_USERNAME", "synthetic_user_marker",
                    "CLINIC_TEST_SQLSERVER_PASSWORD", "synthetic_secret_marker");
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> NhanVienRepositorySqlServerTest.testConnectionString(DATABASE_NAME, inputs::get));
            for (String value : inputs.values()) {
                assertFalse(failure.toString().contains(value), "Validation must not render test inputs");
            }
            for (String marker : List.of("synthetic-host", DATABASE_NAME, "synthetic_input_marker",
                    "invalid_encryption_marker", "invalid_certificate_marker")) {
                assertFalse(failure.toString().contains(marker), "Validation must not render URL values");
            }
            assertNull(failure.getCause());
        }
    }

    @Test
    void usesOnlyDedicatedInputs_andEscapesCredentialsWithoutTrimmingSecrets() {
        List<String> requestedKeys = new ArrayList<>();
        Map<String, String> inputs = Map.of(
                "CLINIC_TEST_SQLSERVER_URL", URL + ";databaseName=" + DATABASE_NAME + TLS,
                "CLINIC_TEST_SQLSERVER_USERNAME", "synthetic};user=other",
                "CLINIC_TEST_SQLSERVER_PASSWORD", " synthetic};database=clinicDb;secret ");
        String connectionString = NhanVienRepositorySqlServerTest.testConnectionString(DATABASE_NAME, key -> {
            requestedKeys.add(key);
            return inputs.get(key);
        });
        assertEquals(Set.of("CLINIC_TEST_SQLSERVER_URL", "CLINIC_TEST_SQLSERVER_USERNAME",
                "CLINIC_TEST_SQLSERVER_PASSWORD"), new HashSet<>(requestedKeys));
        assertEquals(3, requestedKeys.size());
        // Use a boolean assertion so a failure cannot render credential-bearing strings.
        assertTrue((URL + ";databaseName=" + DATABASE_NAME + TLS
                + ";user={synthetic}};user=other};password={ synthetic}};database=clinicDb;secret }")
                .equals(connectionString), "Dedicated credentials must be escaped and preserved");
    }

    @Test
    void rejectsMissingOrBlankDedicatedInputs_evenWhenApplicationEnvironmentIsComplete() {
        Map<String, String> complete = Map.of(
                "CLINIC_TEST_SQLSERVER_URL", URL + ";databaseName=" + DATABASE_NAME + TLS,
                "CLINIC_TEST_SQLSERVER_USERNAME", "synthetic_user_marker",
                "CLINIC_TEST_SQLSERVER_PASSWORD", "synthetic_secret_marker");
        for (String missingKey : complete.keySet()) {
            for (String missingValue : new String[]{null, "", "   "}) {
                IllegalStateException failure = assertThrows(IllegalStateException.class,
                        () -> NhanVienRepositorySqlServerTest.testConnectionString(DATABASE_NAME, key -> {
                            if (complete.containsKey(key)) {
                                return key.equals(missingKey) ? missingValue : complete.get(key);
                            }
                            // Any attempted fallback to application keys fails this test.
                            throw new AssertionError("Application environment must not be read");
                        }));
                assertEquals("Missing dedicated SQL Server test input: " + missingKey, failure.getMessage());
                assertNull(failure.getCause());
            }
        }
        Map<String, String> applicationOnly = Map.of(
                "DB_URL", URL + ";databaseName=" + DATABASE_NAME,
                "DB_USERNAME", "synthetic_user_marker", "DB_PASSWORD", "synthetic_secret_marker",
                "DB_CONNECTION_STRING", URL + ";databaseName=" + DATABASE_NAME);
        assertThrows(IllegalStateException.class,
                () -> NhanVienRepositorySqlServerTest.testConnectionString(DATABASE_NAME, applicationOnly::get));
    }

    @Test
    void rejectsMismatchedOrAmbiguousDedicatedUrl_withoutExposingInputs() {
        for (String url : List.of(URL + ";databaseName=clinicDb",
                URL + ";databaseName=clinic_test_other",
                URL + ";databaseName=" + DATABASE_NAME + ";database=" + DATABASE_NAME,
                "jdbc:postgresql://synthetic-host/" + DATABASE_NAME,
                "jdbc:sqlserver://;databaseName=" + DATABASE_NAME)) {
            Map<String, String> inputs = Map.of("CLINIC_TEST_SQLSERVER_URL", url + TLS,
                    "CLINIC_TEST_SQLSERVER_USERNAME", "synthetic_user_marker",
                    "CLINIC_TEST_SQLSERVER_PASSWORD", "synthetic_secret_marker");
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> NhanVienRepositorySqlServerTest.testConnectionString(DATABASE_NAME, inputs::get));
            for (String value : inputs.values()) {
                assertFalse(failure.toString().contains(value), "Validation must not render test inputs");
            }
            assertNull(failure.getCause());
        }
    }

    @Test
    void rejectsEmbeddedCredentialsAlternateAuthenticationAndDuplicateProperties() {
        for (String properties : List.of(";user=synthetic", "; USERname ={synthetic}",
                ";password={synthetic;database=clinicDb;escaped}}value}",
                ";integratedSecurity=true", ";authentication=ActiveDirectoryIntegrated",
                ";authenticationScheme=JavaKerberos", ";accessToken=synthetic",
                ";accessTokenCallbackClass=synthetic", ";encrypt=true;ENCRYPT=false")) {
            assertThrows(IllegalStateException.class,
                    () -> NhanVienRepositorySqlServerTest.syntheticDatabaseUrl(
                            URL + ";databaseName=" + DATABASE_NAME + TLS + properties, DATABASE_NAME));
        }
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
                    () -> NhanVienRepositorySqlServerTest.syntheticDatabaseUrl(URL + properties + TLS, DATABASE_NAME));
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
