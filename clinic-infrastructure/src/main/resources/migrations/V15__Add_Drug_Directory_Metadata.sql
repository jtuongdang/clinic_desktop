-- Thông tin danh mục thuốc; không suy diễn BHYT/nhóm điều trị cho dữ liệu cũ.
ALTER TABLE dbo.Item ADD maTuongDuong NVARCHAR(100) NULL,
    nhomThuoc NVARCHAR(100) NULL,
    nongDo NVARCHAR(100) NULL,
    theTich NVARCHAR(100) NULL,
    thuocBHYT BIT NULL,
    maDanhMucYHCT NVARCHAR(100) NULL;
