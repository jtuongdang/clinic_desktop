-- Bổ sung 5 nhân viên mẫu + mô phỏng đầy đủ Lịch làm việc, Chấm công, Kỳ lương & Bảng lương tháng 9, 10 (và cập nhật tháng 11/2026)

DECLARE @coSoId INT = ISNULL((SELECT TOP 1 id FROM dbo.CoSo WHERE isDeleted = 0 ORDER BY id), 1);

DECLARE @khoaKb INT = (SELECT TOP 1 id FROM dbo.Khoa WHERE maCoSo = @coSoId AND tenKhoa LIKE N'%Khám%' AND isDeleted = 0 ORDER BY id);
DECLARE @khoaNhi INT = ISNULL((SELECT TOP 1 id FROM dbo.Khoa WHERE maCoSo = @coSoId AND tenKhoa LIKE N'%Nhi%' AND isDeleted = 0 ORDER BY id), @khoaKb);
DECLARE @khoaDuoc INT = ISNULL((SELECT TOP 1 id FROM dbo.Khoa WHERE maCoSo = @coSoId AND tenKhoa LIKE N'%Dược%' AND isDeleted = 0 ORDER BY id), @khoaKb);
DECLARE @khoaCls INT = ISNULL((SELECT TOP 1 id FROM dbo.Khoa WHERE maCoSo = @coSoId AND (tenKhoa LIKE N'%Cận%' OR tenKhoa LIKE N'%Xét%') AND isDeleted = 0 ORDER BY id), @khoaKb);

DECLARE @cvBs INT = (SELECT TOP 1 id FROM dbo.ChucVu WHERE tenChucVu LIKE N'%Bác sĩ%' AND isDeleted = 0 ORDER BY id);
DECLARE @cvDd INT = ISNULL((SELECT TOP 1 id FROM dbo.ChucVu WHERE tenChucVu LIKE N'%Điều dưỡng%' AND isDeleted = 0 ORDER BY id), @cvBs);
DECLARE @cvDs INT = ISNULL((SELECT TOP 1 id FROM dbo.ChucVu WHERE tenChucVu LIKE N'%Dược%' AND isDeleted = 0 ORDER BY id), @cvDd);

DECLARE @roleDoc INT = (SELECT TOP 1 id FROM dbo.VaiTro WHERE tenVaiTro = 'DOCTOR' AND isDeleted = 0);
DECLARE @roleRec INT = (SELECT TOP 1 id FROM dbo.VaiTro WHERE tenVaiTro = 'RECEPTIONIST' AND isDeleted = 0);
DECLARE @rolePhm INT = (SELECT TOP 1 id FROM dbo.VaiTro WHERE tenVaiTro = 'PHARMACIST' AND isDeleted = 0);
DECLARE @roleCsh INT = (SELECT TOP 1 id FROM dbo.VaiTro WHERE tenVaiTro = 'CASHIER' AND isDeleted = 0);
DECLARE @roleLab INT = (SELECT TOP 1 id FROM dbo.VaiTro WHERE tenVaiTro = 'LAB_TECH' AND isDeleted = 0);

-- 1. Bổ sung 5 nhân viên mới (nếu chưa có)
IF NOT EXISTS (SELECT 1 FROM dbo.NhanVien WHERE maNhanVien = 'BS0002')
BEGIN
    INSERT INTO dbo.NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan, heSoLuong, phuCap)
    VALUES ('BS0002', N'Trần Minh', N'Đức', '0912000101', 'ductm@ehc.vn', N'TP. Bắc Ninh', 'M', '1986-04-12', @cvBs, 'ductm', 'pbkdf2_sha256$100000$sample$samplehash', 1, '009812/BN-CCHN', '2020-02-01', 19000000, 1.20, 2000000);
    DECLARE @idBs2 INT = SCOPE_IDENTITY();
    IF @roleDoc IS NOT NULL INSERT INTO dbo.NhanVienVaiTro (maNhanVien, maVaiTro) VALUES (@idBs2, @roleDoc);
    IF @khoaNhi IS NOT NULL INSERT INTO dbo.NhanVienKhoa (maNhanVien, maKhoa) VALUES (@idBs2, @khoaNhi);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.NhanVien WHERE maNhanVien = 'DD0001')
