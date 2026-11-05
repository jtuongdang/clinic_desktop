package vn.clinicmanagement.infrastructure.persistence.identity;

import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;
import vn.clinicmanagement.domain.repositories.ISpecification;
import vn.clinicmanagement.domain.repositories.PagedResult;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

/** SQL Server implementation of employee repository. */
public final class NhanVienRepositoryImpl implements INhanVienRepository {
    private static final String SELECT_COLUMNS = "SELECT id, maNhanVien, ho, ten, soDienThoai, email, diaChi, "
            + "gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, dangNhapCuoi, "
            + "chungChiHanhNghe, ngayVaoLam, ngayNghiViec, lyDoNghiViec, luongCoBan, heSoLuong, phuCap, "
            + "version, createdAt, createdBy, updatedAt, updatedBy, isDeleted, deletedAt, deletedBy FROM NhanVien";

    private final JdbcConnectionProvider connections;
    private final JdbcUnitOfWork unitOfWork;

    public NhanVienRepositoryImpl(JdbcConnectionProvider connections, JdbcUnitOfWork unitOfWork) {
        this.connections = connections;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Optional<NhanVien> getById(int id) {
        return queryOne(SELECT_COLUMNS + " WHERE id = ? AND isDeleted = 0", ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<NhanVien> getByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return queryOne(SELECT_COLUMNS + " WHERE LOWER(username) = LOWER(?) AND isDeleted = 0",
                ps -> ps.setString(1, username.trim()));
    }

    @Override
    public List<NhanVien> getAll() {
        return queryMany(SELECT_COLUMNS + " WHERE isDeleted = 0 ORDER BY id", ps -> {});
    }

    @Override
    public PagedResult<NhanVien> getPaged(int pageIndex, int pageSize, ISpecification<NhanVien> specification) {
        int safePage = Math.max(1, pageIndex);
        int safeSize = Math.max(1, Math.min(pageSize, 500));
        List<NhanVien> filtered = find(specification);
        int from = Math.min((safePage - 1) * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        return new PagedResult<>(filtered.subList(from, to), filtered.size(), safePage, safeSize);
    }

    @Override
    public List<NhanVien> find(ISpecification<NhanVien> specification) {
        List<NhanVien> employees = getAll();
        return specification == null ? employees
                : employees.stream().filter(specification.toPredicate()).toList();
    }

    @Override
    public boolean exists(ISpecification<NhanVien> specification) {
        return !find(specification).isEmpty();
    }

    @Override
    public int count(ISpecification<NhanVien> specification) {
        return find(specification).size();
    }

    @Override
    public void add(NhanVien employee) {
        String sql = "INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, "
                + "maChucVu, username, passwordHash, hoatDong, dangNhapCuoi, chungChiHanhNghe, ngayVaoLam, "
                + "ngayNghiViec, lyDoNghiViec, luongCoBan, heSoLuong, phuCap, createdBy) "
                + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bindEmployee(ps, employee);
                ps.setObject(21, employee.getCreatedBy(), Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalStateException("Không nhận được ID nhân viên sau khi lưu.");
                    employee.setId(rs.getInt(1));
                }
            }
            recordChange();
            return null;
        });
    }

