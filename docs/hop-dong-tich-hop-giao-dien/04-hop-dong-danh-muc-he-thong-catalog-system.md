# HỢP ĐỒNG 04: DANH MỤC DÙNG CHUNG & QUẢN TRỊ HỆ THỐNG (CATALOG & SYSTEM MODULE)
**Mã hợp đồng:** HD-INTEG-04 | **Trạng thái:** Sẵn sàng triển khai

---

## 1. MỤC TIÊU TRIỂN KHAI
1. Nâng cấp phân hệ **Quản Lý Danh Mục (`CatalogPanel`)**: Từ bảng tra cứu ICD-10 chỉ đọc thành trung tâm quản trị toàn bộ danh mục viện phí, thuốc, vật tư và cơ sở vật chất.
2. Nâng cấp phân hệ **Quản Lý Hệ Thống (`SystemConfigPanel`)**: Từ cây danh sách chuỗi tĩnh sang kết nối với `SystemRepository` để lưu trữ và nạp các tham số cấu hình bệnh viện.

---

## 2. MA TRẬN ĐẤU NỐI MÀN HÌNH - NGHIỆP VỤ - CSDL

### 2.1. Quản Trị Danh Mục Dùng Chung (`CatalogPanel`)
- **Điểm yếu hiện tại:** Chỉ có một bảng SELECT ICD cứng từ CSDL.
- **Cam kết hợp đồng:**
  - Bổ sung thanh chọn danh mục trên cùng (Tabbed / Dropdown):
    1. *Danh mục Bệnh ICD-10 (BYT)*: Tìm kiếm theo mã, tên bệnh, chương bệnh. Bổ sung nút [Thêm mới], [Sửa], [Khóa].
    2. *Danh mục Dịch vụ Kỹ thuật*: Nạp từ bảng `DanhMucDichVu` (Đơn giá, nhóm dịch vụ, tỷ lệ BHYT thanh toán). Bổ sung tính năng CRUD cập nhật bảng giá viện phí.
    3. *Danh mục Thuốc & Hoạt chất*: Nạp từ bảng `Item` (Mã thuốc, hoạt chất, nồng độ/hàm lượng, đường dùng, đơn vị tính).
    4. *Danh mục Phòng khám & Khoa phòng*: Nạp từ bảng `Khoa`, `PhongKham`.

### 2.2. Quản Trị Cấu Hình Hệ Thống (`SystemConfigPanel`)
- **Điểm yếu hiện tại:** Mảng chuỗi tĩnh `rawData`, double-click chỉ hiện popup `JOptionPane` thông báo.
- **Cam kết hợp đồng:**
  - Kết nối với `SystemRepository` và bảng `ThamSoHeThong` / `SystemConfig`.
  - Khi quản trị viên chọn một mục cấu hình bên trái (VD: "Cấu hình viện phí", "Thời gian timeout phiên làm việc", "Khóa đăng nhập sai quá số lần"):
    - Bảng bên phải hiển thị danh sách khóa - giá trị (`config_key`, `config_value`, `description`, `updated_at`).
    - Cho phép sửa trực tiếp và bấm `[Lưu Cấu Hình]` để cập nhật CSDL.
    - Ghi nhận Audit Log hành động thay đổi tham số hệ thống.

---

## 3. TẦNG NGHIỆP VỤ & SERVICES SỬ DỤNG
- `vn.clinicmanagement.infrastructure.persistence.system.SystemRepositoryImpl`
- `vn.clinicmanagement.application.facility.CreateRoomHandler`
- `vn.clinicmanagement.application.inventory.CreateItemHandler`
- Bảng CSDL liên quan: `ICD`, `DanhMucDichVu`, `Item`, `PhongKham`, `Khoa`, `ThamSoHeThong`.
