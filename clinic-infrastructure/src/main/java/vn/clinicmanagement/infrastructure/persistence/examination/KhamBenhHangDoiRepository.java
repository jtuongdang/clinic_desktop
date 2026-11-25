package vn.clinicmanagement.infrastructure.persistence.examination;

import vn.clinicmanagement.application.examination.KhamBenhHangDoiCongKetNoi;
import vn.clinicmanagement.application.examination.KhamBenhHangDoiMatHang;
import vn.clinicmanagement.application.examination.KhamBenhLichSuMatHang;
import vn.clinicmanagement.domain.common.enums.HinhThucDieuTri;
import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.ChanDoan;
import vn.clinicmanagement.domain.examination.ICD;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.facility.PhongKhamRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Triển khai JDBC truy vấn hàng đợi khám bệnh trong ngày và lịch sử lượt khám của người bệnh.
 */
public final class KhamBenhHangDoiRepository implements KhamBenhHangDoiCongKetNoi {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JdbcKetNoiNhaCungCap connections;

    public KhamBenhHangDoiRepository(JdbcKetNoiNhaCungCap connections) {
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    @Override
    public List<KhamBenhHangDoiMatHang> getTodayQueue(int facilityId, int doctorId) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        return getQueueInternal(facilityId, 0, doctorId > 0 ? "DOCTOR" : null,
                doctorId, 0, 0, today, today, false);
    }

