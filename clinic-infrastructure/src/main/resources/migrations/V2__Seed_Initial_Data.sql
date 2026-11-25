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
    (8, 4, N'Phòng Xét Nghiệm Huyết Học', N'Tầng 2 - Khu C', 1),
    (9, 1, N'PK Nội tổng quát', N'Tầng 1 - Khu A', 1),
    (10, 1, N'PK Ngoại khoa', N'Tầng 1 - Khu A', 1),
    (11, 1, N'PK Chuyên khoa Mắt', N'Tầng 2 - Khu A', 1),
    (12, 1, N'PK Răng Hàm Mặt', N'Tầng 2 - Khu A', 1),
    (13, 1, N'PK Phụ sản', N'Tầng 1 - Khu B', 1),
    (14, 1, N'PK Tai Mũi Họng', N'Tầng 2 - Khu A', 1),
    (15, 1, N'PK Dinh dưỡng', N'Tầng 1 - Khu Nhi', 1),
    (16, 1, N'PK Hội chẩn chuyên khoa Nhi', N'Tầng 2 - Khu Nhi', 1),
    (17, 1, N'PK theo yêu cầu tuyến tỉnh', N'Tầng 2 - Khu Dịch vụ YC', 1),
    (18, 1, N'PK theo yêu cầu tuyến TW', N'Tầng 2 - Khu Dịch vụ YC', 1),
    (19, 1, N'PK ĐT ngoại trú tâm thần', N'Tầng 1 - Khu Chuyên khoa', 1);
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

INSERT INTO ICD (maICD, tenBenh, moTa, hoatDong)
SELECT v.maICD, v.tenBenh, v.moTa, 1
FROM (VALUES
    ('A02.0', N'Viêm ruột do Salmonella', N'Nhiễm khuẩn đường ruột Salmonella'),
    ('A08', N'Nhiễm virus và các bệnh nhiễm trùng đường ruột khác', N'Tiêu chảy do virus'),
    ('B01', N'Bệnh thủy đậu [Varicella]', N'Thủy đậu'),
    ('B05', N'Bệnh sởi [Measles]', N'Sởi'),
    ('H10', N'Viêm kết mạc', N'Đau mắt đỏ'),
    ('H20', N'Viêm mống mắt thể mi (Viêm màng bồ đào)', N'Viêm màng bồ đào'),
    ('H66', N'Viêm tai giữa có mủ và không xác định', N'Viêm tai giữa'),
    ('J03', N'Viêm amiđan cấp', N'Viêm amiđan'),
    ('J06.9', N'Nhiễm khuẩn đường hô hấp trên cấp, không xác định', N'Viêm đường hô hấp trên'),
    ('J18', N'Viêm phổi, tác nhân không xác định', N'Viêm phổi'),
    ('K52.9', N'Viêm dạ dày-ruột và viêm đại tràng không nhiễm khuẩn', N'Rối loạn tiêu hóa'),
    ('L20', N'Viêm da cơ địa', N'Chàm / Viêm da cơ địa'),
    ('N39.0', N'Nhiễm khuẩn đường tiết niệu, vị trí không xác định', N'Nhiễm trùng tiểu'),
    ('R05', N'Ho', N'Triệu chứng ho'),
    ('R50.9', N'Sốt, không xác định', N'Sốt chưa rõ nguyên nhân')
) AS v(maICD, tenBenh, moTa)
WHERE NOT EXISTS (SELECT 1 FROM ICD i WHERE i.maICD = v.maICD AND i.isDeleted = 0);

-- 7. DANH MỤC DỊCH VỤ KỸ THUẬT (DanhMucDichVu)
IF COL_LENGTH('DanhMucDichVu', 'nhomDichVu') IS NULL
    ALTER TABLE DanhMucDichVu ADD nhomDichVu NVARCHAR(100) NULL;

