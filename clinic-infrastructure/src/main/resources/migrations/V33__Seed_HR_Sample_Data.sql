-- Seed dữ liệu mẫu đầy đủ cho phân hệ Quản Lý Nhân Sự (Ca trực, Lịch làm việc, Chấm công, Nhân viên tạm khóa, Bảng lương)

IF NOT EXISTS (SELECT 1 FROM dbo.LoaiCa WHERE isDeleted = 0)
BEGIN
    INSERT INTO dbo.LoaiCa (tenLoaiCa, moTa) VALUES
        (N'Ca hành chính', N'Ca làm việc tiêu chuẩn ban ngày'),
        (N'Ca trực', N'Ca trực chuyên môn 24h / ban đêm');
END;

DECLARE @loaiHanhChinh INT = (SELECT TOP 1 id FROM dbo.LoaiCa WHERE tenLoaiCa = N'Ca hành chính' AND isDeleted = 0 ORDER BY id);
DECLARE @loaiTruc INT = ISNULL((SELECT TOP 1 id FROM dbo.LoaiCa WHERE tenLoaiCa = N'Ca trực' AND isDeleted = 0 ORDER BY id), @loaiHanhChinh);

IF NOT EXISTS (SELECT 1 FROM dbo.CaTruc WHERE tenCa = N'Ca sáng' AND isDeleted = 0)
    INSERT INTO dbo.CaTruc (maLoaiCa, tenCa, gioBatDau, gioKetThuc, hoatDong) VALUES (@loaiHanhChinh, N'Ca sáng', '07:30:00', '11:30:00', 1);
IF NOT EXISTS (SELECT 1 FROM dbo.CaTruc WHERE tenCa = N'Ca chiều' AND isDeleted = 0)
    INSERT INTO dbo.CaTruc (maLoaiCa, tenCa, gioBatDau, gioKetThuc, hoatDong) VALUES (@loaiHanhChinh, N'Ca chiều', '13:30:00', '17:30:00', 1);
IF NOT EXISTS (SELECT 1 FROM dbo.CaTruc WHERE tenCa = N'Ca hành chính' AND isDeleted = 0)
    INSERT INTO dbo.CaTruc (maLoaiCa, tenCa, gioBatDau, gioKetThuc, hoatDong) VALUES (@loaiHanhChinh, N'Ca hành chính', '07:30:00', '17:00:00', 1);
IF NOT EXISTS (SELECT 1 FROM dbo.CaTruc WHERE tenCa = N'Ca trực đêm' AND isDeleted = 0)
    INSERT INTO dbo.CaTruc (maLoaiCa, tenCa, gioBatDau, gioKetThuc, hoatDong) VALUES (@loaiTruc, N'Ca trực đêm', '17:30:00', '07:30:00', 1);

-- Cập nhật mức lương mẫu cho nhân viên chưa có lương cơ bản
UPDATE dbo.NhanVien
SET luongCoBan = CASE WHEN maNhanVien LIKE 'BS%' THEN 18000000 ELSE 11000000 END,
    heSoLuong = CASE WHEN ISNULL(heSoLuong, 0) <= 0 THEN 1.2 ELSE heSoLuong END,
    phuCap = CASE WHEN ISNULL(phuCap, 0) = 0 THEN 1200000 ELSE phuCap END
WHERE isDeleted = 0 AND ISNULL(luongCoBan, 0) = 0;

-- Thêm 1 nhân viên đã khóa để test tab Đã Khóa & Mở Khóa tài khoản
IF NOT EXISTS (SELECT 1 FROM dbo.NhanVien WHERE maNhanVien = 'NV0004')
BEGIN
    DECLARE @cvDd INT = (SELECT TOP 1 id FROM dbo.ChucVu WHERE isDeleted = 0 ORDER BY id DESC);
    INSERT INTO dbo.NhanVien (
        maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh,
        maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam,
        ngayNghiViec, lyDoNghiViec, luongCoBan, heSoLuong, phuCap
    ) VALUES (
        'NV0004', N'Lê Thị', N'Mai', '0912345678', 'mailt@ehc.vn', N'TP. Bắc Ninh', 'F', '1993-08-20',
        @cvDd, 'mailt', 'pbkdf2_sha256$100000$sample$samplehash', 0, 'CCHN-DD-004', '2021-03-01',
        '2026-10-31', N'Tạm nghỉ / chuyển công tác', 9500000, 1.0, 800000
    );