BEGIN
    INSERT INTO dbo.NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan, heSoLuong, phuCap)
    VALUES ('DD0001', N'Phạm Thu', N'Hà', '0912000102', 'hapt@ehc.vn', N'Từ Sơn, Bắc Ninh', 'F', '1994-09-18', @cvDd, 'hapt', 'pbkdf2_sha256$100000$sample$samplehash', 1, '004521/BN-DD', '2021-06-15', 10500000, 1.10, 1000000);
    DECLARE @idDd1 INT = SCOPE_IDENTITY();
    IF @roleRec IS NOT NULL INSERT INTO dbo.NhanVienVaiTro (maNhanVien, maVaiTro) VALUES (@idDd1, @roleRec);
    IF @khoaKb IS NOT NULL INSERT INTO dbo.NhanVienKhoa (maNhanVien, maKhoa) VALUES (@idDd1, @khoaKb);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.NhanVien WHERE maNhanVien = 'DS0001')
BEGIN
    INSERT INTO dbo.NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan, heSoLuong, phuCap)
    VALUES ('DS0001', N'Ngô Quốc', N'Bảo', '0912000103', 'baonq@ehc.vn', N'Yên Phong, Bắc Ninh', 'M', '1991-11-05', @cvDs, 'baonq', 'pbkdf2_sha256$100000$sample$samplehash', 1, '007732/BN-DS', '2019-08-01', 13500000, 1.15, 1200000);
    DECLARE @idDs1 INT = SCOPE_IDENTITY();
    IF @rolePhm IS NOT NULL INSERT INTO dbo.NhanVienVaiTro (maNhanVien, maVaiTro) VALUES (@idDs1, @rolePhm);
    IF @khoaDuoc IS NOT NULL INSERT INTO dbo.NhanVienKhoa (maNhanVien, maKhoa) VALUES (@idDs1, @khoaDuoc);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.NhanVien WHERE maNhanVien = 'TN0001')
BEGIN
    INSERT INTO dbo.NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan, heSoLuong, phuCap)
    VALUES ('TN0001', N'Vũ Ngọc', N'Lan', '0912000104', 'lanvn@ehc.vn', N'Quế Võ, Bắc Ninh', 'F', '1996-02-22', @cvDd, 'lanvn', 'pbkdf2_sha256$100000$sample$samplehash', 1, '', '2022-04-10', 9500000, 1.00, 900000);
    DECLARE @idTn1 INT = SCOPE_IDENTITY();
    IF @roleCsh IS NOT NULL INSERT INTO dbo.NhanVienVaiTro (maNhanVien, maVaiTro) VALUES (@idTn1, @roleCsh);
    IF @khoaKb IS NOT NULL INSERT INTO dbo.NhanVienKhoa (maNhanVien, maKhoa) VALUES (@idTn1, @khoaKb);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.NhanVien WHERE maNhanVien = 'XN0001')
BEGIN
    INSERT INTO dbo.NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan, heSoLuong, phuCap)
    VALUES ('XN0001', N'Hoàng Gia', N'Huy', '0912000105', 'huyhg@ehc.vn', N'Tiên Du, Bắc Ninh', 'M', '1992-07-14', @cvDd, 'huyhg', 'pbkdf2_sha256$100000$sample$samplehash', 1, '006619/BN-KTV', '2021-01-10', 12000000, 1.10, 1100000);
    DECLARE @idXn1 INT = SCOPE_IDENTITY();
    IF @roleLab IS NOT NULL INSERT INTO dbo.NhanVienVaiTro (maNhanVien, maVaiTro) VALUES (@idXn1, @roleLab);
    IF @khoaCls IS NOT NULL INSERT INTO dbo.NhanVienKhoa (maNhanVien, maKhoa) VALUES (@idXn1, @khoaCls);
END;

-- 2. Mô phỏng Lịch làm việc & Chấm công đầy đủ tháng 9 và tháng 10/2026 (Thứ 2 - Thứ 7) cho từng nhân viên
DECLARE @caSang INT = (SELECT TOP 1 id FROM dbo.CaTruc WHERE tenCa = N'Ca sáng' AND isDeleted = 0 ORDER BY id);
DECLARE @caChieu INT = (SELECT TOP 1 id FROM dbo.CaTruc WHERE tenCa = N'Ca chiều' AND isDeleted = 0 ORDER BY id);
DECLARE @caHc INT = (SELECT TOP 1 id FROM dbo.CaTruc WHERE tenCa = N'Ca hành chính' AND isDeleted = 0 ORDER BY id);
DECLARE @phongDefault INT = (SELECT TOP 1 id FROM dbo.PhongKham WHERE isDeleted = 0 ORDER BY id);