IF NOT EXISTS (SELECT 1 FROM DanhMucDichVu WHERE maDichVu = 'DV_KHAM_NHI')
BEGIN
    INSERT INTO DanhMucDichVu (maDichVu, tenDichVu, loaiDichVu, nhomDichVu, donGia, giaBHYT, hoatDong, moTa) VALUES 
    ('DV_KHAM_NHI', N'Khám Nhi chuyên khoa', 'KHAM_BENH', N'Khám bệnh', 120000, 39000, 1, N'Khám chuyên khoa nhi'),
    ('DV_KHAM_NOI', N'Khám Nội tổng quát', 'KHAM_BENH', N'Khám bệnh', 100000, 39000, 1, N'Khám tổng quát nội khoa'),
    ('DV_KHAM_NGOAI', N'Khám Ngoại khoa', 'KHAM_BENH', N'Khám bệnh', 100000, 39000, 1, N'Khám ngoại khoa'),
    ('DV_KHAM_MAT', N'Khám Mắt', 'KHAM_BENH', N'Khám bệnh', 100000, 39000, 1, N'Khám chuyên khoa mắt'),
    ('DV_KHAM_RANG', N'Khám Răng', 'KHAM_BENH', N'Khám bệnh', 100000, 39000, 1, N'Khám răng'),
    ('DV_KHAM_PHUSAN', N'Khám Phụ sản', 'KHAM_BENH', N'Khám bệnh', 120000, 39000, 1, N'Khám chuyên khoa phụ sản'),
    ('DV_KHAM_TMH', N'Khám Tai mũi họng', 'KHAM_BENH', N'Khám bệnh', 100000, 39000, 1, N'Khám chuyên khoa tai mũi họng'),
    ('DV_KHAM_RHM', N'Khám Răng hàm mặt', 'KHAM_BENH', N'Khám bệnh', 100000, 39000, 1, N'Khám chuyên khoa RHM'),
    ('DV_KHAM_ODONG', N'Khám Ổ đồng', 'KHAM_BENH', N'Khám bệnh', 100000, 39000, 1, N'Khám ổ đồng'),
    ('DV_HC_NHI', N'Hội chẩn ca bệnh khó chuyên ngành Nhi', 'KHAM_BENH', N'Khám bệnh', 250000, 39000, 1, N'Hội chẩn nhi'),
    ('DV_KHAM_YC_TINH', N'Khám bệnh theo yêu cầu mời bác sỹ tuyến tỉnh [YC]', 'KHAM_BENH', N'Khám bệnh', 300000, 39000, 1, N'Khám YC tỉnh'),
    ('DV_KHAM_YC_TW', N'Khám bệnh theo yêu cầu mời BS tuyến TW [YC]', 'KHAM_BENH', N'Khám bệnh', 500000, 39000, 1, N'Khám YC TW'),
    ('DV_KHAM_TAMTHAN', N'Khám tâm thần', 'KHAM_BENH', N'Khám bệnh', 100000, 39000, 1, N'Khám chuyên khoa tâm thần'),
    ('XN_CTM', N'Tổng phân tích tế bào máu ngoại vi (bằng máy đếm tự động 18 thông số)', 'XET_NGHIEM', N'Huyết học', 85000, 42000, 1, N'Xét nghiệm CTM'),
    ('XN_GLUCOSE', N'Định lượng Glucose máu', 'XET_NGHIEM', N'Hóa sinh', 45000, 25000, 1, N'Đường huyết'),
    ('CDHA_XQ_TIMPHOI', N'Chụp X-quang ngực thẳng (1 phim)', 'CDHA', N'X-Quang', 120000, 65000, 1, N'Chụp phổi thẳng'),
    ('CDHA_SIEUAM_BUNG', N'Siêu âm ổ bụng tổng quát (màu)', 'CDHA', N'Siêu âm', 150000, 85000, 1, N'Siêu âm tổng quát'),
    ('XN_HS_URE_CRE', N'Định lượng Ure máu [Máu]', 'XET_NGHIEM', N'Hóa sinh', 40000, 21500, 1, N'Chức năng thận'),
    ('XN_HS_CREATININ', N'Định lượng Creatinin máu [Máu]', 'XET_NGHIEM', N'Hóa sinh', 40000, 21500, 1, N'Chức năng thận'),
    ('XN_HS_AST_GOT', N'Đo hoạt độ AST (GOT) [Máu]', 'XET_NGHIEM', N'Hóa sinh', 45000, 21500, 1, N'Men gan'),
    ('XN_HS_ALT_GPT', N'Đo hoạt độ ALT (GPT) [Máu]', 'XET_NGHIEM', N'Hóa sinh', 45000, 21500, 1, N'Men gan'),
    ('XN_HS_CHOLESTEROL', N'Định lượng Cholesterol toàn phần', 'XET_NGHIEM', N'Hóa sinh', 50000, 26000, 1, N'Mỡ máu'),
    ('XN_HS_TRIGLYCERID', N'Định lượng Triglycerid máu', 'XET_NGHIEM', N'Hóa sinh', 50000, 26000, 1, N'Mỡ máu'),
    ('XN_HS_DIENGIAI', N'Điện giải đồ (Na, K, Cl) [Máu]', 'XET_NGHIEM', N'Hóa sinh', 65000, 35000, 1, N'Điện giải đồ'),
    ('XN_HH_NHOMMAU', N'Định nhóm máu hệ ABO, Rh(D)', 'XET_NGHIEM', N'Huyết học', 75000, 40000, 1, N'Nhóm máu'),
    ('XN_HH_MAULANG', N'Tốc độ máu lắng (VS)', 'XET_NGHIEM', N'Huyết học', 35000, 18000, 1, N'Huyết học'),
    ('XN_DM_PT_APTT', N'Đông máu cơ bản (PT, APTT, Fibrinogen)', 'XET_NGHIEM', N'Đông máu', 135000, 72000, 1, N'Đông máu'),
    ('XN_DM_DDIMER', N'Định lượng D-Dimer máu', 'XET_NGHIEM', N'Đông máu', 260000, 150000, 1, N'Đông máu chuyên sâu'),
    ('XN_NT_TONGPHANTICH', N'Tổng phân tích nước tiểu (10 thông số)', 'XET_NGHIEM', N'Nước tiểu', 45000, 27400, 1, N'Nước tiểu'),
    ('XN_TB_LIQUIPREP', N'Xét nghiệm tế bào học bằng phương pháp Liqui Prep', 'XET_NGHIEM', N'Tế bào', 450000, 0, 1, N'Tế bào học'),
    ('XN_GP_SINHTHIET', N'Xét nghiệm mô bệnh học mẫu sinh thiết nhỏ', 'XET_NGHIEM', N'Giải phẫu', 520000, 310000, 1, N'Giải phẫu bệnh'),
    ('XN_VS_SOITUOIPHAN', N'Soi tươi tìm vi khuẩn / ký sinh trùng', 'XET_NGHIEM', N'Vi sinh', 60000, 32000, 1, N'Vi sinh cơ bản'),
    ('XN_VS_CAYMAU', N'Nuôi cấy vi khuẩn định danh và kháng sinh đồ', 'XET_NGHIEM', N'Vi sinh', 320000, 195000, 1, N'Vi sinh nuôi cấy'),
    ('XN_MD_CRP', N'Định lượng CRP (hs-CRP) [Máu]', 'XET_NGHIEM', N'Miễn dịch', 95000, 52000, 1, N'Miễn dịch - Viêm'),
    ('XN_MD_AMH', N'Định lượng AMH [YC]', 'XET_NGHIEM', N'Miễn dịch', 850000, 0, 1, N'Miễn dịch nội tiết'),
    ('XN_MD_HBSAG', N'HBsAg test nhanh / miễn dịch', 'XET_NGHIEM', N'Miễn dịch', 90000, 48000, 1, N'Viêm gan B'),
    ('CDHA_SA_TIM', N'Siêu âm Doppler tim, van tim', 'CDHA', N'Siêu âm', 320000, 222000, 1, N'Siêu âm tim'),
    ('CDHA_DIENTIM', N'Điện tâm đồ thường (ECG 12 chuyển đạo)', 'CDHA', N'Thăm dò chức năng', 65000, 41200, 1, N'Thăm dò chức năng tim'),
    ('CDHA_NOISOI_TMH', N'Nội soi Tai - Mũi - Họng ống cứng/mềm', 'CDHA', N'Thăm dò chức năng', 220000, 110000, 1, N'Nội soi TMH');
