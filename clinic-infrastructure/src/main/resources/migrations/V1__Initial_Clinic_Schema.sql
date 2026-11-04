-- ==============================================================================
-- DATABASE MIGRATION SCRIPT: V1__Initial_Clinic_Schema.sql
-- HỆ THỐNG: CLINIC MANAGEMENT (EHC HIS)
-- CƠ SỞ DỮ LIỆU: MICROSOFT SQL SERVER
-- ==============================================================================

SET NOCOUNT ON;

-- 1. BẢNG CƠ SỞ (CoSo)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'CoSo')
CREATE TABLE CoSo (
    id INT IDENTITY(1,1) PRIMARY KEY,
    tenCoSo NVARCHAR(255) NOT NULL,
    diaChi NVARCHAR(500),
    dienThoai VARCHAR(20),
    email VARCHAR(100),
    loaiCoSo VARCHAR(50) DEFAULT 'PHONG_KHAM',
    hoatDong BIT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL
);

-- 2. BẢNG CHUYÊN KHOA (Khoa)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Khoa')
CREATE TABLE Khoa (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maCoSo INT NOT NULL,
    tenKhoa NVARCHAR(255) NOT NULL,
    moTa NVARCHAR(500),
    hoatDong BIT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_Khoa_CoSo FOREIGN KEY (maCoSo) REFERENCES CoSo(id)
);

-- 3. BẢNG PHÒNG KHÁM / BUỒNG KỸ THUẬT (PhongKham)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PhongKham')
CREATE TABLE PhongKham (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maKhoa INT NOT NULL,
    tenPhong NVARCHAR(255) NOT NULL,
    viTri NVARCHAR(255),
    hoatDong BIT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_PhongKham_Khoa FOREIGN KEY (maKhoa) REFERENCES Khoa(id)
);

-- 4. BẢNG CHỨC VỤ (ChucVu)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ChucVu')
CREATE TABLE ChucVu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    tenChucVu NVARCHAR(100) NOT NULL,
    moTa NVARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL
);

-- 5. BẢNG VAI TRÒ (VaiTro)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'VaiTro')
CREATE TABLE VaiTro (
    id INT IDENTITY(1,1) PRIMARY KEY,
    tenVaiTro NVARCHAR(100) NOT NULL,
    moTa NVARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL
);

-- 6. BẢNG NHÂN VIÊN / BÁC SĨ (NhanVien)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'NhanVien')
CREATE TABLE NhanVien (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maNhanVien VARCHAR(50) NOT NULL UNIQUE,
    ho NVARCHAR(100) NOT NULL,
    ten NVARCHAR(100) NOT NULL,
    soDienThoai VARCHAR(20),
    email VARCHAR(100),
    diaChi NVARCHAR(500),
    gioiTinh VARCHAR(10) DEFAULT 'M',
    ngaySinh DATE,
    maChucVu INT NULL,
    username VARCHAR(100) UNIQUE,
    passwordHash VARCHAR(255),
    hoatDong BIT NOT NULL DEFAULT 1,
    dangNhapCuoi DATETIME2 NULL,
    chungChiHanhNghe NVARCHAR(100),
    ngayVaoLam DATE,
    ngayNghiViec DATE NULL,
    lyDoNghiViec NVARCHAR(500),
    luongCoBan DECIMAL(18,2) DEFAULT 0,
    heSoLuong DECIMAL(5,2) DEFAULT 1.0,
    phuCap DECIMAL(18,2) DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_NhanVien_ChucVu FOREIGN KEY (maChucVu) REFERENCES ChucVu(id)
);

-- 7. BẢNG Nối NHÂN VIÊN - VAI TRÒ (NhanVienVaiTro)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'NhanVienVaiTro')
CREATE TABLE NhanVienVaiTro (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maNhanVien INT NOT NULL,
    maVaiTro INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_NhanVienVaiTro_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(id),
    CONSTRAINT FK_NhanVienVaiTro_VaiTro FOREIGN KEY (maVaiTro) REFERENCES VaiTro(id)
);

-- 8. BẢNG Nối NHÂN VIÊN - KHOA (NhanVienKhoa)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'NhanVienKhoa')
CREATE TABLE NhanVienKhoa (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maNhanVien INT NOT NULL,
    maKhoa INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_NhanVienKhoa_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(id),
    CONSTRAINT FK_NhanVienKhoa_Khoa FOREIGN KEY (maKhoa) REFERENCES Khoa(id)
);

-- 9. BẢNG Nối PHÂN QUYỀN CƠ SỞ (PhanQuyenCoSo)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PhanQuyenCoSo')
CREATE TABLE PhanQuyenCoSo (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maNhanVien INT NOT NULL,
    maCoSo INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_PhanQuyenCoSo_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(id),
    CONSTRAINT FK_PhanQuyenCoSo_CoSo FOREIGN KEY (maCoSo) REFERENCES CoSo(id)
);

