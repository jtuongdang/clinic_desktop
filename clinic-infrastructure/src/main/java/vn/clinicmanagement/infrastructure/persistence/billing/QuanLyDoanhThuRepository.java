package vn.clinicmanagement.infrastructure.persistence.billing;

import vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Triển khai JDBC đọc dữ liệu Quản Lý Doanh Thu và 5 báo cáo doanh thu từ clinicDb.
 */
public final class QuanLyDoanhThuRepository implements QuanLyDoanhThuReadPort {
    private final JdbcKetNoiNhaCungCap connections;

    public QuanLyDoanhThuRepository(JdbcKetNoiNhaCungCap connections) {
        this.connections = connections;
    }

    @Override
    public List<RevenueVisitRow> listVisits(int facilityId, boolean filterByDischarge, LocalDateTime from, LocalDateTime to) {
        String dateCol = filterByDischarge ? "COALESCE(l.thoiGianKetThucKham, l.thoiGianTiepNhan)" : "l.thoiGianTiepNhan";
        String sql = "SELECT l.id, l.thoiGianTiepNhan, l.thoiGianKetThucKham, p.maBenhNhanFormat, "
                + "p.ho + N' ' + p.ten AS patientName, p.ngaySinh, ISNULL(p.gioiTinh, N''), "
                + "ISNULL(k.tenKhoa, N''), ISNULL(r.tenPhong, N''), "
                + "ISNULL(l.doiTuongKham, N'THU_PHI'), ISNULL(l.hinhThucDieuTri, N'NGOAI_TRU'), ISNULL(l.trangThai, N''), "
                + "ISNULL(inv.tongTien, 0) AS tongChiPhi, ISNULL(inv.daThanhToan, 0) AS bnThanhToan "
                + "FROM dbo.LuotKham l "
                + "JOIN dbo.BenhNhan p ON p.id = l.maBenhNhan AND p.isDeleted = 0 "
                + "LEFT JOIN dbo.Khoa k ON k.id = l.maKhoa "
                + "LEFT JOIN dbo.PhongKham r ON r.id = l.maPhongKham "
                + "OUTER APPLY (SELECT SUM(h.tongTien) tongTien, SUM(h.daThanhToan) daThanhToan "
                + "             FROM dbo.HoaDon h WHERE h.maLuotKham = l.id AND h.isDeleted = 0) inv "
                + "WHERE l.isDeleted = 0 AND l.maCoSo = ? AND " + dateCol + " >= ? AND " + dateCol + " <= ? "
                + "ORDER BY l.thoiGianTiepNhan DESC, l.id DESC";
        List<RevenueVisitRow> rows = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    var admit = rs.getTimestamp(2);
                    var disch = rs.getTimestamp(3);
                    var dob = rs.getDate(6);
                    rows.add(new RevenueVisitRow(
                            rs.getInt(1),
                            admit == null ? null : admit.toLocalDateTime(),
                            disch == null ? null : disch.toLocalDateTime(),
                            rs.getString(4),
                            rs.getString(5),
                            dob == null ? null : dob.toLocalDate(),
                            rs.getString(7),
                            rs.getString(8),
                            rs.getString(9),
                            rs.getString(10),
                            rs.getString(11),
                            rs.getString(12),
                            nz(rs.getBigDecimal(13)),
                            nz(rs.getBigDecimal(14))
                    ));
                }
            }
            return List.copyOf(rows);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được dữ liệu Quản Lý Doanh Thu.", ex);
        }
    }

    @Override
    public List<ReceiptReportRow> listReceiptReport(int facilityId, boolean filterByDate, int timeModeIndex,
                                                    LocalDateTime from, LocalDateTime to, int sortModeIndex) {
        String dateColumn = timeModeIndex == 2 ? "h.ngayLap" : "pt.ngayThu";
        String orderBy = sortModeIndex == 1 ? "pt.phuongThuc, pt.ngayThu DESC"
                : sortModeIndex == 2 ? "h.soHoaDon, pt.ngayThu DESC"
                : "thuNgan, pt.ngayThu DESC, pt.id DESC";

        String sql = "SELECT pt.id, h.id AS maVienPhi, h.soHoaDon, pt.ngayThu, l.thoiGianTiepNhan, "
                + "p.ho + N' ' + p.ten AS bn, p.ngaySinh, ISNULL(p.gioiTinh, N'') AS gioiTinh, ISNULL(p.diaChi, N'') AS diaChi, "
                + "ISNULL(nv.ho + N' ' + nv.ten, N'Thu ngân') AS thuNgan, "
                + "ISNULL(pt.phuongThuc, N'TIEN_MAT') AS hinhThuc, "
                + "ISNULL(l.doiTuongKham, N'THU_PHI') AS doiTuong, ISNULL(l.hinhThucDieuTri, N'NGOAI_TRU') AS hinhThucDieuTri, "
                + "ISNULL(l.loaiDangKyKham, N'') AS loaiDangKyKham, ISNULL(k.tenKhoa, N'') AS tenKhoa, "
                + "ISNULL(r.tenPhong, N'') AS tenPhong, "
                + "ISNULL(svc.bhytCount, 0) AS bhytCount, ISNULL(svc.serviceCount, 0) AS serviceCount, "
                + "ISNULL(svc.feeCount, 0) AS feeCount, ISNULL(svc.lineCount, 0) AS lineCount, "
                + "pt.soTien, ISNULL(h.soTienHoanTra, 0) AS totalSoTienHoanTra, "
                + "ISNULL(h.soTienTamUng, 0) AS totalSoTienTamUng, "
                + "CASE WHEN ROW_NUMBER() OVER(PARTITION BY h.id ORDER BY pt.ngayThu, pt.id) = 1 "
                + "THEN ISNULL(h.soTienHoanTra, 0) ELSE 0 END AS soTienHoanTra, "
                + "CASE WHEN ROW_NUMBER() OVER(PARTITION BY h.id ORDER BY pt.ngayThu, pt.id) = 1 "
                + "THEN ISNULL(h.soTienTamUng, 0) ELSE 0 END AS soTienTamUng, "
                + "CASE WHEN ROW_NUMBER() OVER(PARTITION BY h.id ORDER BY pt.ngayThu, pt.id) = 1 "
                + "THEN ISNULL(h.soTienGiamGia, 0) ELSE 0 END AS soTienGiamGia, "
                + "ISNULL(h.lyDoGiamGia, N'') AS lyDoGiamGia, ISNULL(pt.ghiChu, N'') AS ghiChu, "
                + "ISNULL(pt.soThu, N'') AS soThu, ISNULL(pt.laSoTruc, 0) AS laSoTruc, "
                + "ISNULL(pt.loaiPhieu, N'THU_TIEN') AS loaiPhieu, ISNULL(pt.daChotSo, 0) AS daChotSo, "
                + "ISNULL(h.trangThai, N'') AS trangThai, "
                + "ISNULL(svcMeta.moTaDichVu, N'') AS moTaDichVu "
                + "FROM dbo.PhieuThu pt "
                + "JOIN dbo.HoaDon h ON h.id = pt.maHoaDon "
                + "JOIN dbo.LuotKham l ON l.id = h.maLuotKham "
                + "JOIN dbo.BenhNhan p ON p.id = l.maBenhNhan "
                + "LEFT JOIN dbo.NhanVien nv ON nv.id = pt.nguoiThu "
                + "LEFT JOIN dbo.Khoa k ON k.id = l.maKhoa "
                + "LEFT JOIN dbo.PhongKham r ON r.id = l.maPhongKham "
                + "OUTER APPLY (SELECT COUNT(*) lineCount, "
                + "SUM(CASE WHEN UPPER(ISNULL(ct.doiTuongThanhToan, N'')) IN (N'BHYT', N'BHYT_DICH_VU') THEN 1 ELSE 0 END) bhytCount, "
                + "SUM(CASE WHEN UPPER(ISNULL(ct.doiTuongThanhToan, N'')) = N'BHYT_DICH_VU' THEN 1 ELSE 0 END) serviceCount, "
                + "SUM(CASE WHEN UPPER(ISNULL(ct.doiTuongThanhToan, N'')) IN (N'THU_PHI', N'MIEN_PHI') THEN 1 ELSE 0 END) feeCount "
                + "FROM dbo.ChiTietHoaDon ct WHERE ct.maHoaDon = h.id AND ct.isDeleted = 0) svc "
                + "OUTER APPLY (SELECT STUFF((SELECT N'; ' + ct2.moTa + N': ' "
                + "+ CAST(CAST(ct2.soLuong * ct2.donGia AS BIGINT) AS NVARCHAR(30)) + N'đ' "
                + "FROM dbo.ChiTietHoaDon ct2 WHERE ct2.maHoaDon = h.id AND ct2.isDeleted = 0 "
                + "ORDER BY ct2.id FOR XML PATH(''), TYPE).value('.', 'NVARCHAR(MAX)'), 1, 2, N'') AS moTaDichVu) svcMeta "
                + "WHERE h.maCoSo = ? AND pt.isDeleted = 0 AND h.isDeleted = 0 "
                + (filterByDate ? "AND " + dateColumn + " >= ? AND " + dateColumn + " <= ? " : "")
                + "ORDER BY " + orderBy;

        List<ReceiptReportRow> rows = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            if (filterByDate) {
                ps.setTimestamp(2, Timestamp.valueOf(from));
                ps.setTimestamp(3, Timestamp.valueOf(to));
            }
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("ngayThu");
                    Timestamp visitTs = rs.getTimestamp("thoiGianTiepNhan");
                    var dob = rs.getDate("ngaySinh");
                    rows.add(new ReceiptReportRow(
                            rs.getInt("id"),
                            rs.getInt("maVienPhi"),
                            rs.getString("soHoaDon"),
                            ts == null ? null : ts.toLocalDateTime(),
                            visitTs == null ? null : visitTs.toLocalDateTime(),
                            rs.getString("bn"),
                            dob == null ? null : dob.toLocalDate(),
                            rs.getString("gioiTinh"),
                            rs.getString("diaChi"),
                            rs.getString("thuNgan"),
                            rs.getString("hinhThuc"),
                            rs.getString("doiTuong"),
                            rs.getString("hinhThucDieuTri"),
                            rs.getString("loaiDangKyKham"),
                            rs.getString("tenKhoa"),
                            rs.getString("tenPhong"),
                            rs.getInt("bhytCount"),
                            rs.getInt("serviceCount"),
                            rs.getInt("feeCount"),
                            rs.getInt("lineCount"),
                            nz(rs.getBigDecimal("soTien")),
                            nz(rs.getBigDecimal("totalSoTienHoanTra")),
                            nz(rs.getBigDecimal("totalSoTienTamUng")),
                            nz(rs.getBigDecimal("soTienHoanTra")),
                            nz(rs.getBigDecimal("soTienTamUng")),
                            nz(rs.getBigDecimal("soTienGiamGia")),
                            rs.getString("lyDoGiamGia"),
                            rs.getString("ghiChu"),
                            rs.getString("moTaDichVu"),
                            rs.getString("soThu"),
                            rs.getBoolean("laSoTruc"),
                            rs.getString("loaiPhieu"),
                            rs.getBoolean("daChotSo"),
                            rs.getString("trangThai")
                    ));
                }
            }
            return List.copyOf(rows);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được dữ liệu báo cáo phiếu thu.", ex);
        }
    }

    @Override
    public List<String> listReceiptFilterValues(int facilityId, String filterKey) {
        String valueSql = switch (filterKey) {
            case "account", "detailAccount" -> "SELECT DISTINCT ISNULL(l.doiTuongKham, N'THU_PHI') value ";
            case "payment", "detailPayment" -> "SELECT DISTINCT ISNULL(pt.phuongThuc, N'TIEN_MAT') value ";
            case "cashier" -> "SELECT DISTINCT ISNULL(nv.ho + N' ' + nv.ten, N'Thu ngân') value ";
            case "department" -> "SELECT DISTINCT ISNULL(k.tenKhoa, N'') value ";
            case "room" -> "SELECT DISTINCT ISNULL(r.tenPhong, N'') value ";
            case "examType" -> "SELECT DISTINCT ISNULL(l.loaiDangKyKham, N'') value ";
            case "book" -> "SELECT DISTINCT ISNULL(pt.soThu, N'') value ";
            default -> null;
        };
        if (valueSql == null) return List.of();
        String joins = "FROM dbo.PhieuThu pt JOIN dbo.HoaDon h ON h.id=pt.maHoaDon "
                + "JOIN dbo.LuotKham l ON l.id=h.maLuotKham LEFT JOIN dbo.NhanVien nv ON nv.id=pt.nguoiThu "
                + "LEFT JOIN dbo.Khoa k ON k.id=l.maKhoa LEFT JOIN dbo.PhongKham r ON r.id=l.maPhongKham ";
        String sql = valueSql + joins + "WHERE h.maCoSo=? AND pt.isDeleted=0 AND h.isDeleted=0 ORDER BY value";
        List<String> values = new ArrayList<>();
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    String v = rs.getString(1);
                    if (v != null && !v.isBlank()) values.add(v);
                }
            }
            return List.copyOf(values);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được danh sách lọc.", ex);
        }
    }

    @Override
    public List<ServiceTypeReportRow> listServiceTypeReport(int facilityId, LocalDateTime from, LocalDateTime to,
                                                            Integer roomId, String groupType, String serviceName,
                                                            boolean groupByDate, boolean groupByRoom) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT CONVERT(VARCHAR(10), l.thoiGianTiepNhan, 103) AS ngay, ")
                .append("ISNULL(r.tenPhong, N'(Chưa phân phòng)') AS tenPhong, ")
                .append("ISNULL(ct.loaiDoiTuong, N'KHAC') AS loaiDichVu, ")
                .append("ISNULL(ct.moTa, N'') AS tenDichVu, ")
                .append("p.ho + N' ' + p.ten AS tenBenhNhan, ")
                .append("ISNULL(ct.soLuong, 1) AS soLuong, ")
                .append("ISNULL(ct.donGia, 0) AS donGia, ")
                .append("ISNULL(ct.soLuong * ct.donGia, 0) AS thanhTien, ")
                .append("ISNULL(ct.soTienMienGiam, 0) AS mienGiam, ")
                .append("ISNULL(ct.soTienBenhNhan, 0) AS thuCuaBn, ")
                .append("CASE WHEN ISNULL(h.soTienBenhNhanChiTra, 0) > 0 AND ISNULL(h.daThanhToan, 0) >= h.soTienBenhNhanChiTra ")
                .append("     THEN ISNULL(ct.soTienBenhNhan, 0) ")
                .append("     WHEN ISNULL(h.soTienBenhNhanChiTra, 0) > 0 ")
                .append("     THEN ROUND(ISNULL(ct.soTienBenhNhan, 0) * ISNULL(h.daThanhToan, 0) / h.soTienBenhNhanChiTra, 0) ")
                .append("     ELSE 0 END AS daThu ")
                .append("FROM dbo.ChiTietHoaDon ct ")
                .append("JOIN dbo.HoaDon h ON h.id = ct.maHoaDon AND h.isDeleted = 0 ")
                .append("JOIN dbo.LuotKham l ON l.id = h.maLuotKham AND l.isDeleted = 0 ")
                .append("JOIN dbo.BenhNhan p ON p.id = l.maBenhNhan AND p.isDeleted = 0 ")
                .append("LEFT JOIN dbo.PhongKham r ON r.id = l.maPhongKham ")
                .append("WHERE ct.isDeleted = 0 AND l.maCoSo = ? AND l.thoiGianTiepNhan >= ? AND l.thoiGianTiepNhan <= ? ");
        if (roomId != null) sql.append("AND l.maPhongKham = ? ");
        if (groupType != null) sql.append("AND ct.loaiDoiTuong = ? ");
        if (serviceName != null) sql.append("AND ct.moTa = ? ");
        sql.append("ORDER BY ").append(groupByDate ? "l.thoiGianTiepNhan DESC, " : "")
                .append(groupByRoom ? "tenPhong, " : "")
                .append("loaiDichVu, tenDichVu, ct.id");

        List<ServiceTypeReportRow> rows = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, facilityId);
            ps.setTimestamp(idx++, Timestamp.valueOf(from));
            ps.setTimestamp(idx++, Timestamp.valueOf(to));
            if (roomId != null) ps.setInt(idx++, roomId);
            if (groupType != null) ps.setString(idx++, groupType);
            if (serviceName != null) ps.setString(idx++, serviceName);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ServiceTypeReportRow(
                            rs.getString("ngay"),
                            rs.getString("tenPhong"),
                            formatServiceGroup(rs.getString("loaiDichVu")),
                            rs.getString("tenDichVu"),
                            rs.getString("tenBenhNhan"),
                            rs.getInt("soLuong"),
                            nz(rs.getBigDecimal("donGia")),
                            nz(rs.getBigDecimal("thanhTien")),
                            nz(rs.getBigDecimal("mienGiam")),
                            nz(rs.getBigDecimal("thuCuaBn")),
                            nz(rs.getBigDecimal("daThu"))
                    ));
                }
            }
            return List.copyOf(rows);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được báo cáo loại hình dịch vụ.", ex);
        }
    }

    @Override
    public List<String> listServiceNames() {
        List<String> services = new ArrayList<>();
        String sql = "SELECT DISTINCT TOP 100 moTa FROM dbo.ChiTietHoaDon WHERE isDeleted = 0 AND moTa IS NOT NULL AND moTa <> N'' ORDER BY moTa";
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) services.add(rs.getString(1));
            return List.copyOf(services);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được danh sách dịch vụ.", ex);
        }
    }

    @Override
    public List<RoomOption> listClinicRooms(int facilityId) {
        List<RoomOption> options = new ArrayList<>();
        String sql = "SELECT id, tenPhong FROM dbo.PhongKham WHERE isDeleted = 0 AND maCoSo = ? ORDER BY tenPhong";
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) options.add(new RoomOption(rs.getInt(1), rs.getString(2)));
            }
            return List.copyOf(options);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được danh sách phòng khám.", ex);
        }
    }

    @Override
    public List<RoomVisitDetail> listRoomReport(int facilityId, LocalDateTime from, LocalDateTime to,
                                                Integer roomId, boolean groupByDate, boolean withService, boolean paidOnly) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.id AS visitId, CONVERT(VARCHAR(10), l.thoiGianTiepNhan, 103) AS ngay, ")
                .append("ISNULL(r.tenPhong, N'(Chưa phân phòng)') AS tenPhong, ")
                .append("p.ho + N' ' + p.ten AS tenBenhNhan, p.ngaySinh, ISNULL(p.gioiTinh, N'') AS gioiTinh, ")
                .append("ISNULL(p.diaChi, N'') AS diaChi, ")
                .append("ISNULL(inv.tongTien, 0) AS thanhTien, ")
                .append("ISNULL(inv.giamGia, 0) AS mienGiam, ")
                .append("ISNULL(inv.thuBn, 0) AS thuCuaBn, ")
                .append("ISNULL(inv.daThu, 0) AS daThu ")
                .append("FROM dbo.LuotKham l ")
                .append("JOIN dbo.BenhNhan p ON p.id = l.maBenhNhan AND p.isDeleted = 0 ")
                .append("LEFT JOIN dbo.PhongKham r ON r.id = l.maPhongKham ")
                .append("OUTER APPLY (SELECT SUM(h.tongTien) tongTien, SUM(h.soTienGiamGia) giamGia, ")
                .append("             SUM(h.soTienBenhNhanChiTra) thuBn, SUM(h.daThanhToan) daThu ")
                .append("             FROM dbo.HoaDon h WHERE h.maLuotKham = l.id AND h.isDeleted = 0) inv ")
                .append("WHERE l.isDeleted = 0 AND l.maCoSo = ? AND l.thoiGianTiepNhan >= ? AND l.thoiGianTiepNhan <= ? ");
        if (roomId != null) sql.append("AND l.maPhongKham = ? ");
        if (paidOnly) sql.append("AND ISNULL(inv.daThu, 0) > 0 ");
        sql.append("ORDER BY ").append(groupByDate ? "l.thoiGianTiepNhan DESC, " : "").append("tenPhong, l.id DESC");

        Map<Integer, List<RoomServiceLine>> servicesByVisit = new HashMap<>();
        List<RoomVisitDetail> visits = new ArrayList<>();
        try (var lease = connections.acquire()) {
            if (withService) {
                String svcSql = "SELECT h.maLuotKham, ISNULL(ct.moTa, N'') AS moTa, ISNULL(ct.soLuong, 1) AS sl, "
                        + "ISNULL(ct.soLuong * ct.donGia, 0) AS thanhTien, "
                        + "ISNULL(ct.soTienMienGiam, 0) AS mienGiam, "
                        + "ISNULL(ct.soTienBenhNhan, 0) AS thuBn "
                        + "FROM dbo.ChiTietHoaDon ct "
                        + "JOIN dbo.HoaDon h ON h.id = ct.maHoaDon AND h.isDeleted = 0 "
                        + "JOIN dbo.LuotKham l ON l.id = h.maLuotKham AND l.isDeleted = 0 "
                        + "WHERE ct.isDeleted = 0 AND l.maCoSo = ? AND l.thoiGianTiepNhan >= ? AND l.thoiGianTiepNhan <= ?"
                        + (roomId != null ? " AND l.maPhongKham = ?" : "")
                        + (paidOnly ? " AND ISNULL(h.daThanhToan, 0) > 0" : "")
                        + " ORDER BY ct.id";
                try (var sps = lease.connection().prepareStatement(svcSql)) {
                    int sidx = 1;
                    sps.setInt(sidx++, facilityId);
                    sps.setTimestamp(sidx++, Timestamp.valueOf(from));
                    sps.setTimestamp(sidx++, Timestamp.valueOf(to));
                    if (roomId != null) sps.setInt(sidx, roomId);
                    try (var srs = sps.executeQuery()) {
                        while (srs.next()) {
                            servicesByVisit.computeIfAbsent(srs.getInt("maLuotKham"), k -> new ArrayList<>())
                                    .add(new RoomServiceLine(
                                            srs.getString("moTa"),
                                            srs.getInt("sl"),
                                            nz(srs.getBigDecimal("thanhTien")),
                                            nz(srs.getBigDecimal("mienGiam")),
                                            nz(srs.getBigDecimal("thuBn"))
                                    ));
                        }
                    }
                }
            }

            try (var ps = lease.connection().prepareStatement(sql.toString())) {
                int idx = 1;
                ps.setInt(idx++, facilityId);
                ps.setTimestamp(idx++, Timestamp.valueOf(from));
                ps.setTimestamp(idx++, Timestamp.valueOf(to));
                if (roomId != null) ps.setInt(idx, roomId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int visitId = rs.getInt("visitId");
                        var dob = rs.getDate("ngaySinh");
                        String yr = dob == null ? "" : String.valueOf(dob.toLocalDate().getYear());
                        String g = rs.getString("gioiTinh");
                        boolean fem = "F".equalsIgnoreCase(g) || "NU".equalsIgnoreCase(g) || "NỮ".equalsIgnoreCase(g);
                        visits.add(new RoomVisitDetail(
                                visitId,
                                rs.getString("ngay"),
                                rs.getString("tenPhong"),
                                rs.getString("tenBenhNhan"),
                                fem ? "" : yr,
                                fem ? yr : "",
                                rs.getString("diaChi"),
                                nz(rs.getBigDecimal("thanhTien")),
                                nz(rs.getBigDecimal("mienGiam")),
                                nz(rs.getBigDecimal("thuCuaBn")),
                                nz(rs.getBigDecimal("daThu")),
                                servicesByVisit.getOrDefault(visitId, List.of())
                        ));
                    }
                }
            }
            return List.copyOf(visits);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được báo cáo theo phòng khám.", ex);
        }
    }

    @Override
    public List<String> listService019FilterValues(int facilityId, String filterKey) {
        String sql = switch (filterKey) {
            case "ORDER_ROOM", "EXEC_ROOM" -> "SELECT DISTINCT tenPhong FROM dbo.PhongKham WHERE isDeleted = 0 AND maCoSo = ? ORDER BY tenPhong";
            case "SVC_GROUP" -> "SELECT DISTINCT loaiDoiTuong FROM dbo.ChiTietHoaDon WHERE isDeleted = 0 AND loaiDoiTuong IS NOT NULL ORDER BY loaiDoiTuong";
            case "SERVICE" -> "SELECT DISTINCT TOP 100 moTa FROM dbo.ChiTietHoaDon WHERE isDeleted = 0 AND loaiDoiTuong <> 'DON_THUOC' AND moTa IS NOT NULL ORDER BY moTa";
            case "DRUG_SUPPLY" -> "SELECT DISTINCT TOP 100 moTa FROM dbo.ChiTietHoaDon WHERE isDeleted = 0 AND loaiDoiTuong = 'DON_THUOC' AND moTa IS NOT NULL ORDER BY moTa";
            case "ACCT_GROUP", "SVC_SUBJECT" -> "SELECT DISTINCT doiTuongThanhToan FROM dbo.ChiTietHoaDon WHERE isDeleted = 0 AND doiTuongThanhToan IS NOT NULL ORDER BY doiTuongThanhToan";
            case "RECEIPT_BOOK" -> "SELECT DISTINCT soThu FROM dbo.PhieuThu WHERE isDeleted = 0 AND soThu IS NOT NULL AND soThu <> N'' ORDER BY soThu";
            case "CASHIER", "ORDER_DOCTOR" -> "SELECT DISTINCT ho + N' ' + ten FROM dbo.NhanVien WHERE isDeleted = 0 ORDER BY 1";
            default -> null;
        };
        if (sql == null) return List.of();
        List<String> options = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            if ("ORDER_ROOM".equals(filterKey) || "EXEC_ROOM".equals(filterKey)) ps.setInt(1, facilityId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    String v = rs.getString(1);
                    if (v != null && !v.isBlank()) options.add(v);
                }
            }
            return List.copyOf(options);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được danh sách lọc.", ex);
        }
    }

    @Override
    public List<Service019RawRow> listService019Report(int facilityId, int searchByIndex, LocalDateTime from, LocalDateTime to) {
        String dateCol = switch (searchByIndex) {
            case 1 -> "l.thoiGianTiepNhan";
            case 2 -> "h.ngayLap";
            default -> "COALESCE(pt.ngayThu, h.ngayLap, l.thoiGianTiepNhan)";
        };
        String sql = "SELECT CONVERT(VARCHAR(10), " + dateCol + ", 103) AS ngay, "
                + "ISNULL(r.tenPhong, N'(Chưa phân phòng)') AS tenPhong, "
                + "ISNULL(ct.loaiDoiTuong, N'KHAC') AS loaiDichVu, "
                + "ISNULL(ct.moTa, N'') AS tenDichVu, "
                + "ISNULL(p.maBenhNhanFormat, N'') AS maBN, "
                + "p.ho + N' ' + p.ten AS tenBenhNhan, "
                + "ISNULL(l.hinhThucDieuTri, N'') AS hinhThucDieuTri, "
                + "ISNULL(l.loaiDangKyKham, N'') AS loaiDangKyKham, "
                + "ISNULL(l.doiTuongKham, N'THU_PHI') AS doiTuongKham, "
                + "ISNULL(ct.doiTuongThanhToan, N'THU_PHI') AS doiTuongThanhToan, "
                + "ISNULL(pt.soThu, N'') AS soThu, "
                + "ISNULL(nv.ho + N' ' + nv.ten, N'') AS thuNgan, "
                + "ISNULL(bs.ho + N' ' + bs.ten, N'') AS nguoiThucHien, "
                + "ISNULL(ct.soLuong, 1) AS soLuong, "
                + "ISNULL(ct.donGia, 0) AS donGia, "
                + "ISNULL(ct.soLuong * ct.donGia, 0) AS thanhTien, "
                + "ISNULL(ct.soTienMienGiam, 0) AS mienGiam, "
                + "ISNULL(ct.soTienBenhNhan, 0) AS thuCuaBn, "
                + "CASE WHEN ISNULL(h.soTienBenhNhanChiTra, 0) > 0 AND ISNULL(h.daThanhToan, 0) >= h.soTienBenhNhanChiTra "
                + "     THEN ISNULL(ct.soTienBenhNhan, 0) ELSE 0 END AS daThu "
                + "FROM dbo.ChiTietHoaDon ct "
                + "JOIN dbo.HoaDon h ON h.id = ct.maHoaDon AND h.isDeleted = 0 "
                + "JOIN dbo.LuotKham l ON l.id = h.maLuotKham AND l.isDeleted = 0 "
                + "JOIN dbo.BenhNhan p ON p.id = l.maBenhNhan AND p.isDeleted = 0 "
                + "LEFT JOIN dbo.PhongKham r ON r.id = l.maPhongKham "
                + "OUTER APPLY (SELECT TOP 1 p2.ngayThu, p2.nguoiThu, p2.soThu FROM dbo.PhieuThu p2 WHERE p2.maHoaDon = h.id AND p2.isDeleted = 0 ORDER BY p2.id DESC) pt "
                + "LEFT JOIN dbo.NhanVien nv ON nv.id = pt.nguoiThu "
                + "LEFT JOIN dbo.NhanVien bs ON bs.id = l.maBacSi "
                + "WHERE ct.isDeleted = 0 AND l.maCoSo = ? AND " + dateCol + " >= ? AND " + dateCol + " <= ? "
                + "ORDER BY " + dateCol + " DESC, tenPhong, loaiDichVu, tenDichVu";

        List<Service019RawRow> rows = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Service019RawRow(
                            rs.getString("ngay"),
                            rs.getString("tenPhong"),
                            rs.getString("loaiDichVu"),
                            rs.getString("tenDichVu"),
                            rs.getString("maBN"),
                            rs.getString("tenBenhNhan"),
                            rs.getString("hinhThucDieuTri"),
                            rs.getString("loaiDangKyKham"),
                            rs.getString("doiTuongKham"),
                            rs.getString("doiTuongThanhToan"),
                            rs.getString("soThu"),
                            rs.getString("thuNgan"),
                            rs.getString("nguoiThucHien"),
                            rs.getInt("soLuong"),
                            nz(rs.getBigDecimal("donGia")),
                            nz(rs.getBigDecimal("thanhTien")),
                            nz(rs.getBigDecimal("mienGiam")),
                            nz(rs.getBigDecimal("thuCuaBn")),
                            nz(rs.getBigDecimal("daThu"))
                    ));
                }
            }
            return List.copyOf(rows);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được báo cáo dịch vụ (019).", ex);
        }
    }

    @Override
    public ClinicSummaryStats getClinicSummary(int facilityId, LocalDateTime from, LocalDateTime to) {
        String visitSql = "SELECT COUNT(*) FROM dbo.LuotKham WHERE isDeleted = 0 AND maCoSo = ? AND thoiGianTiepNhan >= ? AND thoiGianTiepNhan <= ?";
        String invSql = "SELECT ISNULL(SUM(h.tongTien), 0), ISNULL(SUM(h.daThanhToan), 0), ISNULL(SUM(h.soTienGiamGia), 0) "
                + "FROM dbo.HoaDon h JOIN dbo.LuotKham l ON l.id = h.maLuotKham AND l.isDeleted = 0 "
                + "WHERE h.isDeleted = 0 AND h.maCoSo = ? AND l.thoiGianTiepNhan >= ? AND l.thoiGianTiepNhan <= ?";
        String lineSql = "SELECT "
                + "ISNULL(SUM(CASE WHEN ct.loaiDoiTuong = 'KHAM_BENH' THEN ct.soLuong * ct.donGia ELSE 0 END), 0), "
                + "ISNULL(SUM(CASE WHEN ct.loaiDoiTuong = 'DICH_VU_CLS' AND (UPPER(ISNULL(ct.moTa, N'')) LIKE N'%XÉT NGHIỆM%' OR UPPER(ISNULL(ct.moTa, N'')) LIKE N'%XET NGHIEM%' OR UPPER(ISNULL(ct.moTa, N'')) LIKE N'%SINH HÓA%' OR UPPER(ISNULL(ct.moTa, N'')) LIKE N'%HUYẾT HỌC%') THEN ct.soLuong * ct.donGia ELSE 0 END), 0), "
                + "ISNULL(SUM(CASE WHEN ct.loaiDoiTuong = 'DICH_VU_CLS' AND NOT (UPPER(ISNULL(ct.moTa, N'')) LIKE N'%XÉT NGHIỆM%' OR UPPER(ISNULL(ct.moTa, N'')) LIKE N'%XET NGHIEM%' OR UPPER(ISNULL(ct.moTa, N'')) LIKE N'%SINH HÓA%' OR UPPER(ISNULL(ct.moTa, N'')) LIKE N'%HUYẾT HỌC%') THEN ct.soLuong * ct.donGia ELSE 0 END), 0), "
                + "ISNULL(SUM(CASE WHEN ct.loaiDoiTuong = 'DON_THUOC' THEN ct.soLuong * ct.donGia ELSE 0 END), 0), "
                + "ISNULL(SUM(ct.soTienMienGiam), 0) "
                + "FROM dbo.ChiTietHoaDon ct "
                + "JOIN dbo.HoaDon h ON h.id = ct.maHoaDon AND h.isDeleted = 0 "
                + "JOIN dbo.LuotKham l ON l.id = h.maLuotKham AND l.isDeleted = 0 "
                + "WHERE ct.isDeleted = 0 AND h.maCoSo = ? AND l.thoiGianTiepNhan >= ? AND l.thoiGianTiepNhan <= ?";

        int visits = 0;
        BigDecimal totalRev = BigDecimal.ZERO, collected = BigDecimal.ZERO, invDisc = BigDecimal.ZERO;
        BigDecimal examRev = BigDecimal.ZERO, labRev = BigDecimal.ZERO, imgRev = BigDecimal.ZERO,
                drugRev = BigDecimal.ZERO, svcDisc = BigDecimal.ZERO;

        try (var lease = connections.acquire()) {
            try (var ps = lease.connection().prepareStatement(visitSql)) {
                ps.setInt(1, facilityId);
                ps.setTimestamp(2, Timestamp.valueOf(from));
                ps.setTimestamp(3, Timestamp.valueOf(to));
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) visits = rs.getInt(1);
                }
            }
            try (var ps = lease.connection().prepareStatement(invSql)) {
                ps.setInt(1, facilityId);
                ps.setTimestamp(2, Timestamp.valueOf(from));
                ps.setTimestamp(3, Timestamp.valueOf(to));
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalRev = nz(rs.getBigDecimal(1));
                        collected = nz(rs.getBigDecimal(2));
                        invDisc = nz(rs.getBigDecimal(3));
                    }
                }
            }
            try (var ps = lease.connection().prepareStatement(lineSql)) {
                ps.setInt(1, facilityId);
                ps.setTimestamp(2, Timestamp.valueOf(from));
                ps.setTimestamp(3, Timestamp.valueOf(to));
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        examRev = nz(rs.getBigDecimal(1));
                        labRev = nz(rs.getBigDecimal(2));
                        imgRev = nz(rs.getBigDecimal(3));
                        drugRev = nz(rs.getBigDecimal(4));
                        svcDisc = nz(rs.getBigDecimal(5));
                    }
                }
            }
            return new ClinicSummaryStats(visits, totalRev, examRev, labRev, imgRev, drugRev, collected, svcDisc, invDisc);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được thống kê Quản Lý Phòng Khám.", ex);
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String formatServiceGroup(String code) {
        if (code == null) return "Khác";
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "DICH_VU_CLS" -> "Cận lâm sàng (CLS)";
            case "KHAM_BENH" -> "Khám bệnh";
            case "DON_THUOC" -> "Thuốc / Vật tư";
            default -> code;
        };
    }
}
