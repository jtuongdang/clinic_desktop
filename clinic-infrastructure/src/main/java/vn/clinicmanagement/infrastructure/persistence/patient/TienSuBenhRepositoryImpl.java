package vn.clinicmanagement.infrastructure.persistence.patient;

import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.domain.repositories.ITienSuBenhRepository;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** JDBC read adapter for approved history categories in an exact facility/department scope. */
public final class TienSuBenhRepositoryImpl implements ITienSuBenhRepository {
    private static final String HISTORY_SQL = "SELECT id, maBenhNhan, loaiTienSu, moTa, maCoSo, maKhoa, "
            + "version, createdAt, createdBy, updatedAt, updatedBy, isDeleted, deletedAt, deletedBy "
            + "FROM TienSuBenh WHERE maBenhNhan=? AND maCoSo=? AND maKhoa=? "
            + "AND maCoSo IS NOT NULL AND maKhoa IS NOT NULL AND isDeleted=0 "
            + "AND loaiTienSu IN ('BENH_NEN', 'DI_UNG', 'PHAU_THUAT') ORDER BY id";
    private static final String ACTIVE_BY_ID_SQL = HISTORY_SQL.substring(0, HISTORY_SQL.indexOf(" ORDER BY id"))
            .replace("WHERE maBenhNhan=? AND maCoSo=? AND maKhoa=?", "WHERE id=? AND maBenhNhan=? AND maCoSo=? AND maKhoa=?");

    private final JdbcConnectionProvider connections;

