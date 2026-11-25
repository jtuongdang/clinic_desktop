package vn.clinicmanagement.infrastructure.persistence.service;

import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.ITieuChuan;
import vn.clinicmanagement.domain.repositories.PhanTrangKetQua;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;
import vn.clinicmanagement.domain.service.DanhMucDichVu;
import vn.clinicmanagement.domain.service.ThietLapThucHienDichVu;
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
 * Triển khai JDBC cho cổng lưu trữ chỉ định dịch vụ kỹ thuật và danh mục viện phí (IChiDinhDichVuRepository).
 */
public final class ChiDinhDichVuRepository implements IChiDinhDichVuRepository {

    private static final String CATALOG_SELECT =
            "SELECT id, maDichVu, tenDichVu, loaiDichVu, nhomDichVu, donGia, giaBHYT, hoatDong, moTa " +
            "FROM DanhMucDichVu WHERE isDeleted = 0";

    private static final String ORDER_SELECT =
            "SELECT id, maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, bacSiThucHien, " +
            "soLuong, donGia, trangThai, ketQua, ketLuan, danhSachHinhAnhUrl, " +
            "thoiGianChiDinh, thoiGianThucHien, thoiGianTraKetQua, version, createdAt, createdBy, updatedAt, updatedBy " +
            "FROM ChiDinhDichVu WHERE isDeleted = 0";

    private static final String LAB_RESULT_SELECT =
            "SELECT id, maChiDinhDichVu, tenChiSo, giaTri, donViDo, khoangThamChieu, danhGia, " +
            "version, createdAt, createdBy, updatedAt, updatedBy " +
            "FROM ChiTietKetQuaXetNghiem WHERE isDeleted = 0";

    private final JdbcKetNoiNhaCungCap connections;
    private final JdbcUnitOfWork unitOfWork;

    public ChiDinhDichVuRepository(JdbcKetNoiNhaCungCap connections) {
        this(connections, new JdbcUnitOfWork());
    }

    public ChiDinhDichVuRepository(JdbcKetNoiNhaCungCap connections, JdbcUnitOfWork unitOfWork) {
        this.connections = Objects.requireNonNull(connections, "connections");
        this.unitOfWork = unitOfWork != null ? unitOfWork : new JdbcUnitOfWork();
    }