    @Override
    public List<KhamBenhHangDoiMatHang> getTodayQueue(int facilityId, int employeeId, String roleCode,
                                                       int departmentId, int roomId) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        return getQueue(facilityId, employeeId, roleCode, departmentId, roomId, today, today);
    }

    @Override
    public List<KhamBenhHangDoiMatHang> getQueue(int facilityId, int employeeId, String roleCode,
                                                  int departmentId, int roomId,
                                                  LocalDate fromDate, LocalDate toDate) {
        if (employeeId <= 0 || roleCode == null
                || (!"DOCTOR".equalsIgnoreCase(roleCode) && !"ADMIN".equalsIgnoreCase(roleCode))) {
            return List.of();
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate start = fromDate != null ? fromDate : today;
        LocalDate end = toDate != null ? toDate : start;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        int doctorId = "DOCTOR".equalsIgnoreCase(roleCode) ? employeeId : 0;
        return getQueueInternal(facilityId, employeeId, roleCode, doctorId,
                departmentId, roomId, start, end, true);
    }

    private List<KhamBenhHangDoiMatHang> getQueueInternal(int facilityId, int employeeId, String roleCode,
                                                          int doctorId, int departmentId, int roomId,
                                                          LocalDate fromDate, LocalDate toDate,
                                                          boolean enforceAssignedScope) {
        if (facilityId <= 0) {
            return List.of();
        }
        String sql = "SELECT bn.id, bn.maBenhNhanFormat, bn.ho, bn.ten, bn.gioiTinh, bn.ngaySinh, "
                + "       bn.diaChi, bn.dienThoai, bn.danToc, bn.ngheNghiep, bn.quocTich, bn.soCCCD, "
                + "       lk.id AS maLuotKham, lk.trangThai, lk.loaiDangKyKham, "
                + "       lk.soThuTuDisplay, lk.doiTuongKham, pk.tenPhong, kp.tenKhoa, "
                + "       ISNULL(NULLIF(LTRIM(RTRIM(CONCAT(nv.ho, N' ', nv.ten))), N''), N'Chưa phân công') AS tenBacSi, "
                + "       lk.thoiGianTiepNhan, lk.thoiGianKetThucKham, "
                + "       COALESCE(NULLIF(diag.chanDoanDisplay, N''), lk.lyDoKham, N'') AS lyDoKham, "
                + "       ISNULL(bh.soBaoHiem, '') AS soBaoHiem, ISNULL(bh.mucHuong, 0) AS mucHuong "
                + "FROM BenhNhan bn "
                + "INNER JOIN LuotKham lk ON lk.maBenhNhan = bn.id AND lk.isDeleted = 0 "
                + "LEFT JOIN PhongKham pk ON lk.maPhongKham = pk.id "
                + "LEFT JOIN Khoa kp ON lk.maKhoa = kp.id "
                + "LEFT JOIN NhanVien nv ON lk.maBacSi = nv.id "
                + "OUTER APPLY (SELECT TOP 1 CASE WHEN icd.maICD IS NOT NULL AND icd.maICD <> '' "
                + "             THEN icd.maICD + N' - ' + cd.chanDoanText ELSE cd.chanDoanText END AS chanDoanDisplay "
                + "             FROM ChanDoan cd LEFT JOIN ICD icd ON icd.id = cd.maICD "
                + "             WHERE cd.maLuotKham = lk.id AND cd.isDeleted = 0 "
                + "             ORDER BY cd.laChinh DESC, cd.id DESC) diag "
                + "OUTER APPLY (SELECT TOP 1 b.soBaoHiem, b.mucHuong FROM BaoHiem b "
                + "             WHERE b.maBenhNhan = bn.id AND b.isDeleted = 0 "
                + "             ORDER BY b.ngayHetHan DESC, b.id DESC) bh "
                + "WHERE bn.isDeleted = 0 "
                + "AND lk.ngayKham >= ? AND lk.ngayKham <= ? AND lk.maCoSo = ? "
                + "AND (? = 0 OR lk.maBacSi = ? OR lk.maBacSi IS NULL) "
                + "AND (? = 0 OR lk.maKhoa = ?) "
                + "AND (? = 0 OR lk.maPhongKham = ?) ";
        if (enforceAssignedScope) {
            sql += "AND EXISTS (SELECT 1 FROM PhanQuyenCoSo pq "
                    + "INNER JOIN NhanVienKhoa nk ON nk.maNhanVien = pq.maNhanVien "
                    + "INNER JOIN Khoa allowedK ON allowedK.id = nk.maKhoa AND allowedK.maCoSo = pq.maCoSo "
                    + "WHERE pq.maNhanVien = ? AND pq.maCoSo = lk.maCoSo AND pq.isDeleted = 0 "
                    + "AND nk.isDeleted = 0 AND allowedK.isDeleted = 0 AND allowedK.hoatDong = 1 "
                    + "AND allowedK.id = lk.maKhoa) ";
            if ("DOCTOR".equalsIgnoreCase(roleCode)) {
                sql += "AND lk.maBacSi = ? ";
            }
        }
        sql += "ORDER BY lk.ngayKham DESC, lk.id DESC, bn.id DESC";

        List<KhamBenhHangDoiMatHang> items = new ArrayList<>();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setObject(1, fromDate);
            ps.setObject(2, toDate);
            ps.setInt(3, facilityId);
            ps.setInt(4, doctorId);
            ps.setInt(5, doctorId);
            ps.setInt(6, departmentId);
            ps.setInt(7, departmentId);
            ps.setInt(8, roomId);
            ps.setInt(9, roomId);
            if (enforceAssignedScope) {
                ps.setInt(10, employeeId);
                if ("DOCTOR".equalsIgnoreCase(roleCode)) ps.setInt(11, employeeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fullName = (nullToEmpty(rs.getString("ho")) + " " + nullToEmpty(rs.getString("ten")))
                            .trim().toUpperCase();
                    items.add(new KhamBenhHangDoiMatHang(
                            rs.getInt("id"),
                            rs.getString("maBenhNhanFormat"),
                            fullName,
                            rs.getString("gioiTinh"),
                            localDate(rs, "ngaySinh"),
                            nullToEmpty(rs.getString("diaChi")),
                            nullToEmpty(rs.getString("dienThoai")),
                            nullToEmpty(rs.getString("danToc")),
                            nullToEmpty(rs.getString("ngheNghiep")),
                            nullToEmpty(rs.getString("quocTich")),
                            nullToEmpty(rs.getString("soCCCD")),
                            rs.getInt("maLuotKham"),
                            rs.getString("soThuTuDisplay"),
                            rs.getString("trangThai"),
                            nullToEmpty(rs.getString("loaiDangKyKham")),
                            rs.getString("doiTuongKham"),
                            nullToEmpty(rs.getString("tenKhoa")),
                            nullToEmpty(rs.getString("tenPhong")),
                            nullToEmpty(rs.getString("tenBacSi")),
                            localDateTime(rs, "thoiGianTiepNhan"),
                            localDateTime(rs, "thoiGianKetThucKham"),
                            nullToEmpty(rs.getString("lyDoKham")),
                            nullToEmpty(rs.getString("soBaoHiem")),
                            rs.getInt("mucHuong")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh sách bệnh nhân khám bệnh.", ex);
        }
        return items;
    }

    @Override
    public List<Khoa> getAuthorizedDepartments(int facilityId, int employeeId) {
        if (facilityId <= 0 || employeeId <= 0) return List.of();
        String sql = "SELECT DISTINCT k.id, k.maCoSo, k.tenKhoa FROM Khoa k "
                + "INNER JOIN NhanVienKhoa nk ON nk.maKhoa = k.id "
                + "INNER JOIN PhanQuyenCoSo pq ON pq.maNhanVien = nk.maNhanVien AND pq.maCoSo = k.maCoSo "
                + "INNER JOIN CoSo cs ON cs.id = k.maCoSo "
                + "WHERE nk.maNhanVien = ? AND k.maCoSo = ? AND nk.isDeleted = 0 AND pq.isDeleted = 0 "
                + "AND k.isDeleted = 0 AND k.hoatDong = 1 AND cs.isDeleted = 0 AND cs.hoatDong = 1 "
                + "ORDER BY k.tenKhoa";
        List<Khoa> departments = new ArrayList<>();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, facilityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Khoa department = new Khoa();
                    department.setId(rs.getInt("id"));
                    department.setMaCoSo(rs.getInt("maCoSo"));
                    department.setTenKhoa(nullToEmpty(rs.getString("tenKhoa")));
                    department.setHoatDong(true);
                    departments.add(department);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh sách khoa được phân công.", ex);
        }
        return departments;
    }

    @Override
    public List<KhamBenhLichSuMatHang> getPatientVisitHistory(int patientId) {
        if (patientId <= 0) {
            return List.of();
        }
        String sql = "SELECT l.id, l.trangThai, l.thoiGianTiepNhan, l.thoiGianKetThucKham, "
                + "       ISNULL(l.loaiDangKyKham, N'Khám bệnh') AS loaiDangKyKham, "
                + "       ISNULL(l.doiTuongKham, 'THU_PHI') AS doiTuongKham, "
                + "       ISNULL(kp.tenKhoa, N'Khoa Khám Bệnh') AS tenKhoa, "
                + "       ISNULL(pk.tenPhong, N'') AS tenPhong, "
                + "       ISNULL(bh.soBaoHiem, '') AS soBaoHiem "
                + "FROM LuotKham l "
                + "LEFT JOIN Khoa kp ON kp.id = l.maKhoa "
                + "LEFT JOIN PhongKham pk ON pk.id = l.maPhongKham "
                + "OUTER APPLY (SELECT TOP 1 b.soBaoHiem FROM BaoHiem b "
                + "             WHERE b.maBenhNhan = l.maBenhNhan AND b.isDeleted = 0 "
                + "             ORDER BY b.ngayHetHan DESC, b.id DESC) bh "
                + "WHERE l.maBenhNhan = ? AND l.isDeleted = 0 "
                + "ORDER BY l.thoiGianTiepNhan DESC, l.id DESC";

        List<KhamBenhLichSuMatHang> history = new ArrayList<>();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new KhamBenhLichSuMatHang(
                            rs.getInt("id"),
                            rs.getString("trangThai"),
                            localDateTime(rs, "thoiGianTiepNhan"),
                            localDateTime(rs, "thoiGianKetThucKham"),
                            nullToEmpty(rs.getString("loaiDangKyKham")),
                            rs.getString("doiTuongKham"),
                            nullToEmpty(rs.getString("tenKhoa")),
                            nullToEmpty(rs.getString("tenPhong")),
                            nullToEmpty(rs.getString("soBaoHiem"))
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải lịch sử lượt khám của bệnh nhân.", ex);
        }
        return history;
    }

    @Override
    public List<ICD> searchIcdCatalog(String keyword) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, maICD, tenBenh, moTa, hoatDong FROM ICD WHERE isDeleted = 0 AND hoatDong = 1");
        String q = keyword == null ? "" : keyword.trim();
        if (!q.isEmpty()) {
            sql.append(" AND (LOWER(maICD) LIKE ? OR LOWER(tenBenh) LIKE ? OR LOWER(ISNULL(moTa, N'')) LIKE ?)");
        }
        sql.append(" ORDER BY maICD ASC");

        List<ICD> list = new ArrayList<>();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql.toString())) {
            if (!q.isEmpty()) {
                String pattern = "%" + q.toLowerCase(java.util.Locale.ROOT) + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ICD icd = new ICD();
                    icd.setId(rs.getInt("id"));
                    icd.setMaICD(nullToEmpty(rs.getString("maICD")));
                    icd.setTenBenh(nullToEmpty(rs.getString("tenBenh")));
                    icd.setMoTa(rs.getString("moTa"));
                    icd.setHoatDong(rs.getBoolean("hoatDong"));
                    list.add(icd);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh mục ICD-10 từ CSDL.", ex);
        }
        return list;
    }

    @Override
    public Optional<LuotKham> getVisitDetail(int visitId) {
        if (visitId <= 0) return Optional.empty();
        String visitSql = "SELECT id, maBenhNhan, maBacSi, maCoSo, maKhoa, maPhongKham, hinhThucDieuTri, trangThai, "
                + "thoiGianBatDauKham, thoiGianKetThucKham, thoiGianTiepNhan, lyDoKham, tienSuBanThan, tienSuGiaDinh, quaTrinhBenhLy, khamToanThan, khamBoPhan, "
                + "huongDieuTri, loiDanBacSi, ketQuaDieuTri, huongXuTri, ngayHenTaiKham, soNgayNghiHuongBHXH "
                + "FROM LuotKham WHERE id = ? AND isDeleted = 0";
        String diagSql = "SELECT cd.id, cd.maLuotKham, cd.maICD, cd.chanDoanText, cd.laChinh, cd.loaiChanDoan, "
                + "       icd.maICD AS icdCode, icd.tenBenh AS icdName "
                + "FROM ChanDoan cd LEFT JOIN ICD icd ON icd.id = cd.maICD "
                + "WHERE cd.maLuotKham = ? AND cd.isDeleted = 0 ORDER BY cd.laChinh DESC, cd.id ASC";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(visitSql)) {
            ps.setInt(1, visitId);
            LuotKham visit = null;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    visit = new LuotKham();
                    visit.setId(rs.getInt("id"));
                    visit.setMaBenhNhan(rs.getInt("maBenhNhan"));
                    visit.setMaBacSi((Integer) rs.getObject("maBacSi"));
                    visit.setMaCoSo(rs.getInt("maCoSo"));
                    visit.setMaKhoa(rs.getInt("maKhoa"));
                    visit.setMaPhongKham((Integer) rs.getObject("maPhongKham"));
                    String htdt = rs.getString("hinhThucDieuTri");
                    if (htdt != null) visit.setHinhThucDieuTri(HinhThucDieuTri.valueOf(htdt));
                    String st = rs.getString("trangThai");
                    if (st != null) visit.setTrangThai(TrangThaiLuotKham.valueOf(st));
                    visit.setThoiGianBatDauKham(localDateTime(rs, "thoiGianBatDauKham"));
                    visit.setThoiGianKetThucKham(localDateTime(rs, "thoiGianKetThucKham"));
                    visit.setThoiGianTiepNhan(localDateTime(rs, "thoiGianTiepNhan"));
                    visit.setLyDoKham(rs.getString("lyDoKham"));
                    visit.setTienSuBanThan(rs.getString("tienSuBanThan"));
                    visit.setTienSuGiaDinh(rs.getString("tienSuGiaDinh"));
                    visit.setQuaTrinhBenhLy(rs.getString("quaTrinhBenhLy"));
                    visit.setKhamToanThan(rs.getString("khamToanThan"));
                    visit.setKhamBoPhan(rs.getString("khamBoPhan"));
                    visit.setHuongDieuTri(rs.getString("huongDieuTri"));
                    visit.setLoiDanBacSi(rs.getString("loiDanBacSi"));
                    String kq = rs.getString("ketQuaDieuTri");
                    if (kq != null) visit.setKetQuaDieuTri(KetQuaDieuTri.valueOf(kq));
                    String hxt = rs.getString("huongXuTri");
                    if (hxt != null) visit.setHuongXuTri(HuongXuTri.valueOf(hxt));
                    visit.setNgayHenTaiKham(localDate(rs, "ngayHenTaiKham"));
                    visit.setSoNgayNghiHuongBHXH((Integer) rs.getObject("soNgayNghiHuongBHXH"));
                }
            }
            if (visit == null) return Optional.empty();
            try (PreparedStatement dps = lease.connection().prepareStatement(diagSql)) {
                dps.setInt(1, visitId);
                try (ResultSet drs = dps.executeQuery()) {
                    while (drs.next()) {
                        ChanDoan cd = new ChanDoan();
                        cd.setId(drs.getInt("id"));
                        cd.setMaLuotKham(visitId);
                        cd.setMaICD((Integer) drs.getObject("maICD"));
                        cd.setChanDoanText(nullToEmpty(drs.getString("chanDoanText")));
                        cd.setLaChinh(drs.getBoolean("laChinh"));
                        cd.setLoaiChanDoan(drs.getString("loaiChanDoan"));
                        String icdCode = drs.getString("icdCode");
                        if (icdCode != null && !icdCode.isBlank()) {
                            ICD icd = new ICD();
                            icd.setId(cd.getMaICD());
                            icd.setMaICD(icdCode);
                            icd.setTenBenh(nullToEmpty(drs.getString("icdName")));
                            cd.setIcd(icd);
                        }
                        visit.getChanDoans().add(cd);
                    }
                }
            }
            return Optional.of(visit);
        } catch (SQLException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<PhongKham> getActiveClinicRooms() {
        try {
            return new PhongKhamRepository(connections).getActiveRooms();
        } catch (Exception ex) {
            return List.of();
        }
    }

    @Override
    public List<PhongKham> getActiveClinicRooms(int facilityId) {
        return new PhongKhamRepository(connections).getActiveRoomsInFacility(facilityId);
    }

    @Override
    public List<PhongKham> getActiveClinicRooms(int facilityId, int departmentId) {
        if (facilityId <= 0 || departmentId <= 0) return List.of();
        try {
            return new PhongKhamRepository(connections).getActiveRoomsInScope(facilityId, departmentId);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