-- 10. BẢNG BỆNH NHÂN (BenhNhan)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'BenhNhan')
CREATE TABLE BenhNhan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maBenhNhanFormat VARCHAR(50) UNIQUE,
    ho NVARCHAR(100) NOT NULL,
    ten NVARCHAR(100) NOT NULL,
    ngaySinh DATE NOT NULL,
    gioiTinh VARCHAR(10) DEFAULT 'M',
    dienThoai VARCHAR(20),
    email VARCHAR(100),
    soCCCD VARCHAR(20),
    diaChi NVARCHAR(500),
    soNhaThonPho NVARCHAR(255),
    noiSinh NVARCHAR(255),
    ngayCapCCCD DATE,
    noiCapCCCD NVARCHAR(255),
    noiLamViec NVARCHAR(255),
    ngheNghiep NVARCHAR(100),
    danToc NVARCHAR(50) DEFAULT N'Kinh',
    quocTich NVARCHAR(50) DEFAULT N'Việt Nam',
    maTinh VARCHAR(10),
    maHuyen VARCHAR(10),
    maXa VARCHAR(10),
    nguoiLienHeTen NVARCHAR(200),
    nguoiLienHeDienThoai VARCHAR(20),
    username VARCHAR(100),
    passwordHash VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL
);

-- 11. BẢNG BỆNH NHÂN - CƠ SỞ (BenhNhanCoSo)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'BenhNhanCoSo')
CREATE TABLE BenhNhanCoSo (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maBenhNhan INT NOT NULL,
    maCoSo INT NOT NULL,
    ngayDangKy DATETIME2 NOT NULL DEFAULT GETDATE(),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_BenhNhanCoSo_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(id),
    CONSTRAINT FK_BenhNhanCoSo_CoSo FOREIGN KEY (maCoSo) REFERENCES CoSo(id)
);

-- 12. BẢNG BẢO HIỂM Y TẾ (BaoHiem)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'BaoHiem')
CREATE TABLE BaoHiem (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maBenhNhan INT NOT NULL,
    loaiBaoHiem VARCHAR(50) DEFAULT 'BHYT',
    soBaoHiem VARCHAR(20) NOT NULL,
    ngayBatDau DATE NULL,
    ngayHetHan DATE NOT NULL,
    mucHuong INT NOT NULL DEFAULT 80,
    maNoiSong VARCHAR(10),
    maNoiDangKyKCB VARCHAR(20),
    noiDangKyKCB NVARCHAR(255),
    diaChiThe NVARCHAR(500),
    dungTuyen BIT NOT NULL DEFAULT 1,
    coGiayKhaiSinh BIT NOT NULL DEFAULT 0,
    khongGiuThe BIT NOT NULL DEFAULT 0,
    mienCungChiTra BIT NOT NULL DEFAULT 0,
    ngayMienCungChiTra DATE NULL,
    ngayDu5NamLienTuc DATE NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_BaoHiem_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(id)
);

-- 13. BẢNG TIỀN SỬ BỆNH (TienSuBenh)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'TienSuBenh')
CREATE TABLE TienSuBenh (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maBenhNhan INT NOT NULL,
    loaiTienSu VARCHAR(50) NOT NULL,
    moTa NVARCHAR(1000) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_TienSuBenh_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(id)
);

-- 14. BẢNG LỊCH HẸN KHÁM (LichHen)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'LichHen')
CREATE TABLE LichHen (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maBenhNhan INT NOT NULL,
    maBacSi INT NOT NULL,
    maCoSo INT NOT NULL,
    maKhoa INT NULL,
    thoiGianBatDau DATETIME2 NOT NULL,
    thoiGianKetThuc DATETIME2 NOT NULL,
    trangThai VARCHAR(50) DEFAULT 'CHO_XAC_NHAN',
    lyDo NVARCHAR(500),
    nguonDat VARCHAR(50) DEFAULT 'LE_TAN',
    chiPhiGiuCho DECIMAL(18,2) DEFAULT 0,
    daThanhToanGiuCho BIT DEFAULT 0,
    soPhieuThu VARCHAR(50),
    ngayThanhToan DATETIME2 NULL,
    nguoiThuTien INT NULL,
    ghiChuHuy NVARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_LichHen_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(id),
    CONSTRAINT FK_LichHen_BacSi FOREIGN KEY (maBacSi) REFERENCES NhanVien(id),
    CONSTRAINT FK_LichHen_CoSo FOREIGN KEY (maCoSo) REFERENCES CoSo(id),
    CONSTRAINT FK_LichHen_Khoa FOREIGN KEY (maKhoa) REFERENCES Khoa(id)
);

