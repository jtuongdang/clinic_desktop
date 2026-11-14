package vn.clinicmanagement.infrastructure.persistence.system;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public final class JdbcKiemToanRepository implements KiemToanWriteCongKetNoi {
    private static final String INSERT_SYSTEM_EVENT = "INSERT INTO NhatKyHeThong "
            + "(maNhanVien, hanhDong, chiTiet, ipAddress, version, createdBy, updatedAt, updatedBy, "
            + "isDeleted, deletedAt, deletedBy, maVaiTro, maVaiTroCode, maCoSo, maKhoa, loaiDoiTuong, "
            + "maDoiTuong, ketQua, maLyDo, workstationId, correlationId, occurredAtUtc) "
            + "VALUES (?, ?, NULL, NULL, 0, NULL, NULL, NULL, 0, NULL, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_MEDICAL_RECORD_EVENT = "INSERT INTO NhatKyHoSoBenhAn "
            + "(maLuotKham, maNhanVien, hanhDong, lyDo, version, createdBy, updatedAt, updatedBy, "
            + "isDeleted, deletedAt, deletedBy, maVaiTro, maVaiTroCode, maCoSo, maKhoa, loaiDoiTuong, "
            + "maDoiTuong, ketQua, maLyDo, workstationId, correlationId, occurredAtUtc) "
            + "VALUES (?, ?, ?, NULL, 0, NULL, NULL, NULL, 0, NULL, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcKetNoiNhaCungCap connections;
    /** Trường `unitOfWork`: đơn vị của làm việc. */
    private final JdbcUnitOfWork unitOfWork;
    private final IndependentConnectionSupplier independentConnections;

    public JdbcKiemToanRepository(
            JdbcKetNoiNhaCungCap connections,
            JdbcUnitOfWork unitOfWork,
            IndependentConnectionSupplier independentConnections) {
        this.connections = Objects.requireNonNull(connections, "connections");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork");
        this.independentConnections = Objects.requireNonNull(independentConnections, "independentConnections");
    }

    /** Hàm `appendInCurrentTransaction`: thêm vào trong hientai giao dịch. Gốc khai báo tại `vn.clinicmanagement.application.attendance.KiemToanWriteCongKetNoi#appendInCurrentTransaction`. */
    @Override
    public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {
        Objects.requireNonNull(entry, "entry");
        requireActiveTransaction();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire()) {
            insertSystemEvent(lease.connection(), entry);
            unitOfWork.recordChanges(1);
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    /** Hàm `appendIndependently`: thêm vào `Independently`. Gốc khai báo tại `vn.clinicmanagement.application.attendance.KiemToanWriteCongKetNoi#appendIndependently`. */
    @Override
    public void appendIndependently(HeThongKiemToanNhapLieu entry) {
        Objects.requireNonNull(entry, "entry");
        appendIndependently(connection -> insertSystemEvent(connection, entry));
    }

    /** Hàm `appendMedicalRecordInCurrentTransaction`: thêm vào yte ghi nhận trong hientai giao dịch. Gốc khai báo tại `vn.clinicmanagement.application.attendance.KiemToanWriteCongKetNoi#appendMedicalRecordInCurrentTransaction`. */
    @Override
    public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {
        Objects.requireNonNull(entry, "entry");
        requireActiveTransaction();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire()) {
            insertMedicalRecordEvent(lease.connection(), entry);
            unitOfWork.recordChanges(1);
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    /** Hàm `appendMedicalRecordIndependently`: thêm vào yte ghi nhận `Independently`. Gốc khai báo tại `vn.clinicmanagement.application.attendance.KiemToanWriteCongKetNoi#appendMedicalRecordIndependently`. */
    @Override
    public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {
        Objects.requireNonNull(entry, "entry");
        appendIndependently(connection -> insertMedicalRecordEvent(connection, entry));
    }

    /** Hàm `appendIndependently`: thêm vào `Independently`. */
    private void appendIndependently(SqlInsert insert) {
        try (Connection connection = independentConnections.get()) {
            if (connection == null) {
                throw new IllegalStateException("Independent audit connection supplier returned no connection.");
            }
            requireIndependentConnection(connection);
            connection.setAutoCommit(false);
            try {
                insert.execute(connection);
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        } catch (SQLException exception) {
            throw persistenceFailure(exception);
        }
    }

    /** Hàm `requireActiveTransaction`: yêu cầu hoạt động giao dịch. */
    private void requireActiveTransaction() {
        if (!connections.isTransactionActive()) {
            throw new IllegalStateException("An active transaction is required for this audit append.");
        }
    }

    /** Hàm `requireIndependentConnection`: yêu cầu `Independent` kết nối. */
    private void requireIndependentConnection(Connection independentConnection) throws SQLException {
        if (!connections.isTransactionActive()) return;
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire()) {
            if (independentConnection == lease.connection()) {
                throw new IllegalStateException(
                        "Independent audit append cannot reuse the active transaction connection.");
            }
        }
    }

    /** Hàm `insertSystemEvent`: chèn hệ thống sukien. */
    private static void insertSystemEvent(Connection connection, HeThongKiemToanNhapLieu entry) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SYSTEM_EVENT)) {
            setNullableInteger(statement, 1, entry.actorEmployeeId());
            statement.setString(2, entry.actionCode().name());
            bindSharedMetadata(statement, 3, entry.roleId(), entry.roleCode(), entry.facilityId(),
                    entry.departmentId(), entry.resourceType() == null ? null : entry.resourceType().name(),
                    entry.resourceId(), entry.outcome().name(), entry.reasonCode() == null ? null
                            : entry.reasonCode().name(), entry.workstationId(), entry.correlationId().toString(),
                    entry.occurredAtUtc());
            requireSingleInsert(statement.executeUpdate());
        }
    }

    /** Hàm `insertMedicalRecordEvent`: chèn yte ghi nhận sukien. */
    private static void insertMedicalRecordEvent(
            Connection connection, YTeRecordKiemToanNhapLieu entry) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_MEDICAL_RECORD_EVENT)) {
            statement.setInt(1, entry.visitId());
            statement.setInt(2, entry.actorEmployeeId());
            statement.setString(3, entry.actionCode().name());
            bindSharedMetadata(statement, 4, entry.roleId(), entry.roleCode(), entry.facilityId(),
                    entry.departmentId(), entry.resourceType().name(), entry.resourceId(),
                    entry.outcome().name(), entry.reasonCode() == null ? null : entry.reasonCode().name(),
                    entry.workstationId(), entry.correlationId().toString(), entry.occurredAtUtc());
            requireSingleInsert(statement.executeUpdate());
        }
    }

    /** Hàm `bindSharedMetadata`: gắn kết `Shared` sieudulieu. */
    private static void bindSharedMetadata(
            PreparedStatement statement,
            int firstIndex,
            Integer roleId,
            String roleCode,
            Integer facilityId,
            Integer departmentId,
            String resourceType,
            Integer resourceId,
            String outcome,
            String reasonCode,
            String workstationId,
            String correlationId,
            java.time.Instant occurredAtUtc) throws SQLException {
        setNullableInteger(statement, firstIndex, roleId);
        setNullableString(statement, firstIndex + 1, roleCode);
        setNullableInteger(statement, firstIndex + 2, facilityId);
        setNullableInteger(statement, firstIndex + 3, departmentId);
        setNullableString(statement, firstIndex + 4, resourceType);
        setNullableString(statement, firstIndex + 5, resourceId == null ? null : resourceId.toString());
        statement.setString(firstIndex + 6, outcome);
        setNullableString(statement, firstIndex + 7, reasonCode);
        setNullableString(statement, firstIndex + 8, workstationId);
        statement.setString(firstIndex + 9, correlationId);
        statement.setObject(firstIndex + 10, LocalDateTime.ofInstant(occurredAtUtc, ZoneOffset.UTC));
    }

    /** Hàm `setNullableInteger`: gán `Nullable` `Integer`. */
    private static void setNullableInteger(PreparedStatement statement, int index, Integer value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.INTEGER);
        else statement.setInt(index, value);
    }

    /** Hàm `setNullableString`: gán `Nullable` `String`. */
    private static void setNullableString(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.VARCHAR);
        else statement.setString(index, value);
    }

    /** Hàm `requireSingleInsert`: yêu cầu `Single` chèn. */
    private static void requireSingleInsert(int affectedRows) throws SQLException {
        if (affectedRows != 1) throw new SQLException("Audit insert did not affect exactly one row.");
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    /** Hàm `persistenceFailure`: luutru thất bại. */
    private static IllegalStateException persistenceFailure(SQLException exception) {
        return new IllegalStateException("Audit persistence failed.", exception);
    }

    @FunctionalInterface
    public interface IndependentConnectionSupplier {
        /** Hàm `get`: lấy. */
        Connection get() throws SQLException;
    }

    @FunctionalInterface
    private interface SqlInsert {
        void execute(Connection connection) throws SQLException;
    }
}
