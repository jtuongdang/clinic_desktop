-- ==============================================================================
-- SCRIPT NẠP TOÀN BỘ DỮ LIỆU TEST CHUẨN UAT CHO CSDL TEST SONG SONG (clinicDb_test)
-- Phủ kín 100% quy trình: Tiếp đón -> Khám -> Cận lâm sàng -> Dược -> Viện phí -> Báo cáo
-- ==============================================================================

USE clinicDb_test;
GO
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

BEGIN TRANSACTION;

-- 1. HASH MẬT KHẨU DEMO CHO TẤT CẢ TÀI KHOẢN (Password: Password123!)
DECLARE @demoHash VARCHAR(255) = 'pbkdf2_sha256$210000$1CmHHyyKRFqB70+3bqIiAQ==$HccuNSaYa4j7W001EdKDppnLGOPBpdK99ClDkwnkfWA=';

-- 2. CƠ SỞ Y TẾ (CoSo) & KHOA PHÒNG (Khoa, PhongKham)
IF NOT EXISTS (SELECT 1 FROM CoSo WHERE tenCoSo LIKE N'%EHC Bắc Ninh%')
    INSERT INTO CoSo (tenCoSo, diaChi, dienThoai, hoatDong)
    VALUES (N'Bệnh Viện Đa Khoa Quốc Tế EHC Bắc Ninh', N'Số 12 Đường Lý Thái Tổ, Phường Ninh Xá, TP. Bắc Ninh', '02223821234', 1);

DECLARE @cs1Id INT = (SELECT TOP 1 id FROM CoSo ORDER BY id);

-- KHOA
IF NOT EXISTS (SELECT 1 FROM Khoa WHERE tenKhoa = N'Khoa Khám Bệnh')
    INSERT INTO Khoa (maCoSo, tenKhoa, moTa, hoatDong) VALUES (@cs1Id, N'Khoa Khám Bệnh', N'Tiếp đón & khám chữa bệnh ngoại trú', 1);
IF NOT EXISTS (SELECT 1 FROM Khoa WHERE tenKhoa = N'Khoa Xét Nghiệm')
    INSERT INTO Khoa (maCoSo, tenKhoa, moTa, hoatDong) VALUES (@cs1Id, N'Khoa Xét Nghiệm', N'Huyết học, sinh hóa, miễn dịch, vi sinh', 1);
IF NOT EXISTS (SELECT 1 FROM Khoa WHERE tenKhoa = N'Khoa Chẩn Đoán Hình Ảnh')
    INSERT INTO Khoa (maCoSo, tenKhoa, moTa, hoatDong) VALUES (@cs1Id, N'Khoa Chẩn Đoán Hình Ảnh', N'X-Quang kỹ thuật số, Siêu âm Doppler, CT-Scanner', 1);
IF NOT EXISTS (SELECT 1 FROM Khoa WHERE tenKhoa = N'Khoa Dược')
    INSERT INTO Khoa (maCoSo, tenKhoa, moTa, hoatDong) VALUES (@cs1Id, N'Khoa Dược', N'Kho dược chính & tủ trực thuốc viện phí', 1);
IF NOT EXISTS (SELECT 1 FROM Khoa WHERE tenKhoa = N'Khoa Thu Ngân & Viện Phí')
    INSERT INTO Khoa (maCoSo, tenKhoa, moTa, hoatDong) VALUES (@cs1Id, N'Khoa Thu Ngân & Viện Phí', N'Quầy thu ngân viện phí và giám định BHYT', 1);

DECLARE @khoaKhamId INT = (SELECT TOP 1 id FROM Khoa WHERE tenKhoa = N'Khoa Khám Bệnh');
DECLARE @khoaXnId INT = (SELECT TOP 1 id FROM Khoa WHERE tenKhoa = N'Khoa Xét Nghiệm');
DECLARE @khoaCdhaId INT = (SELECT TOP 1 id FROM Khoa WHERE tenKhoa = N'Khoa Chẩn Đoán Hình Ảnh');
DECLARE @khoaDuocId INT = (SELECT TOP 1 id FROM Khoa WHERE tenKhoa = N'Khoa Dược');
DECLARE @khoaVienPhiId INT = (SELECT TOP 1 id FROM Khoa WHERE tenKhoa = N'Khoa Thu Ngân & Viện Phí');
IF @khoaCdhaId IS NULL SET @khoaCdhaId = 4;
IF @khoaXnId IS NULL SET @khoaXnId = 4;
IF @khoaVienPhiId IS NULL SET @khoaVienPhiId = 1;

-- PHÒNG KHÁM
IF NOT EXISTS (SELECT 1 FROM PhongKham WHERE tenPhong = N'Phòng Khám Nội Tổng Hợp')
    INSERT INTO PhongKham (maKhoa, tenPhong, viTri, hoatDong) VALUES (@khoaKhamId, N'Phòng Khám Nội Tổng Hợp', N'Tầng 1 - Khu A', 1);