DECLARE @empId INT;
DECLARE @empKhoa INT;
DECLARE @curDate DATE;
DECLARE @shiftId INT;
DECLARE @inMin INT;
DECLARE @outMin INT;
DECLARE @lateHrs DECIMAL(5,2);
DECLARE @earlyHrs DECIMAL(5,2);
DECLARE @workHrs DECIMAL(5,2);
DECLARE @ccStatus VARCHAR(50);
DECLARE @llvId INT;
DECLARE @seedKey INT;

DECLARE emp_cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT nv.id,
           ISNULL((SELECT TOP 1 nvk.maKhoa FROM dbo.NhanVienKhoa nvk WHERE nvk.maNhanVien = nv.id AND nvk.isDeleted = 0 ORDER BY nvk.id), @khoaKb)
    FROM dbo.NhanVien nv
    WHERE nv.isDeleted = 0 AND nv.hoatDong = 1;

OPEN emp_cur;
FETCH NEXT FROM emp_cur INTO @empId, @empKhoa;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @curDate = '2026-09-01';
    WHILE @curDate <= '2026-11-22'
    BEGIN
        -- Bỏ qua Chủ Nhật (DATEPART(WEEKDAY) phụ thuộc DATEFIRST nên dùng @@DATEFIRST chuẩn hóa)
        IF ((DATEPART(WEEKDAY, @curDate) + @@DATEFIRST - 2) % 7) <> 6
        BEGIN
            SET @seedKey = (DAY(@curDate) + MONTH(@curDate) * 3 + @empId) % 9;
            SET @shiftId = CASE
                WHEN @seedKey IN (0, 3, 6) THEN ISNULL(@caHc, @caSang)
                WHEN @seedKey IN (1, 4, 7) THEN ISNULL(@caSang, @caHc)
                ELSE ISNULL(@caChieu, @caHc)
            END;

            IF NOT EXISTS (SELECT 1 FROM dbo.LichLamViec WHERE maNhanVien = @empId AND ngayLamViec = @curDate AND isDeleted = 0)
            BEGIN
                INSERT INTO dbo.LichLamViec (maNhanVien, maCaTruc, maCoSo, maKhoa, maPhongKham, ngayLamViec, trangThai, nguonLich, ghiChu)
                VALUES (@empId, @shiftId, @coSoId, @empKhoa, @phongDefault, @curDate, 'DA_DIEM_DANH', 'HE_THONG', N'Lịch phân công định kỳ');
                SET @llvId = SCOPE_IDENTITY();

                IF @shiftId = @caChieu
                BEGIN
                    SET @inMin = CASE WHEN @seedKey = 5 THEN 825 ELSE 810 END; -- 13:45 (muộn 15p) hoặc 13:30
                    SET @outMin = CASE WHEN @seedKey = 8 THEN 1030 ELSE 1050 END; -- 17:10 (sớm 20p) hoặc 17:30
                    SET @lateHrs = CASE WHEN @seedKey = 5 THEN 0.25 ELSE 0.00 END;
                    SET @earlyHrs = CASE WHEN @seedKey = 8 THEN 0.33 ELSE 0.00 END;
                    SET @ccStatus = CASE WHEN @seedKey = 5 THEN 'DI_MUON' WHEN @seedKey = 8 THEN 'VE_SOM' ELSE 'DUNG_GIO' END;
                END
                ELSE IF @shiftId = @caSang
                BEGIN
                    SET @inMin = CASE WHEN @seedKey = 4 THEN 465 ELSE 450 END; -- 07:45 hoặc 07:30
                    SET @outMin = CASE WHEN @seedKey = 7 THEN 670 ELSE 690 END; -- 11:10 hoặc 11:30
                    SET @lateHrs = CASE WHEN @seedKey = 4 THEN 0.25 ELSE 0.00 END;
                    SET @earlyHrs = CASE WHEN @seedKey = 7 THEN 0.33 ELSE 0.00 END;
                    SET @ccStatus = CASE WHEN @seedKey = 4 THEN 'DI_MUON' WHEN @seedKey = 7 THEN 'VE_SOM' ELSE 'DUNG_GIO' END;
                END
                ELSE
                BEGIN
                    SET @inMin = CASE WHEN @seedKey = 3 THEN 470 ELSE 450 END; -- 07:50 hoặc 07:30
                    SET @outMin = CASE WHEN @seedKey = 6 THEN 1000 ELSE 1020 END; -- 16:40 hoặc 17:00
                    SET @lateHrs = CASE WHEN @seedKey = 3 THEN 0.33 ELSE 0.00 END;
                    SET @earlyHrs = CASE WHEN @seedKey = 6 THEN 0.33 ELSE 0.00 END;
                    SET @ccStatus = CASE WHEN @seedKey = 3 THEN 'DI_MUON' WHEN @seedKey = 6 THEN 'VE_SOM' ELSE 'DUNG_GIO' END;
                END

                SET @workHrs = CAST((@outMin - @inMin) / 60.0 AS DECIMAL(5,2));
                INSERT INTO dbo.ChamCong (maLichLamViec, thoiGianCheckIn, thoiGianCheckOut, trangThai, soGioLam, soGioDiMuon, soGioVeSom)
                VALUES (
                    @llvId,
                    DATEADD(MINUTE, @inMin, CAST(@curDate AS DATETIME2)),
                    DATEADD(MINUTE, @outMin, CAST(@curDate AS DATETIME2)),
                    @ccStatus,
                    @workHrs,
                    @lateHrs,
                    @earlyHrs
                );
            END;
        END;
        SET @curDate = DATEADD(DAY, 1, @curDate);
    END;
    FETCH NEXT FROM emp_cur INTO @empId, @empKhoa;
