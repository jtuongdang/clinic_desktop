SET NOCOUNT ON;

CREATE TABLE dbo.DichVuVatTuHaoPhi (
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maDichVu INT NOT NULL,
    maCoSo INT NOT NULL,
    maKhoa INT NOT NULL,
    maKho INT NOT NULL,
    stage VARCHAR(32) NOT NULL,
    rounding BIT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updatedBy INT NOT NULL,
    CONSTRAINT FK_DichVuVatTuHaoPhi_DichVu FOREIGN KEY(maDichVu) REFERENCES dbo.DanhMucDichVu(id),
    CONSTRAINT FK_DichVuVatTuHaoPhi_CoSo FOREIGN KEY(maCoSo) REFERENCES dbo.CoSo(id),
    CONSTRAINT FK_DichVuVatTuHaoPhi_Khoa FOREIGN KEY(maKhoa) REFERENCES dbo.Khoa(id),
    CONSTRAINT FK_DichVuVatTuHaoPhi_Kho FOREIGN KEY(maKho) REFERENCES dbo.Kho(id),
    CONSTRAINT UQ_DichVuVatTuHaoPhi_Scope UNIQUE(maDichVu,maCoSo,maKhoa,maKho),
    CONSTRAINT CK_DichVuVatTuHaoPhi_Stage CHECK(stage IN ('ORDER','START','RESULT','ISSUE','EXECUTION_TIME'))
);
CREATE TABLE dbo.DichVuVatTuHaoPhiDong (
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maCauHinh INT NOT NULL,
    maItem INT NOT NULL,
    soLuongDinhMuc DECIMAL(18,4) NOT NULL,
    donVi NVARCHAR(50) NOT NULL,
    basis VARCHAR(32) NOT NULL DEFAULT 'PER_EXECUTION',
    CONSTRAINT FK_DichVuVatTuHaoPhiDong_CauHinh FOREIGN KEY(maCauHinh) REFERENCES dbo.DichVuVatTuHaoPhi(id),
    CONSTRAINT FK_DichVuVatTuHaoPhiDong_Item FOREIGN KEY(maItem) REFERENCES dbo.Item(id),
    CONSTRAINT UQ_DichVuVatTuHaoPhiDong_Item UNIQUE(maCauHinh,maItem),
    CONSTRAINT CK_DichVuVatTuHaoPhiDong_Quantity CHECK(soLuongDinhMuc>0),
    CONSTRAINT CK_DichVuVatTuHaoPhiDong_Basis CHECK(basis IN ('PER_EXECUTION','PER_SAMPLE','PER_BATCH','SHARED_QC'))
);

CREATE TABLE dbo.TuTrucTieuHao (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maChiDinhDichVu INT NOT NULL,
    operationKey UNIQUEIDENTIFIER NOT NULL,
    maDichVu INT NOT NULL,
    maCoSo INT NOT NULL,
    maKhoa INT NOT NULL,
    configVersion BIGINT NULL,
    triggerStage VARCHAR(32) NOT NULL,
    trangThai VARCHAR(24) NOT NULL,
    executedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    createdBy INT NOT NULL,
    reason NVARCHAR(500) NULL,
    CONSTRAINT FK_TuTrucTieuHao_ChiDinh FOREIGN KEY(maChiDinhDichVu) REFERENCES dbo.ChiDinhDichVu(id),
    CONSTRAINT FK_TuTrucTieuHao_DichVu FOREIGN KEY(maDichVu) REFERENCES dbo.DanhMucDichVu(id),
    CONSTRAINT FK_TuTrucTieuHao_CoSo FOREIGN KEY(maCoSo) REFERENCES dbo.CoSo(id),
    CONSTRAINT FK_TuTrucTieuHao_Khoa FOREIGN KEY(maKhoa) REFERENCES dbo.Khoa(id),
    CONSTRAINT UQ_TuTrucTieuHao_Order UNIQUE(maChiDinhDichVu),
    CONSTRAINT UQ_TuTrucTieuHao_Operation UNIQUE(operationKey),
    CONSTRAINT CK_TuTrucTieuHao_Status CHECK(trangThai IN ('PENDING','POSTED','NO_DEFINITION','REVERSED'))
);
CREATE TABLE dbo.TuTrucTieuHaoDong (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maTieuHao BIGINT NOT NULL,
    maKho INT NOT NULL,
    maItem INT NOT NULL,
    soLuongDinhMuc DECIMAL(18,4) NOT NULL,
    soLuongThucTe DECIMAL(18,4) NOT NULL,
    donVi NVARCHAR(50) NOT NULL,
    basis VARCHAR(32) NOT NULL,
    trangThai VARCHAR(24) NOT NULL,
    CONSTRAINT FK_TuTrucTieuHaoDong_Header FOREIGN KEY(maTieuHao) REFERENCES dbo.TuTrucTieuHao(id),
    CONSTRAINT FK_TuTrucTieuHaoDong_Kho FOREIGN KEY(maKho) REFERENCES dbo.Kho(id),
    CONSTRAINT FK_TuTrucTieuHaoDong_Item FOREIGN KEY(maItem) REFERENCES dbo.Item(id),
    CONSTRAINT UQ_TuTrucTieuHaoDong_Item UNIQUE(maTieuHao,maKho,maItem),
    CONSTRAINT CK_TuTrucTieuHaoDong_Quantity CHECK(soLuongDinhMuc>0 AND soLuongThucTe>=0),
    CONSTRAINT CK_TuTrucTieuHaoDong_Basis CHECK(basis IN ('PER_EXECUTION','PER_SAMPLE','PER_BATCH','SHARED_QC')),
    CONSTRAINT CK_TuTrucTieuHaoDong_Status CHECK(trangThai IN ('PENDING','POSTED','REVERSED'))
);
CREATE TABLE dbo.TuTrucTieuHaoLo (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maDong BIGINT NOT NULL,
    maTonKho INT NOT NULL,
    soLuong DECIMAL(18,4) NOT NULL,
    donGia DECIMAL(18,2) NOT NULL,
    CONSTRAINT FK_TuTrucTieuHaoLo_Dong FOREIGN KEY(maDong) REFERENCES dbo.TuTrucTieuHaoDong(id),
    CONSTRAINT FK_TuTrucTieuHaoLo_TonKho FOREIGN KEY(maTonKho) REFERENCES dbo.TonKho(id),
    CONSTRAINT CK_TuTrucTieuHaoLo_Quantity CHECK(soLuong>0)
);
CREATE INDEX IX_TuTrucTieuHao_Status ON dbo.TuTrucTieuHao(trangThai,maCoSo,maKhoa,executedAt);
CREATE INDEX IX_TuTrucTieuHaoDong_Item ON dbo.TuTrucTieuHaoDong(maKho,maItem,trangThai);

