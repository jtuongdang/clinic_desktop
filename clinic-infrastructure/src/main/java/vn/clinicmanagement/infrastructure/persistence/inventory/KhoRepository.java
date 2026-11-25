package vn.clinicmanagement.infrastructure.persistence.inventory;

import vn.clinicmanagement.domain.common.enums.LoaiKho;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.common.enums.LoaiXuatKho;
import vn.clinicmanagement.domain.common.enums.TrangThaiKho;
import vn.clinicmanagement.domain.common.enums.TrangThaiPhieuNhap;
import vn.clinicmanagement.domain.common.enums.TrangThaiXuatNoiBo;
import vn.clinicmanagement.domain.inventory.ChiTietNhapKho;
import vn.clinicmanagement.domain.inventory.ChiTietXuatKho;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.KiemKe;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IKhoRepository;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Triển khai JDBC cho danh mục thuốc / vật tư (Item), kho (Kho) và tồn kho (TonKho).
 */
public final class KhoRepository implements IKhoRepository {

    private static final String ITEM_SELECT =
            "SELECT id, maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, " +
            "giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong " +
            "FROM Item WHERE isDeleted = 0";

    private static final String TONKHO_SELECT =
            "SELECT id, maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan " +
            "FROM TonKho WHERE isDeleted = 0";

    private final JdbcKetNoiNhaCungCap connections;
    private final JdbcUnitOfWork unitOfWork;

    public KhoRepository(JdbcKetNoiNhaCungCap connections) {
        this(connections, new JdbcUnitOfWork());
    }

    public KhoRepository(JdbcKetNoiNhaCungCap connections, JdbcUnitOfWork unitOfWork) {
        this.connections = Objects.requireNonNull(connections, "connections");
        this.unitOfWork = unitOfWork != null ? unitOfWork : new JdbcUnitOfWork();
    }

