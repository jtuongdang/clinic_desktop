-- Phân loại nhóm thuốc & BHYT cho danh mục thuốc hiện có, bổ sung thuốc đủ 11 nhóm phân loại,
-- tồn kho lô ban đầu và 2 nhà cung cấp mẫu (idempotent).

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Item_Thuoc_PhanLoai' AND object_id = OBJECT_ID('dbo.Item'))
    CREATE NONCLUSTERED INDEX IX_Item_Thuoc_PhanLoai ON dbo.Item (loaiItem, nhomThuoc, thuocBHYT) WHERE isDeleted = 0;

-- 1. Cập nhật phân loại & nhóm BHYT cho 15 thuốc có sẵn (chỉ gán khi đang NULL)
UPDATE dbo.Item SET nhomThuoc = ISNULL(nhomThuoc, N'Thuốc Tiêu Hóa'), thuocBHYT = ISNULL(thuocBHYT, 0) WHERE maItem IN ('THUOC001', 'THUOC004');
UPDATE dbo.Item SET nhomThuoc = ISNULL(nhomThuoc, N'Thuốc Tiêu Hóa'), thuocBHYT = ISNULL(thuocBHYT, 1) WHERE maItem IN ('THUOC010', 'THUOC011', 'THUOC012');
UPDATE dbo.Item SET nhomThuoc = ISNULL(nhomThuoc, N'Thuốc Kháng Sinh'), thuocBHYT = ISNULL(thuocBHYT, 1) WHERE maItem IN ('THUOC005', 'THUOC006', 'THUOC007');
UPDATE dbo.Item SET nhomThuoc = ISNULL(nhomThuoc, N'Thuốc Hô Hấp'), thuocBHYT = ISNULL(thuocBHYT, 1) WHERE maItem IN ('THUOC008', 'THUOC009');
UPDATE dbo.Item SET nhomThuoc = ISNULL(nhomThuoc, N'Thuốc Hạ Nhiệt - Giảm Đau'), thuocBHYT = ISNULL(thuocBHYT, 1) WHERE maItem = 'THUOC003';
UPDATE dbo.Item SET nhomThuoc = ISNULL(nhomThuoc, N'Thuốc Nhỏ Mũi'), thuocBHYT = ISNULL(thuocBHYT, 0) WHERE maItem = 'THUOC013';
UPDATE dbo.Item SET nhomThuoc = ISNULL(nhomThuoc, N'Thuốc khác'), thuocBHYT = ISNULL(thuocBHYT, 1) WHERE maItem IN ('THUOC002', 'THUOC015');
UPDATE dbo.Item SET nhomThuoc = ISNULL(nhomThuoc, N'Thuốc khác'), thuocBHYT = ISNULL(thuocBHYT, 0) WHERE maItem = 'THUOC014';

