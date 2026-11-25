-- Chỉ lưu giá của lượt đăng ký mới; không suy diễn giá lịch sử từ danh mục hiện tại.
IF OBJECT_ID(N'dbo.DangKyKhamGia', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DangKyKhamGia (
        maLuotKham INT NOT NULL PRIMARY KEY,
        maDichVu INT NOT NULL,
        tenDichVu NVARCHAR(255) NOT NULL,
        soLuong DECIMAL(18,2) NOT NULL DEFAULT 1,
        donGia DECIMAL(18,2) NOT NULL,
        createdAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        createdBy INT NOT NULL,
        CONSTRAINT FK_DangKyKhamGia_LuotKham FOREIGN KEY (maLuotKham) REFERENCES LuotKham(id),
        CONSTRAINT FK_DangKyKhamGia_DichVu FOREIGN KEY (maDichVu) REFERENCES DanhMucDichVu(id),
        CONSTRAINT CK_DangKyKhamGia_Gia CHECK (donGia >= 0 AND soLuong > 0)
    );
END;

-- Bổ sung quyền cho tính năng mới ở vai trò tiếp đón/quản trị đã có quyền xem tiếp đón.
-- Giữ nguyên mọi cấu hình cho phép/từ chối đã tồn tại, không mở thêm phạm vi dữ liệu.
INSERT INTO MaTranPhanQuyen (maVaiTro, phanHe, hanhDong, phamViDuLieu, duocPhep)
SELECT DISTINCT viewGrant.maVaiTro, 'RECEPTION', actions.hanhDong, 'KHOA_PHONG', 1
FROM MaTranPhanQuyen viewGrant
INNER JOIN VaiTro role ON role.id=viewGrant.maVaiTro AND role.isDeleted=0
CROSS JOIN (VALUES ('IN'), ('XUAT_FILE')) actions(hanhDong)
WHERE viewGrant.phanHe='RECEPTION' AND viewGrant.hanhDong='XEM'
  AND viewGrant.duocPhep=1 AND viewGrant.isDeleted=0 AND viewGrant.phamViDuLieu='KHOA_PHONG'
  AND UPPER(role.tenVaiTro) IN ('RECEPTIONIST','ADMIN')
  AND NOT EXISTS (SELECT 1 FROM MaTranPhanQuyen existing
      WHERE existing.maVaiTro=viewGrant.maVaiTro AND existing.phanHe='RECEPTION'
      AND existing.hanhDong=actions.hanhDong AND existing.isDeleted=0);