    @Override
    public Optional<MatHang> getItemById(int id) {
        if (id <= 0) return Optional.empty();
        String sql = ITEM_SELECT + " AND id = ?";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MatHang item = mapMatHang(rs);
                    attachTonKho(List.of(item));
                    return Optional.of(item);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải thông tin mặt hàng #" + id, ex);
        }
        return Optional.empty();
    }

    @Override
    public Optional<MatHang> getItemByCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        String sql = ITEM_SELECT + " AND maItem = ?";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    MatHang item = mapMatHang(rs);
                    attachTonKho(List.of(item));
                    return Optional.of(item);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tìm mặt hàng theo mã: " + code, ex);
        }
        return Optional.empty();
    }

    @Override
    public MatHang saveItem(MatHang item) {
        if (item == null) return null;
        if (item.getId() == null || item.getId() <= 0) {
            String sql = "INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, "
                    + "giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, createdBy) "
                    + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
                 PreparedStatement ps = lease.connection().prepareStatement(sql)) {
                bindItemParameters(ps, item);
                ps.setObject(17, item.getCreatedBy(), Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        item.setId(rs.getInt(1));
                    }
                }
                recordChange();
                return item;
            } catch (SQLException ex) {
                throw new IllegalStateException("Không thể thêm mới mặt hàng.", ex);
            }
        }
        String sql = "UPDATE Item SET maItem=?, tenItem=?, loaiItem=?, hoatChat=?, hamLuong=?, donViCoBan=?, quyCachDongGoi=?, "
                + "giaNhapMacDinh=?, giaBanMacDinh=?, tonToiThieu=?, tonToiDa=?, dangBaoChe=?, nuocSanXuat=?, hangSanXuat=?, soDangKy=?, hoatDong=?, "
                + "updatedAt=SYSDATETIME(), updatedBy=?, version=version+1 WHERE id=? AND isDeleted=0";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            bindItemParameters(ps, item);
            ps.setObject(17, item.getUpdatedBy(), Types.INTEGER);
            ps.setInt(18, item.getId());
            ps.executeUpdate();
            recordChange();
            return item;
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể cập nhật mặt hàng #" + item.getId(), ex);
        }
    }

    @Override
    public List<MatHang> searchItems(String keyword, Boolean hoatDong) {
        StringBuilder sql = new StringBuilder(ITEM_SELECT);
        List<Object> params = new ArrayList<>();
        if (hoatDong != null) {
            sql.append(" AND hoatDong = ?");
            params.add(hoatDong ? 1 : 0);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(tenItem) LIKE ? OR LOWER(maItem) LIKE ? OR LOWER(COALESCE(hoatChat,'')) LIKE ?)");
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(" ORDER BY tenItem ASC");

        List<MatHang> items = new ArrayList<>();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapMatHang(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể truy vấn danh mục thuốc/vật tư.", ex);
        }
        attachTonKho(items);
        return items;
    }

    @Override
    public Optional<Kho> getKhoById(int id) {
        if (id <= 0) return Optional.empty();
        String sql = "SELECT id, maCoSo, tenKho, loaiKho, trangThai, maNguoiPhuTrach FROM Kho WHERE id = ? AND isDeleted = 0";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Kho kho = new Kho();
                    kho.setId(rs.getInt("id"));
                    kho.setMaCoSo(rs.getInt("maCoSo"));
                    kho.setTenKho(rs.getString("tenKho"));
                    kho.setLoaiKho(mapLoaiKho(rs.getString("loaiKho")));
                    kho.setTrangThai(mapTrangThaiKho(rs.getString("trangThai")));
                    kho.setMaNguoiPhuTrach((Integer) rs.getObject("maNguoiPhuTrach"));
                    return Optional.of(kho);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải thông tin kho #" + id, ex);
        }
        return Optional.empty();
    }

    @Override
    public List<TonKho> getAvailableBatches(int maKho, int maItem) {
        String sql = TONKHO_SELECT + " AND maKho = ? AND maItem = ? AND soLuongTon > 0 ORDER BY hanSuDung ASC, id ASC";
        List<TonKho> list = new ArrayList<>();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, maKho);
            ps.setInt(2, maItem);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTonKho(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải tồn kho theo lô.", ex);
        }
        return list;
    }

    @Override
    public List<TonKho> getBatchesByKho(int maKho) {
        String sql = TONKHO_SELECT + " AND maKho = ? ORDER BY maItem ASC, hanSuDung ASC";
        List<TonKho> list = new ArrayList<>();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, maKho);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTonKho(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh sách tồn kho của kho #" + maKho, ex);
        }
        return list;
    }

    @Override
    public Optional<TonKho> getBatchById(int id) {
        if (id <= 0) return Optional.empty();
        String sql = TONKHO_SELECT + " AND id = ?";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTonKho(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải thông tin lô tồn kho #" + id, ex);
        }
        return Optional.empty();
    }

    @Override
    public TonKho saveTonKho(TonKho tonKho) {
        if (tonKho == null) return null;
        if (tonKho.getId() == null || tonKho.getId() <= 0) {
            String sql = "INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, createdBy) "
                    + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
                 PreparedStatement ps = lease.connection().prepareStatement(sql)) {
                ps.setInt(1, tonKho.getMaKho());
                ps.setInt(2, tonKho.getMaItem());
                ps.setString(3, tonKho.getSoLo());
                ps.setDate(4, tonKho.getHanSuDung() != null ? Date.valueOf(tonKho.getHanSuDung()) : null);
                ps.setBigDecimal(5, tonKho.getSoLuongTon() != null ? tonKho.getSoLuongTon() : BigDecimal.ZERO);
                ps.setBigDecimal(6, tonKho.getGiaNhap() != null ? tonKho.getGiaNhap() : BigDecimal.ZERO);
                ps.setBigDecimal(7, tonKho.getGiaBan() != null ? tonKho.getGiaBan() : BigDecimal.ZERO);
                ps.setObject(8, tonKho.getCreatedBy(), Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) tonKho.setId(rs.getInt(1));
                }
                recordChange();
                return tonKho;
            } catch (SQLException ex) {
                throw new IllegalStateException("Không thể lưu tồn kho.", ex);
            }
        }
        String sql = "UPDATE TonKho SET soLuongTon=?, giaNhap=?, giaBan=?, updatedAt=SYSDATETIME(), updatedBy=?, version=version+1 "
                + "WHERE id=? AND isDeleted=0";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setBigDecimal(1, tonKho.getSoLuongTon() != null ? tonKho.getSoLuongTon() : BigDecimal.ZERO);
            ps.setBigDecimal(2, tonKho.getGiaNhap() != null ? tonKho.getGiaNhap() : BigDecimal.ZERO);
            ps.setBigDecimal(3, tonKho.getGiaBan() != null ? tonKho.getGiaBan() : BigDecimal.ZERO);
            ps.setObject(4, tonKho.getUpdatedBy(), Types.INTEGER);
            ps.setInt(5, tonKho.getId());
            ps.executeUpdate();
            recordChange();
            return tonKho;
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể cập nhật tồn kho #" + tonKho.getId(), ex);
        }
    }

    @Override
    public PhieuNhapKho savePhieuNhapKho(PhieuNhapKho p) {
        if (p == null) return null;
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire()) {
            LocalDateTime ngayNhap = p.getNgayNhap() != null ? p.getNgayNhap() : LocalDateTime.now();
            String trangThai = p.getTrangThai() != null ? p.getTrangThai().name() : TrangThaiPhieuNhap.TAM.name();
            BigDecimal tongTien = p.getTongTien() != null ? p.getTongTien() : BigDecimal.ZERO;
            if (p.getId() == null || p.getId() <= 0) {
                String sql = "INSERT INTO PhieuNhapKho (soPhieuNhap, maKho, maNhaCungCap, ngayNhap, tongTien, trangThai, ghiChu, createdBy) "
                        + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = lease.connection().prepareStatement(sql)) {
                    ps.setString(1, p.getSoPhieuNhap());
                    ps.setInt(2, p.getMaKho());
                    ps.setInt(3, p.getMaNhaCungCap());
                    ps.setTimestamp(4, Timestamp.valueOf(ngayNhap));
                    ps.setBigDecimal(5, tongTien);
                    ps.setString(6, trangThai);
                    ps.setString(7, p.getGhiChu());
                    ps.setObject(8, p.getCreatedBy(), Types.INTEGER);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) p.setId(rs.getInt(1));
                    }
                }
            } else {
                String sql = "UPDATE PhieuNhapKho SET maKho=?, maNhaCungCap=?, ngayNhap=?, tongTien=?, trangThai=?, ghiChu=?, "
                        + "updatedAt=SYSDATETIME(), updatedBy=?, version=version+1 WHERE id=? AND isDeleted=0";
                try (PreparedStatement ps = lease.connection().prepareStatement(sql)) {
                    ps.setInt(1, p.getMaKho());
                    ps.setInt(2, p.getMaNhaCungCap());
                    ps.setTimestamp(3, Timestamp.valueOf(ngayNhap));
                    ps.setBigDecimal(4, tongTien);
                    ps.setString(5, trangThai);
                    ps.setString(6, p.getGhiChu());
                    ps.setObject(7, p.getUpdatedBy(), Types.INTEGER);
                    ps.setInt(8, p.getId());
                    ps.executeUpdate();
                }
                try (PreparedStatement del = lease.connection().prepareStatement(
                        "DELETE FROM ChiTietNhapKho WHERE maPhieuNhap = ?")) {
                    del.setInt(1, p.getId());
                    del.executeUpdate();
                }
            }
            if (p.getId() != null && p.getChiTiets() != null && !p.getChiTiets().isEmpty()) {
                String lineSql = "INSERT INTO ChiTietNhapKho (maPhieuNhap, maItem, soLo, hanSuDung, soLuong, giaNhap, giaBan, createdBy) "
                        + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement lps = lease.connection().prepareStatement(lineSql)) {
                    for (ChiTietNhapKho ct : p.getChiTiets()) {
                        if (ct == null) continue;
                        ct.setMaPhieuNhap(p.getId());
                        lps.setInt(1, p.getId());
                        lps.setInt(2, ct.getMaItem());
                        lps.setString(3, ct.getSoLo());
                        lps.setDate(4, ct.getHanSuDung() != null ? Date.valueOf(ct.getHanSuDung()) : null);
                        lps.setBigDecimal(5, ct.getSoLuong() != null ? ct.getSoLuong() : BigDecimal.ZERO);
                        lps.setBigDecimal(6, ct.getGiaNhap() != null ? ct.getGiaNhap() : BigDecimal.ZERO);
                        lps.setBigDecimal(7, ct.getGiaBan() != null ? ct.getGiaBan() : BigDecimal.ZERO);
                        lps.setObject(8, ct.getCreatedBy() != null ? ct.getCreatedBy() : p.getCreatedBy(), Types.INTEGER);
                        try (ResultSet lrs = lps.executeQuery()) {
                            if (lrs.next()) ct.setId(lrs.getInt(1));
                        }
                    }
                }
            }
            recordChange();
            return p;
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể lưu phiếu nhập kho.", ex);
        }
    }

    @Override
    public Optional<PhieuNhapKho> getPhieuNhapKhoById(int id) {
        if (id <= 0) return Optional.empty();
        String sql = "SELECT id, soPhieuNhap, maKho, maNhaCungCap, ngayNhap, tongTien, trangThai, ghiChu, createdBy "
                + "FROM PhieuNhapKho WHERE id = ? AND isDeleted = 0";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                PhieuNhapKho p = new PhieuNhapKho();
                p.setId(rs.getInt("id"));
                p.setSoPhieuNhap(rs.getString("soPhieuNhap"));
                p.setMaKho(rs.getInt("maKho"));
                p.setMaNhaCungCap(rs.getInt("maNhaCungCap"));
                Timestamp ngay = rs.getTimestamp("ngayNhap");
                if (ngay != null) p.setNgayNhap(ngay.toLocalDateTime());
                p.setTongTien(rs.getBigDecimal("tongTien"));
                p.setTrangThai(mapTrangThaiPhieuNhap(rs.getString("trangThai")));
                p.setGhiChu(rs.getString("ghiChu"));
                p.setCreatedBy((Integer) rs.getObject("createdBy"));
                String lineSql = "SELECT id, maPhieuNhap, maItem, soLo, hanSuDung, soLuong, giaNhap, giaBan "
                        + "FROM ChiTietNhapKho WHERE maPhieuNhap = ? AND isDeleted = 0 ORDER BY id";
                try (PreparedStatement lps = lease.connection().prepareStatement(lineSql)) {
                    lps.setInt(1, id);
                    try (ResultSet lrs = lps.executeQuery()) {
                        List<ChiTietNhapKho> lines = new ArrayList<>();
                        while (lrs.next()) {
                            ChiTietNhapKho ct = new ChiTietNhapKho();
                            ct.setId(lrs.getInt("id"));
                            ct.setMaPhieuNhap(lrs.getInt("maPhieuNhap"));
                            ct.setMaItem(lrs.getInt("maItem"));
                            ct.setSoLo(lrs.getString("soLo"));
                            Date hsd = lrs.getDate("hanSuDung");
                            if (hsd != null) ct.setHanSuDung(hsd.toLocalDate());
                            ct.setSoLuong(lrs.getBigDecimal("soLuong"));
                            ct.setGiaNhap(lrs.getBigDecimal("giaNhap"));
                            ct.setGiaBan(lrs.getBigDecimal("giaBan"));
                            lines.add(ct);
                        }
                        p.setChiTiets(lines);
                    }
                }
                return Optional.of(p);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải phiếu nhập kho #" + id, ex);
        }
    }

    @Override
    public PhieuXuatKho savePhieuXuatKho(PhieuXuatKho p) {
        if (p == null) return null;
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire()) {
            LocalDateTime ngayXuat = p.getNgayXuat() != null ? p.getNgayXuat() : LocalDateTime.now();
            String loaiXuat = p.getLoaiXuat() != null ? p.getLoaiXuat().name() : LoaiXuatKho.BAN_THUOC.name();
            String trangThaiNoiBo = p.getMaDonThuoc() != null
                    ? "DA_XUAT"
                    : (p.getTrangThaiNoiBo() != null ? p.getTrangThaiNoiBo().name() : "DA_XUAT");
            BigDecimal tongTien = p.getTongTien() != null ? p.getTongTien() : BigDecimal.ZERO;
            String sql = "INSERT INTO PhieuXuatKho (soPhieuXuat, maKhoXuat, loaiXuat, maDonThuoc, maKhoNhan, ngayXuat, tongTien, trangThaiNoiBo, ghiChu, createdBy) "
                    + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = lease.connection().prepareStatement(sql)) {
                ps.setString(1, p.getSoPhieuXuat());
                ps.setInt(2, p.getMaKhoXuat());
                ps.setString(3, loaiXuat);
                ps.setObject(4, p.getMaDonThuoc(), Types.INTEGER);
                ps.setObject(5, p.getMaKhoNhan(), Types.INTEGER);
                ps.setTimestamp(6, Timestamp.valueOf(ngayXuat));
                ps.setBigDecimal(7, tongTien);
                ps.setString(8, trangThaiNoiBo);
                ps.setString(9, p.getGhiChu());
                ps.setObject(10, p.getCreatedBy(), Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) p.setId(rs.getInt(1));
                }
            }
            if (p.getId() != null && p.getChiTiets() != null && !p.getChiTiets().isEmpty()) {
                String lineSql = "INSERT INTO ChiTietXuatKho (maPhieuXuat, maItem, maTonKho, soLuong, donGia, createdBy) "
                        + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?)";
                String txSql = "INSERT INTO InventoryTransaction (maKho, maItem, maTonKho, loaiGiaoDich, soLuongThayDoi, "
                        + "tonTruocKhiThayDoi, tonSauKhiThayDoi, maChungTu, createdBy) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                String balSql = "SELECT soLuongTon FROM TonKho WHERE id = ?";
                try (PreparedStatement lps = lease.connection().prepareStatement(lineSql);
                     PreparedStatement tps = lease.connection().prepareStatement(txSql);
                     PreparedStatement bps = lease.connection().prepareStatement(balSql)) {
                    for (ChiTietXuatKho ct : p.getChiTiets()) {
                        if (ct == null) continue;
                        ct.setMaPhieuXuat(p.getId());
                        BigDecimal qty = ct.getSoLuong() != null ? ct.getSoLuong() : BigDecimal.ZERO;
                        lps.setInt(1, p.getId());
                        lps.setInt(2, ct.getMaItem());
                        lps.setInt(3, ct.getMaTonKho());
                        lps.setBigDecimal(4, qty);
                        lps.setBigDecimal(5, ct.getDonGia() != null ? ct.getDonGia() : BigDecimal.ZERO);
                        lps.setObject(6, ct.getCreatedBy() != null ? ct.getCreatedBy() : p.getCreatedBy(), Types.INTEGER);
                        try (ResultSet lrs = lps.executeQuery()) {
                            if (lrs.next()) ct.setId(lrs.getInt(1));
                        }
                        if (ct.getMaTonKho() > 0 && qty.signum() > 0) {
                            BigDecimal after = BigDecimal.ZERO;
                            bps.setInt(1, ct.getMaTonKho());
                            try (ResultSet brs = bps.executeQuery()) {
                                if (brs.next() && brs.getBigDecimal(1) != null) after = brs.getBigDecimal(1);
                            }
                            tps.setInt(1, p.getMaKhoXuat());
                            tps.setInt(2, ct.getMaItem());
                            tps.setInt(3, ct.getMaTonKho());
                            tps.setString(4, p.getMaDonThuoc() != null ? "XUAT_DON_THUOC" : loaiXuat);
                            tps.setBigDecimal(5, qty.negate());
                            tps.setBigDecimal(6, after.add(qty));
                            tps.setBigDecimal(7, after);
                            tps.setString(8, p.getSoPhieuXuat());
                            tps.setObject(9, p.getCreatedBy(), Types.INTEGER);
                            tps.executeUpdate();
                        }
                    }
                }
            }
            recordChange();
            return p;
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể lưu phiếu xuất kho.", ex);
        }
    }

    @Override
    public Optional<PhieuXuatKho> getPhieuXuatKhoById(int id) {
        if (id <= 0) return Optional.empty();
        String sql = "SELECT id, soPhieuXuat, maKhoXuat, loaiXuat, maDonThuoc, maKhoNhan, ngayXuat, tongTien, trangThaiNoiBo, ghiChu "
                + "FROM PhieuXuatKho WHERE id = ? AND isDeleted = 0";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                PhieuXuatKho p = new PhieuXuatKho();
                p.setId(rs.getInt("id"));
                p.setSoPhieuXuat(rs.getString("soPhieuXuat"));
                p.setMaKhoXuat(rs.getInt("maKhoXuat"));
                p.setLoaiXuat(mapLoaiXuat(rs.getString("loaiXuat")));
                p.setMaDonThuoc((Integer) rs.getObject("maDonThuoc"));
                p.setMaKhoNhan((Integer) rs.getObject("maKhoNhan"));
                Timestamp ngay = rs.getTimestamp("ngayXuat");
                if (ngay != null) p.setNgayXuat(ngay.toLocalDateTime());
                p.setTongTien(rs.getBigDecimal("tongTien"));
                p.setTrangThaiNoiBo(mapTrangThaiXuat(rs.getString("trangThaiNoiBo")));
                p.setGhiChu(rs.getString("ghiChu"));
                String lineSql = "SELECT id, maPhieuXuat, maItem, maTonKho, soLuong, donGia "
                        + "FROM ChiTietXuatKho WHERE maPhieuXuat = ? AND isDeleted = 0 ORDER BY id";
                try (PreparedStatement lps = lease.connection().prepareStatement(lineSql)) {
                    lps.setInt(1, id);
                    try (ResultSet lrs = lps.executeQuery()) {
                        List<ChiTietXuatKho> lines = new ArrayList<>();
                        while (lrs.next()) {
                            ChiTietXuatKho ct = new ChiTietXuatKho();
                            ct.setId(lrs.getInt("id"));
                            ct.setMaPhieuXuat(lrs.getInt("maPhieuXuat"));
                            ct.setMaItem(lrs.getInt("maItem"));
                            ct.setMaTonKho(lrs.getInt("maTonKho"));
                            ct.setSoLuong(lrs.getBigDecimal("soLuong"));
                            ct.setDonGia(lrs.getBigDecimal("donGia"));
                            lines.add(ct);
                        }
                        p.setChiTiets(lines);
                    }
                }
                return Optional.of(p);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải phiếu xuất kho #" + id, ex);
        }
    }

    private static TrangThaiPhieuNhap mapTrangThaiPhieuNhap(String raw) {
        if (raw == null) return TrangThaiPhieuNhap.TAM;
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "CHO_DUYET" -> TrangThaiPhieuNhap.CHO_DUYET;
            case "DA_NHAP", "HOAN_THANH" -> TrangThaiPhieuNhap.DA_NHAP;
            case "DA_HUY" -> TrangThaiPhieuNhap.DA_HUY;
            default -> TrangThaiPhieuNhap.TAM;
        };
    }

    private static LoaiXuatKho mapLoaiXuat(String raw) {
        if (raw == null) return LoaiXuatKho.BAN_THUOC;
        try {
            return LoaiXuatKho.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return LoaiXuatKho.BAN_THUOC;
        }
    }

    private static TrangThaiXuatNoiBo mapTrangThaiXuat(String raw) {
        if (raw == null) return TrangThaiXuatNoiBo.CHO_DUYET;
        try {
            return TrangThaiXuatNoiBo.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return TrangThaiXuatNoiBo.DA_DUYET;
        }
    }
    @Override public DonDatHang savePurchaseOrder(DonDatHang d) { return d; }
    @Override public Optional<DonDatHang> getPurchaseOrderById(int id) { return Optional.empty(); }
    @Override public KiemKe saveStocktake(KiemKe k) { return k; }
    @Override public Optional<KiemKe> getStocktakeById(int id) { return Optional.empty(); }

    @Override public int saveChanges() { return unitOfWork.saveChanges(); }
    @Override public boolean saveEntities() { return unitOfWork.saveEntities(); }

    private void attachTonKho(List<MatHang> items) {
        if (items == null || items.isEmpty()) return;
        Map<Integer, MatHang> byId = new LinkedHashMap<>();
        for (MatHang item : items) {
            if (item != null && item.getId() != null) {
                item.getTonKhos().clear();
                byId.put(item.getId(), item);
            }
        }
        if (byId.isEmpty()) return;
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(TONKHO_SELECT + " AND soLuongTon > 0 ORDER BY hanSuDung ASC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TonKho tk = mapTonKho(rs);
                    MatHang owner = byId.get(tk.getMaItem());
                    if (owner != null) {
                        owner.getTonKhos().add(tk);
                    }
                }
            }
        } catch (SQLException ignored) {
        }
    }

    private void bindItemParameters(PreparedStatement ps, MatHang item) throws SQLException {
        ps.setString(1, item.getMaItem());
        ps.setString(2, item.getTenItem());
        ps.setString(3, item.getLoaiItem() != null ? item.getLoaiItem().name() : LoaiMatHang.THUOC.name());
        ps.setString(4, item.getHoatChat());
        ps.setString(5, item.getHamLuong());
        ps.setString(6, item.getDonViCoBan());
        ps.setString(7, item.getQuyCachDongGoi());
        ps.setBigDecimal(8, item.getGiaNhapMacDinh() != null ? item.getGiaNhapMacDinh() : BigDecimal.ZERO);
        ps.setBigDecimal(9, item.getGiaBanMacDinh() != null ? item.getGiaBanMacDinh() : BigDecimal.ZERO);
        ps.setBigDecimal(10, item.getTonToiThieu() != null ? item.getTonToiThieu() : BigDecimal.ZERO);
        ps.setBigDecimal(11, item.getTonToiDa() != null ? item.getTonToiDa() : new BigDecimal("1000000"));
        ps.setString(12, item.getDangBaoChe());
        ps.setString(13, item.getNuocSanXuat());
        ps.setString(14, item.getHangSanXuat());
        ps.setString(15, item.getSoDangKy());
        ps.setBoolean(16, item.isHoatDong());
    }

    private static MatHang mapMatHang(ResultSet rs) throws SQLException {
        MatHang m = new MatHang();
        m.setId(rs.getInt("id"));
        m.setMaItem(rs.getString("maItem"));
        m.setTenItem(rs.getString("tenItem"));
        String loaiStr = rs.getString("loaiItem");
        m.setLoaiItem("VAT_TU".equalsIgnoreCase(loaiStr) ? LoaiMatHang.VAT_TU : LoaiMatHang.THUOC);
        m.setHoatChat(rs.getString("hoatChat"));
        m.setHamLuong(rs.getString("hamLuong"));
        m.setDonViCoBan(rs.getString("donViCoBan"));
        m.setQuyCachDongGoi(rs.getString("quyCachDongGoi"));
        m.setGiaNhapMacDinh(rs.getBigDecimal("giaNhapMacDinh") != null ? rs.getBigDecimal("giaNhapMacDinh") : BigDecimal.ZERO);
        m.setGiaBanMacDinh(rs.getBigDecimal("giaBanMacDinh") != null ? rs.getBigDecimal("giaBanMacDinh") : BigDecimal.ZERO);
        m.setTonToiThieu(rs.getBigDecimal("tonToiThieu") != null ? rs.getBigDecimal("tonToiThieu") : BigDecimal.ZERO);
        m.setTonToiDa(rs.getBigDecimal("tonToiDa") != null ? rs.getBigDecimal("tonToiDa") : new BigDecimal("1000000"));
        m.setDangBaoChe(rs.getString("dangBaoChe"));
        m.setNuocSanXuat(rs.getString("nuocSanXuat"));
        m.setHangSanXuat(rs.getString("hangSanXuat"));
        m.setSoDangKy(rs.getString("soDangKy"));
        m.setHoatDong(rs.getBoolean("hoatDong"));
        return m;
    }

    private static TonKho mapTonKho(ResultSet rs) throws SQLException {
        TonKho tk = new TonKho();
        tk.setId(rs.getInt("id"));
        tk.setMaKho(rs.getInt("maKho"));
        tk.setMaItem(rs.getInt("maItem"));
        tk.setSoLo(rs.getString("soLo"));
        Date hsd = rs.getDate("hanSuDung");
        if (hsd != null) tk.setHanSuDung(hsd.toLocalDate());
        tk.setSoLuongTon(rs.getBigDecimal("soLuongTon") != null ? rs.getBigDecimal("soLuongTon") : BigDecimal.ZERO);
        tk.setGiaNhap(rs.getBigDecimal("giaNhap") != null ? rs.getBigDecimal("giaNhap") : BigDecimal.ZERO);
        tk.setGiaBan(rs.getBigDecimal("giaBan") != null ? rs.getBigDecimal("giaBan") : BigDecimal.ZERO);
        return tk;
    }

    private static LoaiKho mapLoaiKho(String raw) {
        if (raw == null) return LoaiKho.THUOC;
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "VAT_TU" -> LoaiKho.VAT_TU;
            case "KHO_TONG", "TONG_HOP" -> LoaiKho.TONG_HOP;
            default -> LoaiKho.THUOC;
        };
    }

    private static TrangThaiKho mapTrangThaiKho(String raw) {
        if (raw == null) return TrangThaiKho.HOAT_DONG;
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "TAM_NGUNG", "NGUNG_HOAT_DONG" -> TrangThaiKho.TAM_NGUNG;
            case "DONG_CUA" -> TrangThaiKho.DONG_CUA;
            default -> TrangThaiKho.HOAT_DONG;
        };
    }

    private void recordChange() {
        if (connections.isTransactionActive()) {
            unitOfWork.recordChanges(1);
        }
    }
}
