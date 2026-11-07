-- Seed script for clinicDb to enable complete End-to-End UAT testing
USE clinicDb;
GO
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

BEGIN TRANSACTION;

-- 1. TÀI KHOẢN NHÂN VIÊN DEMO CHUẨN UAT (Password: Password123!)
-- Hash PBKDF2 chuẩn: pbkdf2_sha256$210000$1CmHHyyKRFqB70+3bqIiAQ==$HccuNSaYa4j7W001EdKDppnLGOPBpdK99ClDkwnkfWA=
DECLARE @demoHash VARCHAR(255) = 'pbkdf2_sha256$210000$1CmHHyyKRFqB70+3bqIiAQ==$HccuNSaYa4j7W001EdKDppnLGOPBpdK99ClDkwnkfWA=';

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'doctor')
BEGIN
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan, isDeleted)
    VALUES ('BS0002', N'Lê Thị', N'Bác Sĩ', '0988888888', 'doctor@ehc.vn', N'Bắc Ninh', 'F', '1990-08-15', 3, 'doctor', @demoHash, 1, N'008888/BYT-CCHN', '2019-01-01', 20000000, 0);
END;

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'director')
BEGIN
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan, isDeleted)
    VALUES ('GD0001', N'Trần Văn', N'Giám Đốc', '0901234567', 'director@ehc.vn', N'Hà Nội', 'M', '1975-03-20', 1, 'director', @demoHash, 1, N'000001/BYT-CCHN', '2010-01-01', 35000000, 0);
END;

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'receptionist')
BEGIN
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan, isDeleted)
    VALUES ('LT0001', N'Hoàng Thị', N'Lễ Tân', '0912111222', 'receptionist@ehc.vn', N'Bắc Ninh', 'F', '1996-10-10', 4, 'receptionist', @demoHash, 1, NULL, '2021-03-01', 10000000, 0);
END;

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'pharmacist')
BEGIN
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan, isDeleted)
    VALUES ('DS0001', N'Phạm Văn', N'Dược Sĩ', '0933444555', 'pharmacist@ehc.vn', N'Bắc Ninh', 'M', '1992-12-05', 5, 'pharmacist', @demoHash, 1, N'009999/BYT-CCHN', '2020-05-15', 14000000, 0);
END;

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'accountant')
BEGIN
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan, isDeleted)
    VALUES ('TN0001', N'Vũ Thị', N'Thu Ngân', '0944555666', 'accountant@ehc.vn', N'Bắc Ninh', 'F', '1995-07-22', 4, 'accountant', @demoHash, 1, NULL, '2021-06-01', 11000000, 0);
END;

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'cashier')
BEGIN
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan, isDeleted)
    VALUES ('TN0002', N'Đặng Văn', N'Thu Ngân', '0944555777', 'cashier@ehc.vn', N'Bắc Ninh', 'M', '1994-04-18', 4, 'cashier', @demoHash, 1, NULL, '2021-08-01', 11000000, 0);
END;

-- 2. GÁN VAI TRÒ (NhanVienVaiTro)
-- Vai trò: 1=ADMIN, 2=DOCTOR, 3=RECEPTIONIST, 4=PHARMACIST, 5=CASHIER
INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro)
SELECT nv.id, 2 FROM NhanVien nv WHERE nv.username = 'doctor' AND NOT EXISTS (SELECT 1 FROM NhanVienVaiTro WHERE maNhanVien = nv.id AND maVaiTro = 2);

INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro)
SELECT nv.id, 1 FROM NhanVien nv WHERE nv.username = 'director' AND NOT EXISTS (SELECT 1 FROM NhanVienVaiTro WHERE maNhanVien = nv.id AND maVaiTro = 1);

INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro)
SELECT nv.id, 3 FROM NhanVien nv WHERE nv.username = 'receptionist' AND NOT EXISTS (SELECT 1 FROM NhanVienVaiTro WHERE maNhanVien = nv.id AND maVaiTro = 3);

INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro)
SELECT nv.id, 4 FROM NhanVien nv WHERE nv.username = 'pharmacist' AND NOT EXISTS (SELECT 1 FROM NhanVienVaiTro WHERE maNhanVien = nv.id AND maVaiTro = 4);

INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro)
SELECT nv.id, 5 FROM NhanVien nv WHERE nv.username IN ('accountant', 'cashier') AND NOT EXISTS (SELECT 1 FROM NhanVienVaiTro WHERE maNhanVien = nv.id AND maVaiTro = 5);

-- 3. GÁN CƠ SỞ (PhanQuyenCoSo) & KHOA PHÒNG (NhanVienKhoa)
INSERT INTO PhanQuyenCoSo (maNhanVien, maCoSo)
SELECT nv.id, 1 FROM NhanVien nv WHERE nv.username IN ('doctor', 'director', 'receptionist', 'pharmacist', 'accountant', 'cashier')
  AND NOT EXISTS (SELECT 1 FROM PhanQuyenCoSo WHERE maNhanVien = nv.id AND maCoSo = 1);

INSERT INTO NhanVienKhoa (maNhanVien, maKhoa)
SELECT nv.id, CASE WHEN nv.username = 'pharmacist' THEN 5 ELSE 1 END
FROM NhanVien nv 
WHERE nv.username IN ('doctor', 'director', 'receptionist', 'pharmacist', 'accountant', 'cashier')
  AND NOT EXISTS (SELECT 1 FROM NhanVienKhoa WHERE maNhanVien = nv.id);

