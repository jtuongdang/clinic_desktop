package vn.clinicmanagement.infrastructure.persistence.reception;

import vn.clinicmanagement.application.reception.PhongKhamPhongHangDoiTongHop;
import vn.clinicmanagement.application.reception.BacSiHangDoiCongKetNoi;
import vn.clinicmanagement.application.reception.TiepDonHangDoiMatHang;
import vn.clinicmanagement.application.reception.TiepDonHangDoiCongKetNoi;
import vn.clinicmanagement.application.reception.TiepDonLichSuKhamMatHang;
import vn.clinicmanagement.application.reception.TiepDonKhamGia;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Ghi lượt khám mới bằng JDBC và cung cấp mô hình đọc cho hàng đợi tiếp đón. */
public final class TiepDonHangDoiRepository implements TiepDonHangDoiCongKetNoi, BacSiHangDoiCongKetNoi {
    public static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JdbcKetNoiNhaCungCap connections;
    private final JdbcUnitOfWork unitOfWork;
    private final Clock clock;

    public TiepDonHangDoiRepository(JdbcKetNoiNhaCungCap connections, JdbcUnitOfWork unitOfWork) {
        this(connections, unitOfWork, Clock.system(CLINIC_ZONE));
    }

    public TiepDonHangDoiRepository(JdbcKetNoiNhaCungCap connections, JdbcUnitOfWork unitOfWork, Clock clock) {
        this.connections = Objects.requireNonNull(connections, "connections");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork");
        this.clock = Objects.requireNonNull(clock, "clock").withZone(CLINIC_ZONE);
    }

    public Clock clock() {
        return clock;
    }