    @Override
    public ThietLapThucHienDichVu getExecutionSettings(int orderId) {
        String sql = """
                SELECT c.giaTriXml
                FROM dbo.ChiDinhDichVu o
                JOIN dbo.DichVuCauHinh c ON c.maDichVu = o.maDichVu
                WHERE o.id = ? AND o.isDeleted = 0
                """;
        try (var lease = connections.acquire(); var statement = lease.connection().prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) return ThietLapThucHienDichVu.MAC_DINH;
                var settings = DichVuCauHinhRepository.decode(result.getString(1));
                return new ThietLapThucHienDichVu(
                        Boolean.parseBoolean(settings.get("blockCancellation")),
                        Boolean.parseBoolean(settings.get("noSeparateResult")),
                        settings.get("labUnit"), settings.get("labNormalText"),
                        settings.get("clsReportInstructions"), settings.get("clsResultFormat"),
                        settings.get("clsConclusionTemplate"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không đọc được cấu hình thực hiện dịch vụ.", ex);
        }
    }

    /**
     * Tra cứu danh mục dịch vụ theo từ khóa và loại dịch vụ (KHAM, XET_NGHIEM, CDHA, DVKHAC).
     */
    @Override
    public List<DanhMucDichVu> searchDichVu(String keyword, LoaiDichVu loaiDichVu) {
        StringBuilder sql = new StringBuilder(CATALOG_SELECT);
        sql.append(" AND hoatDong = 1");
        List<Object> params = new ArrayList<>();

        if (loaiDichVu != null) {
            switch (loaiDichVu) {
                case KHAM -> sql.append(" AND loaiDichVu IN ('KHAM_BENH', 'KHAM')");
                case XET_NGHIEM -> sql.append(" AND loaiDichVu = 'XET_NGHIEM'");
                case CHAN_DOAN_HINH_ANH -> sql.append(" AND loaiDichVu IN ('CDHA', 'CHAN_DOAN_HINH_ANH')");
                case DICH_VU_KY_THUAT -> sql.append(" AND loaiDichVu IN ('DVKHAC', 'DICH_VU_KY_THUAT')");
            }
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(tenDichVu) LIKE ? OR LOWER(maDichVu) LIKE ? OR LOWER(COALESCE(nhomDichVu, '')) LIKE ?)");
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append(" ORDER BY id ASC");

        List<DanhMucDichVu> list = new ArrayList<>();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDanhMucDichVu(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn danh mục dịch vụ.", ex);
        }
        return list;
    }

    /**
     * Lấy dịch vụ danh mục theo ID.
     */
    @Override
    public Optional<DanhMucDichVu> getDichVuById(int maDichVu) {
        String sql = CATALOG_SELECT + " AND id = ?";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, maDichVu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapDanhMucDichVu(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Lỗi truy vấn dịch vụ theo ID: " + maDichVu, ex);
        }
        return Optional.empty();
    }

    @Override
    public void save(ChiDinhDichVu chiDinh) {
        if (chiDinh == null) return;
        if (chiDinh.getId() == null || chiDinh.getId() <= 0) {
            String sql = "INSERT INTO ChiDinhDichVu (maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, "
                    + "bacSiThucHien, soLuong, donGia, trangThai, ketQua, ketLuan, danhSachHinhAnhUrl, "
                    + "thoiGianChiDinh, thoiGianThucHien, thoiGianTraKetQua, createdBy) "
                    + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
                 PreparedStatement ps = lease.connection().prepareStatement(sql)) {
                ps.setInt(1, chiDinh.getMaLuotKham());
                ps.setInt(2, chiDinh.getMaDichVu());
                ps.setInt(3, chiDinh.getMaBacSiChiDinh());
                ps.setObject(4, chiDinh.getMaPhongThucHien(), Types.INTEGER);
                ps.setObject(5, chiDinh.getBacSiThucHien(), Types.INTEGER);
                ps.setBigDecimal(6, chiDinh.getSoLuong() != null ? chiDinh.getSoLuong() : BigDecimal.ONE);
                ps.setBigDecimal(7, chiDinh.getDonGia() != null ? chiDinh.getDonGia() : BigDecimal.ZERO);
                ps.setString(8, chiDinh.getTrangThai() != null
                        ? chiDinh.getTrangThai().name() : TrangThaiChiDinhDichVu.DA_CHI_DINH.name());
                ps.setString(9, chiDinh.getKetQua());
                ps.setString(10, chiDinh.getKetLuan());
                ps.setString(11, chiDinh.getDanhSachHinhAnhUrl());
                LocalDateTime orderedAt = chiDinh.getThoiGianChiDinh() != null
                        ? chiDinh.getThoiGianChiDinh() : LocalDateTime.now();
                ps.setTimestamp(12, Timestamp.valueOf(orderedAt));
                ps.setTimestamp(13, chiDinh.getThoiGianThucHien() != null
                        ? Timestamp.valueOf(chiDinh.getThoiGianThucHien()) : null);
                ps.setTimestamp(14, chiDinh.getThoiGianTraKetQua() != null
                        ? Timestamp.valueOf(chiDinh.getThoiGianTraKetQua()) : null);
                ps.setObject(15, chiDinh.getCreatedBy(), Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        chiDinh.setId(rs.getInt(1));
                    }
                }
                assignLabSpecimenMetadataIfNeeded(lease.connection(), chiDinh.getId(), chiDinh.getMaLuotKham(), chiDinh.getMaDichVu());
                recordChange();
            } catch (SQLException ex) {
                throw new IllegalStateException("Không thể lưu chỉ định dịch vụ.", ex);
            }
            return;
        }

