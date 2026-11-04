-- ==============================================================================
-- DATABASE MIGRATION SCRIPT: V2__Seed_Initial_Data.sql
-- HỆ THỐNG: CLINIC MANAGEMENT (EHC HIS)
-- DỮ LIỆU BAN ĐẦU CHUẨN (SEED DATA)
-- ==============================================================================

SET NOCOUNT ON;

-- 1. CƠ SỞ MẪU (CoSo)
IF NOT EXISTS (SELECT 1 FROM CoSo WHERE id = 1)
BEGIN
    SET IDENTITY_INSERT CoSo ON;
    INSERT INTO CoSo (id, tenCoSo, diaChi, dienThoai, email, loaiCoSo, hoatDong)
    VALUES (1, N'BỆNH VIỆN SẢN NHI BẮC NINH', N'Đường Huyền Quang, Phường Võ Cường, TP. Bắc Ninh', '02223821234', 'sannhibacninh@gmail.com', 'BENH_VIEN', 1);
    SET IDENTITY_INSERT CoSo OFF;
END;

-- 2. CHUYÊN KHOA (Khoa)
IF NOT EXISTS (SELECT 1 FROM Khoa WHERE id = 1)
BEGIN
    SET IDENTITY_INSERT Khoa ON;
    INSERT INTO Khoa (id, maCoSo, tenKhoa, moTa, hoatDong) VALUES 
    (1, 1, N'Khoa Khám Bệnh', N'Tiếp đón và khám chữa bệnh ngoại trú', 1),
    (2, 1, N'Khoa Nhi', N'Khám và điều trị chuyên khoa nhi', 1),
    (3, 1, N'Khoa Phụ Sản', N'Khám và chăm sóc sức khỏe sản phụ', 1),
    (4, 1, N'Khoa Cận Lâm Sàng & CĐHA', N'Thực hiện xét nghiệm, siêu âm, X-quang', 1),
    (5, 1, N'Khoa Dược', N'Quản lý và cấp phát thuốc, vật tư y tế', 1);
    SET IDENTITY_INSERT Khoa OFF;
END;

-- 3. PHÒNG KHÁM CỤ THỂ (PhongKham)
IF NOT EXISTS (SELECT 1 FROM PhongKham WHERE id = 1)
BEGIN
    SET IDENTITY_INSERT PhongKham ON;
    INSERT INTO PhongKham (id, maKhoa, tenPhong, viTri, hoatDong) VALUES 
    (1, 1, N'PK Nhi 1', N'Tầng 1 - Khu A', 1),
    (2, 1, N'PK Nhi 2', N'Tầng 1 - Khu A', 1),
    (3, 1, N'Phòng Tiêm Chủng', N'Tầng 1 - Khu B', 1),
    (4, 1, N'Cấp cứu nhi', N'Tầng trệt - Cửa số 1', 1),
    (5, 1, N'PK theo yêu cầu Sản - Phụ khoa', N'Tầng 2 - Khu B', 1),
    (6, 4, N'Phòng Siêu âm 01', N'Tầng 1 - Khu C', 1),
    (7, 4, N'Phòng X-Quang Kỹ Thuật Số', N'Tầng 1 - Khu C', 1),
    (8, 4, N'Phòng Xét Nghiệm Huyết Học', N'Tầng 2 - Khu C', 1);
    SET IDENTITY_INSERT PhongKham OFF;
END;

-- 4. VAI TRÒ (VaiTro) & CHỨC VỤ (ChucVu)
IF NOT EXISTS (SELECT 1 FROM VaiTro WHERE id = 1)
BEGIN
    SET IDENTITY_INSERT VaiTro ON;
    INSERT INTO VaiTro (id, tenVaiTro, moTa) VALUES 
    (1, N'ADMIN', N'Quản trị viên toàn hệ thống'),
    (2, N'DOCTOR', N'Bác sĩ khám và điều trị lâm sàng'),
    (3, N'RECEPTIONIST', N'Nhân viên tiếp đón và đăng ký khám'),
    (4, N'PHARMACIST', N'Dược sĩ kho dược và quầy thuốc'),
    (5, N'CASHIER', N'Thu ngân viện phí'),
    (6, N'LAB_TECH', N'Kỹ thuật viên cận lâm sàng');
    SET IDENTITY_INSERT VaiTro OFF;
END;

