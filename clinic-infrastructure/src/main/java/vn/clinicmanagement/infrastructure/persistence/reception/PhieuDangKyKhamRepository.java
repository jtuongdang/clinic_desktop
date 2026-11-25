package vn.clinicmanagement.infrastructure.persistence.reception;

import vn.clinicmanagement.application.reception.*;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Đọc phiếu trực tiếp bằng JDBC, giới hạn cơ sở/khoa trong chính câu truy vấn. */
public final class PhieuDangKyKhamRepository implements IPhieuDangKyKhamRepository {
    private final JdbcKetNoiNhaCungCap connections;
    public PhieuDangKyKhamRepository(JdbcKetNoiNhaCungCap connections) { this.connections = connections; }
    static final String SQL = "SELECT l.id, p.maBenhNhanFormat, c.tenCoSo, c.diaChi facilityAddress, c.dienThoai, "
            + "p.ho + N' ' + p.ten patientName, p.ngaySinh, p.gioiTinh, "
            + "CONCAT(NULLIF(p.soNhaThonPho,''), CASE WHEN NULLIF(p.soNhaThonPho,'') IS NOT NULL AND NULLIF(p.diaChi,'') IS NOT NULL THEN N', ' ELSE N'' END, p.diaChi) patientAddress, "
            + "r.tenPhong, l.thoiGianTiepNhan, g.tenDichVu snapshotName, d.tenDichVu catalogName, g.soLuong, g.donGia, "
            + "paid.ngayThu, paid.collectorName FROM LuotKham l "
            + "INNER JOIN BenhNhan p ON p.id=l.maBenhNhan AND p.isDeleted=0 "
            + "INNER JOIN CoSo c ON c.id=l.maCoSo AND c.isDeleted=0 "
            + "LEFT JOIN PhongKham r ON r.id=l.maPhongKham "
            + "LEFT JOIN DangKyKhamGia g ON g.maLuotKham=l.id "
            + "LEFT JOIN DanhMucDichVu d ON d.id=l.maDichVuKham "
            + "OUTER APPLY (SELECT TOP 1 pt.ngayThu, nv.ho + N' ' + nv.ten collectorName "
            + "FROM HoaDon h INNER JOIN PhieuThu pt ON pt.maHoaDon=h.id AND pt.isDeleted=0 "
            + "LEFT JOIN NhanVien nv ON nv.id=pt.nguoiThu "
            + "WHERE h.maLuotKham=l.id AND h.maBenhNhan=l.maBenhNhan AND h.maCoSo=l.maCoSo AND h.isDeleted=0 AND pt.soTien>0 "
            + "ORDER BY pt.ngayThu DESC, pt.id DESC) paid "
            + "WHERE l.id=? AND l.maCoSo=? AND l.maKhoa=? AND l.isDeleted=0";
    @Override public Optional<PhieuDangKyKhamDuLieu> getByVisit(int visitId, int facilityId, int departmentId) {
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(SQL)) {
            ps.setInt(1, visitId); ps.setInt(2, facilityId); ps.setInt(3, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                List<PhieuDangKyKhamDuLieu.DongDichVu> lines = new ArrayList<>();
                String name = rs.getString("snapshotName");
                if (name == null) name = rs.getString("catalogName");
                if (name != null) lines.add(new PhieuDangKyKhamDuLieu.DongDichVu(name,
                        rs.getBigDecimal("soLuong") == null ? BigDecimal.ONE : rs.getBigDecimal("soLuong"), rs.getBigDecimal("donGia")));
                java.sql.Date birth = rs.getDate("ngaySinh");
                return Optional.of(new PhieuDangKyKhamDuLieu(rs.getInt("id"), rs.getString("maBenhNhanFormat"),
                        rs.getString("tenCoSo"), rs.getString("facilityAddress"), rs.getString("dienThoai"),
                        rs.getString("patientName"), birth == null ? null : birth.toLocalDate(),
                        switch (Objects.toString(rs.getString("gioiTinh"), "")) { case "M" -> "Nam"; case "F" -> "Nữ"; case "O" -> "Khác"; default -> ""; },
                        rs.getString("patientAddress"), rs.getString("tenPhong"), dateTime(rs, "thoiGianTiepNhan"),
                        dateTime(rs, "ngayThu"), rs.getString("collectorName"), lines));
            }
        } catch (SQLException error) { throw new IllegalStateException("Không thể đọc phiếu đăng ký khám.", error); }
    }
    private static LocalDateTime dateTime(ResultSet rs, String field) throws SQLException {
        Timestamp value = rs.getTimestamp(field); return value == null ? null : value.toLocalDateTime();
    }
}
