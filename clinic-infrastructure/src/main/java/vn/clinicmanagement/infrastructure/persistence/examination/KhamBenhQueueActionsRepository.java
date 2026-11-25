package vn.clinicmanagement.infrastructure.persistence.examination;

import vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Triển khai SQL Server cho các thao tác ngữ cảnh trên hàng đợi Khám Bệnh & Buồng giường nội trú. */
public final class KhamBenhQueueActionsRepository implements KhamBenhQueueActionsPort {

    private final JdbcKetNoiNhaCungCap connections;

    public KhamBenhQueueActionsRepository(JdbcKetNoiNhaCungCap connections) {
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    @Override
    public void callPatientToRoom(int visitId, int facilityId, int actorId) {
        String sql = """
                UPDATE dbo.LuotKham
                SET trangThai = CASE WHEN trangThai = 'CHO_KHAM' THEN 'DANG_KHAM' ELSE trangThai END,
                    thoiGianBatDauKham = ISNULL(thoiGianBatDauKham, GETDATE()),
                    updatedAt = SYSUTCDATETIME(), updatedBy = ?
                WHERE id = ? AND maCoSo = ? AND isDeleted = 0
                """;
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, actorId);
            ps.setInt(2, visitId);
            ps.setInt(3, facilityId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể gọi bệnh nhân vào phòng khám.", ex);
        }
    }

    @Override
    public void bookFollowUpAppointment(int visitId, int patientId, int doctorId, int facilityId, LocalDateTime date, String reason) {
        String sql = """
                INSERT INTO dbo.LichHen (maBenhNhan, maBacSi, maCoSo, thoiGianBatDau, thoiGianKetThuc, trangThai, lyDo, nguonDat, createdBy)
                VALUES (?, ?, ?, ?, ?, 'CHO_XAC_NHAN', ?, 'BAC_SI', ?)
                """;
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setInt(3, facilityId);
            ps.setTimestamp(4, Timestamp.valueOf(date));
            ps.setTimestamp(5, Timestamp.valueOf(date.plusMinutes(30)));
            ps.setString(6, reason);
            ps.setInt(7, doctorId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể đặt lịch hẹn tái khám.", ex);
        }
    }

