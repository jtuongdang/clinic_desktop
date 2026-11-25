SET NOCOUNT ON;

-- Validate legacy data before changing schema. Never rewrite or delete permission rows.
IF EXISTS (
    SELECT 1
    FROM dbo.MaTranPhanQuyen
    WHERE isDeleted = 0
      AND duocPhep = 1
      AND (phamViDuLieu IS NULL OR phamViDuLieu <> 'KHOA_PHONG')
)
BEGIN
    THROW 51070, 'V7 refused: active ALLOW permission rows must use KHOA_PHONG scope.', 1;
END;

IF EXISTS (
    SELECT 1
    FROM dbo.MaTranPhanQuyen
    WHERE isDeleted = 0
    GROUP BY maVaiTro, phanHe, hanhDong
    HAVING COUNT_BIG(*) > 1
)
BEGIN
    THROW 51071, 'V7 refused: duplicate active role/module/action permission rows exist.', 1;
END;
GO

-- Replace the V1 defaults without touching any existing data.
DECLARE @dropPhamViDefault NVARCHAR(MAX);
SELECT @dropPhamViDefault = N'ALTER TABLE dbo.MaTranPhanQuyen DROP CONSTRAINT '
        + QUOTENAME(dc.name) + N';'
FROM sys.default_constraints dc
JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
WHERE dc.parent_object_id = OBJECT_ID(N'dbo.MaTranPhanQuyen') AND c.name = N'phamViDuLieu';
IF @dropPhamViDefault IS NOT NULL EXEC sys.sp_executesql @dropPhamViDefault;

DECLARE @dropDuocPhepDefault NVARCHAR(MAX);
SELECT @dropDuocPhepDefault = N'ALTER TABLE dbo.MaTranPhanQuyen DROP CONSTRAINT '
        + QUOTENAME(dc.name) + N';'
FROM sys.default_constraints dc
JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
WHERE dc.parent_object_id = OBJECT_ID(N'dbo.MaTranPhanQuyen') AND c.name = N'duocPhep';
IF @dropDuocPhepDefault IS NOT NULL EXEC sys.sp_executesql @dropDuocPhepDefault;

IF NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = N'DF_MaTranPhanQuyen_PhamViDuLieu')
    ALTER TABLE dbo.MaTranPhanQuyen ADD CONSTRAINT DF_MaTranPhanQuyen_PhamViDuLieu
        DEFAULT 'KHOA_PHONG' FOR phamViDuLieu;
IF NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = N'DF_MaTranPhanQuyen_DuocPhep')
    ALTER TABLE dbo.MaTranPhanQuyen ADD CONSTRAINT DF_MaTranPhanQuyen_DuocPhep
        DEFAULT 0 FOR duocPhep;

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_MaTranPhanQuyen_ActiveAllowScope')
    ALTER TABLE dbo.MaTranPhanQuyen WITH CHECK ADD CONSTRAINT CK_MaTranPhanQuyen_ActiveAllowScope
        CHECK (isDeleted = 1 OR duocPhep = 0 OR
               (phamViDuLieu IS NOT NULL AND phamViDuLieu = 'KHOA_PHONG'));

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID(N'dbo.MaTranPhanQuyen')
                 AND name = N'UX_MaTranPhanQuyen_Active_Role_Module_Action')
    CREATE UNIQUE INDEX UX_MaTranPhanQuyen_Active_Role_Module_Action
        ON dbo.MaTranPhanQuyen (maVaiTro, phanHe, hanhDong)
        WHERE isDeleted = 0;
GO
