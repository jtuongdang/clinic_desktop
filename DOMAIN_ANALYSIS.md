# BÁO CÁO PHÂN TÍCH TOÀN DIỆN PACKAGE CLINIC-DOMAIN & ĐÁNH GIÁ MỨC ĐỘ ĐÁP ỨNG CHO ỨNG DỤNG DESKTOP (EHC HIS)

> **Tài liệu phân tích kỹ thuật kiến trúc miền (Domain Architecture Analysis)**  
> **Dự án:** Clinic Management Desktop (EHC HIS - Electronic Health Record & Hospital Information System)  
> **Mục tiêu:** 
> 1. Phân tích cấu trúc package `vn.clinicmanagement.domain`, vai trò, chức năng và các trường dữ liệu của từng package con và từng file Java.
> 2. Đánh giá đối soát chuyên sâu: Các trường dữ liệu hiện tại trong Domain đã đủ để phục vụ toàn bộ các màn hình giao diện Desktop (theo chuẩn `UI_CODING_STANDARDS.md` và nghiệp vụ bệnh viện thực tế) hay chưa?

---

## MỤC LỤC
1. [TỔNG QUAN KIẾN TRÚC PACKAGE DOMAIN](#1-tổng-quan-kiến-trúc-package-domain)
   - 1.1. [Mô hình thiết kế kiến trúc DDD](#11-mô-hình-thiết-kế-kiến-trúc-ddd)
   - 1.2. [Thống kê phân bố các thành phần](#12-thống-kê-phân-bố-các-thành-phần)
2. [PHÂN TÍCH CHI TIẾT TỪNG PACKAGE CON VÀ CÁC FILE THÀNH PHẦN](#2-phân-tích-chi-tiết-từng-package-con-và-các-file-thành-phần)
   - 2.1. [`domain.common` - Hạ tầng cốt lõi DDD](#21-domaincommon---hạ-tầng-cốt-lõi-ddd)
   - 2.2. [`domain.common.valueobjects` - Các đối tượng giá trị bất biến](#22-domaincommonvalueobjects---các-đối-tượng-giá-trị-bất-biến)
   - 2.3. [`domain.common.events` - Hệ thống sự kiện miền (Domain Events)](#23-domaincommonevents---hệ-thống-sự-kiện-miền-domain-events)
   - 2.4. [`domain.common.exceptions` - Hệ thống ngoại lệ miền](#24-domaincommonexceptions---hệ-thống-ngoại-lệ-miền)
   - 2.5. [`domain.common.enums` - Hệ thống danh mục hằng số & trạng thái](#25-domaincommonenums---hệ-thống-danh-mục-hằng-số--trạng-thái)
   - 2.6. [`domain.patient` - Phân hệ Hồ sơ Bệnh nhân](#26-domainpatient---phân-hệ-hồ-sơ-bệnh-nhân)
   - 2.7. [`domain.examination` - Phân hệ Khám bệnh & Hồ sơ EMR](#27-domainexamination---phân-hệ-khám-bệnh--hồ-sơ-emr)
   - 2.8. [`domain.service` - Phân hệ Cận lâm sàng & Danh mục Dịch vụ](#28-domainservice---phân-hệ-cận-lâm-sàng--danh-mục-dịch-vụ)
   - 2.9. [`domain.prescription` - Phân hệ Kê đơn & An toàn Dược lâm sàng](#29-domainprescription---phân-hệ-kê-đơn--an-toàn-dược-lâm-sàng)
   - 2.10. [`domain.inventory` - Phân hệ Quản trị Kho Dược & Vật tư Y tế](#210-domaininventory---phân-hệ-quản-trị-kho-dược--vật-tư-y-tế)
   - 2.11. [`domain.billing` - Phân hệ Viện phí & Quản lý Hóa đơn](#211-domainbilling---phân-hệ-viện-phí--quản-lý-hóa-đơn)
   - 2.12. [`domain.appointment` - Phân hệ Lịch hẹn & Tiếp đón Đặt trước](#212-domainappointment---phân-hệ-lịch-hẹn--tiếp-đón-đặt-trước)
   - 2.13. [`domain.facility` - Phân hệ Cơ sở & Hạ tầng Buồng/Khoa](#213-domainfacility---phân-hệ-cơ-sở--hạ-tầng-buồngkhoa)
   - 2.14. [`domain.identity` - Phân hệ Nhân sự, Bác sĩ & Chấm công](#214-domainidentity---phân-hệ-nhân-sự-bác-sĩ--chấm-công)
   - 2.15. [`domain.payroll` - Phân hệ Tính lương & Thù lao Bác sĩ](#215-domainpayroll---phân-hệ-tính-lương--thù-lao-bác-sĩ)
   - 2.16. [`domain.system` - Phân hệ Cấu hình & Giám sát Hệ thống](#216-domainsystem---phân-hệ-cấu-hình--giám-sát-hệ-thống)
   - 2.17. [`domain.repositories` - Khế ước Lưu trữ Dữ liệu (Repository Interfaces)](#217-domainrepositories---khế-ước-lưu-trữ-dữ-liệu-repository-interfaces)
3. [ĐÁNH GIÁ CHUYÊN SÂU: CÁC TRƯỜNG DỮ LIỆU ĐÃ ĐỦ CHO DESKTOP CHƯA?](#3-đánh-giá-chuyên-sâu-các-trường-dữ-liệu-đã-đủ-cho-desktop-chưa)
   - 3.1. [Kết luận tổng quan](#31-kết-luận-tổng-quan)
   - 3.2. [Bảng đối soát chi tiết từng Màn hình Desktop (Desktop Forms vs Domain Entities)](#32-bảng-đối-soát-chi-tiết-từng-màn-hình-desktop-desktop-forms-vs-domain-entities)
   - 3.3. [Danh mục các trường dữ liệu BẮT BUỘC cần bổ sung ngay](#33-danh-mục-các-trường-dữ-liệu-bắt-buộc-cần-bổ-sung-ngay)
4. [ĐỀ XUẤT LỘ TRÌNH NÂNG CẤP DOMAIN ĐÁP ỨNG THỰC CHIẾN DESKTOP](#4-đề-xuất-lộ-trình-nâng-cấp-domain-đáp-ứng-thực-chiến-desktop)

---

## 1. TỔNG QUAN KIẾN TRÚC PACKAGE DOMAIN

### 1.1. Mô hình thiết kế kiến trúc DDD
Package `vn.clinicmanagement.domain` đóng vai trò là **Trái tim cốt lõi (Core Domain Layer)** trong mô hình Kiến trúc Củ hành / Kiến trúc Sạch (Clean / Onion Architecture):
* **Tính độc lập cao nhất (Zero External Dependencies):** Package không phụ thuộc vào bất kỳ thư viện UI (Java Swing, FlatLaf), không phụ thuộc vào cơ sở dữ liệu (JDBC, Hibernate, MyBatis), và không phụ thuộc vào Web Framework. 
* **Tập trung vào Nghiệp vụ Y tế thuần túy:** Toàn bộ thực thể (Entities), Aggregate Roots, Quy tắc miền (Invariants), Sự kiện miền (Domain Events) và Dịch vụ nghiệp vụ (Domain Services) được đóng gói cô lập.
* **Định nghĩa khế ước (Contracts):** Định nghĩa các Repository Interfaces (`IRepository`, `IBenhNhanRepository`,...) để tầng Infrastructure triển khai hiện thực hóa việc đọc/ghi dữ liệu.

```
       +-------------------------------------------------------+
       |               DESKTOP UI (Java Swing / MDI)           |
       +-------------------------------------------------------+
                                  |
                                  v
       +-------------------------------------------------------+
       |               APPLICATION SERVICE LAYER               |
       +-------------------------------------------------------+
                                  |
                                  v
       +-------------------------------------------------------+
       |                  CLINIC DOMAIN LAYER                  |
       |  - Aggregate Roots (BenhNhan, LuotKham, DonThuoc...)  |
       |  - Domain Services (FefoAllocation, SafetyCheck...)   |
       |  - Domain Events (EmrLockedEvent, Dispensed...)      |
       |  - Repository Interfaces & Specifications             |
       +-------------------------------------------------------+
                                  ^
                                  |
       +-------------------------------------------------------+
       |                 INFRASTRUCTURE LAYER                  |
       |  (Spring Data / JDBC / SQL Server / REST Integration) |
       +-------------------------------------------------------+
```

### 1.2. Thống kê phân bố các thành phần
* **Tổng số package con:** 17 package
* **Tổng số file mã nguồn Java:** 126 files
* **Phân bố các thành phần:**
  * **Aggregate Roots & Entities:** 38 thực thể
  * **Enums (Định nghĩa trạng thái, danh mục):** 31 enums
  * **Domain Events & Event Bus Interfaces:** 14 files
  * **Domain Exceptions:** 5 files
  * **Value Objects:** 3 files
  * **Domain Services:** 2 services (`FefoAllocationService`, `ClinicalDrugSafetyService`)
  * **Repository & Query Contracts:** 11 files
  * **Các thực thể quan hệ & Bảng nối (Junction/Bridge Entities):** 22 files

---

## 2. PHÂN TÍCH CHI TIẾT TỪNG PACKAGE CON VÀ CÁC FILE THÀNH PHẦN

### 2.1. `domain.common` - Hạ tầng cốt lõi DDD
Package định nghĩa các lớp nền tảng cho toàn bộ thực thể trong hệ thống.

| Tên File | Loại | Vai trò & Chức năng | Các trường dữ liệu & Phương thức cốt lõi |
| :--- | :--- | :--- | :--- |
| **`BaseEntity.java`** | Abstract Class | Lớp cha cơ sở của toàn bộ Entity trong hệ thống. Cung cấp cơ chế quản trị khóa chính Generic (`TId`), Audit Trail, Soft Delete, Optimistic Locking và Domain Event Registration. | **Thuộc tính:**<br>- `id: TId`<br>- `version: Long` (Khóa lạc quan phòng chống ghi đè dữ liệu đồng thời)<br>- `createdAt, createdBy: LocalDateTime, Integer`<br>- `updatedAt, updatedBy: LocalDateTime, Integer`<br>- `isDeleted, deletedAt, deletedBy: boolean, LocalDateTime, Integer`<br>- `domainEvents: transient List<IDomainEvent>`<br>**Phương thức:** `markAsUpdated()`, `softDelete()`, `restore()`, `registerDomainEvent()`, `clearDomainEvents()`. |
| **`IAggregateRoot.java`** | Interface | Marker interface dùng để đánh dấu một Entity là Aggregate Root. Chỉ Aggregate Root mới có Repository tương ứng và chịu trách nhiệm bảo toàn tính toàn vẹn nghiệp vụ cho cả nhóm thực thể con. | Không có phương thức. |
| **`IUnitOfWork.java`** | Interface | Định nghĩa giao tác Unit of Work để đảm bảo tính toàn vẹn (Atomic Transaction) khi ghi nhiều entity vào cơ sở dữ liệu cùng lúc. | **Phương thức:**<br>- `int saveChanges()`<br>- `boolean saveEntities()` |
| **`TrangThaiContract.java`** | Class | Lưu trữ các hằng số trạng thái chuẩn chuỗi (`HOAT_DONG`, `NGUNG_HOAT_DONG`, `TAM_NGUNG`). | Các hằng số `public static final String`. |

---

### 2.2. `domain.common.valueobjects` - Các đối tượng giá trị bất biến (Value Objects)
Đặc trưng: Bất biến (Immutable), không có danh tính riêng (không có Id), định danh bằng toàn bộ giá trị các trường.

| Tên File | Loại | Vai trò & Chức năng | Các trường dữ liệu & Phương thức |
| :--- | :--- | :--- | :--- |
| **`DateRange.java`** | Final Class | Biểu diễn một khoảng thời gian (từ ngày đến ngày). Tự động validate ngày bắt đầu không được lớn hơn ngày kết thúc. Hỗ trợ khoảng thời gian mở. | **Thuộc tính:**<br>- `startDate: LocalDate`<br>- `endDate: LocalDate`<br>**Phương thức:** `contains(LocalDate date)`, `overlaps(DateRange other)`. |
| **`Money.java`** | Final Class | Đại diện cho tiền tệ chính xác (Mặc định: VND). Ngăn ngừa sai số dấu phẩy động (Floating point error) thường gặp với `float`/`double`. Hỗ trợ làm tròn tiền tệ. | **Thuộc tính:**<br>- `amount: BigDecimal` (Scale 2, RoundingMode.HALF_UP)<br>- `currency: String` ("VND")<br>**Phương thức:** `add()`, `subtract()`, `multiply()`, `isGreaterThan()`, `of()`, `isPositive()`. |
| **`PhoneNumber.java`** | Final Class | Đại diện cho số điện thoại hợp lệ tại Việt Nam. Tự động chuẩn hóa (loại bỏ dấu chấm, khoảng trắng, gạch ngang) và kiểm tra định dạng Regex 10 số (03x, 05x, 07x, 08x, 09x). | **Thuộc tính:**<br>- `value: String`<br>**Phương thức:** `getValue()`, tự động ném ngoại lệ nếu sai định dạng. |

---

### 2.3. `domain.common.events` - Hệ thống sự kiện miền (Domain Events)
Triển khai mẫu Event-Driven Architecture giúp phân tách các luồng xử lý (Decoupling) và hỗ trợ kiểm toán (Audit).

| Tên File | Chức năng nghiệp vụ | Dữ liệu mang theo (Event Payload) |
| :--- | :--- | :--- |
| **`IDomainEvent.java`** | Interface gốc của mọi Domain Event. | `getEventId(): UUID`, `getOccurredOn(): LocalDateTime`. |
| **`AbstractDomainEvent.java`** | Lớp trừu tượng triển khai sẵn Id tự sinh ngẫu nhiên và thời điểm phát sinh. | `eventId`, `occurredOn`. |
| **`IDomainEventPublisher.java`** | Khế ước dịch vụ phát hành sự kiện. | `publish(IDomainEvent event)`. |
| **`IDomainEventHandler.java`** | Interface cho các lớp lắng nghe và xử lý sự kiện. | `handle(T event)`. |
| **`BenhNhanRegisteredEvent.java`** | Phát sinh khi một bệnh nhân mới đăng ký hồ sơ vào hệ thống. | `patientId`, `cccd`, `fullName`. |
| **`LuotKhamStartedEvent.java`** | Phát sinh khi Bác sĩ bắt đầu ca khám tại phòng khám. | `visitId`, `patientId`, `doctorId`. |
| **`LuotKhamCompletedEvent.java`** | Phát sinh khi Bác sĩ hoàn thành phiên khám và chỉ định xử trí. | `visitId`, `patientId`, `doctorId`. |
| **`EmrLockedEvent.java`** | Phát sinh khi Bác sĩ thực hiện Ký số EMR, khóa vĩnh viễn hồ sơ bệnh án. | `visitId`, `doctorId`, `signature` (Chữ ký điện tử). |
| **`DonThuocApprovedEvent.java`** | Phát sinh khi Bác sĩ duyệt hoàn tất đơn thuốc cho người bệnh. | `prescriptionId`, `visitId`, `doctorId`. |
| **`DonThuocDispensedEvent.java`** | Phát sinh khi Dược sĩ tại quầy thuốc xuất cấp thuốc thực tế cho bệnh nhân. | `prescriptionId`, `dispenserId`. |
| **`HoaDonCreatedEvent.java`** | Phát sinh khi tạo mới một hóa đơn viện phí từ lượt khám. | `invoiceId`, `visitId`, `totalAmount`. |
| **`PaymentRecordedEvent.java`** | Phát sinh khi Thu ngân nhận tiền thanh toán (Tiền mặt, QR, Thẻ). | `invoiceId`, `amountPaid`, `paymentMethod`. |
| **`LichHenBookedEvent.java`** | Phát sinh khi một lịch hẹn khám được tạo thành công trên hệ thống. | `appointmentId`, `patientId`, `doctorId`, `startTime`. |
| **`LowStockWarningEvent.java`** | Phát sinh khi tồn kho của một mặt hàng thuốc/vật tư giảm xuống dưới mức tồn tối thiểu. | `itemId`, `itemName`, `currentStock`, `minStock`. |

---

### 2.4. `domain.common.exceptions` - Hệ thống ngoại lệ miền
Chứa các ngoại lệ định danh chuẩn (Business Domain Exceptions), giúp tầng UI Desktop hiển thị thông báo lỗi thân thiện thay vì văng crash.

| Tên File | Ý nghĩa & Tình huống kích hoạt |
| :--- | :--- |
| **`DomainException.java`** | Ngoại lệ gốc (kế thừa `RuntimeException`) của toàn bộ lỗi nghiệp vụ miền. |
| **`BusinessRuleViolationException.java`** | Ném ra khi vi phạm bất biến nghiệp vụ (Invariants), ví dụ: Hủy lịch hẹn đã qua, nhập số lượng âm, v.v. |
| **`EntityNotFoundException.java`** | Ném ra khi không tìm thấy thực thể theo ID chỉ định trong cơ sở dữ liệu. |
| **`EmrLockedException.java`** | Ném ra khi người dùng cố gắng chỉnh sửa, thêm dịch vụ, sửa đơn thuốc của Lượt khám đã Ký số và Khóa bệnh án điện tử. |
| **`InsufficientStockException.java`** | Ném ra khi kho dược không đủ số lượng thuốc thuộc lô yêu cầu để cấp phát. |

---

### 2.5. `domain.common.enums` - Hệ thống danh mục hằng số & trạng thái
Bao gồm 31 Enums định nghĩa chuẩn hóa toàn bộ giá trị phân loại trong cơ sở dữ liệu và giao diện người dùng. Mỗi enum đều có `value (int)` và `description (String)` tiếng Việt chuẩn y tế:
1. `GioiTinh`: M (Nam), F (Nữ), O (Khác).
2. `HanhDongQuyen`: XEM (1), THEM (2), SUA (3), XOA (4), DUYET (5), IN (6), XUAT_FILE (7).
3. `HinhThucDieuTri`: NGOAI_TRU, NOI_TRU, DIEU_TRI_BAN_NGAY, CAP_CUU.
4. `LoaiBaoHiem`: BHYT, DICH_VU, CONG_DOAN, QUAN_DOI.
5. `LoaiCoSo`: PHONG_KHAM, BENH_VIEN, NHA_THUOC, TRUNG_TAM_XET_NGHIEM.
6. `LoaiDichVu`: KHAM_BENH, XET_NGHIEM, CDHA, THU_THUAT, PHAU_THUAT, GIUONG_BENH.
7. `LoaiGiaoDichKho`: NHAP_NHA_CUNG_CAP, XUAT_DON_THUOC, XUAT_DIEU_CHUYEN, NHAP_DIEU_CHUYEN, XUAT_HUY, CAN_BANG_KIEM_KE.
8. `LoaiItem`: THUOC, VAT_TU_Y_TE, HOA_CHAT, SINH_PHAM.
9. `LoaiKho`: KHO_TONG, KHO_CHAN, KHO_LE, KHO_QUAY_THUOC, TU_TRUC_CAP_CUU.
10. `LoaiNghiPhep`: NGHI_PHEP_NAM, NGHI_OM, NGHI_THAI_SAN, NGHI_KHONG_LUONG.
11. `LoaiTienSu`: DI_UNG_THUOC, DI_UNG_THUC_PHAM, BENH_MAN_TINH, TIEN_SU_PHAU_THUAT, TIEN_SU_GIA_DINH.
12. `LoaiXuatKho`: XUAT_DON_THUOC, XUAT_NOI_BO, XUAT_TRA_NCC, XUAT_HUY.
13. `NguonLich`: TRUC_TIEP, TONG_DAI, WEBSITE, APP_MOBILE.
14. `PhamViDuLieu`: TOAN_HE_THONG, THEO_CO_SO, THEO_KHOA, CA_NHAN.
15. `PhuongThucThanhToan`: TIEN_MAT, CHUYEN_KHOAN_QR, THE_POS, VNPAY, MOMO, VI_DIEN_TU.
16. Các Enums Trạng Thái: `TrangThaiChamCong`, `TrangThaiChiDinhDichVu`, `TrangThaiCongNo`, `TrangThaiDieuChinhChamCong`, `TrangThaiDonDatHang`, `TrangThaiDonThuoc`, `TrangThaiHoaDon`, `TrangThaiKho`, `TrangThaiKiemKe`, `TrangThaiLichHen`, `TrangThaiLichLamViec`, `TrangThaiLuotKham`, `TrangThaiNghiPhep`, `TrangThaiNhaCungCap`, `TrangThaiPhieuNhap`, `TrangThaiXuatNoiBo`.

---

### 2.6. `domain.patient` - Phân hệ Hồ sơ Bệnh nhân

| Tên File | Loại | Chức năng nghiệp vụ | Danh sách các trường dữ liệu |
| :--- | :--- | :--- | :--- |
| **`BenhNhan.java`** | Aggregate Root | Quản lý định danh toàn diện hồ sơ bệnh nhân, thông tin liên lạc khẩn cấp và các danh sách lịch sử y tế. | **Thuộc tính:**<br>- `ho, ten: String`<br>- `ngaySinh: LocalDate`<br>- `gioiTinh: GioiTinh`<br>- `dienThoai: String`<br>- `email: String`<br>- `soCCCD: String`<br>- `diaChi: String`<br>- `nguoiLienHeTen, nguoiLienHeDienThoai: String`<br>- `username, passwordHash: String`<br>- *Quan hệ:* `benhNhanCoSo`, `baoHiems`, `tienSuBenhs`, `luotKhams`, `hoaDons`.<br>**Phương thức:** `getHoTen()`. Phát hành `BenhNhanRegisteredEvent`. |
| **`BaoHiem.java`** | Entity | Lưu trữ thông tin thẻ bảo hiểm y tế (BHYT) của người bệnh. | **Thuộc tính:**<br>- `maBenhNhan: int`<br>- `loaiBaoHiem: LoaiBaoHiem`<br>- `soBaoHiem: String` (Mã thẻ BHYT 15 ký tự)<br>- `ngayHetHan: LocalDate`<br>- `mucHuong: int` (Tỷ lệ hưởng 80%, 95%, 100%)<br>- `noiDangKyKCB: String` (Mã nơi ĐKKCB ban đầu). |
| **`TienSuBenh.java`** | Entity | Ghi nhận tiền sử dị ứng, tiền sử phẫu thuật, bệnh lý mạn tính. | **Thuộc tính:**<br>- `maBenhNhan: int`<br>- `loaiTienSu: LoaiTienSu`<br>- `moTa: String`. |
| **`BenhNhanCoSo.java`** | Junction Entity | Quản lý mối liên kết giữa bệnh nhân và cơ sở phòng khám nơi đăng ký khám lần đầu. | **Thuộc tính:**<br>- `maBenhNhan: int`<br>- `maCoSo: int`<br>- `ngayDangKy: LocalDateTime`. |

---

### 2.7. `domain.examination` - Phân hệ Khám bệnh & Hồ sơ EMR

| Tên File | Loại | Chức năng nghiệp vụ | Danh sách các trường dữ liệu |
| :--- | :--- | :--- | :--- |
| **`LuotKham.java`** | Aggregate Root | Trung tâm điều phối toàn bộ một phiên khám bệnh của bệnh nhân từ khi tiếp nhận đến khi kết thúc; quản lý khóa hồ sơ bệnh án điện tử (EMR Lock). | **Thuộc tính:**<br>- `maBenhNhan: int`<br>- `maBacSi: Integer`<br>- `maLichHen: Integer`<br>- `maCoSo: int`<br>- `maKhoa: int`<br>- `maPhongKham: Integer`<br>- `hinhThucDieuTri: HinhThucDieuTri`<br>- `trangThai: TrangThaiLuotKham`<br>- `soThuTu: Integer`<br>- `ngayKham: LocalDate`<br>- `thoiGianBatDauKham, thoiGianKetThucKham: LocalDateTime`<br>- `lyDoKham: String`<br>- `emrLocked: boolean`<br>- `emrSignature: String`<br>- `emrLockedAt: LocalDateTime`<br>- `emrLockedBy: Integer`<br>- *Quan hệ:* `chanDoans`, `sinhHieus`, `chiDinhDichVus`, `donThuocs`, `hoaDons`.<br>**Phương thức:** `startExam()`, `completeExam()`, `lockEmr(signature, doctorId)`. |
| **`ChanDoan.java`** | Entity | Ghi nhận chẩn đoán bệnh của bác sĩ cho lượt khám, liên kết mã bệnh quốc tế ICD-10. | **Thuộc tính:**<br>- `maLuotKham: int`<br>- `maICD: Integer`<br>- `chanDoanText: String`<br>- `laChinh: boolean` (Chẩn đoán bệnh chính hay bệnh phụ kèm theo). |
| **`ICD.java`** | Entity / Catalog | Danh mục tra cứu mã bệnh tật quốc tế ICD-10. | **Thuộc tính:**<br>- `maICD: String` (Ví dụ: "I10", "E11")<br>- `tenBenh: String`<br>- `moTa: String`<br>- `hoatDong: boolean`. |
| **`SinhHieu.java`** | Entity | Chỉ số sinh tồn của bệnh nhân đo đạc tại bàn tiếp đón hoặc phòng điều dưỡng. | **Thuộc tính:**<br>- `maLuotKham: int`<br>- `mach: Integer` (lần/phút)<br>- `nhietDo: BigDecimal` (°C)<br>- `huyetApTamThu, huyetApTamTruong: Integer` (mmHg)<br>- `nhipTho: Integer` (lần/phút)<br>- `canNang, chieuCao: BigDecimal` (kg, cm)<br>- `bmi: BigDecimal`<br>- `spo2: BigDecimal` (%). |

---

### 2.8. `domain.service` - Phân hệ Cận lâm sàng & Danh mục Dịch vụ

| Tên File | Loại | Chức năng nghiệp vụ | Danh sách các trường dữ liệu |
| :--- | :--- | :--- | :--- |
| **`DanhMucDichVu.java`** | Aggregate Root | Bảng giá và danh mục dịch vụ kỹ thuật, xét nghiệm, chẩn đoán hình ảnh, thủ thuật. | **Thuộc tính:**<br>- `maDichVu: String`<br>- `tenDichVu: String`<br>- `loaiDichVu: LoaiDichVu`<br>- `donGia, giaBHYT: BigDecimal`<br>- `hoatDong: boolean`<br>- `moTa: String`. |
| **`ChiDinhDichVu.java`** | Entity | Phiếu chỉ định một dịch vụ kỹ thuật hoặc xét nghiệm cụ thể trong lượt khám. | **Thuộc tính:**<br>- `maLuotKham: int`<br>- `maDichVu: int`<br>- `maBacSiChiDinh: int`<br>- `soLuong, donGia: BigDecimal`<br>- `trangThai: TrangThaiChiDinhDichVu`<br>- `ketQua: String` (Mô tả chi tiết kết quả CLS)<br>- `ketLuan: String` (Kết luận chuyên môn của Bác sĩ CĐHA/Xét nghiệm)<br>- `thoiGianChiDinh, thoiGianThucHien: LocalDateTime`. |
| **`ChiTietKetQuaXetNghiem.java`** | Entity | Chi tiết từng chỉ số thành phần trong một gói xét nghiệm máu, sinh hóa, nước tiểu. | **Thuộc tính:**<br>- `maChiDinhDichVu: int`<br>- `tenChiSo: String` (Ví dụ: "Glucose", "WBC", "RBC")<br>- `giaTri: String`<br>- `donViDo: String` (Ví dụ: "mmol/L")<br>- `khoangThamChieu: String` (Ví dụ: "3.9 - 6.4")<br>- `danhGia: String` (Bình thường / Cao / Thấp / Nguy hiểm). |
| **`DinhMucVatTuDichVu.java`** | Entity | Định mức tiêu hao vật tư y tế đi kèm theo dịch vụ (Ví dụ: 1 ca tiêm cần 1 bơm tiêm, 1 bông cồn). | **Thuộc tính:**<br>- `maDichVu: int`<br>- `maItem: int`<br>- `soLuongDinhMuc: BigDecimal`. |

---

### 2.9. `domain.prescription` - Phân hệ Kê đơn & An toàn Dược lâm sàng

| Tên File | Loại | Chức năng nghiệp vụ | Danh sách các trường dữ liệu & Phương thức |
| :--- | :--- | :--- | :--- |
| **`DonThuoc.java`** | Aggregate Root | Đại diện cho một đơn thuốc của lượt khám. Quản lý trạng thái phê duyệt của bác sĩ và cấp phát của dược sĩ. | **Thuộc tính:**<br>- `maLuotKham: int`<br>- `maBacSi: int`<br>- `maCoSo: int`<br>- `ngayKe: LocalDateTime`<br>- `trangThai: TrangThaiDonThuoc`<br>- `ghiChu: String`<br>- `ngayCapThuoc: LocalDateTime`<br>- `maNguoiCapThuoc: Integer`<br>- `chiTietDonThuocs: List<ChiTietDonThuoc>`.<br>**Phương thức:** `approve()`, `completeDispense(maNguoiCapThuoc)`. Phát hành `DonThuocApprovedEvent`, `DonThuocDispensedEvent`. |
| **`ChiTietDonThuoc.java`** | Entity | Từng dòng thuốc được kê trong đơn thuốc. | **Thuộc tính:**<br>- `maDonThuoc: int`<br>- `maItem: int`<br>- `soLuongKe, soLuongDaXuat: BigDecimal`<br>- `lieuDung: String` (Ví dụ: "1 viên/lần")<br>- `tanSuat: String` (Ví dụ: "2 lần/ngày")<br>- `soNgay: Integer`<br>- `huongDan: String` (Ví dụ: "Uống sau ăn no")<br>- `donGiaKe: BigDecimal`. |
| **`ClinicalDrugSafetyService.java`** | Domain Service | Dịch vụ kiểm tra an toàn sử dụng thuốc: đối chiếu tiền sử dị ứng của bệnh nhân với hoạt chất của các thuốc kê đơn, phát hiện trùng hoạt chất hoặc tương tác thuốc. | **Record:** `SafetyWarning(itemCode, itemName, warningMessage, isCritical)`.<br>**Phương thức:** `evaluatePrescriptionSafety(BenhNhan, List<Item>)`. |

---

### 2.10. `domain.inventory` - Phân hệ Quản trị Kho Dược & Vật tư Y tế

| Tên File | Loại | Chức năng nghiệp vụ | Danh sách các trường dữ liệu |
| :--- | :--- | :--- | :--- |
| **`Item.java`** | Aggregate Root | Quản lý danh mục thuốc, hóa chất, vật tư y tế, định mức tồn an toàn và đơn vị chuyển đổi. | **Thuộc tính:**<br>- `maItem: String`<br>- `tenItem: String`<br>- `loaiItem: LoaiItem`<br>- `hoatChat, hamLuong, donViCoBan, quyCachDongGoi: String`<br>- `giaNhapMacDinh, giaBanMacDinh: BigDecimal`<br>- `tonToiThieu, tonToiDa: BigDecimal`<br>- `hoatDong: boolean`<br>- *Quan hệ:* `donViTinhQuyDois`, `tonKhos`. |
| **`Kho.java`** | Aggregate Root | Quản lý danh mục kho vật lý (Kho chẵn, Kho ngoại trú, Tủ trực cấp cứu). | **Thuộc tính:**<br>- `maCoSo: int`<br>- `tenKho: String`<br>- `loaiKho: LoaiKho`<br>- `trangThai: TrangThaiKho`<br>- `maNguoiPhuTrach: Integer`. |
| **`TonKho.java`** | Entity | Quản lý số lượng tồn thực tế của từng mặt hàng theo từng Lô (`soLo`) và Hạn sử dụng (`hanSuDung`). | **Thuộc tính:**<br>- `maKho: int`<br>- `maItem: int`<br>- `soLo: String`<br>- `hanSuDung: LocalDate`<br>- `soLuongTon, giaNhap, giaBan: BigDecimal`. |
| **`FefoAllocationService.java`** | Domain Service | Thuật toán phân bổ xuất kho tự động theo tiêu chuẩn dược phẩm **FEFO (First Expired, First Out - Lô có hạn dùng gần nhất sẽ xuất trước)**. | **Record:** `AllocationLine(TonKho batch, BigDecimal quantityToTake, BigDecimal quantityBefore)`.<br>**Phương thức:** `allocateFefo(List<TonKho>, BigDecimal requiredQty)`. |
| **`PhieuNhapKho.java` & `ChiTietNhapKho.java`** | Aggregate & Entity | Phiếu nhập hàng thuốc/vật tư từ nhà cung cấp vào kho kèm đơn giá nhập, số lô, hạn dùng. | **Thuộc tính:** `soPhieuNhap`, `maKho`, `maNhaCungCap`, `ngayNhap`, `tongTien`, `trangThai`, `chiTiets`. |
| **`PhieuXuatKho.java` & `ChiTietXuatKho.java`** | Aggregate & Entity | Phiếu xuất kho thuốc theo đơn hoặc xuất điều chuyển nội bộ giữa các kho/tủ trực. | **Thuộc tính:** `soPhieuXuat`, `maKhoXuat`, `loaiXuat`, `maDonThuoc`, `maKhoNhan`, `ngayXuat`, `tongTien`, `chiTiets`. |
| **`DonDatHang.java` & `ChiTietDonDatHang.java`** | Aggregate & Entity | Đơn đặt hàng vật tư, dược phẩm gửi đến Nhà cung cấp. | **Thuộc tính:** `soDonDatHang`, `maNhaCungCap`, `ngayDat`, `tongTien`, `trangThai`, `chiTiets`. |
| **`NhaCungCap.java`** | Entity | Danh mục công ty dược phẩm, nhà phân phối thiết bị y tế. | **Thuộc tính:** `tenNhaCungCap`, `maSoThue`, `soDienThoai`, `email`, `diaChi`, `trangThai`. |
| **`Stocktake.java` & `StocktakeCount.java`** | Aggregate & Entity | Biên bản kiểm kê kho định kỳ, ghi nhận số lượng sổ sách, số lượng đếm thực tế và độ lệch chênh lệch. | **Thuộc tính:** `maKho`, `ngayKiemKe`, `trangThai`, `soLuongSoSach`, `soLuongThucTe`, `chenhLech`. |
| **`InventoryTransaction.java`** | Entity | Thẻ kho điện tử (Audit trail biến động kho), ghi nhận từng lần thay đổi tồn kho cùng mã chứng từ liên kết. | **Thuộc tính:** `maKho`, `maItem`, `maTonKho`, `loaiGiaoDich`, `soLuongThayDoi`, `tonTruocKhiThayDoi`, `tonSauKhiThayDoi`, `thoiGian`, `maChungTu`. |
| **`DonViTinhQuyDoi.java`** | Entity | Quy đổi đơn vị tính (Ví dụ: 1 Hộp = 10 Vỉ = 100 Viên) cùng hệ số quy đổi và giá bán theo đơn vị. | **Thuộc tính:** `maItem`, `tenDonVi`, `tyLeQuyDoi`, `giaBan`. |
| **`IdempotencyRecord.java`** | Entity | Đảm bảo an toàn giao dịch kho tránh trừ kho 2 lần khi mạng chập chờn hoặc người dùng click đúp. | **Thuộc tính:** `idempotencyKey`, `operationName`, `responsePayload`, `statusCode`, `expiryTime`. |

---

### 2.11. `domain.billing` - Phân hệ Viện phí & Quản lý Hóa đơn

| Tên File | Loại | Chức năng nghiệp vụ | Danh sách các trường dữ liệu & Phương thức |
| :--- | :--- | :--- | :--- |
| **`HoaDon.java`** | Aggregate Root | Hóa đơn thanh toán viện phí của một đợt khám bệnh. Tổng hợp chi phí khám, thuốc, dịch vụ, trừ phần BHYT chi trả. | **Thuộc tính:**<br>- `soHoaDon: String`<br>- `maLuotKham: int`<br>- `maBenhNhan: int`<br>- `maCoSo: int`<br>- `ngayLap: LocalDateTime`<br>- `tongTien, soTienBaoHiemChiTra, soTienBenhNhanChiTra, daThanhToan: BigDecimal`<br>- `trangThai: TrangThaiHoaDon`<br>- `ghiChu: String`<br>- *Quan hệ:* `chiTietHoaDons`, `phieuThus`, `congNoBenhNhan`.<br>**Phương thức:** `recordPayment(amount, phuongThuc, nguoiThu, ghiChu)`. Phát hành `PaymentRecordedEvent`, `HoaDonCreatedEvent`. |
| **`ChiTietHoaDon.java`** | Entity | Chi tiết từng mục chi phí được xuất trên hóa đơn (Khám bệnh, Xét nghiệm, Đơn thuốc, Vật tư tiêu hao). | **Thuộc tính:**<br>- `maHoaDon: int`<br>- `loaiDoiTuong: String`<br>- `maDoiTuong: int`<br>- `moTa: String`<br>- `soLuong, donGia: BigDecimal`. |
| **`PhieuThu.java`** | Entity | Biên lai thu tiền từng lần tương ứng với từng giao dịch nộp tiền của bệnh nhân (hỗ trợ thanh toán nhiều lần hoặc tạm ứng). | **Thuộc tính:**<br>- `maHoaDon: int`<br>- `soTien: BigDecimal`<br>- `ngayThu: LocalDateTime`<br>- `phuongThuc: PhuongThucThanhToan`<br>- `nguoiThu: int`<br>- `ghiChu: String`. |
| **`CongNoBenhNhan.java`** | Entity | Theo dõi số tiền người bệnh còn nợ phòng khám chưa thanh toán đủ. | **Thuộc tính:**<br>- `maHoaDon: int`<br>- `maBenhNhan: int`<br>- `soTienNo, soTienDaTra: BigDecimal`<br>- `hanThanhToan: LocalDate`<br>- `trangThai: TrangThaiCongNo`. |

---

### 2.12. `domain.appointment` - Phân hệ Lịch hẹn & Tiếp đón Đặt trước

| Tên File | Loại | Chức năng nghiệp vụ | Danh sách các trường dữ liệu & Phương thức |
| :--- | :--- | :--- | :--- |
| **`LichHen.java`** | Aggregate Root | Quản lý thông tin đặt lịch khám trước của bệnh nhân, bác sĩ hẹn khám, phòng khám, chi phí cọc giữ chỗ và trạng thái xác nhận. | **Thuộc tính:**<br>- `maBenhNhan: int`<br>- `maBacSi: int`<br>- `maCoSo: int`<br>- `maKhoa: Integer`<br>- `thoiGianBatDau, thoiGianKetThuc: LocalDateTime`<br>- `trangThai: TrangThaiLichHen`<br>- `lyDo, nguonDat: String`<br>- `chiPhiGiuCho: BigDecimal`<br>- `daThanhToanGiuCho: boolean`<br>- `soPhieuThu: String`<br>- `ngayThanhToan: LocalDateTime`<br>- `nguoiThuTien: Integer`<br>- `ghiChuHuy: String`.<br>**Phương thức:** `xacNhanThuTienMat()`, `huyLichHen(lyDo)`. Phát hành `LichHenBookedEvent`. |
| **`INotificationService.java`** | Service Contract | Khế ước dịch vụ gửi tin nhắn xác nhận lịch hẹn, nhắc hẹn khám và thông báo hủy lịch (qua SMS/Zalo/Email). | **Phương thức:** `sendAppointmentConfirmation()`, `sendAppointmentReminder()`, `sendCancellationNotice()`. |
| **`IPaymentGatewayService.java`** | Service Contract | Khế ước kết nối cổng thanh toán trực tuyến (VNPay, Momo, Napas) để thu tiền cọc giữ chỗ qua mạng. | **Records:** `PaymentResult`, `PaymentCallbackResult`.<br>**Phương thức:** `createPaymentUrl()`, `processCallback()`. |

---

### 2.13. `domain.facility` - Phân hệ Cơ sở & Hạ tầng Buồng/Khoa

| Tên File | Loại | Chức năng nghiệp vụ | Danh sách các trường dữ liệu |
| :--- | :--- | :--- | :--- |
| **`CoSo.java`** | Aggregate Root | Quản lý chi nhánh phòng khám/bệnh viện trong mô hình chuỗi y tế đa cơ sở. | **Thuộc tính:** `tenCoSo`, `diaChi`, `dienThoai`, `email`, `loaiCoSo`, `hoatDong`, `khoas`. |
| **`Khoa.java`** | Entity | Chuyên khoa thuộc cơ sở (Khoa Khám Bệnh, Khoa Nội, Ngoại, CĐHA, Xét Nghiệm...). | **Thuộc tính:** `maCoSo`, `tenKhoa`, `moTa`, `hoatDong`, `phongKhams`. |
| **`PhongKham.java`** | Entity | Buồng khám bệnh hoặc phòng thủ thuật vật lý (Ví dụ: Phòng khám Nhi 01, Phòng X-Quang 02). | **Thuộc tính:** `maKhoa`, `tenPhong`, `viTri`, `hoatDong`. |

---

### 2.14. `domain.identity` - Phân hệ Nhân sự, Bác sĩ & Chấm công

| Tên File | Loại | Chức năng nghiệp vụ | Danh sách các trường dữ liệu |
| :--- | :--- | :--- | :--- |
| **`NhanVien.java`** | Aggregate Root | Hồ sơ toàn bộ cán bộ y tế (Bác sĩ, Dược sĩ, Y tá, Thu ngân, Admin). Quản lý chứng chỉ hành nghề, tài khoản đăng nhập và chế độ lương. | **Thuộc tính:**<br>- `maNhanVien, ho, ten: String`<br>- `soDienThoai, email, diaChi: String`<br>- `gioiTinh: GioiTinh`<br>- `ngaySinh: LocalDate`<br>- `maChucVu: Integer`<br>- `username, passwordHash: String`<br>- `hoatDong: boolean`<br>- `dangNhapCuoi: LocalDateTime`<br>- `chungChiHanhNghe: String`<br>- `ngayVaoLam, ngayNghiViec, lyDoNghiViec: LocalDate, String`<br>- `luongCoBan, heSoLuong, phuCap: BigDecimal`. |
| **`VaiTro.java` & `ChucVu.java`** | Entity | Danh mục vai trò ủy quyền (ADMIN, DOCTOR, NURSE, PHARMACIST) và chức danh công việc (Trưởng khoa, Bác sĩ chính). | `tenVaiTro`, `moTa`, `tenChucVu`. |
| **`CaTruc.java` & `LoaiCa.java`** | Entity | Định nghĩa ca làm việc (Ca sáng: 07:00 - 11:30, Ca chiều: 13:30 - 17:00, Ca đêm). | `maLoaiCa`, `tenCa`, `gioBatDau`, `gioKetThuc`, `hoatDong`. |
| **`LichLamViec.java`, `LichLamViecCoDinh.java`, `LichLamViecDieuChinh.java`** | Entity | Phân ca làm việc chi tiết cho nhân viên y tế theo từng ngày, lịch cố định hàng tuần và phiếu xin đổi ca trực. | `maNhanVien`, `maCaTruc`, `maCoSo`, `maKhoa`, `maPhongKham`, `ngayLamViec`, `trangThai`, `nguonLich`. |
| **`ChamCong.java`, `DieuChinhChamCong.java`, `KyChamCong.java`** | Entity | Ghi nhận dữ liệu quét vân tay/khuôn mặt check-in, check-out, tính giờ làm, đi muộn về sớm và duyệt khiếu nại chấm công. | `thoiGianCheckIn`, `thoiGianCheckOut`, `soGioLam`, `soGioDiMuon`, `soGioVeSom`, `trangThai`. |
| **`NghiPhep.java`** | Entity | Quản lý đơn xin nghỉ phép (phép năm, nghỉ ốm, thai sản) và phê duyệt của cấp quản lý. | `loaiNghiPhep`, `tuNgay`, `denNgay`, `lyDo`, `trangThai`. |
| **`NhanVienKhoa.java`, `NhanVienVaiTro.java`, `PhanQuyenCoSo.java`** | Junction | Bảng nối phục vụ quan hệ n-n: một nhân viên có thể kiêm nhiệm nhiều khoa, nhiều vai trò và được phân công trên nhiều cơ sở y tế. | `maNhanVien`, `maKhoa`, `maVaiTro`, `maCoSo`. |

---

### 2.15. `domain.payroll` - Phân hệ Tính lương & Thù lao Bác sĩ

| Tên File | Loại | Chức năng nghiệp vụ | Danh sách các trường dữ liệu |
| :--- | :--- | :--- | :--- |
| **`BangLuong.java` & `ChiTietBangLuong.java`** | Aggregate & Entity | Bảng thanh toán lương tổng hợp cho nhân viên theo kỳ tính lương, gồm lương cơ bản, phụ cấp, hoa hồng thủ thuật và các khoản giảm trừ BHXH. | **Thuộc tính:** `maKyLuong`, `maNhanVien`, `luongCoBan`, `heSoLuong`, `tongPhuCap`, `tongThuLaoKham`, `tongKhauTru`, `thucLinh`, `chiTiets`. |
| **`KyLuong.java`** | Entity | Quản lý chu kỳ tính lương từng tháng trong năm (từ ngày đến ngày) và trạng thái chốt sổ lương. | **Thuộc tính:** `tenKy`, `thang`, `nam`, `tuNgay`, `denNgay`, `daChot`, `bangLuongs`. |
| **`CauHinhHoaHongLoaiDichVu.java`** | Entity | Tỷ lệ trích thưởng phần trăm hoa hồng cho Bác sĩ thực hiện theo từng loại dịch vụ y tế (Ví dụ: Siêu âm hưởng 15%, Phẫu thuật hưởng 25%). | **Thuộc tính:** `loaiDichVu`, `tyLeHoaHong`, `hoatDong`. |

---

### 2.16. `domain.system` - Phân hệ Cấu hình & Giám sát Hệ thống

| Tên File | Loại | Chức năng nghiệp vụ | Danh sách các trường dữ liệu |
| :--- | :--- | :--- | :--- |
| **`MaTranPhanQuyen.java`** | Entity | Ma trận phân quyền chi tiết (RBAC & ABAC): quy định cụ thể một Vai trò được thực hiện Hành động gì (Xem, Thêm, Sửa, Xóa, Duyệt, In) trên Phân hệ nào và trong Phạm vi dữ liệu nào. | **Thuộc tính:** `maVaiTro`, `phanHe`, `hanhDong: HanhDongQuyen`, `phamViDuLieu: PhamViDuLieu`, `duocPhep: boolean`. |
| **`NhatKyHeThong.java`** | Entity | Nhật ký kiểm toán bảo mật (Audit Log): ghi nhận toàn bộ thao tác hệ thống, địa chỉ IP máy trạm, thời gian và chi tiết hành động. | **Thuộc tính:** `maNhanVien`, `hanhDong`, `chiTiet`, `ipAddress`, `thoiGian`. |
| **`NhatKyHoSoBenhAn.java`** | Entity | Nhật ký truy vết hồ sơ bệnh án (Bắt buộc theo Luật Khám bệnh, chữa bệnh 2023): lưu lại nhân viên nào mở xem hoặc in hồ sơ bệnh án của bệnh nhân với lý do gì. | **Thuộc tính:** `maLuotKham`, `maNhanVien`, `hanhDong`, `lyDo`, `thoiGian`. |
| **`ThamSoHeThong.java`** | Entity | Cấu hình tham số động hệ thống (Ví dụ: Tên bệnh viện, Thời gian tự động logout phiên làm việc, Tiêu đề phiếu in). | **Thuộc tính:** `maThamSo`, `tenThamSo`, `giaTri`, `moTa`. |
| **`CauHinhTichHop.java`** | Entity | Cấu hình kết nối cổng API ngoại vi (Cổng giám định BHYT Việt Nam, Cổng Hóa đơn điện tử M-Invoice / Viettel, SMS Brandname). | **Thuộc tính:** `maDichVuTichHop`, `endpoint`, `apiKey`, `secretKey`, `hoatDong`. |
| **`TokenLamMoi.java`** | Entity | Quản lý Refresh Token xác thực người dùng, hỗ trợ cơ chế tự động đăng xuất và kiểm soát phiên làm việc trên từng máy trạm. | **Thuộc tính:** `maNhanVien`, `token`, `hetHan`, `daHuy`, `lyDoHuy`, `ipTao`, `thietBi`. |

---

### 2.17. `domain.repositories` - Khế ước Lưu trữ Dữ liệu (Repository Interfaces)
Package chỉ định nghĩa các Interface trừu tượng, hoàn toàn không dính líu đến SQL hay JDBC (Tuân thủ nguyên lý Dependency Inversion Principle - DIP):
* **`IRepository<T>`:** Generic Repository cung cấp các hàm CRUD chuẩn: `findById(int id)`, `findAll()`, `save(T entity)`, `delete(T entity)`, `find(ISpecification<T> spec)`, `findPaged(...)`.
* **`ISpecification<T>`:** Định nghĩa tiêu chuẩn lọc dữ liệu nghiệp vụ (Specification Pattern).
* **`PagedResult<T>`:** Đối tượng đóng gói kết quả phân trang (Danh sách bản ghi `items`, tổng số bản ghi `totalCount`, trang hiện tại `pageIndex`, kích thước trang `pageSize`).
* **Các Repository chuyên biệt theo Aggregate:**
  - `IBenhNhanRepository`: Tìm bệnh nhân theo CCCD, số điện thoại, mã thẻ BHYT.
  - `ILuotKhamRepository`: Lấy hàng đợi khám theo phòng khám, lấy lịch sử khám bệnh nhân.
  - `IDonThuocRepository`: Lấy danh sách đơn thuốc chờ duyệt hoặc chờ xuất cấp tại kho dược.
  - `IHoaDonRepository`: Quản lý hóa đơn viện phí, truy vấn công nợ.
  - `IKhoRepository`: Kế thừa `IUnitOfWork`, xử lý biến động tồn kho và trừ kho FEFO.
  - `ILichHenRepository`: Lấy lịch hẹn trong ngày theo Bác sĩ hoặc Cơ sở.
  - `INhanVienRepository`: Tìm nhân viên theo username, xác thực và phân quyền.
  - `ISystemRepository`: Đọc ghi tham số cấu hình và ghi log kiểm toán.

---

## 3. ĐÁNH GIÁ CHUYÊN SÂU: CÁC TRƯỜNG DỮ LIỆU ĐÃ ĐỦ CHO DESKTOP CHƯA?

### 3.1. Kết luận tổng quan

> [!IMPORTANT]
> **KẾT LUẬN ĐÁNH GIÁ:**
> 1. **Về Khung kiến trúc và Nghiệp vụ cốt lõi (Core Domain Logic): ĐẠT 85% - 90%**
>    - Kiến trúc DDD được xây dựng rất bài bản, rõ ràng, phân định đúng Aggregate Root, Entity, Value Object và Domain Events.
>    - Các luồng nghiệp vụ phức tạp như: Thuật toán cấp phát thuốc FEFO (`FefoAllocationService`), Kiểm tra an toàn dược lâm sàng (`ClinicalDrugSafetyService`), Khóa hồ sơ điện tử có chữ ký số (`emrLocked`, `emrSignature`), Theo dõi Audit Trail và Soft Delete đều đã hoàn thiện.
> 
> 2. **Về Mức độ đáp ứng dữ liệu cho Giao diện Desktop (UI Data Completeness): CHƯA ĐỦ HOÀN TOÀN (Đáp ứng ~70% - 75%)**
>    - Các màn hình Desktop y tế (theo `UI_CODING_STANDARDS.md`) có đặc thù **Mật độ thông tin rất cao (High Information Density)** và tuân thủ các quy định khắt khe của **Bộ Y Tế Việt Nam** (Biểu mẫu Bệnh án ngoại trú số 01/BV, Cổng dữ liệu BHYT Quyết định 4210/130/QĐ-BYT, Đơn thuốc điện tử Thông tư 04/2022/TT-BYT).
>    - Hiện tại, một số màn hình Desktop cốt lõi như: **Bàn khám Bác sĩ (MedicalRecordTabPanel)**, **Kê đơn thuốc ngoại trú**, **Tiếp đón hành chính (CCCD/BHYT)** và **Xuất hóa đơn viện phí** đang bị thiếu hụt một số trường dữ liệu trọng yếu. Nếu chỉ dùng nguyên trạng các trường Domain hiện tại, tầng UI Desktop sẽ phải "chắp vá" hoặc không thể hiển thị đầy đủ các trường nhập liệu cần thiết.

---

### 3.2. Bảng đối soát chi tiết từng Màn hình Desktop (Desktop Forms vs Domain Entities)

#### ① Phân hệ Tiếp đón & Đăng ký khám (`ReceptionPanel.java`, `PatientRegistrationFormPanel.java`)
* **Thực thể Domain tương ứng:** `BenhNhan`, `BaoHiem`, `LuotKham`.
* **Trường dữ liệu đã có trong Domain:**
  - `BenhNhan`: Họ, Tên, Ngày sinh, Giới tính, Số điện thoại, Email, Số CCCD, Địa chỉ, Người liên hệ.
  - `BaoHiem`: Số thẻ BHYT, Loại bảo hiểm, Hạn dùng, Mức hưởng, Nơi ĐKKCB ban đầu.
  - `LuotKham`: Số thứ tự, Ngày khám, Phòng khám, Lý do khám, Hình thức điều trị.
* **Các trường dữ liệu Desktop đang CẦN nhưng Domain CHƯA CÓ:**
  1. `maBenhNhan`: Hiện tại `BenhNhan` chỉ có `id (int)` tự tăng trong database. Trên UI Desktop y tế bắt buộc phải có **Mã Bệnh Nhân định dạng chuỗi hiển thị Barcode/QR** (Ví dụ: `BN24000185`).
  2. `ngheNghiep` (Nghề nghiệp): Trường bắt buộc trong hồ sơ hành chính Bộ Y Tế.
  3. `danToc` (Dân tộc): Bắt buộc cho báo cáo y tế (Ví dụ: Kinh, Tày, Thái...).
  4. `quocTich` (Quốc tịch): Mặc định "Việt Nam", hỗ trợ bệnh nhân người nước ngoài.
  5. `maTinhThanh`, `maQuanHuyen`, `maPhuongXa`: Hiện tại chỉ có `diaChi` dạng chuỗi tự do. Không thể trích xuất mã địa bàn hành chính để gửi cổng BHYT nếu thiếu mã Tỉnh/Huyện/Xã.
  6. `soThuTuDisplay`: Cần định dạng chuỗi số thứ tự hiển thị tại bảng điện tử phòng chờ (Ví dụ: "PK01-025").
  7. `doiTuongKham`: Phân loại đối tượng khám (BHYT / Thu phí / Khám dịch vụ / Ưu tiên người già >75t, trẻ em <6t).
  8. `thoiGianTiepNhan`: Thời điểm bệnh nhân lấy số tại cửa tiếp đón (khác với `thoiGianBatDauKham` lúc bác sĩ nhấn gọi vào bàn khám).

---

#### ② Phân hệ Bàn khám Bác sĩ & Bệnh án ngoại trú (`DoctorStationPanel`, `MedicalRecordTabPanel.java`)
* **Thực thể Domain tương ứng:** `LuotKham`, `ChanDoan`, `SinhHieu`.
* **Trường dữ liệu đã có trong Domain:**
  - `LuotKham`: Lý do khám, Thời gian bắt đầu/kết thúc khám, Chữ ký số EMR, Cờ khóa EMR.
  - `ChanDoan`: Mã ICD-10, Chuỗi chẩn đoán văn bản, Cờ bệnh chính.
  - `SinhHieu`: Mạch, Nhiệt độ, Huyết áp tâm thu/trương, Nhịp thở, Chiều cao, Cân nặng, BMI, SpO2.
* **Khoảng trống dữ liệu NGHIÊM TRỌNG (Critical Gap):**
  - Màn hình `MedicalRecordTabPanel` mô phỏng tờ Bệnh án ngoại trú chuẩn Bộ Y Tế. Hiện tại `LuotKham` chỉ có trường duy nhất là `lyDoKham: String`, hoàn toàn thiếu các phân đoạn bệnh án lâm sàng:
  1. `quaTrinhBenhLy` (Diễn tiến bệnh lý từ khi khởi phát đến nay).
  2. `tienSuBanThanText` & `tienSuGiaDinhText` (Mô tả tự do diễn giải chi tiết tiền sử gia đình, bản thân).
  3. `khamToanThan` (Khám tổng quát thể trạng: Da niêm mạc, hệ thống hạch bạch huyết, tuyến giáp).
  4. `khamBoPhan` (Khám chuyên khoa: Tuần hoàn, Hô hấp, Tiêu hóa, Thần kinh, Cơ xương khớp).
  5. `tomTatBenhAn` (Tóm tắt các triệu chứng cơ năng, thực thể cốt lõi).
  6. `huongDieuTri` / `loiDanBacSi` (Hướng điều trị, chế độ dinh dưỡng, dặn dò chăm sóc).
  - `ChanDoan`: Cần phân loại rõ ràng:
    + Chẩn đoán sơ bộ (Lúc tiếp nhận vào phòng khám).
    + Chẩn đoán xác định (Sau khi đã có kết quả xét nghiệm / CĐHA).
    + Bệnh kèm theo / Biến chứng.

---

#### ③ Phân hệ Xử trí & Kết thúc khám (`TreatmentResolutionTabPanel.java`)
* **Thực thể Domain tương ứng:** `LuotKham`.
* **Trường dữ liệu đã có trong Domain:** `trangThaiLuotKham` (`DANG_KHAM`, `HOAN_THANH`, `HUY`).
* **Các trường dữ liệu Desktop đang CẦN nhưng Domain CHƯA CÓ:**
  1. `ketQuaDieuTri`: Kết quả điều trị đợt khám (Enum: `KHOI`, `DO_GIAM`, `KHONG_THAY_DOI`, `NANG_HON`, `TU_VONG`).
  2. `huongXuTri`: Xử trí ra viện (Enum: `CHO_VE_TOA`, `HEN_TAI_KHAM`, `NHAP_VIEN_NOI_TRU`, `CHUYEN_TUYEN_TREN`).
  3. `ngayHenTaiKham`: Ngày cụ thể bác sĩ dặn bệnh nhân tái khám.
  4. `soNgayNghiHuongBHXH`: Số ngày cấp giấy nghỉ ốm hưởng bảo hiểm xã hội (Căn cứ in Giấy nghỉ việc hưởng BHXH).
  5. `lyDoChuyenTuyen`: Ghi chú lý do chuyển bệnh nhân lên bệnh viện tuyến trên.

---

#### ④ Phân hệ Kê đơn thuốc ngoại trú (`PrescriptionOrderTabPanel.java`)
* **Thực thể Domain tương ứng:** `DonThuoc`, `ChiTietDonThuoc`, `Item`.
* **Trường dữ liệu đã có trong Domain:**
  - `DonThuoc`: Ngày kê, Trạng thái đơn, Ghi chú, Ngày cấp thuốc, Bác sĩ kê.
  - `ChiTietDonThuoc`: Số lượng kê, Số ngày, Liều dùng, Tần suất, Hướng dẫn, Đơn giá.
* **Các trường dữ liệu Desktop đang CẦN nhưng Domain CHƯA CÓ:**
  1. `duongDung` trong `ChiTietDonThuoc`: Đường dùng thuốc (Uống, Tiêm bắp, Tiêm tĩnh mạch, Nhỏ mắt, Bôi ngoài da, Khí dung, Đặt dưới lưỡi...). Đây là trường bắt buộc in trên đơn thuốc Bộ Y Tế.
  2. `sang`, `trua`, `chieu`, `toi`: Liều dùng chia theo buổi trong ngày (Ví dụ: Sáng 1 viên, Chiều 1 viên) để hiển thị lên lưới nhập liệu tốc độ cao cho bác sĩ.
  3. `maKhoXuat` trong `DonThuoc`: Bác sĩ chỉ định lấy thuốc từ Kho ngoại trú hay từ Tủ trực của phòng cấp cứu.
  4. `loiDanDonThuoc`: Dặn dò chung của đơn thuốc (Ví dụ: "Uống nhiều nước, kiêng đồ cay nóng").

---

#### ⑤ Phân hệ Cận lâm sàng & Chẩn đoán hình ảnh (`ParaclinicalOrderPanel.java`)
* **Thực thể Domain tương ứng:** `ChiDinhDichVu`, `ChiTietKetQuaXetNghiem`.
* **Trường dữ liệu đã có trong Domain:**
  - `ChiDinhDichVu`: Số lượng, Đơn giá, Trạng thái, Kết quả (text), Kết luận (text), Thời gian chỉ định/thực hiện.
  - `ChiTietKetQuaXetNghiem`: Tên chỉ số, Giá trị, Đơn vị đo, Khoảng tham chiếu, Đánh giá.
* **Các trường dữ liệu Desktop đang CẦN nhưng Domain CHƯA CÓ:**
  1. `maPhongThucHien`: Chỉ định thực hiện tại buồng cận lâm sàng nào (Phòng Siêu âm 01 hay Buồng X-Quang kỹ thuật số).
  2. `bacSiThucHien` / `kyThuatVien`: Người trực tiếp chạy máy hoặc kỹ thuật viên lấy mẫu máu.
  3. `danhSachHinhAnhUrl`: Đường dẫn danh sách file ảnh kết quả DICOM / JPG (Siêu âm, X-quang, Nội soi) để hiển thị xem trước trực tiếp trên Desktop.
  4. `thoiGianTraKetQua`: Thời điểm hoàn tất và ký số kết quả cận lâm sàng.

---

#### ⑥ Phân hệ Quản lý Viện phí & Hóa đơn (`BillingInvoicePanel.java`)
* **Thực thể Domain tương ứng:** `HoaDon`, `ChiTietHoaDon`, `PhieuThu`, `CongNoBenhNhan`.
* **Trường dữ liệu đã có trong Domain:**
  - `HoaDon`: Tổng tiền, Tiền BHYT chi trả, Tiền BN chi trả, Đã thanh toán, Trạng thái.
  - `PhieuThu`: Số tiền, Phương thức thanh toán, Người thu.
* **Các trường dữ liệu Desktop đang CẦN nhưng Domain CHƯA CÓ:**
  1. `chietKhauTienMat`, `chietKhauPhanTram`: Chính sách giảm giá viện phí, miễn giảm cho người nghèo hoặc thẻ VIP.
  2. `lyDoGiamGia`: Căn cứ duyệt miễn giảm.
  3. `soTienTamUng`: Theo dõi số tiền người bệnh đã đặt cọc tạm ứng trước khi làm thủ thuật hoặc khám.
  4. `soTienHoanTra`: Số tiền thu ngân trả lại cho bệnh nhân khi tổng chi phí nhỏ hơn tiền tạm ứng.
  5. `soHoaDonDienTu`, `maTraCuuHddt`: Thông tin phục vụ tích hợp xuất Hóa đơn điện tử VAT có mã của cơ quan Thuế.

---

#### ⑦ Phân hệ Kho Dược & Tủ trực (`PharmacyDispensePanel.java`)
* **Thực thể Domain tương ứng:** `Item`, `TonKho`, `PhieuNhapKho`, `PhieuXuatKho`.
* **Trường dữ liệu đã có trong Domain:**
  - Mã, Tên, Hoạt chất, Hàm lượng, Đơn vị cơ bản, Quy cách, Giá nhập/bán, Tồn tối thiểu/tối đa, Tồn theo số lô, hạn dùng, Thuật toán FEFO.
* **Các trường dữ liệu Desktop đang CẦN nhưng Domain CHƯA CÓ:**
  1. `dangBaoChe`: Viên nén, Viên nang, Siro, Hỗn dịch tiêm, Bột pha...
  2. `nuocSanXuat`, `hangSanXuat`: Nước sản xuất, Hãng sản xuất thuốc (Bắt buộc khi đấu thầu và xuất hóa đơn).
  3. `soDangKy`: Số visa đăng ký lưu hành thuốc do Cục Quản lý Dược cấp (Bắt buộc gửi cổng BHYT).

---

### 3.3. Danh mục các trường dữ liệu BẮT BUỘC cần bổ sung ngay

Dưới đây là bảng tổng hợp các trường dữ liệu cần được bổ sung vào các Entity của `clinic-domain` để đảm bảo hệ thống Desktop chạy trơn tru:

| Thực thể (Entity) | Trường dữ liệu cần bổ sung | Kiểu dữ liệu | Mục đích nghiệp vụ trên Desktop |
| :--- | :--- | :--- | :--- |
| **`BenhNhan`** | `maBenhNhanFormat` | `String` | Mã bệnh nhân hiển thị in thẻ khám, quét Barcode/QR (`BN24000185`). |
| | `ngheNghiep` | `String` | Hồ sơ hành chính bệnh nhân theo mẫu Bộ Y Tế. |
| | `danToc` | `String` | Dân tộc (Mặc định: "Kinh"). |
| | `quocTich` | `String` | Quốc tịch (Mặc định: "Việt Nam"). |
| | `maTinh`, `maHuyen`, `maXa` | `String` | Mã danh mục hành chính phục vụ gửi cổng BHYT 4210. |
| **`LuotKham`** | `quaTrinhBenhLy` | `String` | Tab Bệnh án: Quá trình diễn tiến bệnh lý. |
| | `tienSuBanThan` | `String` | Tab Bệnh án: Tiền sử bản thân diễn giải. |
| | `tienSuGiaDinh` | `String` | Tab Bệnh án: Tiền sử gia đình diễn giải. |
| | `khamToanThan` | `String` | Tab Bệnh án: Khám tổng quát (da, niêm mạc, hạch...). |
| | `khamBoPhan` | `String` | Tab Bệnh án: Khám chuyên khoa (tuần hoàn, hô hấp, tiêu hóa...). |
| | `tomTatBenhAn` | `String` | Tab Bệnh án: Tóm tắt bệnh án lâm sàng. |
| | `huongDieuTri` | `String` | Tab Bệnh án: Lời dặn dò, hướng điều trị tiếp theo. |
| | `ketQuaDieuTri` | `String / Enum` | Tab Xử trí: Khỏi / Đỡ / Không đổi / Nặng hơn / Tử vong. |
| | `huongXuTri` | `String / Enum` | Tab Xử trí: Về toa / Tái khám / Nhập viện / Chuyển tuyến. |
| | `ngayHenTaiKham` | `LocalDate` | Tab Xử trí: Ngày hẹn bệnh nhân đến khám lại. |
| | `soNgayNghiBHXH` | `Integer` | Tab Xử trí: Cấp giấy nghỉ ốm hưởng BHXH. |
| **`ChiTietDonThuoc`** | `duongDung` | `String` | Đường dùng thuốc (Uống, Tiêm, Bôi...) in trên toa thuốc. |
| | `sang`, `trua`, `chieu`, `toi` | `BigDecimal` | Liều lượng chi tiết từng buổi trong ngày phục vụ nhập liệu nhanh. |
| **`ChiDinhDichVu`** | `maPhongThucHien` | `Integer` | Buồng kỹ thuật/phòng thực hiện xét nghiệm/CĐHA. |
| | `bacSiThucHien` | `Integer` | Kỹ thuật viên hoặc bác sĩ chuyên khoa đọc kết quả. |
| | `hinhAnhUrls` | `String` | Danh sách URL/đường dẫn ảnh DICOM/JPG kết quả cận lâm sàng. |
| **`HoaDon`** | `soTienTamUng` | `BigDecimal` | Số tiền bệnh nhân đã nộp tạm ứng trước đó. |
| | `soTienGiamGia` | `BigDecimal` | Số tiền miễn giảm / chiết khấu viện phí. |
| | `soHoaDonDienTu` | `String` | Số hóa đơn điện tử VAT gửi cơ quan Thuế. |
| **`Item`** | `dangBaoChe` | `String` | Dạng bào chế thuốc (Viên nén, Dung dịch...). |
| | `soDangKy` | `String` | Số đăng ký lưu hành thuốc (Visa thuốc). |

---

## 4. ĐỀ XUẤT LỘ TRÌNH NÂNG CẤP DOMAIN ĐÁP ỨNG THỰC CHIẾN DESKTOP

Để hệ thống Desktop phát triển nhanh chóng, không bị vướng mắc khi nối giao diện Swing vào nghiệp vụ, khuyến nghị triển khai theo 3 giai đoạn:

```
[ Giai đoạn 1: Lâm Sàng Bác Sĩ ] 
   └── Bổ sung ngay các trường Bệnh án ngoại trú & Xử trí vào LuotKham.
   └── Bổ sung đường dùng và liều theo buổi vào ChiTietDonThuoc.
                │
                ▼
[ Giai đoạn 2: Tiếp Đón & Hành Chính BHYT ] 
   └── Bổ sung mã bệnh nhân chuỗi, dân tộc, nghề nghiệp vào BenhNhan.
   └── Bổ sung phân nhóm đối tượng khám vào LuotKham.
                │
                ▼
[ Giai đoạn 3: Viện Phí & Cận Lâm Sàng ] 
   └── Bổ sung tạm ứng, giảm giá vào HoaDon.
   └── Hỗ trợ đường dẫn file ảnh kết quả trong ChiDinhDichVu.
```

1. **Giai đoạn 1 (Ưu tiên số 1 - Phân hệ Lâm sàng Bác sĩ):**
   * Bổ sung ngay 7 trường bệnh án lâm sàng (`quaTrinhBenhLy`, `khamToanThan`, `khamBoPhan`, `tomTatBenhAn`, `huongDieuTri`, `ketQuaDieuTri`, `huongXuTri`, `ngayHenTaiKham`) vào `LuotKham.java`.
   * Bổ sung `duongDung`, `sang`, `trua`, `chieu`, `toi` vào `ChiTietDonThuoc.java`.
   * *Kết quả:* Cho phép hoàn thiện ngay 100% chức năng Bàn khám Bác sĩ (`DoctorStationPanel`) và toàn bộ 8 Tab lâm sàng.

2. **Giai đoạn 2 (Ưu tiên số 2 - Tiếp đón & Hành chính BHYT):**
   * Bổ sung `maBenhNhanFormat`, `ngheNghiep`, `danToc`, `quocTich`, `maTinh`/`maHuyen`/`maXa` vào `BenhNhan.java`.
   * *Kết quả:* Giúp màn hình Đăng ký khám (`ReceptionPanel`) quét được CCCD gắn chip và điền form đầy đủ chuẩn Bộ Y Tế.

3. **Giai đoạn 3 (Ưu tiên số 3 - Viện phí, Kho Dược & Cận lâm sàng):**
   * Bổ sung `soTienTamUng`, `soTienGiamGia`, `soHoaDonDienTu` vào `HoaDon.java`.
   * Bổ sung `danhSachHinhAnhUrl`, `maPhongThucHien` vào `ChiDinhDichVu.java`.
   * Bổ sung `soDangKy`, `dangBaoChe` vào `Item.java`.
   * *Kết quả:* Hoàn thiện phân hệ Thu ngân thanh toán và Kho cấp phát thuốc FEFO.


---

## 5. NHẬT KÝ CẬP NHẬT: HOÀN TẤT BỔ SUNG TRƯỜNG DỮ LIỆU CHO DESKTOP (EHC HIS)
*(Ngày cập nhật: 01/11/2026 - Đã biên dịch thành công 100% với OpenJDK javac)*

Hệ thống đã chính thức bổ sung đầy đủ các trường dữ liệu thiếu hụt vào các Entity cốt lõi và khởi tạo thêm 3 Enums nghiệp vụ y tế:

### 1. Tạo mới 3 Enums y tế (`domain.common.enums`):
* `KetQuaDieuTri.java`: `KHOI` (1), `DO_GIAM` (2), `KHONG_THAY_DOI` (3), `NANG_HON` (4), `TU_VONG` (5).
* `HuongXuTri.java`: `CHO_VE_TOA` (1), `HEN_TAI_KHAM` (2), `NHAP_VIEN_NOI_TRU` (3), `CHUYEN_TUYEN_TREN` (4), `CHUYEN_CHUYEN_KHOA` (5).
* `DoiTuongKham.java`: `BHYT` (1), `THU_PHI` (2), `DICH_VU_YEU_CAU` (3), `MIEN_PHI` (4), `UU_TIEN` (5).

### 2. Cập nhật chi tiết các Entity:
1. **`LuotKham.java`**:
   - Tiếp đón: `soThuTuDisplay`, `thoiGianTiepNhan`, `doiTuongKham`.
   - Bệnh án ngoại trú (Mẫu BA 01/BV): `quaTrinhBenhLy`, `tienSuBanThan`, `tienSuGiaDinh`, `khamToanThan`, `khamBoPhan`, `tomTatBenhAn`, `huongDieuTri`, `loiDanBacSi`.
   - Xử trí kết thúc khám: `ketQuaDieuTri`, `huongXuTri`, `ngayHenTaiKham`, `soNgayNghiHuongBHXH`, `lyDoChuyenTuyen`.
2. **`ChanDoan.java`**:
   - Thêm `loaiChanDoan` (Sơ bộ, Xác định, Bệnh kèm theo, Biến chứng).
3. **`ChiTietDonThuoc.java`**:
   - Thêm đường dùng thuốc in toa: `duongDung` (Uống, Tiêm, Bôi, Nhỏ...).
   - Thêm liều theo buổi: `sang`, `trua`, `chieu`, `toi`.
4. **`DonThuoc.java`**:
   - Thêm kho xuất thuốc: `maKhoXuat`.
   - Thêm dặn dò đơn thuốc: `loiDanDonThuoc`.
5. **`BenhNhan.java`**:
   - Thêm mã barcode/QR: `maBenhNhanFormat`.
   - Thêm hành chính y tế: `ngheNghiep`, `danToc`, `quocTich`.
   - Thêm phân cấp địa giới BHYT: `maTinh`, `maHuyen`, `maXa`.
   - Thêm hàm tiện ích: `getTuoi()`.
6. **`ChiDinhDichVu.java`**:
   - Thêm buồng kỹ thuật: `maPhongThucHien`.
   - Thêm bác sĩ/KTV thực hiện: `bacSiThucHien`.
   - Thêm danh sách ảnh DICOM/PACS: `danhSachHinhAnhUrl`.
   - Thêm thời gian trả kết quả: `thoiGianTraKetQua`.
7. **`HoaDon.java`**:
   - Thêm viện phí: `soTienTamUng`, `soTienGiamGia`, `chietKhauPhanTram`, `lyDoGiamGia`, `soTienHoanTra`.
   - Thêm Hóa đơn điện tử VAT: `soHoaDonDienTu`, `maTraCuuHddt`.
8. **`Item.java`**:
   - Thêm chuẩn Dược: `dangBaoChe`, `nuocSanXuat`, `hangSanXuat`, `soDangKy` (Visa thuốc).

**Trạng thái hiện tại:** Cả `clinic-domain` và `clinic-desktop` đã được biên dịch kiểm chứng thành công, sẵn sàng 100% để phát triển giao diện và liên kết dữ liệu!


---

## 6. CẬP NHẬT: ĐỒNG BỘ 100% CÁC TRƯỜNG MÀN HÌNH ĐĂNG KÝ BỆNH NHÂN VÀO DOMAIN
*(Ngày cập nhật: 04/11/2026 - Kiểm thử biên dịch OpenJDK javac thành công 100%)*

Hệ thống đã chính thức bổ sung toàn bộ 15 trường dữ liệu chuyên biệt phục vụ phân hệ Đón tiếp & Cổng giám định BHYT Quyết định 130/BYT:

1. **`BenhNhan.java`**:
   - `noiSinh: String`: Nơi sinh của người bệnh (quan trọng với hồ sơ nhi/sơ sinh).
   - `soNhaThonPho: String`: Số nhà, thôn xóm, phố.
   - `ngayCapCCCD: LocalDate`: Ngày cấp CMND/CCCD.
   - `noiCapCCCD: String`: Nơi cấp CMND/CCCD.
   - `noiLamViec: String`: Cơ quan / nơi công tác (phục vụ tai nạn lao động).

2. **`BaoHiem.java`**:
   - `ngayBatDau: LocalDate`: Giá trị sử dụng từ ngày (đã hoàn thiện cặp Từ ngày - Đến ngày của thẻ BHYT).
   - `maNoiSong: String`: Mã khu vực nơi sống (K1, K2, K3 hưởng 100% chi phí KCB BHYT).
   - `maNoiDangKyKCB: String`: Mã cơ sở KCB ban đầu.
   - `diaChiThe: String`: Địa chỉ in trên phôi thẻ BHYT.
   - `dungTuyen: boolean`: Khám đúng tuyến hay trái tuyến.
   - `coGiayKhaiSinh: boolean`: Cờ trẻ em dưới 6 tuổi khám bằng giấy khai sinh.
   - `khongGiuThe: boolean`: Cờ không giữ thẻ BHYT bản gốc.
   - `mienCungChiTra: boolean`: Giấy chứng nhận không cùng chi trả trong năm.
   - `ngayMienCungChiTra: LocalDate`: Ngày bắt đầu được miễn cùng chi trả.
   - `ngayDu5NamLienTuc: LocalDate`: Ngày tham gia BHYT đủ 5 năm liên tục.

3. **`LuotKham.java`**:
   - `maDichVuKham: Integer`: Dịch vụ khám ban đầu được chọn tại tiếp đón.
   - `loaiDangKyKham: String`: Khám mới / Tái khám / Cấp cứu.
   - `dnVnTien: boolean`: Cờ ưu tiên "ĐN VN tiền".

**Kết quả:** Màn hình Đón tiếp & Đăng ký khám (`PatientRegistrationFormPanel.java`) và Domain Model đã đạt độ tương thích **100%** (Toàn bộ các ô nhập, checkbox, combobox đều đã có trường ánh xạ trực tiếp trong Domain).