END;
CLOSE emp_cur;
DEALLOCATE emp_cur;

-- 3. Tạo Kỳ lương & Bảng lương đầy đủ cho Tháng 9/2026 (Đã chốt), Tháng 10/2026 (Đã chốt) và cập nhật Tháng 11/2026 (Đang mở)
DECLARE @ky9 INT = (SELECT TOP 1 id FROM dbo.KyLuong WHERE thang = 9 AND nam = 2026 AND isDeleted = 0 ORDER BY id);
IF @ky9 IS NULL
BEGIN
    INSERT INTO dbo.KyLuong (tenKy, thang, nam, tuNgay, denNgay, daChot)
    VALUES (N'Kỳ lương Tháng 9/2026', 9, 2026, '2026-09-01', '2026-09-30', 1);
    SET @ky9 = SCOPE_IDENTITY();
END;

INSERT INTO dbo.BangLuong (maKyLuong, maNhanVien, luongCoBan, heSoLuong, tongPhuCap, tongThuLaoKham, tongKhauTru, thucLinh)
SELECT @ky9, nv.id,
       ISNULL(nv.luongCoBan, 11000000),
       ISNULL(nv.heSoLuong, 1.0),
       ISNULL(nv.phuCap, 1000000),
       CASE WHEN nv.maNhanVien LIKE 'BS%' THEN 2400000 WHEN nv.maNhanVien LIKE 'DS%' THEN 600000 ELSE 450000 END,
       ISNULL((SELECT COUNT(*) * 50000 FROM dbo.ChamCong cc JOIN dbo.LichLamViec llv ON llv.id = cc.maLichLamViec WHERE llv.maNhanVien = nv.id AND llv.isDeleted = 0 AND cc.isDeleted = 0 AND MONTH(llv.ngayLamViec) = 9 AND YEAR(llv.ngayLamViec) = 2026 AND cc.trangThai IN ('DI_MUON', 'VE_SOM', 'DI_MUON_VE_SOM')), 0),
       (ISNULL(nv.luongCoBan, 11000000) * ISNULL(nv.heSoLuong, 1.0)) + ISNULL(nv.phuCap, 1000000)
         + CASE WHEN nv.maNhanVien LIKE 'BS%' THEN 2400000 WHEN nv.maNhanVien LIKE 'DS%' THEN 600000 ELSE 450000 END
         - ISNULL((SELECT COUNT(*) * 50000 FROM dbo.ChamCong cc JOIN dbo.LichLamViec llv ON llv.id = cc.maLichLamViec WHERE llv.maNhanVien = nv.id AND llv.isDeleted = 0 AND cc.isDeleted = 0 AND MONTH(llv.ngayLamViec) = 9 AND YEAR(llv.ngayLamViec) = 2026 AND cc.trangThai IN ('DI_MUON', 'VE_SOM', 'DI_MUON_VE_SOM')), 0)
FROM dbo.NhanVien nv
WHERE nv.isDeleted = 0 AND nv.hoatDong = 1
  AND NOT EXISTS (SELECT 1 FROM dbo.BangLuong bl WHERE bl.maKyLuong = @ky9 AND bl.maNhanVien = nv.id AND bl.isDeleted = 0);