        String sql = "UPDATE ChiDinhDichVu SET maPhongThucHien = ?, bacSiThucHien = ?, soLuong = ?, donGia = ?, "
                + "trangThai = ?, ketQua = ?, ketLuan = ?, danhSachHinhAnhUrl = ?, thoiGianThucHien = ?, "
                + "thoiGianTraKetQua = ?, updatedAt = SYSDATETIME(), updatedBy = ?, version = version + 1 "
                + "WHERE id = ? AND isDeleted = 0";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setObject(1, chiDinh.getMaPhongThucHien(), Types.INTEGER);
            ps.setObject(2, chiDinh.getBacSiThucHien(), Types.INTEGER);
            ps.setBigDecimal(3, chiDinh.getSoLuong() != null ? chiDinh.getSoLuong() : BigDecimal.ONE);
            ps.setBigDecimal(4, chiDinh.getDonGia() != null ? chiDinh.getDonGia() : BigDecimal.ZERO);
            ps.setString(5, chiDinh.getTrangThai() != null
                    ? chiDinh.getTrangThai().name() : TrangThaiChiDinhDichVu.DA_CHI_DINH.name());
            ps.setString(6, chiDinh.getKetQua());
            ps.setString(7, chiDinh.getKetLuan());
            ps.setString(8, chiDinh.getDanhSachHinhAnhUrl());
            ps.setTimestamp(9, chiDinh.getThoiGianThucHien() != null
                    ? Timestamp.valueOf(chiDinh.getThoiGianThucHien()) : null);
            ps.setTimestamp(10, chiDinh.getThoiGianTraKetQua() != null
                    ? Timestamp.valueOf(chiDinh.getThoiGianTraKetQua()) : null);
            ps.setObject(11, chiDinh.getUpdatedBy(), Types.INTEGER);
            ps.setInt(12, chiDinh.getId());
            ps.executeUpdate();
            recordChange();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể cập nhật chỉ định dịch vụ.", ex);
        }
    }

    @Override
    public List<ChiDinhDichVu> getByLuotKham(int maLuotKham) {
        if (maLuotKham <= 0) return List.of();
        String sql = ORDER_SELECT + " AND maLuotKham = ? ORDER BY thoiGianChiDinh DESC, id DESC";
        List<ChiDinhDichVu> orders = new ArrayList<>();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, maLuotKham);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapChiDinhDichVu(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh sách chỉ định theo lượt khám.", ex);
        }
        return orders;
    }

    @Override
    public List<ChiTietKetQuaXetNghiem> getKetQuaByChiDinh(int maChiDinh) {
        if (maChiDinh <= 0) return List.of();
        String sql = LAB_RESULT_SELECT + " AND maChiDinhDichVu = ? ORDER BY id ASC";
        List<ChiTietKetQuaXetNghiem> results = new ArrayList<>();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, maChiDinh);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapKetQuaXetNghiem(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải chi tiết kết quả xét nghiệm.", ex);
        }
        return results;
    }

    @Override
    public void saveKetQua(ChiTietKetQuaXetNghiem ketQua) {
        if (ketQua == null) return;
        if (ketQua.getId() == null || ketQua.getId() <= 0) {
            String findSql = "SELECT TOP 1 id FROM ChiTietKetQuaXetNghiem "
                    + "WHERE maChiDinhDichVu = ? AND tenChiSo = ? AND isDeleted = 0 ORDER BY id ASC";
            try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
                 PreparedStatement findPs = lease.connection().prepareStatement(findSql)) {
                findPs.setInt(1, ketQua.getMaChiDinhDichVu());
                findPs.setString(2, ketQua.getTenChiSo());
                try (ResultSet findRs = findPs.executeQuery()) {
                    if (findRs.next()) {
                        ketQua.setId(findRs.getInt(1));
                    }
                }
            } catch (SQLException ignored) {}
        }
        if (ketQua.getId() == null || ketQua.getId() <= 0) {
            String sql = "INSERT INTO ChiTietKetQuaXetNghiem (maChiDinhDichVu, tenChiSo, giaTri, donViDo, "
                    + "khoangThamChieu, danhGia, createdBy) OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
                 PreparedStatement ps = lease.connection().prepareStatement(sql)) {
                ps.setInt(1, ketQua.getMaChiDinhDichVu());
                ps.setString(2, ketQua.getTenChiSo());
                ps.setString(3, ketQua.getGiaTri());
                ps.setString(4, ketQua.getDonViDo());
                ps.setString(5, ketQua.getKhoangThamChieu());
                ps.setString(6, ketQua.getDanhGia());
                ps.setObject(7, ketQua.getCreatedBy(), Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ketQua.setId(rs.getInt(1));
                    }
                }
                recordChange();
            } catch (SQLException ex) {
                throw new IllegalStateException("Không thể lưu chi tiết kết quả xét nghiệm.", ex);
            }
            return;
        }

        String sql = "UPDATE ChiTietKetQuaXetNghiem SET tenChiSo = ?, giaTri = ?, donViDo = ?, "
                + "khoangThamChieu = ?, danhGia = ?, updatedAt = SYSDATETIME(), updatedBy = ?, version = version + 1 "
                + "WHERE id = ? AND isDeleted = 0";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setString(1, ketQua.getTenChiSo());
            ps.setString(2, ketQua.getGiaTri());
            ps.setString(3, ketQua.getDonViDo());
            ps.setString(4, ketQua.getKhoangThamChieu());
            ps.setString(5, ketQua.getDanhGia());
            ps.setObject(6, ketQua.getUpdatedBy() != null ? ketQua.getUpdatedBy() : ketQua.getCreatedBy(), Types.INTEGER);
            ps.setInt(7, ketQua.getId());
            ps.executeUpdate();
            recordChange();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể cập nhật chi tiết kết quả xét nghiệm.", ex);
        }
    }

    @Override
    public Optional<ChiDinhDichVu> getById(int id) {
        if (id <= 0) return Optional.empty();
        String sql = ORDER_SELECT + " AND id = ?";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapChiDinhDichVu(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải thông tin chỉ định dịch vụ.", ex);
        }
        return Optional.empty();
    }

    @Override
    public List<ChiDinhDichVu> getAll() {
        return List.of();
    }

    @Override
    public PhanTrangKetQua<ChiDinhDichVu> getPaged(int pageIndex, int pageSize, ITieuChuan<ChiDinhDichVu> specification) {
        return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
    }

    @Override
    public List<ChiDinhDichVu> find(ITieuChuan<ChiDinhDichVu> specification) {
        return List.of();
    }

    @Override
    public boolean exists(ITieuChuan<ChiDinhDichVu> specification) {
        return false;
    }

    @Override
    public int count(ITieuChuan<ChiDinhDichVu> specification) {
        return 0;
    }

    @Override
    public void add(ChiDinhDichVu entity) {
        save(entity);
    }

    @Override
    public void update(ChiDinhDichVu entity) {
        save(entity);
    }

    @Override
    public void delete(ChiDinhDichVu entity) {
        // no-op
    }

    @Override
    public IUnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    private static DanhMucDichVu mapDanhMucDichVu(ResultSet rs) throws SQLException {
        DanhMucDichVu d = new DanhMucDichVu();
        d.setId(rs.getInt("id"));
        d.setMaDichVu(rs.getString("maDichVu"));
        d.setTenDichVu(rs.getString("tenDichVu"));
        String loaiStr = rs.getString("loaiDichVu");
        if (loaiStr != null) {
            switch (loaiStr.trim().toUpperCase()) {
                case "KHAM_BENH", "KHAM" -> d.setLoaiDichVu(LoaiDichVu.KHAM);
                case "XET_NGHIEM" -> d.setLoaiDichVu(LoaiDichVu.XET_NGHIEM);
                case "CDHA", "CHAN_DOAN_HINH_ANH" -> d.setLoaiDichVu(LoaiDichVu.CHAN_DOAN_HINH_ANH);
                default -> d.setLoaiDichVu(LoaiDichVu.DICH_VU_KY_THUAT);
            }
        }
        d.setDonGia(rs.getBigDecimal("donGia") != null ? rs.getBigDecimal("donGia") : BigDecimal.ZERO);
        d.setGiaBHYT(rs.getBigDecimal("giaBHYT") != null ? rs.getBigDecimal("giaBHYT") : BigDecimal.ZERO);
        d.setHoatDong(rs.getBoolean("hoatDong"));
        d.setMoTa(rs.getString("moTa"));
        d.setNhomDichVu(rs.getString("nhomDichVu"));
        return d;
    }

    private void assignLabSpecimenMetadataIfNeeded(java.sql.Connection conn, Integer orderId, int visitId, int serviceId) {
        if (conn == null || orderId == null || orderId <= 0) return;
        String sql = "UPDATE cd "
                + "SET cd.maPhieuChiDinh = COALESCE(cd.maPhieuChiDinh, grp.maPhieuChiDinh, CONCAT('XN', FORMAT(ISNULL(cd.thoiGianChiDinh, SYSDATETIME()), 'yyMMdd'), '.', cd.id)), "
                + "    cd.soBarcode = COALESCE(cd.soBarcode, grp.soBarcode, CONCAT(CASE WHEN dv.nhomDichVu = N'Nước tiểu' THEN 'U' WHEN dv.nhomDichVu = N'Huyết học' THEN '' ELSE 'H' END, RIGHT(CONCAT('00000', CAST(15000 + cd.id AS VARCHAR(10))), 5))), "
                + "    cd.noiLayMau = COALESCE(cd.noiLayMau, CASE "
                + "        WHEN dv.nhomDichVu = N'Huyết học' THEN N'Lấy mẫu huyết học' "
                + "        WHEN dv.nhomDichVu = N'Nước tiểu' THEN N'Lấy mẫu nước tiểu' "
                + "        WHEN dv.nhomDichVu = N'Vi sinh' THEN N'Lấy mẫu vi sinh' "
                + "        ELSE N'Lấy mẫu sinh hóa' END) "
                + "FROM ChiDinhDichVu cd "
                + "JOIN DanhMucDichVu dv ON dv.id = cd.maDichVu AND dv.loaiDichVu = 'XET_NGHIEM' "
                + "OUTER APPLY ("
                + "    SELECT TOP 1 c2.maPhieuChiDinh, c2.soBarcode "
                + "    FROM ChiDinhDichVu c2 "
                + "    JOIN DanhMucDichVu d2 ON d2.id = c2.maDichVu "
                + "    WHERE c2.maLuotKham = ? AND c2.id <> ? AND c2.isDeleted = 0 "
                + "      AND c2.trangThai IN ('CHO_THUC_HIEN', 'DA_CHI_DINH') "
                + "      AND ISNULL(d2.nhomDichVu, N'') = ISNULL(dv.nhomDichVu, N'') "
                + "      AND c2.soBarcode IS NOT NULL "
                + "    ORDER BY c2.id DESC"
                + ") grp "
                + "WHERE cd.id = ? AND cd.maDichVu = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            ps.setInt(2, orderId);
            ps.setInt(3, orderId);
            ps.setInt(4, serviceId);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private void recordChange() {
        if (connections.isTransactionActive()) {
            unitOfWork.recordChanges(1);
        }
    }

    private static ChiDinhDichVu mapChiDinhDichVu(ResultSet rs) throws SQLException {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(rs.getInt("id"));
        order.setMaLuotKham(rs.getInt("maLuotKham"));
        order.setMaDichVu(rs.getInt("maDichVu"));
        order.setMaBacSiChiDinh(rs.getInt("maBacSiChiDinh"));
        order.setMaPhongThucHien((Integer) rs.getObject("maPhongThucHien"));
        order.setBacSiThucHien((Integer) rs.getObject("bacSiThucHien"));
        order.setSoLuong(rs.getBigDecimal("soLuong") != null ? rs.getBigDecimal("soLuong") : BigDecimal.ONE);
        order.setDonGia(rs.getBigDecimal("donGia") != null ? rs.getBigDecimal("donGia") : BigDecimal.ZERO);
        String statusStr = rs.getString("trangThai");
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                order.setTrangThai(TrangThaiChiDinhDichVu.valueOf(statusStr.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                order.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
            }
        }
        order.setKetQua(rs.getString("ketQua"));
        order.setKetLuan(rs.getString("ketLuan"));
        order.setDanhSachHinhAnhUrl(rs.getString("danhSachHinhAnhUrl"));
        order.setThoiGianChiDinh(localDateTime(rs, "thoiGianChiDinh"));
        order.setThoiGianThucHien(localDateTime(rs, "thoiGianThucHien"));
        order.setThoiGianTraKetQua(localDateTime(rs, "thoiGianTraKetQua"));
        order.setVersion(rs.getLong("version"));
        order.setCreatedAt(localDateTime(rs, "createdAt"));
        Integer createdBy = (Integer) rs.getObject("createdBy");
        if (createdBy != null && createdBy > 0) order.setCreatedBy(createdBy);
        order.setUpdatedAt(localDateTime(rs, "updatedAt"));
        order.setUpdatedBy((Integer) rs.getObject("updatedBy"));
        return order;
    }

    private static ChiTietKetQuaXetNghiem mapKetQuaXetNghiem(ResultSet rs) throws SQLException {
        ChiTietKetQuaXetNghiem line = new ChiTietKetQuaXetNghiem();
        line.setId(rs.getInt("id"));
        line.setMaChiDinhDichVu(rs.getInt("maChiDinhDichVu"));
        line.setTenChiSo(rs.getString("tenChiSo"));
        line.setGiaTri(rs.getString("giaTri"));
        line.setDonViDo(rs.getString("donViDo"));
        line.setKhoangThamChieu(rs.getString("khoangThamChieu"));
        line.setDanhGia(rs.getString("danhGia"));
        line.setVersion(rs.getLong("version"));
        line.setCreatedAt(localDateTime(rs, "createdAt"));
        Integer createdBy = (Integer) rs.getObject("createdBy");
        if (createdBy != null && createdBy > 0) line.setCreatedBy(createdBy);
        line.setUpdatedAt(localDateTime(rs, "updatedAt"));
        line.setUpdatedBy((Integer) rs.getObject("updatedBy"));
        return line;
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }
}
