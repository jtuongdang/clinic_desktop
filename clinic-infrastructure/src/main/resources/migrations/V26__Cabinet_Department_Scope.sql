SET NOCOUNT ON;

CREATE TABLE dbo.TuTrucKhoKhoa (
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maCoSo INT NOT NULL,
    maKhoa INT NOT NULL,
    maKho INT NOT NULL,
    createdAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    createdBy INT NULL,
    CONSTRAINT FK_TuTrucKhoKhoa_CoSo FOREIGN KEY (maCoSo) REFERENCES dbo.CoSo(id),
    CONSTRAINT FK_TuTrucKhoKhoa_Khoa FOREIGN KEY (maKhoa) REFERENCES dbo.Khoa(id),
    CONSTRAINT FK_TuTrucKhoKhoa_Kho FOREIGN KEY (maKho) REFERENCES dbo.Kho(id),
    CONSTRAINT UQ_TuTrucKhoKhoa UNIQUE (maCoSo,maKhoa,maKho)
);
CREATE INDEX IX_TuTrucKhoKhoa_Kho ON dbo.TuTrucKhoKhoa(maKho,maCoSo,maKhoa);

-- Safe bootstrap only where the existing warehouse owner has exactly one active
-- department in the same facility. Ambiguous/unassigned cabinets need admin mapping.
WITH OwnerDepartment AS (
    SELECT k.id maKho,k.maCoSo,nk.maKhoa,
           COUNT(*) OVER(PARTITION BY k.id) departmentCount
    FROM dbo.Kho k
    JOIN dbo.NhanVienKhoa nk ON nk.maNhanVien=k.maNguoiPhuTrach AND nk.isDeleted=0
    JOIN dbo.Khoa d ON d.id=nk.maKhoa AND d.maCoSo=k.maCoSo AND d.hoatDong=1 AND d.isDeleted=0
    WHERE k.isDeleted=0 AND k.trangThai='HOAT_DONG' AND k.loaiKho LIKE 'TU[_]TRUC%'
)
INSERT dbo.TuTrucKhoKhoa(maCoSo,maKhoa,maKho)
SELECT maCoSo,maKhoa,maKho FROM OwnerDepartment WHERE departmentCount=1;

INSERT dbo.MaTranPhanQuyen(maVaiTro,phanHe,hanhDong,phamViDuLieu,duocPhep)
SELECT r.id,'INVENTORY','SUA','KHOA_PHONG',1
FROM dbo.VaiTro r
WHERE r.tenVaiTro=N'ADMIN' AND NOT EXISTS(
    SELECT 1 FROM dbo.MaTranPhanQuyen p
    WHERE p.maVaiTro=r.id AND p.phanHe='INVENTORY' AND p.hanhDong='SUA' AND p.isDeleted=0
);
