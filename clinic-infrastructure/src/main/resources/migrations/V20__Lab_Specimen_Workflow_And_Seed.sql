-- V20: Bổ sung cột quy trình lấy mẫu bệnh phẩm (Barcode, Mã phiếu, Bắt đầu/Kết thúc lấy mẫu, Người/Nơi lấy mẫu, Loại bệnh phẩm)
-- và nạp dữ liệu mẫu xét nghiệm thực tế cho màn hình Lấy Mẫu Bệnh Phẩm.
-- Idempotent: có thể chạy lại nhiều lần an toàn.

IF COL_LENGTH('dbo.DanhMucDichVu', 'loaiBenhPham') IS NULL
BEGIN
    ALTER TABLE dbo.DanhMucDichVu ADD loaiBenhPham NVARCHAR(150) NULL;
END;
GO

IF COL_LENGTH('dbo.ChiDinhDichVu', 'maPhieuChiDinh') IS NULL
BEGIN
    ALTER TABLE dbo.ChiDinhDichVu ADD maPhieuChiDinh VARCHAR(40) NULL;
END;

IF COL_LENGTH('dbo.ChiDinhDichVu', 'soBarcode') IS NULL
BEGIN
    ALTER TABLE dbo.ChiDinhDichVu ADD soBarcode VARCHAR(30) NULL;
END;

IF COL_LENGTH('dbo.ChiDinhDichVu', 'thoiGianBatDauLayMau') IS NULL
BEGIN
    ALTER TABLE dbo.ChiDinhDichVu ADD thoiGianBatDauLayMau DATETIME2 NULL;
END;

IF COL_LENGTH('dbo.ChiDinhDichVu', 'thoiGianKetThucLayMau') IS NULL
BEGIN
    ALTER TABLE dbo.ChiDinhDichVu ADD thoiGianKetThucLayMau DATETIME2 NULL;
END;

IF COL_LENGTH('dbo.ChiDinhDichVu', 'nguoiLayMau') IS NULL
BEGIN
    ALTER TABLE dbo.ChiDinhDichVu ADD nguoiLayMau INT NULL;
END;

IF COL_LENGTH('dbo.ChiDinhDichVu', 'noiLayMau') IS NULL
BEGIN
    ALTER TABLE dbo.ChiDinhDichVu ADD noiLayMau NVARCHAR(150) NULL;
END;

IF COL_LENGTH('dbo.ChiDinhDichVu', 'ghiChuChiDinh') IS NULL
BEGIN
    ALTER TABLE dbo.ChiDinhDichVu ADD ghiChuChiDinh NVARCHAR(500) NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_ChiDinhDichVu_Barcode_TrangThai'
      AND object_id = OBJECT_ID('dbo.ChiDinhDichVu')
)
BEGIN
    CREATE INDEX IX_ChiDinhDichVu_Barcode_TrangThai
        ON dbo.ChiDinhDichVu (soBarcode, trangThai, thoiGianChiDinh)
        WHERE isDeleted = 0;
END;
GO

-- 1. Cập nhật Loại Bệnh Phẩm chuẩn cho toàn bộ danh mục Xét Nghiệm
UPDATE dbo.DanhMucDichVu
SET loaiBenhPham = CASE
    WHEN nhomDichVu = N'Hóa sinh' OR maDichVu LIKE 'XN_HS_%' OR maDichVu = 'XN_GLUCOSE'
        THEN N'Máu hóa sinh, miễn dịch'
    WHEN nhomDichVu = N'Miễn dịch' OR maDichVu LIKE 'XN_MD_%'
        THEN N'Máu hóa sinh, miễn dịch'
    WHEN nhomDichVu = N'Huyết học' OR maDichVu LIKE 'XN_HH_%' OR maDichVu = 'XN_CTM'
        THEN N'Máu toàn phần EDTA'
    WHEN nhomDichVu = N'Đông máu' OR maDichVu LIKE 'XN_DM_%'
        THEN N'Huyết tương Citrat 3.8%'
    WHEN nhomDichVu = N'Nước tiểu' OR maDichVu LIKE 'XN_NT_%'
        THEN N'Nước tiểu giữa dòng'
    WHEN nhomDichVu = N'Vi sinh' OR maDichVu LIKE 'XN_VS_%'
        THEN N'Bệnh phẩm vi sinh / dịch'
    ELSE N'Mẫu tế bào / mô bệnh học'
END
WHERE loaiDichVu = 'XET_NGHIEM' AND (loaiBenhPham IS NULL OR LTRIM(RTRIM(loaiBenhPham)) = '');

