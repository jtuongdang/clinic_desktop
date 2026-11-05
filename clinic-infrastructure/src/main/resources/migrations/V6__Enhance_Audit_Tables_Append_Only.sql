SET NOCOUNT ON;

IF COL_LENGTH(N'dbo.NhatKyHeThong', N'maVaiTro') IS NULL
    ALTER TABLE dbo.NhatKyHeThong ADD maVaiTro INT NULL;
IF COL_LENGTH(N'dbo.NhatKyHeThong', N'maVaiTroCode') IS NULL
    ALTER TABLE dbo.NhatKyHeThong ADD maVaiTroCode VARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.NhatKyHeThong', N'maCoSo') IS NULL
    ALTER TABLE dbo.NhatKyHeThong ADD maCoSo INT NULL;
IF COL_LENGTH(N'dbo.NhatKyHeThong', N'maKhoa') IS NULL
    ALTER TABLE dbo.NhatKyHeThong ADD maKhoa INT NULL;
IF COL_LENGTH(N'dbo.NhatKyHeThong', N'loaiDoiTuong') IS NULL
    ALTER TABLE dbo.NhatKyHeThong ADD loaiDoiTuong VARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.NhatKyHeThong', N'maDoiTuong') IS NULL
    ALTER TABLE dbo.NhatKyHeThong ADD maDoiTuong VARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.NhatKyHeThong', N'ketQua') IS NULL
    ALTER TABLE dbo.NhatKyHeThong ADD ketQua VARCHAR(30) NULL;
IF COL_LENGTH(N'dbo.NhatKyHeThong', N'maLyDo') IS NULL
    ALTER TABLE dbo.NhatKyHeThong ADD maLyDo VARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.NhatKyHeThong', N'workstationId') IS NULL
    ALTER TABLE dbo.NhatKyHeThong ADD workstationId VARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.NhatKyHeThong', N'correlationId') IS NULL
    ALTER TABLE dbo.NhatKyHeThong ADD correlationId VARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.NhatKyHeThong', N'occurredAtUtc') IS NULL
    ALTER TABLE dbo.NhatKyHeThong ADD occurredAtUtc DATETIME2(7) NULL;

IF COL_LENGTH(N'dbo.NhatKyHoSoBenhAn', N'maVaiTro') IS NULL
    ALTER TABLE dbo.NhatKyHoSoBenhAn ADD maVaiTro INT NULL;
IF COL_LENGTH(N'dbo.NhatKyHoSoBenhAn', N'maVaiTroCode') IS NULL
    ALTER TABLE dbo.NhatKyHoSoBenhAn ADD maVaiTroCode VARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.NhatKyHoSoBenhAn', N'maCoSo') IS NULL
    ALTER TABLE dbo.NhatKyHoSoBenhAn ADD maCoSo INT NULL;
IF COL_LENGTH(N'dbo.NhatKyHoSoBenhAn', N'maKhoa') IS NULL
    ALTER TABLE dbo.NhatKyHoSoBenhAn ADD maKhoa INT NULL;
IF COL_LENGTH(N'dbo.NhatKyHoSoBenhAn', N'loaiDoiTuong') IS NULL
    ALTER TABLE dbo.NhatKyHoSoBenhAn ADD loaiDoiTuong VARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.NhatKyHoSoBenhAn', N'maDoiTuong') IS NULL
    ALTER TABLE dbo.NhatKyHoSoBenhAn ADD maDoiTuong VARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.NhatKyHoSoBenhAn', N'ketQua') IS NULL
    ALTER TABLE dbo.NhatKyHoSoBenhAn ADD ketQua VARCHAR(30) NULL;