CREATE TABLE dbo.TuTrucNhuCauBu (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maTieuHaoDong BIGINT NOT NULL,
    maCoSo INT NOT NULL,
    maKhoa INT NOT NULL,
    maKho INT NOT NULL,
    maItem INT NOT NULL,
    soLuongCoTheBu DECIMAL(18,4) NOT NULL,
    soLuongDaClaim DECIMAL(18,4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    trangThai VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    createdAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_TuTrucNhuCauBu_TieuHao FOREIGN KEY(maTieuHaoDong) REFERENCES dbo.TuTrucTieuHaoDong(id),
    CONSTRAINT FK_TuTrucNhuCauBu_CoSo FOREIGN KEY(maCoSo) REFERENCES dbo.CoSo(id),
    CONSTRAINT FK_TuTrucNhuCauBu_Khoa FOREIGN KEY(maKhoa) REFERENCES dbo.Khoa(id),
    CONSTRAINT FK_TuTrucNhuCauBu_Kho FOREIGN KEY(maKho) REFERENCES dbo.Kho(id),
    CONSTRAINT FK_TuTrucNhuCauBu_Item FOREIGN KEY(maItem) REFERENCES dbo.Item(id),
    CONSTRAINT UQ_TuTrucNhuCauBu_Source UNIQUE(maTieuHaoDong),
    CONSTRAINT CK_TuTrucNhuCauBu_Quantity CHECK(soLuongCoTheBu>0 AND soLuongDaClaim>=0 AND soLuongDaClaim<=soLuongCoTheBu),
    CONSTRAINT CK_TuTrucNhuCauBu_Status CHECK(trangThai IN ('OPEN','PARTIAL','CLAIMED','CLOSED'))
);
CREATE TABLE dbo.TuTrucNhuCauBuClaim (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maNhuCauBu BIGINT NOT NULL,
    maDongChuyenKho INT NOT NULL,
    soLuongClaim DECIMAL(18,4) NOT NULL,
    createdAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    createdBy INT NOT NULL,
    CONSTRAINT FK_TuTrucNhuCauBuClaim_Demand FOREIGN KEY(maNhuCauBu) REFERENCES dbo.TuTrucNhuCauBu(id),
    CONSTRAINT FK_TuTrucNhuCauBuClaim_Line FOREIGN KEY(maDongChuyenKho) REFERENCES dbo.TuTrucChuyenKhoDong(id),
    CONSTRAINT UQ_TuTrucNhuCauBuClaim UNIQUE(maNhuCauBu,maDongChuyenKho),
    CONSTRAINT CK_TuTrucNhuCauBuClaim_Quantity CHECK(soLuongClaim>0)
);
CREATE INDEX IX_TuTrucNhuCauBu_Available ON dbo.TuTrucNhuCauBu(maCoSo,maKhoa,maKho,trangThai,id);
