# Rà soát Phase 2: Vai trò miền, phạm vi dữ liệu và độ sẵn sàng kiểm toán (Domain Roles & Audit Readiness Review)

**Ngày rà soát:** 05/11/2026  
**Phạm vi:** Kiểm tra độ sẵn sàng của Phase 2 (`SEC-01`, `AUD-01`, `AUTH-TEST-01`) đối chiếu giữa tài liệu (`DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `UI_CODING_STANDARDS.md`, `docs/ke-hoach-trien-khai/*`), schema SQL Server (`V1`–`V4`) và mã nguồn thực tế trong `clinic-domain`, `clinic-application`, `clinic-infrastructure`, `clinic-desktop`.  
**Nguyên tắc:** Phân biệt rõ **sự tồn tại của model/interface/bảng CSDL** với **cơ chế lưu trữ (persistence) và cưỡng chế thực thi (runtime enforcement)**; không suy diễn chính sách lâm sàng/quản trị thay cho chủ dự án (Owner); chỉ sử dụng dữ liệu tổng hợp (synthetic data) và không lộ thông tin nhạy cảm/secret.

> **Lưu ý về thời điểm:** Đây là snapshot rà soát ngày 05/11/2026, trước các đợt triển khai bảo mật tiếp theo; các mô tả “chưa thực thi” trong bảng dưới không phải trạng thái hiện tại. Từ sau snapshot này, hệ thống đã bổ sung lựa chọn role/scope khi đăng nhập, session lifecycle, evaluator default-deny, audit write và bảo vệ các luồng reception được nêu trong [`02-cac-dot-trien-khai.md`](02-cac-dot-trien-khai.md) và [`03-model-task-board.md`](03-model-task-board.md). Dùng hai tài liệu đó cùng [`phase2-authz-enforcement-review.md`](phase2-authz-enforcement-review.md) để biết trạng thái hiện hành; các Owner decision hiện hành nằm trong [`phase2-role-permission-matrix-approval.md`](phase2-role-permission-matrix-approval.md).

---

## 1. Tóm tắt điều hành và trạng thái Gate Phase 2

Theo [02-cac-dot-trien-khai.md](02-cac-dot-trien-khai.md) và [03-model-task-board.md](03-model-task-board.md), **Phase 2 (`SEC-01`, `AUD-01`) là chốt chặn bắt buộc (`[!] Blocked`)** trước khi mở các luồng nghiệp vụ nhạy cảm ở Phase 4 (`EMR-01`, `EMR-02`), Phase 6 (`RX-01`, `INV-02`..`INV-07`) và Phase 7 (`BILL-01`, `BILL-02`).

| Hạng mục Phase 2 | Model / Contract / Schema hiện có | Persistence & Runtime Enforcement thực tế | Đánh giá sẵn sàng |
|---|---|---|---|
| **Xác thực đăng nhập (`NhanVien`, `MatKhauBoBam`)** | `NhanVien`, `INhanVienRepository`, `AuthenticateNguoiDungCommand`, `AuthenticateNguoiDungHandler`, `PBKDF2MatKhauBoBam`; bảng `NhanVien` (`V1`), seed/repair hash (`V2`, `V3`). | Đã nối `DangNhapBieuMau` → `XacThucGhepNoiGoc` → `AuthenticateNguoiDungHandler` → `NhanVienRepository`. Kiểm tra `hoatDong`, `isDeleted`, PBKDF2 hash và cập nhật `dangNhapCuoi` trong transaction. Có 9 unit tests synthetic (`AUTH-TEST-01`). Chưa có khóa tài khoản khi sai mật khẩu nhiều lần. | **Có nền tảng xác thực cơ bản; chưa đủ chính sách khóa/phiên** |
| **Vai trò & Ma trận phân quyền (`VaiTro`, `NhanVienVaiTro`, `MaTranPhanQuyen`)** | `VaiTro`, `NhanVienVaiTro`, `MaTranPhanQuyen`, `HanhDongQuyen`, `PhamViDuLieu`, `IHeThongRepository.getMatrixByRoleId`; bảng `VaiTro`, `NhanVienVaiTro`, `MaTranPhanQuyen` (`V1`), 6 vai trò seed (`V2`). | `NhanVienRepository.getRoleNames` đọc danh sách tên vai trò, nhưng `AuthenticateNguoiDungHandler` chỉ lấy vai trò đầu tiên (`findFirst()`). **Chưa có** `HeThongRepository`, bảng `MaTranPhanQuyen` chưa có dữ liệu seed, và **không có** bộ kiểm tra quyền (authorization guard/interceptor) ở `CommandHandler`/`QueryHandler`. | **Chưa thực thi (Model/Schema only)** |
| **Phạm vi Cơ sở / Khoa (`CoSo`, `Khoa`, `PhongKham`, `PhanQuyenCoSo`, `NhanVienKhoa`, `BenhNhanCoSo`)** | Các class trong `domain.facility`, `domain.identity`, `domain.patient` và các bảng tương ứng trong `V1`, seed liên kết cho 2 tài khoản mẫu trong `V2`. | `PhanQuyenCoSo` mới chỉ được join trong `NhanVienRepository.getActiveDoctors(int maCoSo)`. `DaXacThucNguoiDung` và `NguoiDungPhien` không lưu danh sách `maCoSo`/`maKhoa` được phép; `DangNhapBieuMau` gán cứng `setDepartmentName("Chưa phân công")`. Các truy vấn tiếp đón/bệnh nhân hiện lấy toàn bộ dữ liệu active mà chưa lọc theo scope cơ sở/khoa của người dùng. | **Chưa thực thi (Model/Schema only)** |
| **Nhật ký hệ thống & Truy vết bệnh án (`NhatKyHeThong`, `NhatKyHoSoBenhAn`, `CoSoThucThe`, Domain Events)** | `CoSoThucThe` (`createdBy`, `updatedBy`, `deletedBy`, `version`), `NhatKyHeThong`, `NhatKyHoSoBenhAn`, 10 lớp `IMienNghiepVuSuKien` và `IMienNghiepVuSuKienPhatHanh`; bảng `NhatKyHeThong`, `NhatKyHoSoBenhAn` (`V1`). | Cột `createdBy`/`updatedBy` được ghi ở một số bảng (`NhanVien`, `BenhNhan`, `BaoHiem`, `LuotKham`) nhưng lấy từ `NguoiDungPhien.getInstance().getUserId()` chưa được xác thực chặt chẽ ở tầng Application. **Không có** repository hay lệnh SQL nào ghi/đọc `NhatKyHeThong` và `NhatKyHoSoBenhAn`. Các `IMienNghiepVuSuKien` chỉ nằm trong bộ nhớ tạm (`transient List<IMienNghiepVuSuKien>`) và chưa có bộ phát hành/lắng nghe. | **Chưa thực thi (Model/Schema only)** |
| **Quản lý phiên & Token (`NguoiDungPhien`, `MaXacThucLamMoi`, `ThamSoHeThong`)** | `NguoiDungPhien` (Desktop), `MaXacThucLamMoi`, `ThamSoHeThong` (`domain.system`), seed `AUTO_LOGOUT_MINUTES = '30'` (`V2`). | `NguoiDungPhien` là singleton cục bộ có các setter công khai (`setRole`, `setUserId`), không có bộ đếm tự động đăng xuất theo `AUTO_LOGOUT_MINUTES` và không có chức năng đăng xuất về `DangNhapBieuMau`. `MaXacThucLamMoi` (cùng `CauHinhTichHop`) **chưa có bảng trong `V1`–`V4`** và chưa có persistence. | **Chưa thực thi (Model only / Thiếu bảng SQL)** |

---

## 2. Đối chiếu chi tiết Bằng chứng Mã nguồn, ERD và Schema (Model/Interface vs. Persistence/Enforcement)

### 2.1. Định danh, Vai trò và Ma trận phân quyền (`domain.identity`, `domain.system`)

1. **Thực thể và Enum trong `clinic-domain`:**
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/identity/NhanVien.java`: Aggregate Root chứa `username`, `passwordHash`, `hoatDong`, `dangNhapCuoi`, `maChucVu`, `chungChiHanhNghe` nhưng không có các trường đếm số lần đăng nhập sai, thời điểm khóa tạm thời hoặc cờ bắt buộc đổi mật khẩu.
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/identity/VaiTro.java`, `ChucVu.java`, `NhanVienVaiTro.java`: Các lớp thực thể biểu diễn vai trò, chức vụ và bảng nối `n-n` nhân viên–vai trò.
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/system/MaTranPhanQuyen.java`: Thực thể ánh xạ `maVaiTro`, `phanHe: String`, `hanhDong: HanhDongQuyen`, `phamViDuLieu: PhamViDuLieu`, `duocPhep: boolean`.
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/HanhDongQuyen.java`: Hiện định nghĩa 5 giá trị `XEM(1)`, `TAO(2)`, `SUA(3)`, `XOA(4)`, `DUYET(5)`.
     - *Điểm lệch với tài liệu:* `DOMAIN_ANALYSIS.md` (Mục 2.5) mô tả 7 hành động gồm `XEM (1), THEM (2), SUA (3), XOA (4), DUYET (5), IN (6), XUAT_FILE (7)`. Trong code thực tế dùng tên `TAO` thay vì `THEM` và chưa có `IN`, `XUAT_FILE` hay hành động chuyên biệt cho mở khóa bệnh án.
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/PhamViDuLieu.java`: Hiện định nghĩa 4 giá trị `TOAN_HE_THONG(1)`, `CO_SO(2)`, `KHOA_PHONG(3)`, `CHI_NHANH(4)`.
     - *Điểm lệch với tài liệu và schema:* `DOMAIN_ANALYSIS.md` (Mục 2.5) mô tả `TOAN_HE_THONG, THEO_CO_SO, THEO_KHOA, CA_NHAN`. Trong code thực tế không có mức `CA_NHAN`, đồng thời tồn tại cả `CO_SO` và `CHI_NHANH` chưa rõ ranh giới. Ngoài ra, `MaTranPhanQuyen.java` đặt mặc định `PhamViDuLieu.CO_SO`, trong khi `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql` (dòng 1066) đặt `DEFAULT 'TOAN_HE_THONG'`.

2. **Khế ước Repository (`domain.repositories`) và Lưu trữ (`clinic-infrastructure`):**
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/repositories/INhanVienRepository.java`: Khai báo `getByUsername(String)`, `getActiveDoctors(int maCoSo)`, `getRoleNames(int employeeId)`, `recordSuccessfulLogin(int employeeId, LocalDateTime loginAt)`. Đã được hiện thực tại `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepository.java`.
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/repositories/IHeThongRepository.java`: Chỉ khai báo duy nhất phương thức `List<MaTranPhanQuyen> getMatrixByRoleId(int roleId)`. Chưa có lớp triển khai JDBC nào trong `clinic-infrastructure` hiện thực `IHeThongRepository`.
   - `clinic-infrastructure/src/main/resources/migrations/V2__Seed_Initial_Data.sql` (dòng 48–59, 74–87): Seed 6 vai trò (`ADMIN`, `DOCTOR`, `RECEPTIONIST`, `PHARMACIST`, `CASHIER`, `LAB_TECH`) và gán `NhanVienVaiTro` cho 2 nhân viên mẫu, nhưng **không có bản ghi seed nào trong bảng `MaTranPhanQuyen`**.

3. **Thực thi tại tầng Application & Desktop (`clinic-application`, `clinic-desktop`):**
   - `clinic-application/src/main/java/vn/clinicmanagement/application/auth/AuthenticateNguoiDungHandler.java` (dòng 42–60): Gọi `employees.getRoleNames(employee.getId())` nhưng rút gọn danh sách vai trò bằng `roles.stream().filter(role -> role != null && !role.isBlank()).findFirst().orElse("")` và chỉ trả về một `roleCode` duy nhất trong `DaXacThucNguoiDung` (`clinic-application/src/main/java/vn/clinicmanagement/application/auth/DaXacThucNguoiDung.java`). Nếu một nhân viên kiêm nhiệm nhiều vai trò trong `NhanVienVaiTro`, các vai trò thứ hai trở đi bị bỏ qua.
   - `clinic-application/src/main/java/vn/clinicmanagement/application/common/Command.java`, `CommandHandler.java`, `Query.java`, `QueryHandler.java`: Các giao diện điều phối use case hiện chưa nhận ngữ cảnh người thực thi (actor/session context) và chưa có bước kiểm tra quyền (`MaTranPhanQuyen`) trước khi chạy handler.
   - `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainBangDieuKhienCuaSo.java` (dòng 35–40, 80–82):
     - Constructor mặc định không tham số `MainBangDieuKhienCuaSo()` vẫn gán cứng `this("admin", "Bác sĩ điều trị")`.
     - Biến `isDoctor` chỉ kiểm tra chuỗi hiển thị `role.contains("Bác sĩ") || role.contains("Doctor")`; mọi vai trò khác (`RECEPTIONIST`, `CASHIER`, `PHARMACIST`, `LAB_TECH`, `ADMIN`) đều rơi vào nhánh `buildAdminRibbonBar()` mở toàn bộ 12 nút phân hệ trên thanh Ribbon.
   - `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/system/HeThongCauHinhManHinh.java` (dòng 148–264): Chỉ hiển thị bảng danh mục chức năng tĩnh và bật `JOptionPane` thông báo khi nhấp đúp; chưa có màn hình hay kết nối dữ liệu để quản trị người dùng, vai trò hoặc ma trận phân quyền.

---

### 2.2. Phạm vi Cơ sở và Khoa phòng (`domain.facility`, `PhanQuyenCoSo`, `NhanVienKhoa`, `BenhNhanCoSo`)

1. **Thực thể và Schema:**
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/facility/CoSo.java`, `Khoa.java`, `PhongKham.java`: Biểu diễn phân cấp `CoSo` (1–n) `Khoa` (1–n) `PhongKham`.
     - *Điểm lệch enum:* `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/LoaiCoSo.java` chỉ có duy nhất 1 hằng số `PHONG_KHAM_DA_KHOA(3, "Phòng khám đa khoa")`, trong khi `DOMAIN_ANALYSIS.md` (Mục 2.5) liệt kê `PHONG_KHAM, BENH_VIEN, NHA_THUOC, TRUNG_TAM_XET_NGHIEM`, `V1__Initial_Clinic_Schema.sql` (dòng 17) đặt `DEFAULT 'PHONG_KHAM'`, và `V2__Seed_Initial_Data.sql` (dòng 14) seed `'BENH_VIEN'`.
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/identity/PhanQuyenCoSo.java` (`maNhanVien`, `maCoSo`) và `NhanVienKhoa.java` (`maNhanVien`, `maKhoa`): Bảng nối xác định cơ sở và khoa mà nhân viên được phép làm việc (`V1__Initial_Clinic_Schema.sql` dòng 153–186; `ERD_DIAGRAM.md` dòng 530–533).
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/patient/BenhNhanCoSo.java` (`maBenhNhan`, `maCoSo`, `ngayDangKy`): Bảng nối ghi nhận bệnh nhân đăng ký tại cơ sở (`V1__Initial_Clinic_Schema.sql` dòng 227–243; `ERD_DIAGRAM.md` dòng 17, 72–77).

2. **Persistence & Thực thi phạm vi dữ liệu (Scope Enforcement):**
   - Trong `clinic-infrastructure`, chỉ có `NhanVienRepository.getActiveDoctors(int maCoSo)` (dòng 149–160) là có `INNER JOIN PhanQuyenCoSo pq ON pq.maNhanVien=nv.id WHERE ... pq.maCoSo=?`.
   - Khi đăng nhập, `AuthenticateNguoiDungHandler` **không** truy vấn `PhanQuyenCoSo` hay `NhanVienKhoa`, nên không kiểm tra nhân viên có được phân quyền vào cơ sở/khoa đang hoạt động hay không, và `DaXacThucNguoiDung` không mang theo `facilityIds` hay `departmentIds`.
   - Tại `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/DangNhapBieuMau.java` (dòng 135–138), sau khi đăng nhập thành công, ứng dụng gọi `session.setDepartmentName("Chưa phân công")` và bỏ trống `facilityName`, khiến `TrangThaiThanhManHinh` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/components/TrangThaiThanhManHinh.java` dòng 40–46) hiển thị chuỗi mẫu cố định.
   - Các luồng đọc/ghi đã nối ở Phase 1 chưa áp dụng lọc theo phạm vi cơ sở/khoa của phiên đăng nhập:
     - `PhongKhamRepository.getActiveRooms()` (`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/facility/PhongKhamRepository.java` dòng 39–42) trả về tất cả phòng khám active của mọi `CoSo` và `Khoa`.
     - `TiepDonHangDoiRepository.getQueue(LocalDate)` và `getRoomSummaries(LocalDate)` (`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/TiepDonHangDoiRepository.java` dòng 85–128) truy vấn toàn bộ `LuotKham` và `PhongKham` theo ngày mà không lọc theo `maCoSo` hay `maKhoa`.
     - `RegisterBenhNhanHandler` (`clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterBenhNhanHandler.java` dòng 48–75) lấy `facilityId` và `departmentId` từ `PhongKham` được chọn nhưng không đối chiếu xem `createdBy` có quyền trong `PhanQuyenCoSo`/`NhanVienKhoa` tương ứng hay không, và cũng chưa ghi bản ghi vào bảng `BenhNhanCoSo`.
     - `BenhNhanRepository.search(String)` (`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/patient/BenhNhanRepository.java` dòng 64–73) tìm trên toàn bảng `BenhNhan` (`isDeleted = 0`).

---

### 2.3. Nhật ký hệ thống, Truy vết bệnh án và Sự kiện miền (`NhatKyHeThong`, `NhatKyHoSoBenhAn`, `CoSoThucThe`, Events)

1. **Thực thể, Sự kiện và Schema:**
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/CoSoThucThe.java`: Cung cấp `createdAt`, `createdBy`, `updatedAt`, `updatedBy`, `isDeleted`, `deletedAt`, `deletedBy`, `version` và danh sách `transient List<IMienNghiepVuSuKien> domainEvents`.
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/system/NhatKyHeThong.java` và bảng `NhatKyHeThong` (`V1__Initial_Clinic_Schema.sql` dòng 1080–1097): Gồm `maNhanVien` (nullable), `hanhDong`, `chiTiet`, `ipAddress`, `thoiGian` (kèm các cột `CoSoThucThe`).
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/system/NhatKyHoSoBenhAn.java` và bảng `NhatKyHoSoBenhAn` (`V1__Initial_Clinic_Schema.sql` dòng 1099–1117): Gồm `maLuotKham` (`NOT NULL`), `maNhanVien` (`NOT NULL`), `hanhDong`, `lyDo`, `thoiGian` (kèm các cột `CoSoThucThe`).
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/events/`: Định nghĩa `IMienNghiepVuSuKien`, `TruuTuongMienNghiepVuSuKien`, `IMienNghiepVuSuKienPhatHanh`, `IMienNghiepVuSuKienHandler` và 10 sự kiện nghiệp vụ (`BenhNhanDaDangKySuKien`, `LuotKhamDaBatDauSuKien`, `LuotKhamDaHoanTatSuKien`, `EMRDaKhoaSuKien`, `DonThuocDaDuyetSuKien`, `DonThuocDaCapPhatSuKien`, `HoaDonDaTaoSuKien`, `ThanhToanDaGhiNhanSuKien`, `LichHenDaDatSuKien`, `ThapTonKhoCanhBaoSuKien`).

2. **Khoảng trống Persistence và Enforcement:**
   - **Không có persistence cho bảng audit:** Không có interface hay class JDBC nào trong `clinic-domain`, `clinic-application`, `clinic-infrastructure` thực hiện `INSERT` hoặc `SELECT` trên hai bảng `NhatKyHeThong` và `NhatKyHoSoBenhAn`.
   - **Không có audit cho sự kiện xác thực:** `AuthenticateNguoiDungHandler` chỉ cập nhật cột `NhanVien.dangNhapCuoi` khi đăng nhập thành công (`recordSuccessfulLogin`), không ghi dòng mới vào `NhatKyHeThong` khi đăng nhập thành công, đăng nhập thất bại, tài khoản bị khóa/không có vai trò hoặc khi đăng xuất.
   - **Sự kiện miền chưa được phát hành:** Các phương thức miền như `LuotKham.startExam()`, `LuotKham.completeExam()`, `LuotKham.lockEmr(...)` (`clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java` dòng 88–115), `DonThuoc.approve()`, `DonThuoc.completeDispense(...)` (`DonThuoc.java` dòng 38–54) và `HoaDon.recordPayment(...)` (`HoaDon.java` dòng 49–68) gọi `registerDomainEvent(...)`, nhưng chưa có `IMienNghiepVuSuKienPhatHanh` thực thi và `JdbcUnitCuaWork` không đọc hay phát hành `domainEvents` khi commit transaction. Với `BenhNhan`, `BenhNhanDangKyBieuMauManHinh` khởi tạo bằng constructor rỗng `new BenhNhan()` thay vì constructor 9 tham số nên `BenhNhanDaDangKySuKien` cũng không được đăng ký.
   - **Truyền `createdBy`/`updatedBy` chưa được bảo vệ ở tầng Application:** Trong `TiepDonManHinh.saveRegistration()` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/TiepDonManHinh.java` dòng 143) và `BenhNhanDangKyBieuMauManHinh.toRegisterCommand(int userId)` (dòng 371–391, 412–419), `userId` lấy từ `NguoiDungPhien.getInstance().getUserId()`. Nếu `userId <= 0`, code chuyển thành `null` khi gán `createdBy`/`updatedBy`, và `RegisterBenhNhanHandler` không từ chối lệnh có `createdBy <= 0` cũng như không xác minh `createdBy` khớp với một phiên đăng nhập hợp lệ.
   - **Hộp thoại mở lại bệnh án (`ReopenYTeRecordHopThoai` / `ReopenYTeRecordManHinh`) mới chỉ dừng ở UI:** Tại `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/dialogs/ReopenYTeRecordManHinh.java` (dòng 375–406), nút Lưu chỉ kiểm tra ô `txtReason` không rỗng, gọi callback UI và bật `JOptionPane` báo thành công (với dữ liệu fallback cố định nếu `patient == null`), hoàn toàn chưa kiểm tra quyền của người yêu cầu, chưa mở khóa `LuotKham.emrLocked` trong DB và chưa ghi `NhatKyHoSoBenhAn`.

---

### 2.4. Quản lý phiên làm việc và Token (`NguoiDungPhien`, `MaXacThucLamMoi`, `ThamSoHeThong`, `CauHinhTichHop`)

1. **Thực thể và Hiện trạng:**
   - `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/NguoiDungPhien.java`: Lớp Singleton giữ trạng thái trên bộ nhớ client (`loggedOn`, `userId`, `username`, `fullName`, `role`, `facilityName`, `departmentName`, `stationId`, `workstationId`). Mọi thuộc tính đều có hàm `set...` công khai, bất kỳ đoạn mã nào trong cùng JVM đều có thể ghi đè `setRole(...)` hoặc `setUserId(...)`.
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/system/MaXacThucLamMoi.java`: Có phương thức `daHetHan()` (`LocalDateTime.now().isAfter(hetHan)`), lưu `maNhanVien`, `token`, `hetHan`, `daHuy`, `lyDoHuy`, `ipTao`, `thietBi`.
   - `clinic-domain/src/main/java/vn/clinicmanagement/domain/system/ThamSoHeThong.java` và seed `AUTO_LOGOUT_MINUTES = '30'` tại `V2__Seed_Initial_Data.sql` (dòng 149).

2. **Khoảng trống Schema và Thực thi:**
   - **Thiếu bảng trong migration SQL:** Hai thực thể `MaXacThucLamMoi` và `CauHinhTichHop` có file Java trong `clinic-domain/src/main/java/vn/clinicmanagement/domain/system/` và được mô tả trong `DOMAIN_ANALYSIS.md` (Mục 2.16), nhưng **không tồn tại câu lệnh `CREATE TABLE` trong `V1__Initial_Clinic_Schema.sql`..`V4`** (và cũng không có trong `ERD_DIAGRAM.md`).
   - **Chưa thực thi vòng đời phiên:**
     - `ThamSoHeThong` (`AUTO_LOGOUT_MINUTES`) không được đọc lên khi khởi động hay đăng nhập; ứng dụng không có bộ theo dõi thao tác bàn phím/chuột (`AWTEventListener`) để tự động khóa màn hình hoặc đăng xuất khi hết thời gian chờ.
     - Nút "Kết Thúc" trên `MainBangDieuKhienCuaSo` (dòng 148–153, 290–295) gọi thẳng `System.exit(0)` mà không có chức năng "Đăng xuất / Đổi người dùng" để gọi `NguoiDungPhien.getInstance().clearSession()`, ghi nhật ký và quay về `DangNhapBieuMau`.
     - Khi tài khoản bị vô hiệu hóa (`NhanVien.hoatDong = 0` hoặc `isDeleted = 1`) hoặc bị thu hồi vai trò trong CSDL sau thời điểm đăng nhập, phiên `NguoiDungPhien` đang mở trên máy trạm vẫn tiếp tục gửi `userId` cũ mà không bị chặn tại `RegisterBenhNhanHandler`, `SearchBenhNhanHandler` hay `GetTiepDonHangDoiHandler`.

---

## 3. Danh mục Quyết định Chính sách cần Owner chốt (Owner Policy Decisions)

> Các mục dưới đây là quyết định nghiệp vụ, bảo mật và vận hành thuộc thẩm quyền của Owner/PO. Tài liệu này liệt kê đầy đủ các phương án và hệ quả kỹ thuật để Owner quyết định, **tuyệt đối không tự suy diễn hoặc tự chọn thay Owner**.

### 3.1. Ma trận Vai trò và Quyền hạn (`VaiTro`, `MaTranPhanQuyen`, `HanhDongQuyen`)

1. **QD-ROLE-01 — Danh mục vai trò chuẩn và cơ chế xử lý đa vai trò (`NhanVienVaiTro`):**
   - *Hiện trạng:* `V2__Seed_Initial_Data.sql` có 6 vai trò (`ADMIN`, `DOCTOR`, `RECEPTIONIST`, `PHARMACIST`, `CASHIER`, `LAB_TECH`), trong khi `DOMAIN_ANALYSIS.md` (Mục 2.14) nhắc tới `NURSE` và `ChucVu` có `Điều dưỡng viên`, `Trưởng khoa`, `Giám đốc chuyên môn`. `AuthenticateNguoiDungHandler` đang chỉ lấy vai trò đầu tiên (`findFirst()`).
   - *Câu hỏi cần Owner quyết định:*
     - Danh mục `VaiTro` chính thức có bổ sung `NURSE` (Điều dưỡng) hoặc `HEAD_DOCTOR` (Trưởng khoa) độc lập với `ChucVu` hay không?
     - Khi một nhân viên được gán nhiều vai trò trong `NhanVienVaiTro`, hệ thống sẽ: **(a)** hợp nhất (union) toàn bộ quyền của các vai trò đang active, hay **(b)** yêu cầu người dùng chọn 1 vai trò làm việc cụ thể lúc đăng nhập / chuyển đổi vai trò trên thanh trạng thái?
2. **QD-ROLE-02 — Chuẩn hóa từ điển Phân hệ (`phanHe`) và Hành động (`HanhDongQuyen`) trong `MaTranPhanQuyen`:**
   - *Hiện trạng:* `HanhDongQuyen.java` chỉ có `XEM`, `TAO`, `SUA`, `XOA`, `DUYET`, khác với `DOMAIN_ANALYSIS.md` (có thêm `IN`, `XUAT_FILE` và dùng `THEM`). Bảng `MaTranPhanQuyen` hiện trống.
   - *Câu hỏi cần Owner quyết định:*
     - Bộ mã `phanHe` chuẩn (ví dụ: `RECEPTION`, `PATIENT`, `APPOINTMENT`, `EMR`, `CLS`, `PRESCRIPTION`, `INVENTORY`, `BILLING`, `HR_PAYROLL`, `CATALOG`, `REPORT`, `SYSTEM`, `AUDIT`) gồm những giá trị nào?
     - Có tách riêng quyền `IN` (in phiếu), `XUAT_FILE` (xuất DOCX/Excel), `KY_SO` (ký khóa EMR), `MO_KHOA_EMR` (mở lại bệnh án đã khóa) và `HUY_CHUNG_TU` (hủy phiếu thu/phiếu xuất/chỉ định) trong `HanhDongQuyen` hay gộp vào `SUA`/`DUYET`/`XOA`?
     - Ma trận cho phép/từ chối (`duocPhep`) cụ thể cho từng cặp `(VaiTro × phanHe × HanhDongQuyen)` là gì?
3. **QD-ROLE-03 — Thẩm quyền phê duyệt và thao tác nhạy cảm (Override / Dual-Control):**
   - *Câu hỏi cần Owner quyết định:*
     - Ai có quyền mở lại hồ sơ bệnh án đã khóa (`LuotKham.emrLocked = true`): chính bác sĩ đã khóa trong cùng ngày, Trưởng khoa, hay chỉ `ADMIN`? Có bắt buộc xác thực lại mật khẩu của người phê duyệt trên `ReopenYTeRecordHopThoai` (như gợi ý tại Mục 5.2.4 của `UI_CODING_STANDARDS.md`) hay không?
     - Trong luồng kê đơn (`DonThuoc`), bác sĩ kê đơn có tự `approve()` đơn của mình hay cần vai trò Dược sĩ lâm sàng duyệt trước khi cấp phát?
     - Ai có quyền duyệt biên bản kiểm kê kho (`KiemKe`), hủy phiếu xuất kho (`PhieuXuatKho`), miễn giảm viện phí (`soTienGiamGia`, `chietKhauPhanTram`) và hoàn trả/hủy phiếu thu (`PhieuThu`)?

---

### 3.2. Phạm vi Cơ sở và Khoa phòng (`PhamViDuLieu`, `PhanQuyenCoSo`, `NhanVienKhoa`, `BenhNhanCoSo`)

1. **QD-SCOPE-01 — Chuẩn hóa các mức `PhamViDuLieu`:**
   - *Hiện trạng:* `PhamViDuLieu.java` có `TOAN_HE_THONG`, `CO_SO`, `KHOA_PHONG`, `CHI_NHANH`, còn `DOMAIN_ANALYSIS.md` ghi `TOAN_HE_THONG`, `THEO_CO_SO`, `THEO_KHOA`, `CA_NHAN`.
   - *Câu hỏi cần Owner quyết định:*
     - Có giữ cả `CO_SO` và `CHI_NHANH` hay hợp nhất thành một mức Cơ sở (`CO_SO`)?
     - Có bổ sung mức `CA_NHAN` (ví dụ: bác sĩ chỉ được sửa/khóa lượt khám hoặc xem bảng lương của chính mình) vào `PhamViDuLieu` hay không?
     - Giá trị mặc định khi tạo dòng `MaTranPhanQuyen` phải là `CO_SO` (như code Java) hay `TOAN_HE_THONG` (như default cột SQL `V1`) — ưu tiên nguyên tắc đặc quyền tối thiểu (least privilege)?
2. **QD-SCOPE-02 — Quy tắc chia sẻ Hồ sơ bệnh nhân (`BenhNhan`, `BaoHiem`) so với cô lập Lượt khám / Kho / Viện phí theo `CoSo` và `Khoa`:**
   - *Hiện trạng:* `V4__Enforce_Active_Patient_CCCD_Uniqueness.sql` áp đặt tính duy nhất của CCCD active trên toàn bảng `BenhNhan` (toàn hệ thống).
   - *Câu hỏi cần Owner quyết định:*
     - Hồ sơ hành chính `BenhNhan` và `BaoHiem` được tra cứu/cập nhật liên cơ sở (toàn chuỗi phòng khám) để tránh trùng CCCD, hay chỉ hiển thị cho nhân viên thuộc cơ sở đã liên kết trong `BenhNhanCoSo`? Khi tiếp đón bệnh nhân cũ tại cơ sở mới, hệ thống có tự động ghi thêm dòng vào `BenhNhanCoSo` hay không?
     - Hàng đợi tiếp đón (`TiepDonHangDoiCongKetNoi`), danh sách phòng khám (`IPhongKhamRepository.getActiveRooms`), lượt khám (`LuotKham`), tồn kho (`Kho`, `TonKho`) và hóa đơn (`HoaDon`) phải lọc bắt buộc theo `maCoSo` (từ `PhanQuyenCoSo`) và/hoặc `maKhoa` (từ `NhanVienKhoa`) của người dùng đang đăng nhập như thế nào?
3. **QD-SCOPE-03 — Chọn Cơ sở / Khoa / Phòng làm việc khi đăng nhập:**
   - *Câu hỏi cần Owner quyết định:*
     - Nếu nhân viên được gán nhiều cơ sở trong `PhanQuyenCoSo` hoặc nhiều khoa trong `NhanVienKhoa`, màn hình `DangNhapBieuMau` có bắt buộc người dùng chọn 1 Cơ sở + 1 Khoa/Phòng làm việc cho phiên hiện tại (sau khi xác thực mật khẩu) hay không?
     - Tài khoản không có bản ghi active nào trong `PhanQuyenCoSo` (trừ trường hợp có quyền `TOAN_HE_THONG` nếu Owner cho phép) có bị từ chối đăng nhập giống như trường hợp không có `VaiTro` (`AUTH_NO_ROLE`) hay không?

---

### 3.3. Sự kiện Kiểm toán, Lưu trữ, Ẩn dữ liệu nhạy cảm và Quyền truy cập Audit (`NhatKyHeThong`, `NhatKyHoSoBenhAn`)

1. **QD-AUDIT-01 — Danh mục sự kiện bắt buộc ghi vào `NhatKyHeThong` và `NhatKyHoSoBenhAn`:**
   - *Câu hỏi cần Owner quyết định:*
     - **`NhatKyHeThong`:** Xác nhận danh sách hành động bắt buộc ghi log ở Phase 2 và các phase tiếp theo:
       - Xác thực & Phiên: `AUTH_LOGIN_SUCCESS`, `AUTH_LOGIN_FAILED`, `AUTH_LOGOUT`, `AUTH_SESSION_EXPIRED`, `AUTH_ACCESS_DENIED`.
       - Quản trị quyền: tạo/khóa/mở khóa `NhanVien`, đổi mật khẩu, thay đổi `NhanVienVaiTro`, `PhanQuyenCoSo`, `NhanVienKhoa`, `MaTranPhanQuyen`, `ThamSoHeThong`.
       - Nghiệp vụ nhạy cảm: tạo/cập nhật `BenhNhan` & `BaoHiem`, tạo/hủy `LuotKham`, duyệt/hủy `DonThuoc`, xuất/nhập/kiểm kê kho (`PhieuNhapKho`, `PhieuXuatKho`, `KiemKe`), lập hóa đơn/thu tiền/hoàn tiền/miễn giảm (`HoaDon`, `PhieuThu`).
     - **`NhatKyHoSoBenhAn`:** Xác nhận các thao tác bắt buộc ghi truy vết gắn với `maLuotKham`: mở xem hồ sơ bệnh án (`VIEW_EMR`), lưu/cập nhật bệnh án (`UPDATE_EMR`), ký/khóa EMR (`LOCK_EMR`), yêu cầu/phê duyệt mở lại EMR (`REOPEN_EMR` kèm `lyDo` bắt buộc), in/xuất file bệnh án hoặc đơn thuốc (`PRINT_EMR`, `EXPORT_EMR`).
2. **QD-AUDIT-02 — Tính nguyên tử (Transaction Boundary) khi ghi Audit:**
   - *Câu hỏi cần Owner quyết định:*
     - Với các thao tác làm thay đổi dữ liệu nghiệp vụ (như đăng ký khám, khóa/mở khóa EMR, cấp phát thuốc, thu tiền), bản ghi `NhatKyHeThong` / `NhatKyHoSoBenhAn` có bắt buộc nằm **chung trong một transaction DB** (nếu ghi audit lỗi thì rollback toàn bộ thao tác nghiệp vụ — fail-closed) hay không?
     - Với các sự kiện thất bại (`AUTH_LOGIN_FAILED`, `AUTH_ACCESS_DENIED`), bản ghi `NhatKyHeThong` phải được ghi trong một transaction độc lập (commit ngay cả khi use case trả về `KetQua.failure`) như thế nào? Với `AUTH_LOGIN_FAILED` khi `username` không tồn tại trong bảng `NhanVien`, cột `maNhanVien` (`INT NULL`) để `NULL` và định danh tài khoản thử đăng nhập được ghi ẩn danh/giới hạn trong `chiTiet` ra sao để không lộ mật khẩu gõ nhầm vào ô username?
3. **QD-AUDIT-03 — Định dạng `chiTiet`, Quy tắc che giấu dữ liệu (Redaction) và Định danh máy trạm (`ipAddress`):**
   - *Câu hỏi cần Owner quyết định:*
     - Cấu trúc trường `chiTiet` trong `NhatKyHeThong` (JSON chuẩn hay chuỗi key-value chứa `entityType`, `entityId`, `beforeVersion`, `afterVersion`, `changedFields`) cần lưu đến mức nào: chỉ lưu tên trường thay đổi và mã bản ghi hay lưu giá trị cũ/mới?
     - Quy tắc che giấu (masking/redaction): Tuyệt đối cấm ghi `password`, `passwordHash`, `token`, `apiKey`, `secretKey`, chuỗi kết nối DB; với dữ liệu định danh bệnh nhân (`soCCCD`, `soBaoHiem`, `dienThoai`), có bắt buộc che một phần (ví dụ chỉ giữ 3–4 ký tự cuối) trong `NhatKyHeThong.chiTiet` hay không?
     - Trên ứng dụng Swing Desktop kết nối JDBC, trường `ipAddress` (và mã trạm) sẽ lấy từ địa chỉ IP LAN của máy trạm + hostname hay biến cấu hình mã quầy/máy trạm?
4. **QD-AUDIT-04 — Thời hạn lưu trữ (Retention), Tính bất biến (Immutability) và Quyền tra cứu (`AUD-01`):**
   - *Hiện trạng:* Cả `NhatKyHeThong` và `NhatKyHoSoBenhAn` đều kế thừa `CoSoThucThe` nên trong bảng SQL (`V1__Initial_Clinic_Schema.sql` dòng 1088–1095, 1107–1114) có các cột `updatedAt`, `updatedBy`, `isDeleted`, `deletedAt`, `deletedBy`.
   - *Câu hỏi cần Owner quyết định:*
     - Nhật ký kiểm toán có bắt buộc là **chỉ ghi thêm (append-only / immutable)**, nghiêm cấm mọi lệnh `UPDATE`, `DELETE` và xóa mềm (`softDelete`) từ ứng dụng hay không?
     - Thời hạn lưu trữ tối thiểu đối với `NhatKyHeThong` và `NhatKyHoSoBenhAn` là bao lâu (theo quy định lưu trữ hồ sơ bệnh án ngoại trú/nội trú)?
     - Vai trò nào được phép mở màn hình tra cứu `NhatKyHeThong` và `NhatKyHoSoBenhAn` (`AUD-01`), và người tra cứu có bị giới hạn theo `maCoSo` hay không?

---

### 3.4. Vòng đời Phiên làm việc, Chính sách Mật khẩu và Phạm vi `MaXacThucLamMoi` (`NguoiDungPhien`, `MaXacThucLamMoi`, `ThamSoHeThong`)

1. **QD-SESS-01 — Kiến trúc phiên làm việc trên Desktop và số phận của model `MaXacThucLamMoi`:**
   - *Hiện trạng:* `docs/ke-hoach-trien-khai/phan-he/08-co-so-bao-mat-cau-hinh.md` (dòng 31) và `03-model-task-board.md` (`SYS-01`) nêu rõ cần xác nhận `MaXacThucLamMoi` có dùng cho desktop hay không. Hiện `MaXacThucLamMoi` chưa có bảng trong `V1`–`V4`, còn `NguoiDungPhien` chỉ là biến tĩnh trong bộ nhớ.
   - *Câu hỏi cần Owner quyết định:*
     - **Phương án A:** Dùng `MaXacThucLamMoi` (hoặc bảng phiên đăng nhập tương đương trong migration mới `V5`) để lưu phiên làm việc phía CSDL khi đăng nhập; mỗi `Command`/`Query` truyền kèm mã phiên/ngữ cảnh đã ký hoặc tra cứu trạng thái phiên trên DB thay vì tin tưởng `NguoiDungPhien` có setter mở.
     - **Phương án B:** Loại `MaXacThucLamMoi` khỏi phạm vi ứng dụng Swing kết nối JDBC trực tiếp (chỉ giữ cho cổng API/OAuth tương lai nếu có), nhưng phải khóa chặt `NguoiDungPhien` (bỏ các public setter rời rạc, chỉ khởi tạo qua đối tượng phiên bất biến do tầng Application cấp và kiểm tra lại trạng thái `NhanVien.hoatDong` + quyền tại `CommandHandler`/`QueryHandler`).
2. **QD-SESS-02 — Tự động khóa/đăng xuất (`AUTO_LOGOUT_MINUTES`), Đăng xuất chủ động và Thu hồi phiên tức thì:**
   - *Câu hỏi cần Owner quyết định:*
     - Khi hết thời gian không tương tác theo tham số `AUTO_LOGOUT_MINUTES` (đang seed là 30 phút trong `ThamSoHeThong`), ứng dụng sẽ: **(a)** bật hộp thoại khóa màn hình yêu cầu nhập lại mật khẩu của tài khoản hiện tại (giữ nguyên các tab đang mở), hay **(b)** xóa sạch `NguoiDungPhien`, đóng toàn bộ tab `MainBangDieuKhienCuaSo` và quay về `DangNhapBieuMau`? Nếu người dùng đang nhập dở form tiếp đón hoặc bệnh án nháp thì xử lý dữ liệu chưa lưu thế nào?
     - Khi quản trị viên khóa tài khoản (`NhanVien.hoatDong = 0`) hoặc thu hồi vai trò/cơ sở của một nhân viên đang đăng nhập trên máy trạm khác, việc chặn thao tác phải có hiệu lực ngay ở **mọi lệnh `Command`/`Query` kế tiếp** tại tầng Application hay kiểm tra định kỳ?
3. **QD-SESS-03 — Chính sách mật khẩu và Chống dò mật khẩu (Brute-force Lockout):**
   - *Câu hỏi cần Owner quyết định:*
     - Ngưỡng khóa tạm thời khi nhập sai mật khẩu liên tiếp là bao nhiêu lần (ví dụ 5 lần như nêu trong `UI_CODING_STANDARDS.md` Mục 4.2) và thời gian khóa là bao lâu (hoặc phải do `ADMIN` mở khóa)? Cần bổ sung các cột nào vào `NhanVien` (ví dụ `failedLoginAttempts`, `lockedUntil`, `mustChangePassword`) qua migration mới?
     - Quy tắc độ mạnh mật khẩu khi tạo mới/đổi mật khẩu và cơ chế bắt buộc đổi mật khẩu lần đầu đối với tài khoản seed/reset là gì?

---

## 4. Các Kiểm thử Kỹ thuật Phụ thuộc vào Quyết định của Owner (Dependent Technical Tests)

Mọi kiểm thử dưới đây bắt buộc chỉ dùng **dữ liệu tổng hợp (synthetic fixtures)**, không dùng dữ liệu bệnh nhân hay thông tin định danh thật, và là điều kiện nghiệm thu kỹ thuật cho `SEC-01` và `AUD-01` trước khi mở Phase 4, Phase 6 và Phase 7:

| Mã nhóm kiểm thử | Phụ thuộc vào quyết định Owner | Loại kiểm thử & Vị trí module dự kiến | Hành vi kỹ thuật cần chứng minh bằng test |
|---|---|---|---|
| **`TEST-SEC-ROLE-01`**<br>Xác thực đa vai trò & chuẩn hóa phiên | `QD-ROLE-01`, `QD-SCOPE-03`, `QD-SESS-01` | Unit / Application test (`clinic-application`: mở rộng `AuthenticateNguoiDungHandlerTest`) | - Tài khoản synthetic có nhiều vai trò trong `NhanVienVaiTro` và nhiều cơ sở/khoa trong `PhanQuyenCoSo`/`NhanVienKhoa` nhận đúng tập quyền và phạm vi theo policy đã chốt (không bị rơi mất vai trò thứ hai do `findFirst()`).<br>- Từ chối đăng nhập nếu tài khoản không có phạm vi cơ sở hợp lệ (theo `QD-SCOPE-03`). |
| **`TEST-SEC-AUTHZ-01`**<br>Cưỡng chế Ma trận phân quyền tại Application Boundary | `QD-ROLE-01`, `QD-ROLE-02`, `QD-ROLE-03` | Unit / Application test (`clinic-application`) | - Với mỗi `Command`/`Query` nhạy cảm (tiếp đón, xem/sửa/khóa/mở khóa EMR, kê/duyệt/cấp phát đơn thuốc, nhập/xuất/kiểm kê kho, lập hóa đơn/thu/hoàn tiền, cấu hình hệ thống): kiểm thử **Allow** khi vai trò có `MaTranPhanQuyen.duocPhep = true` và **Deny** (`AUTH_FORBIDDEN` / `AUTH_UNAUTHORIZED`) khi không có quyền hoặc `userId <= 0`.<br>- Chứng minh việc ẩn/hiện nút trên `MainBangDieuKhienCuaSo` không thay thế kiểm tra ở handler: gọi trực tiếp handler bằng actor không đủ quyền phải bị chặn. |
| **`TEST-SEC-SCOPE-01`**<br>Cô lập phạm vi dữ liệu theo Cơ sở & Khoa (`PhanQuyenCoSo`, `NhanVienKhoa`) | `QD-SCOPE-01`, `QD-SCOPE-02`, `QD-SCOPE-03` | Application test & Opt-in SQL Server integration test (`clinic-application`, `clinic-infrastructure`) | - Tạo 2 cơ sở synthetic (`CoSo A`, `CoSo B`) và 2 khoa (`Khoa 1`, `Khoa 2`).<br>- Nhân viên chỉ được gán `PhanQuyenCoSo` tại `CoSo A` khi gọi `getActiveRooms()`, `GetTiepDonHangDoiHandler`, hoặc tạo/cập nhật lượt khám/đơn thuốc/hóa đơn tại `CoSo B` phải bị từ chối hoặc chỉ nhận dữ liệu thuộc `CoSo A`.<br>- Kiểm tra hành vi chia sẻ/cô lập tìm kiếm `BenhNhan` và tự động ghi `BenhNhanCoSo` đúng theo `QD-SCOPE-02`. |
| **`TEST-SEC-EMR-LOCK-01`**<br>Quyền khóa và mở lại Bệnh án điện tử (`LuotKham.emrLocked`) | `QD-ROLE-03`, `QD-AUDIT-01`, `QD-AUDIT-02` | Domain / Application / Infrastructure test (`clinic-domain`, `clinic-application`, `clinic-infrastructure`) | - Sau khi `LuotKham.lockEmr(...)` được lưu, mọi lệnh sửa chẩn đoán, sinh hiệu, chỉ định, đơn thuốc hoặc thông tin khám trên lượt khám đó đều bị chặn.<br>- Lệnh mở lại bệnh án (`ReopenMedicalRecord`) bị từ chối nếu actor không thuộc vai trò được Owner cấp quyền hoặc thiếu `lyDo`.<br>- Khi mở lại hợp lệ, trạng thái khóa và bản ghi `NhatKyHoSoBenhAn` được ghi nguyên tử trong cùng transaction. |
| **`TEST-AUD-WRITE-01`**<br>Ghi nhận `NhatKyHeThong` & `NhatKyHoSoBenhAn` và tính nguyên tử | `QD-AUDIT-01`, `QD-AUDIT-02` | Application test & Opt-in SQL Server integration test (`clinic-application`, `clinic-infrastructure`) | - Đăng nhập thành công, đăng nhập thất bại (`AUTH_INVALID`), bị từ chối quyền (`AUTH_FORBIDDEN`) và đăng xuất đều tạo bản ghi `NhatKyHeThong` đúng `maNhanVien`, `hanhDong`, `thoiGian`.<br>- Khi mô phỏng lỗi ghi `NhatKyHeThong` / `NhatKyHoSoBenhAn` trong một giao dịch nghiệp vụ (ví dụ đăng ký bệnh nhân hoặc khóa/mở khóa EMR), toàn bộ giao dịch nghiệp vụ được rollback nếu Owner chốt cơ chế fail-closed. |
| **`TEST-AUD-REDACT-01`**<br>Không lộ Secret/PII trong Audit Log và Thông báo lỗi | `QD-AUDIT-03` | Unit / Application / Infrastructure test (`clinic-application`, `clinic-infrastructure`) | - Kiểm tra nội dung `NhatKyHeThong.chiTiet` và `NhatKyHoSoBenhAn.lyDo` trong mọi nhánh thành công/thất bại không bao giờ chứa mật khẩu (`char[]` / chuỗi), `passwordHash`, `token`, `apiKey`, `secretKey` hoặc chuỗi kết nối JDBC, và áp dụng đúng quy tắc che `soCCCD`/`soBaoHiem` theo `QD-AUDIT-03`. |
| **`TEST-AUD-IMMUTABLE-01`**<br>Tính bất biến và Phân quyền tra cứu Audit (`AUD-01`) | `QD-AUDIT-04` | Application & Infrastructure test (`clinic-application`, `clinic-infrastructure`) | - Repository/use case của `NhatKyHeThong` và `NhatKyHoSoBenhAn` chỉ cho phép thêm mới và truy vấn chỉ đọc (read-only) theo bộ lọc (`maNhanVien`, `maLuotKham`, `hanhDong`, khoảng thời gian); không cho phép sửa hoặc xóa mềm.<br>- Chỉ vai trò được Owner chỉ định trong `QD-AUDIT-04` mới gọi được query tra cứu audit. |
| **`TEST-SESS-LIFECYCLE-01`**<br>Hết hạn phiên, Khóa tài khoản và Thu hồi quyền tức thì | `QD-SESS-01`, `QD-SESS-02`, `QD-SESS-03` | Application & Headless Desktop test (`clinic-application`, `clinic-desktop`) | - Kiểm thử khóa tài khoản sau `N` lần đăng nhập sai liên tiếp với mật khẩu synthetic.<br>- Khi tài khoản bị chuyển `hoatDong = false` hoặc bị thu hồi vai trò/cơ sở trong DB giữa phiên làm việc, lệnh `Command`/`Query` tiếp theo từ phiên đó bị từ chối ngay tại tầng Application.<br>- Kiểm thử cơ chế tự động khóa/đăng xuất khi vượt quá `AUTO_LOGOUT_MINUTES` và luồng đăng xuất chủ động xóa sạch `NguoiDungPhien`. |

---

## 5. Kết luận rà soát cho lộ trình triển khai

1. **Không xem việc có sẵn class/bảng là đã hoàn thành Phase 2:** Mặc dù `clinic-domain` và `V1__Initial_Clinic_Schema.sql` đã khai báo các lớp và bảng cho `NhanVien`, `VaiTro`, `NhanVienVaiTro`, `PhanQuyenCoSo`, `NhanVienKhoa`, `MaTranPhanQuyen`, `NhatKyHeThong`, `NhatKyHoSoBenhAn`, hiện chỉ có nhánh đăng nhập cơ bản (`AuthenticateNguoiDungHandler` + `NhanVienRepository` + `PBKDF2MatKhauBoBam`) là có persistence và unit test. Toàn bộ phần cưỡng chế phân quyền theo vai trò/phạm vi cơ sở, quản lý vòng đời phiên và ghi nhận nhật ký kiểm toán vẫn chưa được hiện thực hóa.
2. **Thứ tự thực hiện sau khi Owner chốt quyết định:**
   - **Bước 1:** Owner chốt 4 nhóm quyết định (`QD-ROLE-01..03`, `QD-SCOPE-01..03`, `QD-AUDIT-01..04`, `QD-SESS-01..03`).
   - **Bước 2:** Đồng bộ các enum/schema còn lệch (`HanhDongQuyen`, `PhamViDuLieu`, `LoaiCoSo`, bảng/cột cho phiên hoặc khóa tài khoản nếu Owner chọn) qua migration mới tuần tự sau `V4`, kèm seed chuẩn cho `MaTranPhanQuyen`.
   - **Bước 3:** Hiện thực hóa persistence cho `IHeThongRepository` / Audit repository, bổ sung ngữ cảnh phiên và kiểm tra quyền/phạm vi tập trung tại tầng `clinic-application`, nối ghi audit trong transaction và cập nhật `NguoiDungPhien` / `MainBangDieuKhienCuaSo` / `DangNhapBieuMau`.
   - **Bước 4:** Bổ sung và chạy bộ kiểm thử synthetic `TEST-SEC-*`, `TEST-AUD-*`, `TEST-SESS-*` để nghiệm thu `SEC-01` và `AUD-01` trước khi mở các công việc `EMR-01`, `RX-01`, `INV-02`..`INV-07` và `BILL-01`..`BILL-02`.
