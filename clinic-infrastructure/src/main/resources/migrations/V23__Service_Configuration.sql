-- Optional screen settings live beside the existing catalog; existing orders and tariffs are preserved.
IF OBJECT_ID(N'dbo.DichVuCauHinh', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DichVuCauHinh (
        maDichVu INT NOT NULL PRIMARY KEY,
        giaTriXml NVARCHAR(MAX) NOT NULL,
        updatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        updatedBy INT NOT NULL,
        CONSTRAINT FK_DichVuCauHinh_DanhMuc FOREIGN KEY(maDichVu) REFERENCES dbo.DanhMucDichVu(id)
    );
END;