IF NOT EXISTS (SELECT 1 FROM PhongKham WHERE tenPhong = N'Phòng Khám Nhi Khoa')
    INSERT INTO PhongKham (maKhoa, tenPhong, viTri, hoatDong) VALUES (@khoaKhamId, N'Phòng Khám Nhi Khoa', N'Tầng 1 - Khu B', 1);
IF NOT EXISTS (SELECT 1 FROM PhongKham WHERE tenPhong = N'Phòng Khám Tai Mũi Họng')
    INSERT INTO PhongKham (maKhoa, tenPhong, viTri, hoatDong) VALUES (@khoaKhamId, N'Phòng Khám Tai Mũi Họng', N'Tầng 2 - Khu A', 1);
IF NOT EXISTS (SELECT 1 FROM PhongKham WHERE tenPhong = N'Phòng Khám Răng Hàm Mặt')
    INSERT INTO PhongKham (maKhoa, tenPhong, viTri, hoatDong) VALUES (@khoaKhamId, N'Phòng Khám Răng Hàm Mặt', N'Tầng 2 - Khu B', 1);

-- 3. DANH SÁCH TÀI KHOẢN NHÂN SỰ ĐẦY ĐỦ CÁC VAI TRÒ
IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'admin')
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, isDeleted)
    VALUES ('NV0001', N'Quản Trị', N'Hệ Thống', '0912000001', 'admin@ehc.vn', N'TP. Bắc Ninh', 'M', '1985-01-01', 1, 'admin', @demoHash, 1, 0);

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'doctor')
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, isDeleted)
    VALUES ('BS0003', N'Lê Thị', N'Bác Sĩ', '0988888888', 'doctor@ehc.vn', N'TP. Bắc Ninh', 'F', '1990-08-15', 3, 'doctor', @demoHash, 1, N'008888/BYT-CCHN', 0);

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'doctor2')
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, isDeleted)
    VALUES ('BS0002', N'Nguyễn Văn', N'Trưởng Khoa', '0988999888', 'doctor2@ehc.vn', N'TP. Bắc Ninh', 'M', '1982-05-10', 2, 'doctor2', @demoHash, 1, N'007777/BYT-CCHN', 0);

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'receptionist')
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, isDeleted)
    VALUES ('LT0001', N'Hoàng Thị', N'Lễ Tân', '0912111222', 'receptionist@ehc.vn', N'TP. Bắc Ninh', 'F', '1996-10-10', 4, 'receptionist', @demoHash, 1, 0);

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'pharmacist')
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, isDeleted)
    VALUES ('DS0001', N'Phạm Văn', N'Dược Sĩ', '0933444555', 'pharmacist@ehc.vn', N'TP. Bắc Ninh', 'M', '1992-12-05', 5, 'pharmacist', @demoHash, 1, N'009999/BYT-CCHN', 0);

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'cashier')
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, isDeleted)
    VALUES ('TN0001', N'Vũ Thị', N'Thu Ngân', '0944555666', 'cashier@ehc.vn', N'TP. Bắc Ninh', 'F', '1995-07-22', 4, 'cashier', @demoHash, 1, 0);

IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE username = 'lab_tech')
    INSERT INTO NhanVien (maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, isDeleted)
    VALUES ('KTV001', N'Bùi Văn', N'Xét Nghiệm', '0966777888', 'lab@ehc.vn', N'TP. Bắc Ninh', 'M', '1994-03-12', 3, 'lab_tech', @demoHash, 1, 0);

-- GÁN VAI TRÒ (NhanVienVaiTro)
INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro)
SELECT nv.id, 1 FROM NhanVien nv WHERE nv.username = 'admin' AND NOT EXISTS (SELECT 1 FROM NhanVienVaiTro WHERE maNhanVien = nv.id AND maVaiTro = 1);
INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro)
SELECT nv.id, 2 FROM NhanVien nv WHERE nv.username IN ('doctor', 'doctor2') AND NOT EXISTS (SELECT 1 FROM NhanVienVaiTro WHERE maNhanVien = nv.id AND maVaiTro = 2);
INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro)
SELECT nv.id, 3 FROM NhanVien nv WHERE nv.username = 'receptionist' AND NOT EXISTS (SELECT 1 FROM NhanVienVaiTro WHERE maNhanVien = nv.id AND maVaiTro = 3);
INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro)
SELECT nv.id, 4 FROM NhanVien nv WHERE nv.username = 'pharmacist' AND NOT EXISTS (SELECT 1 FROM NhanVienVaiTro WHERE maNhanVien = nv.id AND maVaiTro = 4);
INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro)
SELECT nv.id, 5 FROM NhanVien nv WHERE nv.username = 'cashier' AND NOT EXISTS (SELECT 1 FROM NhanVienVaiTro WHERE maNhanVien = nv.id AND maVaiTro = 5);
INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro)
SELECT nv.id, 6 FROM NhanVien nv WHERE nv.username = 'lab_tech' AND NOT EXISTS (SELECT 1 FROM NhanVienVaiTro WHERE maNhanVien = nv.id AND maVaiTro = 6);

