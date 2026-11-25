-- Legacy history rows remain unscoped. Never infer ownership from patient-facility links or encounters.
IF COL_LENGTH('dbo.TienSuBenh', 'maCoSo') IS NULL
BEGIN
    ALTER TABLE dbo.TienSuBenh ADD maCoSo INT NULL;
END;
GO

IF COL_LENGTH('dbo.TienSuBenh', 'maKhoa') IS NULL
BEGIN
    ALTER TABLE dbo.TienSuBenh ADD maKhoa INT NULL;
END;
GO

-- SQL Server requires a declared unique key matching the referenced composite FK columns.
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.Khoa') AND name = 'UX_Khoa_Id_MaCoSo'
)
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UX_Khoa_Id_MaCoSo ON dbo.Khoa(id, maCoSo);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('dbo.TienSuBenh')
      AND name = 'CK_TienSuBenh_Facility_Department_Pair'
)
BEGIN
    ALTER TABLE dbo.TienSuBenh WITH CHECK
        ADD CONSTRAINT CK_TienSuBenh_Facility_Department_Pair
        CHECK ((maCoSo IS NULL AND maKhoa IS NULL) OR (maCoSo IS NOT NULL AND maKhoa IS NOT NULL));
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID('dbo.TienSuBenh')
      AND name = 'FK_TienSuBenh_Khoa_Facility'
)
BEGIN
    ALTER TABLE dbo.TienSuBenh WITH CHECK
        ADD CONSTRAINT FK_TienSuBenh_Khoa_Facility
        FOREIGN KEY (maKhoa, maCoSo) REFERENCES dbo.Khoa(id, maCoSo);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.TienSuBenh')
      AND name = 'IX_TienSuBenh_Facility_Department_Patient'
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_TienSuBenh_Facility_Department_Patient
        ON dbo.TienSuBenh(maCoSo, maKhoa, maBenhNhan);
END;
GO

-- Existing rows remain NULL/NULL, but new rows must provide a verified owner pair.
IF OBJECT_ID(N'dbo.TR_TienSuBenh_Require_Scope_On_Insert', N'TR') IS NULL
BEGIN
    EXEC(N'
        CREATE TRIGGER dbo.TR_TienSuBenh_Require_Scope_On_Insert
        ON dbo.TienSuBenh
        AFTER INSERT
        AS
        BEGIN
            SET NOCOUNT ON;
            IF EXISTS (SELECT 1 FROM inserted WHERE maCoSo IS NULL OR maKhoa IS NULL)
                THROW 51010, ''New TienSuBenh rows require facility and department ownership.'', 1;
        END
    ');
END;
