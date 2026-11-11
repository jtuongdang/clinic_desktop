-- ==============================================================================
-- SCRIPT XÓA SẠCH DỮ LIỆU GIAO DỊCH / KHÁM CHỮA BỆNH TEST (DATABASE CLEANUP)
-- HỆ THỐNG: PHẦN MỀM QUẢN LÝ BỆNH VIỆN EHC HIS
-- ==============================================================================
-- MỤC ĐÍCH:
-- 1. Xóa toàn bộ dữ liệu phát sinh (Bệnh nhân, Lượt khám, Bệnh án, Đơn thuốc, 
--    Viện phí, Cận lâm sàng, Hóa đơn, Công nợ, Chấm công, Bảng lương, Nhật ký...).
-- 2. BẢO TOÀN NGUYÊN VẸN 100% CẤU TRÚC VÀ DANH MỤC HỆ THỐNG:
--    - Cơ sở y tế (CoSo), Khoa phòng (Khoa, PhongKham)
--    - Tài khoản nhân sự (NhanVien, NhanVienVaiTro, NhanVienKhoa, PhanQuyenCoSo)
--    - Vai trò & Ma trận phân quyền (VaiTro, ChucVu, MaTranPhanQuyen)
--    - Danh mục dịch vụ kỹ thuật & cận lâm sàng (DanhMucDichVu)
--    - Danh mục mã bệnh ICD-10 (ICD)
--    - Danh mục kho & danh mục thuốc/vật tư (Kho, Item, DonViTinhQuyDoi)
--    - Cấu hình & Tham số hệ thống (ThamSoHeThong)
-- ==============================================================================

SET NOCOUNT ON;

BEGIN TRANSACTION;

