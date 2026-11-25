SET NOCOUNT ON;

INSERT dbo.MaTranPhanQuyen(maVaiTro,phanHe,hanhDong,phamViDuLieu,duocPhep)
SELECT r.id,'INVENTORY','GHI_NHAN_TIEU_HAO','KHOA_PHONG',1
FROM (VALUES(N'ADMIN'),(N'DOCTOR'),(N'LAB_TECH')) expected(roleName)
JOIN dbo.VaiTro r ON r.tenVaiTro=expected.roleName
WHERE NOT EXISTS(SELECT 1 FROM dbo.MaTranPhanQuyen p WHERE p.maVaiTro=r.id AND p.phanHe='INVENTORY' AND p.hanhDong='GHI_NHAN_TIEU_HAO' AND p.isDeleted=0);

-- Kỹ thuật viên CLS cần đọc định mức/hao phí và báo cáo trong khoa được phân quyền.
INSERT dbo.MaTranPhanQuyen(maVaiTro,phanHe,hanhDong,phamViDuLieu,duocPhep)
SELECT r.id,'INVENTORY','XEM','KHOA_PHONG',1
FROM dbo.VaiTro r
WHERE r.tenVaiTro=N'LAB_TECH'
  AND NOT EXISTS(SELECT 1 FROM dbo.MaTranPhanQuyen p WHERE p.maVaiTro=r.id AND p.phanHe='INVENTORY' AND p.hanhDong='XEM' AND p.phamViDuLieu='KHOA_PHONG' AND p.isDeleted=0);