-- GÁN CƠ SỞ & KHOA PHÒNG CHO NHÂN VIÊN
INSERT INTO PhanQuyenCoSo (maNhanVien, maCoSo)
SELECT nv.id, @cs1Id FROM NhanVien nv WHERE NOT EXISTS (SELECT 1 FROM PhanQuyenCoSo WHERE maNhanVien = nv.id AND maCoSo = @cs1Id);

INSERT INTO NhanVienKhoa (maNhanVien, maKhoa)
SELECT nv.id, 
    CASE 
        WHEN nv.username IN ('doctor', 'doctor2', 'receptionist') THEN @khoaKhamId
        WHEN nv.username = 'pharmacist' THEN @khoaDuocId
        WHEN nv.username = 'cashier' THEN @khoaVienPhiId
        WHEN nv.username = 'lab_tech' THEN @khoaXnId
        ELSE @khoaKhamId
    END
FROM NhanVien nv
WHERE NOT EXISTS (SELECT 1 FROM NhanVienKhoa WHERE maNhanVien = nv.id);

-- 4. DANH MỤC DỊCH VỤ KỸ THUẬT (DanhMucDichVu)
IF NOT EXISTS (SELECT 1 FROM DanhMucDichVu WHERE maDichVu = 'DV_KHAM_NOI')
    INSERT INTO DanhMucDichVu (maDichVu, tenDichVu, loaiDichVu, donGia, giaBHYT, hoatDong, moTa)
    VALUES ('DV_KHAM_NOI', N'Khám Nội Tổng Hợp', 'KHAM', 150000.0, 39000.0, 1, N'Khám lâm sàng nội khoa tổng quát');

IF NOT EXISTS (SELECT 1 FROM DanhMucDichVu WHERE maDichVu = 'DV_KHAM_NHI')
    INSERT INTO DanhMucDichVu (maDichVu, tenDichVu, loaiDichVu, donGia, giaBHYT, hoatDong, moTa)
    VALUES ('DV_KHAM_NHI', N'Khám Nhi Khoa', 'KHAM', 150000.0, 39000.0, 1, N'Khám chuyên khoa nhi');

IF NOT EXISTS (SELECT 1 FROM DanhMucDichVu WHERE maDichVu = 'DV_KHAM_TMH')
    INSERT INTO DanhMucDichVu (maDichVu, tenDichVu, loaiDichVu, donGia, giaBHYT, hoatDong, moTa)
    VALUES ('DV_KHAM_TMH', N'Khám Tai Mũi Họng', 'KHAM', 180000.0, 42000.0, 1, N'Khám nội soi tai mũi họng');

-- Xét nghiệm
IF NOT EXISTS (SELECT 1 FROM DanhMucDichVu WHERE maDichVu = 'XN_HUYET_HOC_18')
    INSERT INTO DanhMucDichVu (maDichVu, tenDichVu, loaiDichVu, donGia, giaBHYT, hoatDong, moTa)
    VALUES ('XN_HUYET_HOC_18', N'Tổng Phân Tích Tế Bào Máu (18 Chỉ Số)', 'XET_NGHIEM', 120000.0, 52000.0, 1, N'Đếm bạch cầu, hồng cầu, huyết sắc tố, tiểu cầu');

IF NOT EXISTS (SELECT 1 FROM DanhMucDichVu WHERE maDichVu = 'XN_GLUCOSE')
    INSERT INTO DanhMucDichVu (maDichVu, tenDichVu, loaiDichVu, donGia, giaBHYT, hoatDong, moTa)
    VALUES ('XN_GLUCOSE', N'Định Lượng Glucose Máu (Đường Huyết)', 'XET_NGHIEM', 65000.0, 24000.0, 1, N'Kiểm tra đái tháo đường');

IF NOT EXISTS (SELECT 1 FROM DanhMucDichVu WHERE maDichVu = 'XN_CREATININE')
    INSERT INTO DanhMucDichVu (maDichVu, tenDichVu, loaiDichVu, donGia, giaBHYT, hoatDong, moTa)
    VALUES ('XN_CREATININE', N'Định Lượng Creatinine Máu (Chức Năng Thận)', 'XET_NGHIEM', 70000.0, 28000.0, 1, N'Đánh giá mức lọc cầu thận');

IF NOT EXISTS (SELECT 1 FROM DanhMucDichVu WHERE maDichVu = 'XN_MEN_GAN_ALT')
    INSERT INTO DanhMucDichVu (maDichVu, tenDichVu, loaiDichVu, donGia, giaBHYT, hoatDong, moTa)
    VALUES ('XN_MEN_GAN_ALT', N'Đo Hoạt Độ Men Gan ALT (GPT)', 'XET_NGHIEM', 75000.0, 31000.0, 1, N'Kiểm tra chức năng tế bào gan');

-- Chẩn đoán hình ảnh
IF NOT EXISTS (SELECT 1 FROM DanhMucDichVu WHERE maDichVu = 'CDHA_XQ_NGUC')
    INSERT INTO DanhMucDichVu (maDichVu, tenDichVu, loaiDichVu, donGia, giaBHYT, hoatDong, moTa)
    VALUES ('CDHA_XQ_NGUC', N'Chụp X-Quang Ngực Thẳng Kỹ Thuật Số', 'CHAN_DOAN_HINH_ANH', 180000.0, 68000.0, 1, N'Khảo sát nhu mô phổi và bóng tim');

