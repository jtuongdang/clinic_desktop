-- Không tự gộp/xóa chứng từ cũ: dữ liệu trùng cần được đối soát trước khi nâng cấp.
IF EXISTS (SELECT maLuotKham FROM dbo.HoaDon WHERE isDeleted=0 GROUP BY maLuotKham HAVING COUNT(*)>1)
    THROW 51014, N'Có nhiều hóa đơn cho cùng lượt khám. Cần đối soát trước khi nâng cấp viện phí.', 1;
IF EXISTS (SELECT maHoaDon FROM dbo.CongNoBenhNhan WHERE isDeleted=0 GROUP BY maHoaDon HAVING COUNT(*)>1)
    THROW 51014, N'Có nhiều bản ghi công nợ cho cùng hóa đơn. Cần đối soát trước khi nâng cấp.', 1;
IF EXISTS (SELECT maHoaDon,maDoiTuong FROM dbo.ChiTietHoaDon
           WHERE isDeleted=0 AND loaiDoiTuong='DICH_VU_CLS'
           GROUP BY maHoaDon,maDoiTuong HAVING COUNT(*)>1)
    THROW 51014, N'Có khoản phí CLS bị ghi trùng. Cần đối soát trước khi nâng cấp.', 1;

CREATE UNIQUE INDEX UX_HoaDon_ActiveVisit ON dbo.HoaDon(maLuotKham) WHERE isDeleted=0;
CREATE UNIQUE INDEX UX_CongNo_ActiveInvoice ON dbo.CongNoBenhNhan(maHoaDon) WHERE isDeleted=0;
CREATE UNIQUE INDEX UX_ChiTietHoaDon_ActiveClsOrder ON dbo.ChiTietHoaDon(maHoaDon,maDoiTuong)
    WHERE isDeleted=0 AND loaiDoiTuong='DICH_VU_CLS';
ALTER TABLE dbo.PhieuThu ADD requestId UNIQUEIDENTIFIER NULL;
GO
CREATE UNIQUE INDEX UX_PhieuThu_Request ON dbo.PhieuThu(requestId) WHERE requestId IS NOT NULL;
GO
CREATE OR ALTER TRIGGER dbo.TR_PhieuThu_AppendOnly
ON dbo.PhieuThu
AFTER UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM deleted)
        THROW 51015, N'Phiếu thu là chứng từ bất biến; không được sửa hoặc xóa.', 1;
END;
GO
