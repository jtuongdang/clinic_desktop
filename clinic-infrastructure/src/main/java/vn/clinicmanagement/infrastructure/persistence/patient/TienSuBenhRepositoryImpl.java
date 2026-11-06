package vn.clinicmanagement.infrastructure.persistence.patient;

import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.domain.repositories.ITienSuBenhRepository;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
