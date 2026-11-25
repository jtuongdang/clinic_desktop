-- Bo sung thong tin so thu, so truc, loai phieu va trang thai chot so tren dbo.PhieuThu
-- de bao cao doanh thu theo phieu thu loc chinh xac theo nghiep vu.
IF COL_LENGTH(N'dbo.PhieuThu', N'soThu') IS NULL
BEGIN
    ALTER TABLE dbo.PhieuThu ADD soThu NVARCHAR(100) NULL;
END;

IF COL_LENGTH(N'dbo.PhieuThu', N'laSoTruc') IS NULL
BEGIN
    ALTER TABLE dbo.PhieuThu ADD laSoTruc BIT NOT NULL CONSTRAINT DF_PhieuThu_laSoTruc DEFAULT 0;
END;

IF COL_LENGTH(N'dbo.PhieuThu', N'loaiPhieu') IS NULL
BEGIN
    ALTER TABLE dbo.PhieuThu ADD loaiPhieu NVARCHAR(30) NOT NULL CONSTRAINT DF_PhieuThu_loaiPhieu DEFAULT N'THU_TIEN';
END;

IF COL_LENGTH(N'dbo.PhieuThu', N'daChotSo') IS NULL
BEGIN
    ALTER TABLE dbo.PhieuThu ADD daChotSo BIT NOT NULL CONSTRAINT DF_PhieuThu_daChotSo DEFAULT 0;
END;

EXEC(N'
UPDATE pt
SET
    soThu = COALESCE(NULLIF(pt.soThu, N''''), N''Sổ thu chính''),
    loaiPhieu = CASE
        WHEN ISNULL(h.soTienHoanTra, 0) > 0 THEN N''HOAN_UNG''
        WHEN ISNULL(h.soTienTamUng, 0) > 0 THEN N''TAM_UNG''
        ELSE N''THU_TIEN''
    END,
    daChotSo = CASE
        WHEN UPPER(ISNULL(h.trangThai, N'''')) LIKE N''%DA_CHOT%''
          OR UPPER(ISNULL(h.trangThai, N'''')) LIKE N''%DA_THANH_TOAN%''
          OR UPPER(ISNULL(h.trangThai, N'''')) LIKE N''%HOAN_THANH%'' THEN 1
        ELSE 0
    END
FROM dbo.PhieuThu pt
JOIN dbo.HoaDon h ON h.id = pt.maHoaDon
WHERE pt.isDeleted = 0;
');