-- 15. BẢNG LƯỢT KHÁM BỆNH (LuotKham) - TRÁI TIM HỒ SƠ EMR
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'LuotKham')
CREATE TABLE LuotKham (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maBenhNhan INT NOT NULL,
    maBacSi INT NULL,
    maLichHen INT NULL,
    maCoSo INT NOT NULL,
    maKhoa INT NOT NULL,
    maPhongKham INT NULL,
    hinhThucDieuTri VARCHAR(50) DEFAULT 'NGOAI_TRU',
    trangThai VARCHAR(50) DEFAULT 'CHO_KHAM',
    soThuTu INT,
    soThuTuDisplay VARCHAR(50),
    ngayKham DATE NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    thoiGianTiepNhan DATETIME2 NOT NULL DEFAULT GETDATE(),
    thoiGianBatDauKham DATETIME2 NULL,
    thoiGianKetThucKham DATETIME2 NULL,
    lyDoKham NVARCHAR(500),
    doiTuongKham VARCHAR(50) DEFAULT 'THU_PHI',
    maDichVuKham INT NULL,
    loaiDangKyKham NVARCHAR(100) DEFAULT N'Khám bệnh',
    dnVnTien BIT NOT NULL DEFAULT 0,
    quaTrinhBenhLy NVARCHAR(MAX),
    tienSuBanThan NVARCHAR(MAX),
    tienSuGiaDinh NVARCHAR(MAX),
    khamToanThan NVARCHAR(MAX),
    khamBoPhan NVARCHAR(MAX),
    tomTatBenhAn NVARCHAR(MAX),
    huongDieuTri NVARCHAR(MAX),
    loiDanBacSi NVARCHAR(MAX),
    ketQuaDieuTri VARCHAR(50) NULL,
    huongXuTri VARCHAR(50) DEFAULT 'CHO_VE_TOA',
    ngayHenTaiKham DATE NULL,
    soNgayNghiHuongBHXH INT NULL,
    lyDoChuyenTuyen NVARCHAR(MAX),
    emrLocked BIT NOT NULL DEFAULT 0,
    emrSignature NVARCHAR(MAX),
    emrLockedAt DATETIME2 NULL,
    emrLockedBy INT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_LuotKham_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(id),
    CONSTRAINT FK_LuotKham_BacSi FOREIGN KEY (maBacSi) REFERENCES NhanVien(id),
    CONSTRAINT FK_LuotKham_LichHen FOREIGN KEY (maLichHen) REFERENCES LichHen(id),
    CONSTRAINT FK_LuotKham_CoSo FOREIGN KEY (maCoSo) REFERENCES CoSo(id),
    CONSTRAINT FK_LuotKham_Khoa FOREIGN KEY (maKhoa) REFERENCES Khoa(id),
    CONSTRAINT FK_LuotKham_PhongKham FOREIGN KEY (maPhongKham) REFERENCES PhongKham(id)
);

-- 16. BẢNG DANH MỤC MÃ BỆNH QUỐC TẾ ICD-10 (ICD)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ICD')
CREATE TABLE ICD (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maICD VARCHAR(50) NOT NULL UNIQUE,
    tenBenh NVARCHAR(500) NOT NULL,
    moTa NVARCHAR(1000),
    hoatDong BIT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL
);

-- 17. BẢNG CHẨN ĐOÁN (ChanDoan)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ChanDoan')
CREATE TABLE ChanDoan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maLuotKham INT NOT NULL,
    maICD INT NULL,
    chanDoanText NVARCHAR(1000) NOT NULL,
    laChinh BIT NOT NULL DEFAULT 0,
    loaiChanDoan VARCHAR(50) DEFAULT 'XAC_DINH',
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_ChanDoan_LuotKham FOREIGN KEY (maLuotKham) REFERENCES LuotKham(id),
    CONSTRAINT FK_ChanDoan_ICD FOREIGN KEY (maICD) REFERENCES ICD(id)
);

-- 18. BẢNG SINH HIỆU / CHỈ SỐ SINH TỒN (SinhHieu)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'SinhHieu')
CREATE TABLE SinhHieu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maLuotKham INT NOT NULL,
    mach INT,
    nhietDo DECIMAL(4,1),
    huyetApTamThu INT,
    huyetApTamTruong INT,
    nhipTho INT,
    canNang DECIMAL(5,2),
    chieuCao DECIMAL(5,2),
    bmi DECIMAL(4,1),
    spo2 DECIMAL(4,1),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_SinhHieu_LuotKham FOREIGN KEY (maLuotKham) REFERENCES LuotKham(id)
);

-- 19. BẢNG DANH MỤC DỊCH VỤ KỸ THUẬT (DanhMucDichVu)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DanhMucDichVu')
CREATE TABLE DanhMucDichVu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maDichVu VARCHAR(50) NOT NULL UNIQUE,
    tenDichVu NVARCHAR(255) NOT NULL,
    loaiDichVu VARCHAR(50) NOT NULL,
    donGia DECIMAL(18,2) NOT NULL DEFAULT 0,
    giaBHYT DECIMAL(18,2) NOT NULL DEFAULT 0,
    hoatDong BIT NOT NULL DEFAULT 1,
    moTa NVARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL
);