END;

DECLARE @coSoId INT = ISNULL((SELECT TOP 1 id FROM dbo.CoSo WHERE isDeleted = 0 ORDER BY id), 1);
DECLARE @khoaKb INT = (SELECT TOP 1 id FROM dbo.Khoa WHERE maCoSo = @coSoId AND isDeleted = 0 ORDER BY id);
DECLARE @phongId INT = (SELECT TOP 1 id FROM dbo.PhongKham WHERE maKhoa = @khoaKb AND isDeleted = 0 ORDER BY id);
DECLARE @caSang INT = (SELECT TOP 1 id FROM dbo.CaTruc WHERE tenCa = N'Ca sáng' AND isDeleted = 0 ORDER BY id);
DECLARE @caChieu INT = (SELECT TOP 1 id FROM dbo.CaTruc WHERE tenCa = N'Ca chiều' AND isDeleted = 0 ORDER BY id);
DECLARE @caHc INT = (SELECT TOP 1 id FROM dbo.CaTruc WHERE tenCa = N'Ca hành chính' AND isDeleted = 0 ORDER BY id);

DECLARE @nv1 INT = (SELECT TOP 1 id FROM dbo.NhanVien WHERE isDeleted = 0 AND hoatDong = 1 ORDER BY id);
DECLARE @nv2 INT = (SELECT TOP 1 id FROM dbo.NhanVien WHERE isDeleted = 0 AND hoatDong = 1 AND id <> @nv1 ORDER BY id);
DECLARE @nv3 INT = (SELECT TOP 1 id FROM dbo.NhanVien WHERE isDeleted = 0 AND hoatDong = 1 AND id NOT IN (@nv1, ISNULL(@nv2, -1)) ORDER BY id);

DECLARE @d1 DATE = DATEADD(DAY, -2, CAST(GETDATE() AS DATE));
DECLARE @d2 DATE = DATEADD(DAY, -1, CAST(GETDATE() AS DATE));
DECLARE @d3 DATE = CAST(GETDATE() AS DATE);

-- Seed Lịch làm việc & Chấm công mẫu cho ngày hôm kia & hôm qua, và lịch chưa chấm công cho hôm nay
IF @nv1 IS NOT NULL AND @caSang IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.LichLamViec WHERE maNhanVien = @nv1 AND ngayLamViec = @d1 AND maCaTruc = @caSang AND isDeleted = 0)
    BEGIN
        INSERT INTO dbo.LichLamViec (maNhanVien, maCaTruc, maCoSo, maKhoa, maPhongKham, ngayLamViec, trangThai, nguonLich, ghiChu)
        VALUES (@nv1, @caSang, @coSoId, @khoaKb, @phongId, @d1, 'DA_DIEM_DANH', 'QUAN_LY', N'Trực khám buổi sáng');
        DECLARE @llv1 INT = SCOPE_IDENTITY();
        INSERT INTO dbo.ChamCong (maLichLamViec, thoiGianCheckIn, thoiGianCheckOut, trangThai, soGioLam, soGioDiMuon, soGioVeSom)
        VALUES (@llv1, DATEADD(MINUTE, 450, CAST(@d1 AS DATETIME2)), DATEADD(MINUTE, 690, CAST(@d1 AS DATETIME2)), 'DUNG_GIO', 4.00, 0.00, 0.00);
    END;

    IF NOT EXISTS (SELECT 1 FROM dbo.LichLamViec WHERE maNhanVien = @nv1 AND ngayLamViec = @d3 AND maCaTruc = @caHc AND isDeleted = 0)
        INSERT INTO dbo.LichLamViec (maNhanVien, maCaTruc, maCoSo, maKhoa, maPhongKham, ngayLamViec, trangThai, nguonLich, ghiChu)
        VALUES (@nv1, @caHc, @coSoId, @khoaKb, @phongId, @d3, 'CHUA_DIEM_DANH', 'QUAN_LY', N'Ca hành chính hôm nay');
END;