IF NOT EXISTS (SELECT 1 FROM DanhMucDichVu WHERE maDichVu = 'CDHA_SIEU_AM_BUNG')
    INSERT INTO DanhMucDichVu (maDichVu, tenDichVu, loaiDichVu, donGia, giaBHYT, hoatDong, moTa)
    VALUES ('CDHA_SIEU_AM_BUNG', N'Siêu Âm Ổ Bụng Tổng Quát', 'CHAN_DOAN_HINH_ANH', 200000.0, 75000.0, 1, N'Khảo sát gan mật tụy lách thận bàng quang');

-- 5. DANH MỤC THUỐC, VẬT TƯ & TỒN KHO FEFO
IF NOT EXISTS (SELECT 1 FROM Kho WHERE tenKho LIKE N'%Kho Dược%')
    INSERT INTO Kho (maCoSo, tenKho, loaiKho, trangThai)
    VALUES (@cs1Id, N'Kho Dược Ngoại Trú & BHYT', 'KHO_LE', 'HOAT_DONG');

DECLARE @khoDuocRecordId INT = (SELECT TOP 1 id FROM Kho WHERE tenKho LIKE N'%Kho Dược%' OR id = 1 ORDER BY id);

-- Thuốc
IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'THUOC001')
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, giaNhapMacDinh, giaBanMacDinh, hoatDong, isDeleted)
    VALUES ('THUOC001', N'Paracetamol 500mg', 'THUOC', N'Paracetamol', '500mg', N'Viên', 1200.0, 2000.0, 1, 0);

IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'THUOC002')
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, giaNhapMacDinh, giaBanMacDinh, hoatDong, isDeleted)
    VALUES ('THUOC002', N'Amoxicillin 500mg', 'THUOC', N'Amoxicillin', '500mg', N'Viên', 2500.0, 3500.0, 1, 0);

IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'THUOC003')
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, giaNhapMacDinh, giaBanMacDinh, hoatDong, isDeleted)
    VALUES ('THUOC003', N'Cefixim 200mg', 'THUOC', N'Cefixim', '200mg', N'Viên', 6000.0, 8500.0, 1, 0);

IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'THUOC004')
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, giaNhapMacDinh, giaBanMacDinh, hoatDong, isDeleted)
    VALUES ('THUOC004', N'Amlodipin 5mg', 'THUOC', N'Amlodipin', '5mg', N'Viên', 1800.0, 2800.0, 1, 0);

IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'THUOC005')
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, giaNhapMacDinh, giaBanMacDinh, hoatDong, isDeleted)
    VALUES ('THUOC005', N'Metformin 500mg', 'THUOC', N'Metformin HCl', '500mg', N'Viên', 2200.0, 3200.0, 1, 0);

-- Vật tư tiêu hao
IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'VT001')
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, giaNhapMacDinh, giaBanMacDinh, hoatDong, isDeleted)
    VALUES ('VT001', N'Bơm Cho Ăn 5ml', 'VAT_TU', N'Nhựa y tế', '5ml', N'Cái', 4500.0, 6500.0, 1, 0);

IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'VT002')
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, giaNhapMacDinh, giaBanMacDinh, hoatDong, isDeleted)
    VALUES ('VT002', N'Băng Cuộn Vô Trùng', 'VAT_TU', N'Cotton y tế', '10cm x 2m', N'Cuộn', 5000.0, 8000.0, 1, 0);

-- LÔ TỒN KHO FEFO CHO THUỐC
DECLARE @idPara INT = (SELECT id FROM Item WHERE maItem = 'THUOC001');
DECLARE @idAmox INT = (SELECT id FROM Item WHERE maItem = 'THUOC002');
DECLARE @idCef INT = (SELECT id FROM Item WHERE maItem = 'THUOC003');
DECLARE @idAmlo INT = (SELECT id FROM Item WHERE maItem = 'THUOC004');
DECLARE @idMet INT = (SELECT id FROM Item WHERE maItem = 'THUOC005');
DECLARE @idVt1 INT = (SELECT id FROM Item WHERE maItem = 'VT001');

IF @idPara IS NOT NULL AND NOT EXISTS (SELECT 1 FROM TonKho WHERE maItem = @idPara AND soLo = 'PARA-2026A')
    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, isDeleted)
    VALUES (@khoDuocRecordId, @idPara, 'PARA-2026A', '2026-12-31', 2500.0, 1200.0, 2000.0, 0);

IF @idPara IS NOT NULL AND NOT EXISTS (SELECT 1 FROM TonKho WHERE maItem = @idPara AND soLo = 'PARA-2027B')
    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, isDeleted)
    VALUES (@khoDuocRecordId, @idPara, 'PARA-2027B', '2027-08-31', 5000.0, 1300.0, 2100.0, 0);

