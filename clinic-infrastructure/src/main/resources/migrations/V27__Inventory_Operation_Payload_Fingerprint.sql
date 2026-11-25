SET NOCOUNT ON;

ALTER TABLE dbo.TuTrucChuyenKhoThaoTac ADD payloadHash CHAR(64) NULL;
CREATE INDEX IX_TuTrucChuyenKhoThaoTac_KeyAction ON dbo.TuTrucChuyenKhoThaoTac(operationKey,hanhDong)
    INCLUDE(maPhieuXuat,payloadHash);