IF NOT EXISTS (SELECT 1 FROM ChucVu WHERE id = 1)
BEGIN
    SET IDENTITY_INSERT ChucVu ON;
    INSERT INTO ChucVu (id, tenChucVu, moTa) VALUES 
    (1, N'Giám đốc chuyên môn', N'Lãnh đạo bệnh viện'),
    (2, N'Trưởng khoa', N'Quản lý chuyên môn khoa phòng'),
    (3, N'Bác sĩ chuyên khoa I', N'Bác sĩ điều trị'),
    (4, N'Điều dưỡng viên', N'Chăm sóc và đo sinh hiệu'),
    (5, N'Dược sĩ đại học', N'Phụ trách kho dược');
    SET IDENTITY_INSERT ChucVu OFF;
END;

-- 5. TÀI KHOẢN MẪU (NhanVien, NhanVienVaiTro, PhanQuyenCoSo)
IF NOT EXISTS (SELECT 1 FROM NhanVien WHERE id = 1)
BEGIN
    SET IDENTITY_INSERT NhanVien ON;
    -- Tài khoản demo: admin / tiennv; mật khẩu ban đầu Password123!. Đổi mật khẩu trước khi dùng môi trường thật.
    INSERT INTO NhanVien (id, maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username, passwordHash, hoatDong, chungChiHanhNghe, ngayVaoLam, luongCoBan)
    VALUES 
    (1, 'NV0001', N'Nguyễn Quản', N'Trị', '0912345678', 'admin@ehc.vn', N'Hà Nội', 'M', '1985-01-01', 1, 'admin', 'pbkdf2_sha256$210000$1CmHHyyKRFqB70+3bqIiAQ==$HccuNSaYa4j7W001EdKDppnLGOPBpdK99ClDkwnkfWA=', 1, N'001234/BYT-CCHN', '2015-01-01', 25000000),
    (2, 'BS0001', N'Nguyễn Văn', N'Tiến', '0987654321', 'tiennv@ehc.vn', N'Bắc Ninh', 'M', '1988-05-15', 3, 'tiennv', 'pbkdf2_sha256$210000$pzwZ5lgrTRC3FUL54MMaXw==$msrLCG673s188PHhk9CWmmSzUIpoAJ2yH7Tz1CjotHA=', 1, N'005678/BN-CCHN', '2018-06-01', 18000000);
    SET IDENTITY_INSERT NhanVien OFF;

    INSERT INTO NhanVienVaiTro (maNhanVien, maVaiTro) VALUES (1, 1), (2, 2);
    INSERT INTO PhanQuyenCoSo (maNhanVien, maCoSo) VALUES (1, 1), (2, 1);
    INSERT INTO NhanVienKhoa (maNhanVien, maKhoa) VALUES (1, 1), (2, 2);
END;

-- 6. DANH MỤC ICD-10 MẪU (ICD)
IF NOT EXISTS (SELECT 1 FROM ICD WHERE maICD = 'I10')
BEGIN
    INSERT INTO ICD (maICD, tenBenh, moTa, hoatDong) VALUES 
    ('I10', N'Bệnh tăng huyết áp vô căn (nguyên phát)', N'Tăng huyết áp', 1),
    ('E11', N'Bệnh đái tháo đường không phụ thuộc insulin (typ 2)', N'Tiểu đường typ 2', 1),
    ('J00', N'Viêm mũi họng cấp [cảm thường]', N'Cảm cúm thông thường', 1),
    ('J02', N'Viêm họng cấp', N'Viêm họng cấp tính', 1),
    ('J20', N'Viêm phế quản cấp', N'Viêm phế quản', 1),
    ('K29', N'Viêm dạ dày và tá tràng', N'Đau dạ dày tá tràng', 1),
    ('A09', N'Tiêu chảy và viêm dạ dày - ruột do nhiễm khuẩn', N'Rối loạn tiêu hóa', 1);
END;