IF @idAmox IS NOT NULL AND NOT EXISTS (SELECT 1 FROM TonKho WHERE maItem = @idAmox AND soLo = 'AMOX-2026A')
    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, isDeleted)
    VALUES (@khoDuocRecordId, @idAmox, 'AMOX-2026A', '2027-01-15', 3000.0, 2500.0, 3500.0, 0);

IF @idCef IS NOT NULL AND NOT EXISTS (SELECT 1 FROM TonKho WHERE maItem = @idCef AND soLo = 'CEF-2027A')
    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, isDeleted)
    VALUES (@khoDuocRecordId, @idCef, 'CEF-2027A', '2027-09-30', 1500.0, 6000.0, 8500.0, 0);

IF @idAmlo IS NOT NULL AND NOT EXISTS (SELECT 1 FROM TonKho WHERE maItem = @idAmlo AND soLo = 'AMLO-2027A')
    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, isDeleted)
    VALUES (@khoDuocRecordId, @idAmlo, 'AMLO-2027A', '2027-11-20', 2000.0, 1800.0, 2800.0, 0);

IF @idMet IS NOT NULL AND NOT EXISTS (SELECT 1 FROM TonKho WHERE maItem = @idMet AND soLo = 'MET-2027A')
    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, isDeleted)
    VALUES (@khoDuocRecordId, @idMet, 'MET-2027A', '2027-12-15', 2500.0, 2200.0, 3200.0, 0);

IF @idVt1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM TonKho WHERE maItem = @idVt1 AND soLo = 'VT-2028A')
    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, isDeleted)
    VALUES (@khoDuocRecordId, @idVt1, 'VT-2028A', '2028-06-30', 800.0, 4500.0, 6500.0, 0);

