SET NOCOUNT ON;

DECLARE @ApprovedGrants TABLE (
    roleName NVARCHAR(100) NOT NULL,
    phanHe VARCHAR(50) NOT NULL,
    hanhDong VARCHAR(50) NOT NULL,
    PRIMARY KEY (roleName, phanHe, hanhDong)
);

INSERT INTO @ApprovedGrants (roleName, phanHe, hanhDong) VALUES
    (N'ADMIN', 'HOME', 'XEM'),
    (N'ADMIN', 'CATALOG', 'XEM'),
    (N'ADMIN', 'CATALOG', 'TAO'),
    (N'ADMIN', 'CATALOG', 'SUA'),
    (N'ADMIN', 'SYSTEM', 'XEM'),
    (N'ADMIN', 'SYSTEM', 'TAO'),
    (N'ADMIN', 'SYSTEM', 'SUA'),

    (N'DOCTOR', 'HOME', 'XEM'),
    (N'DOCTOR', 'PATIENT', 'XEM'),
    (N'DOCTOR', 'RECEPTION', 'XEM'),
    (N'DOCTOR', 'RECEPTION', 'SUA'),
    (N'DOCTOR', 'APPOINTMENT', 'XEM'),
    (N'DOCTOR', 'APPOINTMENT', 'TAO'),
    (N'DOCTOR', 'APPOINTMENT', 'SUA'),
    (N'DOCTOR', 'EMR', 'XEM'),
    (N'DOCTOR', 'EMR', 'TAO'),
    (N'DOCTOR', 'EMR', 'SUA'),
    (N'DOCTOR', 'EMR', 'DUYET'),
    (N'DOCTOR', 'CLS', 'XEM'),
    (N'DOCTOR', 'CLS', 'TAO'),
    (N'DOCTOR', 'PRESCRIPTION', 'XEM'),
    (N'DOCTOR', 'PRESCRIPTION', 'TAO'),
    (N'DOCTOR', 'PRESCRIPTION', 'SUA'),
    (N'DOCTOR', 'INVENTORY', 'XEM'),
    (N'DOCTOR', 'BILLING', 'XEM'),
    (N'DOCTOR', 'CATALOG', 'XEM'),

    (N'RECEPTIONIST', 'HOME', 'XEM'),
    (N'RECEPTIONIST', 'PATIENT', 'XEM'),
    (N'RECEPTIONIST', 'PATIENT', 'TAO'),
    (N'RECEPTIONIST', 'PATIENT', 'SUA'),
    (N'RECEPTIONIST', 'RECEPTION', 'XEM'),
    (N'RECEPTIONIST', 'RECEPTION', 'TAO'),
    (N'RECEPTIONIST', 'RECEPTION', 'SUA'),
    (N'RECEPTIONIST', 'APPOINTMENT', 'XEM'),
    (N'RECEPTIONIST', 'APPOINTMENT', 'TAO'),
    (N'RECEPTIONIST', 'APPOINTMENT', 'SUA'),
    (N'RECEPTIONIST', 'CATALOG', 'XEM'),

    (N'PHARMACIST', 'HOME', 'XEM'),
    (N'PHARMACIST', 'PATIENT', 'XEM'),
    (N'PHARMACIST', 'PRESCRIPTION', 'XEM'),
    (N'PHARMACIST', 'INVENTORY', 'XEM'),
    (N'PHARMACIST', 'INVENTORY', 'TAO'),
    (N'PHARMACIST', 'INVENTORY', 'SUA'),
    (N'PHARMACIST', 'CATALOG', 'XEM'),

    (N'CASHIER', 'HOME', 'XEM'),
    (N'CASHIER', 'PATIENT', 'XEM'),
    (N'CASHIER', 'APPOINTMENT', 'XEM'),
    (N'CASHIER', 'CLS', 'XEM'),
    (N'CASHIER', 'BILLING', 'XEM'),
    (N'CASHIER', 'BILLING', 'TAO'),
    (N'CASHIER', 'BILLING', 'SUA'),
    (N'CASHIER', 'CATALOG', 'XEM'),

    (N'LAB_TECH', 'HOME', 'XEM'),
    (N'LAB_TECH', 'PATIENT', 'XEM'),
    (N'LAB_TECH', 'CLS', 'XEM'),
    (N'LAB_TECH', 'CLS', 'SUA'),
    (N'LAB_TECH', 'CATALOG', 'XEM');

DECLARE @ExpectedRoles TABLE (roleName NVARCHAR(100) NOT NULL PRIMARY KEY);
INSERT INTO @ExpectedRoles (roleName)
SELECT DISTINCT roleName FROM @ApprovedGrants;

IF EXISTS (
    SELECT expected.roleName
    FROM @ExpectedRoles expected
    LEFT JOIN dbo.VaiTro vaiTro ON vaiTro.tenVaiTro = expected.roleName
    GROUP BY expected.roleName
    HAVING COUNT(vaiTro.id) <> 1
)
BEGIN
    THROW 51080, 'V8 refused: each approved role name must resolve to exactly one stored role.', 1;
END;

INSERT INTO dbo.MaTranPhanQuyen (maVaiTro, phanHe, hanhDong, phamViDuLieu, duocPhep)
SELECT vaiTro.id, grantRow.phanHe, grantRow.hanhDong, 'KHOA_PHONG', 1
FROM @ApprovedGrants grantRow
JOIN dbo.VaiTro vaiTro ON vaiTro.tenVaiTro = grantRow.roleName
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.MaTranPhanQuyen existing
    WHERE existing.maVaiTro = vaiTro.id
      AND existing.phanHe = grantRow.phanHe
      AND existing.hanhDong = grantRow.hanhDong
      AND existing.isDeleted = 0
);