BEGIN TRY
    PRINT N'=================================================================';
    PRINT N' BẮT ĐẦU XÓA DỮ LIỆU GIAO DỊCH TEST TRONG CƠ SỞ DỮ LIỆU';
    PRINT N'=================================================================';

    -- 1. XÓA VIỆN PHÍ, HÓA ĐƠN & CÔNG NỢ
    PRINT N'1. Đang dọn dẹp phân hệ Viện phí, Hóa đơn và Thu ngân...';
    IF OBJECT_ID('dbo.ChiTietHoaDon', 'U') IS NOT NULL DELETE FROM dbo.ChiTietHoaDon;
    IF OBJECT_ID('dbo.PhieuThu', 'U') IS NOT NULL DELETE FROM dbo.PhieuThu;
    IF OBJECT_ID('dbo.HoaDon', 'U') IS NOT NULL DELETE FROM dbo.HoaDon;
    IF OBJECT_ID('dbo.CongNoBenhNhan', 'U') IS NOT NULL DELETE FROM dbo.CongNoBenhNhan;

    -- 2. XÓA ĐƠN THUỐC & CẤP PHÁT THUỐC
    PRINT N'2. Đang dọn dẹp phân hệ Đơn thuốc và Cấp phát thuốc...';
    IF OBJECT_ID('dbo.ChiTietDonThuoc', 'U') IS NOT NULL DELETE FROM dbo.ChiTietDonThuoc;
    IF OBJECT_ID('dbo.DonThuoc', 'U') IS NOT NULL DELETE FROM dbo.DonThuoc;

    -- 3. XÓA CẬN LÂM SÀNG & KẾT QUẢ XÉT NGHIỆM
    PRINT N'3. Đang dọn dẹp phân hệ Cận lâm sàng và Chỉ định dịch vụ...';
    IF OBJECT_ID('dbo.ChiTietKetQuaXetNghiem', 'U') IS NOT NULL DELETE FROM dbo.ChiTietKetQuaXetNghiem;
    IF OBJECT_ID('dbo.ChiDinhDichVu', 'U') IS NOT NULL DELETE FROM dbo.ChiDinhDichVu;

    -- 4. XÓA BỆNH ÁN, SINH HIỆU & CHẨN ĐOÁN
    PRINT N'4. Đang dọn dẹp phân hệ Hồ sơ bệnh án, Sinh hiệu và Chẩn đoán...';
    IF OBJECT_ID('dbo.SinhHieu', 'U') IS NOT NULL DELETE FROM dbo.SinhHieu;
    IF OBJECT_ID('dbo.ChanDoan', 'U') IS NOT NULL DELETE FROM dbo.ChanDoan;

    -- 5. XÓA TIẾP ĐÓN, HÀNG ĐỢI & LƯỢT KHÁM
    PRINT N'5. Đang dọn dẹp Lượt khám và Lịch hẹn tiếp đón...';
    IF OBJECT_ID('dbo.LichHen', 'U') IS NOT NULL DELETE FROM dbo.LichHen;
    IF OBJECT_ID('dbo.LuotKham', 'U') IS NOT NULL DELETE FROM dbo.LuotKham;

    -- 6. XÓA THÔNG TIN BỆNH NHÂN & BẢO HIỂM
    PRINT N'6. Đang dọn dẹp Hồ sơ bệnh nhân, Bảo hiểm y tế và Tiền sử bệnh...';
    IF OBJECT_ID('dbo.TienSuBenh', 'U') IS NOT NULL DELETE FROM dbo.TienSuBenh;
    IF OBJECT_ID('dbo.BaoHiem', 'U') IS NOT NULL DELETE FROM dbo.BaoHiem;
    IF OBJECT_ID('dbo.BenhNhanCoSo', 'U') IS NOT NULL DELETE FROM dbo.BenhNhanCoSo;
    IF OBJECT_ID('dbo.BenhNhan', 'U') IS NOT NULL DELETE FROM dbo.BenhNhan;

    -- 7. XÓA CÁC GIAO DỊCH KHO PHÁT SINH
    PRINT N'7. Đang dọn dẹp Giao dịch xuất nhập kho dược phát sinh...';
    IF OBJECT_ID('dbo.InventoryTransaction', 'U') IS NOT NULL DELETE FROM dbo.InventoryTransaction;
    IF OBJECT_ID('dbo.ChiTietXuatKho', 'U') IS NOT NULL DELETE FROM dbo.ChiTietXuatKho;
    IF OBJECT_ID('dbo.PhieuXuatKho', 'U') IS NOT NULL DELETE FROM dbo.PhieuXuatKho;
    IF OBJECT_ID('dbo.ChiTietNhapKho', 'U') IS NOT NULL DELETE FROM dbo.ChiTietNhapKho;
    IF OBJECT_ID('dbo.PhieuNhapKho', 'U') IS NOT NULL DELETE FROM dbo.PhieuNhapKho;

    -- 8. XÓA DỮ LIỆU CHẤM CÔNG & BẢNG LƯƠNG PHÁT SINH
    PRINT N'8. Đang dọn dẹp Chấm công và Bảng lương nhân sự...';
    IF OBJECT_ID('dbo.BangLuong', 'U') IS NOT NULL DELETE FROM dbo.BangLuong;
    IF OBJECT_ID('dbo.KyLuong', 'U') IS NOT NULL DELETE FROM dbo.KyLuong;
    IF OBJECT_ID('dbo.ChamCong', 'U') IS NOT NULL DELETE FROM dbo.ChamCong;
    IF OBJECT_ID('dbo.LichLamViec', 'U') IS NOT NULL DELETE FROM dbo.LichLamViec;

    -- 9. XÓA NHẬT KÝ KIỂM TOÁN (AUDIT TRAIL) & RESET TRẠNG THÁI KHÓA ĐĂNG NHẬP
    PRINT N'9. Đang dọn dẹp Nhật ký kiểm toán và Reset bộ đếm đăng nhập sai...';
    IF OBJECT_ID('dbo.NhatKyHoSoBenhAn', 'U') IS NOT NULL DELETE FROM dbo.NhatKyHoSoBenhAn;
    IF OBJECT_ID('dbo.NhatKyHeThong', 'U') IS NOT NULL DELETE FROM dbo.NhatKyHeThong;
    
    -- Reset login throttle trên bảng NhanVien
    IF OBJECT_ID('dbo.NhanVien', 'U') IS NOT NULL AND COL_LENGTH('dbo.NhanVien', 'loginFailureCount') IS NOT NULL
    BEGIN
        UPDATE dbo.NhanVien
        SET loginFailureCount = 0,
            loginDelaySeconds = 0,
            loginFailureWindowStartedAt = NULL,
            loginLastFailureAt = NULL,
            loginNextAllowedAt = NULL;
    END;

    -- 10. RESET IDENTITY SEED CHO CÁC BẢNG PHÁT SINH
    PRINT N'10. Đang reset lại chỉ số tự tăng (Identity Seed) cho các bảng...';
    IF OBJECT_ID('dbo.BenhNhan', 'U') IS NOT NULL DBCC CHECKIDENT ('dbo.BenhNhan', RESEED, 0);
    IF OBJECT_ID('dbo.LuotKham', 'U') IS NOT NULL DBCC CHECKIDENT ('dbo.LuotKham', RESEED, 0);
    IF OBJECT_ID('dbo.LichHen', 'U') IS NOT NULL DBCC CHECKIDENT ('dbo.LichHen', RESEED, 0);
    IF OBJECT_ID('dbo.ChiDinhDichVu', 'U') IS NOT NULL DBCC CHECKIDENT ('dbo.ChiDinhDichVu', RESEED, 0);
    IF OBJECT_ID('dbo.DonThuoc', 'U') IS NOT NULL DBCC CHECKIDENT ('dbo.DonThuoc', RESEED, 0);
    IF OBJECT_ID('dbo.HoaDon', 'U') IS NOT NULL DBCC CHECKIDENT ('dbo.HoaDon', RESEED, 0);
    IF OBJECT_ID('dbo.PhieuThu', 'U') IS NOT NULL DBCC CHECKIDENT ('dbo.PhieuThu', RESEED, 0);

    COMMIT TRANSACTION;

    PRINT N'=================================================================';
    PRINT N' ✅ ĐÃ XÓA SẠCH DỮ LIỆU GIAO DỊCH THÀNH CÔNG!';
    PRINT N' ✅ TOÀN BỘ DANH MỤC & TÀI KHOẢN HỆ THỐNG ĐÃ ĐƯỢC BẢO TOÀN NGUYÊN VẸN!';
    PRINT N'=================================================================';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    DECLARE @ErrMsg NVARCHAR(4000) = ERROR_MESSAGE();
    DECLARE @ErrSeverity INT = ERROR_SEVERITY();
    DECLARE @ErrState INT = ERROR_STATE();
    PRINT N'❌ LỖI TRONG QUÁ TRÌNH DỌN DẸP DỮ LIỆU: ' + @ErrMsg;
    RAISERROR(@ErrMsg, @ErrSeverity, @ErrState);
END CATCH;
GO
