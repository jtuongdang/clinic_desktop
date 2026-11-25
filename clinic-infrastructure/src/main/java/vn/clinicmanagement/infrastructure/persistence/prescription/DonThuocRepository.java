package vn.clinicmanagement.infrastructure.persistence.prescription;

import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.ITieuChuan;
import vn.clinicmanagement.domain.repositories.PhanTrangKetQua;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Triển khai JDBC cho đơn thuốc (DonThuoc) và chi tiết đơn thuốc (ChiTietDonThuoc).
 */
public final class DonThuocRepository implements IDonThuocRepository {

    private static final String HEADER_SELECT =
            "SELECT id, maLuotKham, maBacSi, maCoSo, maKhoXuat, ngayKe, trangThai, ghiChu, loiDanDonThuoc, " +
            "ngayCapThuoc, maNguoiCapThuoc, version, createdAt, createdBy, updatedAt, updatedBy " +
            "FROM DonThuoc WHERE isDeleted = 0";

    private static final String DETAIL_SELECT =
            "SELECT c.id, c.maDonThuoc, c.maItem, c.soLuongKe, c.soLuongDaXuat, c.lieuDung, c.tanSuat, " +
            "c.soNgay, c.huongDan, c.donGiaKe, c.duongDung, c.sang, c.trua, c.chieu, c.toi, " +
            "i.maItem AS itemCode, i.tenItem AS itemName, i.donViCoBan AS itemUnit, " +
            "i.quyCachDongGoi AS itemPack, i.giaBanMacDinh AS itemPrice, i.loaiItem AS itemType " +
            "FROM ChiTietDonThuoc c " +
            "LEFT JOIN Item i ON i.id = c.maItem " +
            "WHERE c.isDeleted = 0 AND c.maDonThuoc = ? ORDER BY c.id ASC";

    private final JdbcKetNoiNhaCungCap connections;
    private final JdbcUnitOfWork unitOfWork;

    public DonThuocRepository(JdbcKetNoiNhaCungCap connections) {
        this(connections, new JdbcUnitOfWork());
    }

    public DonThuocRepository(JdbcKetNoiNhaCungCap connections, JdbcUnitOfWork unitOfWork) {
        this.connections = Objects.requireNonNull(connections, "connections");
        this.unitOfWork = unitOfWork != null ? unitOfWork : new JdbcUnitOfWork();
    }