-- 20. BẢNG CHỈ ĐỊNH DỊCH VỤ / CẬN LÂM SÀNG (ChiDinhDichVu)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ChiDinhDichVu')
CREATE TABLE ChiDinhDichVu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maLuotKham INT NOT NULL,
    maDichVu INT NOT NULL,
    maBacSiChiDinh INT NOT NULL,
    maPhongThucHien INT NULL,
    bacSiThucHien INT NULL,
    soLuong DECIMAL(18,2) NOT NULL DEFAULT 1,
    donGia DECIMAL(18,2) NOT NULL DEFAULT 0,
    trangThai VARCHAR(50) DEFAULT 'DA_CHI_DINH',
    ketQua NVARCHAR(MAX),
    ketLuan NVARCHAR(MAX),
    danhSachHinhAnhUrl NVARCHAR(MAX),
    thoiGianChiDinh DATETIME2 NOT NULL DEFAULT GETDATE(),
    thoiGianThucHien DATETIME2 NULL,
    thoiGianTraKetQua DATETIME2 NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_ChiDinhDichVu_LuotKham FOREIGN KEY (maLuotKham) REFERENCES LuotKham(id),
    CONSTRAINT FK_ChiDinhDichVu_DanhMucDichVu FOREIGN KEY (maDichVu) REFERENCES DanhMucDichVu(id),
    CONSTRAINT FK_ChiDinhDichVu_BacSiChiDinh FOREIGN KEY (maBacSiChiDinh) REFERENCES NhanVien(id),
    CONSTRAINT FK_ChiDinhDichVu_PhongThucHien FOREIGN KEY (maPhongThucHien) REFERENCES PhongKham(id),
    CONSTRAINT FK_ChiDinhDichVu_BacSiThucHien FOREIGN KEY (bacSiThucHien) REFERENCES NhanVien(id)
);

-- 21. BẢNG CHI TIẾT KẾT QUẢ XÉT NGHIỆM (ChiTietKetQuaXetNghiem)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ChiTietKetQuaXetNghiem')
CREATE TABLE ChiTietKetQuaXetNghiem (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maChiDinhDichVu INT NOT NULL,
    tenChiSo NVARCHAR(100) NOT NULL,
    giaTri NVARCHAR(100) NOT NULL,
    donViDo NVARCHAR(50),
    khoangThamChieu NVARCHAR(100),
    danhGia NVARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_ChiTietKetQuaXetNghiem_ChiDinhDichVu FOREIGN KEY (maChiDinhDichVu) REFERENCES ChiDinhDichVu(id)
);

-- 22. BẢNG DANH MỤC THUỐC & VẬT TƯ Y TẾ (Item)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Item')
CREATE TABLE Item (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maItem VARCHAR(50) NOT NULL UNIQUE,
    tenItem NVARCHAR(255) NOT NULL,
    loaiItem VARCHAR(50) DEFAULT 'THUOC',
    hoatChat NVARCHAR(255),
    hamLuong NVARCHAR(100),
    donViCoBan NVARCHAR(50) NOT NULL,
    quyCachDongGoi NVARCHAR(255),
    giaNhapMacDinh DECIMAL(18,2) DEFAULT 0,
    giaBanMacDinh DECIMAL(18,2) DEFAULT 0,
    tonToiThieu DECIMAL(18,2) DEFAULT 0,
    tonToiDa DECIMAL(18,2) DEFAULT 1000000,
    dangBaoChe NVARCHAR(100),
    nuocSanXuat NVARCHAR(100),
    hangSanXuat NVARCHAR(255),
    soDangKy VARCHAR(50),
    hoatDong BIT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL
);

-- 23. BẢNG QUY ĐỔI ĐƠN VỊ TÍNH (DonViTinhQuyDoi)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DonViTinhQuyDoi')
CREATE TABLE DonViTinhQuyDoi (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maItem INT NOT NULL,
    tenDonVi NVARCHAR(50) NOT NULL,
    tyLeQuyDoi DECIMAL(18,4) NOT NULL DEFAULT 1,
    giaBan DECIMAL(18,2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_DonViTinhQuyDoi_Item FOREIGN KEY (maItem) REFERENCES Item(id)
);

-- 24. BẢNG DANH MỤC KHO DƯỢC (Kho)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Kho')
CREATE TABLE Kho (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maCoSo INT NOT NULL,
    tenKho NVARCHAR(255) NOT NULL,
    loaiKho VARCHAR(50) DEFAULT 'KHO_LE',
    trangThai VARCHAR(50) DEFAULT 'HOAT_DONG',
    maNguoiPhuTrach INT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_Kho_CoSo FOREIGN KEY (maCoSo) REFERENCES CoSo(id),
    CONSTRAINT FK_Kho_NguoiPhuTrach FOREIGN KEY (maNguoiPhuTrach) REFERENCES NhanVien(id)
);

-- 25. BẢNG TỒN KHO THEO LÔ VÀ HẠN DÙNG (TonKho)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'TonKho')
CREATE TABLE TonKho (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maKho INT NOT NULL,
    maItem INT NOT NULL,
    soLo VARCHAR(50) NOT NULL,
    hanSuDung DATE NOT NULL,
    soLuongTon DECIMAL(18,4) NOT NULL DEFAULT 0,
    giaNhap DECIMAL(18,2) NOT NULL DEFAULT 0,
    giaBan DECIMAL(18,2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_TonKho_Kho FOREIGN KEY (maKho) REFERENCES Kho(id),
    CONSTRAINT FK_TonKho_Item FOREIGN KEY (maItem) REFERENCES Item(id)
);

-- 26. BẢNG ĐỊNH MỨC VẬT TƯ TIÊU HAO THEO DỊCH VỤ (DinhMucVatTuDichVu)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DinhMucVatTuDichVu')
CREATE TABLE DinhMucVatTuDichVu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maDichVu INT NOT NULL,
    maItem INT NOT NULL,
    soLuongDinhMuc DECIMAL(18,4) NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_DinhMucVatTu_DichVu FOREIGN KEY (maDichVu) REFERENCES DanhMucDichVu(id),
    CONSTRAINT FK_DinhMucVatTu_Item FOREIGN KEY (maItem) REFERENCES Item(id)
);