IF @nv2 IS NOT NULL AND @caChieu IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.LichLamViec WHERE maNhanVien = @nv2 AND ngayLamViec = @d2 AND maCaTruc = @caChieu AND isDeleted = 0)
    BEGIN
        INSERT INTO dbo.LichLamViec (maNhanVien, maCaTruc, maCoSo, maKhoa, maPhongKham, ngayLamViec, trangThai, nguonLich, ghiChu)
        VALUES (@nv2, @caChieu, @coSoId, @khoaKb, @phongId, @d2, 'DA_DIEM_DANH', 'QUAN_LY', N'Khám chuyên khoa chiều');
        DECLARE @llv2 INT = SCOPE_IDENTITY();
        INSERT INTO dbo.ChamCong (maLichLamViec, thoiGianCheckIn, thoiGianCheckOut, trangThai, soGioLam, soGioDiMuon, soGioVeSom)
        VALUES (@llv2, DATEADD(MINUTE, 825, CAST(@d2 AS DATETIME2)), DATEADD(MINUTE, 1050, CAST(@d2 AS DATETIME2)), 'DI_MUON', 3.75, 0.25, 0.00);
    END;
END;

IF @nv3 IS NOT NULL AND @caSang IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.LichLamViec WHERE maNhanVien = @nv3 AND ngayLamViec = @d2 AND maCaTruc = @caSang AND isDeleted = 0)
    BEGIN
        INSERT INTO dbo.LichLamViec (maNhanVien, maCaTruc, maCoSo, maKhoa, maPhongKham, ngayLamViec, trangThai, nguonLich, ghiChu)
        VALUES (@nv3, @caSang, @coSoId, @khoaKb, @phongId, @d2, 'DA_DIEM_DANH', 'QUAN_LY', N'Ca sáng quầy tiếp đón');
        DECLARE @llv3 INT = SCOPE_IDENTITY();
        INSERT INTO dbo.ChamCong (maLichLamViec, thoiGianCheckIn, thoiGianCheckOut, trangThai, soGioLam, soGioDiMuon, soGioVeSom)
        VALUES (@llv3, DATEADD(MINUTE, 450, CAST(@d2 AS DATETIME2)), DATEADD(MINUTE, 670, CAST(@d2 AS DATETIME2)), 'VE_SOM', 3.67, 0.00, 0.33);
    END;
END;

-- Seed Kỳ lương & Bảng lương tháng hiện tại để test chỉnh sửa / in phiếu lương / chốt kỳ
DECLARE @m INT = MONTH(GETDATE());
DECLARE @y INT = YEAR(GETDATE());
DECLARE @kyId INT = (SELECT TOP 1 id FROM dbo.KyLuong WHERE thang = @m AND nam = @y AND isDeleted = 0 ORDER BY id);

IF @kyId IS NULL
BEGIN
    INSERT INTO dbo.KyLuong (tenKy, thang, nam, tuNgay, denNgay, daChot)
    VALUES (CONCAT(N'Kỳ lương Tháng ', @m, N'/', @y), @m, @y, DATEFROMPARTS(@y, @m, 1), EOMONTH(GETDATE()), 0);
    SET @kyId = SCOPE_IDENTITY();
END;

INSERT INTO dbo.BangLuong (maKyLuong, maNhanVien, luongCoBan, heSoLuong, tongPhuCap, tongThuLaoKham, tongKhauTru, thucLinh)
SELECT @kyId, nv.id,
       ISNULL(nv.luongCoBan, 11000000),
       ISNULL(nv.heSoLuong, 1.0),
       ISNULL(nv.phuCap, 1000000),
       CASE WHEN nv.maNhanVien LIKE 'BS%' THEN 1500000 ELSE 300000 END,
       200000,
       (ISNULL(nv.luongCoBan, 11000000) * ISNULL(nv.heSoLuong, 1.0)) + ISNULL(nv.phuCap, 1000000)
         + CASE WHEN nv.maNhanVien LIKE 'BS%' THEN 1500000 ELSE 300000 END - 200000
FROM dbo.NhanVien nv
WHERE nv.isDeleted = 0 AND nv.hoatDong = 1
  AND NOT EXISTS (SELECT 1 FROM dbo.BangLuong bl WHERE bl.maKyLuong = @kyId AND bl.maNhanVien = nv.id AND bl.isDeleted = 0);