    @Override
    public LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                                 String registrationType, String reason, DoiTuongKham payerType,
                                 boolean priority, int createdBy) {
        return registerVisit(patientId, facilityId, departmentId, roomId, null,
                registrationType, reason, payerType, priority, createdBy);
    }

    @Override
    public LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                                 Integer serviceId, String registrationType, String reason,
                                 DoiTuongKham payerType, boolean priority, int createdBy) {
        if (serviceId != null && serviceId > 0 && !connections.isTransactionActive())
            throw new IllegalStateException("Đăng ký có dịch vụ phải lưu cùng giao dịch chốt giá.");
        return withConnection(connection -> {
            LocalDateTime businessNow = LocalDateTime.now(clock);
            LocalDate visitDate = businessNow.toLocalDate();
            LocalDateTime registeredAt = businessNow;
            int queueNumber = nextQueueNumber(connection, roomId, visitDate);
            String queueDisplay = String.format("%02d", queueNumber);
            Integer normalizedServiceId = serviceId != null && serviceId > 0 ? serviceId : null;
            String sql = "INSERT INTO LuotKham (maBenhNhan, maCoSo, maKhoa, maPhongKham, maDichVuKham, trangThai, soThuTu, "
                    + "soThuTuDisplay, ngayKham, thoiGianTiepNhan, lyDoKham, doiTuongKham, loaiDangKyKham, dnVnTien, createdBy) "
                    + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            LuotKham visit = new LuotKham();
            visit.setMaBenhNhan(patientId);
            visit.setMaCoSo(facilityId);
            visit.setMaKhoa(departmentId);
            visit.setMaPhongKham(roomId);
            visit.setMaDichVuKham(normalizedServiceId);
            visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);
            visit.setSoThuTu(queueNumber);
            visit.setSoThuTuDisplay(queueDisplay);
            visit.setNgayKham(visitDate);
            visit.setThoiGianTiepNhan(registeredAt);
            visit.setLyDoKham(reason == null ? "" : reason.trim());
            visit.setDoiTuongKham(payerType);
            visit.setLoaiDangKyKham(registrationType.trim());
            visit.setDnVnTien(priority);
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, patientId);
                ps.setInt(2, facilityId);
                ps.setInt(3, departmentId);
                ps.setInt(4, roomId);
                ps.setObject(5, normalizedServiceId, Types.INTEGER);
                ps.setString(6, TrangThaiLuotKham.CHO_KHAM.name());
                ps.setInt(7, queueNumber);
                ps.setString(8, queueDisplay);
                ps.setObject(9, visitDate);
                ps.setTimestamp(10, Timestamp.valueOf(registeredAt));
                ps.setString(11, reason == null ? "" : reason.trim());
                ps.setString(12, payerType.name());
                ps.setString(13, registrationType.trim());
                ps.setBoolean(14, priority);
                ps.setObject(15, createdBy > 0 ? createdBy : null, Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Không nhận được ID lượt khám sau khi lưu.");
                    visit.setId(rs.getInt(1));
                }
            }
            if (normalizedServiceId != null) {
                // Chốt giá thu phí tiêu chuẩn ngay khi đăng ký; quyền lợi BHYT được quyết toán ở viện phí.
                String snapshotSql = "INSERT INTO DangKyKhamGia (maLuotKham, maDichVu, tenDichVu, soLuong, donGia, createdBy) "
                        + "SELECT ?, id, tenDichVu, 1, donGia, ? FROM DanhMucDichVu WITH (HOLDLOCK) "
                        + "WHERE id=? AND hoatDong=1 AND isDeleted=0 AND loaiDichVu IN ('KHAM','KHAM_BENH')";
                try (PreparedStatement snapshot = connection.prepareStatement(snapshotSql)) {
                    snapshot.setInt(1, visit.getId()); snapshot.setInt(2, createdBy); snapshot.setInt(3, normalizedServiceId);
                    if (snapshot.executeUpdate() != 1) throw new SQLException("Không thể chốt giá dịch vụ khám.");
                }
                recordChange();
            }
            recordChange();
            return visit;
        });
    }

    /** Đọc giá dịch vụ từ bản chụp đã ghi trong giao dịch đăng ký hiện tại. */
    @Override
    public Optional<TiepDonKhamGia> getRegistrationPrice(int visitId, int serviceId) {
        if (visitId <= 0 || serviceId <= 0 || !connections.isTransactionActive()) return Optional.empty();
        String sql = "SELECT maLuotKham, maDichVu, tenDichVu, donGia FROM DangKyKhamGia "
                + "WHERE maLuotKham=? AND maDichVu=?";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, visitId);
            ps.setInt(2, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new TiepDonKhamGia(rs.getInt("maLuotKham"), rs.getInt("maDichVu"),
                        rs.getString("tenDichVu"), rs.getBigDecimal("donGia")));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể đọc giá dịch vụ đã chốt cho lượt khám.", ex);
        }
    }

    @Override
    public List<TiepDonHangDoiMatHang> getQueue(LocalDate date, int facilityId, int departmentId) {
        if (facilityId <= 0 || departmentId <= 0) return List.of();
        String sql = scopedQueueSql();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setObject(1, date);
            ps.setInt(2, facilityId);
            ps.setInt(3, departmentId);
            return readQueueItems(ps);
        } catch (SQLException ex) { throw new IllegalStateException("Không thể đọc hàng đợi tiếp đón.", ex); }
    }

    @Override
    public List<TiepDonHangDoiMatHang> getQueue(LocalDate fromDate, LocalDate toDate, int facilityId, int departmentId) {
        if (facilityId <= 0 || departmentId <= 0) return List.of();
        LocalDate start = fromDate != null ? fromDate : LocalDate.now(clock);
        LocalDate end = toDate != null ? toDate : start;
        if (start.isAfter(end)) { LocalDate tmp = start; start = end; end = tmp; }
        if (start.equals(end)) return getQueue(start, facilityId, departmentId);
        String sql = "SELECT l.id visitId, p.id patientId, p.maBenhNhanFormat patientCode, "
                + "CONCAT(p.ho, N' ', p.ten) patientName, r.id roomId, r.tenPhong roomName, "
                + "ISNULL(l.soThuTu,0) queueNumber, ISNULL(l.soThuTuDisplay,'') queueDisplay, l.trangThai, l.thoiGianTiepNhan "
                + "FROM LuotKham l INNER JOIN BenhNhan p ON p.id=l.maBenhNhan "
                + "LEFT JOIN PhongKham r ON r.id=l.maPhongKham "
                + "WHERE l.ngayKham>=? AND l.ngayKham<=? AND l.maCoSo=? AND l.maKhoa=? AND l.isDeleted=0 "
                + "AND l.trangThai<>'DA_HUY' AND p.isDeleted=0 "
                + "ORDER BY l.ngayKham DESC, l.dnVnTien DESC, l.soThuTu, l.id";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setObject(1, start);
            ps.setObject(2, end);
            ps.setInt(3, facilityId);
            ps.setInt(4, departmentId);
            return readQueueItems(ps);
        } catch (SQLException ex) { throw new IllegalStateException("Không thể đọc hàng đợi tiếp đón theo khoảng ngày.", ex); }
    }

    @Override
    public List<TiepDonHangDoiMatHang> getFacilityBillingQueue(LocalDate date, int facilityId) {
        if (date == null || facilityId <= 0) return List.of();
        String sql = facilityBillingQueueSql();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setObject(1, date);
            ps.setInt(2, facilityId);
            return readQueueItems(ps);
        } catch (SQLException ex) { throw new IllegalStateException("Không thể đọc danh sách lượt khám thu ngân.", ex); }
    }

    @Override
    public List<TiepDonHangDoiMatHang> getFacilityBillingQueue(LocalDate fromDate, LocalDate toDate, int facilityId) {
        if (facilityId <= 0) return List.of();
        LocalDate start = fromDate != null ? fromDate : LocalDate.now(clock);
        LocalDate end = toDate != null ? toDate : start;
        if (start.isAfter(end)) { LocalDate tmp = start; start = end; end = tmp; }
        if (start.equals(end)) return getFacilityBillingQueue(start, facilityId);
        String sql = "SELECT l.id visitId, p.id patientId, p.maBenhNhanFormat patientCode, "
                + "CONCAT(p.ho, N' ', p.ten) patientName, r.id roomId, r.tenPhong roomName, "
                + "ISNULL(l.soThuTu,0) queueNumber, ISNULL(l.soThuTuDisplay,'') queueDisplay, l.trangThai, l.thoiGianTiepNhan "
                + "FROM LuotKham l INNER JOIN BenhNhan p ON p.id=l.maBenhNhan "
                + "LEFT JOIN PhongKham r ON r.id=l.maPhongKham "
                + "WHERE l.ngayKham>=? AND l.ngayKham<=? AND l.maCoSo=? AND l.isDeleted=0 "
                + "AND l.trangThai<>'DA_HUY' AND p.isDeleted=0 "
                + "ORDER BY l.ngayKham DESC, l.dnVnTien DESC, l.soThuTu, l.id";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setObject(1, start);
            ps.setObject(2, end);
            ps.setInt(3, facilityId);
            return readQueueItems(ps);
        } catch (SQLException ex) { throw new IllegalStateException("Không thể đọc danh sách lượt khám thu ngân theo khoảng ngày.", ex); }
    }

    private List<TiepDonHangDoiMatHang> readQueueItems(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<TiepDonHangDoiMatHang> items = new ArrayList<>();
            while (rs.next()) items.add(new TiepDonHangDoiMatHang(rs.getInt("visitId"), rs.getInt("patientId"),
                    rs.getString("patientCode"), rs.getString("patientName"), rs.getInt("roomId"),
                    rs.getString("roomName"), rs.getInt("queueNumber"), rs.getString("queueDisplay"),
                    rs.getString("trangThai"), localDateTime(rs, "thoiGianTiepNhan")));
            return items;
        }
    }

    @Override
    public List<TiepDonHangDoiMatHang> getActiveDoctorQueue(LocalDate date, int facilityId, int departmentId) {
        if (date == null || facilityId <= 0 || departmentId <= 0) return List.of();
        String sql = activeDoctorQueueSql();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setObject(1, date);
            ps.setInt(2, facilityId);
            ps.setInt(3, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<TiepDonHangDoiMatHang> items = new ArrayList<>();
                while (rs.next()) items.add(new TiepDonHangDoiMatHang(rs.getInt("visitId"), rs.getInt("patientId"),
                        rs.getString("patientCode"), rs.getString("patientName"), rs.getInt("roomId"),
                        rs.getString("roomName"), rs.getInt("queueNumber"), rs.getString("queueDisplay"),
                        rs.getString("trangThai"), localDateTime(rs, "thoiGianTiepNhan")));
                return items;
            }
        } catch (SQLException ex) { throw new IllegalStateException("Không thể đọc hàng đợi bác sĩ.", ex); }
    }

    @Override
    public List<PhongKhamPhongHangDoiTongHop> getRoomSummaries(LocalDate date, int facilityId, int departmentId) {
        if (facilityId <= 0 || departmentId <= 0) return List.of();
        String sql = scopedRoomSummariesSql();
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setObject(1, date);
            ps.setInt(2, facilityId);
            ps.setInt(3, departmentId);
            ps.setInt(4, facilityId);
            ps.setInt(5, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PhongKhamPhongHangDoiTongHop> items = new ArrayList<>();
                while (rs.next()) items.add(new PhongKhamPhongHangDoiTongHop(rs.getInt("roomId"), rs.getString("roomName"),
                        rs.getString("departmentName"), rs.getInt("registeredCount"), rs.getInt("waitingCount"),
                        rs.getInt("examiningCount")));
                return items;
            }
        } catch (SQLException ex) { throw new IllegalStateException("Không thể đọc thống kê phòng khám.", ex); }
    }

    @Override
    public List<TiepDonLichSuKhamMatHang> getPatientVisitHistory(int patientId, int facilityId, int departmentId) {
        if (patientId <= 0 || facilityId <= 0) return List.of();
        String sql = "SELECT l.id visitId, l.maBenhNhan patientId, ISNULL(r.tenPhong, N'') roomName, "
                + "ISNULL(l.soThuTu, 0) queueNumber, ISNULL(l.soThuTuDisplay, '') queueDisplay, "
                + "ISNULL(l.trangThai, '') trangThai, l.thoiGianTiepNhan, l.ngayHenTaiKham, "
                + "l.ngayKham, CASE WHEN nv.id IS NOT NULL THEN CONCAT(nv.ho, N' ', nv.ten) ELSE N'' END doctorName, "
                + "l.thoiGianKetThucKham, ISNULL(l.lyDoKham, N'') lyDoKham "
                + "FROM LuotKham l "
                + "LEFT JOIN PhongKham r ON r.id = l.maPhongKham "
                + "LEFT JOIN NhanVien nv ON nv.id = l.maBacSi "
                + "WHERE l.maBenhNhan = ? AND l.maCoSo = ? AND l.isDeleted = 0 "
                + "ORDER BY l.thoiGianTiepNhan DESC, l.id DESC";
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, facilityId);
            try (ResultSet rs = ps.executeQuery()) {
                List<TiepDonLichSuKhamMatHang> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(new TiepDonLichSuKhamMatHang(
                            rs.getInt("visitId"),
                            rs.getInt("patientId"),
                            rs.getString("roomName"),
                            rs.getInt("queueNumber"),
                            rs.getString("queueDisplay"),
                            rs.getString("trangThai"),
                            localDateTime(rs, "thoiGianTiepNhan"),
                            localDate(rs, "ngayHenTaiKham"),
                            localDate(rs, "ngayKham"),
                            rs.getString("doctorName"),
                            localDateTime(rs, "thoiGianKetThucKham"),
                            rs.getString("lyDoKham")
                    ));
                }
                return items;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể đọc lịch sử khám của người bệnh.", ex);
        }
    }

    static String scopedQueueSql() {
        return "SELECT l.id visitId, p.id patientId, p.maBenhNhanFormat patientCode, "
                + "CONCAT(p.ho, N' ', p.ten) patientName, r.id roomId, r.tenPhong roomName, "
                + "ISNULL(l.soThuTu,0) queueNumber, ISNULL(l.soThuTuDisplay,'') queueDisplay, l.trangThai, l.thoiGianTiepNhan "
                + "FROM LuotKham l INNER JOIN BenhNhan p ON p.id=l.maBenhNhan "
                + "LEFT JOIN PhongKham r ON r.id=l.maPhongKham "
                + "WHERE l.ngayKham=? AND l.maCoSo=? AND l.maKhoa=? AND l.isDeleted=0 "
                + "AND l.trangThai<>'DA_HUY' AND p.isDeleted=0 "
                + "ORDER BY l.dnVnTien DESC, l.soThuTu, l.id";
    }

    static String facilityBillingQueueSql() {
        return "SELECT l.id visitId, p.id patientId, p.maBenhNhanFormat patientCode, "
                + "CONCAT(p.ho, N' ', p.ten) patientName, r.id roomId, r.tenPhong roomName, "
                + "ISNULL(l.soThuTu,0) queueNumber, ISNULL(l.soThuTuDisplay,'') queueDisplay, l.trangThai, l.thoiGianTiepNhan "
                + "FROM LuotKham l INNER JOIN BenhNhan p ON p.id=l.maBenhNhan "
                + "LEFT JOIN PhongKham r ON r.id=l.maPhongKham "
                + "WHERE l.ngayKham=? AND l.maCoSo=? AND l.isDeleted=0 "
                + "AND l.trangThai<>'DA_HUY' AND p.isDeleted=0 "
                + "ORDER BY l.dnVnTien DESC, l.soThuTu, l.id";
    }

    static String activeDoctorQueueSql() {
        return "SELECT l.id visitId, p.id patientId, p.maBenhNhanFormat patientCode, "
                + "CONCAT(p.ho, N' ', p.ten) patientName, r.id roomId, r.tenPhong roomName, "
                + "ISNULL(l.soThuTu,0) queueNumber, ISNULL(l.soThuTuDisplay,'') queueDisplay, l.trangThai, l.thoiGianTiepNhan "
                + "FROM LuotKham l INNER JOIN BenhNhan p ON p.id=l.maBenhNhan "
                + "LEFT JOIN PhongKham r ON r.id=l.maPhongKham "
                + "WHERE l.ngayKham=? AND l.maCoSo=? AND l.maKhoa=? AND l.isDeleted=0 "
                + "AND l.trangThai IN ('CHO_KHAM','DANG_KHAM','CHO_CLS') AND p.isDeleted=0 "
                + "ORDER BY l.dnVnTien DESC, l.soThuTu, l.id";
    }

    static String scopedRoomSummariesSql() {
        return "SELECT p.id roomId, p.tenPhong roomName, k.tenKhoa departmentName, "
                + "COUNT(b.id) registeredCount, SUM(CASE WHEN b.id IS NOT NULL AND l.trangThai='CHO_KHAM' THEN 1 ELSE 0 END) waitingCount, "
                + "SUM(CASE WHEN b.id IS NOT NULL AND l.trangThai='DANG_KHAM' THEN 1 ELSE 0 END) examiningCount "
                + "FROM PhongKham p INNER JOIN Khoa k ON k.id=p.maKhoa INNER JOIN CoSo c ON c.id=k.maCoSo "
                + "LEFT JOIN LuotKham l ON l.maPhongKham=p.id AND l.ngayKham=? AND l.maCoSo=? AND l.maKhoa=? "
                + "AND l.isDeleted=0 AND l.trangThai<>'DA_HUY' "
                + "LEFT JOIN BenhNhan b ON b.id=l.maBenhNhan AND b.isDeleted=0 "
                + "WHERE p.hoatDong=1 AND p.isDeleted=0 AND k.hoatDong=1 AND k.isDeleted=0 "
                + "AND c.hoatDong=1 AND c.isDeleted=0 AND k.maCoSo=? AND k.id=? "
                + "GROUP BY p.id, p.tenPhong, k.tenKhoa ORDER BY k.tenKhoa, p.tenPhong";
    }

    private int nextQueueNumber(Connection connection, int roomId, LocalDate date) throws SQLException {
        String sql = "SELECT ISNULL(MAX(soThuTu), 0) + 1 FROM LuotKham WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE maPhongKham=? AND ngayKham=? AND isDeleted=0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setObject(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Không cấp được số thứ tự phòng khám.");
                return rs.getInt(1);
            }
        }
    }

    private <T> T withConnection(SqlWork<T> work) {
        try (JdbcKetNoiNhaCungCap.Lease lease = connections.acquire()) { return work.execute(lease.connection()); }
        catch (SQLException ex) { throw new IllegalStateException("Lỗi ghi hàng đợi tiếp đón.", ex); }
    }

    private void recordChange() {
        if (connections.isTransactionActive()) unitOfWork.recordChanges(1);
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column); return value == null ? null : value.toLocalDateTime();
    }

    private static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column); return value == null ? null : value.toLocalDate();
    }

    @FunctionalInterface private interface SqlWork<T> { T execute(Connection connection) throws SQLException; }
}
