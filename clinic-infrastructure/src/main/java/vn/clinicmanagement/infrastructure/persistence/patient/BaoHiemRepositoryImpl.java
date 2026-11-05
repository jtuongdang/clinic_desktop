package vn.clinicmanagement.infrastructure.persistence.patient;

import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
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

/** JDBC mapper for the complete insurance-card model, including the ten reception/BHYT fields. */
public final class BaoHiemRepositoryImpl implements IBaoHiemRepository {
    private static final String SELECT_COLUMNS = "SELECT id, maBenhNhan, loaiBaoHiem, soBaoHiem, ngayBatDau, ngayHetHan, "
            + "mucHuong, maNoiSong, maNoiDangKyKCB, noiDangKyKCB, diaChiThe, dungTuyen, coGiayKhaiSinh, khongGiuThe, "
            + "mienCungChiTra, ngayMienCungChiTra, ngayDu5NamLienTuc, version, createdAt, createdBy, updatedAt, updatedBy, "
            + "isDeleted, deletedAt, deletedBy FROM BaoHiem";

    private final JdbcConnectionProvider connections;
    private final JdbcUnitOfWork unitOfWork;

    public BaoHiemRepositoryImpl(JdbcConnectionProvider connections, JdbcUnitOfWork unitOfWork) {
        this.connections = connections;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Optional<BaoHiem> getById(int id) {
        return queryOne(SELECT_COLUMNS + " WHERE id=? AND isDeleted=0", ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<BaoHiem> getLatestByPatientId(int patientId) {
        return queryOne(SELECT_COLUMNS + " WHERE maBenhNhan=? AND isDeleted=0 ORDER BY ngayHetHan DESC, id DESC",
                ps -> ps.setInt(1, patientId));
    }

    @Override
    public List<BaoHiem> getByPatientId(int patientId) {
        return queryMany(SELECT_COLUMNS + " WHERE maBenhNhan=? AND isDeleted=0 ORDER BY ngayHetHan DESC, id DESC",
                ps -> ps.setInt(1, patientId));
    }

    @Override
    public void add(BaoHiem card) {
        String sql = "INSERT INTO BaoHiem (maBenhNhan, loaiBaoHiem, soBaoHiem, ngayBatDau, ngayHetHan, mucHuong, "
                + "maNoiSong, maNoiDangKyKCB, noiDangKyKCB, diaChiThe, dungTuyen, coGiayKhaiSinh, khongGiuThe, "
                + "mienCungChiTra, ngayMienCungChiTra, ngayDu5NamLienTuc, createdBy) "
                + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bindCard(ps, card);
                ps.setObject(17, card.getCreatedBy(), Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Không nhận được ID thẻ BHYT sau khi lưu.");
                    card.setId(rs.getInt(1));
                }
            }
            recordChange();
            return null;
        });
    }

