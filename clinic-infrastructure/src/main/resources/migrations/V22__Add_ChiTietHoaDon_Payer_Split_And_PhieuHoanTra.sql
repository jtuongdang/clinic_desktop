-- ============================================================================
-- V22: BỔ SUNG ĐỐI TƯỢNG THANH TOÁN TỪNG DÒNG CHI TIẾT HÓA ĐƠN & SỔ CÁI PHIẾU HOÀN TRẢ
-- ============================================================================

IF COL_LENGTH('dbo.ChiTietHoaDon', 'doiTuongThanhToan') IS NULL
    ALTER TABLE dbo.ChiTietHoaDon ADD doiTuongThanhToan NVARCHAR(30) NULL;
IF COL_LENGTH('dbo.ChiTietHoaDon', 'giaBHYT') IS NULL
    ALTER TABLE dbo.ChiTietHoaDon ADD giaBHYT DECIMAL(18,2) NOT NULL CONSTRAINT DF_ChiTietHoaDon_GiaBHYT DEFAULT 0;
IF COL_LENGTH('dbo.ChiTietHoaDon', 'tyLeHuong') IS NULL
    ALTER TABLE dbo.ChiTietHoaDon ADD tyLeHuong INT NOT NULL CONSTRAINT DF_ChiTietHoaDon_TyLeHuong DEFAULT 0;
IF COL_LENGTH('dbo.ChiTietHoaDon', 'soTienBHYT') IS NULL
    ALTER TABLE dbo.ChiTietHoaDon ADD soTienBHYT DECIMAL(18,2) NOT NULL CONSTRAINT DF_ChiTietHoaDon_SoTienBHYT DEFAULT 0;
IF COL_LENGTH('dbo.ChiTietHoaDon', 'soTienMienGiam') IS NULL
    ALTER TABLE dbo.ChiTietHoaDon ADD soTienMienGiam DECIMAL(18,2) NOT NULL CONSTRAINT DF_ChiTietHoaDon_SoTienMienGiam DEFAULT 0;
IF COL_LENGTH('dbo.ChiTietHoaDon', 'soTienBenhNhan') IS NULL
    ALTER TABLE dbo.ChiTietHoaDon ADD soTienBenhNhan DECIMAL(18,2) NOT NULL CONSTRAINT DF_ChiTietHoaDon_SoTienBenhNhan DEFAULT 0;
GO

UPDATE ct
SET ct.doiTuongThanhToan = CASE
        WHEN ISNULL(lk.doiTuongKham, 'THU_PHI') = 'MIEN_PHI' THEN N'MIEN_PHI'
        WHEN ISNULL(lk.doiTuongKham, 'THU_PHI') = 'BHYT' AND h.soTienBaoHiemChiTra > 0 THEN N'BHYT'
        ELSE N'THU_PHI'
    END,
    ct.soTienMienGiam = CASE
        WHEN ISNULL(lk.doiTuongKham, 'THU_PHI') = 'MIEN_PHI' THEN ROUND(ct.soLuong * ct.donGia, 2)
        ELSE ct.soTienMienGiam
    END,
    ct.soTienBenhNhan = CASE
        WHEN ISNULL(lk.doiTuongKham, 'THU_PHI') = 'MIEN_PHI' THEN 0
        ELSE ROUND(ct.soLuong * ct.donGia, 2) - ct.soTienBHYT - ct.soTienMienGiam
    END
FROM dbo.ChiTietHoaDon ct
JOIN dbo.HoaDon h ON h.id = ct.maHoaDon
LEFT JOIN dbo.LuotKham lk ON lk.id = h.maLuotKham
WHERE ct.doiTuongThanhToan IS NULL;
GO

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PhieuHoanTra')
CREATE TABLE dbo.PhieuHoanTra (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maHoaDon INT NOT NULL,
    soTien DECIMAL(18,2) NOT NULL CONSTRAINT CK_PhieuHoanTra_SoTien CHECK (soTien > 0),
    ngayHoan DATETIME2 NOT NULL DEFAULT GETDATE(),
    phuongThuc VARCHAR(50) NOT NULL DEFAULT 'TIEN_MAT',
    nguoiHoan INT NOT NULL,
    lyDo NVARCHAR(500) NULL,
    requestId UNIQUEIDENTIFIER NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_PhieuHoanTra_HoaDon FOREIGN KEY (maHoaDon) REFERENCES dbo.HoaDon(id),
    CONSTRAINT FK_PhieuHoanTra_NguoiHoan FOREIGN KEY (nguoiHoan) REFERENCES dbo.NhanVien(id)
);
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UX_PhieuHoanTra_Request' AND object_id = OBJECT_ID('dbo.PhieuHoanTra'))
CREATE UNIQUE INDEX UX_PhieuHoanTra_Request ON dbo.PhieuHoanTra(requestId) WHERE requestId IS NOT NULL;
GO

CREATE OR ALTER TRIGGER dbo.TR_PhieuHoanTra_AppendOnly
ON dbo.PhieuHoanTra
AFTER UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51016, N'Phiếu hoàn trả là chứng từ bất biến; không được sửa hoặc xóa.', 1;
END;
GO
