package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.reception.GetReceptionQueueQuery;
import vn.clinicmanagement.application.reception.PatientSearchItem;
import vn.clinicmanagement.application.reception.RegisterPatientCommand;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.application.reception.SearchPatientsQuery;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.infrastructure.persistence.DatabaseConnection;
import vn.clinicmanagement.infrastructure.persistence.DatabaseMigrationRunner;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceptionSqlServerPersistenceTest {
    private static final String OPT_IN_PROPERTY = "clinic.test.sqlserver";
    private static final String DATABASE_PROPERTY = "clinic.test.sqlserver.databaseName";
    private static final Pattern SAFE_DATABASE_NAME =
            Pattern.compile("(?:clinic_orchestrator_review_|clinic_test_)[A-Za-z0-9_]*");
    private static final Pattern DATABASE_NAME_PARAMETER =
            Pattern.compile("(?i)(databaseName\\s*=)[^;]*");

    @Test
    void registersSearchesQueuesAndReloadsSyntheticReceptionDataWhenExplicitlyEnabled() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty(OPT_IN_PROPERTY)),
                "SQL Server reception persistence test requires explicit opt-in");

        String databaseName = System.getProperty(DATABASE_PROPERTY);
        assertNotNull(databaseName, "An explicit synthetic SQL Server database name is required");
        assertTrue(SAFE_DATABASE_NAME.matcher(databaseName).matches(),
                "SQL Server target must use an approved synthetic database name");

        configureSyntheticDatabase(databaseName);
        assertTrue(runMigrationsWithoutLogging(), "Database migrations did not complete");

        String generatedSuffix = Long.toString(ThreadLocalRandom.current().nextLong(10_000_000L, 100_000_000L));
        String syntheticCccd = "0000" + generatedSuffix;
        String syntheticPhone = "09" + generatedSuffix;
        String syntheticCardNumber = "SYNTH" + generatedSuffix;
        int registeredPatientId = 0;

        try {
            ReceptionCompositionRoot.Services services = ReceptionCompositionRoot.create();
            var room = services.rooms().getActiveRooms().stream().findFirst()
                    .orElseThrow(() -> new AssertionError("No active seeded clinic room is available"));

            BenhNhan patient = new BenhNhan();
            patient.setHo("Synthetic");
            patient.setTen("Patient" + generatedSuffix);
            patient.setNgaySinh(LocalDate.of(1990, 1, 2));
            patient.setGioiTinh(GioiTinh.M);
            patient.setDienThoai(syntheticPhone);
            patient.setSoCCCD(syntheticCccd);
            patient.setDiaChi("Synthetic test address");

            BaoHiem insurance = new BaoHiem();
            insurance.setLoaiBaoHiem(LoaiBaoHiem.BHYT);
            insurance.setSoBaoHiem(syntheticCardNumber);
            insurance.setNgayBatDau(LocalDate.now().minusDays(1));
            insurance.setNgayHetHan(LocalDate.now().plusYears(1));
            insurance.setMucHuong(80);

            var registrationResult = services.registerPatient().handle(new RegisterPatientCommand(
                    patient, insurance, room.getId(), "WALK_IN", "Synthetic persistence test",
                    DoiTuongKham.BHYT, false, 0));
            assertTrue(registrationResult.isSuccess(), "Synthetic reception registration failed");
            var receipt = registrationResult.valueOrThrow();
            registeredPatientId = receipt.patientId();
            assertTrue(registeredPatientId > 0, "Registration did not return a patient identifier");
            assertTrue(receipt.visitId() > 0, "Registration did not return a visit identifier");

            PatientSearchItem initialMatch = findByCccd(
                    services.searchPatients().handle(new SearchPatientsQuery(syntheticCccd)).valueOrThrow(),
                    registeredPatientId);
            assertPatientAndInsurance(initialMatch, "Patient" + generatedSuffix, syntheticCccd, syntheticPhone,
                    syntheticCardNumber);
            assertQueueContains(services.getQueue()
                            .handle(new GetReceptionQueueQuery(LocalDate.now())).valueOrThrow().queue(),
                    receipt.visitId(), registeredPatientId);

            ReceptionCompositionRoot.Services freshServices = ReceptionCompositionRoot.create();
            PatientSearchItem reloadedMatch = findByCccd(
                    freshServices.searchPatients().handle(new SearchPatientsQuery(syntheticCccd)).valueOrThrow(),
                    registeredPatientId);
            assertPatientAndInsurance(reloadedMatch, "Patient" + generatedSuffix, syntheticCccd, syntheticPhone,
                    syntheticCardNumber);
            assertQueueContains(freshServices.getQueue()
                            .handle(new GetReceptionQueueQuery(LocalDate.now())).valueOrThrow().queue(),
                    receipt.visitId(), registeredPatientId);
        } finally {
            softDeleteGeneratedRecords(registeredPatientId);
        }
    }

    private static void configureSyntheticDatabase(String databaseName) throws Exception {
        Properties properties = new Properties();
        try (InputStream input = ReceptionSqlServerPersistenceTest.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new IllegalStateException("Local classpath db.properties is required for the opt-in test");
            }
            properties.load(input);
        }

        String url = properties.getProperty("db.url");
        String username = properties.getProperty("db.username");
        String credentialValue = properties.getProperty("db." + "password");
        if (url == null || !url.toLowerCase(Locale.ROOT).startsWith("jdbc:sqlserver:")
                || username == null || username.isBlank() || credentialValue == null || credentialValue.isBlank()) {
            throw new IllegalStateException("Local classpath db.properties must contain SQL Server credentials");
        }

        Matcher matcher = DATABASE_NAME_PARAMETER.matcher(url);
        String targetUrl = matcher.find()
                ? matcher.replaceAll("$1" + Matcher.quoteReplacement(databaseName))
                : url + ";databaseName=" + databaseName;
        DatabaseConnection.setConnectionString("");
        DatabaseConnection.configure(targetUrl, username, credentialValue);
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

    private static PatientSearchItem findByCccd(List<PatientSearchItem> matches, int patientId) {
        return matches.stream().filter(match -> match.patient().getId() == patientId).findFirst()
                .orElseThrow(() -> new AssertionError("Registered patient was not found by CCCD"));
    }

    private static void assertPatientAndInsurance(PatientSearchItem match, String patientName, String cccd,
                                                  String phone, String cardNumber) {
        assertTrue(patientName.equals(match.patient().getTen()), "Patient name did not persist");
        assertTrue(cccd.equals(match.patient().getSoCCCD()), "Patient CCCD did not persist");
        assertTrue(phone.equals(match.patient().getDienThoai()), "Patient phone did not persist");
        assertNotNull(match.latestInsurance(), "BHYT card was not returned with the patient");
        assertTrue(cardNumber.equals(match.latestInsurance().getSoBaoHiem()), "BHYT number did not persist");
        assertTrue(match.latestInsurance().getLoaiBaoHiem() == LoaiBaoHiem.BHYT,
                "Insurance type was not reloaded as BHYT");
        assertTrue(match.latestInsurance().getMucHuong() == 80, "BHYT coverage did not persist");
    }

    private static void assertQueueContains(List<ReceptionQueueItem> queue, int visitId, int patientId) {
        assertTrue(queue.stream().anyMatch(item -> item.visitId() == visitId && item.patientId() == patientId),
                "Registered visit was not present in the reception queue");
    }

    private static void softDeleteGeneratedRecords(int patientId) throws SQLException {
        if (patientId <= 0) return;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                softDeleteByPatient(connection, "LuotKham", patientId);
                softDeleteByPatient(connection, "BaoHiem", patientId);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE BenhNhan SET isDeleted=1, deletedAt=SYSDATETIME(), deletedBy=NULL, version=version+1 "
                                + "WHERE id=? AND isDeleted=0")) {
                    statement.setInt(1, patientId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private static void softDeleteByPatient(Connection connection, String table, int patientId) throws SQLException {
        String sql = "UPDATE " + table
                + " SET isDeleted=1, deletedAt=SYSDATETIME(), deletedBy=NULL, version=version+1 "
                + "WHERE maBenhNhan=? AND isDeleted=0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.executeUpdate();
        }
    }
}
