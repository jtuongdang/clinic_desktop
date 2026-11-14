package vn.clinicmanagement.infrastructure.persistence.patient;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.BenhNhanDinhDanhXungDotNgoaiLe;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenhNhanRepositoryTest {
    private static final List<String> REGISTRATION_COLUMNS = List.of(
            "ho", "ten", "ngaySinh", "gioiTinh", "dienThoai", "email", "soCCCD", "diaChi",
            "nguoiLienHeTen", "nguoiLienHeDienThoai", "noiSinh", "soNhaThonPho", "ngayCapCCCD",
            "noiCapCCCD", "noiLamViec");

    @Test
    void selectMapsEveryApprovedPatientRegistrationField() {
        RegistrationJdbc jdbc = new RegistrationJdbc();
        jdbc.selectRow = registrationRow();
        Persistence persistence = persistence(jdbc);

        KetQua<BenhNhan> result = persistence.transactions.inTransaction(
                () -> KetQua.success(persistence.repository.getById(17).orElseThrow()));

        assertTrue(result.isSuccess());
        assertSelectedRegistrationColumns(jdbc.sql);
        assertRegistrationFields(result.getValue().orElseThrow());
    }

    @Test
    void insertAndUpdateBindEveryApprovedPatientRegistrationField() {
        RegistrationJdbc jdbc = new RegistrationJdbc();
        Persistence persistence = persistence(jdbc);
        BenhNhan patient = syntheticRegistrationPatient();

        KetQua<Integer> insertResult = persistence.transactions.inTransaction(() -> {
            persistence.repository.add(patient);
            return KetQua.success(patient.getId());
        });

        assertTrue(insertResult.isSuccess());
        assertEquals(77, insertResult.getValue().orElseThrow());
        assertSelectedRegistrationColumns(jdbc.sql);
        assertInsertRegistrationBindings(jdbc.parameters);

        patient.setId(17);
        patient.setVersion(3L);
        KetQua<Integer> updateResult = persistence.transactions.inTransaction(() -> {
            persistence.repository.update(patient);
            return KetQua.success(patient.getVersion().intValue());
        });

        assertTrue(updateResult.isSuccess());
        assertEquals(4, updateResult.getValue().orElseThrow());
        assertSelectedRegistrationColumns(jdbc.sql);
        assertUpdateRegistrationBindings(jdbc.parameters);
    }

    @Test
    void successfulDeleteAdvancesDatabaseAndEntityVersionsOnce() {
        SyntheticJdbc jdbc = new SyntheticJdbc(7);
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, jdbc.connection);
        BenhNhanRepository repository = new BenhNhanRepository(
                new JdbcKetNoiNhaCungCap(transactions), unitOfWork);
        BenhNhan patient = new BenhNhan();
        patient.setId(17);
        patient.setVersion(7L);
        patient.setDeletedBy(4);

        KetQua<Integer> result = transactions.inTransaction(() -> {
            repository.delete(patient);
            return KetQua.success(unitOfWork.saveChanges());
        });

        assertTrue(result.isSuccess());
        assertEquals(1, result.getValue().orElseThrow());
        assertEquals(8L, jdbc.databaseVersion);
        assertEquals(8L, patient.getVersion());
        assertTrue(jdbc.databaseDeleted);
        assertTrue(patient.isDeleted());
    }

    @Test
    void staleDeleteLeavesDatabaseAndEntityStateUnchanged() {
        SyntheticJdbc jdbc = new SyntheticJdbc(9);
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, jdbc.connection);
        BenhNhanRepository repository = new BenhNhanRepository(
                new JdbcKetNoiNhaCungCap(transactions), unitOfWork);
        BenhNhan patient = new BenhNhan();
        patient.setId(17);
        patient.setVersion(8L);

        KetQua<Integer> result = transactions.inTransaction(() -> {
            try {
                repository.delete(patient);
                throw new AssertionError("Expected stale-version conflict.");
            } catch (IllegalStateException conflict) {
                assertEquals("Hồ sơ đã thay đổi hoặc không còn tồn tại.", conflict.getMessage());
                return KetQua.success(unitOfWork.saveChanges());
            }
        });

        assertTrue(result.isSuccess());
        assertEquals(0, result.getValue().orElseThrow());
        assertEquals(9L, jdbc.databaseVersion);
        assertFalse(jdbc.databaseDeleted);
        assertEquals(8L, patient.getVersion());
        assertFalse(patient.isDeleted());
    }

    @Test
    void scopedCccdLookupRequiresBothFacilityAndDepartmentOnLinkedVisit() {
        String sql = BenhNhanRepository.scopedCccdSql();

        assertTrue(sql.contains("EXISTS (SELECT 1 FROM LuotKham lk"));
        assertTrue(sql.contains("lk.maBenhNhan=BenhNhan.id"));
        assertTrue(sql.contains("lk.maCoSo=?"));
        assertTrue(sql.contains("lk.maKhoa=?"));
        assertTrue(sql.contains("lk.isDeleted=0"));
        assertFalse(sql.contains("OR lk.maCoSo"));
    }

    @Test
    void scopedIdLookupRequiresBothFacilityAndDepartmentOnLinkedVisit() {
        String sql = BenhNhanRepository.scopedIdSql();

        assertTrue(sql.contains("WHERE id=? AND isDeleted=0 AND EXISTS"));
        assertTrue(sql.contains("lk.maBenhNhan=BenhNhan.id"));
        assertTrue(sql.contains("lk.maCoSo=?"));
        assertTrue(sql.contains("lk.maKhoa=?"));
        assertTrue(sql.contains("lk.isDeleted=0"));
    }

    @Test
    void scopedSearchRequiresBothFacilityAndDepartmentOnNondeletedVisitAndCapsResults() {
        String sql = BenhNhanRepository.scopedSearchSql(500);

        assertTrue(sql.contains("TOP (50)"));
        assertTrue(sql.contains("lk.maCoSo=?"));
        assertTrue(sql.contains("lk.maKhoa=?"));
        assertTrue(sql.contains("lk.isDeleted=0"));
        assertTrue(sql.contains("EXISTS (SELECT 1 FROM LuotKham lk"));
    }

    @Test
    void mapsOnlyTheActiveCccdIndexViolationWithoutLeakingTheDuplicateValue() {
        for (int errorCode : new int[]{2601, 2627}) {
            SQLException sqlException = new SQLException(
                    "Cannot insert duplicate key row with unique index 'UX_BenhNhan_Active_CCCD'. "
                            + "The duplicate key value is (CCCD:synthetic-cccd-value).",
                    "23000", errorCode);

            RuntimeException failure = BenhNhanRepository.persistenceFailure(sqlException);

            assertInstanceOf(BenhNhanDinhDanhXungDotNgoaiLe.class, failure);
            assertNull(failure.getCause());
            assertTrue(failure.getMessage().contains("Định danh"));
            assertFalse(failure.getMessage().contains("synthetic-cccd-value"));
        }
    }

    @Test
    void doesNotMapOtherSqlServerUniqueKeysToDuplicateCccd() {
        for (int errorCode : new int[]{2601, 2627}) {
            SQLException sqlException = new SQLException(
                    "Cannot insert duplicate key row with unique index 'UX_BenhNhan_MaBenhNhanFormat'. "
                            + "The duplicate key value is (synthetic-patient-code).",
                    "23000", errorCode);

            RuntimeException failure = BenhNhanRepository.persistenceFailure(sqlException);

            assertFalse(failure instanceof BenhNhanDinhDanhXungDotNgoaiLe);
            assertInstanceOf(IllegalStateException.class, failure);
            assertTrue(failure.getCause() == sqlException);
        }
    }

    @Test
    void doesNotMapDuplicateCodesWithoutTheCccdIndexName() {
        SQLException sqlException = new SQLException("Duplicate key: synthetic-value", "23000", 2601);

        RuntimeException failure = BenhNhanRepository.persistenceFailure(sqlException);

        assertFalse(failure instanceof BenhNhanDinhDanhXungDotNgoaiLe);
        assertInstanceOf(IllegalStateException.class, failure);
    }

    @Test
    void keepsNonDuplicateSqlErrorsAsPersistenceFailures() {
        SQLException sqlException = new SQLException("Synthetic database failure", "42000", 102);

        RuntimeException failure = BenhNhanRepository.persistenceFailure(sqlException);

        assertInstanceOf(IllegalStateException.class, failure);
        assertTrue(failure.getCause() == sqlException);
    }

    private static Persistence persistence(RegistrationJdbc jdbc) {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, jdbc.connection);
        BenhNhanRepository repository = new BenhNhanRepository(
                new JdbcKetNoiNhaCungCap(transactions), unitOfWork);
        return new Persistence(transactions, repository);
    }

    private static Map<String, Object> registrationRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 17);
        row.put("ho", "Synthetic Nguyen");
        row.put("ten", "An");
        row.put("ngaySinh", Date.valueOf("1992-04-15"));
        row.put("gioiTinh", "F");
        row.put("dienThoai", "synthetic-phone-01");
        row.put("email", "synthetic.person@example.invalid");
        row.put("soCCCD", "synthetic-cccd-01");
        row.put("diaChi", "Synthetic address");
        row.put("nguoiLienHeTen", "Synthetic contact");
        row.put("nguoiLienHeDienThoai", "synthetic-contact-phone-01");
        row.put("noiSinh", "Synthetic birthplace");
        row.put("soNhaThonPho", "Synthetic street");
        row.put("ngayCapCCCD", null);
        row.put("noiCapCCCD", "Synthetic issuing office");
        row.put("noiLamViec", "Synthetic workplace");
        row.put("version", 3L);
        row.put("createdAt", null);
        row.put("createdBy", null);
        row.put("updatedAt", null);
        row.put("updatedBy", null);
        row.put("isDeleted", false);
        row.put("deletedAt", null);
        row.put("deletedBy", null);
        return row;
    }

    private static BenhNhan syntheticRegistrationPatient() {
        BenhNhan patient = new BenhNhan();
        patient.setHo("Synthetic Nguyen");
        patient.setTen("An");
        patient.setNgaySinh(LocalDate.of(1992, 4, 15));
        patient.setGioiTinh(vn.clinicmanagement.domain.common.enums.GioiTinh.F);
        patient.setDienThoai(" synthetic-phone-01 ");
        patient.setEmail("synthetic.person@example.invalid");
        patient.setSoCCCD(" synthetic-cccd-01 ");
        patient.setDiaChi("Synthetic address");
        patient.setNguoiLienHeTen("Synthetic contact");
        patient.setNguoiLienHeDienThoai("synthetic-contact-phone-01");
        patient.setNoiSinh("Synthetic birthplace");
        patient.setSoNhaThonPho("Synthetic street");
        patient.setNgayCapCCCD(null);
        patient.setNoiCapCCCD("Synthetic issuing office");
        patient.setNoiLamViec("Synthetic workplace");
        return patient;
    }

    private static void assertSelectedRegistrationColumns(String sql) {
        assertTrue(sql.startsWith("SELECT") || sql.startsWith("INSERT") || sql.startsWith("UPDATE"));
        for (String column : REGISTRATION_COLUMNS) {
            assertTrue(sql.contains(column), "SQL should include patient registration column " + column);
        }
    }

    private static void assertRegistrationFields(BenhNhan patient) {
        assertEquals("Synthetic Nguyen", patient.getHo());
        assertEquals("An", patient.getTen());
        assertEquals(LocalDate.of(1992, 4, 15), patient.getNgaySinh());
        assertEquals(vn.clinicmanagement.domain.common.enums.GioiTinh.F, patient.getGioiTinh());
        assertEquals("synthetic-phone-01", patient.getDienThoai());
        assertEquals("synthetic.person@example.invalid", patient.getEmail());
        assertEquals("synthetic-cccd-01", patient.getSoCCCD());
        assertEquals("Synthetic address", patient.getDiaChi());
        assertEquals("Synthetic contact", patient.getNguoiLienHeTen());
        assertEquals("synthetic-contact-phone-01", patient.getNguoiLienHeDienThoai());
        assertEquals("Synthetic birthplace", patient.getNoiSinh());
        assertEquals("Synthetic street", patient.getSoNhaThonPho());
        assertNull(patient.getNgayCapCCCD());
        assertEquals("Synthetic issuing office", patient.getNoiCapCCCD());
        assertEquals("Synthetic workplace", patient.getNoiLamViec());
    }

    private static void assertInsertRegistrationBindings(Map<Integer, Object> parameters) {
        assertEquals("Synthetic Nguyen", parameters.get(2));
        assertEquals("An", parameters.get(3));
        assertEquals(LocalDate.of(1992, 4, 15), parameters.get(4));
        assertEquals("F", parameters.get(5));
        assertEquals("synthetic-phone-01", parameters.get(6));
        assertEquals("synthetic.person@example.invalid", parameters.get(7));
        assertEquals("synthetic-cccd-01", parameters.get(8));
        assertEquals("Synthetic address", parameters.get(9));
        assertEquals("Synthetic street", parameters.get(10));
        assertEquals("Synthetic birthplace", parameters.get(11));
        assertNull(parameters.get(12));
        assertEquals("Synthetic issuing office", parameters.get(13));
        assertEquals("Synthetic workplace", parameters.get(14));
        assertEquals("Synthetic contact", parameters.get(21));
        assertEquals("synthetic-contact-phone-01", parameters.get(22));
    }

    private static void assertUpdateRegistrationBindings(Map<Integer, Object> parameters) {
        assertEquals("Synthetic Nguyen", parameters.get(1));
        assertEquals("An", parameters.get(2));
        assertEquals(LocalDate.of(1992, 4, 15), parameters.get(3));
        assertEquals("F", parameters.get(4));
        assertEquals("synthetic-phone-01", parameters.get(5));
        assertEquals("synthetic.person@example.invalid", parameters.get(6));
        assertEquals("synthetic-cccd-01", parameters.get(7));
        assertEquals("Synthetic address", parameters.get(8));
        assertEquals("Synthetic street", parameters.get(9));
        assertEquals("Synthetic birthplace", parameters.get(10));
        assertNull(parameters.get(11));
        assertEquals("Synthetic issuing office", parameters.get(12));
        assertEquals("Synthetic workplace", parameters.get(13));
        assertEquals("Synthetic contact", parameters.get(20));
        assertEquals("synthetic-contact-phone-01", parameters.get(21));
    }

    private static JdbcGiaoDichQuanLy transactionManager(
            JdbcUnitOfWork unitOfWork, Connection connection) {
        try {
            Class<?> supplierType = Class.forName(
                    "vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager$ConnectionSupplier");
            Object supplier = Proxy.newProxyInstance(
                    supplierType.getClassLoader(),
                    new Class<?>[]{supplierType},
                    (proxy, method, args) -> connection);
            Constructor<JdbcGiaoDichQuanLy> constructor = JdbcGiaoDichQuanLy.class
                    .getDeclaredConstructor(JdbcUnitOfWork.class, supplierType);
            constructor.setAccessible(true);
            return constructor.newInstance(unitOfWork, supplier);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Could not configure synthetic JDBC transaction.", ex);
        }
    }

    private record Persistence(JdbcGiaoDichQuanLy transactions, BenhNhanRepository repository) {}

    private static final class RegistrationJdbc {
        private final Connection connection;
        private Map<String, Object> selectRow = Map.of();
        private Map<Integer, Object> parameters = new LinkedHashMap<>();
        private String sql = "";

        private RegistrationJdbc() {
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit", "commit", "rollback", "close" -> null;
                        case "prepareStatement" -> prepareStatement((String) args[0]);
                        case "isClosed" -> false;
                        case "toString" -> "SyntheticRegistrationConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private PreparedStatement prepareStatement(String statement) {
            sql = statement;
            parameters = new LinkedHashMap<>();
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setString", "setObject", "setInt", "setLong", "setDate", "setTimestamp",
                                "setBoolean" -> {
                            parameters.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "executeQuery" -> resultSet(statement.startsWith("SELECT") ? selectRow : Map.of());
                        case "executeUpdate" -> 1;
                        case "close" -> null;
                        case "toString" -> "SyntheticRegistrationPreparedStatement";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private ResultSet resultSet(Map<String, Object> row) {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> ++cursor[0] == 0;
                        case "getInt" -> {
                            Object value = value(row, args[0], 77);
                            yield value == null ? 0 : ((Number) value).intValue();
                        }
                        case "getLong" -> {
                            Object value = value(row, args[0], 0L);
                            yield value == null ? 0L : ((Number) value).longValue();
                        }
                        case "getString" -> {
                            Object value = value(row, args[0], null);
                            yield value == null ? null : value.toString();
                        }
                        case "getDate" -> value(row, args[0], null);
                        case "getTimestamp" -> value(row, args[0], null);
                        case "getObject" -> value(row, args[0], null);
                        case "getBoolean" -> Boolean.TRUE.equals(value(row, args[0], false));
                        case "close" -> null;
                        case "toString" -> "SyntheticRegistrationResultSet";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private Object value(Map<String, Object> row, Object column, Object scalar) {
            return column instanceof String name ? row.get(name) : scalar;
        }
    }

    private static final class SyntheticJdbc {
        private final Connection connection;
        private long databaseVersion;
        private boolean databaseDeleted;
        private boolean transactionActive;
        private final Map<Integer, Object> parameters = new LinkedHashMap<>();

        private SyntheticJdbc(long version) {
            databaseVersion = version;
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit" -> {
                            transactionActive = !(boolean) args[0];
                            yield null;
                        }
                        case "prepareStatement" -> preparedStatement();
                        case "commit", "rollback", "close" -> {
                            transactionActive = false;
                            yield null;
                        }
                        case "isClosed" -> false;
                        case "toString" -> "SyntheticConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private PreparedStatement preparedStatement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setObject", "setInt", "setLong" -> {
                            parameters.put((int) args[0], args[1]);
                            yield null;
                        }
                        case "executeUpdate" -> {
                            long expectedVersion = ((Number) parameters.get(3)).longValue();
                            if (expectedVersion == databaseVersion && !databaseDeleted) {
                                databaseVersion++;
                                databaseDeleted = true;
                                yield 1;
                            }
                            yield 0;
                        }
                        case "close" -> null;
                        case "toString" -> "SyntheticPreparedStatement";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
