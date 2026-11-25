-- V19: Bổ sung bảng thanh toán công nợ phiếu nhập nhà cung cấp và chỉ mục tối ưu cộng dồn tồn kho theo lô.
-- Idempotent: có thể chạy lại nhiều lần an toàn.

IF OBJECT_ID('dbo.ThanhToanPhieuNhap', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ThanhToanPhieuNhap (
        id              INT IDENTITY(1,1) PRIMARY KEY,
        maPhieuNhap     INT             NOT NULL,
        soPhieuChi      VARCHAR(50)     NOT NULL UNIQUE,
        loaiPhieu       NVARCHAR(50)    NOT NULL DEFAULT N'Tiền mặt',
        ngayThanhToan   DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
        soTien          DECIMAL(18,2)   NOT NULL CHECK (soTien > 0),
        ghiChu          NVARCHAR(500)   NULL,
        trangThai       VARCHAR(20)     NOT NULL DEFAULT 'HOAT_DONG',
        createdAt       DATETIME2       NOT NULL DEFAULT SYSDATETIME(),
        createdBy       INT             NULL,
        updatedAt       DATETIME2       NULL,
        updatedBy       INT             NULL,
        isDeleted       BIT             NOT NULL DEFAULT 0,
        CONSTRAINT FK_ThanhToanPhieuNhap_PhieuNhap
            FOREIGN KEY (maPhieuNhap) REFERENCES dbo.PhieuNhapKho(id),
        CONSTRAINT CK_ThanhToanPhieuNhap_TrangThai
            CHECK (trangThai IN ('HOAT_DONG', 'DA_HUY'))
    );
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_ThanhToanPhieuNhap_PhieuNhap'
      AND object_id = OBJECT_ID('dbo.ThanhToanPhieuNhap')
)
BEGIN
    CREATE INDEX IX_ThanhToanPhieuNhap_PhieuNhap
        ON dbo.ThanhToanPhieuNhap (maPhieuNhap, trangThai)
        WHERE isDeleted = 0;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_TonKho_Kho_Item_Lo'
      AND object_id = OBJECT_ID('dbo.TonKho')
)
BEGIN
    CREATE INDEX IX_TonKho_Kho_Item_Lo
        ON dbo.TonKho (maKho, maItem, soLo)
        WHERE isDeleted = 0;
END;
