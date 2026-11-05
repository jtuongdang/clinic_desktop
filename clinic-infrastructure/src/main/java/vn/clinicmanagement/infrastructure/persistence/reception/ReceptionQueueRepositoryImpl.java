package vn.clinicmanagement.infrastructure.persistence.reception;

import vn.clinicmanagement.application.reception.ClinicRoomQueueSummary;
import vn.clinicmanagement.application.reception.ReceptionQueueItem;
import vn.clinicmanagement.application.reception.ReceptionQueuePort;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
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

/** JDBC writes for newly registered visits and read models for the reception queue. */
public final class ReceptionQueueRepositoryImpl implements ReceptionQueuePort {
    private final JdbcConnectionProvider connections;
    private final JdbcUnitOfWork unitOfWork;

    public ReceptionQueueRepositoryImpl(JdbcConnectionProvider connections, JdbcUnitOfWork unitOfWork) {
        this.connections = connections;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                                 String registrationType, String reason, DoiTuongKham payerType,
                                 boolean priority, int createdBy) {
        return withConnection(connection -> {
            LocalDate today = LocalDate.now();
            int queueNumber = nextQueueNumber(connection, roomId, today);
            String queueDisplay = String.format("%02d", queueNumber);
            LocalDateTime registeredAt = LocalDateTime.now();
            String sql = "INSERT INTO LuotKham (maBenhNhan, maCoSo, maKhoa, maPhongKham, trangThai, soThuTu, "
                    + "soThuTuDisplay, ngayKham, thoiGianTiepNhan, lyDoKham, doiTuongKham, loaiDangKyKham, dnVnTien, createdBy) "
                    + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            LuotKham visit = new LuotKham();
            visit.setMaBenhNhan(patientId);
            visit.setMaCoSo(facilityId);
            visit.setMaKhoa(departmentId);
            visit.setMaPhongKham(roomId);
            visit.setTrangThai(TrangThaiLuotKham.CHO_KHAM);
            visit.setSoThuTu(queueNumber);
            visit.setSoThuTuDisplay(queueDisplay);
            visit.setNgayKham(today);
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
                ps.setString(5, TrangThaiLuotKham.CHO_KHAM.name());
                ps.setInt(6, queueNumber);
                ps.setString(7, queueDisplay);
                ps.setObject(8, today);
                ps.setTimestamp(9, Timestamp.valueOf(registeredAt));
                ps.setString(10, reason == null ? "" : reason.trim());
                ps.setString(11, payerType.name());
                ps.setString(12, registrationType.trim());
                ps.setBoolean(13, priority);
                ps.setObject(14, createdBy > 0 ? createdBy : null, Types.INTEGER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Không nhận được ID lượt khám sau khi lưu.");
                    visit.setId(rs.getInt(1));
                }
            }
            unitOfWork.recordChanges(1);
            return visit;
        });
    }

    @Override
    public List<ReceptionQueueItem> getQueue(LocalDate date, int facilityId, int departmentId) {
        if (facilityId <= 0 || departmentId <= 0) return List.of();
        String sql = scopedQueueSql();
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setObject(1, date);
            ps.setInt(2, facilityId);
            ps.setInt(3, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ReceptionQueueItem> items = new ArrayList<>();
                while (rs.next()) items.add(new ReceptionQueueItem(rs.getInt("visitId"), rs.getInt("patientId"),
                        rs.getString("patientCode"), rs.getString("patientName"), rs.getInt("roomId"),
                        rs.getString("roomName"), rs.getInt("queueNumber"), rs.getString("queueDisplay"),
                        rs.getString("trangThai"), localDateTime(rs, "thoiGianTiepNhan")));
                return items;
            }
        } catch (SQLException ex) { throw new IllegalStateException("Không thể đọc hàng đợi tiếp đón.", ex); }
    }

    @Override
    public List<ClinicRoomQueueSummary> getRoomSummaries(LocalDate date, int facilityId, int departmentId) {
        if (facilityId <= 0 || departmentId <= 0) return List.of();
        String sql = scopedRoomSummariesSql();
        try (JdbcConnectionProvider.Lease lease = connections.acquire();
             PreparedStatement ps = lease.connection().prepareStatement(sql)) {
            ps.setObject(1, date);
            ps.setInt(2, facilityId);
            ps.setInt(3, departmentId);
            ps.setInt(4, facilityId);
            ps.setInt(5, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ClinicRoomQueueSummary> items = new ArrayList<>();
                while (rs.next()) items.add(new ClinicRoomQueueSummary(rs.getInt("roomId"), rs.getString("roomName"),
                        rs.getString("departmentName"), rs.getInt("registeredCount"), rs.getInt("waitingCount"),
                        rs.getInt("examiningCount")));
                return items;
            }
        } catch (SQLException ex) { throw new IllegalStateException("Không thể đọc thống kê phòng khám.", ex); }
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
        try (JdbcConnectionProvider.Lease lease = connections.acquire()) { return work.execute(lease.connection()); }
        catch (SQLException ex) { throw new IllegalStateException("Lỗi ghi hàng đợi tiếp đón.", ex); }
    }

    private static LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column); return value == null ? null : value.toLocalDateTime();
    }

    @FunctionalInterface private interface SqlWork<T> { T execute(Connection connection) throws SQLException; }
}
