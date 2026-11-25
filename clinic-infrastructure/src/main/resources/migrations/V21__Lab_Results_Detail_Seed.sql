-- V21: Nạp dữ liệu chi tiết kết quả xét nghiệm (ChiTietKetQuaXetNghiem) cho các phiếu đã có kết quả
-- và bổ sung các phiếu xét nghiệm đa chỉ số (Hóa sinh, Huyết học) trong ngày để liên kết đầy đủ màn hình Xét Nghiệm với CSDL.
-- Idempotent: có thể chạy lại nhiều lần an toàn.

DECLARE @bsId INT = ISNULL((SELECT TOP 1 id FROM dbo.NhanVien WHERE isDeleted = 0 ORDER BY id), 1);
DECLARE @pkId INT = ISNULL((SELECT TOP 1 id FROM dbo.PhongKham WHERE isDeleted = 0 ORDER BY id), 1);

DECLARE @ureId INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_HS_URE_CRE' AND isDeleted = 0);
DECLARE @creId INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_HS_CREATININ' AND isDeleted = 0);
DECLARE @gluId INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_GLUCOSE' AND isDeleted = 0);
DECLARE @ctmId INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_CTM' AND isDeleted = 0);

DECLARE @lk1 INT = (SELECT TOP 1 id FROM dbo.LuotKham WHERE isDeleted = 0 AND maCoSo = 1 ORDER BY CASE WHEN doiTuongKham = 'BHYT' THEN 0 ELSE 1 END, id DESC);
DECLARE @lk2 INT = (SELECT TOP 1 id FROM dbo.LuotKham WHERE isDeleted = 0 AND maCoSo = 1 AND id <> ISNULL(@lk1, 0) ORDER BY id DESC);

-- 1. Bổ sung chi tiết ChiTietKetQuaXetNghiem cho phiếu H15252 (Định lượng CRP)
DECLARE @cdCrp INT = (SELECT TOP 1 id FROM dbo.ChiDinhDichVu WHERE soBarcode = 'H15252' AND isDeleted = 0);
IF @cdCrp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.ChiTietKetQuaXetNghiem WHERE maChiDinhDichVu = @cdCrp AND isDeleted = 0)
BEGIN
    INSERT INTO dbo.ChiTietKetQuaXetNghiem (maChiDinhDichVu, tenChiSo, giaTri, donViDo, khoangThamChieu, danhGia, createdBy)
    VALUES (@cdCrp, N'Định lượng CRP (hs-CRP) [Máu]', N'3.2', N'mg/L', N'0.0 - 5.0', N'Bình thường', @bsId);
END;

-- 2. Bổ sung chi tiết ChiTietKetQuaXetNghiem cho phiếu H01001 (Tổng phân tích tế bào máu 18 thông số)
DECLARE @cdCtmOld INT = (SELECT TOP 1 id FROM dbo.ChiDinhDichVu WHERE soBarcode = 'H01001' AND isDeleted = 0);
IF @cdCtmOld IS NOT NULL
BEGIN
    UPDATE dbo.ChiDinhDichVu
    SET ketQua = N'WBC: 7.2 G/L; RBC: 4.6 T/L; Hb: 141 g/L; HCT: 42.0 %; PLT: 255 G/L',
        ketLuan = N'Công thức máu trong giới hạn bình thường',
        thoiGianBatDauLayMau = COALESCE(thoiGianBatDauLayMau, thoiGianThucHien, thoiGianChiDinh),
        thoiGianKetThucLayMau = COALESCE(thoiGianKetThucLayMau, thoiGianTraKetQua, thoiGianChiDinh),
        nguoiLayMau = COALESCE(nguoiLayMau, bacSiThucHien, @bsId)
    WHERE id = @cdCtmOld AND (ketQua IS NULL OR LTRIM(RTRIM(ketQua)) = '');

    IF NOT EXISTS (SELECT 1 FROM dbo.ChiTietKetQuaXetNghiem WHERE maChiDinhDichVu = @cdCtmOld AND isDeleted = 0)
    BEGIN
        INSERT INTO dbo.ChiTietKetQuaXetNghiem (maChiDinhDichVu, tenChiSo, giaTri, donViDo, khoangThamChieu, danhGia, createdBy)
        VALUES
            (@cdCtmOld, N'Số lượng bạch cầu (WBC)', N'7.2', N'G/L', N'4.0 - 10.0', N'Bình thường', @bsId),
            (@cdCtmOld, N'Số lượng hồng cầu (RBC)', N'4.6', N'T/L', N'3.8 - 5.5', N'Bình thường', @bsId),
            (@cdCtmOld, N'Huyết sắc tố (Hb)', N'141', N'g/L', N'120 - 160', N'Bình thường', @bsId),
            (@cdCtmOld, N'Hematocrit (HCT)', N'42.0', N'%', N'35.0 - 47.0', N'Bình thường', @bsId),
            (@cdCtmOld, N'Số lượng tiểu cầu (PLT)', N'255', N'G/L', N'150 - 450', N'Bình thường', @bsId);
    END;
