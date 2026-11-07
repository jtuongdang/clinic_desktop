IF COL_LENGTH('dbo.NhanVien', 'loginFailureWindowStartedAt') IS NULL
BEGIN
    ALTER TABLE dbo.NhanVien ADD loginFailureWindowStartedAt DATETIME2(7) NULL;
END;
GO

IF COL_LENGTH('dbo.NhanVien', 'loginFailureCount') IS NULL
BEGIN
    ALTER TABLE dbo.NhanVien ADD loginFailureCount INT NOT NULL
        CONSTRAINT DF_NhanVien_loginFailureCount DEFAULT (0) WITH VALUES;
END;
GO

IF COL_LENGTH('dbo.NhanVien', 'loginLastFailureAt') IS NULL
BEGIN
    ALTER TABLE dbo.NhanVien ADD loginLastFailureAt DATETIME2(7) NULL;
END;
GO

IF COL_LENGTH('dbo.NhanVien', 'loginDelaySeconds') IS NULL
BEGIN
    ALTER TABLE dbo.NhanVien ADD loginDelaySeconds INT NOT NULL
        CONSTRAINT DF_NhanVien_loginDelaySeconds DEFAULT (0) WITH VALUES;
END;
GO

IF COL_LENGTH('dbo.NhanVien', 'loginNextAllowedAt') IS NULL
BEGIN
    ALTER TABLE dbo.NhanVien ADD loginNextAllowedAt DATETIME2(7) NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('dbo.NhanVien')
      AND name = 'CK_NhanVien_LoginThrottle_State'
)
BEGIN
    ALTER TABLE dbo.NhanVien WITH CHECK
        ADD CONSTRAINT CK_NhanVien_LoginThrottle_State
        CHECK (loginFailureCount >= 0
            AND loginDelaySeconds IN (0, 30, 60, 120, 240, 480, 900));
END;
GO
