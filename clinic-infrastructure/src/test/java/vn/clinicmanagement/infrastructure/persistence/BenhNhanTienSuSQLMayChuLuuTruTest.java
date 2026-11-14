package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.patienthistory.GetBenhNhanTienSuHandler;
import vn.clinicmanagement.application.patienthistory.GetBenhNhanTienSuQuery;
import vn.clinicmanagement.application.patienthistory.BenhNhanTienSuBienNhan;
import vn.clinicmanagement.application.patienthistory.SaveBenhNhanTienSuCommand;
import vn.clinicmanagement.application.patienthistory.SaveBenhNhanTienSuHandler;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import vn.clinicmanagement.infrastructure.persistence.patient.BenhNhanRepository;
import vn.clinicmanagement.infrastructure.persistence.patient.TienSuBenhRepository;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenhNhanTienSuSQLMayChuLuuTruTest {
    private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile("clinic_test_[A-Za-z0-9_]+");
    private static final Pattern DATABASE_OPTION_PATTERN =
            Pattern.compile("(?i);\\s*(?:databaseName|database)\\s*=[^;]*");
    private static final Pattern EMBEDDED_CREDENTIAL_PATTERN =
            Pattern.compile("(?i);\\s*(?:user|username|password)\\s*=");

    @Test
    void persistsAndAuditsTheApprovedPatientHistoryWorkflowOnSqlServer() throws Exception {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getProperty("clinic.test.sqlserver"))
                        || Boolean.parseBoolean(System.getProperty("clinic.sqlserver.audit.enabled")),
                "Set clinic.test.sqlserver=true or clinic.sqlserver.audit.enabled=true to opt in"
        );

        String databaseName = resolveDatabaseName();
        Assumptions.assumeTrue(
                databaseName != null && DATABASE_NAME_PATTERN.matcher(databaseName).matches(),
                "Set clinic.test.sqlserver.databaseName or clinic.sqlserver.test.database "
                        + "to an explicitly named clinic_test_* database"
        );
        configureSyntheticDatabase(databaseName);

        try (Connection connection = CoSoDuLieuKetNoi.getConnection()) {
            if (!databaseName.equals(queryString(connection, "SELECT DB_NAME()"))) {
                throw new AssertionError(
                        "The SQL Server connection did not select the exact configured clinic_test_* database"
                );
            }
            assertV10Ready(connection);
        } catch (SQLException exception) {
            throw new AssertionError(
                    "SQL Server is unavailable. Configure an existing migrated clinic_test_* database "
                            + "through the ignored local db.properties and rerun with the SQL Server opt-in; "
                            + "connection details are suppressed."
            );
        }

        Fixture fixture = new Fixture();
        String temporaryTrigger = "TR_PatientHistoryReject_" + UUID.randomUUID().toString().replace("-", "");
        boolean triggerInstalled = false;
        try {
            createFixture(fixture);
            Workflow workflow = createWorkflow(fixture);

            UUID insertCorrelation = UUID.randomUUID();
            KetQua<BenhNhanTienSuBienNhan> insertResult = workflow.saveHandler().handle(
                    command(fixture, workflow, null, null, LoaiTienSu.DI_UNG,
                            "Synthetic allergy history", insertCorrelation));
            assertTrue(insertResult.isSuccess(), "The scoped V10-triggered history insert should succeed");
            BenhNhanTienSuBienNhan receipt = insertResult.valueOrThrow();
            fixture.historyIds.add(receipt.historyId());
            assertTrue(receipt.historyId() > 0, "The JDBC adapter should return the generated identity");
            assertEquals(0L, receipt.version(), "A newly inserted history row starts at version zero");
            assertHistoryMetadata(fixture, workflow, receipt.historyId(), true);
            assertSuccessfulAudit(insertCorrelation, "PATIENT_UPDATED", fixture, workflow);

            int chronicConditionId = insertHistory(
                    fixture.patientId, fixture.facilityId, fixture.departmentId, "BENH_NEN",
                    "Synthetic chronic history", workflow.employeeId());
            fixture.historyIds.add(chronicConditionId);
            int surgeryId = insertHistory(
                    fixture.patientId, fixture.facilityId, fixture.departmentId, "PHAU_THUAT",
                    "Synthetic surgery history", workflow.employeeId());
            fixture.historyIds.add(surgeryId);
            fixture.historyIds.add(insertHistory(
                    fixture.patientId, fixture.facilityId, fixture.otherDepartmentId, "BENH_NEN",
                    "Synthetic other-department history", workflow.employeeId()));
            fixture.historyIds.add(insertHistory(
                    fixture.patientId, fixture.otherFacilityId, fixture.otherFacilityDepartmentId, "DI_UNG",
                    "Synthetic other-facility history", workflow.employeeId()));
            fixture.historyIds.add(insertHistory(
                    fixture.otherPatientId, fixture.facilityId, fixture.departmentId, "DI_UNG",
                    "Synthetic other-patient history", workflow.employeeId()));
            fixture.historyIds.add(insertHistory(
                    fixture.patientId, fixture.facilityId, fixture.departmentId, "TIEN_SU_GIA_DINH",
                    "Synthetic unsupported-category history", workflow.employeeId()));
            int deletedHistoryId = insertHistory(
                    fixture.patientId, fixture.facilityId, fixture.departmentId, "DI_UNG",
                    "Synthetic inactive history", workflow.employeeId());
            fixture.historyIds.add(deletedHistoryId);
            executeUpdate("UPDATE TienSuBenh SET isDeleted=1 WHERE id=?", deletedHistoryId);

            UUID updateCorrelation = UUID.randomUUID();
            KetQua<BenhNhanTienSuBienNhan> updateResult = workflow.saveHandler().handle(
                    command(fixture, workflow, receipt.historyId(), receipt.version(), LoaiTienSu.DI_UNG,
                            "Synthetic updated allergy history", updateCorrelation));
            assertTrue(updateResult.isSuccess(), "The scoped history update should succeed");
            assertEquals(1L, updateResult.valueOrThrow().version(), "The successful update increments version");
            assertHistoryMetadata(fixture, workflow, receipt.historyId(), false);
            assertSuccessfulAudit(updateCorrelation, "PATIENT_UPDATED", fixture, workflow);

            UUID readCorrelation = UUID.randomUUID();
            KetQua<List<TienSuBenh>> readResult = workflow.getHandler().handle(new GetBenhNhanTienSuQuery(
                    fixture.patientId, workflow.authorizationContext(),
                    workflow.actor(readCorrelation)));
            assertTrue(readResult.isSuccess(), "The approved exact-scope read should succeed");
            List<TienSuBenh> histories = readResult.valueOrThrow();
            assertEquals(
                    List.of(receipt.historyId(), chronicConditionId, surgeryId),
                    histories.stream().map(TienSuBenh::getId).toList(),
                    "The read must include only active approved categories for the exact patient and scope"
            );
            assertTrue(histories.stream().allMatch(row ->
                            row.hasExactOwnershipScope(fixture.facilityId, fixture.departmentId)),
                    "Every returned row must have the exact facility and department pair");
            assertReadAudit(readCorrelation, fixture, workflow);

            UUID rejectedCorrelation = UUID.randomUUID();
            installRejectingTrigger(temporaryTrigger, rejectedCorrelation);
            triggerInstalled = true;
            KetQua<BenhNhanTienSuBienNhan> rejectedUpdate = workflow.saveHandler().handle(
                    command(fixture, workflow, receipt.historyId(), 1L, LoaiTienSu.DI_UNG,
                            "Synthetic update rejected by audit trigger", rejectedCorrelation));
            assertFalse(rejectedUpdate.isSuccess(), "A rejected audit insert must fail the write workflow");
            assertHistoryUnchangedAfterRejectedAudit(receipt.historyId(), rejectedCorrelation);
        } finally {
            cleanup(fixture, temporaryTrigger, triggerInstalled);
        }
    }

    private static void createFixture(Fixture fixture) throws SQLException {
        try (Connection connection = CoSoDuLieuKetNoi.getConnection()) {
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT TOP (1) f.id AS facilityId, k.id AS departmentId, "
                                 + "r.id AS roleId, e.id AS employeeId "
                                 + "FROM CoSo f JOIN Khoa k ON k.maCoSo=f.id "
                                 + "CROSS JOIN VaiTro r CROSS JOIN NhanVien e "
                                 + "WHERE f.isDeleted=0 AND f.hoatDong=1 AND k.isDeleted=0 "
                                 + "AND r.isDeleted=0 AND r.tenVaiTro=N'DOCTOR' "
                                 + "AND e.isDeleted=0 AND e.hoatDong=1 ORDER BY f.id, k.id, e.id")) {
                if (!result.next()) {
                    throw new AssertionError(
                            "The migrated clinic_test_* database must contain an active facility, department, "
                                    + "DOCTOR role, and employee reference."
                    );
                }
                fixture.facilityId = result.getInt("facilityId");
                fixture.departmentId = result.getInt("departmentId");
                fixture.roleId = result.getInt("roleId");
                fixture.employeeId = result.getInt("employeeId");
            }
        }

        String suffix = UUID.randomUUID().toString().replace("-", "");
        fixture.otherDepartmentId = insertIdentity(
                "INSERT INTO Khoa (maCoSo, tenKhoa) OUTPUT INSERTED.id VALUES (?, ?)",
                fixture.facilityId, "Synthetic history department " + suffix);
        fixture.otherFacilityId = insertIdentity(
                "INSERT INTO CoSo (tenCoSo) OUTPUT INSERTED.id VALUES (?)",
                "Synthetic history facility " + suffix);
        fixture.otherFacilityDepartmentId = insertIdentity(
                "INSERT INTO Khoa (maCoSo, tenKhoa) OUTPUT INSERTED.id VALUES (?, ?)",
                fixture.otherFacilityId, "Synthetic other-facility department " + suffix);
        fixture.patientId = insertPatient("HISTORY-" + suffix + "-A");
        fixture.patientIds.add(fixture.patientId);
        fixture.otherPatientId = insertPatient("HISTORY-" + suffix + "-B");
        fixture.patientIds.add(fixture.otherPatientId);
        fixture.visitIds.add(insertIdentity(
                "INSERT INTO LuotKham (maBenhNhan, maCoSo, maKhoa) OUTPUT INSERTED.id VALUES (?, ?, ?)",
                fixture.patientId, fixture.facilityId, fixture.departmentId));
    }

    private static Workflow createWorkflow(Fixture fixture) {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = new JdbcGiaoDichQuanLy(unitOfWork);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        TienSuBenhRepository histories = new TienSuBenhRepository(connections);
        IBenhNhanRepository patients = new BenhNhanRepository(connections, unitOfWork);
        JdbcKiemToanRepository audit = new JdbcKiemToanRepository(
                connections, unitOfWork, CoSoDuLieuKetNoi::getConnection);
        UUID sessionId = UUID.randomUUID();
        PhanQuyenNguCanh authorizationContext = new PhanQuyenNguCanh(
                fixture.employeeId, fixture.roleId, "DOCTOR", fixture.facilityId,
                fixture.departmentId, sessionId);
        PhanQuyenDichVu authorization = new PhanQuyenDichVu(
                requestedSession -> requestedSession.equals(sessionId)
                        ? Optional.of(new HoatDongPhienCongKetNoi.ActiveSession(
                                sessionId, fixture.employeeId, fixture.roleId, "DOCTOR",
                                fixture.facilityId, fixture.departmentId, true, true, true))
                        : Optional.empty(),
                syntheticEmployeeRepository(fixture),
                syntheticPermissionRepository(fixture));
        return new Workflow(
                fixture, authorizationContext,
                new SaveBenhNhanTienSuHandler(histories, patients, authorization, transactions, audit),
                new GetBenhNhanTienSuHandler(histories, authorization, audit));
    }

    private static INhanVienRepository syntheticEmployeeRepository(Fixture fixture) {
        return (INhanVienRepository) java.lang.reflect.Proxy.newProxyInstance(
                INhanVienRepository.class.getClassLoader(),
                new Class<?>[]{INhanVienRepository.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getById" -> (int) arguments[0] == fixture.employeeId
                            ? Optional.of(syntheticEmployee(fixture.employeeId)) : Optional.empty();
                    case "getAssignedRoles" -> {
                        requireSyntheticEmployee(fixture, arguments);
                        VaiTro role = new VaiTro();
                        role.setId(fixture.roleId);
                        role.setTenVaiTro("DOCTOR");
                        yield List.of(role);
                    }
                    case "getAssignedWorkScopes" -> {
                        requireSyntheticEmployee(fixture, arguments);
                        yield List.of(new NhanVienWorkPhamVi(
                                fixture.facilityId, "Synthetic test scope",
                                fixture.departmentId, "Synthetic test department"));
                    }
                    case "toString" -> "SyntheticPatientHistoryAuthorizationEmployees";
                    default -> throw new UnsupportedOperationException(
                            "Only synthetic patient-history authorization reads are supported.");
                });
    }

    private static IHeThongRepository syntheticPermissionRepository(Fixture fixture) {
        return new IHeThongRepository() {
            @Override
            public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
                if (roleId != fixture.roleId) return List.of();
                MaTranPhanQuyen grant = new MaTranPhanQuyen();
                grant.setId(1);
                grant.setMaVaiTro(fixture.roleId);
                grant.setPhanHe("PATIENT");
                grant.setHanhDong(HanhDongQuyen.XEM);
                grant.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);
                grant.setDuocPhep(true);
                return List.of(grant);
            }

            @Override
            public Optional<String> getSystemParameterValue(String key) {
                return Optional.empty();
            }
        };
    }

    private static NhanVien syntheticEmployee(int employeeId) {
        NhanVien employee = new NhanVien();
        employee.setId(employeeId);
        employee.setHoatDong(true);
        return employee;
    }

    private static void requireSyntheticEmployee(Fixture fixture, Object[] arguments) {
        if (arguments == null || arguments.length == 0
                || (int) arguments[0] != fixture.employeeId) {
            throw new IllegalStateException("Synthetic authorization employee was not selected.");
        }
    }

    private static SaveBenhNhanTienSuCommand command(
            Fixture fixture,
            Workflow workflow,
            Integer historyId,
            Long expectedVersion,
            LoaiTienSu category,
            String narrative,
            UUID correlationId) {
        return new SaveBenhNhanTienSuCommand(
                fixture.patientId, historyId, expectedVersion, category, narrative,
                workflow.authorizationContext(), workflow.actor(correlationId));
    }

    private static void assertHistoryMetadata(
            Fixture fixture, Workflow workflow, int historyId, boolean inserted) throws SQLException {
        String metadataColumn = inserted ? "createdAt" : "updatedAt";
        String sql = "SELECT version, createdAt, createdBy, updatedAt, updatedBy, maCoSo, maKhoa, moTa, "
                + "CASE WHEN " + metadataColumn + " BETWEEN DATEADD(MINUTE, -2, SYSUTCDATETIME()) "
                + "AND DATEADD(MINUTE, 2, SYSUTCDATETIME()) THEN 1 ELSE 0 END AS metadataIsUtc, "
                + "CASE WHEN updatedAt IS NOT NULL AND updatedBy=? THEN 1 ELSE 0 END AS updateActorMatches "
                + "FROM TienSuBenh WHERE id=?";
        try (Connection connection = CoSoDuLieuKetNoi.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, workflow.employeeId());
            statement.setInt(2, historyId);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next(), "The synthetic history row should be persisted");
                assertEquals(inserted ? 0L : 1L, result.getLong("version"));
                assertEquals(fixture.employeeId, result.getInt(inserted ? "createdBy" : "updatedBy"));
                assertEquals(fixture.facilityId, result.getInt("maCoSo"));
                assertEquals(fixture.departmentId, result.getInt("maKhoa"));
                assertEquals(
                        inserted ? "Synthetic allergy history" : "Synthetic updated allergy history",
                        result.getString("moTa"));
                Timestamp timestamp = result.getTimestamp(inserted ? "createdAt" : "updatedAt");
                assertNotNull(timestamp, "The history write should persist UTC metadata");
                assertEquals(
                        1, result.getInt("metadataIsUtc"),
                        "History metadata must be generated from SQL Server UTC time");
                if (inserted) {
                    assertNull(result.getObject("updatedBy"), "An inserted history row has no update actor");
                    assertEquals(
                            fixture.employeeId, result.getInt("createdBy"),
                            "Creation metadata should use the authenticated actor");
                } else {
                    assertEquals(1, result.getInt("updateActorMatches"),
                            "An update should store its actor and UTC timestamp");
                }
            }
        }
    }

    private static void assertSuccessfulAudit(
            UUID correlationId, String action, Fixture fixture, Workflow workflow) throws SQLException {
        try (Connection connection = CoSoDuLieuKetNoi.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT hanhDong, loaiDoiTuong, maDoiTuong, maNhanVien, maVaiTroCode, maCoSo, "
                             + "maKhoa, ketQua, chiTiet FROM NhatKyHeThong WHERE correlationId=?")) {
            statement.setString(1, correlationId.toString());
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next(), "The successful history mutation should be audited");
                assertEquals(action, result.getString("hanhDong"));
                assertEquals("PATIENT", result.getString("loaiDoiTuong"));
                assertEquals(Integer.toString(fixture.patientId), result.getString("maDoiTuong"));
                assertEquals(workflow.employeeId(), result.getInt("maNhanVien"));
                assertEquals("DOCTOR", result.getString("maVaiTroCode"));
                assertEquals(fixture.facilityId, result.getInt("maCoSo"));
                assertEquals(fixture.departmentId, result.getInt("maKhoa"));
                assertEquals("SUCCESS", result.getString("ketQua"));
                assertNull(result.getString("chiTiet"), "Audit must not contain clinical narrative");
                assertFalse(result.next(), "Each test correlation ID should identify one audit event");
            }
        }
    }

    private static void assertReadAudit(UUID correlationId, Fixture fixture, Workflow workflow)
            throws SQLException {
        try (Connection connection = CoSoDuLieuKetNoi.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT hanhDong, loaiDoiTuong, maDoiTuong, maNhanVien, maVaiTro, maVaiTroCode, "
                             + "maCoSo, maKhoa, ketQua, workstationId, occurredAtUtc, chiTiet "
                             + "FROM NhatKyHeThong WHERE correlationId=?")) {
            statement.setString(1, correlationId.toString());
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next(), "A successful read must commit its audit before returning");
                assertEquals("PATIENT_VIEW", result.getString("hanhDong"));
                assertEquals("PATIENT", result.getString("loaiDoiTuong"));
                assertEquals(Integer.toString(fixture.patientId), result.getString("maDoiTuong"));
                assertEquals(workflow.employeeId(), result.getInt("maNhanVien"));
                assertEquals(fixture.roleId, result.getInt("maVaiTro"));
                assertEquals("DOCTOR", result.getString("maVaiTroCode"));
                assertEquals(fixture.facilityId, result.getInt("maCoSo"));
                assertEquals(fixture.departmentId, result.getInt("maKhoa"));
                assertEquals("SUCCESS", result.getString("ketQua"));
                assertEquals("synthetic-history-workstation", result.getString("workstationId"));
                Timestamp occurredAt = result.getTimestamp("occurredAtUtc");
                assertNotNull(occurredAt, "The read audit should have a UTC timestamp");
                assertNull(result.getString("chiTiet"), "Read audit must not contain clinical narrative");
                assertFalse(result.next(), "Each test correlation ID should identify one audit event");
            }
        }
        assertEquals(
                1,
                queryCountByString(
                        "SELECT COUNT(*) FROM NhatKyHeThong WHERE correlationId=? AND occurredAtUtc "
                                + "BETWEEN DATEADD(MINUTE, -2, SYSUTCDATETIME()) "
                                + "AND DATEADD(MINUTE, 2, SYSUTCDATETIME())",
                        correlationId.toString()),
                "Read audit time must be UTC");
    }

    private static void assertHistoryUnchangedAfterRejectedAudit(
            int historyId, UUID rejectedCorrelation) throws SQLException {
        try (Connection connection = CoSoDuLieuKetNoi.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT moTa, version FROM TienSuBenh WHERE id=?")) {
            statement.setInt(1, historyId);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next(), "The previously committed row should remain");
                assertEquals("Synthetic updated allergy history", result.getString("moTa"));
                assertEquals(1L, result.getLong("version"));
            }
        }
        assertEquals(
                0,
                queryCountByString("SELECT COUNT(*) FROM NhatKyHeThong WHERE correlationId=?",
                        rejectedCorrelation.toString()),
                "The rejected audit event must not persist");
    }

    private static int insertHistory(
            int patientId, int facilityId, int departmentId, String category, String narrative, int createdBy)
            throws SQLException {
        String sql = "SET NOCOUNT ON; DECLARE @GeneratedKeys TABLE (id INT); "
                + "INSERT INTO TienSuBenh (maBenhNhan, loaiTienSu, moTa, maCoSo, maKhoa, createdAt, createdBy) "
                + "OUTPUT INSERTED.id INTO @GeneratedKeys (id) "
                + "VALUES (?, ?, ?, ?, ?, SYSUTCDATETIME(), ?); SELECT id FROM @GeneratedKeys;";
        try (Connection connection = CoSoDuLieuKetNoi.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            statement.setString(2, category);
            statement.setString(3, narrative);
            statement.setInt(4, facilityId);
            statement.setInt(5, departmentId);
            statement.setInt(6, createdBy);
            boolean hasResult = statement.execute();
            while (!hasResult && statement.getUpdateCount() != -1) {
                hasResult = statement.getMoreResults();
            }
            if (!hasResult) throw new SQLException("Synthetic history insert returned no identity result.");
            try (ResultSet keys = statement.getResultSet()) {
                if (!keys.next()) throw new SQLException("Synthetic history insert returned no identity.");
                return keys.getInt(1);
            }
        }
    }

    private static int insertPatient(String patientCode) throws SQLException {
        return insertIdentity(
                "INSERT INTO BenhNhan (maBenhNhanFormat, ho, ten, ngaySinh) "
                        + "OUTPUT INSERTED.id VALUES (?, N'Synthetic', N'HistoryAcceptance', '2001-02-03')",
                patientCode);
    }

    private static int insertIdentity(String sql, Object... values) throws SQLException {
        try (Connection connection = CoSoDuLieuKetNoi.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                if (values[index] instanceof Integer integer) statement.setInt(index + 1, integer);
                else statement.setString(index + 1, values[index].toString());
            }
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Synthetic fixture insert returned no identity.");
                return result.getInt(1);
            }
        }
    }

    private static void installRejectingTrigger(String triggerName, UUID correlationId) throws SQLException {
        String sql = "CREATE TRIGGER dbo.[" + triggerName + "] ON dbo.NhatKyHeThong AFTER INSERT AS "
                + "BEGIN SET NOCOUNT ON; IF EXISTS (SELECT 1 FROM inserted WHERE correlationId=N'"
                + correlationId + "') THROW 51020, 'Synthetic patient-history audit rejection.', 1; END";
        try (Connection connection = CoSoDuLieuKetNoi.getConnection();
             Statement statement = connection.createStatement()) {
            if (queryCount(connection, "SELECT COUNT(*) FROM sys.triggers WHERE name=N'"
                    + triggerName + "'") != 0) {
                throw new AssertionError("The uniquely named patient-history trigger already exists.");
            }
            statement.execute(sql);
        }
    }

    private static void cleanup(Fixture fixture, String triggerName, boolean triggerInstalled) {
        SQLException cleanupFailure = null;
        if (triggerInstalled) {
            try (Connection connection = CoSoDuLieuKetNoi.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP TRIGGER dbo.[" + triggerName + "]");
            } catch (SQLException exception) {
                cleanupFailure = exception;
            }
        }

        try (Connection connection = CoSoDuLieuKetNoi.getConnection()) {
            for (int historyId : fixture.historyIds) {
                executeUpdate(connection, "DELETE FROM TienSuBenh WHERE id=?", historyId);
            }
            for (int visitId : fixture.visitIds) {
                executeUpdate(connection, "DELETE FROM LuotKham WHERE id=?", visitId);
            }
            for (int patientId : fixture.patientIds) {
                executeUpdate(connection, "DELETE FROM BenhNhan WHERE id=?", patientId);
            }
            if (fixture.otherFacilityDepartmentId > 0) {
                executeUpdate(connection, "DELETE FROM Khoa WHERE id=?", fixture.otherFacilityDepartmentId);
            }
            if (fixture.otherDepartmentId > 0) {
                executeUpdate(connection, "DELETE FROM Khoa WHERE id=?", fixture.otherDepartmentId);
            }
            if (fixture.otherFacilityId > 0) {
                executeUpdate(connection, "DELETE FROM CoSo WHERE id=?", fixture.otherFacilityId);
            }
        } catch (SQLException exception) {
            if (cleanupFailure == null) cleanupFailure = exception;
            else cleanupFailure.addSuppressed(exception);
        }
        if (cleanupFailure != null) {
            throw new AssertionError(
                    "Could not remove the test's synthetic patient-history rows or temporary trigger; "
                            + "database details are suppressed.");
        }
    }

    private static void executeUpdate(String sql, int id) throws SQLException {
        try (Connection connection = CoSoDuLieuKetNoi.getConnection()) {
            executeUpdate(connection, sql, id);
        }
    }

    private static void executeUpdate(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private static void assertV10Ready(Connection connection) throws SQLException {
        if (queryInt(connection,
                "SELECT CASE WHEN COL_LENGTH('dbo.TienSuBenh','maCoSo') IS NOT NULL "
                        + "AND COL_LENGTH('dbo.TienSuBenh','maKhoa') IS NOT NULL "
                        + "AND OBJECT_ID(N'dbo.TR_TienSuBenh_Require_Scope_On_Insert', N'TR') IS NOT NULL "
                        + "THEN 1 ELSE 0 END") != 1) {
            throw new AssertionError(
                    "Apply the existing migrations through V10 to the explicitly selected clinic_test_* database "
                            + "before running this acceptance test."
            );
        }
    }

    private static void configureSyntheticDatabase(String databaseName) {
        Properties properties = loadLocalProperties();
        String baseUrl = requiredProperty(properties, "db.url").trim();
        String username = requiredProperty(properties, "db.username").trim();
        String password = requiredProperty(properties, "db." + "password");
        if (!baseUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:sqlserver:")
                || EMBEDDED_CREDENTIAL_PATTERN.matcher(baseUrl).find()) {
            throw new IllegalArgumentException(
                    "The ignored local db.properties must contain a SQL Server URL without embedded credentials.");
        }
        String serverUrl = DATABASE_OPTION_PATTERN.matcher(baseUrl).replaceAll("").replaceFirst(";+$", "");
        CoSoDuLieuKetNoi.setConnectionString("");
        CoSoDuLieuKetNoi.configure(serverUrl + ";databaseName=" + databaseName, username, password);
    }

    private static Properties loadLocalProperties() {
        Properties properties = new Properties();
        try (InputStream classpathInput = BenhNhanTienSuSQLMayChuLuuTruTest.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (classpathInput != null) {
                properties.load(classpathInput);
                return properties;
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not load the ignored local SQL Server configuration.");
        }
        for (Path candidate : List.of(
                Path.of("src", "main", "resources", "db.properties"),
                Path.of("clinic-infrastructure", "src", "main", "resources", "db.properties"))) {
            if (!Files.isRegularFile(candidate)) continue;
            try (InputStream fileInput = Files.newInputStream(candidate)) {
                properties.load(fileInput);
                return properties;
            } catch (IOException exception) {
                throw new AssertionError("Could not load the ignored local SQL Server configuration.");
            }
        }
        throw new AssertionError(
                "Provide the ignored local db.properties for SQL Server and keep its credentials out of output.");
    }

    private static String requiredProperty(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new AssertionError("The ignored local SQL Server configuration is incomplete.");
        }
        return value;
    }

    private static String resolveDatabaseName() {
        for (String key : List.of(
                "clinic.sqlserver.test.database",
                "clinic.test.sqlserver.databaseName",
                "clinic.sqlserver.audit.database")) {
            String value = System.getProperty(key);
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private static String queryString(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) throw new SQLException("Database identity query returned no result.");
            return result.getString(1);
        }
    }

    private static int queryInt(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) throw new SQLException("SQL Server assertion query returned no result.");
            return result.getInt(1);
        }
    }

    private static int queryCount(Connection connection, String sql) throws SQLException {
        return queryInt(connection, sql);
    }

    private static int queryCountByString(String sql, String value) throws SQLException {
        try (Connection connection = CoSoDuLieuKetNoi.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Audit count query returned no result.");
                return result.getInt(1);
            }
        }
    }

    private static final class Fixture {
        private int facilityId;
        private int departmentId;
        private int roleId;
        private int employeeId;
        private int otherDepartmentId;
        private int otherFacilityId;
        private int otherFacilityDepartmentId;
        private int patientId;
        private int otherPatientId;
        private final List<Integer> historyIds = new ArrayList<>();
        private final List<Integer> patientIds = new ArrayList<>();
        private final List<Integer> visitIds = new ArrayList<>();
    }

    private record Workflow(
            Fixture fixture,
            PhanQuyenNguCanh authorizationContext,
            SaveBenhNhanTienSuHandler saveHandler,
            GetBenhNhanTienSuHandler getHandler) {
        int employeeId() {
            return fixture.employeeId;
        }

        KiemToanTacNhanNguCanh actor(UUID correlationId) {
            return new KiemToanTacNhanNguCanh(
                    fixture.employeeId, fixture.roleId, "DOCTOR", fixture.facilityId,
                    fixture.departmentId, "synthetic-history-workstation", correlationId);
        }
    }
}