    @Override
    public void update(NhanVien employee) {
        if (employee.getId() == null) throw new IllegalArgumentException("Không thể cập nhật nhân viên chưa có ID.");
        String sql = "UPDATE NhanVien SET maNhanVien=?, ho=?, ten=?, soDienThoai=?, email=?, diaChi=?, gioiTinh=?, "
                + "ngaySinh=?, maChucVu=?, username=?, passwordHash=?, hoatDong=?, dangNhapCuoi=?, "
                + "chungChiHanhNghe=?, ngayVaoLam=?, ngayNghiViec=?, lyDoNghiViec=?, luongCoBan=?, heSoLuong=?, "
                + "phuCap=?, updatedAt=SYSDATETIME(), updatedBy=?, version=version+1 "
                + "WHERE id=? AND version=? AND isDeleted=0";
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bindEmployee(ps, employee);
                ps.setObject(21, employee.getUpdatedBy(), Types.INTEGER);
                ps.setInt(22, employee.getId());
                ps.setLong(23, employee.getVersion() == null ? 0L : employee.getVersion());
                int changed = ps.executeUpdate();
                if (changed == 0) throw new IllegalStateException("Nhân viên không tồn tại hoặc dữ liệu đã được cập nhật bởi người khác.");
                employee.setVersion((employee.getVersion() == null ? 0L : employee.getVersion()) + 1);
                employee.setUpdatedAt(LocalDateTime.now());
            }
            recordChange();
            return null;
        });
    }

    @Override
    public void delete(NhanVien employee) {
        if (employee.getId() == null) return;
        withConnection(connection -> {
            String sql = "UPDATE NhanVien SET isDeleted=1, deletedAt=SYSDATETIME(), deletedBy=?, "
                    + "version=version+1 WHERE id=? AND version=? AND isDeleted=0";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setObject(1, employee.getDeletedBy(), Types.INTEGER);
                ps.setInt(2, employee.getId());
                ps.setLong(3, employee.getVersion() == null ? 0L : employee.getVersion());
                int changed = ps.executeUpdate();
                if (changed == 0) throw new IllegalStateException("Nhân viên không tồn tại hoặc dữ liệu đã được cập nhật bởi người khác.");
            }
            employee.setVersion((employee.getVersion() == null ? 0L : employee.getVersion()) + 1);
            employee.setDeleted(true);
            employee.setDeletedAt(LocalDateTime.now());
            recordChange();
            return null;
        });
    }

    @Override
    public List<NhanVien> getActiveDoctors(int maCoSo) {
        String sql = "SELECT DISTINCT nv.id, nv.maNhanVien, nv.ho, nv.ten, nv.soDienThoai, nv.email, nv.diaChi, "
                + "nv.gioiTinh, nv.ngaySinh, nv.maChucVu, nv.username, nv.passwordHash, nv.hoatDong, nv.dangNhapCuoi, "
                + "nv.chungChiHanhNghe, nv.ngayVaoLam, nv.ngayNghiViec, nv.lyDoNghiViec, nv.luongCoBan, nv.heSoLuong, nv.phuCap, "
                + "nv.version, nv.createdAt, nv.createdBy, nv.updatedAt, nv.updatedBy, nv.isDeleted, nv.deletedAt, nv.deletedBy "
                + " FROM NhanVien nv INNER JOIN NhanVienVaiTro nvt ON nvt.maNhanVien=nv.id "
                + "INNER JOIN VaiTro vt ON vt.id=nvt.maVaiTro "
                + "INNER JOIN PhanQuyenCoSo pq ON pq.maNhanVien=nv.id "
                + "WHERE nv.hoatDong=1 AND nv.isDeleted=0 AND nvt.isDeleted=0 AND vt.isDeleted=0 "
                + "AND UPPER(vt.tenVaiTro)='DOCTOR' AND pq.maCoSo=? AND pq.isDeleted=0 ORDER BY nv.ho, nv.ten";
        return queryMany(sql, ps -> ps.setInt(1, maCoSo));
    }

    @Override
    public List<String> getRoleNames(int employeeId) {
        String sql = "SELECT vt.tenVaiTro FROM NhanVienVaiTro nvt "
                + "INNER JOIN VaiTro vt ON vt.id=nvt.maVaiTro "
                + "WHERE nvt.maNhanVien=? AND nvt.isDeleted=0 AND vt.isDeleted=0 ORDER BY vt.id";
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> roles = new ArrayList<>();
                while (rs.next()) roles.add(rs.getString(1));
                return roles;
            }
        } catch (SQLException ex) {
            throw persistenceFailure(ex);
        }
    }

    @Override
    public List<VaiTro> getAssignedRoles(int employeeId) {
        String sql = "SELECT vt.id, vt.tenVaiTro, vt.version, vt.createdAt, vt.createdBy, "
                + "vt.updatedAt, vt.updatedBy, vt.isDeleted, vt.deletedAt, vt.deletedBy FROM NhanVien nv "
                + "INNER JOIN NhanVienVaiTro nvt ON nvt.maNhanVien=nv.id "
                + "INNER JOIN VaiTro vt ON vt.id=nvt.maVaiTro "
                + "WHERE nv.id=? AND nv.hoatDong=1 AND nv.isDeleted=0 "
                + "AND nvt.isDeleted=0 AND vt.isDeleted=0 ORDER BY vt.id";
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                List<VaiTro> roles = new ArrayList<>();
                while (rs.next()) {
                    VaiTro role = new VaiTro();
                    role.setId(rs.getInt("id"));
                    role.setTenVaiTro(rs.getString("tenVaiTro"));
                    role.setVersion(rs.getLong("version"));
                    role.setCreatedAt(localDateTime(rs, "createdAt"));
                    role.setCreatedBy((Integer) rs.getObject("createdBy"));
                    role.setUpdatedAt(localDateTime(rs, "updatedAt"));
                    role.setUpdatedBy((Integer) rs.getObject("updatedBy"));
                    role.setDeleted(rs.getBoolean("isDeleted"));
                    role.setDeletedAt(localDateTime(rs, "deletedAt"));
                    role.setDeletedBy((Integer) rs.getObject("deletedBy"));
                    roles.add(role);
                }
                return roles;
            }
        } catch (SQLException ex) {
            throw persistenceFailure(ex);
        }
    }

    @Override
    public List<NhanVienWorkScope> getAssignedWorkScopes(int employeeId) {
        String sql = "SELECT DISTINCT cs.id AS maCoSo, cs.tenCoSo AS tenCoSo, "
                + "k.id AS maKhoa, k.tenKhoa AS tenKhoa "
                + "FROM NhanVien nv "
                + "INNER JOIN PhanQuyenCoSo pq ON pq.maNhanVien=nv.id "
                + "INNER JOIN CoSo cs ON cs.id=pq.maCoSo "
                + "INNER JOIN NhanVienKhoa nk ON nk.maNhanVien=nv.id "
                + "INNER JOIN Khoa k ON k.id=nk.maKhoa AND k.maCoSo=cs.id AND k.maCoSo=pq.maCoSo "
                + "WHERE nv.id=? AND nv.hoatDong=1 AND nv.isDeleted=0 "
                + "AND pq.isDeleted=0 AND cs.hoatDong=1 AND cs.isDeleted=0 "
                + "AND nk.isDeleted=0 AND k.hoatDong=1 AND k.isDeleted=0 "
                + "ORDER BY maCoSo, maKhoa";
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                List<NhanVienWorkScope> scopes = new ArrayList<>();
                while (rs.next()) {
                    scopes.add(new NhanVienWorkScope(rs.getInt("maCoSo"), rs.getString("tenCoSo"),
                            rs.getInt("maKhoa"), rs.getString("tenKhoa")));
                }
                return scopes;
            }
        } catch (SQLException ex) {
            throw persistenceFailure(ex);
        }
    }

    @Override
    public void recordSuccessfulLogin(int employeeId, LocalDateTime loginAt) {
        if (employeeId <= 0) throw new IllegalArgumentException("Mã nhân viên không hợp lệ.");
        if (loginAt == null) throw new IllegalArgumentException("Thời điểm đăng nhập không được để trống.");
        if (!connections.isTransactionActive()) {
            throw new IllegalStateException("Cập nhật thời điểm đăng nhập yêu cầu giao dịch đang hoạt động.");
        }
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE NhanVien SET dangNhapCuoi=? WHERE id=? AND hoatDong=1 AND isDeleted=0")) {
                ps.setTimestamp(1, Timestamp.valueOf(loginAt));
                ps.setInt(2, employeeId);
                if (ps.executeUpdate() != 1) throw new IllegalStateException("Tài khoản không còn hoạt động.");
            }
            recordChange();
            return null;
        });
    }

    @Override
    public IUnitOfWork getUnitOfWork() { return unitOfWork; }

    private Optional<NhanVien> queryOne(String sql, SqlBinder binder) {
        List<NhanVien> matches = queryMany(sql, binder);
        return matches.stream().findFirst();
    }

    private List<NhanVien> queryMany(String sql, SqlBinder binder) {
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<NhanVien> employees = new ArrayList<>();
                while (rs.next()) employees.add(mapEmployee(rs));
                return employees;
            }
        } catch (SQLException ex) {
            throw persistenceFailure(ex);
        }
    }

    private <T> T withConnection(SqlWork<T> work) {
        try (JdbcConnectionProvider.Lease lease = connections.acquire()) {
            return work.execute(lease.connection());
        } catch (SQLException ex) {
            throw persistenceFailure(ex);
        }
    }

    private void recordChange() {
        if (connections.isTransactionActive()) unitOfWork.recordChanges(1);
    }

    private static NhanVien mapEmployee(ResultSet rs) throws SQLException {
        NhanVien employee = new NhanVien();
        employee.setId(rs.getInt("id"));
        employee.setMaNhanVien(rs.getString("maNhanVien"));
        employee.setHo(rs.getString("ho"));
        employee.setTen(rs.getString("ten"));
        employee.setSoDienThoai(rs.getString("soDienThoai"));
        employee.setEmail(rs.getString("email"));
        employee.setDiaChi(rs.getString("diaChi"));
        String gender = rs.getString("gioiTinh");
        if (gender != null) {
            try { employee.setGioiTinh(GioiTinh.valueOf(gender.trim().toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException ignored) { employee.setGioiTinh(GioiTinh.O); }
        }
        employee.setNgaySinh(localDate(rs, "ngaySinh"));
        employee.setMaChucVu((Integer) rs.getObject("maChucVu"));
        employee.setUsername(rs.getString("username"));
        employee.setPasswordHash(rs.getString("passwordHash"));
        employee.setHoatDong(rs.getBoolean("hoatDong"));
        employee.setDangNhapCuoi(localDateTime(rs, "dangNhapCuoi"));
        employee.setChungChiHanhNghe(rs.getString("chungChiHanhNghe"));
        employee.setNgayVaoLam(localDate(rs, "ngayVaoLam"));
        employee.setNgayNghiViec(localDate(rs, "ngayNghiViec"));
        employee.setLyDoNghiViec(rs.getString("lyDoNghiViec"));
        employee.setLuongCoBan(zeroIfNull(rs.getBigDecimal("luongCoBan")));
        employee.setHeSoLuong(zeroIfNull(rs.getBigDecimal("heSoLuong")));
        employee.setPhuCap(zeroIfNull(rs.getBigDecimal("phuCap")));
        employee.setVersion(rs.getLong("version"));
        employee.setCreatedAt(localDateTime(rs, "createdAt"));
        employee.setCreatedBy((Integer) rs.getObject("createdBy"));
        employee.setUpdatedAt(localDateTime(rs, "updatedAt"));
        employee.setUpdatedBy((Integer) rs.getObject("updatedBy"));
        employee.setDeleted(rs.getBoolean("isDeleted"));
        employee.setDeletedAt(localDateTime(rs, "deletedAt"));
        employee.setDeletedBy((Integer) rs.getObject("deletedBy"));
        return employee;
    }

    private static void bindEmployee(PreparedStatement ps, NhanVien e) throws SQLException {
        int i = 1;
        ps.setString(i++, e.getMaNhanVien());
        ps.setString(i++, e.getHo());
        ps.setString(i++, e.getTen());
        ps.setString(i++, e.getSoDienThoai());
        ps.setString(i++, e.getEmail());
        ps.setString(i++, e.getDiaChi());
        ps.setString(i++, e.getGioiTinh() == null ? null : e.getGioiTinh().name());
        ps.setObject(i++, e.getNgaySinh());
        ps.setObject(i++, e.getMaChucVu(), Types.INTEGER);
        ps.setString(i++, e.getUsername());
        ps.setString(i++, e.getPasswordHash());
        ps.setBoolean(i++, e.isHoatDong());
        setDateTime(ps, i++, e.getDangNhapCuoi());
        ps.setString(i++, e.getChungChiHanhNghe());
        ps.setObject(i++, e.getNgayVaoLam());
        ps.setObject(i++, e.getNgayNghiViec());
        ps.setString(i++, e.getLyDoNghiViec());
        ps.setBigDecimal(i++, valueOrZero(e.getLuongCoBan()));
        ps.setBigDecimal(i++, valueOrZero(e.getHeSoLuong()));
        ps.setBigDecimal(i++, valueOrZero(e.getPhuCap()));
    }

    private static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static void setDateTime(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) ps.setNull(index, Types.TIMESTAMP);
        else ps.setTimestamp(index, Timestamp.valueOf(value));
    }

    private static BigDecimal valueOrZero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static BigDecimal zeroIfNull(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private static IllegalStateException persistenceFailure(SQLException ex) {
        return new IllegalStateException("Lỗi truy cập dữ liệu nhân viên.", ex);
    }

    @FunctionalInterface private interface SqlBinder { void bind(PreparedStatement statement) throws SQLException; }
    @FunctionalInterface private interface SqlWork<T> { T execute(Connection connection) throws SQLException; }
}