END;

-- 8. KHO DƯỢC & THUỐC MẪU (Kho, Item, TonKho)
IF NOT EXISTS (SELECT 1 FROM Kho WHERE id = 1)
BEGIN
    SET IDENTITY_INSERT Kho ON;
    INSERT INTO Kho (id, maCoSo, tenKho, loaiKho, trangThai, maNguoiPhuTrach) VALUES 
    (1, 1, N'Kho Thuốc Ngoại Trú', 'KHO_LE', 'HOAT_DONG', 1),
    (2, 1, N'Kho Tổng Chẵn', 'KHO_TONG', 'HOAT_DONG', 1),
    (3, 1, N'Tủ Trực Cấp Cứu Nhi', 'TU_TRUC_CAP_CUU', 'HOAT_DONG', 2),
    (4, 1, N'Kho Vật Tư Y Tế', 'VAT_TU', 'HOAT_DONG', 1);
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

IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'THUOC004')
BEGIN
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong)
    VALUES
    ('THUOC004', N'Attapulgite + Aluminium hydroxyd (Gastropulgite) 2,5g + 0,5g', 'THUOC', N'Attapulgite', '2.5g+0.5g', N'Gói', N'Hộp 30 gói', 2500, 3357, 30, 5000, N'Bột pha hỗn dịch', N'Pháp', N'Ipsen', 'VN-20101-19', 1),
    ('THUOC005', N'Amoxicillin + Acid Clavulanic (Augmentin 250/31.25)', 'THUOC', N'Amoxicillin + Clavulanic', '250mg/31.25mg', N'Gói', N'Hộp 12 gói', 9000, 11416, 20, 3000, N'Bột pha hỗn dịch', N'Anh', N'GSK', 'VN-20202-20', 1),
    ('THUOC006', N'Amoxicillin + Acid Clavulanic (Vigentin 500/62.5 DT)', 'THUOC', N'Amoxicillin + Clavulanic', '500mg/62.5mg', N'Viên', N'Hộp 12 gói', 6500, 8774, 30, 5000, N'Viên phân tán', N'Việt Nam', N'Vĩnh Phúc', 'VD-20303-21', 1),
    ('THUOC007', N'Azithromycin (Zitromax) 200mg/5ml', 'THUOC', N'Azithromycin', '200mg/5ml', N'Lọ', N'Hộp 1 lọ 15ml', 95000, 120000, 10, 1000, N'Bột pha hỗn dịch', N'Ý', N'Pfizer', 'VN-20404-20', 1),
    ('THUOC008', N'Acetylcysteine (Exomuc) 200mg', 'THUOC', N'Acetylcysteine', '200mg', N'Gói', N'Hộp 30 gói', 3000, 4150, 30, 5000, N'Cốm pha uống', N'Pháp', N'Bouchara', 'VN-20505-19', 1),
    ('THUOC009', N'Ambroxol (Halixol) 15mg/5ml', 'THUOC', N'Ambroxol', '15mg/5ml', N'Lọ', N'Hộp 1 lọ 100ml', 55000, 72760, 10, 1000, N'Siro uống', N'Hungary', N'Egis', 'VN-20606-20', 1),
    ('THUOC010', N'Esomeprazole (Nexium) 10mg', 'THUOC', N'Esomeprazole', '10mg', N'Gói', N'Hộp 28 gói', 18000, 23000, 20, 3000, N'Cốm kháng dịch dạ dày', N'Thụy Điển', N'AstraZeneca', 'VN-20707-21', 1),
    ('THUOC011', N'Domperidone (Motilium-M) 10mg', 'THUOC', N'Domperidone', '10mg', N'Viên', N'Hộp 10 vỉ x 10 viên', 1800, 2500, 50, 5000, N'Viên nén bao phim', N'Thái Lan', N'Janssen', 'VN-20808-19', 1),
    ('THUOC012', N'Natri clorid + Natri citrat + Kali clorid + Glucose (Oresol 245)', 'THUOC', N'ORS', '245 mOsm/L', N'Gói', N'Hộp 20 gói', 1800, 2750, 50, 8000, N'Bột pha uống', N'Việt Nam', N'Dược Hậu Giang', 'VD-20909-20', 1),
    ('THUOC013', N'Nước biển phun sương đẳng trương (Sterimar) 100ml', 'THUOC', N'Nước biển đẳng trương', '100ml', N'Chai', N'Chai 100ml', 75000, 95000, 10, 1000, N'Dung dịch xịt mũi', N'Pháp', N'Sofibel', 'VN-21010-21', 1),
    ('THUOC014', N'Sắt (III) hydroxyd polymaltose (Fogyma) 50mg/10ml', 'THUOC', N'Sắt III', '50mg/10ml', N'Ống', N'Hộp 20 ống', 6500, 8500, 20, 3000, N'Dung dịch uống', N'Việt Nam', N'CPC1 Hà Nội', 'VD-21111-22', 1),
    ('THUOC015', N'Albendazole (Zentel Tab) 200mg', 'THUOC', N'Albendazole', '200mg', N'Viên', N'Hộp 1 vỉ x 2 viên', 4200, 5992, 20, 2000, N'Viên nén', N'Pháp', N'GSK', 'VN-21212-20', 1);

    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan)
    SELECT 1, i.id, 'LO-' + i.maItem, DATEADD(MONTH, 24, CAST(GETDATE() AS DATE)), 1000, i.giaNhapMacDinh, i.giaBanMacDinh
    FROM Item i
    WHERE i.maItem IN ('THUOC004','THUOC005','THUOC006','THUOC007','THUOC008','THUOC009','THUOC010','THUOC011','THUOC012','THUOC013','THUOC014','THUOC015')
      AND NOT EXISTS (SELECT 1 FROM TonKho tk WHERE tk.maItem = i.id AND tk.isDeleted = 0);