-- 6. DANH SÁCH BỆNH NHÂN MẪU ĐA DẠNG ĐỐI TƯỢNG (BenhNhan)
IF NOT EXISTS (SELECT 1 FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261108-0001')
    INSERT INTO BenhNhan (maBenhNhanFormat, ho, ten, ngaySinh, gioiTinh, dienThoai, soCCCD, diaChi, isDeleted)
    VALUES ('BN-20261108-0001', N'Trần Văn', N'An', '1985-05-12', 'M', '0912123456', '001085001122', N'Số 15 Phố Vũ Kiệt, TP. Bắc Ninh', 0);

IF NOT EXISTS (SELECT 1 FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261108-0002')
    INSERT INTO BenhNhan (maBenhNhanFormat, ho, ten, ngaySinh, gioiTinh, dienThoai, soCCCD, diaChi, isDeleted)
    VALUES ('BN-20261108-0002', N'Nguyễn Thị', N'Bình', '1990-09-20', 'F', '0988234567', '001090002233', N'Khu Đô Thị Nam Võ Cường, TP. Bắc Ninh', 0);

IF NOT EXISTS (SELECT 1 FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261108-0003')
    INSERT INTO BenhNhan (maBenhNhanFormat, ho, ten, ngaySinh, gioiTinh, dienThoai, soCCCD, diaChi, isDeleted)
    VALUES ('BN-20261108-0003', N'Nguyễn Hoàng', N'Long', '1995-04-15', 'M', '0977345678', '001095003344', N'Phường Suối Hoa, TP. Bắc Ninh', 0);

IF NOT EXISTS (SELECT 1 FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261108-0004')
    INSERT INTO BenhNhan (maBenhNhanFormat, ho, ten, ngaySinh, gioiTinh, dienThoai, soCCCD, diaChi, isDeleted)
    VALUES ('BN-20261108-0004', N'Phạm Quỳnh', N'Trang', '2019-11-25', 'F', '0966456789', '001119004455', N'Đồng Kỵ, TP. Từ Sơn, Bắc Ninh', 0);

IF NOT EXISTS (SELECT 1 FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261108-0005')
    INSERT INTO BenhNhan (maBenhNhanFormat, ho, ten, ngaySinh, gioiTinh, dienThoai, soCCCD, diaChi, isDeleted)
    VALUES ('BN-20261108-0005', N'Hoàng Văn', N'Đức', '1962-02-18', 'M', '0933567890', '001062005566', N'Đại Phúc, TP. Bắc Ninh', 0);

IF NOT EXISTS (SELECT 1 FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261108-0006')
    INSERT INTO BenhNhan (maBenhNhanFormat, ho, ten, ngaySinh, gioiTinh, dienThoai, soCCCD, diaChi, isDeleted)
    VALUES ('BN-20261108-0006', N'Đỗ Mỹ', N'Linh', '1998-07-08', 'F', '0944678901', '001098006677', N'Tiền An, TP. Bắc Ninh', 0);

-- GẮN BẢO HIỂM Y TẾ (BaoHiem)
DECLARE @bn1Id INT = (SELECT id FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261108-0001');
DECLARE @bn2Id INT = (SELECT id FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261108-0002');
DECLARE @bn3Id INT = (SELECT id FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261108-0003');
DECLARE @bn4Id INT = (SELECT id FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261108-0004');
DECLARE @bn5Id INT = (SELECT id FROM BenhNhan WHERE maBenhNhanFormat = 'BN-20261108-0005');

IF @bn2Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM BaoHiem WHERE maBenhNhan = @bn2Id)
    INSERT INTO BaoHiem (maBenhNhan, soBaoHiem, ngayBatDau, ngayHetHan, mucHuong, dungTuyen)
    VALUES (@bn2Id, 'DN4010123456789', '2024-01-01', '2027-12-31', 80, 1);

IF @bn3Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM BaoHiem WHERE maBenhNhan = @bn3Id)
    INSERT INTO BaoHiem (maBenhNhan, soBaoHiem, ngayBatDau, ngayHetHan, mucHuong, dungTuyen)
    VALUES (@bn3Id, 'CC1010987654321', '2024-01-01', '2027-12-31', 100, 1);

IF @bn4Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM BaoHiem WHERE maBenhNhan = @bn4Id)
    INSERT INTO BaoHiem (maBenhNhan, soBaoHiem, ngayBatDau, ngayHetHan, mucHuong, dungTuyen)
    VALUES (@bn4Id, 'TE1010112233445', '2024-01-01', '2027-12-31', 100, 1);

-- 7. LƯỢT KHÁM (LuotKham) BAO PHỦ TOÀN BỘ CÁC TRẠNG THÁI QUY TRÌNH HÔM NAY
DECLARE @pk101Id INT = (SELECT TOP 1 id FROM PhongKham WHERE tenPhong LIKE N'%Nội%' OR id = 1 ORDER BY id);
DECLARE @pk102Id INT = (SELECT TOP 1 id FROM PhongKham WHERE tenPhong LIKE N'%Nhi%' OR id = 2 ORDER BY id);
DECLARE @bs1Id INT = (SELECT TOP 1 id FROM NhanVien WHERE username = 'doctor');

-- LK 1: Bệnh nhân An - Chờ khám (CHO_KHAM) tại PK-101 (Test màn hình Tiếp đón & Hàng đợi bác sĩ)
IF @bn1Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM LuotKham WHERE maBenhNhan = @bn1Id AND ngayKham = CAST(GETDATE() AS DATE))
    INSERT INTO LuotKham (maBenhNhan, maCoSo, maKhoa, maPhongKham, hinhThucDieuTri, trangThai, soThuTu, soThuTuDisplay, ngayKham, thoiGianTiepNhan, lyDoKham, doiTuongKham, isDeleted)
    VALUES (@bn1Id, @cs1Id, @khoaKhamId, @pk101Id, 'NGOAI_TRU', 'CHO_KHAM', 1, '01', CAST(GETDATE() AS DATE), GETDATE(), N'Đau đầu, chóng mặt và sốt nhẹ', 'THUONG', 0);

-- LK 2: Bệnh nhi Trang - Chờ khám tại PK-102 (Nhi khoa)
IF @bn4Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM LuotKham WHERE maBenhNhan = @bn4Id AND ngayKham = CAST(GETDATE() AS DATE))
    INSERT INTO LuotKham (maBenhNhan, maCoSo, maKhoa, maPhongKham, hinhThucDieuTri, trangThai, soThuTu, soThuTuDisplay, ngayKham, thoiGianTiepNhan, lyDoKham, doiTuongKham, isDeleted)
    VALUES (@bn4Id, @cs1Id, @khoaKhamId, @pk102Id, 'NGOAI_TRU', 'CHO_KHAM', 2, '02', CAST(GETDATE() AS DATE), GETDATE(), N'Sốt cao 38.5 độ kèm ho đờm', 'BHYT', 0);

-- LK 3: Bệnh nhân Bình - Đang khám (DANG_KHAM), có đầy đủ sinh hiệu
IF @bn2Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM LuotKham WHERE maBenhNhan = @bn2Id AND ngayKham = CAST(GETDATE() AS DATE))
    INSERT INTO LuotKham (maBenhNhan, maCoSo, maKhoa, maPhongKham, maBacSi, hinhThucDieuTri, trangThai, soThuTu, soThuTuDisplay, ngayKham, thoiGianTiepNhan, thoiGianBatDauKham, lyDoKham, doiTuongKham, isDeleted)
    VALUES (@bn2Id, @cs1Id, @khoaKhamId, @pk101Id, @bs1Id, 'NGOAI_TRU', 'DANG_KHAM', 3, '03', CAST(GETDATE() AS DATE), GETDATE(), GETDATE(), N'Đau âm ỉ vùng thượng vị sau ăn', 'BHYT', 0);

DECLARE @lk3Id INT = (SELECT id FROM LuotKham WHERE maBenhNhan = @bn2Id AND ngayKham = CAST(GETDATE() AS DATE));
IF @lk3Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM SinhHieu WHERE maLuotKham = @lk3Id)
    INSERT INTO SinhHieu (maLuotKham, mach, nhietDo, huyetApTamThu, huyetApTamTruong, nhipTho, canNang, chieuCao, bmi, spo2)
    VALUES (@lk3Id, 78, 36.8, 120, 80, 18, 56.5, 160.0, 22.07, 98.5);

-- LK 4: Bệnh nhân Long - Chờ kết quả cận lâm sàng (CHO_KET_QUA_CLS), đã có chỉ định dịch vụ
IF @bn3Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM LuotKham WHERE maBenhNhan = @bn3Id AND ngayKham = CAST(GETDATE() AS DATE))
    INSERT INTO LuotKham (maBenhNhan, maCoSo, maKhoa, maPhongKham, maBacSi, hinhThucDieuTri, trangThai, soThuTu, soThuTuDisplay, ngayKham, thoiGianTiepNhan, thoiGianBatDauKham, lyDoKham, doiTuongKham, isDeleted)
    VALUES (@bn3Id, @cs1Id, @khoaKhamId, @pk101Id, @bs1Id, 'NGOAI_TRU', 'CHO_KET_QUA_CLS', 4, '04', CAST(GETDATE() AS DATE), GETDATE(), GETDATE(), N'Sốt kéo dài 3 ngày, mệt mỏi toàn thân', 'BHYT', 0);

DECLARE @lk4Id INT = (SELECT id FROM LuotKham WHERE maBenhNhan = @bn3Id AND ngayKham = CAST(GETDATE() AS DATE));
DECLARE @dvXn18Id INT = (SELECT id FROM DanhMucDichVu WHERE maDichVu = 'XN_HUYET_HOC_18');
DECLARE @dvXqId INT = (SELECT id FROM DanhMucDichVu WHERE maDichVu = 'CDHA_XQ_NGUC');

-- Chỉ định xét nghiệm huyết học (Đang chờ nhập kết quả -> Test module Cận Lâm Sàng)
IF @lk4Id IS NOT NULL AND @dvXn18Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM ChiDinhDichVu WHERE maLuotKham = @lk4Id AND maDichVu = @dvXn18Id)
    INSERT INTO ChiDinhDichVu (maLuotKham, maDichVu, maBacSiChiDinh, soLuong, donGia, trangThai, isDeleted)
    VALUES (@lk4Id, @dvXn18Id, @bs1Id, 1, 120000.0, 'DA_TIEP_NHAN', 0);

-- Chỉ định X-Quang ngực (Đã có kết quả -> Test đọc kết quả CLS)
IF @lk4Id IS NOT NULL AND @dvXqId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM ChiDinhDichVu WHERE maLuotKham = @lk4Id AND maDichVu = @dvXqId)
    INSERT INTO ChiDinhDichVu (maLuotKham, maDichVu, maBacSiChiDinh, soLuong, donGia, ketLuan, trangThai, isDeleted)
    VALUES (@lk4Id, @dvXqId, @bs1Id, 1, 180000.0, N'Hình ảnh nhu mô phổi hai bên sáng đều, rốn phổi bình thường, bóng tim không to.', 'DA_CO_KET_QUA', 0);