-- 4. BỔ SUNG BỆNH NHÂN MẪU (BenhNhan)
IF NOT EXISTS (SELECT 1 FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261107-0003')
BEGIN
    INSERT INTO BenhNhan (maBenhNhanFormat, ho, ten, ngaySinh, gioiTinh, dienThoai, soCCCD, diaChi, isDeleted)
    VALUES ('BN-20261107-0003', N'Nguyễn Hoàng', N'Long', '1995-04-12', 'M', '0912888999', '001095012345', N'Số 12 Lý Thái Tổ, TP. Bắc Ninh', 0);
END;

IF NOT EXISTS (SELECT 1 FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261107-0004')
BEGIN
    INSERT INTO BenhNhan (maBenhNhanFormat, ho, ten, ngaySinh, gioiTinh, dienThoai, soCCCD, diaChi, isDeleted)
    VALUES ('BN-20261107-0004', N'Phạm Quỳnh', N'Trang', '2018-09-20', 'F', '0988777666', '001118098765', N'Đồng Kỵ, Từ Sơn, Bắc Ninh', 0);
END;

-- 5. BỔ SUNG DANH MỤC THUỐC & TỒN KHO FEFO (Item, TonKho)
IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'THUOC004')
BEGIN
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, giaNhapMacDinh, giaBanMacDinh, hoatDong, isDeleted)
    VALUES ('THUOC004', N'Amoxicillin 500mg', 'THUOC', N'Amoxicillin', N'500mg', N'Viên', 2500, 3500, 1, 0);
END;

IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'THUOC005')
BEGIN
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, giaNhapMacDinh, giaBanMacDinh, hoatDong, isDeleted)
    VALUES ('THUOC005', N'Cefixim 200mg', 'THUOC', N'Cefixim', N'200mg', N'Viên', 6000, 8500, 1, 0);
END;

IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'THUOC006')
BEGIN
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, giaNhapMacDinh, giaBanMacDinh, hoatDong, isDeleted)
    VALUES ('THUOC006', N'Oresol 245mg', 'THUOC', N'Glucose + Muối khoáng', N'245mg', N'Gói', 2800, 4000, 1, 0);
END;

-- Tồn kho cho các thuốc mới tại Kho 1 (Kho Ngoại Trú)
DECLARE @item4Id INT = (SELECT id FROM Item WHERE maItem = 'THUOC004');
DECLARE @item5Id INT = (SELECT id FROM Item WHERE maItem = 'THUOC005');
DECLARE @item6Id INT = (SELECT id FROM Item WHERE maItem = 'THUOC006');

IF @item4Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM TonKho WHERE maItem = @item4Id AND maKho = 1)
BEGIN
    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, isDeleted)
    VALUES (1, @item4Id, 'AMOX-2026A', '2028-10-15', 1200.0, 2500.0, 3500.0, 0);
END;

IF @item5Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM TonKho WHERE maItem = @item5Id AND maKho = 1)
BEGIN
    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, isDeleted)
    VALUES (1, @item5Id, 'CEF-2026B', '2028-12-31', 800.0, 6000.0, 8500.0, 0);
END;

IF @item6Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM TonKho WHERE maItem = @item6Id AND maKho = 1)
BEGIN
    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, isDeleted)
    VALUES (1, @item6Id, 'ORE-2026C', '2029-06-30', 2500.0, 2800.0, 4000.0, 0);
END;

-- 6. TẠO HÀNG ĐỢI KHÁM MẪU (LuotKham) CHO PHÒNG KHÁM 1 VÀ 2 HÔM NAY
DECLARE @bn3Id INT = (SELECT id FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261107-0003');
DECLARE @bn4Id INT = (SELECT id FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261107-0004');

IF @bn3Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM LuotKham WHERE maBenhNhan = @bn3Id)
BEGIN
    INSERT INTO LuotKham (maBenhNhan, maCoSo, maKhoa, maPhongKham, hinhThucDieuTri, trangThai, soThuTu, soThuTuDisplay, ngayKham, thoiGianTiepNhan, lyDoKham, doiTuongKham, isDeleted)
    VALUES (@bn3Id, 1, 1, 1, 'NGOAI_TRU', 'CHO_KHAM', 1, '01', CAST(GETDATE() AS DATE), GETDATE(), N'Sốt nhẹ kèm ho khan 2 ngày', 'THUONG', 0);
END;

IF @bn4Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM LuotKham WHERE maBenhNhan = @bn4Id)
BEGIN
    INSERT INTO LuotKham (maBenhNhan, maCoSo, maKhoa, maPhongKham, hinhThucDieuTri, trangThai, soThuTu, soThuTuDisplay, ngayKham, thoiGianTiepNhan, lyDoKham, doiTuongKham, isDeleted)
    VALUES (@bn4Id, 1, 1, 1, 'NGOAI_TRU', 'CHO_KHAM', 2, '02', CAST(GETDATE() AS DATE), GETDATE(), N'Kiểm tra định kỳ và viêm họng', 'THUONG', 0);
END;

COMMIT TRANSACTION;
GO
