package vn.clinicmanagement.infrastructure.persistence.system;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcKiemToanRepositoryTest {
    private static final Instant OCCURRED_AT = Instant.parse("2026-02-03T04:05:06.123456Z");
    private static final UUID CORRELATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void currentTransactionWritesUseAndRecordChangesOnTheBorrowedConnection() {
        RecordingConnection database = new RecordingConnection();
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, database.connection);
        JdbcKiemToanRepository repository = repository(transactions, unitOfWork, new RecordingConnection());

        KetQua<Void> result = transactions.inTransaction(() -> {
            repository.appendInCurrentTransaction(systemEntry());
            repository.appendMedicalRecordInCurrentTransaction(medicalEntry());
            assertEquals(2, unitOfWork.saveChanges());
            return KetQua.success(null);
        });

        assertTrue(result.isSuccess());
        assertEquals(2, database.statements.size());
        assertTrue(database.statements.stream().allMatch(s -> s.connection == database.connection));
        assertTrue(database.statements.get(0).sql.startsWith("INSERT INTO NhatKyHeThong"));
        assertTrue(database.statements.get(1).sql.startsWith("INSERT INTO NhatKyHoSoBenhAn"));
        assertEquals(1, database.commits);
        assertEquals(0, database.rollbacks);
    }

    @Test
    void currentTransactionAppendsRequireAnActiveTransaction() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, new RecordingConnection().connection);
        JdbcKiemToanRepository repository = repository(transactions, unitOfWork, new RecordingConnection());

        assertThrows(IllegalStateException.class, () -> repository.appendInCurrentTransaction(systemEntry()));
        assertThrows(IllegalStateException.class,
                () -> repository.appendMedicalRecordInCurrentTransaction(medicalEntry()));
    }

    @Test
    void systemInsertMapsOnlyTypedMetadataAndSerializesUtcWithoutLegacyPayload() {
        RecordingConnection database = new RecordingConnection();
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, database.connection);
        JdbcKiemToanRepository repository = repository(transactions, unitOfWork, new RecordingConnection());

        assertTrue(transactions.inTransaction(() -> {
            repository.appendInCurrentTransaction(systemEntry());
            return KetQua.success(null);
        }).isSuccess());

        RecordedStatement statement = database.statements.get(0);
        assertTrue(statement.sql.startsWith("INSERT INTO NhatKyHeThong"));
        assertFalse(containsMutationStatement(statement.sql));
        assertTrue(statement.sql.contains("chiTiet, ipAddress"));
        assertEquals("PATIENT_CREATED", statement.bindings.get(2));
        assertEquals(7, statement.bindings.get(3));
        assertEquals("DOCTOR", statement.bindings.get(4));
        assertEquals(3, statement.bindings.get(5));
        assertEquals(5, statement.bindings.get(6));
        assertEquals("PATIENT", statement.bindings.get(7));
        assertEquals("91", statement.bindings.get(8));
        assertEquals("SUCCESS", statement.bindings.get(9));
        assertEquals("AUTH_INVALID", statement.bindings.get(10));
        assertEquals("station-01", statement.bindings.get(11));
        assertEquals(CORRELATION_ID.toString(), statement.bindings.get(12));
        assertEquals(LocalDateTime.of(2026, 2, 3, 4, 5, 6, 123456000), statement.bindings.get(13));
        assertEquals(Types.INTEGER, statement.nullTypes.get(1));
    }

    @Test
    void medicalRecordInsertMapsAuthenticatedActorAndVisitWithoutFreeTextReason() {
        RecordingConnection database = new RecordingConnection();
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, database.connection);
        JdbcKiemToanRepository repository = repository(transactions, unitOfWork, new RecordingConnection());

        assertTrue(transactions.inTransaction(() -> {
            repository.appendMedicalRecordInCurrentTransaction(medicalEntry());
            return KetQua.success(null);
        }).isSuccess());

        RecordedStatement statement = database.statements.get(0);
        assertTrue(statement.sql.startsWith("INSERT INTO NhatKyHoSoBenhAn"));
        assertFalse(containsMutationStatement(statement.sql));
        assertEquals(17, statement.bindings.get(1));
        assertEquals(7, statement.bindings.get(2));
        assertEquals("EMR_VIEW", statement.bindings.get(3));
        assertEquals(2, statement.bindings.get(4));
        assertEquals("DOCTOR", statement.bindings.get(5));
        assertEquals("VISIT", statement.bindings.get(8));
        assertEquals("17", statement.bindings.get(9));
        assertEquals("SUCCESS", statement.bindings.get(10));
        assertEquals(null, statement.bindings.get(11));
    }

    @Test
    void independentAppendCommitsSeparatelyAndDoesNotCommitOrRollbackTheOuterTransaction() {
        RecordingConnection outer = new RecordingConnection();
        RecordingConnection independent = new RecordingConnection();
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, outer.connection);
        JdbcKiemToanRepository repository = repository(transactions, unitOfWork, independent);

        KetQua<Void> result = transactions.inTransaction(() -> {
            repository.appendIndependently(systemEntry());
            return KetQua.failure("SYNTHETIC_FAILURE", "synthetic rollback");
        });

        assertFalse(result.isSuccess());
        assertNotSame(outer.connection, independent.connection);
        assertEquals(0, outer.statements.size());
        assertEquals(0, outer.commits);
        assertEquals(1, outer.rollbacks);
        assertEquals(1, independent.statements.size());
        assertEquals(1, independent.commits);
        assertEquals(0, independent.rollbacks);
        assertEquals(1, independent.closes);
    }

    @Test
    void independentAppendRejectsAnInjectedConnectionThatIsTheActiveTransactionConnection() {
        RecordingConnection outer = new RecordingConnection();
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, outer.connection);
        JdbcKiemToanRepository repository = repositoryWithSupplier(transactions, unitOfWork, () -> outer.connection);

        KetQua<Void> result = transactions.inTransaction(() -> {
            assertThrows(IllegalStateException.class, () -> repository.appendIndependently(systemEntry()));
            return KetQua.success(null);
        });

        assertTrue(result.isSuccess());
        assertEquals(0, outer.statements.size());
        assertEquals(1, outer.commits);
    }

    @Test
    void failedAuditInsertSignalsTheEnclosingTransactionToRollBack() {
        RecordingConnection database = new RecordingConnection(new SQLException("synthetic JDBC failure"));
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, database.connection);
        JdbcKiemToanRepository repository = repository(transactions, unitOfWork, new RecordingConnection());

        KetQua<String> result = transactions.inTransaction(() -> {
            KetQua<Void> nested = transactions.inTransaction(() -> {
                repository.appendInCurrentTransaction(systemEntry());
                return KetQua.success(null);
            });
            assertFalse(nested.isSuccess());
            return KetQua.success("synthetic outer operation");
        });

        assertFalse(result.isSuccess());
        assertEquals(0, database.commits);
        assertEquals(1, database.rollbacks);
        assertEquals(1, database.closes);
    }

    @Test
    void v6SqlHasIdempotentLegacyNullableColumnsAndUnconditionalAppendOnlyTriggers() throws Exception {
        String migration;
        try (var stream = getClass().getResourceAsStream(
                "/migrations/V6__Enhance_Audit_Tables_Append_Only.sql")) {
            if (stream == null) throw new AssertionError("V6 migration resource is missing.");
            migration = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        assertEquals(22, occurrences(migration, "COL_LENGTH("));
        assertTrue(migration.contains("occurredAtUtc DATETIME2(7) NULL"));
        assertTrue(migration.contains("WITH NOCHECK"));
        assertTrue(migration.contains("ketQua IS NULL OR ketQua IN ('SUCCESS', 'FAILURE', 'DENIED')"));
        assertEquals(2, occurrences(migration, "AFTER UPDATE, DELETE"));
        assertEquals(2, occurrences(migration, "THROW 51000"));
        assertFalse(migration.toUpperCase().contains("UPDATE NHATKY"));
        assertFalse(migration.toUpperCase().contains("DELETE FROM NHATKY"));
    }

    private static int occurrences(String text, String value) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(value, offset)) >= 0) {
            count++;
            offset += value.length();
        }
        return count;
    }

    private static boolean containsMutationStatement(String sql) {
        return java.util.regex.Pattern.compile("\\b(?:UPDATE|DELETE)\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(sql)
                .find();
    }

    private static HeThongKiemToanNhapLieu systemEntry() {
        return new HeThongKiemToanNhapLieu(
                null, 7, "DOCTOR", 3, 5, KiemToanHanhDongMa.PATIENT_CREATED,
                KiemToanTaiNguyenLoai.PATIENT, 91, KiemToanKetQua.SUCCESS, KiemToanLyDoMa.AUTH_INVALID,
                OCCURRED_AT, "station-01", CORRELATION_ID);
    }

    private static YTeRecordKiemToanNhapLieu medicalEntry() {
        return new YTeRecordKiemToanNhapLieu(
                17, 7, 2, "DOCTOR", 3, 5, KiemToanHanhDongMa.EMR_VIEW,
                KiemToanKetQua.SUCCESS, null, OCCURRED_AT, "room-02", CORRELATION_ID);
    }

    private static JdbcKiemToanRepository repository(
            JdbcGiaoDichQuanLy transactions,
            JdbcUnitOfWork unitOfWork,
            RecordingConnection independent) {
        return repositoryWithSupplier(transactions, unitOfWork, () -> independent.connection);
    }

    private static JdbcKiemToanRepository repositoryWithSupplier(
            JdbcGiaoDichQuanLy transactions,
            JdbcUnitOfWork unitOfWork,
            JdbcKiemToanRepository.IndependentConnectionSupplier independentConnections) {
        return new JdbcKiemToanRepository(
                new JdbcKetNoiNhaCungCap(transactions), unitOfWork, independentConnections);
    }

    private static JdbcGiaoDichQuanLy transactionManager(
            JdbcUnitOfWork unitOfWork, Connection... connections) {
        try {
            Class<?> supplierType = Class.forName(
                    "vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager$ConnectionSupplier");
            AtomicInteger nextConnection = new AtomicInteger();
            Object supplier = Proxy.newProxyInstance(
                    supplierType.getClassLoader(),
                    new Class<?>[]{supplierType},
                    (proxy, method, args) -> {
                        if ("get".equals(method.getName())) {
                            return connections[nextConnection.getAndIncrement()];
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            Constructor<JdbcGiaoDichQuanLy> constructor = JdbcGiaoDichQuanLy.class
                    .getDeclaredConstructor(JdbcUnitOfWork.class, supplierType);
            constructor.setAccessible(true);
            return constructor.newInstance(unitOfWork, supplier);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not configure synthetic JDBC transaction.", exception);
        }
    }

    private static final class RecordingConnection {
        private final List<RecordedStatement> statements = new ArrayList<>();
        private final SQLException insertFailure;
        private final Connection connection;
        private int commits;
        private int rollbacks;
        private int closes;

        private RecordingConnection() {
            this(null);
        }

        private RecordingConnection(SQLException insertFailure) {
            this.insertFailure = insertFailure;
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "prepareStatement" -> {
                            RecordedStatement statement = new RecordedStatement((String) args[0], this);
                            statements.add(statement);
                            yield statement.preparedStatement;
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
                        case "setAutoCommit" -> null;
                        case "isClosed" -> closes > 0;
                        case "toString" -> "SyntheticAuditConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    private static final class RecordedStatement {
        private final String sql;
        private final RecordingConnection owner;
        private final Map<Integer, Object> bindings = new HashMap<>();
        private final Map<Integer, Integer> nullTypes = new HashMap<>();
        private final PreparedStatement preparedStatement;
        private final Connection connection;

        private RecordedStatement(String sql, RecordingConnection owner) {
            this.sql = sql;
            this.owner = owner;
            this.connection = owner.connection;
            preparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setInt", "setString", "setObject" -> {
                            bindings.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "setNull" -> {
                            int index = (Integer) args[0];
                            bindings.put(index, null);
                            nullTypes.put(index, (Integer) args[1]);
                            yield null;
                        }
                        case "executeUpdate" -> {
                            if (owner.insertFailure != null) throw owner.insertFailure;
                            yield 1;
                        }
                        case "close" -> null;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