IF COL_LENGTH(N'dbo.NhatKyHoSoBenhAn', N'maLyDo') IS NULL
    ALTER TABLE dbo.NhatKyHoSoBenhAn ADD maLyDo VARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.NhatKyHoSoBenhAn', N'workstationId') IS NULL
    ALTER TABLE dbo.NhatKyHoSoBenhAn ADD workstationId VARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.NhatKyHoSoBenhAn', N'correlationId') IS NULL
    ALTER TABLE dbo.NhatKyHoSoBenhAn ADD correlationId VARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.NhatKyHoSoBenhAn', N'occurredAtUtc') IS NULL
    ALTER TABLE dbo.NhatKyHoSoBenhAn ADD occurredAtUtc DATETIME2(7) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_NhatKyHeThong_VaiTro')
    ALTER TABLE dbo.NhatKyHeThong WITH NOCHECK
        ADD CONSTRAINT FK_NhatKyHeThong_VaiTro FOREIGN KEY (maVaiTro) REFERENCES dbo.VaiTro(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_NhatKyHeThong_CoSo')
    ALTER TABLE dbo.NhatKyHeThong WITH NOCHECK
        ADD CONSTRAINT FK_NhatKyHeThong_CoSo FOREIGN KEY (maCoSo) REFERENCES dbo.CoSo(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_NhatKyHeThong_Khoa')
    ALTER TABLE dbo.NhatKyHeThong WITH NOCHECK
        ADD CONSTRAINT FK_NhatKyHeThong_Khoa FOREIGN KEY (maKhoa) REFERENCES dbo.Khoa(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_NhatKyHoSoBenhAn_VaiTro')
    ALTER TABLE dbo.NhatKyHoSoBenhAn WITH NOCHECK
        ADD CONSTRAINT FK_NhatKyHoSoBenhAn_VaiTro FOREIGN KEY (maVaiTro) REFERENCES dbo.VaiTro(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_NhatKyHoSoBenhAn_CoSo')
    ALTER TABLE dbo.NhatKyHoSoBenhAn WITH NOCHECK
        ADD CONSTRAINT FK_NhatKyHoSoBenhAn_CoSo FOREIGN KEY (maCoSo) REFERENCES dbo.CoSo(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_NhatKyHoSoBenhAn_Khoa')
    ALTER TABLE dbo.NhatKyHoSoBenhAn WITH NOCHECK
        ADD CONSTRAINT FK_NhatKyHoSoBenhAn_Khoa FOREIGN KEY (maKhoa) REFERENCES dbo.Khoa(id);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_NhatKyHeThong_AppendOnlyState')
    ALTER TABLE dbo.NhatKyHeThong WITH NOCHECK
        ADD CONSTRAINT CK_NhatKyHeThong_AppendOnlyState CHECK (
            isDeleted = 0 AND updatedAt IS NULL AND updatedBy IS NULL
            AND deletedAt IS NULL AND deletedBy IS NULL AND version = 0
        );
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_NhatKyHoSoBenhAn_AppendOnlyState')
    ALTER TABLE dbo.NhatKyHoSoBenhAn WITH NOCHECK
        ADD CONSTRAINT CK_NhatKyHoSoBenhAn_AppendOnlyState CHECK (
            isDeleted = 0 AND updatedAt IS NULL AND updatedBy IS NULL
            AND deletedAt IS NULL AND deletedBy IS NULL AND version = 0
        );
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_NhatKyHeThong_KetQua')
    ALTER TABLE dbo.NhatKyHeThong WITH NOCHECK
        ADD CONSTRAINT CK_NhatKyHeThong_KetQua
        CHECK (ketQua IS NULL OR ketQua IN ('SUCCESS', 'FAILURE', 'DENIED'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_NhatKyHoSoBenhAn_KetQua')
    ALTER TABLE dbo.NhatKyHoSoBenhAn WITH NOCHECK
        ADD CONSTRAINT CK_NhatKyHoSoBenhAn_KetQua
        CHECK (ketQua IS NULL OR ketQua IN ('SUCCESS', 'FAILURE', 'DENIED'));
GO

CREATE OR ALTER TRIGGER dbo.TR_NhatKyHeThong_AppendOnly
ON dbo.NhatKyHeThong
AFTER UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    THROW 51000, 'Audit tables are append-only.', 1;
END;
GO

CREATE OR ALTER TRIGGER dbo.TR_NhatKyHoSoBenhAn_AppendOnly
ON dbo.NhatKyHoSoBenhAn
AFTER UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    THROW 51000, 'Audit tables are append-only.', 1;
END;
GO
