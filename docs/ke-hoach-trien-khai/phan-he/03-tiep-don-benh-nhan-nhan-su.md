# Phân hệ 03 — Tiếp đón, hồ sơ bệnh nhân và nhân sự

**Trạng thái:** `[~] Đang chuẩn bị kết nối DB` (Schema CSDL SQL Server đã có sẵn các bảng `BenhNhan`, `BaoHiem`, `BenhNhanCoSo`, `TienSuBenh`, `LuotKham`, `SinhHieu`, `NhanVien`)  
**Đợt:** 1 (Bệnh nhân & Tiếp đón), sau đó nhân sự mở rộng ở Đợt 7.  
**Domain:** `patient`, `identity`, `facility`, `encounter`.

---

## 1. Hiện có

- **Models Bệnh nhân (Đã nâng cấp):** `BenhNhan`, `BenhNhanCoSo`, `BaoHiem`, `TienSuBenh`, `DoiTuongKham` (Enum mới).
- **Models Nhân sự & Cơ sở:** `NhanVien`, `ChucVu`, `Khoa`, `PhongKham`, `CoSo`, `NhanVienKhoa`, `NhanVienVaiTro`, `VaiTro`.
- **CSDL SQL Server:** Các bảng quan hệ tương ứng đã được khởi tạo và kiểm tra ràng buộc toàn vẹn qua Migration V1 & V2.
- **UI Desktop:** `ReceptionPanel`, `PatientRegistrationFormPanel`, `ClinicRoomStatusPanel`, `ReceptionTopToolbar`, `ReceptionFooterBar`.

---

## 2. Chi tiết 15 Trường Y Tế Form Đăng Ký Cần Nối Vào Domain & CSDL

Màn hình `PatientRegistrationFormPanel` cần đọc và lưu đầy đủ 15 trường y tế thực chiến:

1. **`doiTuongKham`** ➔ `LuotKham.doiTuongKham` (Enum: `BHYT`, `VIEN_PHI`, `YEU_CAU`, `MIEN_PHI`).
2. **`lyDoKham`** ➔ `LuotKham.lyDoKham` (Lý do vào viện/triệu chứng ban đầu).
3. **`maDkbd`** ➔ `BaoHiem.maDkbd` (Mã cơ sở đăng ký khám chữa bệnh ban đầu).
4. **`mucHuong`** ➔ `BaoHiem.mucHuong` (Mức hưởng bảo hiểm: 80%, 95%, 100%).
5. **`ngayDu5Nam`** ➔ `BaoHiem.ngayDu5Nam` (Ngày miễn cùng chi trả theo luật BHYT).
6. **`dungTuyen`** ➔ `BaoHiem.dungTuyen` (Trạng thái đúng tuyến / thông tuyến).
7. **`nguoiLienHe`** ➔ `BenhNhan.nguoiLienHe` (Họ tên người thân/giám hộ).
8. **`moiQuanHe`** ➔ `BenhNhan.moiQuanHeNguoiLienHe` (Quan hệ: Bố/Mẹ/Vợ/Chồng/Khác).
9. **`soDienThoaiNguoiLienHe`** ➔ `BenhNhan.soDienThoaiNguoiLienHe`.
10. **`ngheNghiep`** ➔ `BenhNhan.ngheNghiep`.
11. **`danToc`** ➔ `BenhNhan.danToc` (Mặc định: "Kinh").
12. **`quocTich`** ➔ `BenhNhan.quocTich` (Mặc định: "Việt Nam").
13. **`diaChi`** ➔ `BenhNhan.diaChi` (Số nhà, tổ dân phố, đường).
14. **`phuongXa`, `quanHuyen`, `tinhThanh`** ➔ `BenhNhan` (Địa chỉ hành chính chuẩn).
15. **`maThe`** ➔ `BaoHiem.maThe` (Mã số thẻ 15 ký tự).

---

## 3. Backlog Kỹ Thuật Tầng Application & Infrastructure

- [ ] **`clinic-application` Use Cases:**
  - `RegisterPatientCommand` & `RegisterPatientCommandHandler`: Tiếp nhận thông tin từ form, validate ngày sinh, CCCD, lưu `BenhNhan` và `BaoHiem` trong một transaction.
  - `SearchPatientsQuery` & `SearchPatientsQueryHandler`: Tìm kiếm đa tiêu chí (Mã BN, CCCD, Điện thoại, Họ tên) trả về `List<PatientSummaryDto>`.
  - `CreateEncounterCommand`: Tiếp nhận vào hàng đợi phòng khám, cấp số thứ tự khám tăng dần trong ngày.
- [ ] **`clinic-infrastructure` Repositories:**
  - `BenhNhanRepositoryImpl`: Thao tác CRUD bảng `BenhNhan`, tìm kiếm tối ưu bằng Index trên `cccd`, `soDienThoai`.
  - `BaoHiemRepositoryImpl`: Thao tác bảng `BaoHiem`.
  - `LuotKhamRepositoryImpl`: Thao tác bảng `LuotKham`, sinh mã lượt khám `LK-yyMMdd-XXXX`.
  - `PhongKhamRepositoryImpl`: Truy vấn danh sách phòng khám đang hoạt động từ bảng `PhongKham`.
- [ ] **Tích hợp giao diện `clinic-desktop`:**
  - Áp dụng `SwingWorker` khi nhấn nút "Lưu hồ sơ" hoặc "Tìm kiếm" để hiển thị loading spinner, không làm đơ giao diện.
  - Tự động điền dữ liệu khi quét mã QR thẻ BHYT / CCCD gắn chip (nếu có thiết bị).

---

## 4. Nghiệm thu

- [ ] Đăng ký bệnh nhân thành công, thông tin được lưu vĩnh viễn vào SQL Server bảng `BenhNhan` và `BaoHiem`.
- [ ] Khởi động lại ứng dụng, tra cứu theo CCCD hoặc SĐT tìm lại được đúng bệnh nhân.
- [ ] Tiếp nhận bệnh nhân vào phòng khám: Bệnh nhân lập tức hiển thị trên bảng hàng đợi `ReceptionPanel` với số thứ tự duy nhất.
- [ ] Cảnh báo trùng lặp khi nhập CCCD đã tồn tại trong hệ thống.