-- 27. BẢNG ĐƠN THUỐC (DonThuoc)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'DonThuoc')
CREATE TABLE DonThuoc (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maLuotKham INT NOT NULL,
    maBacSi INT NOT NULL,
    maCoSo INT NOT NULL,
    maKhoXuat INT NULL,
    ngayKe DATETIME2 NOT NULL DEFAULT GETDATE(),
    trangThai VARCHAR(50) DEFAULT 'TAM',
    ghiChu NVARCHAR(500),
    loiDanDonThuoc NVARCHAR(MAX),
    ngayCapThuoc DATETIME2 NULL,
    maNguoiCapThuoc INT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_DonThuoc_LuotKham FOREIGN KEY (maLuotKham) REFERENCES LuotKham(id),
    CONSTRAINT FK_DonThuoc_BacSi FOREIGN KEY (maBacSi) REFERENCES NhanVien(id),
    CONSTRAINT FK_DonThuoc_CoSo FOREIGN KEY (maCoSo) REFERENCES CoSo(id),
    CONSTRAINT FK_DonThuoc_KhoXuat FOREIGN KEY (maKhoXuat) REFERENCES Kho(id),
    CONSTRAINT FK_DonThuoc_NguoiCapThuoc FOREIGN KEY (maNguoiCapThuoc) REFERENCES NhanVien(id)
);

-- 28. BẢNG CHI TIẾT ĐƠN THUỐC (ChiTietDonThuoc)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ChiTietDonThuoc')
CREATE TABLE ChiTietDonThuoc (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maDonThuoc INT NOT NULL,
    maItem INT NOT NULL,
    soLuongKe DECIMAL(18,2) NOT NULL DEFAULT 0,
    soLuongDaXuat DECIMAL(18,2) NOT NULL DEFAULT 0,
    lieuDung NVARCHAR(255),
    tanSuat NVARCHAR(255),
    soNgay INT,
    huongDan NVARCHAR(500),
    donGiaKe DECIMAL(18,2) NOT NULL DEFAULT 0,
    duongDung NVARCHAR(100) DEFAULT N'Uống',
    sang DECIMAL(18,2) DEFAULT 0,
    trua DECIMAL(18,2) DEFAULT 0,
    chieu DECIMAL(18,2) DEFAULT 0,
    toi DECIMAL(18,2) DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_ChiTietDonThuoc_DonThuoc FOREIGN KEY (maDonThuoc) REFERENCES DonThuoc(id),
    CONSTRAINT FK_ChiTietDonThuoc_Item FOREIGN KEY (maItem) REFERENCES Item(id)
);

-- 29. BẢNG NHÀ CUNG CẤP (NhaCungCap)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'NhaCungCap')
CREATE TABLE NhaCungCap (
    id INT IDENTITY(1,1) PRIMARY KEY,
    tenNhaCungCap NVARCHAR(255) NOT NULL,
    maSoThue VARCHAR(50),
    soDienThoai VARCHAR(20),
    email VARCHAR(100),
    diaChi NVARCHAR(500),
    trangThai VARCHAR(50) DEFAULT 'DANG_GIAO_DICH',
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL
);

-- 30. BẢNG PHIẾU NHẬP KHO (PhieuNhapKho)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PhieuNhapKho')
CREATE TABLE PhieuNhapKho (
    id INT IDENTITY(1,1) PRIMARY KEY,
    soPhieuNhap VARCHAR(50) NOT NULL UNIQUE,
    maKho INT NOT NULL,
    maNhaCungCap INT NOT NULL,
    ngayNhap DATETIME2 NOT NULL DEFAULT GETDATE(),
    tongTien DECIMAL(18,2) NOT NULL DEFAULT 0,
    trangThai VARCHAR(50) DEFAULT 'HOAN_THANH',
    ghiChu NVARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_PhieuNhapKho_Kho FOREIGN KEY (maKho) REFERENCES Kho(id),
    CONSTRAINT FK_PhieuNhapKho_NCC FOREIGN KEY (maNhaCungCap) REFERENCES NhaCungCap(id)
);