END;

IF NOT EXISTS (SELECT 1 FROM Kho WHERE id = 4)
BEGIN
    SET IDENTITY_INSERT Kho ON;
    INSERT INTO Kho (id, maCoSo, tenKho, loaiKho, trangThai, maNguoiPhuTrach)
    VALUES (4, 1, N'Kho Vật Tư Y Tế', 'VAT_TU', 'HOAT_DONG', 1);
    SET IDENTITY_INSERT Kho OFF;
END;

IF NOT EXISTS (SELECT 1 FROM Item WHERE maItem = 'VT001')
BEGIN
    INSERT INTO Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong)
    VALUES
    ('VT001', N'Bơm tiêm sử dụng một lần 5ml (Vinahankook)', 'VAT_TU', N'Vật tư tiêu hao', '5ml', N'Cái', N'Hộp 100 cái', 550, 735, 100, 20000, N'Vô trùng', N'Việt Nam', N'Vinahankook', 'TTBYT-001', 1),
    ('VT002', N'Bơm tiêm sử dụng một lần 10ml', 'VAT_TU', N'Vật tư tiêu hao', '10ml', N'Cái', N'Hộp 100 cái', 900, 1200, 100, 20000, N'Vô trùng', N'Việt Nam', N'Vinahankook', 'TTBYT-002', 1),
    ('VT003', N'Bơm cho ăn 50ml đầu catheter (Nhi/Sơ sinh)', 'VAT_TU', N'Vật tư tiêu hao', '50ml', N'Cái', N'Hộp 25 cái', 3200, 4500, 20, 5000, N'Vô trùng', N'Hàn Quốc', N'Becton Dickinson', 'TTBYT-003', 1),
    ('VT004', N'Kim luồn tĩnh mạch ngoại biên 24G (Nhi)', 'VAT_TU', N'Vật tư tiêu hao', '24G', N'Cái', N'Hộp 50 cái', 9000, 12000, 50, 5000, N'Vô trùng', N'Ấn Độ', N'Polymed', 'TTBYT-004', 1),
    ('VT005', N'Dây truyền dịch vô trùng (có bầu đếm giọt)', 'VAT_TU', N'Vật tư tiêu hao', '150cm', N'Bộ', N'Túi 1 bộ', 4500, 6500, 50, 10000, N'Vô trùng', N'Việt Nam', N'Mekophar', 'TTBYT-005', 1),
    ('VT006', N'Gạc vô khuẩn 10x10cm (8 lớp)', 'VAT_TU', N'Bông băng gạc', '10x10cm', N'Gói', N'Gói 10 miếng', 1800, 2500, 100, 10000, N'Vô trùng', N'Việt Nam', N'Đông Pha', 'TTBYT-006', 1),
    ('VT007', N'Băng keo lụa y tế Urgopore 2.5cm x 5m', 'VAT_TU', N'Bông băng gạc', '2.5cm x 5m', N'Cuộn', N'Hộp 12 cuộn', 13000, 18000, 20, 2000, N'Cuộn', N'Pháp', N'Urgo', 'TTBYT-007', 1),
    ('VT008', N'Găng tay khám y tế không bột (Size M)', 'VAT_TU', N'Bảo hộ y tế', 'Size M', N'Đôi', N'Hộp 50 đôi', 2400, 3500, 100, 10000, N'Nitrile', N'Malaysia', N'Top Glove', 'TTBYT-008', 1),
    ('VT009', N'Que lấy mẫu dịch tỵ hầu vô trùng', 'VAT_TU', N'Vật tư xét nghiệm', '15cm', N'Que', N'Túi 1 que', 3500, 5000, 50, 5000, N'Vô trùng', N'Trung Quốc', N'Improve', 'TTBYT-009', 1),
    ('VT010', N'Ống nghiệm lấy máu EDTA K2 2ml', 'VAT_TU', N'Vật tư xét nghiệm', '2ml', N'Ống', N'Khay 100 ống', 2000, 3000, 100, 10000, N'Chân không', N'Việt Nam', N'Sài Gòn Lab', 'TTBYT-010', 1),
    ('VT011', N'Băng cá nhân vô trùng Urgo Durable', 'VAT_TU', N'Bông băng gạc', '20x72mm', N'Miếng', N'Hộp 100 miếng', 1000, 1500, 100, 10000, N'Vô trùng', N'Pháp', N'Urgo', 'TTBYT-011', 1),
    ('VT012', N'Mặt nạ khí dung trẻ em kèm dây oxy', 'VAT_TU', N'Vật tư hô hấp', 'Trẻ em', N'Bộ', N'Túi 1 bộ', 20000, 28000, 20, 3000, N'Nhựa y tế', N'Đài Loan', N'Besmed', 'TTBYT-012', 1);

    INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan)
    SELECT 4, i.id, 'LO-' + i.maItem, DATEADD(MONTH, 36, CAST(GETDATE() AS DATE)), 1000, i.giaNhapMacDinh, i.giaBanMacDinh
    FROM Item i
    WHERE i.loaiItem = 'VAT_TU'
      AND NOT EXISTS (SELECT 1 FROM TonKho tk WHERE tk.maItem = i.id AND tk.isDeleted = 0);
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
