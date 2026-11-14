package vn.clinicmanagement.infrastructure.persistence.reception;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TiepDonHangDoiRepositoryTest {
    @Test
    void queueSqlFiltersExactFacilityAndDepartment() {
        String sql = TiepDonHangDoiRepository.scopedQueueSql();

        assertTrue(sql.contains("l.maCoSo=?"));
        assertTrue(sql.contains("l.maKhoa=?"));
        assertTrue(sql.contains("l.isDeleted=0"));
        assertTrue(sql.contains("p.isDeleted=0"));
        assertTrue(sql.contains("l.trangThai<>'DA_HUY'"));
    }

    @Test
    void doctorQueueSqlFiltersDateExactScopeActiveRowsAndOnlyApprovedStatuses() {
        String sql = TiepDonHangDoiRepository.activeDoctorQueueSql();

        assertTrue(sql.contains("l.ngayKham=?"));
        assertTrue(sql.contains("l.maCoSo=?"));
        assertTrue(sql.contains("l.maKhoa=?"));
        assertTrue(sql.contains("l.isDeleted=0"));
        assertTrue(sql.contains("p.isDeleted=0"));
        assertTrue(sql.contains("l.trangThai IN ('CHO_KHAM','DANG_KHAM','CHO_CLS')"));
        assertFalse(sql.contains("DA_HUY"));
        assertFalse(sql.contains("DA_KHAM"));
        assertFalse(sql.contains("KET_LUAN"));
        assertFalse(sql.contains("HOAN_THANH"));
        assertFalse(sql.contains("maBacSi"));
    }

    @Test
    void doctorQueueBindsRequestedDateAndExactTeamScopeAndMapsSyntheticRow() throws Exception {
        SyntheticDoctorQueueJdbc jdbc = new SyntheticDoctorQueueJdbc();
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, jdbc.connection);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        TiepDonHangDoiRepository repository = new TiepDonHangDoiRepository(connections, unitOfWork);
        LocalDate date = LocalDate.of(2026, 11, 6);

        List<TiepDonHangDoiMatHang> items = withSyntheticStandaloneConnection(
                jdbc.connection, () -> repository.getActiveDoctorQueue(date, 3, 8));

        assertEquals(1, items.size());
        var item = items.get(0);
        assertEquals(51, item.visitId());
        assertEquals(72, item.patientId());
        assertEquals("BN-SYN-72", item.patientCode());
        assertEquals("Người bệnh tổng hợp", item.patientName());
        assertEquals(91, item.roomId());
        assertEquals("Phòng tổng hợp", item.roomName());
        assertEquals(5, item.queueNumber());
        assertEquals("05", item.queueNumberDisplay());
        assertEquals("CHO_CLS", item.status());
        assertEquals(LocalDateTime.of(2026, 11, 6, 8, 30), item.registeredAt());
        assertEquals(TiepDonHangDoiRepository.activeDoctorQueueSql(), jdbc.statement.sql);
        assertEquals(date, jdbc.statement.bindings.get(1));
        assertEquals(3, jdbc.statement.bindings.get(2));
        assertEquals(8, jdbc.statement.bindings.get(3));
    }

    @Test
    void roomSummarySqlFiltersRoomsAndJoinedVisitsByBothScopeKeys() {
        String sql = TiepDonHangDoiRepository.scopedRoomSummariesSql();

        assertTrue(sql.contains("l.maCoSo=?"));
        assertTrue(sql.contains("l.maKhoa=?"));
        assertTrue(sql.contains("k.maCoSo=?"));
        assertTrue(sql.contains("k.id=?"));
        assertTrue(sql.contains("l.isDeleted=0"));
        assertTrue(sql.contains("k.isDeleted=0"));
        assertTrue(sql.contains("p.isDeleted=0"));
    }

    @Test
    void transactionalRegisterVisitRecordsOneUnitOfWorkChangeAndMapsSyntheticVisit() {
        SyntheticReceptionJdbc jdbc = new SyntheticReceptionJdbc(4, 208);
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, jdbc.connection);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        TiepDonHangDoiRepository repository = new TiepDonHangDoiRepository(connections, unitOfWork);

        KetQua<LuotKham> result = transactions.inTransaction(() -> {
            assertTrue(connections.isTransactionActive());
            LuotKham registered = repository.registerVisit(
                    37, 2, 12, 104, " TAI_KHAM ", " Kiem tra dinh ky ", DoiTuongKham.THU_PHI, false, 15);
            assertEquals(0, jdbc.closes);
            assertEquals(1, unitOfWork.saveChanges());
            return KetQua.success(registered);
        });

        assertTrue(result.isSuccess());
        LuotKham visit = result.getValue().orElseThrow();
        assertSyntheticVisit(visit, 208, 37, 2, 12, 104, 4, "04",
                "TAI_KHAM", "Kiem tra dinh ky", DoiTuongKham.THU_PHI, false);
        assertRecordedVisitSql(jdbc, 37, 2, 12, 104, 4, "04",
                "TAI_KHAM", "Kiem tra dinh ky", DoiTuongKham.THU_PHI, false, 15);
        assertEquals(List.of(false), jdbc.autoCommitSettings);
        assertEquals(1, jdbc.commits);
        assertEquals(0, jdbc.rollbacks);
        assertEquals(1, jdbc.closes);
        assertFalse(connections.isTransactionActive());
        assertEquals(0, unitOfWork.saveChanges());
    }

    @Test
    void standaloneRegisterVisitRecordsZeroUnitOfWorkChangesPreservesAutocommitLeaseAndMapsSyntheticVisit()
            throws Exception {
        SyntheticReceptionJdbc standaloneJdbc = new SyntheticReceptionJdbc(7, 302);
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(
                unitOfWork, new SyntheticReceptionJdbc(99, 999).connection);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        TiepDonHangDoiRepository repository = new TiepDonHangDoiRepository(connections, unitOfWork);

        LuotKham visit = withSyntheticStandaloneConnection(standaloneJdbc.connection, () -> {
            assertFalse(connections.isTransactionActive());
            return repository.registerVisit(
                    41, 3, 18, 205, " KHAM_MOI ", " Kham tong quat ", DoiTuongKham.BHYT, true, 19);
        });

        assertEquals(0, unitOfWork.saveChanges());
        assertFalse(unitOfWork.saveEntities());
        assertSyntheticVisit(visit, 302, 41, 3, 18, 205, 7, "07",
                "KHAM_MOI", "Kham tong quat", DoiTuongKham.BHYT, true);
        assertRecordedVisitSql(standaloneJdbc, 41, 3, 18, 205, 7, "07",
                "KHAM_MOI", "Kham tong quat", DoiTuongKham.BHYT, true, 19);
        assertTrue(standaloneJdbc.autoCommitSettings.isEmpty());
        assertEquals(0, standaloneJdbc.commits);
        assertEquals(0, standaloneJdbc.rollbacks);
        assertEquals(1, standaloneJdbc.closes);
    }

    @Test
    void defaultConstructorUsesExplicitClinicZoneClock() {
        SyntheticReceptionJdbc jdbc = new SyntheticReceptionJdbc(1, 100);
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, jdbc.connection);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        TiepDonHangDoiRepository repository = new TiepDonHangDoiRepository(connections, unitOfWork);

        assertEquals(TiepDonHangDoiRepository.CLINIC_ZONE, repository.clock().getZone());
    }

    @Test
    void registerVisitUsesCapturedVietnamBusinessTimeDespiteNonVietnamDefaultZone() throws Exception {
        TimeZone originalZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            Instant fixedInstant = Instant.parse("2026-11-06T18:30:00Z");
            LocalDate expectedVietnamDate = LocalDate.of(2026, 11, 7);
            LocalDateTime expectedVietnamDateTime = LocalDateTime.of(2026, 11, 7, 1, 30, 0);
            LocalDate utcDate = LocalDate.of(2026, 11, 6);

            assertFalse(expectedVietnamDate.equals(utcDate));

            SyntheticReceptionJdbc jdbc = new SyntheticReceptionJdbc(3, 501);
            JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
            JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, jdbc.connection);
            JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
            Clock utcZonedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
            TiepDonHangDoiRepository repository =
                    new TiepDonHangDoiRepository(connections, unitOfWork, utcZonedClock);

            assertEquals(TiepDonHangDoiRepository.CLINIC_ZONE, repository.clock().getZone());

            LuotKham visit = withSyntheticStandaloneConnection(
                    jdbc.connection,
                    () -> repository.registerVisit(
                            88, 1, 5, 20, "KHAM_MOI", "Kham tong quat", DoiTuongKham.THU_PHI, false, 9));

            assertEquals(expectedVietnamDate, visit.getNgayKham());
            assertEquals(expectedVietnamDateTime, visit.getThoiGianTiepNhan());
            assertEquals(visit.getNgayKham(), visit.getThoiGianTiepNhan().toLocalDate());

            assertEquals(2, jdbc.statements.size());
            RecordedStatement queueSelect = jdbc.statements.get(0);
            assertTrue(queueSelect.sql.startsWith("SELECT ISNULL(MAX(soThuTu), 0) + 1 FROM LuotKham"));
            assertEquals(20, queueSelect.bindings.get(1));
            assertEquals(expectedVietnamDate, queueSelect.bindings.get(2));

            RecordedStatement visitInsert = jdbc.statements.get(1);
            assertTrue(visitInsert.sql.startsWith("INSERT INTO LuotKham"));
            assertEquals(88, visitInsert.bindings.get(1));
            assertEquals(1, visitInsert.bindings.get(2));
            assertEquals(5, visitInsert.bindings.get(3));
            assertEquals(20, visitInsert.bindings.get(4));
            assertEquals(TrangThaiLuotKham.CHO_KHAM.name(), visitInsert.bindings.get(5));
            assertEquals(3, visitInsert.bindings.get(6));
            assertEquals("03", visitInsert.bindings.get(7));
            assertEquals(expectedVietnamDate, visitInsert.bindings.get(8));
            assertTrue(visitInsert.bindings.get(9) instanceof Timestamp);
            Timestamp boundTimestamp = (Timestamp) visitInsert.bindings.get(9);
            assertEquals(Timestamp.valueOf(expectedVietnamDateTime), boundTimestamp);
            assertEquals(expectedVietnamDateTime, boundTimestamp.toLocalDateTime());
            assertEquals(expectedVietnamDate, boundTimestamp.toLocalDateTime().toLocalDate());

            assertEquals(visit.getNgayKham(), queueSelect.bindings.get(2));
            assertEquals(visit.getNgayKham(), visitInsert.bindings.get(8));
        } finally {
            TimeZone.setDefault(originalZone);
        }
    }

    @Test
    void registerVisitInTransactionUsesCapturedVietnamBusinessTimeDespiteNonVietnamDefaultZone() {
        TimeZone originalZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            Instant fixedInstant = Instant.parse("2026-11-06T18:30:00Z");
            LocalDate expectedVietnamDate = LocalDate.of(2026, 11, 7);
            LocalDateTime expectedVietnamDateTime = LocalDateTime.of(2026, 11, 7, 1, 30, 0);

            SyntheticReceptionJdbc jdbc = new SyntheticReceptionJdbc(4, 502);
            JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
            JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, jdbc.connection);
            JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
            Clock utcZonedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
            TiepDonHangDoiRepository repository =
                    new TiepDonHangDoiRepository(connections, unitOfWork, utcZonedClock);

            KetQua<LuotKham> result = transactions.inTransaction(() ->
                    KetQua.success(repository.registerVisit(
                            88, 1, 5, 20, "KHAM_MOI", "Kham tong quat", DoiTuongKham.THU_PHI, false, 9)));

            assertTrue(result.isSuccess());
            LuotKham visit = result.getValue().orElseThrow();
            assertEquals(expectedVietnamDate, visit.getNgayKham());
            assertEquals(expectedVietnamDateTime, visit.getThoiGianTiepNhan());
            assertEquals(2, jdbc.statements.size());
            RecordedStatement queueSelect = jdbc.statements.get(0);
            assertEquals(expectedVietnamDate, queueSelect.bindings.get(2));

            RecordedStatement visitInsert = jdbc.statements.get(1);
            assertEquals(expectedVietnamDate, visitInsert.bindings.get(8));
            Timestamp boundTimestamp = (Timestamp) visitInsert.bindings.get(9);
            assertEquals(Timestamp.valueOf(expectedVietnamDateTime), boundTimestamp);
        } finally {
            TimeZone.setDefault(originalZone);
        }
    }

    private static void assertSyntheticVisit(
            LuotKham visit,
            int expectedId,
            int expectedPatientId,
            int expectedFacilityId,
            int expectedDepartmentId,
            int expectedRoomId,
            int expectedQueueNumber,
            String expectedQueueDisplay,
            String expectedRegistrationType,
            String expectedReason,
            DoiTuongKham expectedPayerType,
            boolean expectedPriority) {
        assertNotNull(visit);
        assertEquals(expectedId, visit.getId());
        assertEquals(expectedPatientId, visit.getMaBenhNhan());
        assertEquals(expectedFacilityId, visit.getMaCoSo());
        assertEquals(expectedDepartmentId, visit.getMaKhoa());
        assertEquals(expectedRoomId, visit.getMaPhongKham());
        assertEquals(TrangThaiLuotKham.CHO_KHAM, visit.getTrangThai());
        assertEquals(expectedQueueNumber, visit.getSoThuTu());
        assertEquals(expectedQueueDisplay, visit.getSoThuTuDisplay());
        assertNotNull(visit.getNgayKham());
        assertNotNull(visit.getThoiGianTiepNhan());
        assertEquals(expectedReason, visit.getLyDoKham());
        assertEquals(expectedPayerType, visit.getDoiTuongKham());
        assertEquals(expectedRegistrationType, visit.getLoaiDangKyKham());
        assertEquals(expectedPriority, visit.isDnVnTien());
    }

    private static void assertRecordedVisitSql(
            SyntheticReceptionJdbc jdbc,
            int expectedPatientId,
            int expectedFacilityId,
            int expectedDepartmentId,
            int expectedRoomId,
            int expectedQueueNumber,
            String expectedQueueDisplay,
            String expectedRegistrationType,
            String expectedReason,
            DoiTuongKham expectedPayerType,
            boolean expectedPriority,
            int expectedCreatedBy) {
        assertEquals(2, jdbc.statements.size());

        RecordedStatement queueSelect = jdbc.statements.get(0);
        assertTrue(queueSelect.sql.startsWith("SELECT ISNULL(MAX(soThuTu), 0) + 1 FROM LuotKham"));
        assertEquals(expectedRoomId, queueSelect.bindings.get(1));
        assertNotNull(queueSelect.bindings.get(2));
        assertTrue(queueSelect.closed);
        assertTrue(queueSelect.resultSetClosed);

        RecordedStatement visitInsert = jdbc.statements.get(1);
        assertTrue(visitInsert.sql.startsWith("INSERT INTO LuotKham"));
        assertTrue(visitInsert.sql.contains("OUTPUT INSERTED.id"));
        assertEquals(expectedPatientId, visitInsert.bindings.get(1));
        assertEquals(expectedFacilityId, visitInsert.bindings.get(2));
        assertEquals(expectedDepartmentId, visitInsert.bindings.get(3));
        assertEquals(expectedRoomId, visitInsert.bindings.get(4));
        assertEquals(TrangThaiLuotKham.CHO_KHAM.name(), visitInsert.bindings.get(5));
        assertEquals(expectedQueueNumber, visitInsert.bindings.get(6));
        assertEquals(expectedQueueDisplay, visitInsert.bindings.get(7));
        assertNotNull(visitInsert.bindings.get(8));
        assertTrue(visitInsert.bindings.get(9) instanceof Timestamp);
        assertEquals(expectedReason, visitInsert.bindings.get(10));
        assertEquals(expectedPayerType.name(), visitInsert.bindings.get(11));
        assertEquals(expectedRegistrationType, visitInsert.bindings.get(12));
        assertEquals(expectedPriority, visitInsert.bindings.get(13));
        assertEquals(expectedCreatedBy, visitInsert.bindings.get(14));
        assertEquals(Types.INTEGER, visitInsert.sqlTypes.get(14));
        assertTrue(visitInsert.closed);
        assertTrue(visitInsert.resultSetClosed);
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static <T> T withSyntheticStandaloneConnection(
            Connection connection, ThrowingSupplier<T> action) throws Exception {
        String syntheticUrl = "jdbc:synthetic:reception-queue-test";
        Driver driver = new SyntheticDriver(syntheticUrl, connection);
        Field customConnectionField = CoSoDuLieuKetNoi.class.getDeclaredField("CUSTOM_CONNECTION_STRING");
        customConnectionField.setAccessible(true);
        String previousConnectionString = (String) customConnectionField.get(null);
        DriverManager.registerDriver(driver);
        try {
            CoSoDuLieuKetNoi.setConnectionString(syntheticUrl);
            return action.get();
        } finally {
            customConnectionField.set(null, previousConnectionString);
            DriverManager.deregisterDriver(driver);
        }
    }

    private static JdbcGiaoDichQuanLy transactionManager(
            JdbcUnitOfWork unitOfWork, Connection connection) {
        try {
            Class<?> supplierType = Class.forName(
                    "vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager$ConnectionSupplier");
            Object supplier = Proxy.newProxyInstance(
                    supplierType.getClassLoader(),
                    new Class<?>[]{supplierType},
                    (proxy, method, args) -> {
                        if ("get".equals(method.getName())) return connection;
                        throw new UnsupportedOperationException(method.getName());
                    });
            Constructor<JdbcGiaoDichQuanLy> constructor = JdbcGiaoDichQuanLy.class
                    .getDeclaredConstructor(JdbcUnitOfWork.class, supplierType);
            constructor.setAccessible(true);
            return constructor.newInstance(unitOfWork, supplier);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Could not configure synthetic JDBC transaction.", ex);
        }
    }

    private static final class SyntheticDriver implements Driver {
        private final String supportedUrl;
        private final Connection connection;

        private SyntheticDriver(String supportedUrl, Connection connection) {
            this.supportedUrl = supportedUrl;
            this.connection = connection;
        }

        @Override
        public Connection connect(String url, Properties info) {
            return acceptsURL(url) ? connection : null;
        }

        @Override
        public boolean acceptsURL(String url) {
            return supportedUrl.equals(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }
    }

    private static final class SyntheticReceptionJdbc {
        private final int nextQueueNumber;
        private final int insertedVisitId;
        private final Connection connection;
        private final List<RecordedStatement> statements = new ArrayList<>();
        private final List<Boolean> autoCommitSettings = new ArrayList<>();
        private int commits;
        private int rollbacks;
        private int closes;

        private SyntheticReceptionJdbc(int nextQueueNumber, int insertedVisitId) {
            this.nextQueueNumber = nextQueueNumber;
            this.insertedVisitId = insertedVisitId;
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "prepareStatement" -> {
                            String sql = (String) args[0];
                            int scalarResult = sql.startsWith("SELECT ") ? this.nextQueueNumber : this.insertedVisitId;
                            RecordedStatement statement = new RecordedStatement(sql, scalarResult);
                            statements.add(statement);
                            yield statement.preparedStatement;
                        }
                        case "setAutoCommit" -> {
                            autoCommitSettings.add((Boolean) args[0]);
                            yield null;
                        }
                        case "commit" -> {
                            commits++;
                            yield null;
                        }
                        case "rollback" -> {
                            rollbacks++;
                            yield null;
                        }
                        case "close" -> {
                            closes++;
                            yield null;
                        }
                        case "isClosed" -> closes > 0;
                        case "toString" -> "SyntheticReceptionConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    private static final class SyntheticDoctorQueueJdbc {
        private final RecordedDoctorQueueStatement statement =
                new RecordedDoctorQueueStatement(TiepDonHangDoiRepository.activeDoctorQueueSql());
        private final Connection connection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "prepareStatement" -> {
                        assertEquals(TiepDonHangDoiRepository.activeDoctorQueueSql(), args[0]);
                        yield statement.preparedStatement;
                    }
                    case "close" -> null;
                    case "isClosed" -> false;
                    case "toString" -> "SyntheticDoctorQueueConnection";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class RecordedDoctorQueueStatement {
        private final String sql;
        private final Map<Integer, Object> bindings = new LinkedHashMap<>();
        private final PreparedStatement preparedStatement;

        private RecordedDoctorQueueStatement(String sql) {
            this.sql = sql;
            preparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setInt", "setObject" -> {
                            bindings.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "executeQuery" -> doctorQueueResultSet();
                        case "close" -> null;
                        case "toString" -> "SyntheticDoctorQueuePreparedStatement";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private ResultSet doctorQueueResultSet() {
            boolean[] returned = {false};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> {
                            if (returned[0]) yield false;
                            returned[0] = true;
                            yield true;
                        }
                        case "getInt" -> switch ((String) args[0]) {
                            case "visitId" -> 51;
                            case "patientId" -> 72;
                            case "roomId" -> 91;
                            case "queueNumber" -> 5;
                            default -> throw new SQLException("Unexpected synthetic integer column.");
                        };
                        case "getString" -> switch ((String) args[0]) {
                            case "patientCode" -> "BN-SYN-72";
                            case "patientName" -> "Người bệnh tổng hợp";
                            case "roomName" -> "Phòng tổng hợp";
                            case "queueDisplay" -> "05";
                            case "trangThai" -> "CHO_CLS";
                            default -> throw new SQLException("Unexpected synthetic string column.");
                        };
                        case "getTimestamp" -> "thoiGianTiepNhan".equals(args[0])
                                ? Timestamp.valueOf(LocalDateTime.of(2026, 11, 6, 8, 30))
                                : throwUnexpectedColumn();
                        case "close" -> null;
                        case "toString" -> "SyntheticDoctorQueueResultSet";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private static Timestamp throwUnexpectedColumn() throws SQLException {
            throw new SQLException("Unexpected synthetic timestamp column.");
        }
    }

    private static final class RecordedStatement {
        private final String sql;
        private final int scalarResult;
        private final Map<Integer, Object> bindings = new LinkedHashMap<>();
        private final Map<Integer, Integer> sqlTypes = new LinkedHashMap<>();
        private final PreparedStatement preparedStatement;
        private boolean closed;
        private boolean resultSetClosed;

        private RecordedStatement(String sql, int scalarResult) {
            this.sql = sql;
            this.scalarResult = scalarResult;
            preparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setInt", "setString", "setBoolean", "setTimestamp" -> {
                            bindings.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "setObject" -> {
                            int index = (Integer) args[0];
                            bindings.put(index, args[1]);
                            if (args.length >= 3 && args[2] instanceof Integer sqlType) {
                                sqlTypes.put(index, sqlType);
                            }
                            yield null;
                        }
                        case "executeQuery" -> singleIntResultSet();
                        case "close" -> {
                            closed = true;
                            yield null;
                        }
                        case "toString" -> "SyntheticReceptionPreparedStatement";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private ResultSet singleIntResultSet() {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> ++cursor[0] == 0;
                        case "getInt" -> {
                            if (cursor[0] != 0 || ((Integer) args[0]) != 1) {
                                throw new SQLException("Unexpected ResultSet access.");
                            }
                            yield scalarResult;
                        }
                        case "close" -> {
                            resultSetClosed = true;
                            yield null;
                        }
                        case "toString" -> "SyntheticReceptionResultSet";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
