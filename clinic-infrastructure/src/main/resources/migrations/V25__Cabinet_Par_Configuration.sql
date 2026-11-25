SET NOCOUNT ON;

CREATE TABLE dbo.TuTrucDinhMuc (
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maCoSo INT NOT NULL,
    maKhoa INT NOT NULL,
    maKho INT NOT NULL,
    maItem INT NOT NULL,
    soLuongToiThieu DECIMAL(18,4) NOT NULL,
    soLuongMucTieu DECIMAL(18,4) NOT NULL,
    soLuongToiDa DECIMAL(18,4) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    createdBy INT NOT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    CONSTRAINT FK_TuTrucDinhMuc_CoSo FOREIGN KEY (maCoSo) REFERENCES dbo.CoSo(id),
    CONSTRAINT FK_TuTrucDinhMuc_Khoa FOREIGN KEY (maKhoa) REFERENCES dbo.Khoa(id),
    CONSTRAINT FK_TuTrucDinhMuc_Kho FOREIGN KEY (maKho) REFERENCES dbo.Kho(id),
    CONSTRAINT FK_TuTrucDinhMuc_Item FOREIGN KEY (maItem) REFERENCES dbo.Item(id),
    CONSTRAINT CK_TuTrucDinhMuc_Quantity CHECK (
        soLuongToiThieu >= 0 AND soLuongMucTieu >= soLuongToiThieu
        AND soLuongToiDa >= soLuongMucTieu)
);
CREATE UNIQUE INDEX UX_TuTrucDinhMuc_Active ON dbo.TuTrucDinhMuc(maCoSo,maKhoa,maKho,maItem)
    WHERE isDeleted=0;
