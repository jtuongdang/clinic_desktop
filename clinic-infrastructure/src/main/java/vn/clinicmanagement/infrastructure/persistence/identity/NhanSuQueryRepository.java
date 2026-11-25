package vn.clinicmanagement.infrastructure.persistence.identity;

import vn.clinicmanagement.application.hr.NhanSuReadPort;
import vn.clinicmanagement.application.hr.NhanVienVaiTroPhanCong;
import vn.clinicmanagement.application.hr.PBKDF2NhanVienMatKhauBoBam;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Truy vấn & cập nhật SQL Server cho phân hệ Nhân sự, Chấm công và Tiền lương. */
public final class NhanSuQueryRepository implements NhanSuReadPort, NhanVienVaiTroPhanCong {

    private final JdbcKetNoiNhaCungCap connections;

    public NhanSuQueryRepository(JdbcKetNoiNhaCungCap connections) {
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    @Override
    public List<EmployeeRow> listEmployees(int facilityId) {
        String sql = """
                SELECT nv.id, ISNULL(nv.maNhanVien, N'') maNhanVien,
                       LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N'')))) fullName,
                       ISNULL(nv.username, N'') username,
                       ISNULL(nv.soDienThoai, N'') soDienThoai,
                       ISNULL(nv.email, N'') email,
                       ISNULL(cv.tenChucVu, N'') tenChucVu,
                       nv.hoatDong
                FROM dbo.NhanVien nv
                LEFT JOIN dbo.ChucVu cv ON cv.id = nv.maChucVu AND cv.isDeleted = 0
                WHERE nv.isDeleted = 0
                ORDER BY nv.ten, nv.ho, nv.id
                """;
        List<EmployeeRow> baseRows = new ArrayList<>();
        java.util.Map<Integer, List<String>> rolesByEmp = new java.util.HashMap<>();
        java.util.Map<Integer, List<String>> deptsByEmp = new java.util.HashMap<>();
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            try (var ps = c.prepareStatement(
                    "SELECT nvt.maNhanVien, vt.tenVaiTro FROM dbo.NhanVienVaiTro nvt JOIN dbo.VaiTro vt ON vt.id = nvt.maVaiTro WHERE nvt.isDeleted = 0 AND vt.isDeleted = 0");
                 var rs = ps.executeQuery()) {
                while (rs.next()) {
                    rolesByEmp.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>()).add(text(rs, "tenVaiTro"));
                }
            }
            try (var ps = c.prepareStatement(
                    "SELECT nvk.maNhanVien, k.tenKhoa FROM dbo.NhanVienKhoa nvk JOIN dbo.Khoa k ON k.id = nvk.maKhoa WHERE nvk.isDeleted = 0 AND k.isDeleted = 0 AND k.maCoSo = ?")) {
                ps.setInt(1, facilityId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        deptsByEmp.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>()).add(text(rs, "tenKhoa"));
                    }
                }
            }
            try (var ps = c.prepareStatement(sql);
                 var rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    baseRows.add(new EmployeeRow(
                            id,
                            text(rs, "maNhanVien"),
                            text(rs, "fullName"),
                            text(rs, "username"),
                            text(rs, "soDienThoai"),
                            text(rs, "email"),
                            text(rs, "tenChucVu"),
                            String.join(", ", rolesByEmp.getOrDefault(id, List.of())),
                            String.join(", ", deptsByEmp.getOrDefault(id, List.of())),
                            rs.getBoolean("hoatDong")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh sách nhân viên.", ex);
        }
        return List.copyOf(baseRows);
    }

    @Override
    public List<RoleOption> listRoles() {
        String sql = "SELECT id, tenVaiTro FROM dbo.VaiTro WHERE isDeleted = 0 ORDER BY id";
        List<RoleOption> roles = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = text(rs, "tenVaiTro");
                roles.add(new RoleOption(rs.getInt("id"), name, name));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh sách vai trò.", ex);
        }
        return List.copyOf(roles);
    }

    @Override
    public List<PositionOption> listPositions() {
        String sql = "SELECT id, tenChucVu FROM dbo.ChucVu WHERE isDeleted = 0 ORDER BY id";
        List<PositionOption> list = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PositionOption(rs.getInt("id"), text(rs, "tenChucVu")));
            }
        } catch (SQLException ex) {
            return List.of();
        }
        return List.copyOf(list);
    }


    @Override
    public List<AttendanceRow> listAttendance(int facilityId, int month, int year) {
        String sql = """
                SELECT cc.id, ISNULL(nv.maNhanVien, N'') maNhanVien,
                       LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N'')))) fullName,
                       llv.ngayLamViec, ISNULL(ct.tenCa, N'Ca hành chính') tenCa,
                       cc.thoiGianCheckIn, cc.thoiGianCheckOut,
                       ISNULL(cc.trangThai, N'DUNG_GIO') trangThai,
                       ISNULL(cc.soGioLam, 0) soGioLam,
                       ISNULL(cc.soGioDiMuon, 0) soGioDiMuon
                FROM dbo.ChamCong cc
                JOIN dbo.LichLamViec llv ON llv.id = cc.maLichLamViec
                JOIN dbo.NhanVien nv ON nv.id = llv.maNhanVien
                LEFT JOIN dbo.CaTruc ct ON ct.id = llv.maCaTruc
                WHERE cc.isDeleted = 0 AND llv.isDeleted = 0
                  AND llv.maCoSo = ? AND MONTH(llv.ngayLamViec) = ? AND YEAR(llv.ngayLamViec) = ?
                ORDER BY llv.ngayLamViec DESC, cc.id DESC
                """;
        List<AttendanceRow> rows = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("ngayLamViec");
                    rows.add(new AttendanceRow(
                            rs.getInt("id"),
                            text(rs, "maNhanVien"),
                            text(rs, "fullName"),
                            d != null ? d.toLocalDate() : LocalDate.now(),
                            text(rs, "tenCa"),
                            ts(rs, "thoiGianCheckIn"),
                            ts(rs, "thoiGianCheckOut"),
                            text(rs, "trangThai"),
                            money(rs, "soGioLam"),
                            money(rs, "soGioDiMuon")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải bảng chấm công.", ex);
        }
        return List.copyOf(rows);
    }

    @Override
    public void recordCheckInOrOut(int employeeId, int facilityId, boolean checkOut, int actorId) {
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            LocalDate today = LocalDate.now();
            Integer scheduleId = null;
            LocalTime shiftStart = LocalTime.of(7, 30);
            LocalTime shiftEnd = LocalTime.of(17, 0);
            try (var ps = c.prepareStatement(
                    "SELECT TOP 1 llv.id, ct.gioBatDau, ct.gioKetThuc FROM dbo.LichLamViec llv "
                            + "JOIN dbo.CaTruc ct ON ct.id = llv.maCaTruc "
                            + "WHERE llv.maNhanVien = ? AND llv.maCoSo = ? AND llv.ngayLamViec = ? AND llv.isDeleted = 0 ORDER BY llv.id DESC")) {
                ps.setInt(1, employeeId);
                ps.setInt(2, facilityId);
                ps.setDate(3, Date.valueOf(today));
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        scheduleId = rs.getInt("id");
                        Time st = rs.getTime("gioBatDau");
                        Time et = rs.getTime("gioKetThuc");
                        if (st != null) shiftStart = st.toLocalTime();
                        if (et != null) shiftEnd = et.toLocalTime();
                    }
                }
            }
            if (scheduleId == null) {
                Integer shiftId = null;
                try (var ps = c.prepareStatement("SELECT TOP 1 id, gioBatDau, gioKetThuc FROM dbo.CaTruc WHERE isDeleted = 0 AND hoatDong = 1 ORDER BY id")) {
                    try (var rs = ps.executeQuery()) {
                        if (rs.next()) {
                            shiftId = rs.getInt("id");
                            Time st = rs.getTime("gioBatDau");
                            Time et = rs.getTime("gioKetThuc");
                            if (st != null) shiftStart = st.toLocalTime();
                            if (et != null) shiftEnd = et.toLocalTime();
                        }
                    }
                }
                if (shiftId == null) {
                    throw new IllegalStateException("Chưa có ca trực hoạt động trong bảng CaTruc.");
                }
                try (var ps = c.prepareStatement(
                        "INSERT INTO dbo.LichLamViec (maNhanVien, maCaTruc, maCoSo, ngayLamViec, trangThai, createdBy) OUTPUT INSERTED.id VALUES (?, ?, ?, ?, 'DA_DIEM_DANH', ?)")) {
                    ps.setInt(1, employeeId);
                    ps.setInt(2, shiftId);
                    ps.setInt(3, facilityId);
                    ps.setDate(4, Date.valueOf(today));
                    ps.setInt(5, actorId);
                    try (var rs = ps.executeQuery()) {
                        if (rs.next()) scheduleId = rs.getInt(1);
                    }
                }
            }

            LocalDateTime now = LocalDateTime.now();
            if (!checkOut) {
                LocalDateTime shiftStartDt = LocalDateTime.of(today, shiftStart);
                long lateMins = Math.max(0L, Duration.between(shiftStartDt, now).toMinutes());
                BigDecimal lateHours = lateMins > 5
                        ? BigDecimal.valueOf(lateMins / 60.0).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                String status = lateMins > 5 ? "DI_MUON" : "DUNG_GIO";
                try (var ps = c.prepareStatement(
                        "IF NOT EXISTS (SELECT 1 FROM dbo.ChamCong WHERE maLichLamViec = ? AND isDeleted = 0) "
                                + "INSERT INTO dbo.ChamCong (maLichLamViec, thoiGianCheckIn, trangThai, soGioDiMuon, createdBy) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, scheduleId);
                    ps.setInt(2, scheduleId);
                    ps.setTimestamp(3, Timestamp.valueOf(now));
                    ps.setString(4, status);
                    ps.setBigDecimal(5, lateHours);
                    ps.setInt(6, actorId);
                    ps.executeUpdate();
                }
            } else {
                LocalDateTime checkInTime = null;
                Integer ccId = null;
                String existingStatus = "DUNG_GIO";
                try (var ps = c.prepareStatement(
                        "SELECT TOP 1 id, thoiGianCheckIn, trangThai FROM dbo.ChamCong WHERE maLichLamViec = ? AND isDeleted = 0 ORDER BY id DESC")) {
                    ps.setInt(1, scheduleId);
                    try (var rs = ps.executeQuery()) {
                        if (rs.next()) {
                            ccId = rs.getInt("id");
                            checkInTime = ts(rs, "thoiGianCheckIn");
                            String st = text(rs, "trangThai");
                            if (!st.isBlank()) existingStatus = st;
                        }
                    }
                }
                BigDecimal hours = checkInTime != null
                        ? BigDecimal.valueOf(Math.max(0.0, Duration.between(checkInTime, now).toMinutes() / 60.0)).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.valueOf(8);
                LocalDateTime shiftEndDt = LocalDateTime.of(today, shiftEnd);
                long earlyMins = Math.max(0L, Duration.between(now, shiftEndDt).toMinutes());
                BigDecimal earlyHours = earlyMins > 5
                        ? BigDecimal.valueOf(earlyMins / 60.0).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                String nextStatus = earlyMins > 5
                        ? ("DI_MUON".equals(existingStatus) ? "DI_MUON_VE_SOM" : "VE_SOM")
                        : existingStatus;
                if (ccId == null) {
                    try (var ps = c.prepareStatement(
                            "INSERT INTO dbo.ChamCong (maLichLamViec, thoiGianCheckIn, thoiGianCheckOut, soGioLam, soGioVeSom, trangThai, createdBy) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setInt(1, scheduleId);
                        ps.setTimestamp(2, Timestamp.valueOf(now));
                        ps.setTimestamp(3, Timestamp.valueOf(now));
                        ps.setBigDecimal(4, hours);
                        ps.setBigDecimal(5, earlyHours);
                        ps.setString(6, nextStatus);
                        ps.setInt(7, actorId);
                        ps.executeUpdate();
                    }
                } else {
                    try (var ps = c.prepareStatement(
                            "UPDATE dbo.ChamCong SET thoiGianCheckOut = ?, soGioLam = ?, soGioVeSom = ?, trangThai = ?, updatedAt = SYSUTCDATETIME(), updatedBy = ? WHERE id = ?")) {
                        ps.setTimestamp(1, Timestamp.valueOf(now));
                        ps.setBigDecimal(2, hours);
                        ps.setBigDecimal(3, earlyHours);
                        ps.setString(4, nextStatus);
                        ps.setInt(5, actorId);
                        ps.setInt(6, ccId);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể ghi nhận chấm công.", ex);
        }
    }

    @Override
    public List<PayrollPeriodOption> listPayrollPeriods() {
        String sql = "SELECT id, tenKy, thang, nam, daChot FROM dbo.KyLuong WHERE isDeleted = 0 ORDER BY nam DESC, thang DESC, id DESC";
        List<PayrollPeriodOption> list = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PayrollPeriodOption(
                        rs.getInt("id"), text(rs, "tenKy"),
                        rs.getInt("thang"), rs.getInt("nam"), rs.getBoolean("daChot")));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh sách kỳ lương.", ex);
        }
        return List.copyOf(list);
    }

    @Override
    public List<PayrollPeriodSummary> listPayrollPeriodSummaries() {
        String sql = """
                SELECT k.id, k.tenKy, k.thang, k.nam, k.tuNgay, k.denNgay, k.daChot,
                       COUNT(bl.id) empCount,
                       ISNULL(SUM(bl.thucLinh), 0) totalNet
                FROM dbo.KyLuong k
                LEFT JOIN dbo.BangLuong bl ON bl.maKyLuong = k.id AND bl.isDeleted = 0
                WHERE k.isDeleted = 0
                GROUP BY k.id, k.tenKy, k.thang, k.nam, k.tuNgay, k.denNgay, k.daChot
                ORDER BY k.nam DESC, k.thang DESC, k.id DESC
                """;
        List<PayrollPeriodSummary> list = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                Date fromD = rs.getDate("tuNgay");
                Date toD = rs.getDate("denNgay");
                list.add(new PayrollPeriodSummary(
                        rs.getInt("id"),
                        text(rs, "tenKy"),
                        rs.getInt("thang"),
                        rs.getInt("nam"),
                        fromD != null ? fromD.toLocalDate() : LocalDate.of(rs.getInt("nam"), rs.getInt("thang"), 1),
                        toD != null ? toD.toLocalDate() : LocalDate.of(rs.getInt("nam"), rs.getInt("thang"), 1),
                        rs.getBoolean("daChot"),
                        rs.getInt("empCount"),
                        money(rs, "totalNet")
                ));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải tổng hợp kỳ lương.", ex);
        }
        return List.copyOf(list);
    }


    @Override
    public List<PayrollRow> listPayroll(int periodId) {
        String sql = """
                SELECT bl.id, ISNULL(nv.maNhanVien, N'') maNhanVien,
                       LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N'')))) fullName,
                       ISNULL(cv.tenChucVu, N'') tenChucVu,
                       ISNULL(bl.luongCoBan, 0) luongCoBan,
                       ISNULL(bl.heSoLuong, 1) heSoLuong,
                       ISNULL(bl.tongPhuCap, 0) tongPhuCap,
                       ISNULL(bl.tongThuLaoKham, 0) tongThuLaoKham,
                       ISNULL(bl.tongKhauTru, 0) tongKhauTru,
                       ISNULL(bl.thucLinh, 0) thucLinh
                FROM dbo.BangLuong bl
                JOIN dbo.NhanVien nv ON nv.id = bl.maNhanVien
                LEFT JOIN dbo.ChucVu cv ON cv.id = nv.maChucVu AND cv.isDeleted = 0
                WHERE bl.maKyLuong = ? AND bl.isDeleted = 0
                ORDER BY nv.ten, nv.ho
                """;
        List<PayrollRow> rows = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, periodId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new PayrollRow(
                            rs.getInt("id"), text(rs, "maNhanVien"), text(rs, "fullName"),
                            text(rs, "tenChucVu"), money(rs, "luongCoBan"), money(rs, "heSoLuong"),
                            money(rs, "tongPhuCap"), money(rs, "tongThuLaoKham"),
                            money(rs, "tongKhauTru"), money(rs, "thucLinh")));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải bảng lương.", ex);
        }
        return List.copyOf(rows);
    }

    @Override
    public void calculateOrCreatePayrollPeriod(int month, int year, int actorId) {
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            Integer periodId = null;
            boolean closed = false;
            try (var ps = c.prepareStatement("SELECT TOP 1 id, daChot FROM dbo.KyLuong WHERE thang = ? AND nam = ? AND isDeleted = 0")) {
                ps.setInt(1, month);
                ps.setInt(2, year);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        periodId = rs.getInt("id");
                        closed = rs.getBoolean("daChot");
                    }
                }
            }
            if (closed) {
                throw new IllegalStateException("Kỳ lương tháng " + month + "/" + year + " đã được chốt, không thể tính lại.");
            }
            YearMonth ym = YearMonth.of(year, month);
            if (periodId == null) {
                try (var ps = c.prepareStatement(
                        "INSERT INTO dbo.KyLuong (tenKy, thang, nam, tuNgay, denNgay, daChot, createdBy) OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, 0, ?)")) {
                    ps.setString(1, "Kỳ lương Tháng " + month + "/" + year);
                    ps.setInt(2, month);
                    ps.setInt(3, year);
                    ps.setDate(4, Date.valueOf(ym.atDay(1)));
                    ps.setDate(5, Date.valueOf(ym.atEndOfMonth()));
                    ps.setInt(6, actorId);
                    try (var rs = ps.executeQuery()) {
                        if (rs.next()) periodId = rs.getInt(1);
                    }
                }
            }
            String insertSql = """
                    INSERT INTO dbo.BangLuong (maKyLuong, maNhanVien, luongCoBan, heSoLuong, tongPhuCap, tongThuLaoKham, tongKhauTru, thucLinh, createdBy)
                    SELECT ?, nv.id,
                           ISNULL(nv.luongCoBan, 0),
                           ISNULL(nv.heSoLuong, 1.0),
                           ISNULL(nv.phuCap, 0),
                           ISNULL((SELECT COUNT(*) * 30000 FROM dbo.LuotKham l WHERE l.maBacSi = nv.id AND l.isDeleted = 0 AND MONTH(l.thoiGianTiepNhan) = ? AND YEAR(l.thoiGianTiepNhan) = ?), 0),
                           0,
                           (ISNULL(nv.luongCoBan, 0) * ISNULL(nv.heSoLuong, 1.0)) + ISNULL(nv.phuCap, 0)
                             + ISNULL((SELECT COUNT(*) * 30000 FROM dbo.LuotKham l WHERE l.maBacSi = nv.id AND l.isDeleted = 0 AND MONTH(l.thoiGianTiepNhan) = ? AND YEAR(l.thoiGianTiepNhan) = ?), 0),
                           ?
                    FROM dbo.NhanVien nv
                    WHERE nv.isDeleted = 0 AND nv.hoatDong = 1
                      AND NOT EXISTS (SELECT 1 FROM dbo.BangLuong bl WHERE bl.maKyLuong = ? AND bl.maNhanVien = nv.id AND bl.isDeleted = 0)
                    """;
            try (var ps = c.prepareStatement(insertSql)) {
                ps.setInt(1, periodId);
                ps.setInt(2, month);
                ps.setInt(3, year);
                ps.setInt(4, month);
                ps.setInt(5, year);
                ps.setInt(6, actorId);
                ps.setInt(7, periodId);
                ps.executeUpdate();
            }
            String refreshSql = """
                    UPDATE bl
                    SET bl.luongCoBan = ISNULL(nv.luongCoBan, bl.luongCoBan),
                        bl.heSoLuong = ISNULL(nv.heSoLuong, bl.heSoLuong),
                        bl.tongPhuCap = ISNULL(nv.phuCap, bl.tongPhuCap),
                        bl.thucLinh = (ISNULL(nv.luongCoBan, bl.luongCoBan) * ISNULL(nv.heSoLuong, bl.heSoLuong))
                                      + ISNULL(nv.phuCap, bl.tongPhuCap)
                                      + ISNULL(bl.tongThuLaoKham, 0)
                                      - ISNULL(bl.tongKhauTru, 0),
                        bl.updatedAt = SYSUTCDATETIME(),
                        bl.updatedBy = ?
                    FROM dbo.BangLuong bl
                    JOIN dbo.NhanVien nv ON nv.id = bl.maNhanVien
                    WHERE bl.maKyLuong = ? AND bl.isDeleted = 0
                    """;
            try (var ps = c.prepareStatement(refreshSql)) {
                ps.setInt(1, actorId);
                ps.setInt(2, periodId);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tính lương kỳ " + month + "/" + year, ex);
        }
    }

    @Override
    public void lockPayrollPeriod(int periodId, int actorId) {
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(
                     "UPDATE dbo.KyLuong SET daChot = 1, updatedAt = SYSUTCDATETIME(), updatedBy = ? WHERE id = ? AND isDeleted = 0")) {
            ps.setInt(1, actorId);
            ps.setInt(2, periodId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể chốt kỳ lương.", ex);
        }
    }

    @Override
    public void replaceRoleIds(int employeeId, List<Integer> roleIds) {
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            try (var del = c.prepareStatement("UPDATE dbo.NhanVienVaiTro SET isDeleted = 1, deletedAt = SYSUTCDATETIME() WHERE maNhanVien = ? AND isDeleted = 0")) {
                del.setInt(1, employeeId);
                del.executeUpdate();
            }
            if (roleIds != null) {
                try (var ins = c.prepareStatement("INSERT INTO dbo.NhanVienVaiTro (maNhanVien, maVaiTro) VALUES (?, ?)")) {
                    for (Integer roleId : roleIds) {
                        if (roleId == null || roleId <= 0) continue;
                        ins.setInt(1, employeeId);
                        ins.setInt(2, roleId);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể cập nhật vai trò nhân viên.", ex);
        }
    }

    @Override
    public List<DepartmentOption> listDepartments(int facilityId) {
        String sql = "SELECT id, tenKhoa FROM dbo.Khoa WHERE maCoSo = ? AND isDeleted = 0 ORDER BY tenKhoa";
        List<DepartmentOption> list = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DepartmentOption(rs.getInt("id"), text(rs, "tenKhoa")));
                }
            }
        } catch (SQLException ex) {
            return List.of();
        }
        return List.copyOf(list);
    }


    @Override
    public EmployeeDetail getEmployeeDetail(int employeeId) {
        String sql = """
                SELECT id, maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh,
                       maChucVu, username, chungChiHanhNghe, ngayVaoLam, luongCoBan, heSoLuong, phuCap, hoatDong,
                       ngayNghiViec, lyDoNghiViec, dangNhapCuoi
                FROM dbo.NhanVien
                WHERE id = ? AND isDeleted = 0
                """;
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            List<Integer> roleIds = new ArrayList<>();
            try (var ps = c.prepareStatement("SELECT maVaiTro FROM dbo.NhanVienVaiTro WHERE maNhanVien = ? AND isDeleted = 0")) {
                ps.setInt(1, employeeId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) roleIds.add(rs.getInt(1));
                }
            }
            List<Integer> deptIds = new ArrayList<>();
            try (var ps = c.prepareStatement("SELECT maKhoa FROM dbo.NhanVienKhoa WHERE maNhanVien = ? AND isDeleted = 0")) {
                ps.setInt(1, employeeId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) deptIds.add(rs.getInt(1));
                }
            }
            try (var ps = c.prepareStatement(sql)) {
                ps.setInt(1, employeeId);
                try (var rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    Date dob = rs.getDate("ngaySinh");
                    Date joinDate = rs.getDate("ngayVaoLam");
                    Date leaveDate = rs.getDate("ngayNghiViec");
                    int mc = rs.getInt("maChucVu");
                    Integer maChucVu = rs.wasNull() ? null : mc;
                    return new EmployeeDetail(
                            rs.getInt("id"),
                            text(rs, "maNhanVien"),
                            text(rs, "ho"),
                            text(rs, "ten"),
                            text(rs, "soDienThoai"),
                            text(rs, "email"),
                            text(rs, "diaChi"),
                            text(rs, "gioiTinh"),
                            dob != null ? dob.toLocalDate() : null,
                            maChucVu,
                            text(rs, "username"),
                            text(rs, "chungChiHanhNghe"),
                            joinDate != null ? joinDate.toLocalDate() : null,
                            money(rs, "luongCoBan"),
                            money(rs, "heSoLuong"),
                            money(rs, "phuCap"),
                            rs.getBoolean("hoatDong"),
                            roleIds,
                            deptIds,
                            leaveDate != null ? leaveDate.toLocalDate() : null,
                            text(rs, "lyDoNghiViec"),
                            ts(rs, "dangNhapCuoi")
                    );
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải chi tiết nhân viên.", ex);
        }
    }

    @Override
    public void saveEmployeeDetail(EmployeeDetail detail, char[] newPassword, int actorId) {
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            String sql = """
                    UPDATE dbo.NhanVien
                    SET ho = ?, ten = ?, soDienThoai = ?, email = ?, diaChi = ?, gioiTinh = ?, ngaySinh = ?,
                        maChucVu = ?, chungChiHanhNghe = ?, ngayVaoLam = ?, luongCoBan = ?, heSoLuong = ?, phuCap = ?,
                        hoatDong = ?, ngayNghiViec = ?, lyDoNghiViec = ?, updatedAt = SYSUTCDATETIME(), updatedBy = ?
                    WHERE id = ? AND isDeleted = 0
                    """;
            try (var ps = c.prepareStatement(sql)) {
                ps.setString(1, detail.ho());
                ps.setString(2, detail.ten());
                ps.setString(3, detail.soDienThoai());
                ps.setString(4, detail.email());
                ps.setString(5, detail.diaChi());
                ps.setString(6, detail.gioiTinh());
                ps.setObject(7, detail.ngaySinh() != null ? Date.valueOf(detail.ngaySinh()) : null);
                ps.setObject(8, detail.maChucVu(), java.sql.Types.INTEGER);
                ps.setString(9, detail.chungChiHanhNghe());
                ps.setObject(10, detail.ngayVaoLam() != null ? Date.valueOf(detail.ngayVaoLam()) : null);
                ps.setBigDecimal(11, detail.luongCoBan());
                ps.setBigDecimal(12, detail.heSoLuong());
                ps.setBigDecimal(13, detail.phuCap());
                ps.setBoolean(14, detail.hoatDong());
                ps.setObject(15, detail.ngayNghiViec() != null ? Date.valueOf(detail.ngayNghiViec()) : null);
                ps.setString(16, detail.lyDoNghiViec());
                ps.setInt(17, actorId);
                ps.setInt(18, detail.id());
                ps.executeUpdate();
            }
            if (newPassword != null && newPassword.length > 0) {
                String hash = new PBKDF2NhanVienMatKhauBoBam().hash(newPassword);
                try (var ps = c.prepareStatement("UPDATE dbo.NhanVien SET passwordHash = ?, updatedAt = SYSUTCDATETIME(), updatedBy = ? WHERE id = ?")) {
                    ps.setString(1, hash);
                    ps.setInt(2, actorId);
                    ps.setInt(3, detail.id());
                    ps.executeUpdate();
                }
            }
            replaceRoleIds(detail.id(), detail.roleIds());
            try (var del = c.prepareStatement("UPDATE dbo.NhanVienKhoa SET isDeleted = 1, deletedAt = SYSUTCDATETIME() WHERE maNhanVien = ? AND isDeleted = 0")) {
                del.setInt(1, detail.id());
                del.executeUpdate();
            }
            if (detail.departmentIds() != null && !detail.departmentIds().isEmpty()) {
                try (var ins = c.prepareStatement("INSERT INTO dbo.NhanVienKhoa (maNhanVien, maKhoa) VALUES (?, ?)")) {
                    for (Integer deptId : detail.departmentIds()) {
                        if (deptId == null || deptId <= 0) continue;
                        ins.setInt(1, detail.id());
                        ins.setInt(2, deptId);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể lưu thông tin nhân viên.", ex);
        }
    }

    @Override
    public void resetEmployeePassword(int employeeId, char[] newPassword, int actorId) {
        if (newPassword == null || newPassword.length < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        String hash = new PBKDF2NhanVienMatKhauBoBam().hash(newPassword);
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(
                     "UPDATE dbo.NhanVien SET passwordHash = ?, updatedAt = SYSUTCDATETIME(), updatedBy = ? WHERE id = ? AND isDeleted = 0")) {
            ps.setString(1, hash);
            ps.setInt(2, actorId);
            ps.setInt(3, employeeId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể đặt lại mật khẩu.", ex);
        }
    }

    @Override
    public List<ShiftOption> listShifts() {
        String sql = "SELECT id, tenCa, gioBatDau, gioKetThuc FROM dbo.CaTruc WHERE isDeleted = 0 AND hoatDong = 1 ORDER BY gioBatDau, id";
        List<ShiftOption> list = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                Time st = rs.getTime("gioBatDau");
                Time et = rs.getTime("gioKetThuc");
                list.add(new ShiftOption(
                        rs.getInt("id"),
                        text(rs, "tenCa"),
                        st != null ? st.toLocalTime() : LocalTime.of(7, 30),
                        et != null ? et.toLocalTime() : LocalTime.of(17, 0)));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh sách ca trực.", ex);
        }
        return List.copyOf(list);
    }


    @Override
    public List<RoomOption> listRooms(int facilityId) {
        String sql = """
                SELECT pk.id, pk.maKhoa, pk.tenPhong
                FROM dbo.PhongKham pk
                JOIN dbo.Khoa k ON k.id = pk.maKhoa
                WHERE k.maCoSo = ? AND pk.isDeleted = 0 AND pk.hoatDong = 1 AND k.isDeleted = 0
                ORDER BY pk.tenPhong
                """;
        List<RoomOption> list = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RoomOption(rs.getInt("id"), rs.getInt("maKhoa"), text(rs, "tenPhong")));
                }
            }
        } catch (SQLException ex) {
            return List.of();
        }
        return List.copyOf(list);
    }


    @Override
    public List<ScheduleRow> listSchedules(int facilityId, int month, int year) {
        String sql = """
                SELECT llv.id, llv.maNhanVien, ISNULL(nv.maNhanVien, N'') maNv,
                       LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N'')))) fullName,
                       llv.maCaTruc, ISNULL(ct.tenCa, N'') tenCa, ct.gioBatDau, ct.gioKetThuc,
                       ISNULL(k.tenKhoa, N'') tenKhoa, ISNULL(pk.tenPhong, N'') tenPhong,
                       llv.ngayLamViec, ISNULL(llv.trangThai, '') trangThai, ISNULL(llv.ghiChu, N'') ghiChu
                FROM dbo.LichLamViec llv
                JOIN dbo.NhanVien nv ON nv.id = llv.maNhanVien
                JOIN dbo.CaTruc ct ON ct.id = llv.maCaTruc
                LEFT JOIN dbo.Khoa k ON k.id = llv.maKhoa AND k.isDeleted = 0
                LEFT JOIN dbo.PhongKham pk ON pk.id = llv.maPhongKham AND pk.isDeleted = 0
                WHERE llv.maCoSo = ? AND MONTH(llv.ngayLamViec) = ? AND YEAR(llv.ngayLamViec) = ? AND llv.isDeleted = 0
                ORDER BY llv.ngayLamViec DESC, ct.gioBatDau, nv.ten
                """;
        List<ScheduleRow> rows = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("ngayLamViec");
                    Time st = rs.getTime("gioBatDau");
                    Time et = rs.getTime("gioKetThuc");
                    rows.add(new ScheduleRow(
                            rs.getInt("id"),
                            rs.getInt("maNhanVien"),
                            text(rs, "maNv"),
                            text(rs, "fullName"),
                            rs.getInt("maCaTruc"),
                            text(rs, "tenCa"),
                            st != null ? st.toLocalTime() : LocalTime.of(7, 30),
                            et != null ? et.toLocalTime() : LocalTime.of(17, 0),
                            text(rs, "tenKhoa"),
                            text(rs, "tenPhong"),
                            d != null ? d.toLocalDate() : LocalDate.now(),
                            text(rs, "trangThai"),
                            text(rs, "ghiChu")));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải lịch làm việc.", ex);
        }
        return List.copyOf(rows);
    }

    @Override
    public void createSchedule(int employeeId, int shiftId, int facilityId, Integer departmentId, Integer roomId,
                               LocalDate workDate, String note, int actorId) {
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            try (var dup = c.prepareStatement(
                    "SELECT COUNT(*) FROM dbo.LichLamViec WHERE maNhanVien = ? AND maCaTruc = ? AND ngayLamViec = ? AND isDeleted = 0")) {
                dup.setInt(1, employeeId);
                dup.setInt(2, shiftId);
                dup.setDate(3, Date.valueOf(workDate));
                try (var rs = dup.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new IllegalStateException("Nhân viên đã được xếp ca này trong ngày " + workDate + ".");
                    }
                }
            }
            String sql = """
                    INSERT INTO dbo.LichLamViec (maNhanVien, maCaTruc, maCoSo, maKhoa, maPhongKham, ngayLamViec, trangThai, nguonLich, ghiChu, createdBy)
                    VALUES (?, ?, ?, ?, ?, ?, 'CHUA_DIEM_DANH', 'QUAN_LY', ?, ?)
                    """;
            try (var ps = c.prepareStatement(sql)) {
                ps.setInt(1, employeeId);
                ps.setInt(2, shiftId);
                ps.setInt(3, facilityId);
                ps.setObject(4, departmentId, java.sql.Types.INTEGER);
                ps.setObject(5, roomId, java.sql.Types.INTEGER);
                ps.setDate(6, Date.valueOf(workDate));
                ps.setString(7, note != null ? note : "");
                ps.setInt(8, actorId);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể xếp lịch làm việc.", ex);
        }
    }

    @Override
    public void deleteSchedule(int scheduleId, int actorId) {
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            try (var chk = c.prepareStatement("SELECT COUNT(*) FROM dbo.ChamCong WHERE maLichLamViec = ? AND isDeleted = 0")) {
                chk.setInt(1, scheduleId);
                try (var rs = chk.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new IllegalStateException("Lịch trực đã có dữ liệu chấm công, không thể xóa.");
                    }
                }
            }
            try (var ps = c.prepareStatement(
                    "UPDATE dbo.LichLamViec SET isDeleted = 1, deletedAt = SYSUTCDATETIME(), deletedBy = ? WHERE id = ? AND isDeleted = 0")) {
                ps.setInt(1, actorId);
                ps.setInt(2, scheduleId);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể xóa lịch làm việc.", ex);
        }
    }

    @Override
    public void adjustAttendance(int attendanceId, LocalDateTime checkIn, LocalDateTime checkOut, String status, int actorId) {
        if (checkIn != null && checkOut != null && checkOut.isBefore(checkIn)) {
            throw new IllegalArgumentException("Thời gian Check-out không thể trước Check-in.");
        }
        BigDecimal hours = (checkIn != null && checkOut != null)
                ? BigDecimal.valueOf(Duration.between(checkIn, checkOut).toMinutes() / 60.0).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String sql = """
                UPDATE dbo.ChamCong
                SET thoiGianCheckIn = ?, thoiGianCheckOut = ?, soGioLam = ?, trangThai = ?,
                    updatedAt = SYSUTCDATETIME(), updatedBy = ?
                WHERE id = ? AND isDeleted = 0
                """;
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setObject(1, checkIn != null ? Timestamp.valueOf(checkIn) : null);
            ps.setObject(2, checkOut != null ? Timestamp.valueOf(checkOut) : null);
            ps.setBigDecimal(3, hours);
            ps.setString(4, status != null && !status.isBlank() ? status : "DUNG_GIO");
            ps.setInt(5, actorId);
            ps.setInt(6, attendanceId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể điều chỉnh chấm công.", ex);
        }
    }

    @Override
    public void updatePayrollLine(int payrollLineId, BigDecimal allowance, BigDecimal examCommission, BigDecimal deduction, int actorId) {
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            boolean closed = false;
            try (var chk = c.prepareStatement(
                    "SELECT k.daChot FROM dbo.BangLuong bl JOIN dbo.KyLuong k ON k.id = bl.maKyLuong WHERE bl.id = ? AND bl.isDeleted = 0")) {
                chk.setInt(1, payrollLineId);
                try (var rs = chk.executeQuery()) {
                    if (!rs.next()) throw new IllegalStateException("Không tìm thấy dòng lương.");
                    closed = rs.getBoolean("daChot");
                }
            }
            if (closed) {
                throw new IllegalStateException("Kỳ lương đã chốt, không thể điều chỉnh.");
            }
            String sql = """
                    UPDATE dbo.BangLuong
                    SET tongPhuCap = ?, tongThuLaoKham = ?, tongKhauTru = ?,
                        thucLinh = (ISNULL(luongCoBan, 0) * ISNULL(heSoLuong, 1.0)) + ? + ? - ?,
                        updatedAt = SYSUTCDATETIME(), updatedBy = ?
                    WHERE id = ? AND isDeleted = 0
                    """;
            try (var ps = c.prepareStatement(sql)) {
                ps.setBigDecimal(1, allowance);
                ps.setBigDecimal(2, examCommission);
                ps.setBigDecimal(3, deduction);
                ps.setBigDecimal(4, allowance);
                ps.setBigDecimal(5, examCommission);
                ps.setBigDecimal(6, deduction);
                ps.setInt(7, actorId);
                ps.setInt(8, payrollLineId);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể cập nhật dòng lương.", ex);
        }
    }

    @Override
    public void reopenPayrollPeriod(int periodId, int actorId) {
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(
                     "UPDATE dbo.KyLuong SET daChot = 0, updatedAt = SYSUTCDATETIME(), updatedBy = ? WHERE id = ? AND isDeleted = 0")) {
            ps.setInt(1, actorId);
            ps.setInt(2, periodId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể mở khóa kỳ lương.", ex);
        }
    }

    @Override
    public List<PermissionRow> listRolePermissions(int roleId) {
        String sql = "SELECT id, maVaiTro, phanHe, hanhDong, ISNULL(phamViDuLieu, 'TOAN_HE_THONG') phamViDuLieu, duocPhep FROM dbo.MaTranPhanQuyen WHERE maVaiTro = ? AND isDeleted = 0 ORDER BY phanHe, hanhDong";
        List<PermissionRow> list = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PermissionRow(
                            rs.getInt("id"),
                            rs.getInt("maVaiTro"),
                            text(rs, "phanHe"),
                            text(rs, "hanhDong"),
                            text(rs, "phamViDuLieu"),
                            rs.getBoolean("duocPhep")));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải ma trận phân quyền.", ex);
        }
        return List.copyOf(list);
    }


    @Override
    public void saveRolePermissions(int roleId, List<PermissionRow> permissions, int actorId) {
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            try (var del = c.prepareStatement(
                    "UPDATE dbo.MaTranPhanQuyen SET isDeleted = 1, deletedAt = SYSUTCDATETIME(), deletedBy = ? WHERE maVaiTro = ? AND isDeleted = 0")) {
                del.setInt(1, actorId);
                del.setInt(2, roleId);
                del.executeUpdate();
            }
            if (permissions != null && !permissions.isEmpty()) {
                String insSql = "INSERT INTO dbo.MaTranPhanQuyen (maVaiTro, phanHe, hanhDong, phamViDuLieu, duocPhep, createdBy) VALUES (?, ?, ?, ?, ?, ?)";
                try (var ins = c.prepareStatement(insSql)) {
                    for (PermissionRow p : permissions) {
                        ins.setInt(1, roleId);
                        ins.setString(2, p.phanHe());
                        ins.setString(3, p.hanhDong());
                        ins.setString(4, p.phamViDuLieu() != null && !p.phamViDuLieu().isBlank() ? p.phamViDuLieu() : "TOAN_HE_THONG");
                        ins.setBoolean(5, p.duocPhep());
                        ins.setInt(6, actorId);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể lưu ma trận phân quyền.", ex);
        }
    }

    @Override
    public List<ShiftTypeOption> listShiftTypes() {
        String sql = "SELECT id, tenLoaiCa FROM dbo.LoaiCa WHERE isDeleted = 0 ORDER BY id";
        List<ShiftTypeOption> list = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new ShiftTypeOption(rs.getInt("id"), text(rs, "tenLoaiCa")));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải loại ca trực.", ex);
        }
        return List.copyOf(list);
    }

    @Override
    public List<ShiftDetail> listAllShifts() {
        String sql = """
                SELECT ct.id, ct.maLoaiCa, ISNULL(lc.tenLoaiCa, N'') tenLoaiCa, ct.tenCa, ct.gioBatDau, ct.gioKetThuc, ct.hoatDong
                FROM dbo.CaTruc ct
                LEFT JOIN dbo.LoaiCa lc ON lc.id = ct.maLoaiCa AND lc.isDeleted = 0
                WHERE ct.isDeleted = 0
                ORDER BY ct.gioBatDau, ct.id
                """;
        List<ShiftDetail> list = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                Time st = rs.getTime("gioBatDau");
                Time et = rs.getTime("gioKetThuc");
                list.add(new ShiftDetail(
                        rs.getInt("id"),
                        rs.getInt("maLoaiCa"),
                        text(rs, "tenLoaiCa"),
                        text(rs, "tenCa"),
                        st != null ? st.toLocalTime() : LocalTime.of(7, 30),
                        et != null ? et.toLocalTime() : LocalTime.of(17, 0),
                        rs.getBoolean("hoatDong")));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh mục ca trực.", ex);
        }
        return List.copyOf(list);
    }

    @Override
    public void saveShift(ShiftDetail shift, int actorId) {
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            if (shift.id() <= 0) {
                String sql = "INSERT INTO dbo.CaTruc (maLoaiCa, tenCa, gioBatDau, gioKetThuc, hoatDong, createdBy) VALUES (?, ?, ?, ?, ?, ?)";
                try (var ps = c.prepareStatement(sql)) {
                    ps.setInt(1, shift.shiftTypeId());
                    ps.setString(2, shift.name());
                    ps.setTime(3, Time.valueOf(shift.startTime()));
                    ps.setTime(4, Time.valueOf(shift.endTime()));
                    ps.setBoolean(5, shift.active());
                    ps.setInt(6, actorId);
                    ps.executeUpdate();
                }
            } else {
                String sql = "UPDATE dbo.CaTruc SET maLoaiCa = ?, tenCa = ?, gioBatDau = ?, gioKetThuc = ?, hoatDong = ?, updatedAt = SYSUTCDATETIME(), updatedBy = ? WHERE id = ? AND isDeleted = 0";
                try (var ps = c.prepareStatement(sql)) {
                    ps.setInt(1, shift.shiftTypeId());
                    ps.setString(2, shift.name());
                    ps.setTime(3, Time.valueOf(shift.startTime()));
                    ps.setTime(4, Time.valueOf(shift.endTime()));
                    ps.setBoolean(5, shift.active());
                    ps.setInt(6, actorId);
                    ps.setInt(7, shift.id());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể lưu ca trực.", ex);
        }
    }

    @Override
    public int createScheduleBatch(int employeeId, int shiftId, int facilityId, Integer departmentId, Integer roomId,
                                   LocalDate fromDate, LocalDate toDate, boolean skipSundays, String note, int actorId) {
        if (fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("Khoảng ngày không hợp lệ.");
        }
        int created = 0;
        try (var lease = connections.acquire()) {
            var c = lease.connection();
            String dupSql = "SELECT COUNT(*) FROM dbo.LichLamViec WHERE maNhanVien = ? AND maCaTruc = ? AND ngayLamViec = ? AND isDeleted = 0";
            String insSql = "INSERT INTO dbo.LichLamViec (maNhanVien, maCaTruc, maCoSo, maKhoa, maPhongKham, ngayLamViec, trangThai, nguonLich, ghiChu, createdBy) VALUES (?, ?, ?, ?, ?, ?, 'CHUA_DIEM_DANH', 'QUAN_LY', ?, ?)";
            try (var dup = c.prepareStatement(dupSql);
                 var ins = c.prepareStatement(insSql)) {
                LocalDate d = fromDate;
                while (!d.isAfter(toDate)) {
                    if (!skipSundays || d.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                        dup.setInt(1, employeeId);
                        dup.setInt(2, shiftId);
                        dup.setDate(3, Date.valueOf(d));
                        boolean exists = false;
                        try (var rs = dup.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) exists = true;
                        }
                        if (!exists) {
                            ins.setInt(1, employeeId);
                            ins.setInt(2, shiftId);
                            ins.setInt(3, facilityId);
                            ins.setObject(4, departmentId, java.sql.Types.INTEGER);
                            ins.setObject(5, roomId, java.sql.Types.INTEGER);
                            ins.setDate(6, Date.valueOf(d));
                            ins.setString(7, note != null ? note : "");
                            ins.setInt(8, actorId);
                            ins.addBatch();
                            created++;
                        }
                    }
                    d = d.plusDays(1);
                }
                if (created > 0) ins.executeBatch();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể xếp lịch hàng loạt.", ex);
        }
        return created;
    }

    @Override
    public boolean hasPermission(String roleName, String phanHe, String hanhDong) {
        if (roleName == null || roleName.isBlank() || "ADMIN".equalsIgnoreCase(roleName)) {
            return true;
        }
        String sql = """
                SELECT TOP 1 m.duocPhep
                FROM dbo.MaTranPhanQuyen m
                JOIN dbo.VaiTro vt ON vt.id = m.maVaiTro
                WHERE vt.tenVaiTro = ? AND m.phanHe = ? AND m.hanhDong = ?
                  AND m.isDeleted = 0 AND vt.isDeleted = 0
                """;
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setString(1, roleName);
            ps.setString(2, phanHe);
            ps.setString(3, hanhDong);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("duocPhep");
                }
            }
        } catch (SQLException ignored) {}
        return true;
    }

    private static String text(ResultSet rs, String col) throws SQLException {
        String s = rs.getString(col);
        return s == null ? "" : s;
    }

    private static BigDecimal money(ResultSet rs, String col) throws SQLException {
        BigDecimal v = rs.getBigDecimal(col);
        return v == null ? BigDecimal.ZERO : v;
    }

    private static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }
}