-- LK 5: Bệnh nhân Đức - Đã có kết quả CLS và Đơn thuốc chờ cấp phát tại Nhà thuốc (Test Cấp Phát FEFO)
IF @bn5Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM LuotKham WHERE maBenhNhan = @bn5Id AND ngayKham = CAST(GETDATE() AS DATE))
    INSERT INTO LuotKham (maBenhNhan, maCoSo, maKhoa, maPhongKham, maBacSi, hinhThucDieuTri, trangThai, soThuTu, soThuTuDisplay, ngayKham, thoiGianTiepNhan, thoiGianBatDauKham, lyDoKham, doiTuongKham, isDeleted)
    VALUES (@bn5Id, @cs1Id, @khoaKhamId, @pk101Id, @bs1Id, 'NGOAI_TRU', 'CHO_CAP_THUOC', 5, '05', CAST(GETDATE() AS DATE), GETDATE(), GETDATE(), N'Kiểm tra định kỳ tăng huyết áp & đái tháo đường', 'THUONG', 0);

DECLARE @lk5Id INT = (SELECT id FROM LuotKham WHERE maBenhNhan = @bn5Id AND ngayKham = CAST(GETDATE() AS DATE));
IF @lk5Id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM DonThuoc WHERE maLuotKham = @lk5Id)
BEGIN
    INSERT INTO DonThuoc (maLuotKham, maBacSi, maCoSo, maKhoXuat, ngayKe, trangThai, ghiChu, loiDanDonThuoc)
    VALUES (@lk5Id, @bs1Id, @cs1Id, @khoDuocRecordId, GETDATE(), 'CHO_CAP_PHAT', N'I10 - Tăng huyết áp vô căn; E11 - Đái tháo đường typ 2', N'Uống thuốc đúng giờ sau ăn sáng, hạn chế ăn mặn');
    
    DECLARE @dt5Id INT = SCOPE_IDENTITY();
    IF @idAmlo IS NOT NULL
        INSERT INTO ChiTietDonThuoc (maDonThuoc, maItem, soLuongKe, soLuongDaXuat, lieuDung, tanSuat, soNgay, huongDan, donGiaKe, sang, trua, chieu, toi)
        VALUES (@dt5Id, @idAmlo, 30.0, 0, N'1 viên', N'1 lần/ngày', 30, N'Uống sau ăn sáng', 2800.0, 1.0, 0, 0, 0);
    IF @idMet IS NOT NULL
        INSERT INTO ChiTietDonThuoc (maDonThuoc, maItem, soLuongKe, soLuongDaXuat, lieuDung, tanSuat, soNgay, huongDan, donGiaKe, sang, trua, chieu, toi)
        VALUES (@dt5Id, @idMet, 60.0, 0, N'1 viên', N'2 lần/ngày', 30, N'Uống sau ăn sáng và tối', 3200.0, 1.0, 0, 1.0, 0);