-- 2. Đồng bộ các phiếu ChiDinhDichVu cũ chưa có soBarcode / maPhieuChiDinh / noiLayMau
UPDATE cd
SET cd.maPhieuChiDinh = ISNULL(cd.maPhieuChiDinh, CONCAT('XN', FORMAT(ISNULL(cd.thoiGianChiDinh, SYSDATETIME()), 'yyMMdd'), '.', cd.id)),
    cd.soBarcode      = ISNULL(cd.soBarcode, CONCAT('H', RIGHT(CONCAT('00000', CAST(cd.id AS VARCHAR(10))), 5))),
    cd.noiLayMau      = ISNULL(cd.noiLayMau, CASE
                            WHEN dv.nhomDichVu = N'Huyết học' THEN N'Lấy mẫu huyết học'
                            WHEN dv.nhomDichVu = N'Nước tiểu' THEN N'Lấy mẫu nước tiểu'
                            WHEN dv.nhomDichVu = N'Vi sinh'   THEN N'Lấy mẫu vi sinh'
                            ELSE N'Lấy mẫu sinh hóa'
                        END),
    cd.thoiGianBatDauLayMau = COALESCE(cd.thoiGianBatDauLayMau, cd.thoiGianThucHien),
    cd.thoiGianKetThucLayMau = COALESCE(cd.thoiGianKetThucLayMau, cd.thoiGianTraKetQua, cd.thoiGianThucHien),
    cd.nguoiLayMau    = COALESCE(cd.nguoiLayMau, cd.bacSiThucHien)
FROM dbo.ChiDinhDichVu cd
JOIN dbo.DanhMucDichVu dv ON dv.id = cd.maDichVu
WHERE cd.isDeleted = 0 AND dv.loaiDichVu = 'XET_NGHIEM';

-- 3. Nạp bộ phiếu chỉ định xét nghiệm mẫu (gom nhiều xét nghiệm theo Barcode, đủ trạng thái Đang chờ / Đang lấy mẫu / Đã có KQ)
DECLARE @ureId INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_HS_URE_CRE' AND isDeleted = 0);
DECLARE @creId INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_HS_CREATININ' AND isDeleted = 0);
DECLARE @gluId INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_GLUCOSE' AND isDeleted = 0);
DECLARE @ctmId INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_CTM' AND isDeleted = 0);
DECLARE @ntId  INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_NT_TONGPHANTICH' AND isDeleted = 0);
DECLARE @crpId INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_MD_CRP' AND isDeleted = 0);
DECLARE @astId INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_HS_AST_GOT' AND isDeleted = 0);
DECLARE @altId INT = (SELECT TOP 1 id FROM dbo.DanhMucDichVu WHERE maDichVu = 'XN_HS_ALT_GPT' AND isDeleted = 0);

DECLARE @bsId  INT = ISNULL((SELECT TOP 1 id FROM dbo.NhanVien WHERE isDeleted = 0 ORDER BY id), 1);
DECLARE @pkId  INT = ISNULL((SELECT TOP 1 id FROM dbo.PhongKham WHERE isDeleted = 0 ORDER BY id), 1);

DECLARE @lk1 INT = (SELECT TOP 1 id FROM dbo.LuotKham WHERE isDeleted = 0 AND maCoSo = 1 ORDER BY CASE WHEN doiTuongKham = 'BHYT' THEN 0 ELSE 1 END, id DESC);
DECLARE @lk2 INT = (SELECT TOP 1 id FROM dbo.LuotKham WHERE isDeleted = 0 AND maCoSo = 1 AND id <> ISNULL(@lk1, 0) ORDER BY id DESC);
DECLARE @lk3 INT = (SELECT TOP 1 id FROM dbo.LuotKham WHERE isDeleted = 0 AND maCoSo = 1 AND id NOT IN (ISNULL(@lk1, 0), ISNULL(@lk2, 0)) ORDER BY id DESC);

-- Phiếu 1: Barcode H15256 (Xét nghiệm Hóa sinh - Đang chờ lấy mẫu, gồm 3 chỉ số Ure, Creatinin, Glucose)
IF @lk1 IS NOT NULL AND @ureId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.ChiDinhDichVu WHERE soBarcode = 'H15256' AND isDeleted = 0)
BEGIN
    INSERT INTO dbo.ChiDinhDichVu (
        maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, soLuong, donGia, trangThai,
        thoiGianChiDinh, maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
    )
    VALUES
        (@lk1, @ureId, @bsId, @pkId, 1, 40000, 'CHO_THUC_HIEN', DATEADD(MINUTE, -35, SYSDATETIME()), 'XN261119.9', 'H15256', N'Lấy mẫu sinh hóa', N'Nhịn ăn sáng trước lấy mẫu', @bsId);

    IF @creId IS NOT NULL
        INSERT INTO dbo.ChiDinhDichVu (
            maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, soLuong, donGia, trangThai,
            thoiGianChiDinh, maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
        )
        VALUES
            (@lk1, @creId, @bsId, @pkId, 1, 40000, 'CHO_THUC_HIEN', DATEADD(MINUTE, -35, SYSDATETIME()), 'XN261119.9', 'H15256', N'Lấy mẫu sinh hóa', N'Đánh giá chức năng thận', @bsId);

    IF @gluId IS NOT NULL
        INSERT INTO dbo.ChiDinhDichVu (
            maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, soLuong, donGia, trangThai,
            thoiGianChiDinh, maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
        )
        VALUES
            (@lk1, @gluId, @bsId, @pkId, 1, 45000, 'CHO_THUC_HIEN', DATEADD(MINUTE, -35, SYSDATETIME()), 'XN261119.9', 'H15256', N'Lấy mẫu sinh hóa', N'Đường huyết lúc đói', @bsId);