DECLARE @ky10 INT = (SELECT TOP 1 id FROM dbo.KyLuong WHERE thang = 10 AND nam = 2026 AND isDeleted = 0 ORDER BY id);
IF @ky10 IS NULL
BEGIN
    INSERT INTO dbo.KyLuong (tenKy, thang, nam, tuNgay, denNgay, daChot)
    VALUES (N'Kỳ lương Tháng 10/2026', 10, 2026, '2026-10-01', '2026-10-31', 1);
    SET @ky10 = SCOPE_IDENTITY();
END;

INSERT INTO dbo.BangLuong (maKyLuong, maNhanVien, luongCoBan, heSoLuong, tongPhuCap, tongThuLaoKham, tongKhauTru, thucLinh)
SELECT @ky10, nv.id,
       ISNULL(nv.luongCoBan, 11000000),
       ISNULL(nv.heSoLuong, 1.0),
       ISNULL(nv.phuCap, 1000000),
       CASE WHEN nv.maNhanVien LIKE 'BS%' THEN 2700000 WHEN nv.maNhanVien LIKE 'DS%' THEN 750000 ELSE 500000 END,
       ISNULL((SELECT COUNT(*) * 50000 FROM dbo.ChamCong cc JOIN dbo.LichLamViec llv ON llv.id = cc.maLichLamViec WHERE llv.maNhanVien = nv.id AND llv.isDeleted = 0 AND cc.isDeleted = 0 AND MONTH(llv.ngayLamViec) = 10 AND YEAR(llv.ngayLamViec) = 2026 AND cc.trangThai IN ('DI_MUON', 'VE_SOM', 'DI_MUON_VE_SOM')), 0),
       (ISNULL(nv.luongCoBan, 11000000) * ISNULL(nv.heSoLuong, 1.0)) + ISNULL(nv.phuCap, 1000000)
         + CASE WHEN nv.maNhanVien LIKE 'BS%' THEN 2700000 WHEN nv.maNhanVien LIKE 'DS%' THEN 750000 ELSE 500000 END
         - ISNULL((SELECT COUNT(*) * 50000 FROM dbo.ChamCong cc JOIN dbo.LichLamViec llv ON llv.id = cc.maLichLamViec WHERE llv.maNhanVien = nv.id AND llv.isDeleted = 0 AND cc.isDeleted = 0 AND MONTH(llv.ngayLamViec) = 10 AND YEAR(llv.ngayLamViec) = 2026 AND cc.trangThai IN ('DI_MUON', 'VE_SOM', 'DI_MUON_VE_SOM')), 0)
FROM dbo.NhanVien nv
WHERE nv.isDeleted = 0 AND nv.hoatDong = 1
  AND NOT EXISTS (SELECT 1 FROM dbo.BangLuong bl WHERE bl.maKyLuong = @ky10 AND bl.maNhanVien = nv.id AND bl.isDeleted = 0);

-- Bổ sung 5 nhân viên mới vào Kỳ lương Tháng 11/2026 (nếu đã tồn tại)
DECLARE @ky11 INT = (SELECT TOP 1 id FROM dbo.KyLuong WHERE thang = 11 AND nam = 2026 AND isDeleted = 0 ORDER BY id);
IF @ky11 IS NOT NULL
BEGIN
    INSERT INTO dbo.BangLuong (maKyLuong, maNhanVien, luongCoBan, heSoLuong, tongPhuCap, tongThuLaoKham, tongKhauTru, thucLinh)
    SELECT @ky11, nv.id,
           ISNULL(nv.luongCoBan, 11000000),
           ISNULL(nv.heSoLuong, 1.0),
           ISNULL(nv.phuCap, 1000000),
           CASE WHEN nv.maNhanVien LIKE 'BS%' THEN 1800000 ELSE 400000 END,
           150000,
           (ISNULL(nv.luongCoBan, 11000000) * ISNULL(nv.heSoLuong, 1.0)) + ISNULL(nv.phuCap, 1000000)
             + CASE WHEN nv.maNhanVien LIKE 'BS%' THEN 1800000 ELSE 400000 END - 150000
    FROM dbo.NhanVien nv
    WHERE nv.isDeleted = 0 AND nv.hoatDong = 1
      AND NOT EXISTS (SELECT 1 FROM dbo.BangLuong bl WHERE bl.maKyLuong = @ky11 AND bl.maNhanVien = nv.id AND bl.isDeleted = 0);
END;
