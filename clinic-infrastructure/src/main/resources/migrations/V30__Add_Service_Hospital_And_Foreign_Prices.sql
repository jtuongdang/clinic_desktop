-- Dedicated hospital and foreign price columns on DanhMucDichVu alongside giaBHYT and donGia.
IF COL_LENGTH(N'dbo.DanhMucDichVu', N'giaVienPhi') IS NULL
BEGIN
    ALTER TABLE dbo.DanhMucDichVu ADD giaVienPhi DECIMAL(18,2) NULL;
END;
IF COL_LENGTH(N'dbo.DanhMucDichVu', N'giaNuocNgoai') IS NULL
BEGIN
    ALTER TABLE dbo.DanhMucDichVu ADD giaNuocNgoai DECIMAL(18,2) NULL;
END;