    @Override
    public List<DonThuoc> getByLuotKhamId(int maLuotKham) {
        if (maLuotKham <= 0) return List.of();
        String sql = HEADER_SELECT + " AND maLuotKham = ? ORDER BY ngayKe DESC, id DESC";
        List<DonThuoc> list = new ArrayList<>();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, maLuotKham);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DonThuoc rx = mapHeader(rs);
                    rx.setChiTietDonThuocs(loadDetails(lease, rx.getId()));
                    list.add(rx);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh sách đơn thuốc theo lượt khám.", ex);
        }
        return list;
    }

    @Override
    public Optional<DonThuoc> getById(int id) {
        if (id <= 0) return Optional.empty();
        String sql = HEADER_SELECT + " AND id = ?";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DonThuoc rx = mapHeader(rs);
                    rx.setChiTietDonThuocs(loadDetails(lease, rx.getId()));
                    return Optional.of(rx);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải đơn thuốc #" + id, ex);
        }
        return Optional.empty();
    }

    @Override
    public void add(DonThuoc entity) {
        if (entity == null) return;
        String headerSql = "INSERT INTO DonThuoc (maLuotKham, maBacSi, maCoSo, maKhoXuat, ngayKe, trangThai, ghiChu, loiDanDonThuoc, createdBy) "
                + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire()) {
            try (PreparedStatement ps = lease.connection().prepareStatement(headerSql)) {
                ps.setInt(1, entity.getMaLuotKham());
                ps.setInt(2, entity.getMaBacSi());
                ps.setInt(3, entity.getMaCoSo());
                ps.setObject(4, entity.getMaKhoXuat(), Types.INTEGER);
                LocalDateTime ngayKe = entity.getNgayKe() != null ? entity.getNgayKe() : LocalDateTime.now();
                ps.setTimestamp(5, Timestamp.valueOf(ngayKe));
                ps.setString(6, entity.getTrangThai() != null ? entity.getTrangThai().name() : TrangThaiDonThuoc.TAM.name());
                ps.setString(7, entity.getGhiChu());
                ps.setString(8, entity.getLoiDanDonThuoc());
                ps.setObject(9, entity.getMaBacSi() > 0 ? entity.getMaBacSi() : null, Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        entity.setId(rs.getInt(1));
                    }
                }
            }

            if (entity.getId() != null && entity.getChiTietDonThuocs() != null) {
                String detailSql = "INSERT INTO ChiTietDonThuoc "
                        + "(maDonThuoc, maItem, soLuongKe, soLuongDaXuat, lieuDung, tanSuat, soNgay, huongDan, donGiaKe, duongDung, sang, trua, chieu, toi) "
                        + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                for (ChiTietDonThuoc detail : entity.getChiTietDonThuocs()) {
                    if (detail == null) continue;
                    detail.setMaDonThuoc(entity.getId());
                    try (PreparedStatement dps = lease.connection().prepareStatement(detailSql)) {
                        dps.setInt(1, entity.getId());
                        dps.setInt(2, detail.getMaItem());
                        dps.setBigDecimal(3, detail.getSoLuongKe() != null ? detail.getSoLuongKe() : BigDecimal.ONE);
                        dps.setBigDecimal(4, detail.getSoLuongDaXuat() != null ? detail.getSoLuongDaXuat() : BigDecimal.ZERO);
                        dps.setString(5, detail.getLieuDung());
                        dps.setString(6, detail.getTanSuat());
                        dps.setObject(7, detail.getSoNgay(), Types.INTEGER);
                        dps.setString(8, detail.getHuongDan());
                        dps.setBigDecimal(9, detail.getDonGiaKe() != null ? detail.getDonGiaKe() : BigDecimal.ZERO);
                        dps.setString(10, detail.getDuongDung());
                        dps.setBigDecimal(11, detail.getSang() != null ? detail.getSang() : BigDecimal.ZERO);
                        dps.setBigDecimal(12, detail.getTrua() != null ? detail.getTrua() : BigDecimal.ZERO);
                        dps.setBigDecimal(13, detail.getChieu() != null ? detail.getChieu() : BigDecimal.ZERO);
                        dps.setBigDecimal(14, detail.getToi() != null ? detail.getToi() : BigDecimal.ZERO);
                        try (ResultSet drs = dps.executeQuery()) {
                            if (drs.next()) {
                                detail.setId(drs.getInt(1));
                            }
                        }
                    }
                }
            }
            recordChange();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể lưu đơn thuốc vào CSDL.", ex);
        }
    }

    @Override
    public void update(DonThuoc entity) {
        if (entity == null || entity.getId() == null || entity.getId() <= 0) return;
        String sql = "UPDATE DonThuoc SET trangThai = ?, ghiChu = ?, loiDanDonThuoc = ?, ngayCapThuoc = ?, "
                + "maNguoiCapThuoc = ?, updatedAt = SYSDATETIME(), updatedBy = ?, version = version + 1 "
                + "WHERE id = ? AND isDeleted = 0";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setString(1, entity.getTrangThai() != null ? entity.getTrangThai().name() : TrangThaiDonThuoc.TAM.name());
            ps.setString(2, entity.getGhiChu());
            ps.setString(3, entity.getLoiDanDonThuoc());
            ps.setTimestamp(4, entity.getNgayCapThuoc() != null ? Timestamp.valueOf(entity.getNgayCapThuoc()) : null);
            ps.setObject(5, entity.getMaNguoiCapThuoc(), Types.INTEGER);
            ps.setObject(6, entity.getUpdatedBy(), Types.INTEGER);
            ps.setInt(7, entity.getId());
            ps.executeUpdate();
            if (entity.getChiTietDonThuocs() != null && !entity.getChiTietDonThuocs().isEmpty()) {
                String detailSql = "UPDATE ChiTietDonThuoc SET soLuongDaXuat = ?, updatedAt = SYSDATETIME(), "
                        + "updatedBy = ?, version = version + 1 WHERE id = ? AND maDonThuoc = ? AND isDeleted = 0";
                try (PreparedStatement dps = lease.connection().prepareStatement(detailSql)) {
                    for (ChiTietDonThuoc detail : entity.getChiTietDonThuocs()) {
                        if (detail == null || detail.getId() == null || detail.getId() <= 0) continue;
                        dps.setBigDecimal(1, detail.getSoLuongDaXuat() != null ? detail.getSoLuongDaXuat() : BigDecimal.ZERO);
                        dps.setObject(2, entity.getUpdatedBy(), Types.INTEGER);
                        dps.setInt(3, detail.getId());
                        dps.setInt(4, entity.getId());
                        dps.addBatch();
                    }
                    dps.executeBatch();
                }
            }
            recordChange();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể cập nhật đơn thuốc #" + entity.getId(), ex);
        }
    }

    @Override public List<DonThuoc> getAll() { return List.of(); }
    @Override public PhanTrangKetQua<DonThuoc> getPaged(int p, int s, ITieuChuan<DonThuoc> sp) { return new PhanTrangKetQua<>(List.of(), 0, p, s); }
    @Override public List<DonThuoc> find(ITieuChuan<DonThuoc> sp) { return List.of(); }
    @Override public boolean exists(ITieuChuan<DonThuoc> sp) { return false; }
    @Override public int count(ITieuChuan<DonThuoc> sp) { return 0; }
    @Override public void delete(DonThuoc entity) {}
    @Override public IUnitOfWork getUnitOfWork() { return unitOfWork; }

    private List<ChiTietDonThuoc> loadDetails(JdbcKetNoiNhaCungCap.Lease lease, int maDonThuoc) throws SQLException {
        List<ChiTietDonThuoc> details = new ArrayList<>();
        try (PreparedStatement ps = lease.connection().prepareStatement(DETAIL_SELECT)) {
            ps.setInt(1, maDonThuoc);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChiTietDonThuoc d = new ChiTietDonThuoc();
                    d.setId(rs.getInt("id"));
                    d.setMaDonThuoc(rs.getInt("maDonThuoc"));
                    d.setMaItem(rs.getInt("maItem"));
                    d.setSoLuongKe(rs.getBigDecimal("soLuongKe") != null ? rs.getBigDecimal("soLuongKe") : BigDecimal.ZERO);
                    d.setSoLuongDaXuat(rs.getBigDecimal("soLuongDaXuat") != null ? rs.getBigDecimal("soLuongDaXuat") : BigDecimal.ZERO);
                    d.setLieuDung(rs.getString("lieuDung"));
                    d.setTanSuat(rs.getString("tanSuat"));
                    d.setSoNgay((Integer) rs.getObject("soNgay"));
                    d.setHuongDan(rs.getString("huongDan"));
                    d.setDonGiaKe(rs.getBigDecimal("donGiaKe") != null ? rs.getBigDecimal("donGiaKe") : BigDecimal.ZERO);
                    d.setDuongDung(rs.getString("duongDung"));
                    d.setSang(rs.getBigDecimal("sang") != null ? rs.getBigDecimal("sang") : BigDecimal.ZERO);
                    d.setTrua(rs.getBigDecimal("trua") != null ? rs.getBigDecimal("trua") : BigDecimal.ZERO);
                    d.setChieu(rs.getBigDecimal("chieu") != null ? rs.getBigDecimal("chieu") : BigDecimal.ZERO);
                    d.setToi(rs.getBigDecimal("toi") != null ? rs.getBigDecimal("toi") : BigDecimal.ZERO);

                    String itemName = rs.getString("itemName");
                    if (itemName != null) {
                        MatHang item = new MatHang();
                        item.setId(d.getMaItem());
                        item.setMaItem(rs.getString("itemCode"));
                        item.setTenItem(itemName);
                        item.setDonViCoBan(rs.getString("itemUnit"));
                        item.setQuyCachDongGoi(rs.getString("itemPack"));
                        item.setGiaBanMacDinh(rs.getBigDecimal("itemPrice") != null ? rs.getBigDecimal("itemPrice") : BigDecimal.ZERO);
                        item.setLoaiItem("VAT_TU".equalsIgnoreCase(rs.getString("itemType")) ? LoaiMatHang.VAT_TU : LoaiMatHang.THUOC);
                        d.setItem(item);
                    }
                    details.add(d);
                }
            }
        }
        return details;
    }

    private static DonThuoc mapHeader(ResultSet rs) throws SQLException {
        DonThuoc rx = new DonThuoc();
        rx.setId(rs.getInt("id"));
        rx.setMaLuotKham(rs.getInt("maLuotKham"));
        rx.setMaBacSi(rs.getInt("maBacSi"));
        rx.setMaCoSo(rs.getInt("maCoSo"));
        rx.setMaKhoXuat((Integer) rs.getObject("maKhoXuat"));
        Timestamp ngayKe = rs.getTimestamp("ngayKe");
        if (ngayKe != null) rx.setNgayKe(ngayKe.toLocalDateTime());
        String statusStr = rs.getString("trangThai");
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                rx.setTrangThai(TrangThaiDonThuoc.valueOf(statusStr.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                rx.setTrangThai(TrangThaiDonThuoc.TAM);
            }
        }
        rx.setGhiChu(rs.getString("ghiChu"));
        rx.setLoiDanDonThuoc(rs.getString("loiDanDonThuoc"));
        Timestamp ngayCap = rs.getTimestamp("ngayCapThuoc");
        if (ngayCap != null) rx.setNgayCapThuoc(ngayCap.toLocalDateTime());
        rx.setMaNguoiCapThuoc((Integer) rs.getObject("maNguoiCapThuoc"));
        rx.setVersion(rs.getLong("version"));
        Timestamp createdAt = rs.getTimestamp("createdAt");
        if (createdAt != null) rx.setCreatedAt(createdAt.toLocalDateTime());
        rx.setCreatedBy((Integer) rs.getObject("createdBy"));
        Timestamp updatedAt = rs.getTimestamp("updatedAt");
        if (updatedAt != null) rx.setUpdatedAt(updatedAt.toLocalDateTime());
        rx.setUpdatedBy((Integer) rs.getObject("updatedBy"));
        return rx;
    }

    private void recordChange() {
        if (connections.isTransactionActive()) {
            unitOfWork.recordChanges(1);
        }
    }
}