-- 2. Bổ sung 10 thuốc cho đầy đủ các nhóm phân loại còn trống
IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'THUOC016')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT)
    VALUES ('THUOC016', N'Salbutamol (Ventolin) 100mcg', 'THUOC', N'Salbutamol', N'100mcg/liều', N'Lọ', N'Bình xịt 200 liều', 95000, 120000, 10, 500, N'Hỗn dịch xịt', N'Tây Ban Nha', N'GSK', 'VN-19201-15', 1, N'Thuốc Hen', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'THUOC017')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT)
    VALUES ('THUOC017', N'Montelukast (Singulair) 4mg', 'THUOC', N'Montelukast', N'4mg', N'Gói', N'Hộp 28 gói', 11000, 15000, 30, 1000, N'Cốm pha uống', N'Anh', N'MSD', 'VN-20118-16', 1, N'Thuốc Hen', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'THUOC018')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT)
    VALUES ('THUOC018', N'Alpha Chymotrypsin (Alpha Choay) 4.2mg', 'THUOC', N'Alphachymotrypsin', N'4.2mg', N'Viên', N'Hộp 2 vỉ x 15 viên', 1600, 2500, 100, 5000, N'Viên nén', N'Pháp', N'Sanofi', 'VN-18442-14', 1, N'Thuốc Kháng Viêm', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'THUOC019')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT)
    VALUES ('THUOC019', N'Methylprednisolon (Medrol) 16mg', 'THUOC', N'Methylprednisolon', N'16mg', N'Viên', N'Hộp 3 vỉ x 10 viên', 3200, 4800, 60, 3000, N'Viên nén', N'Ý', N'Pfizer', 'VN-17822-14', 1, N'Thuốc Kháng Viêm', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'THUOC020')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT)
    VALUES ('THUOC020', N'Cetirizin 10mg', 'THUOC', N'Cetirizin dihydroclorid', N'10mg', N'Viên', N'Hộp 10 vỉ x 10 viên', 600, 1200, 100, 5000, N'Viên nén bao phim', N'Việt Nam', N'DHG Pharma', 'VD-25410-16', 1, N'Thuốc Dị Ứng', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'THUOC021')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT)
    VALUES ('THUOC021', N'Desloratadin (Aerius) 0.5mg/ml 60ml', 'THUOC', N'Desloratadin', N'0.5mg/ml', N'Chai', N'Chai 60ml', 62000, 82000, 15, 500, N'Siro uống', N'Bỉ', N'Organon', 'VN-21045-18', 1, N'Thuốc Dị Ứng', 0);

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'THUOC022')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT)
    VALUES ('THUOC022', N'Ofloxacin 0.3% (Nhỏ tai) 5ml', 'THUOC', N'Ofloxacin', N'0.3%', N'Lọ', N'Lọ 5ml', 28000, 42000, 20, 500, N'Dung dịch nhỏ tai', N'Việt Nam', N'Traphaco', 'VD-26812-17', 1, N'Thuốc Nhỏ Tai', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'THUOC023')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT)
    VALUES ('THUOC023', N'Xylometazolin 0.05% (Otrivin) 10ml', 'THUOC', N'Xylometazolin', N'0.05%', N'Lọ', N'Lọ 10ml', 35000, 52000, 20, 500, N'Dung dịch nhỏ mũi', N'Thụy Sĩ', N'GSK', 'VN-19881-16', 1, N'Thuốc Nhỏ Mũi', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'THUOC024')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT)
    VALUES ('THUOC024', N'Ibuprofen (Brufen) 200mg', 'THUOC', N'Ibuprofen', N'200mg', N'Viên', N'Hộp 10 vỉ x 10 viên', 1100, 2000, 100, 5000, N'Viên nén bao đường', N'Việt Nam', N'Imexpharm', 'VD-24109-16', 1, N'Thuốc Hạ Nhiệt - Giảm Đau', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'THUOC025')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT)
    VALUES ('THUOC025', N'Vitamin C 500mg', 'THUOC', N'Acid ascorbic', N'500mg', N'Viên', N'Hộp 10 vỉ x 10 viên', 500, 1000, 100, 5000, N'Viên nén', N'Việt Nam', N'DHG Pharma', 'VD-23900-15', 1, N'Thuốc viên', 0);

-- 3. Tạo lô tồn kho ban đầu tại kho hoạt động đầu tiên cho các thuốc mới nếu chưa có tồn kho
INSERT INTO dbo.TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan)
SELECT k.id, i.id, CONCAT('LO26-', i.maItem), DATEADD(MONTH, 18, CAST(GETDATE() AS DATE)),
       CASE WHEN i.donViCoBan IN (N'Viên', N'Gói') THEN 300 ELSE 60 END,
       i.giaNhapMacDinh, i.giaBanMacDinh
FROM dbo.Item i
CROSS APPLY (SELECT TOP 1 id FROM dbo.Kho WHERE isDeleted = 0 AND trangThai = 'HOAT_DONG' ORDER BY id) k
WHERE i.maItem IN ('THUOC016','THUOC017','THUOC018','THUOC019','THUOC020','THUOC021','THUOC022','THUOC023','THUOC024','THUOC025')
  AND i.isDeleted = 0
  AND NOT EXISTS (SELECT 1 FROM dbo.TonKho t WHERE t.maItem = i.id AND t.maKho = k.id AND t.isDeleted = 0);

-- 4. Bổ sung 2 nhà cung cấp mẫu nếu bảng NhaCungCap đang trống
IF NOT EXISTS (SELECT 1 FROM dbo.NhaCungCap WHERE isDeleted = 0)
BEGIN
    INSERT INTO dbo.NhaCungCap (tenNhaCungCap, maSoThue, soDienThoai, email, diaChi, trangThai)
    VALUES
        (N'Công ty Cổ phần Dược Hậu Giang (DHG Pharma)', '1800156801', '02923891433', 'dhgpharma@dhgpharma.com.vn', N'288 Bis Nguyễn Văn Cừ, Ninh Kiều, Cần Thơ', 'DANG_GIAO_DICH'),
        (N'Công ty Cổ phần Traphaco', '0100108656', '18006612', 'info@traphaco.com.vn', N'75 Yên Ninh, Ba Đình, Hà Nội', 'DANG_GIAO_DICH');
END;