END;

-- Phiếu 2: Barcode 15254 (Huyết học - Đang chờ lấy mẫu)
IF @lk2 IS NOT NULL AND @ctmId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.ChiDinhDichVu WHERE soBarcode = '15254' AND isDeleted = 0)
BEGIN
    INSERT INTO dbo.ChiDinhDichVu (
        maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, soLuong, donGia, trangThai,
        thoiGianChiDinh, maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
    )
    VALUES
        (@lk2, @ctmId, @bsId, @pkId, 1, 85000, 'CHO_THUC_HIEN', DATEADD(MINUTE, -28, SYSDATETIME()), 'XN261119.10', '15254', N'Lấy mẫu huyết học', N'Ống EDTA nắp tím 2ml', @bsId);
END;

-- Phiếu 3: Barcode 15255 (Men gan AST + ALT - Đang lấy mẫu)
IF @lk2 IS NOT NULL AND @astId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.ChiDinhDichVu WHERE soBarcode = '15255' AND isDeleted = 0)
BEGIN
    INSERT INTO dbo.ChiDinhDichVu (
        maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, bacSiThucHien, soLuong, donGia, trangThai,
        thoiGianChiDinh, thoiGianThucHien, thoiGianBatDauLayMau, nguoiLayMau,
        maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
    )
    VALUES
        (@lk2, @astId, @bsId, @pkId, @bsId, 1, 45000, 'DANG_THUC_HIEN',
         DATEADD(MINUTE, -25, SYSDATETIME()), DATEADD(MINUTE, -10, SYSDATETIME()), DATEADD(MINUTE, -10, SYSDATETIME()), @bsId,
         'XN261119.11', '15255', N'Lấy mẫu sinh hóa', N'Kiểm tra men gan', @bsId);

    IF @altId IS NOT NULL
        INSERT INTO dbo.ChiDinhDichVu (
            maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, bacSiThucHien, soLuong, donGia, trangThai,
            thoiGianChiDinh, thoiGianThucHien, thoiGianBatDauLayMau, nguoiLayMau,
            maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
        )
        VALUES
            (@lk2, @altId, @bsId, @pkId, @bsId, 1, 45000, 'DANG_THUC_HIEN',
             DATEADD(MINUTE, -25, SYSDATETIME()), DATEADD(MINUTE, -10, SYSDATETIME()), DATEADD(MINUTE, -10, SYSDATETIME()), @bsId,
             'XN261119.11', '15255', N'Lấy mẫu sinh hóa', N'Kiểm tra men gan', @bsId);
END;

-- Phiếu 4: Barcode U15253 (Nước tiểu - Đang chờ lấy mẫu)
IF @lk3 IS NOT NULL AND @ntId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.ChiDinhDichVu WHERE soBarcode = 'U15253' AND isDeleted = 0)
BEGIN
    INSERT INTO dbo.ChiDinhDichVu (
        maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, soLuong, donGia, trangThai,
        thoiGianChiDinh, maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
    )
    VALUES
        (@lk3, @ntId, @bsId, @pkId, 1, 45000, 'CHO_THUC_HIEN', DATEADD(MINUTE, -20, SYSDATETIME()), 'XN261119.12', 'U15253', N'Lấy mẫu nước tiểu', N'Lấy nước tiểu giữa dòng', @bsId);
END;

-- Phiếu 5: Barcode H15252 (Miễn dịch CRP - Đã có kết quả)
IF @lk3 IS NOT NULL AND @crpId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.ChiDinhDichVu WHERE soBarcode = 'H15252' AND isDeleted = 0)
BEGIN
    INSERT INTO dbo.ChiDinhDichVu (
        maLuotKham, maDichVu, maBacSiChiDinh, maPhongThucHien, bacSiThucHien, soLuong, donGia, trangThai,
        ketQua, ketLuan, thoiGianChiDinh, thoiGianThucHien, thoiGianBatDauLayMau, thoiGianKetThucLayMau, thoiGianTraKetQua,
        nguoiLayMau, maPhieuChiDinh, soBarcode, noiLayMau, ghiChuChiDinh, createdBy
    )
    VALUES
        (@lk3, @crpId, @bsId, @pkId, @bsId, 1, 95000, 'DA_CO_KET_QUA',
         N'hs-CRP: 3.2 mg/L (Bình thường < 5.0 mg/L)', N'Trong giới hạn bình thường',
         DATEADD(MINUTE, -50, SYSDATETIME()), DATEADD(MINUTE, -40, SYSDATETIME()), DATEADD(MINUTE, -40, SYSDATETIME()),
         DATEADD(MINUTE, -35, SYSDATETIME()), DATEADD(MINUTE, -15, SYSDATETIME()),
         @bsId, 'XN261119.13', 'H15252', N'Lấy mẫu sinh hóa', N'Theo dõi phản ứng viêm', @bsId);
END;