END;

-- 3. Bổ sung chi tiết ChiTietKetQuaXetNghiem cho phiếu H00009 (HBsAg)
DECLARE @cdHbs INT = (SELECT TOP 1 id FROM dbo.ChiDinhDichVu WHERE soBarcode = 'H00009' AND isDeleted = 0);
IF @cdHbs IS NOT NULL
BEGIN
    UPDATE dbo.ChiDinhDichVu
    SET ketQua = N'Âm tính (S/CO = 0.28)',
        ketLuan = N'HBsAg Âm tính',
        thoiGianBatDauLayMau = COALESCE(thoiGianBatDauLayMau, thoiGianThucHien, thoiGianChiDinh),
        thoiGianKetThucLayMau = COALESCE(thoiGianKetThucLayMau, thoiGianTraKetQua, thoiGianChiDinh),
        nguoiLayMau = COALESCE(nguoiLayMau, bacSiThucHien, @bsId)
    WHERE id = @cdHbs AND (ketQua IS NULL OR LTRIM(RTRIM(ketQua)) = '');

    IF NOT EXISTS (SELECT 1 FROM dbo.ChiTietKetQuaXetNghiem WHERE maChiDinhDichVu = @cdHbs AND isDeleted = 0)
    BEGIN
        INSERT INTO dbo.ChiTietKetQuaXetNghiem (maChiDinhDichVu, tenChiSo, giaTri, donViDo, khoangThamChieu, danhGia, createdBy)
        VALUES (@cdHbs, N'HBsAg test nhanh / miễn dịch', N'Âm tính (0.28)', N'S/CO', N'< 1.0 (Âm tính)', N'Bình thường', @bsId);
    END;
END;

-- 4. Tạo thêm phiếu Barcode 15258 (Huyết học 18 thông số đã có kết quả chi tiết hôm nay)
IF @lk1 IS NOT NULL AND @ctmId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.ChiDinhDichVu WHERE soBarcode = '15258' AND isDeleted = 0)
BEGIN
    INSERT INTO dbo.ChiDinhDichVu (
        maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, bacSiThucHien, soLuong, donGia, trangThai,
        ketQua, ketLuan, thoiGianChiDinh, thoiGianThucHien, thoiGianBatDauLayMau, thoiGianKetThucLayMau, thoiGianTraKetQua,
        nguoiLayMau, maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
    )
    VALUES (
        @lk1, @ctmId, @bsId, @pkId, @bsId, 1, 85000, 'DA_CO_KET_QUA',
        N'WBC: 6.8 G/L; RBC: 4.5 T/L; Hb: 138 g/L; HCT: 41.2 %; PLT: 240 G/L',
        N'Các chỉ số huyết học trong giới hạn bình thường',
        DATEADD(MINUTE, -65, SYSDATETIME()), DATEADD(MINUTE, -55, SYSDATETIME()),
        DATEADD(MINUTE, -58, SYSDATETIME()), DATEADD(MINUTE, -55, SYSDATETIME()), DATEADD(MINUTE, -30, SYSDATETIME()),
        @bsId, 'XN261119.14', '15258', N'Lấy mẫu huyết học', N'Tổng phân tích tế bào máu ngoại vi', @bsId
    );

    DECLARE @cd15258 INT = SCOPE_IDENTITY();
    IF @cd15258 IS NOT NULL
    BEGIN
        INSERT INTO dbo.ChiTietKetQuaXetNghiem (maChiDinhDichVu, tenChiSo, giaTri, donViDo, khoangThamChieu, danhGia, createdBy)
        VALUES
            (@cd15258, N'Số lượng bạch cầu (WBC)', N'6.8', N'G/L', N'4.0 - 10.0', N'Bình thường', @bsId),
            (@cd15258, N'Số lượng hồng cầu (RBC)', N'4.5', N'T/L', N'3.8 - 5.5', N'Bình thường', @bsId),
            (@cd15258, N'Huyết sắc tố (Hb)', N'138', N'g/L', N'120 - 160', N'Bình thường', @bsId),
            (@cd15258, N'Hematocrit (HCT)', N'41.2', N'%', N'35.0 - 47.0', N'Bình thường', @bsId),
            (@cd15258, N'Số lượng tiểu cầu (PLT)', N'240', N'G/L', N'150 - 450', N'Bình thường', @bsId);
    END;
END;

