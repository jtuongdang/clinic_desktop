package vn.clinicmanagement.infrastructure.persistence.system;

import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.repositories.ISystemRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SystemRepositoryImpl implements ISystemRepository {
    private final JdbcConnectionProvider connections;
    private final JdbcUnitOfWork unitOfWork;

    public SystemRepositoryImpl(JdbcConnectionProvider connections, JdbcUnitOfWork unitOfWork) {
        this.connections = connections;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
        String sql = "SELECT id, maVaiTro, phanHe, hanhDong, phamViDuLieu, duocPhep "
                + "FROM MaTranPhanQuyen WHERE maVaiTro=? AND isDeleted=0";
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement statement = lease.connection().prepareStatement(sql)) {
            statement.setInt(1, roleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MaTranPhanQuyen> matrix = new ArrayList<>();
                while (resultSet.next()) {
                    MaTranPhanQuyen permission = new MaTranPhanQuyen();
                    permission.setId(resultSet.getInt("id"));
                    permission.setMaVaiTro(resultSet.getInt("maVaiTro"));
                    permission.setPhanHe(resultSet.getString("phanHe"));
                    permission.setHanhDong(HanhDongQuyen.valueOf(resultSet.getString("hanhDong")));
                    permission.setPhamViDuLieu(PhamViDuLieu.valueOf(resultSet.getString("phamViDuLieu")));
                    permission.setDuocPhep(resultSet.getBoolean("duocPhep"));
                    matrix.add(permission);
                }
                return matrix;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy cập ma trận phân quyền hệ thống.", ex);
        }
    }

    @Override
    public Optional<String> getSystemParameterValue(String key) {
        String sql = "SELECT giaTri FROM ThamSoHeThong WHERE maThamSo=? AND isDeleted=0";
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement statement = lease.connection().prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.ofNullable(resultSet.getString("giaTri")) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy cập tham số hệ thống.", ex);
        }
    }
}