-- 7. DANH MỤC DỊCH VỤ KỸ THUẬT (DanhMucDichVu)
IF NOT EXISTS (SELECT 1 FROM DanhMucDichVu WHERE maDichVu = 'DV_KHAM_NHI')
BEGIN
    INSERT INTO DanhMucDichVu (maDichVu, tenDichVu, loaiDichVu, donGia, giaBHYT, hoatDong, moTa) VALUES 
    ('DV_KHAM_NHI', N'Khám Nhi chuyên khoa', 'KHAM_BENH', 120000, 39000, 1, N'Khám chuyên khoa nhi'),
    ('DV_KHAM_NOI', N'Khám Nội tổng quát', 'KHAM_BENH', 100000, 39000, 1, N'Khám tổng quát nội khoa'),
    ('XN_CTM', N'Tổng phân tích tế bào máu ngoại vi (bằng máy đếm tự động 18 thông số)', 'XET_NGHIEM', 85000, 42000, 1, N'Xét nghiệm CTM'),
    ('XN_GLUCOSE', N'Định lượng Glucose máu', 'XET_NGHIEM', 45000, 25000, 1, N'Đường huyết'),
    ('CDHA_XQ_TIMPHOI', N'Chụp X-quang ngực thẳng (1 phim)', 'CDHA', 120000, 65000, 1, N'Chụp phổi thẳng'),
    ('CDHA_SIEUAM_BUNG', N'Siêu âm ổ bụng tổng quát (màu)', 'CDHA', 150000, 85000, 1, N'Siêu âm tổng quát');
END;

-- 8. KHO DƯỢC & THUỐC MẪU (Kho, Item, TonKho)
IF NOT EXISTS (SELECT 1 FROM Kho WHERE id = 1)
BEGIN
    SET IDENTITY_INSERT Kho ON;
    INSERT INTO Kho (id, maCoSo, tenKho, loaiKho, trangThai, maNguoiPhuTrach) VALUES 
    (1, 1, N'Kho Thuốc Ngoại Trú', 'KHO_LE', 'HOAT_DONG', 1),
    (2, 1, N'Kho Tổng Chẵn', 'KHO_TONG', 'HOAT_DONG', 1),
    (3, 1, N'Tủ Trực Cấp Cứu Nhi', 'TU_TRUC_CAP_CUU', 'HOAT_DONG', 2);
    SET IDENTITY_INSERT Kho OFF;
END;

IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'THUOC001')
BEGIN
    SET IDENTITY_INSERT Item ON;
    INSERT INTO Item (id, maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong)
    VALUES 
    (1, 'THUOC001', N'Lactulose (Duphalac) 10g/15ml', 'THUOC', N'Lactulose', '10g/15ml', N'Gói', N'Hộp 20 gói', 5000, 7500, 50, 5000, N'Dung dịch uống', N'Hà Lan', N'Abbott', 'VN-12345-18', 1),
    (2, 'THUOC002', N'Aciclovir 5% 5g', 'THUOC', N'Aciclovir', '5%', N'Tuýp', N'Hộp 1 tuýp 5g', 18000, 25000, 20, 1000, N'Kem bôi da', N'Việt Nam', N'Dược Hậu Giang', 'VD-67890-20', 1),
    (3, 'THUOC003', N'Paracetamol 500mg', 'THUOC', N'Paracetamol', '500mg', N'Viên', N'Hộp 10 vỉ x 10 viên', 500, 1000, 100, 10000, N'Viên nén', N'Việt Nam', N'Hasan', 'VD-11111-19', 1);
    SET IDENTITY_INSERT Item OFF;

    -- Tồn kho ban đầu
    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan) VALUES 
    (1, 1, 'L0124', DATEADD(MONTH, 24, CAST(GETDATE() AS DATE)), 500, 5000, 7500),
    (1, 2, 'A0524', DATEADD(MONTH, 18, CAST(GETDATE() AS DATE)), 200, 18000, 25000),
    (1, 3, 'P0824', DATEADD(MONTH, 36, CAST(GETDATE() AS DATE)), 5000, 500, 1000);
END;

-- 9. THAM SỐ HỆ THỐNG (ThamSoHeThong)
IF NOT EXISTS (SELECT 1 FROM ThamSoHeThong WHERE maThamSo = 'CLINIC_NAME')
BEGIN
    INSERT INTO ThamSoHeThong (maThamSo, tenThamSo, giaTri, moTa) VALUES 
    ('CLINIC_NAME', N'Tên cơ sở y tế', N'BỆNH VIỆN SẢN NHI BẮC NINH', N'Hiển thị trên tiêu đề phần mềm và bản in'),
    ('HOTLINE', N'Đường dây nóng', '02223821234', N'Hotline tiếp nhận cấp cứu và tư vấn'),
    ('PRINT_HEADER_SUB', N'Tiêu đề phụ bản in', N'Khoa Khám Bệnh - Bàn Khám Bác Sĩ', N'In dưới tên bệnh viện'),
    ('AUTO_LOGOUT_MINUTES', N'Thời gian tự động khóa màn hình', '30', N'Phút');
END;
