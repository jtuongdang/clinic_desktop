ALTER TABLE dbo.PhieuXuatKho ADD
    maKhachLe NVARCHAR(50) NULL, tenKhachLe NVARCHAR(255) NULL, tuoiKhachLe INT NULL,
    gioiTinhKhachLe NVARCHAR(20) NULL, diaChiKhachLe NVARCHAR(500) NULL,
    dienThoaiKhachLe NVARCHAR(50) NULL, emailKhachLe NVARCHAR(255) NULL, bacSiKe NVARCHAR(255) NULL;
ALTER TABLE dbo.ChiTietXuatKho ADD
    vatBanLe DECIMAL(5,2) NULL, mienGiamBanLe DECIMAL(5,2) NULL, thanhTienBanLe DECIMAL(18,2) NULL,
    huongDanBanLe NVARCHAR(1000) NULL, chanDoanBanLe NVARCHAR(1000) NULL;
