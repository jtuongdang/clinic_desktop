SET NOCOUNT ON;

CREATE TABLE dbo.TuTrucChuyenKho (
    maPhieuXuat INT NOT NULL PRIMARY KEY,
    maCoSo INT NOT NULL,
    maKhoa INT NOT NULL,
    trangThai VARCHAR(40) NOT NULL,
    lyDo NVARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    createdBy INT NOT NULL,
    approvedAt DATETIME2 NULL,
    approvedBy INT NULL,
    issuedAt DATETIME2 NULL,
    issuedBy INT NULL,
    receivedAt DATETIME2 NULL,
    receivedBy INT NULL,
    CONSTRAINT FK_TuTrucChuyenKho_Phieu FOREIGN KEY (maPhieuXuat) REFERENCES dbo.PhieuXuatKho(id),
    CONSTRAINT FK_TuTrucChuyenKho_CoSo FOREIGN KEY (maCoSo) REFERENCES dbo.CoSo(id),
    CONSTRAINT FK_TuTrucChuyenKho_Khoa FOREIGN KEY (maKhoa) REFERENCES dbo.Khoa(id),
    CONSTRAINT CK_TuTrucChuyenKho_Status CHECK (trangThai IN
        ('REQUESTED','PARTIALLY_APPROVED','APPROVED','PARTIALLY_ISSUED','ISSUED','PARTIALLY_RECEIVED','RECEIVED','CANCELLED','REVERSED'))
);

CREATE TABLE dbo.TuTrucChuyenKhoDong (
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maPhieuXuat INT NOT NULL,
    maItem INT NOT NULL,
    soLuongYeuCau DECIMAL(18,4) NOT NULL,
    soLuongDuyet DECIMAL(18,4) NOT NULL DEFAULT 0,
    soLuongXuat DECIMAL(18,4) NOT NULL DEFAULT 0,
    soLuongNhan DECIMAL(18,4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_TuTrucChuyenKhoDong_Phieu FOREIGN KEY (maPhieuXuat) REFERENCES dbo.TuTrucChuyenKho(maPhieuXuat),
    CONSTRAINT FK_TuTrucChuyenKhoDong_Item FOREIGN KEY (maItem) REFERENCES dbo.Item(id),
    CONSTRAINT CK_TuTrucChuyenKhoDong_Quantity CHECK (
        soLuongYeuCau > 0 AND soLuongDuyet >= 0 AND soLuongDuyet <= soLuongYeuCau
        AND soLuongXuat >= 0 AND soLuongXuat <= soLuongDuyet
        AND soLuongNhan >= 0 AND soLuongNhan <= soLuongXuat)
);
CREATE INDEX IX_TuTrucChuyenKhoDong_Phieu ON dbo.TuTrucChuyenKhoDong(maPhieuXuat, id);

CREATE TABLE dbo.TuTrucChuyenKhoLo (
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maDong INT NOT NULL,
    maTonKhoXuat INT NOT NULL,
    maTonKhoNhan INT NULL,
    soLuongXuat DECIMAL(18,4) NOT NULL,
    soLuongNhan DECIMAL(18,4) NOT NULL DEFAULT 0,
    lyDoChenhLech NVARCHAR(500) NULL,
    createdAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_TuTrucChuyenKhoLo_Dong FOREIGN KEY (maDong) REFERENCES dbo.TuTrucChuyenKhoDong(id),
    CONSTRAINT FK_TuTrucChuyenKhoLo_LoXuat FOREIGN KEY (maTonKhoXuat) REFERENCES dbo.TonKho(id),
    CONSTRAINT FK_TuTrucChuyenKhoLo_LoNhan FOREIGN KEY (maTonKhoNhan) REFERENCES dbo.TonKho(id),
    CONSTRAINT CK_TuTrucChuyenKhoLo_Quantity CHECK (soLuongXuat > 0 AND soLuongNhan >= 0 AND soLuongNhan <= soLuongXuat)
);
CREATE INDEX IX_TuTrucChuyenKhoLo_Dong ON dbo.TuTrucChuyenKhoLo(maDong, id);

CREATE TABLE dbo.TuTrucChuyenKhoThaoTac (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maPhieuXuat INT NOT NULL,
    operationKey UNIQUEIDENTIFIER NOT NULL,
    hanhDong VARCHAR(40) NOT NULL,
    createdAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    createdBy INT NOT NULL,
    CONSTRAINT FK_TuTrucChuyenKhoThaoTac_Phieu FOREIGN KEY (maPhieuXuat) REFERENCES dbo.TuTrucChuyenKho(maPhieuXuat),
    CONSTRAINT UQ_TuTrucChuyenKhoThaoTac_Operation UNIQUE (operationKey),
    CONSTRAINT CK_TuTrucChuyenKhoThaoTac_Action CHECK (hanhDong IN ('CREATE','APPROVE','ISSUE','RECEIVE','CANCEL','REVERSE'))
);

CREATE TABLE dbo.TuTrucChuyenKhoNhan (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maThaoTac BIGINT NOT NULL,
    maPhanBo INT NOT NULL,
    soLuongNhan DECIMAL(18,4) NOT NULL,
    lyDoChenhLech NVARCHAR(500) NULL,
    createdAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    createdBy INT NOT NULL,
    CONSTRAINT FK_TuTrucChuyenKhoNhan_ThaoTac FOREIGN KEY (maThaoTac) REFERENCES dbo.TuTrucChuyenKhoThaoTac(id),
    CONSTRAINT FK_TuTrucChuyenKhoNhan_PhanBo FOREIGN KEY (maPhanBo) REFERENCES dbo.TuTrucChuyenKhoLo(id),
    CONSTRAINT CK_TuTrucChuyenKhoNhan_Quantity CHECK (soLuongNhan >= 0),
    CONSTRAINT UQ_TuTrucChuyenKhoNhan_Operation_Lot UNIQUE (maThaoTac,maPhanBo)
);

-- Tách quyền theo thao tác. Chưa có grant nghĩa là từ chối mặc định.
INSERT INTO dbo.MaTranPhanQuyen(maVaiTro, phanHe, hanhDong, phamViDuLieu, duocPhep)
SELECT v.id, g.phanHe, g.hanhDong, 'KHOA_PHONG', 1
FROM (VALUES
    (N'ADMIN', 'INVENTORY', 'DUYET'),
    (N'ADMIN', 'INVENTORY', 'DAO_GIAO_DICH'),
    (N'ADMIN', 'INVENTORY', 'XEM'),
    (N'PHARMACIST', 'INVENTORY', 'XUAT_KHO'),
    (N'PHARMACIST', 'INVENTORY', 'XEM'),
    (N'DOCTOR', 'INVENTORY', 'NHAN_KHO'),
    (N'DOCTOR', 'INVENTORY', 'XEM')
) g(roleName, phanHe, hanhDong)
JOIN dbo.VaiTro v ON v.tenVaiTro = g.roleName
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.MaTranPhanQuyen p
    WHERE p.maVaiTro = v.id AND p.phanHe = g.phanHe AND p.hanhDong = g.hanhDong AND p.isDeleted = 0
);