END;

-- 8. THAM SỐ HỆ THỐNG ĐẦY ĐỦ (ThamSoHeThong)
IF NOT EXISTS (SELECT 1 FROM ThamSoHeThong WHERE maThamSo = 'CLINIC_NAME')
    INSERT INTO ThamSoHeThong (maThamSo, tenThamSo, giaTri, moTa)
    VALUES ('CLINIC_NAME', N'Tên cơ sở y tế', N'BỆNH VIỆN ĐA KHOA QUỐC TẾ EHC', N'Tiêu đề phần mềm và bản in phiếu');

IF NOT EXISTS (SELECT 1 FROM ThamSoHeThong WHERE maThamSo = 'HOTLINE')
    INSERT INTO ThamSoHeThong (maThamSo, tenThamSo, giaTri, moTa)
    VALUES ('HOTLINE', N'Đường dây nóng bệnh viện', '1900 6868', N'Tổng đài cấp cứu và tư vấn sức khỏe');

IF NOT EXISTS (SELECT 1 FROM ThamSoHeThong WHERE maThamSo = 'PRINT_HEADER_SUB')
    INSERT INTO ThamSoHeThong (maThamSo, tenThamSo, giaTri, moTa)
    VALUES ('PRINT_HEADER_SUB', N'Tiêu đề phụ bản in', N'Khoa Khám Bệnh Ngoại Trú - Bàn Khám Bác Sĩ', N'In dưới tên bệnh viện');

IF NOT EXISTS (SELECT 1 FROM ThamSoHeThong WHERE maThamSo = 'SESSION_IDLE_TIMEOUT_MINUTES')
    INSERT INTO ThamSoHeThong (maThamSo, tenThamSo, giaTri, moTa)
    VALUES ('SESSION_IDLE_TIMEOUT_MINUTES', N'Thời gian tự động khóa nhàn rỗi (phút)', '15', N'Tự động khóa màn hình sau thời gian không hoạt động');

IF NOT EXISTS (SELECT 1 FROM ThamSoHeThong WHERE maThamSo = 'SESSION_ABSOLUTE_TIMEOUT_HOURS')
    INSERT INTO ThamSoHeThong (maThamSo, tenThamSo, giaTri, moTa)
    VALUES ('SESSION_ABSOLUTE_TIMEOUT_HOURS', N'Thời hạn tối đa của phiên làm việc (giờ)', '12', N'Phiên đăng nhập bắt buộc đăng xuất sau số giờ này');

-- 9. CẤP TOÀN QUYỀN SUPER-ADMIN CHO VAI TRÒ ADMIN TRÊN MA TRẬN PHÂN QUYỀN (MaTranPhanQuyen)
DECLARE @adminRoleRowId INT = (SELECT id FROM VaiTro WHERE tenVaiTro = 'ADMIN');
IF @adminRoleRowId IS NOT NULL
BEGIN
    INSERT INTO MaTranPhanQuyen (maVaiTro, phanHe, hanhDong, phamViDuLieu, duocPhep)
    SELECT @adminRoleRowId, p.phanHe, p.hanhDong, 'KHOA_PHONG', 1
    FROM (
        VALUES 
            ('RECEPTION', 'XEM'), ('RECEPTION', 'TAO'), ('RECEPTION', 'SUA'),
            ('PATIENT', 'XEM'), ('PATIENT', 'TAO'), ('PATIENT', 'SUA'),
            ('APPOINTMENT', 'XEM'), ('APPOINTMENT', 'TAO'), ('APPOINTMENT', 'SUA'),
            ('EMR', 'XEM'), ('EMR', 'TAO'), ('EMR', 'SUA'), ('EMR', 'DUYET'),
            ('CLS', 'XEM'), ('CLS', 'TAO'), ('CLS', 'SUA'),
            ('PRESCRIPTION', 'XEM'), ('PRESCRIPTION', 'TAO'), ('PRESCRIPTION', 'SUA'),
            ('INVENTORY', 'XEM'), ('INVENTORY', 'TAO'), ('INVENTORY', 'SUA'),
            ('BILLING', 'XEM'), ('BILLING', 'TAO'), ('BILLING', 'SUA')
    ) AS p(phanHe, hanhDong)
    WHERE NOT EXISTS (
        SELECT 1 FROM MaTranPhanQuyen m
        WHERE m.maVaiTro = @adminRoleRowId
          AND m.phanHe = p.phanHe
          AND m.hanhDong = p.hanhDong
          AND m.isDeleted = 0
    );
END;

COMMIT TRANSACTION;
GO
PRINT '=== SEED TOÀN BỘ DỮ LIỆU TEST CHO clinicDb_test THÀNH CÔNG RỰC RỠ! ===';
