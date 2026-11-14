package vn.clinicmanagement.infrastructure.persistence.system;

import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.repositories.IHeThongRepository;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import vn.clinicmanagement.domain.system.ThamSoHeThong;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class HeThongRepository implements IHeThongRepository {
    private final JdbcKetNoiNhaCungCap connections;
    /** Trường `unitOfWork`: đơn vị của làm việc. */
    private final JdbcUnitOfWork unitOfWork;

    public HeThongRepository(JdbcKetNoiNhaCungCap connections, JdbcUnitOfWork unitOfWork) {
        this.connections = connections;
        this.unitOfWork = unitOfWork;
    }

    /** Hàm `getMatrixByRoleId`: lấy matran theo vai trò mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHeThongRepository#getMatrixByRoleId`. */
    @Override
    public List<MaTranPhanQuyen> getMatrixByRoleId(int roleId) {
        String sql = "SELECT id, maVaiTro, phanHe, hanhDong, phamViDuLieu, duocPhep, "
                + "version, createdAt, createdBy, updatedAt, updatedBy, isDeleted, deletedAt, deletedBy "
                + "FROM MaTranPhanQuyen WHERE maVaiTro=? AND isDeleted=0";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
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
                    permission.setVersion(resultSet.getLong("version"));
                    permission.setCreatedAt(localDateTime(resultSet, "createdAt"));
                    permission.setCreatedBy((Integer) resultSet.getObject("createdBy"));
                    permission.setUpdatedAt(localDateTime(resultSet, "updatedAt"));
                    permission.setUpdatedBy((Integer) resultSet.getObject("updatedBy"));
                    permission.setDeleted(resultSet.getBoolean("isDeleted"));
                    permission.setDeletedAt(localDateTime(resultSet, "deletedAt"));
                    permission.setDeletedBy((Integer) resultSet.getObject("deletedBy"));
                    matrix.add(permission);
                }
                return matrix;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy cập ma trận phân quyền hệ thống.", ex);
        }
    }

    /** Hàm `localDateTime`: `local` ngày thời gian. */
    private static LocalDateTime localDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    /** Hàm `getSystemParameterValue`: lấy hệ thống `Parameter` giá trị. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHeThongRepository#getSystemParameterValue`. */
    @Override
    public Optional<String> getSystemParameterValue(String key) {
        String sql = "SELECT giaTri FROM ThamSoHeThong WHERE maThamSo=? AND isDeleted=0";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement statement = lease.connection().prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.ofNullable(resultSet.getString("giaTri")) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy cập tham số hệ thống.", ex);
        }
    }

    /** Hàm `getAllSystemParameters`: lấy `All` hệ thống tham số. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHeThongRepository#getAllSystemParameters`. */
    @Override
    public List<ThamSoHeThong> getAllSystemParameters() {
        String sql = "SELECT id, maThamSo, tenThamSo, giaTri, moTa, version, createdAt, updatedAt "
                + "FROM ThamSoHeThong WHERE isDeleted=0 ORDER BY maThamSo";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement statement = lease.connection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<ThamSoHeThong> list = new ArrayList<>();
            while (resultSet.next()) {
                ThamSoHeThong item = new ThamSoHeThong();
                item.setId(resultSet.getInt("id"));
                item.setMaThamSo(resultSet.getString("maThamSo"));
                item.setTenThamSo(resultSet.getString("tenThamSo"));
                item.setGiaTri(resultSet.getString("giaTri"));
                item.setMoTa(resultSet.getString("moTa"));
                item.setVersion(resultSet.getLong("version"));
                item.setCreatedAt(localDateTime(resultSet, "createdAt"));
                item.setUpdatedAt(localDateTime(resultSet, "updatedAt"));
                list.add(item);
            }
            return list;
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy cập danh sách tham số hệ thống.", ex);
        }
    }

    /** Hàm `updateSystemParameter`: cập nhật hệ thống `Parameter`. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IHeThongRepository#updateSystemParameter`. */
    @Override
    public void updateSystemParameter(String key, String value, Integer updatedBy) {
        String sql = "UPDATE ThamSoHeThong SET giaTri=?, updatedAt=GETDATE(), updatedBy=? WHERE maThamSo=? AND isDeleted=0";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement statement = lease.connection().prepareStatement(sql)) {
            statement.setString(1, value);
            if (updatedBy != null) {
                statement.setInt(2, updatedBy);
            } else {
                statement.setNull(2, java.sql.Types.INTEGER);
            }
            statement.setString(3, key);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi cập nhật tham số hệ thống.", ex);
        }
    }
}