    public TienSuBenhRepositoryImpl(JdbcConnectionProvider connections) {
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    @Override
    public List<TienSuBenh> getHistoryByPatientAndScope(int patientId, int facilityId, int departmentId) {
        if (patientId <= 0 || facilityId <= 0 || departmentId <= 0) {
            throw new IllegalArgumentException("Patient, facility, and department IDs must be positive.");
        }
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement statement = lease.connection().prepareStatement(HISTORY_SQL)) {
            statement.setInt(1, patientId);
            statement.setInt(2, facilityId);
            statement.setInt(3, departmentId);
            try (ResultSet result = statement.executeQuery()) {
                List<TienSuBenh> histories = new ArrayList<>();
                while (result.next()) histories.add(mapHistory(result));
                return List.copyOf(histories);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể truy vấn tiền sử bệnh.", exception);
        }
    }

    @Override
    public java.util.Optional<TienSuBenh> getActiveByIdPatientAndScope(
            int historyId, int patientId, int facilityId, int departmentId) {
        if (historyId <= 0 || patientId <= 0 || facilityId <= 0 || departmentId <= 0) {
            throw new IllegalArgumentException("History, patient, facility, and department IDs must be positive.");
        }
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement statement = lease.connection().prepareStatement(ACTIVE_BY_ID_SQL)) {
            statement.setInt(1, historyId);
            statement.setInt(2, patientId);
            statement.setInt(3, facilityId);
            statement.setInt(4, departmentId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? java.util.Optional.of(mapHistory(result)) : java.util.Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể truy vấn tiền sử bệnh.", exception);
        }
    }

    @Override
    public TienSuBenh add(TienSuBenh history) {
        requireWritable(history);
        String sql = "INSERT INTO TienSuBenh (maBenhNhan, loaiTienSu, moTa, maCoSo, maKhoa, createdAt, createdBy) "
                + "VALUES (?, ?, ?, ?, ?, SYSUTCDATETIME(), ?)";
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement statement = lease.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, history.getMaBenhNhan());
            statement.setString(2, history.getLoaiTienSu().name());
            statement.setString(3, history.getMoTa().trim());
            statement.setInt(4, history.getMaCoSo());
            statement.setInt(5, history.getMaKhoa());
            statement.setInt(6, history.getCreatedBy());
            if (statement.executeUpdate() != 1) throw new SQLException("History insert did not affect one row.");
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("History insert returned no identity.");
                history.setId(keys.getInt(1));
                history.setVersion(0L);
                return history;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể lưu tiền sử bệnh.", exception);
        }
    }

    @Override
    public boolean update(TienSuBenh history, long expectedVersion) {
        if (history == null || history.getMaBenhNhan() <= 0 || !isApproved(history.getLoaiTienSu())
                || history.getMoTa() == null || history.getMoTa().isBlank() || history.getMoTa().length() > 1000
                || history.getMaCoSo() == null || history.getMaKhoa() == null
                || history.getMaCoSo() <= 0 || history.getMaKhoa() <= 0 || history.getUpdatedBy() == null
                || history.getUpdatedBy() <= 0) {
            throw new IllegalArgumentException("History row is not valid for persistence.");
        }
        if (history.getId() == null || history.getId() <= 0 || expectedVersion < 0) {
            throw new IllegalArgumentException("History ID and version must be valid.");
        }
        String sql = "UPDATE TienSuBenh SET loaiTienSu=?, moTa=?, updatedAt=SYSUTCDATETIME(), updatedBy=?, "
                + "version=version+1 WHERE id=? AND maBenhNhan=? AND maCoSo=? AND maKhoa=? "
                + "AND maCoSo IS NOT NULL AND maKhoa IS NOT NULL AND isDeleted=0 AND version=?";
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement statement = lease.connection().prepareStatement(sql)) {
            statement.setString(1, history.getLoaiTienSu().name());
            statement.setString(2, history.getMoTa().trim());
            statement.setInt(3, history.getUpdatedBy());
            statement.setInt(4, history.getId());
            statement.setInt(5, history.getMaBenhNhan());
            statement.setInt(6, history.getMaCoSo());
            statement.setInt(7, history.getMaKhoa());
            statement.setLong(8, expectedVersion);
            boolean updated = statement.executeUpdate() == 1;
            if (updated) history.setVersion(expectedVersion + 1);
            return updated;
        } catch (SQLException exception) {
            throw new IllegalStateException("Không thể cập nhật tiền sử bệnh.", exception);
        }
    }

    private static void requireWritable(TienSuBenh history) {
        if (history == null || history.getMaBenhNhan() <= 0 || !isApproved(history.getLoaiTienSu())
                || history.getMoTa() == null || history.getMoTa().isBlank() || history.getMoTa().length() > 1000
                || history.getMaCoSo() == null || history.getMaKhoa() == null
                || history.getMaCoSo() <= 0 || history.getMaKhoa() <= 0 || history.getCreatedBy() == null
                || history.getCreatedBy() <= 0) {
            throw new IllegalArgumentException("History row is not valid for persistence.");
        }
    }

    private static boolean isApproved(LoaiTienSu category) {
        return category == LoaiTienSu.BENH_NEN || category == LoaiTienSu.DI_UNG
                || category == LoaiTienSu.PHAU_THUAT;
    }

    private static TienSuBenh mapHistory(ResultSet result) throws SQLException {
        TienSuBenh history = new TienSuBenh();
        history.setId(result.getInt("id"));
        history.setMaBenhNhan(result.getInt("maBenhNhan"));
        String category = result.getString("loaiTienSu");
        history.setLoaiTienSu(LoaiTienSu.valueOf(category.trim().toUpperCase(Locale.ROOT)));
        history.setMoTa(result.getString("moTa"));
        history.setOwnershipScope((Integer) result.getObject("maCoSo"), (Integer) result.getObject("maKhoa"));
        history.setVersion(result.getLong("version"));
        history.setCreatedAt(localDateTime(result, "createdAt"));
        history.setCreatedBy((Integer) result.getObject("createdBy"));
        history.setUpdatedAt(localDateTime(result, "updatedAt"));
        history.setUpdatedBy((Integer) result.getObject("updatedBy"));
        history.setDeleted(result.getBoolean("isDeleted"));
        history.setDeletedAt(localDateTime(result, "deletedAt"));
        history.setDeletedBy((Integer) result.getObject("deletedBy"));
        return history;
    }

    private static java.time.LocalDateTime localDateTime(ResultSet result, String column) throws SQLException {
        Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
