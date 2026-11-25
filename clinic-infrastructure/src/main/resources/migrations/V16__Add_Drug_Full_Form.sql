-- Lưu thông tin bổ sung trên biểu mẫu thuốc; trường lõi vẫn nằm trong các cột Item hiện có.
ALTER TABLE dbo.Item ADD thongTinThuoc NVARCHAR(MAX) NULL;
