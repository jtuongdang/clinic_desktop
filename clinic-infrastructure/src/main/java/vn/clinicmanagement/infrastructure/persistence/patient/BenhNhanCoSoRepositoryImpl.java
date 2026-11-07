package vn.clinicmanagement.infrastructure.persistence.patient;

import vn.clinicmanagement.domain.repositories.IBenhNhanCoSoRepository;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** SQL Server persistence for active patient-facility associations. */
public final class BenhNhanCoSoRepositoryImpl implements IBenhNhanCoSoRepository {
    private static final String ACTIVE_LINK_SQL = "SELECT id FROM BenhNhanCoSo "
            + "WITH (UPDLOCK, HOLDLOCK, INDEX(UX_BenhNhanCoSo_Active_Patient_Facility)) "
            + "WHERE maBenhNhan=? AND maCoSo=? AND isDeleted=0";
    private static final String INSERT_LINK_SQL = "INSERT INTO BenhNhanCoSo (maBenhNhan, maCoSo, createdBy) "
            + "VALUES (?, ?, ?)";

    private final JdbcConnectionProvider connections;
    private final JdbcUnitOfWork unitOfWork;

    public BenhNhanCoSoRepositoryImpl(JdbcConnectionProvider connections, JdbcUnitOfWork unitOfWork) {
        this.connections = connections;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public boolean ensureActiveLink(int patientId, int facilityId, int createdBy) {
        if (!connections.isTransactionActive()) {
            throw new IllegalStateException("Patient-facility links must be ensured in the registration transaction.");
        }
        if (patientId <= 0 || facilityId <= 0 || createdBy <= 0) {
            throw new IllegalArgumentException("Patient, facility, and actor identifiers must be positive.");
        }

        try (JdbcConnectionProvider.Lease lease = connections.acquire()) {
            Connection connection = lease.connection();
            if (hasActiveLink(connection, patientId, facilityId)) return false;
            insertLink(connection, patientId, facilityId, createdBy);
            unitOfWork.recordChanges(1);
            return true;
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể lưu liên kết người bệnh với cơ sở.", ex);
        }
    }

    private static boolean hasActiveLink(Connection connection, int patientId, int facilityId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(ACTIVE_LINK_SQL)) {
            statement.setInt(1, patientId);
            statement.setInt(2, facilityId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void insertLink(Connection connection, int patientId, int facilityId, int createdBy)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_LINK_SQL)) {
            statement.setInt(1, patientId);
            statement.setInt(2, facilityId);
            statement.setInt(3, createdBy);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Patient-facility link insert affected an unexpected number of rows.");
            }
        }
    }
}