-- 5. Tạo thêm phiếu Barcode H15257 (Hóa sinh 3 chỉ số đã có kết quả hôm nay để đối chiếu và vẽ biểu đồ)
IF @lk2 IS NOT NULL AND @ureId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.ChiDinhDichVu WHERE soBarcode = 'H15257' AND isDeleted = 0)
BEGIN
    INSERT INTO dbo.ChiDinhDichVu (
        maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, bacSiThucHien, soLuong, donGia, trangThai,
        ketQua, ketLuan, thoiGianChiDinh, thoiGianThucHien, thoiGianBatDauLayMau, thoiGianKetThucLayMau, thoiGianTraKetQua,
        nguoiLayMau, maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
    )
    VALUES (
        @lk2, @ureId, @bsId, @pkId, @bsId, 1, 40000, 'DA_CO_KET_QUA',
        N'5.1 mmol/L', N'Bình thường',
        DATEADD(MINUTE, -75, SYSDATETIME()), DATEADD(MINUTE, -65, SYSDATETIME()),
        DATEADD(MINUTE, -68, SYSDATETIME()), DATEADD(MINUTE, -65, SYSDATETIME()), DATEADD(MINUTE, -40, SYSDATETIME()),
        @bsId, 'XN261119.15', 'H15257', N'Lấy mẫu sinh hóa', N'Chức năng thận - đường huyết', @bsId
    );
    DECLARE @cdUre257 INT = SCOPE_IDENTITY();
    IF @cdUre257 IS NOT NULL
        INSERT INTO dbo.ChiTietKetQuaXetNghiem (maChiDinhDichVu, tenChiSo, giaTri, donViDo, khoangThamChieu, danhGia, createdBy)
        VALUES (@cdUre257, N'Định lượng Ure máu [Máu]', N'5.1', N'mmol/L', N'2.5 - 7.5', N'Bình thường', @bsId);

    IF @creId IS NOT NULL
    BEGIN
        INSERT INTO dbo.ChiDinhDichVu (
            maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, bacSiThucHien, soLuong, donGia, trangThai,
            ketQua, ketLuan, thoiGianChiDinh, thoiGianThucHien, thoiGianBatDauLayMau, thoiGianKetThucLayMau, thoiGianTraKetQua,
            nguoiLayMau, maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
        )
        VALUES (
            @lk2, @creId, @bsId, @pkId, @bsId, 1, 40000, 'DA_CO_KET_QUA',
            N'82 µmol/L', N'Bình thường',
            DATEADD(MINUTE, -75, SYSDATETIME()), DATEADD(MINUTE, -65, SYSDATETIME()),
            DATEADD(MINUTE, -68, SYSDATETIME()), DATEADD(MINUTE, -65, SYSDATETIME()), DATEADD(MINUTE, -40, SYSDATETIME()),
            @bsId, 'XN261119.15', 'H15257', N'Lấy mẫu sinh hóa', N'Chức năng thận - đường huyết', @bsId
        );
        DECLARE @cdCre257 INT = SCOPE_IDENTITY();
        IF @cdCre257 IS NOT NULL
            INSERT INTO dbo.ChiTietKetQuaXetNghiem (maChiDinhDichVu, tenChiSo, giaTri, donViDo, khoangThamChieu, danhGia, createdBy)
            VALUES (@cdCre257, N'Định lượng Creatinin máu [Máu]', N'82', N'µmol/L', N'53 - 106', N'Bình thường', @bsId);
    END;

    IF @gluId IS NOT NULL
    BEGIN
        INSERT INTO dbo.ChiDinhDichVu (
            maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, bacSiThucHien, soLuong, donGia, trangThai,
            ketQua, ketLuan, thoiGianChiDinh, thoiGianThucHien, thoiGianBatDauLayMau, thoiGianKetThucLayMau, thoiGianTraKetQua,
            nguoiLayMau, maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
        )
        VALUES (
            @lk2, @gluId, @bsId, @pkId, @bsId, 1, 45000, 'DA_CO_KET_QUA',
            N'5.6 mmol/L', N'Bình thường',
            DATEADD(MINUTE, -75, SYSDATETIME()), DATEADD(MINUTE, -65, SYSDATETIME()),
            DATEADD(MINUTE, -68, SYSDATETIME()), DATEADD(MINUTE, -65, SYSDATETIME()), DATEADD(MINUTE, -40, SYSDATETIME()),
            @bsId, 'XN261119.15', 'H15257', N'Lấy mẫu sinh hóa', N'Chức năng thận - đường huyết', @bsId
        );
        DECLARE @cdGlu257 INT = SCOPE_IDENTITY();
        IF @cdGlu257 IS NOT NULL
            INSERT INTO dbo.ChiTietKetQuaXetNghiem (maChiDinhDichVu, tenChiSo, giaTri, donViDo, khoangThamChieu, danhGia, createdBy)
            VALUES (@cdGlu257, N'Định lượng Glucose máu', N'5.6', N'mmol/L', N'3.9 - 6.4', N'Bình thường', @bsId);
    END;
END;
