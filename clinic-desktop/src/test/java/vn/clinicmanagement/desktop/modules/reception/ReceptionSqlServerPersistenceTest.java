package vn.clinicmanagement.desktop.modules.reception;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.authorization.AuthorizationDecision;
import vn.clinicmanagement.application.authorization.AuthorizationRequest;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.reception.GetActiveReceptionRoomsQuery;
import vn.clinicmanagement.application.reception.GetReceptionQueueQuery;
import vn.clinicmanagement.application.reception.PatientSearchItem;
import vn.clinicmanagement.application.reception.RegisterPatientCommand;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.application.reception.SearchPatientsQuery;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.desktop.modules.reception.components.PatientRegistrationFormPanel;
import vn.clinicmanagement.infrastructure.persistence.DatabaseConnection;
import vn.clinicmanagement.infrastructure.persistence.DatabaseMigrationRunner;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.SystemRepositoryImpl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Clock;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

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
        int generatedEmployeeId = 0;

        try {
            SyntheticActor actor = createSyntheticActor(generatedSuffix);
            generatedEmployeeId = actor.employeeId();
            SessionLifecycleService lifecycle = syntheticLifecycle(actor);
            AuthorizationContext authorizationContext = lifecycle.currentAuthorizationContext().orElseThrow();
            AuthorizationPorts authorizationPorts = authorizationPortsFor(lifecycle);
            assertTrue(authorizationPorts.employees().getById(actor.employeeId()).isPresent(),
                    "Synthetic employee is not visible to authorization repository");
            assertTrue(authorizationPorts.employees().getAssignedRoles(actor.employeeId()).stream()
                            .anyMatch(role -> role.getId() == actor.roleId()
                                    && "RECEPTIONIST".equals(role.getTenVaiTro())),
                    "Synthetic receptionist role assignment is not visible");
            assertTrue(authorizationPorts.employees().getAssignedWorkScopes(actor.employeeId()).stream()
                            .anyMatch(scope -> scope.maCoSo() == actor.facilityId()
                                    && scope.maKhoa() == actor.departmentId()),
                    "Synthetic clinic scope assignment is not visible");
            assertTrue(authorizationPorts.system().getMatrixByRoleId(actor.roleId()).stream()
                            .anyMatch(grant -> "CATALOG".equals(grant.getPhanHe())
                                    && grant.getHanhDong() == HanhDongQuyen.XEM && grant.isDuocPhep()),
                    "Seeded receptionist catalog grant is not visible");
            AuthorizationDecision catalogDecision = authorizationPorts.authorization().evaluate(new AuthorizationRequest(
                    authorizationContext, "CATALOG", HanhDongQuyen.XEM,
                    authorizationContext.facilityId(), authorizationContext.departmentId()));
            assertTrue(catalogDecision.allowed(), "Synthetic receptionist authorization failed: "
                    + catalogDecision.reasonCode());
            ReceptionCompositionRoot.Services services = ReceptionCompositionRoot.create(lifecycle);
            var room = services.getActiveRooms().handle(new GetActiveReceptionRoomsQuery(
                    authorizationContext, lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow().stream().findFirst()
                    .orElseThrow(() -> new AssertionError("No active seeded clinic room is available"));

            LocalDate birthDate = LocalDate.of(1990, 1, 2);
            LocalDate cccdIssueDate = LocalDate.of(2010, 3, 4);
            LocalDate copayExemptDate = today.minusMonths(3);
            LocalDate fiveYearDate = today.minusYears(5);
            String patientName = "Synthetic Nguyen Patient" + generatedSuffix;
            String patientPhone = syntheticPhone;
            String patientAddress = "Synthetic test address " + generatedSuffix;
            String patientEmail = "synthetic." + generatedSuffix + "@example.invalid";
            String patientStreet = "Synthetic street " + generatedSuffix;
            String patientBirthplace = "Synthetic birthplace";
            String patientIssuePlace = "Synthetic issuing office";
            String patientWorkplace = "Synthetic workplace";
            String patientOccupation = "Synthetic occupation";
            String patientContactName = "Synthetic contact";
            String patientContactPhone = "08" + generatedSuffix;
            String provinceCode = "SP" + generatedSuffix.substring(0, 4);
            String districtCode = "SD" + generatedSuffix.substring(0, 4);
            String wardCode = "SW" + generatedSuffix.substring(0, 4);
            String insuranceFacilityCode = "SF" + generatedSuffix.substring(0, 4);
            String insuranceFacilityName = "Synthetic BHYT facility";
            String insuranceAddress = "Synthetic insurance address";
            BenhNhan expectedPatient = new BenhNhan();
            expectedPatient.setHo("Synthetic Nguyen");
            expectedPatient.setTen("Patient" + generatedSuffix);
            expectedPatient.setNgaySinh(birthDate);
            expectedPatient.setGioiTinh(GioiTinh.M);
            expectedPatient.setDienThoai(patientPhone);
            expectedPatient.setEmail(patientEmail);
            expectedPatient.setSoCCCD(syntheticCccd);
            expectedPatient.setDiaChi(patientAddress);
            expectedPatient.setSoNhaThonPho(patientStreet);
            expectedPatient.setNoiSinh(patientBirthplace);
            expectedPatient.setNgayCapCCCD(cccdIssueDate);
            expectedPatient.setNoiCapCCCD(patientIssuePlace);
            expectedPatient.setNoiLamViec(patientWorkplace);
            expectedPatient.setNgheNghiep(patientOccupation);
            expectedPatient.setDanToc("Tày");
            expectedPatient.setQuocTich("Việt Nam");
            expectedPatient.setMaTinh(provinceCode);
            expectedPatient.setMaHuyen(districtCode);
            expectedPatient.setMaXa(wardCode);
            expectedPatient.setNguoiLienHeTen(patientContactName);
            expectedPatient.setNguoiLienHeDienThoai(patientContactPhone);
            BaoHiem expectedInsurance = new BaoHiem();
            expectedInsurance.setLoaiBaoHiem(LoaiBaoHiem.BHYT);
            expectedInsurance.setSoBaoHiem(syntheticCardNumber);
            expectedInsurance.setNgayBatDau(initialCardStart);
            expectedInsurance.setNgayHetHan(initialCardEnd);
            expectedInsurance.setMucHuong(80);
            expectedInsurance.setMaNoiSong("K2");
            expectedInsurance.setMaNoiDangKyKCB(insuranceFacilityCode);
            expectedInsurance.setNoiDangKyKCB(insuranceFacilityName);
            expectedInsurance.setDiaChiThe(insuranceAddress);
            expectedInsurance.setDungTuyen(false);
            expectedInsurance.setCoGiayKhaiSinh(true);
            expectedInsurance.setKhongGiuThe(true);
            expectedInsurance.setMienCungChiTra(true);
            expectedInsurance.setNgayMienCungChiTra(copayExemptDate);
            expectedInsurance.setNgayDu5NamLienTuc(fiveYearDate);
            RegisterPatientCommand registrationCommand = onEdt(() -> {
                PatientRegistrationFormPanel form = new PatientRegistrationFormPanel();
                form.setClinicRooms(List.of(room));
                setText(form, "txtFullName", patientName);
                setText(form, "txtDateOfBirth", "02/01/1990");
                setCombo(form, "cbGender", "Nam");
                setText(form, "txtPhone", patientPhone);
                setText(form, "txtEmail", patientEmail);
                setText(form, "txtCccd", syntheticCccd);
                setText(form, "txtAddress", patientAddress);
                setText(form, "txtStreet", patientStreet);
                setText(form, "txtBirthPlace", patientBirthplace);
                setText(form, "txtCccdIssueDate", "04/03/2010");
                setText(form, "txtCccdIssuePlace", patientIssuePlace);
                setText(form, "txtWorkplace", patientWorkplace);
                setText(form, "txtOccupation", patientOccupation);
                setCombo(form, "cbEthnicity", "Tày");
                setCombo(form, "cbNationality", "Việt Nam");
                setText(form, "txtProvinceCode", provinceCode);
                setText(form, "txtDistrictCode", districtCode);
                setText(form, "txtWardCode", wardCode);
                setText(form, "txtContactName", patientContactName);
                setText(form, "txtContactPhone", patientContactPhone);
                setCombo(form, "cbRegistrationType", "Khám bệnh");
                setCombo(form, "cbPayerType", "BHYT");
                setCheckBox(form, "chkPriority", true);
                setText(form, "txtReason", "Synthetic persistence test");
                setText(form, "txtInsuranceNumber", syntheticCardNumber);
                setText(form, "txtInsuranceStartDate", formatDate(initialCardStart));
                setText(form, "txtInsuranceEndDate", formatDate(initialCardEnd));
                setText(form, "txtInsuranceRate", "80");
                setCombo(form, "cbLivingArea", "K2");
                setText(form, "txtFacilityCode", insuranceFacilityCode);
                setText(form, "txtFacilityName", insuranceFacilityName);
                setText(form, "txtInsuranceAddress", insuranceAddress);
                setCombo(form, "cbRoute", "Trái tuyến");
                setCheckBox(form, "chkBirthCertificate", true);
                setCheckBox(form, "chkNotHoldingCard", true);
                setCheckBox(form, "chkExemptCopay", true);
                setText(form, "txtCopayExemptDate", formatDate(copayExemptDate));
                setText(form, "txtFiveYearDate", formatDate(fiveYearDate));
                return form.toRegisterCommand(lifecycle.currentAuthorizationContext().orElseThrow(),
                        lifecycle.currentAuditActorContext().orElseThrow());
            });
            assertEquals(authorizationContext, registrationCommand.authorizationContext(),
                    "Registration form command must use the active synthetic session context");
            assertEquals(room.getId(), registrationCommand.roomId(),
                    "Registration form command must use the selected active clinic room");
            BenhNhan patient = registrationCommand.patient();
            BaoHiem insurance = registrationCommand.insurance();
            assertPatientAndInsurance(patient, insurance, expectedPatient, expectedInsurance);

            var registrationResult = services.registerPatient().handle(registrationCommand);
            assertTrue(registrationResult.isSuccess(), "Synthetic reception registration failed");
            var receipt = registrationResult.valueOrThrow();
            registeredPatientId = receipt.patientId();
            generatedCardId = insurance.getId();
            generatedVisitIds.add(receipt.visitId());
            assertTrue(registeredPatientId > 0, "Registration did not return a patient identifier");
            assertTrue(receipt.visitId() > 0, "Registration did not return a visit identifier");
            assertTrue(generatedCardId > 0, "Registration did not return a BHYT card identifier");
            assertActiveFacilityLink(registeredPatientId, authorizationContext.facilityId(),
                    authorizationContext.employeeId());

            PatientSearchItem initialMatch = findByCccd(
                    services.searchPatients().handle(new SearchPatientsQuery(syntheticCccd, authorizationContext,
                            lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow(),
                    registeredPatientId);
            assertNotNull(initialMatch.latestInsurance(), "BHYT card was not returned after registration");
            assertEquals(generatedCardId, initialMatch.latestInsurance().getId(),
                    "BHYT card identifier changed during initial reload");
            assertPatientAndInsurance(initialMatch.patient(), initialMatch.latestInsurance(),
                    expectedPatient, expectedInsurance);
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
            expectedPatient.setTen(updatedName);
            expectedPatient.setNgaySinh(LocalDate.of(1991, 2, 3));
            expectedPatient.setGioiTinh(GioiTinh.F);
            expectedPatient.setDienThoai(updatedPhone);
            expectedPatient.setDiaChi(updatedAddress);
            expectedInsurance.setSoBaoHiem(updatedCardNumber);
            expectedInsurance.setNgayBatDau(updatedCardStart);
            expectedInsurance.setNgayHetHan(updatedCardEnd);
            expectedInsurance.setMucHuong(90);

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
            assertActiveFacilityLink(registeredPatientId, authorizationContext.facilityId(),
                    authorizationContext.employeeId());

            ReceptionCompositionRoot.Services freshServices = ReceptionCompositionRoot.create(lifecycle);
            List<PatientSearchItem> reloadedMatches = freshServices.searchPatients()
                    .handle(new SearchPatientsQuery(syntheticCccd, authorizationContext,
                            lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow();
            assertEquals(1, reloadedMatches.size(),
                    "Updating the patient created a duplicate active patient with the same CCCD");
            PatientSearchItem reloadedMatch = findByCccd(reloadedMatches, registeredPatientId);
            assertPatientAndInsurance(reloadedMatch.patient(), reloadedMatch.latestInsurance(),
                    expectedPatient, expectedInsurance);
            assertEquals(generatedCardId, reloadedMatch.latestInsurance().getId(),
                    "Updating BHYT created a different current card instead of updating the existing card");
            List<ReceptionQueueItem> reloadedQueue = freshServices.getQueue()
                    .handle(new GetReceptionQueueQuery(LocalDate.now(), authorizationContext,
                            lifecycle.currentAuditActorContext().orElseThrow())).valueOrThrow().queue();
            assertQueueContains(reloadedQueue, receipt.visitId(), registeredPatientId);
            assertQueueContains(reloadedQueue, updateReceipt.visitId(), registeredPatientId);
        } finally {
            try {
                softDeleteGeneratedRecords(registeredPatientId, generatedCardId, generatedVisitIds);
            } finally {
                softDeleteGeneratedEmployee(generatedEmployeeId);
            }
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

    private static SessionLifecycleService syntheticLifecycle(SyntheticActor actor) {
        SessionLifecycleService lifecycle = new SessionLifecycleService(UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)), Clock.systemUTC());
        lifecycle.startSession(actor.employeeId(), actor.username(), "Synthetic Reception",
                new RoleChoice(actor.roleId(), "RECEPTIONIST", "Synthetic Receptionist"),
                new WorkScopeChoice(actor.facilityId(), actor.facilityName(),
                        actor.departmentId(), actor.departmentName()),
                UUID.randomUUID().toString());
        return lifecycle;
    }

    private static AuthorizationPorts authorizationPortsFor(SessionLifecycleService lifecycle) {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        NhanVienRepositoryImpl employees = new NhanVienRepositoryImpl(connections, unitOfWork);
        SystemRepositoryImpl system = new SystemRepositoryImpl(connections, unitOfWork);
        return new AuthorizationPorts(new AuthorizationService(lifecycle, employees, system), employees, system);
    }

    private static SyntheticActor createSyntheticActor(String suffix) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int roleId = queryRequiredInt(connection,
                        "SELECT id FROM VaiTro WHERE isDeleted=0 AND tenVaiTro=N'RECEPTIONIST'");
                int facilityId;
                int departmentId;
                String facilityName;
                String departmentName;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT TOP 1 cs.id, cs.tenCoSo, k.id, k.tenKhoa FROM PhongKham p "
                                + "INNER JOIN Khoa k ON k.id=p.maKhoa "
                                + "INNER JOIN CoSo cs ON cs.id=k.maCoSo "
                                + "WHERE p.hoatDong=1 AND p.isDeleted=0 AND k.hoatDong=1 AND k.isDeleted=0 "
                                + "AND cs.hoatDong=1 AND cs.isDeleted=0 ORDER BY cs.id, k.id, p.id")) {
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new AssertionError("No active seeded clinic room is available");
                        facilityId = result.getInt(1);
                        facilityName = result.getString(2);
                        departmentId = result.getInt(3);
                        departmentName = result.getString(4);
                    }
                }

                String code = "SYNTH-RECEPTION-" + suffix;
                String username = "synthetic-reception-" + suffix;
                int employeeId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO NhanVien (maNhanVien, ho, ten, username, hoatDong, "
                                + "ngayVaoLam, createdBy) OUTPUT INSERTED.id VALUES (?, N'Synthetic', "
                                + "N'Reception', ?, 1, CAST(GETDATE() AS DATE), NULL)")) {
                    statement.setString(1, code);
                    statement.setString(2, username);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("Synthetic employee insert returned no identifier");
                        employeeId = result.getInt(1);
                    }
                }
                insertEmployeeAssignment(connection, "NhanVienVaiTro", "maVaiTro", employeeId, roleId);
                insertEmployeeAssignment(connection, "PhanQuyenCoSo", "maCoSo", employeeId, facilityId);
                insertEmployeeAssignment(connection, "NhanVienKhoa", "maKhoa", employeeId, departmentId);
                connection.commit();
                return new SyntheticActor(employeeId, username, roleId, facilityId, facilityName,
                        departmentId, departmentName);
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private static int queryRequiredInt(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) throw new SQLException("Required seeded authorization record is missing");
            return result.getInt(1);
        }
    }

    private static void insertEmployeeAssignment(Connection connection, String table, String scopeColumn,
                                                 int employeeId, int scopeId) throws SQLException {
        String sql = "INSERT INTO " + table + " (maNhanVien, " + scopeColumn + ") VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            statement.setInt(2, scopeId);
            statement.executeUpdate();
        }
    }

    private static void softDeleteGeneratedEmployee(int employeeId) throws SQLException {
        if (employeeId <= 0) return;
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (String table : List.of("NhanVienVaiTro", "PhanQuyenCoSo", "NhanVienKhoa")) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE " + table + " SET isDeleted=1, deletedAt=SYSDATETIME(), deletedBy=NULL, "
                                    + "version=version+1 WHERE maNhanVien=? AND isDeleted=0")) {
                        statement.setInt(1, employeeId);
                        statement.executeUpdate();
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE NhanVien SET isDeleted=1, deletedAt=SYSDATETIME(), deletedBy=NULL, "
                                + "version=version+1 WHERE id=? AND isDeleted=0")) {
                    statement.setInt(1, employeeId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private record SyntheticActor(int employeeId, String username, int roleId,
                                  int facilityId, String facilityName,
                                  int departmentId, String departmentName) {}

    private record AuthorizationPorts(AuthorizationService authorization, NhanVienRepositoryImpl employees,
                                      SystemRepositoryImpl system) {}

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

    private static void assertPatientAndInsurance(BenhNhan actualPatient, BaoHiem actualInsurance,
                                                  BenhNhan expectedPatient, BaoHiem expectedInsurance) {
        assertEquals(expectedPatient.getHo(), actualPatient.getHo(), "Patient family name did not persist");
        assertEquals(expectedPatient.getTen(), actualPatient.getTen(), "Patient given name did not persist");
        assertEquals(expectedPatient.getNgaySinh(), actualPatient.getNgaySinh(), "Patient birth date did not persist");
        assertEquals(expectedPatient.getGioiTinh(), actualPatient.getGioiTinh(), "Patient gender did not persist");
        assertEquals(expectedPatient.getDienThoai(), actualPatient.getDienThoai(), "Patient phone did not persist");
        assertEquals(expectedPatient.getEmail(), actualPatient.getEmail(), "Patient email did not persist");
        assertEquals(expectedPatient.getSoCCCD(), actualPatient.getSoCCCD(), "Patient CCCD did not persist");
        assertEquals(expectedPatient.getDiaChi(), actualPatient.getDiaChi(), "Patient address did not persist");
        assertEquals(expectedPatient.getSoNhaThonPho(), actualPatient.getSoNhaThonPho(),
                "Patient street/neighborhood did not persist");
        assertEquals(expectedPatient.getNoiSinh(), actualPatient.getNoiSinh(), "Patient birthplace did not persist");
        assertEquals(expectedPatient.getNgayCapCCCD(), actualPatient.getNgayCapCCCD(),
                "Patient CCCD issue date did not persist");
        assertEquals(expectedPatient.getNoiCapCCCD(), actualPatient.getNoiCapCCCD(),
                "Patient CCCD issue place did not persist");
        assertEquals(expectedPatient.getNoiLamViec(), actualPatient.getNoiLamViec(),
                "Patient workplace did not persist");
        assertEquals(expectedPatient.getNgheNghiep(), actualPatient.getNgheNghiep(),
                "Patient occupation did not persist");
        assertEquals(expectedPatient.getDanToc(), actualPatient.getDanToc(), "Patient ethnicity did not persist");
        assertEquals(expectedPatient.getQuocTich(), actualPatient.getQuocTich(),
                "Patient nationality did not persist");
        assertEquals(expectedPatient.getMaTinh(), actualPatient.getMaTinh(), "Patient province code did not persist");
        assertEquals(expectedPatient.getMaHuyen(), actualPatient.getMaHuyen(),
                "Patient district code did not persist");
        assertEquals(expectedPatient.getMaXa(), actualPatient.getMaXa(), "Patient ward code did not persist");
        assertEquals(expectedPatient.getNguoiLienHeTen(), actualPatient.getNguoiLienHeTen(),
                "Patient contact name did not persist");
        assertEquals(expectedPatient.getNguoiLienHeDienThoai(), actualPatient.getNguoiLienHeDienThoai(),
                "Patient contact phone did not persist");

        assertNotNull(actualInsurance, "BHYT card was not returned with the patient");
        assertEquals(expectedInsurance.getLoaiBaoHiem(), actualInsurance.getLoaiBaoHiem(),
                "Insurance type was not persisted");
        assertEquals(expectedInsurance.getSoBaoHiem(), actualInsurance.getSoBaoHiem(),
                "BHYT number did not persist");
        assertEquals(expectedInsurance.getNgayBatDau(), actualInsurance.getNgayBatDau(),
                "BHYT start date did not persist");
        assertEquals(expectedInsurance.getNgayHetHan(), actualInsurance.getNgayHetHan(),
                "BHYT expiry date did not persist");
        assertEquals(expectedInsurance.getMucHuong(), actualInsurance.getMucHuong(),
                "BHYT coverage did not persist");
        assertEquals(expectedInsurance.getMaNoiSong(), actualInsurance.getMaNoiSong(),
                "Insurance living-area code did not persist");
        assertEquals(expectedInsurance.getMaNoiDangKyKCB(), actualInsurance.getMaNoiDangKyKCB(),
                "Insurance facility code did not persist");
        assertEquals(expectedInsurance.getNoiDangKyKCB(), actualInsurance.getNoiDangKyKCB(),
                "Insurance facility name did not persist");
        assertEquals(expectedInsurance.getDiaChiThe(), actualInsurance.getDiaChiThe(),
                "Insurance address did not persist");
        assertEquals(expectedInsurance.isDungTuyen(), actualInsurance.isDungTuyen(),
                "Insurance route status did not persist");
        assertEquals(expectedInsurance.isCoGiayKhaiSinh(), actualInsurance.isCoGiayKhaiSinh(),
                "Insurance birth-certificate flag did not persist");
        assertEquals(expectedInsurance.isKhongGiuThe(), actualInsurance.isKhongGiuThe(),
                "Insurance card-retention flag did not persist");
        assertEquals(expectedInsurance.isMienCungChiTra(), actualInsurance.isMienCungChiTra(),
                "Insurance copay-exemption flag did not persist");
        assertEquals(expectedInsurance.getNgayMienCungChiTra(), actualInsurance.getNgayMienCungChiTra(),
                "Insurance copay-exemption date did not persist");
        assertEquals(expectedInsurance.getNgayDu5NamLienTuc(), actualInsurance.getNgayDu5NamLienTuc(),
                "Insurance five-year date did not persist");
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return action.call();
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(action.call());
            } catch (Exception ex) {
                failure.set(ex);
            } catch (Error ex) {
                failure.set(ex);
            }
        });
        Throwable ex = failure.get();
        if (ex instanceof Exception exception) throw exception;
        if (ex instanceof Error error) throw error;
        if (ex != null) throw new IllegalStateException(ex);
        return result.get();
    }

    private static void setText(PatientRegistrationFormPanel form, String fieldName, String value)
            throws ReflectiveOperationException {
        field(form, fieldName, JTextField.class).setText(value);
    }

    private static void setCombo(PatientRegistrationFormPanel form, String fieldName, String value)
            throws ReflectiveOperationException {
        field(form, fieldName, JComboBox.class).setSelectedItem(value);
    }

    private static void setCheckBox(PatientRegistrationFormPanel form, String fieldName, boolean value)
            throws ReflectiveOperationException {
        field(form, fieldName, JCheckBox.class).setSelected(value);
    }

    private static <T> T field(PatientRegistrationFormPanel form, String fieldName, Class<T> type)
            throws ReflectiveOperationException {
        Field field = PatientRegistrationFormPanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(form));
    }

    private static String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static void assertQueueContains(List<ReceptionQueueItem> queue, int visitId, int patientId) {
        assertTrue(queue.stream().anyMatch(item -> item.visitId() == visitId && item.patientId() == patientId),
                "Registered visit was not present in the reception queue");
    }

    private static void assertActiveFacilityLink(int patientId, int facilityId, int actorId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*), MIN(createdBy) FROM BenhNhanCoSo "
                             + "WHERE maBenhNhan=? AND maCoSo=? AND isDeleted=0")) {
            statement.setInt(1, patientId);
            statement.setInt(2, facilityId);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1),
                        "Registration must leave exactly one active link to the selected facility");
                assertEquals(actorId, result.getInt(2),
                        "The active patient-facility link must record the authenticated actor");
            }
        }
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
                        "UPDATE BenhNhanCoSo SET isDeleted=1, deletedAt=SYSDATETIME(), deletedBy=NULL, "
                                + "version=version+1 WHERE maBenhNhan=? AND isDeleted=0")) {
                    statement.setInt(1, patientId);
                    statement.executeUpdate();
                }
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