    @Override
    public GuardianInfo getGuardianInfo(int patientId) {
        String sql = "SELECT ISNULL(nguoiLienHeTen, N'') ten, ISNULL(nguoiLienHeDienThoai, N'') sdt FROM dbo.BenhNhan WHERE id = ? AND isDeleted = 0";
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new GuardianInfo(text(rs, "ten"), text(rs, "sdt"));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải thông tin người liên hệ.", ex);
        }
        return new GuardianInfo("", "");
    }

    @Override
    public void updateGuardianInfo(int patientId, GuardianInfo info, int actorId) {
        String sql = "UPDATE dbo.BenhNhan SET nguoiLienHeTen = ?, nguoiLienHeDienThoai = ?, updatedAt = SYSUTCDATETIME(), updatedBy = ? WHERE id = ? AND isDeleted = 0";
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setString(1, info.contactName());
            ps.setString(2, info.contactPhone());
            ps.setInt(3, actorId);
            ps.setInt(4, patientId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể cập nhật thông tin bố, mẹ, người nhà.", ex);
        }
    }

    @Override
    public String getVisitNote(int visitId) {
        String sql = "SELECT ISNULL(loiDanBacSi, N'') note FROM dbo.LuotKham WHERE id = ? AND isDeleted = 0";
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, visitId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return text(rs, "note");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải thông tin ghi chú.", ex);
        }
        return "";
    }

    @Override
    public void updateVisitNote(int visitId, String note, int actorId) {
        String sql = "UPDATE dbo.LuotKham SET loiDanBacSi = ?, updatedAt = SYSUTCDATETIME(), updatedBy = ? WHERE id = ? AND isDeleted = 0";
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setString(1, note);
            ps.setInt(2, actorId);
            ps.setInt(3, visitId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể lưu ghi chú lượt khám.", ex);
        }
    }

    @Override
    public List<ReexamHistoryRow> listReexamHistory(int patientId, int facilityId) {
        String sql = """
                SELECT l.id, ISNULL(l.maLuotKham, CAST(l.id AS VARCHAR(20))) maLuotKham,
                       l.thoiGianTiepNhan,
                       ISNULL(pk.tenPhong, N'') tenPhong,
                       ISNULL((SELECT TOP 1 cd.tenChanDoan FROM dbo.ChanDoan cd WHERE cd.maLuotKham = l.id AND cd.isDeleted = 0 ORDER BY cd.laChinh DESC, cd.id), N'') diagnosis,
                       ISNULL(l.trangThai, N'') trangThai
                FROM dbo.LuotKham l
                LEFT JOIN dbo.PhongKham pk ON pk.id = l.maPhongKham
                WHERE l.maBenhNhan = ? AND l.maCoSo = ? AND l.isDeleted = 0
                ORDER BY l.thoiGianTiepNhan DESC, l.id DESC
                """;
        List<ReexamHistoryRow> rows = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, facilityId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ReexamHistoryRow(
                            rs.getInt("id"),
                            text(rs, "maLuotKham"),
                            ts(rs, "thoiGianTiepNhan"),
                            text(rs, "tenPhong"),
                            text(rs, "diagnosis"),
                            text(rs, "trangThai")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải lịch sử khám lại.", ex);
        }
        return List.copyOf(rows);
    }

    @Override
    public PatientCardData getPatientCardData(int visitId, int facilityId) {
        String sql = """
                SELECT l.id, ISNULL(l.maLuotKham, CAST(l.id AS VARCHAR(20))) maLuotKham,
                       ISNULL(b.maBenhNhanFormat, CAST(b.id AS VARCHAR(20))) maBenhNhan,
                       b.ho + N' ' + b.ten AS patientName, b.ngaySinh,
                       ISNULL(b.gioiTinh, N'') gioiTinh, ISNULL(b.dienThoai, N'') dienThoai,
                       ISNULL(b.diaChi, N'') diaChi,
                       ISNULL(k.tenKhoa, N'Khoa Khám Bệnh') tenKhoa,
                       ISNULL(pk.tenPhong, N'') tenPhong,
                       ISNULL(l.doiTuongKham, N'THU_PHI') doiTuongKham,
                       ISNULL((SELECT TOP 1 bh.soThe FROM dbo.BaoHiem bh WHERE bh.maBenhNhan = b.id AND bh.isDeleted = 0 ORDER BY bh.id DESC), N'') soThe,
                       l.thoiGianTiepNhan
                FROM dbo.LuotKham l
                JOIN dbo.BenhNhan b ON b.id = l.maBenhNhan
                LEFT JOIN dbo.Khoa k ON k.id = l.maKhoa
                LEFT JOIN dbo.PhongKham pk ON pk.id = l.maPhongKham
                WHERE l.id = ? AND l.maCoSo = ? AND l.isDeleted = 0
                """;
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, visitId);
            ps.setInt(2, facilityId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date dob = rs.getDate("ngaySinh");
                    return new PatientCardData(
                            rs.getInt("id"),
                            text(rs, "maLuotKham"),
                            text(rs, "maBenhNhan"),
                            text(rs, "patientName"),
                            dob != null ? dob.toLocalDate() : null,
                            text(rs, "gioiTinh"),
                            text(rs, "dienThoai"),
                            text(rs, "diaChi"),
                            text(rs, "tenKhoa"),
                            text(rs, "tenPhong"),
                            text(rs, "doiTuongKham"),
                            text(rs, "soThe"),
                            ts(rs, "thoiGianTiepNhan")
                    );
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải thông tin thẻ/vòng tay bệnh nhân.", ex);
        }
        return null;
    }

    @Override
    public void changeVisitSubject(int visitId, String subjectType, int actorId) {
        String sql = "UPDATE dbo.LuotKham SET doiTuongKham = ?, updatedAt = SYSUTCDATETIME(), updatedBy = ? WHERE id = ? AND isDeleted = 0";
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setString(1, subjectType);
            ps.setInt(2, actorId);
            ps.setInt(3, visitId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể đổi đối tượng khám.", ex);
        }
    }

    @Override
    public EmrSyncSummary syncEmrSummary(int visitId, int facilityId, int actorId) {
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            int vitals = countByVisit(c, "SELECT COUNT(*) FROM dbo.SinhHieu WHERE maLuotKham = ? AND isDeleted = 0", visitId);
            int diags = countByVisit(c, "SELECT COUNT(*) FROM dbo.ChanDoan WHERE maLuotKham = ? AND isDeleted = 0", visitId);
            int orders = countByVisit(c, "SELECT COUNT(*) FROM dbo.ChiDinhDichVu WHERE maLuotKham = ? AND isDeleted = 0", visitId);
            int rxs = countByVisit(c, "SELECT COUNT(*) FROM dbo.DonThuoc WHERE maLuotKham = ? AND isDeleted = 0", visitId);
            try (var up = c.prepareStatement("UPDATE dbo.LuotKham SET updatedAt = SYSUTCDATETIME(), updatedBy = ? WHERE id = ? AND maCoSo = ? AND isDeleted = 0")) {
                up.setInt(1, actorId);
                up.setInt(2, visitId);
                up.setInt(3, facilityId);
                up.executeUpdate();
            }
            return new EmrSyncSummary(visitId, vitals, diags, orders, rxs, LocalDateTime.now());
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể đồng bộ dữ liệu EMR.", ex);
        }
    }

    @Override
    public List<InpatientAdmissionRow> listInpatientAdmissions(int facilityId) {
        String sql = """
                SELECT l.id, ISNULL(l.maLuotKham, CAST(l.id AS VARCHAR(20))) maLuotKham,
                       ISNULL(b.maBenhNhanFormat, CAST(b.id AS VARCHAR(20))) maBenhNhan,
                       b.ho + N' ' + b.ten AS patientName,
                       ISNULL(b.gioiTinh, N'') gioiTinh,
                       ISNULL(k.tenKhoa, N'Khoa Nội Trú') tenKhoa,
                       ISNULL(l.huongDieuTri, N'') huongDieuTri,
                       ISNULL(l.tomTatBenhAn, N'') bedInfo,
                       l.thoiGianTiepNhan
                FROM dbo.LuotKham l
                JOIN dbo.BenhNhan b ON b.id = l.maBenhNhan
                LEFT JOIN dbo.Khoa k ON k.id = l.maKhoa
                WHERE l.maCoSo = ? AND l.isDeleted = 0
                  AND (l.huongXuTri = 'NHAP_VIEN_NOI_TRU' OR l.hinhThucDieuTri = 'NOI_TRU')
                ORDER BY l.thoiGianTiepNhan DESC, l.id DESC
                """;
        List<InpatientAdmissionRow> list = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new InpatientAdmissionRow(
                            rs.getInt("id"),
                            text(rs, "maLuotKham"),
                            text(rs, "maBenhNhan"),
                            text(rs, "patientName"),
                            text(rs, "gioiTinh"),
                            text(rs, "tenKhoa"),
                            text(rs, "huongDieuTri"),
                            text(rs, "bedInfo"),
                            ts(rs, "thoiGianTiepNhan")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh sách nhập viện nội trú.", ex);
        }
        return List.copyOf(list);
    }

    @Override
    public void assignInpatientBed(int visitId, String wardName, String bedCode, String note, int actorId) {
        String bedSummary = "Buồng: " + Objects.toString(wardName, "") + " | Giường: " + Objects.toString(bedCode, "")
                + (note != null && !note.isBlank() ? " (" + note.trim() + ")" : "");
        String sql = "UPDATE dbo.LuotKham SET tomTatBenhAn = ?, updatedAt = SYSUTCDATETIME(), updatedBy = ? WHERE id = ? AND isDeleted = 0";
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setString(1, bedSummary);
            ps.setInt(2, actorId);
            ps.setInt(3, visitId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể phân buồng/giường nội trú.", ex);
        }
    }

    private static int countByVisit(java.sql.Connection c, String sql, int visitId) throws SQLException {
        try (var ps = c.prepareStatement(sql)) {
            ps.setInt(1, visitId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static String text(ResultSet rs, String col) throws SQLException {
        String s = rs.getString(col);
        return s == null ? "" : s;
    }

    private static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }
}