    @Override
    public void update(BaoHiem card) {
        if (card.getId() == null) throw new IllegalArgumentException("Không thể cập nhật thẻ BHYT chưa có ID.");
        String sql = "UPDATE BaoHiem SET maBenhNhan=?, loaiBaoHiem=?, soBaoHiem=?, ngayBatDau=?, ngayHetHan=?, mucHuong=?, "
                + "maNoiSong=?, maNoiDangKyKCB=?, noiDangKyKCB=?, diaChiThe=?, dungTuyen=?, coGiayKhaiSinh=?, khongGiuThe=?, "
                + "mienCungChiTra=?, ngayMienCungChiTra=?, ngayDu5NamLienTuc=?, updatedAt=SYSDATETIME(), updatedBy=?, "
                + "version=version+1 WHERE id=? AND version=? AND isDeleted=0";
        withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bindCard(ps, card);
                ps.setObject(17, card.getUpdatedBy(), Types.INTEGER);
                ps.setInt(18, card.getId());
                ps.setLong(19, card.getVersion() == null ? 0L : card.getVersion());
                if (ps.executeUpdate() != 1) throw new IllegalStateException("Thẻ BHYT đã thay đổi hoặc không còn tồn tại.");
            }
            card.setVersion((card.getVersion() == null ? 0L : card.getVersion()) + 1);
            card.setUpdatedAt(LocalDateTime.now());
            recordChange();
            return null;
        });
    }

    private Optional<BaoHiem> queryOne(String sql, SqlBinder binder) { return queryMany(sql, binder).stream().findFirst(); }

    private List<BaoHiem> queryMany(String sql, SqlBinder binder) {
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<BaoHiem> cards = new ArrayList<>();
                while (rs.next()) cards.add(mapCard(rs));
                return cards;
            }
        } catch (SQLException ex) { throw new IllegalStateException("Lỗi truy cập dữ liệu thẻ BHYT.", ex); }
    }

    private <T> T withConnection(SqlWork<T> work) {
        try (JdbcConnectionProvider.Lease lease = connections.acquire()) { return work.execute(lease.connection()); }
        catch (SQLException ex) { throw new IllegalStateException("Lỗi truy cập dữ liệu thẻ BHYT.", ex); }
    }

    private void recordChange() { if (connections.isTransactionActive()) unitOfWork.recordChanges(1); }

    private static BaoHiem mapCard(ResultSet rs) throws SQLException {
        BaoHiem card = new BaoHiem();
        card.setId(rs.getInt("id"));
        card.setMaBenhNhan(rs.getInt("maBenhNhan"));
        String type = rs.getString("loaiBaoHiem");
        if (type != null) {
            try { card.setLoaiBaoHiem(LoaiBaoHiem.valueOf(type.trim().toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException ignored) { card.setLoaiBaoHiem(LoaiBaoHiem.KHAC); }
        }
        card.setSoBaoHiem(rs.getString("soBaoHiem"));
        card.setNgayBatDau(localDate(rs, "ngayBatDau"));
        card.setNgayHetHan(localDate(rs, "ngayHetHan"));
        card.setMucHuong(rs.getInt("mucHuong"));
        card.setMaNoiSong(rs.getString("maNoiSong"));
        card.setMaNoiDangKyKCB(rs.getString("maNoiDangKyKCB"));
        card.setNoiDangKyKCB(rs.getString("noiDangKyKCB"));
        card.setDiaChiThe(rs.getString("diaChiThe"));
        card.setDungTuyen(rs.getBoolean("dungTuyen"));
        card.setCoGiayKhaiSinh(rs.getBoolean("coGiayKhaiSinh"));
        card.setKhongGiuThe(rs.getBoolean("khongGiuThe"));
        card.setMienCungChiTra(rs.getBoolean("mienCungChiTra"));
        card.setNgayMienCungChiTra(localDate(rs, "ngayMienCungChiTra"));
        card.setNgayDu5NamLienTuc(localDate(rs, "ngayDu5NamLienTuc"));
        card.setVersion(rs.getLong("version"));
        card.setCreatedAt(localDateTime(rs, "createdAt"));
        card.setCreatedBy((Integer) rs.getObject("createdBy"));
        card.setUpdatedAt(localDateTime(rs, "updatedAt"));
        card.setUpdatedBy((Integer) rs.getObject("updatedBy"));
        card.setDeleted(rs.getBoolean("isDeleted"));
        card.setDeletedAt(localDateTime(rs, "deletedAt"));
        card.setDeletedBy((Integer) rs.getObject("deletedBy"));
        return card;
    }

    private static void bindCard(PreparedStatement ps, BaoHiem card) throws SQLException {
        ps.setInt(1, card.getMaBenhNhan());
        ps.setString(2, card.getLoaiBaoHiem() == null ? LoaiBaoHiem.BHYT.name() : card.getLoaiBaoHiem().name());
        ps.setString(3, card.getSoBaoHiem());
        ps.setObject(4, card.getNgayBatDau());
        ps.setObject(5, card.getNgayHetHan());
        ps.setInt(6, card.getMucHuong());
        ps.setString(7, card.getMaNoiSong());
        ps.setString(8, card.getMaNoiDangKyKCB());
        ps.setString(9, card.getNoiDangKyKCB());
        ps.setString(10, card.getDiaChiThe());
        ps.setBoolean(11, card.isDungTuyen());
        ps.setBoolean(12, card.isCoGiayKhaiSinh());
        ps.setBoolean(13, card.isKhongGiuThe());
        ps.setBoolean(14, card.isMienCungChiTra());
        ps.setObject(15, card.getNgayMienCungChiTra());
        ps.setObject(16, card.getNgayDu5NamLienTuc());
    }

    private static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column); return value == null ? null : value.toLocalDate();
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column); return value == null ? null : value.toLocalDateTime();
    }

    @FunctionalInterface private interface SqlBinder { void bind(PreparedStatement statement) throws SQLException; }
    @FunctionalInterface private interface SqlWork<T> { T execute(Connection connection) throws SQLException; }
}