-- 31. BẢNG CHI TIẾT NHẬP KHO (ChiTietNhapKho)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ChiTietNhapKho')
CREATE TABLE ChiTietNhapKho (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maPhieuNhap INT NOT NULL,
    maItem INT NOT NULL,
    soLo VARCHAR(50) NOT NULL,
    hanSuDung DATE NOT NULL,
    soLuong DECIMAL(18,4) NOT NULL,
    giaNhap DECIMAL(18,2) NOT NULL,
    giaBan DECIMAL(18,2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_ChiTietNhapKho_PhieuNhap FOREIGN KEY (maPhieuNhap) REFERENCES PhieuNhapKho(id),
    CONSTRAINT FK_ChiTietNhapKho_Item FOREIGN KEY (maItem) REFERENCES Item(id)
);

-- 32. BẢNG PHIẾU XUẤT KHO (PhieuXuatKho)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PhieuXuatKho')
CREATE TABLE PhieuXuatKho (
    id INT IDENTITY(1,1) PRIMARY KEY,
    soPhieuXuat VARCHAR(50) NOT NULL UNIQUE,
    maKhoXuat INT NOT NULL,
    loaiXuat VARCHAR(50) DEFAULT 'XUAT_DON_THUOC',
    maDonThuoc INT NULL,
    maKhoNhan INT NULL,
    ngayXuat DATETIME2 NOT NULL DEFAULT GETDATE(),
    tongTien DECIMAL(18,2) NOT NULL DEFAULT 0,
    trangThaiNoiBo VARCHAR(50) DEFAULT 'DA_XUAT',
    ghiChu NVARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_PhieuXuatKho_KhoXuat FOREIGN KEY (maKhoXuat) REFERENCES Kho(id),
    CONSTRAINT FK_PhieuXuatKho_DonThuoc FOREIGN KEY (maDonThuoc) REFERENCES DonThuoc(id),
    CONSTRAINT FK_PhieuXuatKho_KhoNhan FOREIGN KEY (maKhoNhan) REFERENCES Kho(id)
);

-- 33. BẢNG CHI TIẾT XUẤT KHO (ChiTietXuatKho)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ChiTietXuatKho')
CREATE TABLE ChiTietXuatKho (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maPhieuXuat INT NOT NULL,
    maItem INT NOT NULL,
    maTonKho INT NOT NULL,
    soLuong DECIMAL(18,4) NOT NULL,
    donGia DECIMAL(18,2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_ChiTietXuatKho_PhieuXuat FOREIGN KEY (maPhieuXuat) REFERENCES PhieuXuatKho(id),
    CONSTRAINT FK_ChiTietXuatKho_Item FOREIGN KEY (maItem) REFERENCES Item(id),
    CONSTRAINT FK_ChiTietXuatKho_TonKho FOREIGN KEY (maTonKho) REFERENCES TonKho(id)
);

-- 34. BẢNG THẺ KHO / BIẾN ĐỘNG TỒN KHO (InventoryTransaction)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'InventoryTransaction')
CREATE TABLE InventoryTransaction (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maKho INT NOT NULL,
    maItem INT NOT NULL,
    maTonKho INT NOT NULL,
    loaiGiaoDich VARCHAR(50) NOT NULL,
    soLuongThayDoi DECIMAL(18,4) NOT NULL,
    tonTruocKhiThayDoi DECIMAL(18,4) NOT NULL,
    tonSauKhiThayDoi DECIMAL(18,4) NOT NULL,
    thoiGian DATETIME2 NOT NULL DEFAULT GETDATE(),
    maChungTu VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_InventoryTx_Kho FOREIGN KEY (maKho) REFERENCES Kho(id),
    CONSTRAINT FK_InventoryTx_Item FOREIGN KEY (maItem) REFERENCES Item(id),
    CONSTRAINT FK_InventoryTx_TonKho FOREIGN KEY (maTonKho) REFERENCES TonKho(id)
);

-- 35. BẢNG HÓA ĐƠN VIỆN PHÍ (HoaDon)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'HoaDon')
CREATE TABLE HoaDon (
    id INT IDENTITY(1,1) PRIMARY KEY,
    soHoaDon VARCHAR(50) NOT NULL UNIQUE,
    maLuotKham INT NOT NULL,
    maBenhNhan INT NOT NULL,
    maCoSo INT NOT NULL,
    ngayLap DATETIME2 NOT NULL DEFAULT GETDATE(),
    tongTien DECIMAL(18,2) NOT NULL DEFAULT 0,
    soTienBaoHiemChiTra DECIMAL(18,2) NOT NULL DEFAULT 0,
    soTienBenhNhanChiTra DECIMAL(18,2) NOT NULL DEFAULT 0,
    daThanhToan DECIMAL(18,2) NOT NULL DEFAULT 0,
    soTienTamUng DECIMAL(18,2) NOT NULL DEFAULT 0,
    soTienGiamGia DECIMAL(18,2) NOT NULL DEFAULT 0,
    chietKhauPhanTram DECIMAL(5,2) NOT NULL DEFAULT 0,
    lyDoGiamGia NVARCHAR(500),
    soTienHoanTra DECIMAL(18,2) NOT NULL DEFAULT 0,
    soHoaDonDienTu VARCHAR(50),
    maTraCuuHddt VARCHAR(100),
    trangThai VARCHAR(50) DEFAULT 'CHO_THANH_TOAN',
    ghiChu NVARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_HoaDon_LuotKham FOREIGN KEY (maLuotKham) REFERENCES LuotKham(id),
    CONSTRAINT FK_HoaDon_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(id),
    CONSTRAINT FK_HoaDon_CoSo FOREIGN KEY (maCoSo) REFERENCES CoSo(id)
);

