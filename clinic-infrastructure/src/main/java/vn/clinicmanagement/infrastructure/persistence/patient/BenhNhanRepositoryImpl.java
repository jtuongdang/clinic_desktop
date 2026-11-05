package vn.clinicmanagement.infrastructure.persistence.patient;

import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.PatientIdentityConflictException;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.ISpecification;
import vn.clinicmanagement.domain.repositories.PagedResult;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

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
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** SQL Server persistence for the patient aggregate. */
public final class BenhNhanRepositoryImpl implements IBenhNhanRepository {
    private static final String ACTIVE_CCCD_UNIQUE_INDEX = "UX_BenhNhan_Active_CCCD";
    private static final Pattern ACTIVE_CCCD_INDEX_IN_ERROR =
            Pattern.compile("(?i)(?<![A-Z0-9_])" + Pattern.quote(ACTIVE_CCCD_UNIQUE_INDEX) + "(?![A-Z0-9_])");
    private static final String SELECT_COLUMNS = "SELECT id, maBenhNhanFormat, ho, ten, ngaySinh, gioiTinh, dienThoai, "
            + "email, soCCCD, diaChi, soNhaThonPho, noiSinh, ngayCapCCCD, noiCapCCCD, noiLamViec, ngheNghiep, "
            + "danToc, quocTich, maTinh, maHuyen, maXa, nguoiLienHeTen, nguoiLienHeDienThoai, version, "
            + "createdAt, createdBy, updatedAt, updatedBy, isDeleted, deletedAt, deletedBy FROM BenhNhan";

    private final JdbcConnectionProvider connections;
    private final JdbcUnitOfWork unitOfWork;

