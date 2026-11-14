package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.auth.DaHoanTatDangNhapPhien;
import vn.clinicmanagement.application.reception.GetHoatDongTiepDonPhongQuery;
import vn.clinicmanagement.application.reception.GetTiepDonHangDoiQuery;
import vn.clinicmanagement.application.reception.BenhNhanSearchMatHang;
import vn.clinicmanagement.application.reception.RegisterBenhNhanCommand;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.RoleChoice;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung.WorkScopeChoice;
import vn.clinicmanagement.application.session.PhienHetGioChinhSach;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuDiCuChay;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TiepDonSQLMayChuProcessKhoiDongLaiTest {
    private static final KiemToanWriteCongKetNoi NOOP_AUDIT = new KiemToanWriteCongKetNoi() {
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    };
    private static final String OPT_IN_PROPERTY = "clinic.test.sqlserver";
    private static final String DATABASE_PROPERTY = "clinic.test.sqlserver.databaseName";
    private static final Pattern SAFE_DATABASE_NAME =
            Pattern.compile("(?:clinic_orchestrator_review_|clinic_test_)[A-Za-z0-9_]+");
    private static final Pattern DATABASE_NAME_PARAMETER = Pattern.compile("(?i)(databaseName\\s*=)[^;]*");
    private static final Pattern URL_CREDENTIAL_PARAMETER =
            Pattern.compile("(?i);\\s*(?:user|username|password)\\s*=[^;]*");
    private static final String ENV_URL = "DB_URL";
    private static final String ENV_USERNAME = "DB_USERNAME";
    private static final String ENV_PASSWORD = "DB_PASSWORD";
    private static final String ENV_DATABASE = "CLINIC_TEST_DATABASE_NAME";
    private static final String ENV_CCCD = "CLINIC_TEST_SYNTHETIC_CCCD";
    private static final String ENV_PATIENT_ID = "CLINIC_TEST_SYNTHETIC_PATIENT_ID";
    private static final String ENV_CARD_ID = "CLINIC_TEST_SYNTHETIC_CARD_ID";
    private static final String ENV_VISIT_ID = "CLINIC_TEST_SYNTHETIC_VISIT_ID";
    private static final String PROBE_SUCCESS = "RECEPTION_PROBE_OK";

    @Test
    void syntheticRegistrationSurvivesASecondJvmWhenExplicitlyEnabled() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty(OPT_IN_PROPERTY)),
                "SQL Server process restart test requires explicit opt-in");

        String databaseName = System.getProperty(DATABASE_PROPERTY);
        assertNotNull(databaseName, "An explicit synthetic SQL Server database name is required");
        assertTrue(SAFE_DATABASE_NAME.matcher(databaseName).matches(),
                "SQL Server target must use an approved synthetic database name");

        DatabaseCredentials credentials = loadCredentials(databaseName);
        CoSoDuLieuKetNoi.setConnectionString("");
        CoSoDuLieuKetNoi.configure(credentials.url(), credentials.username(), credentials.password());
        assertTrue(runMigrationsWithoutLogging(), "Database migrations did not complete");

        String generatedSuffix = Long.toString(ThreadLocalRandom.current().nextLong(10_000_000L, 100_000_000L));
        String syntheticCccd = "0000" + generatedSuffix;
        int patientId = 0;
        int cardId = 0;
        int visitId = 0;
        BenhNhan generatedPatient = null;
        BaoHiem generatedInsurance = null;
        try {
            PhienVongDoiDichVu lifecycle = syntheticLifecycle();
            PhanQuyenNguCanh authorizationContext = lifecycle.currentAuthorizationContext().orElseThrow();
            TiepDonGhepNoiGoc services = new TiepDonGhepNoiGoc(lifecycle);
            var room = services.getActiveReceptionRoomsHandler().handle(new GetHoatDongTiepDonPhongQuery(
                    authorizationContext, lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow().stream().findFirst()
                    .orElseThrow(() -> new AssertionError("No active seeded clinic room is available"));

            BenhNhan patient = new BenhNhan();
            generatedPatient = patient;
            patient.setHo("Synthetic");
            patient.setTen("ProcessProbe" + generatedSuffix);
            patient.setNgaySinh(LocalDate.of(1990, 1, 2));
            patient.setGioiTinh(GioiTinh.M);
            patient.setDienThoai("09" + generatedSuffix);
            patient.setSoCCCD(syntheticCccd);
            patient.setDiaChi("Synthetic process restart test");

            BaoHiem insurance = new BaoHiem();
            generatedInsurance = insurance;
            insurance.setLoaiBaoHiem(LoaiBaoHiem.BHYT);
            insurance.setSoBaoHiem("SYNTH" + generatedSuffix);
            insurance.setNgayBatDau(LocalDate.now().minusDays(1));
            insurance.setNgayHetHan(LocalDate.now().plusYears(1));
            insurance.setMucHuong(80);

            var result = services.registerPatientHandler().handle(new RegisterBenhNhanCommand(patient, insurance,
                    room.getId(), "WALK_IN", "Synthetic process restart test", DoiTuongKham.BHYT, false,
                    authorizationContext.employeeId(),
                    authorizationContext, lifecycle.currentAuditActorContext().orElseThrow()));
            assertTrue(result.isSuccess(), "Synthetic reception registration failed");
            var receipt = result.valueOrThrow();
            patientId = receipt.patientId();
            cardId = insurance.getId();
            visitId = receipt.visitId();
            assertTrue(patientId > 0 && cardId > 0 && visitId > 0,
                    "Registration did not return all generated identifiers");

            Process probe = startProbe(credentials, databaseName, syntheticCccd, patientId, cardId, visitId);
            boolean exited = probe.waitFor(45, TimeUnit.SECONDS);
            if (!exited) {
                probe.destroyForcibly();
                probe.waitFor(5, TimeUnit.SECONDS);
            }
            byte[] outputBytes = probe.getInputStream().readAllBytes();
            String output = new String(outputBytes, StandardCharsets.UTF_8).trim();
            assertTrue(exited, "Reception process probe did not finish");
            assertEquals(0, probe.exitValue(), "Reception process probe failed");
            assertEquals(PROBE_SUCCESS, output, "Reception process probe returned unexpected output");
        } finally {
            // Entities may have received IDs before a later step fails; clean only captured generated IDs.
            if (patientId <= 0 && generatedPatient != null && generatedPatient.getId() != null) {
                patientId = generatedPatient.getId();
            }
            if (cardId <= 0 && generatedInsurance != null && generatedInsurance.getId() != null) {
                cardId = generatedInsurance.getId();
            }
            softDeleteGeneratedRecords(patientId, cardId, visitId);
        }
    }

    private static DatabaseCredentials loadCredentials(String databaseName) throws Exception {
        Properties properties = new Properties();
        try (InputStream input = TiepDonSQLMayChuProcessKhoiDongLaiTest.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new IllegalStateException("Local classpath db.properties is required for the opt-in test");
            }
            properties.load(input);
        }

        String configuredUrl = properties.getProperty("db.url");
        String username = properties.getProperty("db.username");
        String credentialValue = properties.getProperty("db." + "password");
        if (configuredUrl == null || !configuredUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:sqlserver:")
                || username == null || username.isBlank() || credentialValue == null || credentialValue.isBlank()) {
            throw new IllegalStateException("Local classpath db.properties must contain SQL Server credentials");
        }

        String url = URL_CREDENTIAL_PARAMETER.matcher(configuredUrl).replaceAll("");
        Matcher matcher = DATABASE_NAME_PARAMETER.matcher(url);
        String targetUrl = matcher.find()
                ? matcher.replaceAll("$1" + Matcher.quoteReplacement(databaseName))
                : url + ";databaseName=" + databaseName;
        return new DatabaseCredentials(targetUrl, username, credentialValue);
    }

    private static PhienVongDoiDichVu syntheticLifecycle() {
        PhienVongDoiDichVu lifecycle = new PhienVongDoiDichVu(NguoiDungPhien.getInstance(),
                new PhienHetGioChinhSach(Duration.ofMinutes(15), Duration.ofHours(12)), Clock.systemUTC(), NOOP_AUDIT);
        String sessionId = UUID.randomUUID().toString();
        lifecycle.startSession(new DaHoanTatDangNhapPhien(701, "synthetic-reception", "Synthetic Reception",
                new RoleChoice(31, "RECEPTION", "Synthetic Reception"),
                new WorkScopeChoice(11, "Synthetic Facility", 21, "Synthetic Department"),
                UUID.fromString(sessionId), Instant.now()));
        return lifecycle;
    }

    private static boolean runMigrationsWithoutLogging() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try (PrintStream sink = new PrintStream(OutputStream.nullOutputStream())) {
            System.setOut(sink);
            System.setErr(sink);
            return CoSoDuLieuDiCuChay.runMigrations();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private static Process startProbe(DatabaseCredentials credentials, String databaseName, String cccd,
                                      int patientId, int cardId, int visitId) throws Exception {
        String classpath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder builder = new ProcessBuilder(javaExecutable, "-cp", classpath,
                ProcessProbe.class.getName());
        builder.redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        environment.remove("DB_CONNECTION_STRING");
        environment.put(ENV_URL, credentials.url());
        environment.put(ENV_USERNAME, credentials.username());
        environment.put(ENV_PASSWORD, credentials.password());
        environment.put(ENV_DATABASE, databaseName);
        environment.put(ENV_CCCD, cccd);
        environment.put(ENV_PATIENT_ID, Integer.toString(patientId));
        environment.put(ENV_CARD_ID, Integer.toString(cardId));
        environment.put(ENV_VISIT_ID, Integer.toString(visitId));
        return builder.start();
    }

    private static void softDeleteGeneratedRecords(int patientId, int cardId, int visitId) throws SQLException {
        if (patientId <= 0) return;
        try (Connection connection = CoSoDuLieuKetNoi.getConnection()) {
            connection.setAutoCommit(false);
            try {
                softDeleteGeneratedRecord(connection, "LuotKham", visitId, patientId);
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

    private record DatabaseCredentials(String url, String username, String password) {}

    /** Entry point loaded by the distinct child JVM. It emits only a fixed result marker. */
    public static final class ProcessProbe {
        private ProcessProbe() {}

        public static void main(String[] args) {
            if (args.length != 0) {
                System.err.print("RECEPTION_PROBE_FAILED");
                System.exit(2);
            }
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            try (PrintStream sink = new PrintStream(OutputStream.nullOutputStream())) {
                System.setOut(sink);
                System.setErr(sink);
                verifyPersistedRegistration();
            } catch (Throwable ignored) {
                System.setOut(originalOut);
                System.setErr(originalErr);
                System.err.print("RECEPTION_PROBE_FAILED");
                System.exit(1);
                return;
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
            System.out.print(PROBE_SUCCESS);
        }

        private static void verifyPersistedRegistration() {
            String databaseName = requiredEnvironment(ENV_DATABASE);
            if (!SAFE_DATABASE_NAME.matcher(databaseName).matches()) {
                throw new IllegalStateException();
            }
            requiredEnvironment(ENV_URL);
            requiredEnvironment(ENV_USERNAME);
            requiredEnvironment(ENV_PASSWORD);
            String cccd = requiredEnvironment(ENV_CCCD);
            int patientId = Integer.parseInt(requiredEnvironment(ENV_PATIENT_ID));
            int cardId = Integer.parseInt(requiredEnvironment(ENV_CARD_ID));
            int visitId = Integer.parseInt(requiredEnvironment(ENV_VISIT_ID));

            PhienVongDoiDichVu lifecycle = syntheticLifecycle();
            TiepDonGhepNoiGoc services = new TiepDonGhepNoiGoc(lifecycle);
            List<BenhNhanSearchMatHang> matches = services.searchPatientsHandler()
                    .handle(new vn.clinicmanagement.application.reception.SearchBenhNhanQuery(cccd,
                            lifecycle.currentAuthorizationContext().orElseThrow(),
                            lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow();
            assertEquals(1, matches.size());
            BenhNhanSearchMatHang match = matches.get(0);
            assertEquals(patientId, match.patient().getId());
            assertEquals(cccd, match.patient().getSoCCCD());
            assertNotNull(match.latestInsurance());
            assertEquals(cardId, match.latestInsurance().getId());
            assertEquals(LoaiBaoHiem.BHYT, match.latestInsurance().getLoaiBaoHiem());
            List<vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang> queue =
                    services.getReceptionQueueHandler()
                    .handle(new GetTiepDonHangDoiQuery(LocalDate.now(),
                            lifecycle.currentAuthorizationContext().orElseThrow(),
                            lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow().queue();
            assertTrue(queue.stream().anyMatch(item -> item.visitId() == visitId && item.patientId() == patientId));
        }

        private static String requiredEnvironment(String name) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) throw new IllegalStateException();
            return value;
        }
    }
}