-- 36. BẢNG CHI TIẾT HÓA ĐƠN (ChiTietHoaDon)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ChiTietHoaDon')
CREATE TABLE ChiTietHoaDon (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maHoaDon INT NOT NULL,
    loaiDoiTuong VARCHAR(50) NOT NULL,
    maDoiTuong INT NOT NULL,
    moTa NVARCHAR(255) NOT NULL,
    soLuong DECIMAL(18,2) NOT NULL,
    donGia DECIMAL(18,2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_ChiTietHoaDon_HoaDon FOREIGN KEY (maHoaDon) REFERENCES HoaDon(id)
);

-- 37. BẢNG PHIẾU THU TIỀN TỪNG LẦN (PhieuThu)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'PhieuThu')
CREATE TABLE PhieuThu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maHoaDon INT NOT NULL,
    soTien DECIMAL(18,2) NOT NULL,
    ngayThu DATETIME2 NOT NULL DEFAULT GETDATE(),
    phuongThuc VARCHAR(50) DEFAULT 'TIEN_MAT',
    nguoiThu INT NOT NULL,
    ghiChu NVARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_PhieuThu_HoaDon FOREIGN KEY (maHoaDon) REFERENCES HoaDon(id),
    CONSTRAINT FK_PhieuThu_NguoiThu FOREIGN KEY (nguoiThu) REFERENCES NhanVien(id)
);

-- 38. BẢNG CÔNG NỢ BỆNH NHÂN (CongNoBenhNhan)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'CongNoBenhNhan')
CREATE TABLE CongNoBenhNhan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maHoaDon INT NOT NULL,
    maBenhNhan INT NOT NULL,
    soTienNo DECIMAL(18,2) NOT NULL,
    soTienDaTra DECIMAL(18,2) NOT NULL DEFAULT 0,
    hanThanhToan DATE NOT NULL,
    trangThai VARCHAR(50) DEFAULT 'CON_NO',
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_CongNoBenhNhan_HoaDon FOREIGN KEY (maHoaDon) REFERENCES HoaDon(id),
    CONSTRAINT FK_CongNoBenhNhan_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(id)
);

-- 39. BẢNG CA TRỰC & LỊCH LÀM VIỆC (CaTruc, LichLamViec, ChamCong)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'LoaiCa')
CREATE TABLE LoaiCa (
    id INT IDENTITY(1,1) PRIMARY KEY,
    tenLoaiCa NVARCHAR(100) NOT NULL,
    moTa NVARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL
);

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'CaTruc')
CREATE TABLE CaTruc (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maLoaiCa INT NOT NULL,
    tenCa NVARCHAR(100) NOT NULL,
    gioBatDau TIME(0) NOT NULL,
    gioKetThuc TIME(0) NOT NULL,
    hoatDong BIT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_CaTruc_LoaiCa FOREIGN KEY (maLoaiCa) REFERENCES LoaiCa(id)
);

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'LichLamViec')
CREATE TABLE LichLamViec (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maNhanVien INT NOT NULL,
    maCaTruc INT NOT NULL,
    maCoSo INT NOT NULL,
    maKhoa INT NULL,
    maPhongKham INT NULL,
    ngayLamViec DATE NOT NULL,
    trangThai VARCHAR(50) DEFAULT 'CHUA_DIEM_DANH',
    nguonLich VARCHAR(50) DEFAULT 'HE_THONG',
    ghiChu NVARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_LichLamViec_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(id),
    CONSTRAINT FK_LichLamViec_CaTruc FOREIGN KEY (maCaTruc) REFERENCES CaTruc(id),
    CONSTRAINT FK_LichLamViec_CoSo FOREIGN KEY (maCoSo) REFERENCES CoSo(id),
    CONSTRAINT FK_LichLamViec_Khoa FOREIGN KEY (maKhoa) REFERENCES Khoa(id),
    CONSTRAINT FK_LichLamViec_PhongKham FOREIGN KEY (maPhongKham) REFERENCES PhongKham(id)
);

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ChamCong')
CREATE TABLE ChamCong (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maLichLamViec INT NOT NULL,
    thoiGianCheckIn DATETIME2 NULL,
    thoiGianCheckOut DATETIME2 NULL,
    trangThai VARCHAR(50) DEFAULT 'DUNG_GIO',
    soGioLam DECIMAL(5,2) DEFAULT 0,
    soGioDiMuon DECIMAL(5,2) DEFAULT 0,
    soGioVeSom DECIMAL(5,2) DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_ChamCong_LichLamViec FOREIGN KEY (maLichLamViec) REFERENCES LichLamViec(id)
);

