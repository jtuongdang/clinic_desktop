package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
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
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.infrastructure.persistence.DatabaseConnection;
import vn.clinicmanagement.infrastructure.persistence.DatabaseMigrationRunner;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        LocalDate today = LocalDate.now();
        LocalDate initialCardStart = today.minusDays(1);
        LocalDate initialCardEnd = today.plusYears(1);
        LocalDate updatedCardStart = today.minusDays(2);
        LocalDate updatedCardEnd = today.plusYears(2);
        int registeredPatientId = 0;
        int generatedCardId = 0;
        List<Integer> generatedVisitIds = new ArrayList<>();

        try {
            SessionLifecycleService lifecycle = syntheticLifecycle();
            AuthorizationContext authorizationContext = lifecycle.currentAuthorizationContext().orElseThrow();
            ReceptionCompositionRoot.Services services = ReceptionCompositionRoot.create(lifecycle);
            var room = services.getActiveRooms().handle(new GetActiveReceptionRoomsQuery(
                    authorizationContext, lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow().stream().findFirst()
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
            insurance.setNgayBatDau(initialCardStart);
            insurance.setNgayHetHan(initialCardEnd);
            insurance.setMucHuong(80);

            var registrationResult = services.registerPatient().handle(new RegisterPatientCommand(
                    patient, insurance, room.getId(), "WALK_IN", "Synthetic persistence test",
                    DoiTuongKham.BHYT, false, authorizationContext.employeeId(), authorizationContext,
                    lifecycle.currentAuditActorContext().orElseThrow()));
            assertTrue(registrationResult.isSuccess(), "Synthetic reception registration failed");
            var receipt = registrationResult.valueOrThrow();
            registeredPatientId = receipt.patientId();
            generatedCardId = insurance.getId();
            generatedVisitIds.add(receipt.visitId());
            assertTrue(registeredPatientId > 0, "Registration did not return a patient identifier");
            assertTrue(receipt.visitId() > 0, "Registration did not return a visit identifier");
            assertTrue(generatedCardId > 0, "Registration did not return a BHYT card identifier");

            PatientSearchItem initialMatch = findByCccd(
                    services.searchPatients().handle(new SearchPatientsQuery(syntheticCccd, authorizationContext,
                            lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow(),
                    registeredPatientId);
            assertNotNull(initialMatch.latestInsurance(), "BHYT card was not returned after registration");
            assertEquals(generatedCardId, initialMatch.latestInsurance().getId(),
                    "BHYT card identifier changed during initial reload");
            assertPatientAndInsurance(initialMatch, "Patient" + generatedSuffix, LocalDate.of(1990, 1, 2),
                    GioiTinh.M, syntheticCccd, syntheticPhone, "Synthetic test address", syntheticCardNumber, 80,
                    initialCardStart, initialCardEnd);
            assertQueueContains(services.getQueue()
                            .handle(new GetReceptionQueueQuery(LocalDate.now(), authorizationContext,
                                    lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow().queue(),
                    receipt.visitId(), registeredPatientId);

            String updatedName = "Updated" + generatedSuffix;
            String updatedPhone = "08" + generatedSuffix;
            String updatedAddress = "Updated synthetic test address";
            String updatedCardNumber = syntheticCardNumber + "U";
            BenhNhan updatedPatient = initialMatch.patient();
            updatedPatient.setTen(updatedName);
            updatedPatient.setNgaySinh(LocalDate.of(1991, 2, 3));
            updatedPatient.setGioiTinh(GioiTinh.F);
            updatedPatient.setDienThoai(updatedPhone);
            updatedPatient.setDiaChi(updatedAddress);

            BaoHiem updatedInsurance = initialMatch.latestInsurance();
            updatedInsurance.setSoBaoHiem(updatedCardNumber);
            updatedInsurance.setNgayBatDau(updatedCardStart);
            updatedInsurance.setNgayHetHan(updatedCardEnd);
            updatedInsurance.setMucHuong(90);

            var updateResult = services.registerPatient().handle(new RegisterPatientCommand(
                    updatedPatient, updatedInsurance, room.getId(), "WALK_IN", "Synthetic update persistence test",
                    DoiTuongKham.BHYT, false, authorizationContext.employeeId(), authorizationContext,
                    lifecycle.currentAuditActorContext().orElseThrow()));
            assertTrue(updateResult.isSuccess(), "Synthetic existing-patient update failed");
            var updateReceipt = updateResult.valueOrThrow();
            assertEquals(registeredPatientId, updateReceipt.patientId(),
                    "Updating an existing patient created a different patient");
            assertTrue(updateReceipt.visitId() > 0, "Update registration did not return a visit identifier");
            generatedVisitIds.add(updateReceipt.visitId());

            ReceptionCompositionRoot.Services freshServices = ReceptionCompositionRoot.create(lifecycle);
            List<PatientSearchItem> reloadedMatches = freshServices.searchPatients()
                    .handle(new SearchPatientsQuery(syntheticCccd, authorizationContext,
                            lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow();
            assertEquals(1, reloadedMatches.size(),
                    "Updating the patient created a duplicate active patient with the same CCCD");
            PatientSearchItem reloadedMatch = findByCccd(reloadedMatches, registeredPatientId);
            assertPatientAndInsurance(reloadedMatch, updatedName, LocalDate.of(1991, 2, 3), GioiTinh.F,
                    syntheticCccd, updatedPhone, updatedAddress, updatedCardNumber, 90, updatedCardStart,
                    updatedCardEnd);
            assertEquals(generatedCardId, reloadedMatch.latestInsurance().getId(),
                    "Updating BHYT created a different current card instead of updating the existing card");
            List<ReceptionQueueItem> reloadedQueue = freshServices.getQueue()
                    .handle(new GetReceptionQueueQuery(LocalDate.now(), authorizationContext,
                            lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow().queue();
            assertQueueContains(reloadedQueue, receipt.visitId(), registeredPatientId);
            assertQueueContains(reloadedQueue, updateReceipt.visitId(), registeredPatientId);
        } finally {
            softDeleteGeneratedRecords(registeredPatientId, generatedCardId, generatedVisitIds);
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

    private static SessionLifecycleService syntheticLifecycle() {
        SessionLifecycleService lifecycle = new SessionLifecycleService(UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)), Clock.systemUTC());
        lifecycle.startSession(701, "synthetic-reception", "Synthetic Reception",
                new RoleChoice(31, "RECEPTION", "Synthetic Reception"),
                new WorkScopeChoice(11, "Synthetic Facility", 21, "Synthetic Department"),
                UUID.randomUUID().toString());
        return lifecycle;
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

    private static void assertPatientAndInsurance(PatientSearchItem match, String patientName, LocalDate birthDate,
                                                  GioiTinh gender, String cccd, String phone, String address,
                                                  String cardNumber, int coverage, LocalDate cardStart,
                                                  LocalDate cardEnd) {
        assertEquals(patientName, match.patient().getTen(), "Patient name did not persist");
        assertEquals(birthDate, match.patient().getNgaySinh(), "Patient birth date did not persist");
        assertEquals(gender, match.patient().getGioiTinh(), "Patient gender did not persist");
        assertEquals(cccd, match.patient().getSoCCCD(), "Patient CCCD did not persist");
        assertEquals(phone, match.patient().getDienThoai(), "Patient phone did not persist");
        assertEquals(address, match.patient().getDiaChi(), "Patient address did not persist");
        assertNotNull(match.latestInsurance(), "BHYT card was not returned with the patient");
        assertEquals(cardNumber, match.latestInsurance().getSoBaoHiem(), "BHYT number did not persist");
        assertEquals(LoaiBaoHiem.BHYT, match.latestInsurance().getLoaiBaoHiem(),
                "Insurance type was not reloaded as BHYT");
        assertEquals(coverage, match.latestInsurance().getMucHuong(), "BHYT coverage did not persist");
        assertEquals(cardStart, match.latestInsurance().getNgayBatDau(), "BHYT start date did not persist");
        assertEquals(cardEnd, match.latestInsurance().getNgayHetHan(), "BHYT expiry date did not persist");
    }

    private static void assertQueueContains(List<ReceptionQueueItem> queue, int visitId, int patientId) {
        assertTrue(queue.stream().anyMatch(item -> item.visitId() == visitId && item.patientId() == patientId),
                "Registered visit was not present in the reception queue");
    }

    private static void softDeleteGeneratedRecords(int patientId, int cardId, List<Integer> visitIds)
            throws SQLException {
        if (patientId <= 0) return;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (int visitId : visitIds) {
                    softDeleteGeneratedRecord(connection, "LuotKham", visitId, patientId);
                }
                softDeleteGeneratedRecord(connection, "BaoHiem", cardId, patientId);
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

    private static void softDeleteGeneratedRecord(Connection connection, String table, int recordId, int patientId)
            throws SQLException {
        if (recordId <= 0) return;
        String sql = "UPDATE " + table
                + " SET isDeleted=1, deletedAt=SYSDATETIME(), deletedBy=NULL, version=version+1 "
                + "WHERE id=? AND maBenhNhan=? AND isDeleted=0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, recordId);
            statement.setInt(2, patientId);
            statement.executeUpdate();
        }
    }
}
