-- Seed sample Functional Food (Thực phẩm chức năng - TPCN) catalog items, stock batches,
-- and sample consultation prescriptions for testing reporting & prescribing (idempotent).

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'TPCN001')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT, thongTinThuoc)
    VALUES ('TPCN001', N'Viên uống Bổ Mắt Lutein Gold', 'THUOC', N'Lutein, Zeaxanthin, Vitamin A', N'20mg', N'Hộp', N'Hộp 30 viên', 135000, 180000, 10, 500, N'Viên nang mềm', N'Mỹ', N'Puritan Pride', 'TPCN-1001-23', 1, N'Thực phẩm chức năng', 0,
            N'<properties><entry key="thucPhamChucNang">true</entry></properties>');

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'TPCN002')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT, thongTinThuoc)
    VALUES ('TPCN002', N'Hoạt Huyết Dưỡng Não Ginkgo Biloba', 'THUOC', N'Ginkgo Biloba Extract', N'120mg', N'Hộp', N'Hộp 60 viên', 110000, 150000, 10, 500, N'Viên nang', N'Pháp', N'Arkopharma', 'TPCN-1002-23', 1, N'Thực phẩm chức năng', 0,
            N'<properties><entry key="thucPhamChucNang">true</entry></properties>');

IF NOT EXISTS (SELECT 1 FROM dbo.Item WHERE maItem = 'TPCN003')
    INSERT INTO dbo.Item (maItem, tenItem, loaiItem, hoatChat, hamLuong, donViCoBan, quyCachDongGoi, giaNhapMacDinh, giaBanMacDinh, tonToiThieu, tonToiDa, dangBaoChe, nuocSanXuat, hangSanXuat, soDangKy, hoatDong, nhomThuoc, thuocBHYT, thongTinThuoc)
    VALUES ('TPCN003', N'Omega-3 Fish Oil 1000mg', 'THUOC', N'EPA, DHA', N'1000mg', N'Lọ', N'Lọ 100 viên', 165000, 220000, 10, 500, N'Viên nang mềm', N'Úc', N'Blackmores', 'TPCN-1003-23', 1, N'Thực phẩm chức năng', 0,
            N'<properties><entry key="thucPhamChucNang">true</entry></properties>');

-- Seed TonKho for TPCN items in the first active Kho of facility 1
DECLARE @KhoId INT = (SELECT TOP 1 id FROM dbo.Kho WHERE maCoSo = 1 AND isDeleted = 0 AND trangThai = 'HOAT_DONG' ORDER BY id);
IF @KhoId IS NOT NULL
BEGIN
    INSERT INTO dbo.TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan)
    SELECT @KhoId, i.id, 'LO-TPCN-01', DATEADD(year, 2, CAST(GETDATE() AS DATE)), 200, i.giaNhapMacDinh, i.giaBanMacDinh
    FROM dbo.Item i
    WHERE i.maItem IN ('TPCN001', 'TPCN002', 'TPCN003')
      AND NOT EXISTS (
          SELECT 1 FROM dbo.TonKho t
          WHERE t.maKho = @KhoId AND t.maItem = i.id AND t.soLo = 'LO-TPCN-01' AND t.isDeleted = 0
      );
END

-- Seed 2 sample TPCN consultation prescriptions on existing LuotKham in facility 1 (if not yet seeded)
DECLARE @DoctorId INT = (SELECT TOP 1 id FROM dbo.NhanVien WHERE maCoSo = 1 AND isDeleted = 0 ORDER BY id);
DECLARE @Visit1 INT = (SELECT TOP 1 id FROM dbo.LuotKham WHERE maCoSo = 1 AND isDeleted = 0 ORDER BY id DESC);
DECLARE @Item1 INT = (SELECT TOP 1 id FROM dbo.Item WHERE maItem = 'TPCN001' AND isDeleted = 0);
DECLARE @Item2 INT = (SELECT TOP 1 id FROM dbo.Item WHERE maItem = 'TPCN002' AND isDeleted = 0);

IF @DoctorId IS NOT NULL AND @Visit1 IS NOT NULL AND @Item1 IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.DonThuoc WHERE maLuotKham = @Visit1 AND ghiChu = N'Tư vấn Thực phẩm chức năng' AND isDeleted = 0)
BEGIN
    INSERT INTO dbo.DonThuoc (maLuotKham, maBacSi, maCoSo, maKhoXuat, ngayKe, trangThai, ghiChu, loiDanDonThuoc, createdBy)
    VALUES (@Visit1, @DoctorId, 1, @KhoId, GETDATE(), 'DA_KE', N'Tư vấn Thực phẩm chức năng', N'Uống sau bữa ăn, kết hợp nghỉ ngơi hợp lý.', @DoctorId);
    DECLARE @DonId INT = SCOPE_IDENTITY();

    INSERT INTO dbo.ChiTietDonThuoc (maDonThuoc, maItem, soLuongKe, soLuongDaXuat, lieuDung, tanSuat, soNgay, huongDan, donGiaKe, duongDung)
    VALUES (@DonId, @Item1, 1, 0, N'1 viên/lần', N'2 lần/ngày', 30, N'Uống sáng - chiều sau ăn', 180000, N'Uống');

    IF @Item2 IS NOT NULL
        INSERT INTO dbo.ChiTietDonThuoc (maDonThuoc, maItem, soLuongKe, soLuongDaXuat, lieuDung, tanSuat, soNgay, huongDan, donGiaKe, duongDung)
        VALUES (@DonId, @Item2, 1, 0, N'1 viên/lần', N'1 lần/ngày', 30, N'Uống buổi sáng sau ăn', 150000, N'Uống');
END