-- 40. BẢNG TÍNH LƯƠNG (KyLuong, BangLuong, ChiTietBangLuong)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'KyLuong')
CREATE TABLE KyLuong (
    id INT IDENTITY(1,1) PRIMARY KEY,
    tenKy NVARCHAR(100) NOT NULL,
    thang INT NOT NULL,
    nam INT NOT NULL,
    tuNgay DATE NOT NULL,
    denNgay DATE NOT NULL,
    daChot BIT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL
);

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'BangLuong')
CREATE TABLE BangLuong (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maKyLuong INT NOT NULL,
    maNhanVien INT NOT NULL,
    luongCoBan DECIMAL(18,2) DEFAULT 0,
    heSoLuong DECIMAL(5,2) DEFAULT 1.0,
    tongPhuCap DECIMAL(18,2) DEFAULT 0,
    tongThuLaoKham DECIMAL(18,2) DEFAULT 0,
    tongKhauTru DECIMAL(18,2) DEFAULT 0,
    thucLinh DECIMAL(18,2) DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_BangLuong_KyLuong FOREIGN KEY (maKyLuong) REFERENCES KyLuong(id),
    CONSTRAINT FK_BangLuong_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(id)
);

-- 41. BẢNG MA TRẬN PHÂN QUYỀN (MaTranPhanQuyen)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'MaTranPhanQuyen')
CREATE TABLE MaTranPhanQuyen (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maVaiTro INT NOT NULL,
    phanHe VARCHAR(50) NOT NULL,
    hanhDong VARCHAR(50) NOT NULL,
    phamViDuLieu VARCHAR(50) DEFAULT 'TOAN_HE_THONG',
    duocPhep BIT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_MaTranPhanQuyen_VaiTro FOREIGN KEY (maVaiTro) REFERENCES VaiTro(id)
);

-- 42. BẢNG NHẬT KÝ HỆ THỐNG & TRUY VẾT BỆNH ÁN (NhatKyHeThong, NhatKyHoSoBenhAn)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'NhatKyHeThong')
CREATE TABLE NhatKyHeThong (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maNhanVien INT NULL,
    hanhDong NVARCHAR(255) NOT NULL,
    chiTiet NVARCHAR(MAX),
    ipAddress VARCHAR(50),
    thoiGian DATETIME2 NOT NULL DEFAULT GETDATE(),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_NhatKyHeThong_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(id)
);

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'NhatKyHoSoBenhAn')
CREATE TABLE NhatKyHoSoBenhAn (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maLuotKham INT NOT NULL,
    maNhanVien INT NOT NULL,
    hanhDong NVARCHAR(255) NOT NULL,
    lyDo NVARCHAR(500),
    thoiGian DATETIME2 NOT NULL DEFAULT GETDATE(),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL,
    CONSTRAINT FK_NhatKyEMR_LuotKham FOREIGN KEY (maLuotKham) REFERENCES LuotKham(id),
    CONSTRAINT FK_NhatKyEMR_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(id)
);

-- 43. BẢNG THAM SỐ HỆ THỐNG (ThamSoHeThong)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ThamSoHeThong')
CREATE TABLE ThamSoHeThong (
    id INT IDENTITY(1,1) PRIMARY KEY,
    maThamSo VARCHAR(100) NOT NULL UNIQUE,
    tenThamSo NVARCHAR(255) NOT NULL,
    giaTri NVARCHAR(MAX),
    moTa NVARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    createdAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    createdBy INT NULL,
    updatedAt DATETIME2 NULL,
    updatedBy INT NULL,
    isDeleted BIT NOT NULL DEFAULT 0,
    deletedAt DATETIME2 NULL,
    deletedBy INT NULL
);

-- ==============================================================================
-- INDEX TỐI ƯU HÓA TRUY VẤN
-- ==============================================================================
CREATE NONCLUSTERED INDEX IX_BenhNhan_CCCD ON BenhNhan(soCCCD) WHERE isDeleted = 0;
CREATE NONCLUSTERED INDEX IX_BenhNhan_Phone ON BenhNhan(dienThoai) WHERE isDeleted = 0;
CREATE NONCLUSTERED INDEX IX_LuotKham_NgayKham ON LuotKham(ngayKham, maPhongKham, trangThai) WHERE isDeleted = 0;
CREATE NONCLUSTERED INDEX IX_TonKho_Fefo ON TonKho(maKho, maItem, hanSuDung ASC) WHERE isDeleted = 0 AND soLuongTon > 0;
CREATE NONCLUSTERED INDEX IX_HoaDon_NgayLap ON HoaDon(ngayLap, trangThai) WHERE isDeleted = 0;