    public BenhNhanRepositoryImpl(JdbcConnectionProvider connections, JdbcUnitOfWork unitOfWork) {
        this.connections = connections;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Optional<BenhNhan> getById(int id) {
        return queryOne(SELECT_COLUMNS + " WHERE id=? AND isDeleted=0", ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<BenhNhan> getByCCCD(String cccd) {
        if (cccd == null || cccd.isBlank()) return Optional.empty();
        return queryOne(SELECT_COLUMNS + " WHERE soCCCD=? AND isDeleted=0", ps -> ps.setString(1, cccd.trim()));
    }

    @Override
    public Optional<BenhNhan> getByCCCDInScope(String cccd, int facilityId, int departmentId) {
        if (cccd == null || cccd.isBlank() || facilityId <= 0 || departmentId <= 0) return Optional.empty();
        return queryOne(scopedCccdSql(), ps -> {
            ps.setString(1, cccd.trim());
            ps.setInt(2, facilityId);
            ps.setInt(3, departmentId);
        });
    }

    @Override
    public Optional<BenhNhan> getByIdInScope(int patientId, int facilityId, int departmentId) {
        if (patientId <= 0 || facilityId <= 0 || departmentId <= 0) return Optional.empty();
        return queryOne(scopedIdSql(), ps -> {
            ps.setInt(1, patientId);
            ps.setInt(2, facilityId);
            ps.setInt(3, departmentId);
        });
    }

    static String scopedIdSql() {
        return SELECT_COLUMNS + " WHERE id=? AND isDeleted=0 AND EXISTS "
                + "(SELECT 1 FROM LuotKham lk WHERE lk.maBenhNhan=BenhNhan.id "
                + "AND lk.maCoSo=? AND lk.maKhoa=? AND lk.isDeleted=0)";
    }

    static String scopedCccdSql() {
        return SELECT_COLUMNS + " WHERE soCCCD=? AND isDeleted=0 AND EXISTS "
                + "(SELECT 1 FROM LuotKham lk WHERE lk.maBenhNhan=BenhNhan.id "
                + "AND lk.maCoSo=? AND lk.maKhoa=? AND lk.isDeleted=0)";
    }

    @Override
    public Optional<BenhNhan> getByPhone(String phone) {
        if (phone == null || phone.isBlank()) return Optional.empty();
        return queryOne(SELECT_COLUMNS + " WHERE dienThoai=? AND isDeleted=0 ORDER BY id DESC", ps -> ps.setString(1, phone.trim()));
    }

    @Override
    public List<BenhNhan> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        String term = "%" + escapeLike(keyword.trim()) + "%";
        String sql = SELECT_COLUMNS + " WHERE isDeleted=0 AND (maBenhNhanFormat LIKE ? ESCAPE '\\' "
                + "OR ho LIKE ? ESCAPE '\\' OR ten LIKE ? ESCAPE '\\' OR soCCCD LIKE ? ESCAPE '\\' "
                + "OR dienThoai LIKE ? ESCAPE '\\' OR CONCAT(ho, N' ', ten) LIKE ? ESCAPE '\\') ORDER BY ho, ten";
        return queryMany(sql, ps -> {
            for (int i = 1; i <= 6; i++) ps.setString(i, term);
        });
    }

    @Override
    public List<BenhNhan> getAll() {
        return queryMany(SELECT_COLUMNS + " WHERE isDeleted=0 ORDER BY id DESC", ps -> {});
    }

    @Override
    public PagedResult<BenhNhan> getPaged(int pageIndex, int pageSize, ISpecification<BenhNhan> specification) {
        int page = Math.max(1, pageIndex);
        int size = Math.max(1, Math.min(500, pageSize));
        List<BenhNhan> rows = find(specification);
        int from = (int) Math.min((long) (page - 1) * size, rows.size());
        int to = Math.min(from + size, rows.size());
        return new PagedResult<>(rows.subList(from, to), rows.size(), page, size);
    }

    @Override
    public List<BenhNhan> find(ISpecification<BenhNhan> specification) {
        List<BenhNhan> rows = getAll();
        return specification == null ? rows : rows.stream().filter(specification.toPredicate()).toList();
    }

    @Override
    public boolean exists(ISpecification<BenhNhan> specification) { return !find(specification).isEmpty(); }

    @Override
    public int count(ISpecification<BenhNhan> specification) { return find(specification).size(); }

    @Override
    public void add(BenhNhan patient) {
        if (patient.getMaBenhNhanFormat() == null || patient.getMaBenhNhanFormat().isBlank()) {
            patient.setMaBenhNhanFormat("BN-" + LocalDate.now().toString().replace("-", "") + "-"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT));
        }
        String sql = "INSERT INTO BenhNhan (maBenhNhanFormat, ho, ten, ngaySinh, gioiTinh, dienThoai, email, soCCCD, "
                + "diaChi, soNhaThonPho, noiSinh, ngayCapCCCD, noiCapCCCD, noiLamViec, ngheNghiep, danToc, quocTich, "
                + "maTinh, maHuyen, maXa, nguoiLienHeTen, nguoiLienHeDienThoai, username, passwordHash, createdBy) "
                + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bindPatient(ps, patient);
                ps.setString(1, patient.getMaBenhNhanFormat());
                ps.setObject(25, patient.getCreatedBy(), Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Không nhận được ID bệnh nhân sau khi lưu.");
                    patient.setId(rs.getInt(1));
                }
            }
            recordChange();
            return null;
        });
    }

    @Override
    public void update(BenhNhan patient) {
        if (patient.getId() == null) throw new IllegalArgumentException("Không thể cập nhật bệnh nhân chưa có ID.");
        String sql = "UPDATE BenhNhan SET ho=?, ten=?, ngaySinh=?, gioiTinh=?, dienThoai=?, email=?, soCCCD=?, diaChi=?, "
                + "soNhaThonPho=?, noiSinh=?, ngayCapCCCD=?, noiCapCCCD=?, noiLamViec=?, ngheNghiep=?, danToc=?, "
                + "quocTich=?, maTinh=?, maHuyen=?, maXa=?, nguoiLienHeTen=?, nguoiLienHeDienThoai=?, "
                + "updatedAt=SYSDATETIME(), updatedBy=?, version=version+1 WHERE id=? AND version=? AND isDeleted=0";
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bindPatientUpdate(ps, patient);
                ps.setObject(22, patient.getUpdatedBy(), Types.INTEGER);
                ps.setInt(23, patient.getId());
                ps.setLong(24, patient.getVersion() == null ? 0L : patient.getVersion());
                if (ps.executeUpdate() != 1) throw new IllegalStateException("Hồ sơ đã thay đổi hoặc không còn tồn tại.");
            }
            patient.setVersion((patient.getVersion() == null ? 0L : patient.getVersion()) + 1);
            patient.setUpdatedAt(LocalDateTime.now());
            recordChange();
            return null;
        });
    }

    @Override
    public void delete(BenhNhan patient) {
        if (patient.getId() == null) return;
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE BenhNhan SET isDeleted=1, deletedAt=SYSDATETIME(), deletedBy=?, version=version+1 "
                            + "WHERE id=? AND version=? AND isDeleted=0")) {
                ps.setObject(1, patient.getDeletedBy(), Types.INTEGER);
                ps.setInt(2, patient.getId());
                ps.setLong(3, patient.getVersion() == null ? 0L : patient.getVersion());
                if (ps.executeUpdate() != 1) throw new IllegalStateException("Hồ sơ đã thay đổi hoặc không còn tồn tại.");
            }
            patient.setDeleted(true);
            patient.setDeletedAt(LocalDateTime.now());
            recordChange();
            return null;
        });
    }

    @Override
    public IUnitOfWork getUnitOfWork() { return unitOfWork; }

    private Optional<BenhNhan> queryOne(String sql, SqlBinder binder) { return queryMany(sql, binder).stream().findFirst(); }

    private List<BenhNhan> queryMany(String sql, SqlBinder binder) {
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<BenhNhan> rows = new ArrayList<>();
                while (rs.next()) rows.add(mapPatient(rs));
                return rows;
            }
        } catch (SQLException ex) { throw persistenceFailure(ex); }
    }

    private <T> T withConnection(SqlWork<T> work) {
        try (JdbcConnectionProvider.Lease lease = connections.acquire()) { return work.execute(lease.connection()); }
        catch (SQLException ex) { throw persistenceFailure(ex); }
    }

    private void recordChange() { if (connections.isTransactionActive()) unitOfWork.recordChanges(1); }

    private static BenhNhan mapPatient(ResultSet rs) throws SQLException {
        BenhNhan patient = new BenhNhan();
        patient.setId(rs.getInt("id"));
        patient.setMaBenhNhanFormat(rs.getString("maBenhNhanFormat"));
        patient.setHo(rs.getString("ho"));
        patient.setTen(rs.getString("ten"));
        patient.setNgaySinh(localDate(rs, "ngaySinh"));
        String gender = rs.getString("gioiTinh");
        if (gender != null) {
            try { patient.setGioiTinh(GioiTinh.valueOf(gender.trim().toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException ignored) { patient.setGioiTinh(GioiTinh.O); }
        }
        patient.setDienThoai(rs.getString("dienThoai"));
        patient.setEmail(rs.getString("email"));
        patient.setSoCCCD(rs.getString("soCCCD"));
        patient.setDiaChi(rs.getString("diaChi"));
        patient.setSoNhaThonPho(rs.getString("soNhaThonPho"));
        patient.setNoiSinh(rs.getString("noiSinh"));
        patient.setNgayCapCCCD(localDate(rs, "ngayCapCCCD"));
        patient.setNoiCapCCCD(rs.getString("noiCapCCCD"));
        patient.setNoiLamViec(rs.getString("noiLamViec"));
        patient.setNgheNghiep(rs.getString("ngheNghiep"));
        patient.setDanToc(rs.getString("danToc"));
        patient.setQuocTich(rs.getString("quocTich"));
        patient.setMaTinh(rs.getString("maTinh"));
        patient.setMaHuyen(rs.getString("maHuyen"));
        patient.setMaXa(rs.getString("maXa"));
        patient.setNguoiLienHeTen(rs.getString("nguoiLienHeTen"));
        patient.setNguoiLienHeDienThoai(rs.getString("nguoiLienHeDienThoai"));
        patient.setVersion(rs.getLong("version"));
        patient.setCreatedAt(localDateTime(rs, "createdAt"));
        patient.setCreatedBy((Integer) rs.getObject("createdBy"));
        patient.setUpdatedAt(localDateTime(rs, "updatedAt"));
        patient.setUpdatedBy((Integer) rs.getObject("updatedBy"));
        patient.setDeleted(rs.getBoolean("isDeleted"));
        patient.setDeletedAt(localDateTime(rs, "deletedAt"));
        patient.setDeletedBy((Integer) rs.getObject("deletedBy"));
        return patient;
    }

    private static void bindPatient(PreparedStatement ps, BenhNhan p) throws SQLException {
        ps.setString(2, p.getHo());
        ps.setString(3, p.getTen());
        ps.setObject(4, p.getNgaySinh());
        ps.setString(5, p.getGioiTinh() == null ? null : p.getGioiTinh().name());
        ps.setString(6, blankToNull(p.getDienThoai()));
        ps.setString(7, p.getEmail());
        ps.setString(8, blankToNull(p.getSoCCCD()));
        ps.setString(9, p.getDiaChi());
        ps.setString(10, p.getSoNhaThonPho());
        ps.setString(11, p.getNoiSinh());
        ps.setObject(12, p.getNgayCapCCCD());
        ps.setString(13, p.getNoiCapCCCD());
        ps.setString(14, p.getNoiLamViec());
        ps.setString(15, p.getNgheNghiep());
        ps.setString(16, p.getDanToc());
        ps.setString(17, p.getQuocTich());
        ps.setString(18, p.getMaTinh());
        ps.setString(19, p.getMaHuyen());
        ps.setString(20, p.getMaXa());
        ps.setString(21, p.getNguoiLienHeTen());
        ps.setString(22, p.getNguoiLienHeDienThoai());
        ps.setString(23, p.getUsername());
        ps.setString(24, p.getPasswordHash());
    }

    private static void bindPatientUpdate(PreparedStatement ps, BenhNhan p) throws SQLException {
        ps.setString(1, p.getHo()); ps.setString(2, p.getTen()); ps.setObject(3, p.getNgaySinh());
        ps.setString(4, p.getGioiTinh() == null ? null : p.getGioiTinh().name());
        ps.setString(5, blankToNull(p.getDienThoai())); ps.setString(6, p.getEmail()); ps.setString(7, blankToNull(p.getSoCCCD()));
        ps.setString(8, p.getDiaChi()); ps.setString(9, p.getSoNhaThonPho()); ps.setString(10, p.getNoiSinh());
        ps.setObject(11, p.getNgayCapCCCD()); ps.setString(12, p.getNoiCapCCCD()); ps.setString(13, p.getNoiLamViec());
        ps.setString(14, p.getNgheNghiep()); ps.setString(15, p.getDanToc()); ps.setString(16, p.getQuocTich());
        ps.setString(17, p.getMaTinh()); ps.setString(18, p.getMaHuyen()); ps.setString(19, p.getMaXa());
        ps.setString(20, p.getNguoiLienHeTen()); ps.setString(21, p.getNguoiLienHeDienThoai());
    }

    private static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column); return value == null ? null : value.toLocalDate();
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column); return value == null ? null : value.toLocalDateTime();
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private static String escapeLike(String value) { return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\["); }

    static RuntimeException persistenceFailure(SQLException ex) {
        if (isActiveCccdDuplicate(ex)) return new PatientIdentityConflictException();
        return new IllegalStateException("Lỗi truy cập dữ liệu bệnh nhân.", ex);
    }

    private static boolean isActiveCccdDuplicate(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            if (isActiveCccdUniqueViolation(current)) return true;
            for (Throwable cause = current.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sqlCause && isActiveCccdUniqueViolation(sqlCause)) return true;
            }
        }
        return false;
    }

    private static boolean isActiveCccdUniqueViolation(SQLException exception) {
        int errorCode = exception.getErrorCode();
        if (errorCode != 2601 && errorCode != 2627) return false;

        // SQL Server includes the unique index/constraint name in duplicate-key errors.
        // Inspect it only to classify the error; never copy the raw message, which may
        // contain the duplicated CCCD, into an application-facing exception.
        String message = exception.getMessage();
        return message != null && ACTIVE_CCCD_INDEX_IN_ERROR.matcher(message).find();
    }

    @FunctionalInterface private interface SqlBinder { void bind(PreparedStatement statement) throws SQLException; }
    @FunctionalInterface private interface SqlWork<T> { T execute(Connection connection) throws SQLException; }
}
