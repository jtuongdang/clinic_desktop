# Phân hệ 08 — Cơ sở, bảo mật và cấu hình hệ thống

**Trạng thái:** `[ ] Chưa đạt điều kiện dùng dữ liệu thật`  
**Đợt:** bảo mật nền ở Đợt 0; cấu hình đa cơ sở ở Đợt 7.  
**Domain:** `facility`, `identity`, `system`.

## Hiện có

- Models facility: `CoSo`, `Khoa`, `PhongKham`.
- Models identity/access: `NhanVien`, `VaiTro`, `NhanVienVaiTro`, `NhanVienKhoa`, `PhanQuyenCoSo`, `MaTranPhanQuyen`.
- Models system: `ThamSoHeThong`, `CauHinhTichHop`, `NhatKyHeThong`, `NhatKyHoSoBenhAn`, `TokenLamMoi`.
- UI: `LoginForm`, `SystemConfigPanel`; panel hệ thống hiện là danh sách chức năng tìm kiếm, chưa phải CRUD/permission manager.

## Backlog bảo mật P0

- [ ] Thay login giả bằng xác thực credentials thật; mật khẩu hash/verify ở tầng server/application, không giữ plaintext.
- [ ] Không cho người dùng tự chọn role để cấp quyền; role lấy từ tài khoản và giới hạn theo cơ sở/khoa.
- [ ] Thêm quản lý user: tạo/khóa/mở khóa, reset mật khẩu, đổi mật khẩu, kiểm tra tài khoản hoạt động.
- [ ] Tạo kiểm tra quyền tập trung cho command/use case; UI chỉ ẩn/disable bổ sung, không là ranh giới bảo mật duy nhất.
- [ ] Ghi audit cho đăng nhập, xem/sửa EMR, cấp phát, thu tiền, cấu hình và thay đổi quyền.
- [ ] Thay đổi cấu hình DB mẫu và không đóng gói credentials mặc định; dùng biến môi trường/secret store, cấu hình TLS phù hợp.

## Backlog quản trị

- [ ] **`CoSo`, `Khoa`, `PhongKham`:** cây danh mục, hiệu lực, trạng thái, phạm vi người dùng.
- [ ] **`MaTranPhanQuyen`, `PhanQuyenCoSo`:** editor ma trận role × action × phạm vi; preview quyền hiệu lực và lịch sử thay đổi.
- [ ] **`ThamSoHeThong`:** form có kiểu dữ liệu, validation, mô tả, giá trị hiệu lực và audit; không chỉnh trực tiếp bảng chung.
- [ ] **`NhatKyHeThong`, `NhatKyHoSoBenhAn`:** trang tra cứu chỉ đọc với filter thời gian/user/model/record/action; kiểm soát quyền xem.
- [ ] **`CauHinhTichHop`:** chỉ tạo khi tích hợp xác định; secret được mask, không log ra màn hình.
- [ ] **`TokenLamMoi`:** PO/kỹ thuật xác nhận có dùng với API OAuth hay không; nếu không thì loại khỏi phạm vi UI desktop.

## Màn hình cần bổ sung

1. Quản lý tài khoản/nhân viên và trạng thái truy cập.
2. Gán vai trò, khoa và cơ sở cho người dùng.
3. Ma trận quyền và màn hình xem quyền hiệu lực.
4. Danh mục cơ sở–khoa–phòng.
5. Audit log hệ thống và audit truy cập EMR.
6. Cấu hình tích hợp có kiểm tra kết nối/secret masking khi có vendor.

## Nghiệm thu

- [ ] Người dùng không thể đăng nhập bằng thông tin mặc định hoặc tự nâng role từ UI.
- [ ] Mọi command nhạy cảm kiểm tra actor, permission và data scope trong Application.
- [ ] Log không lưu mật khẩu/token/secret; truy cập audit có quyền riêng.
- [ ] Vô hiệu hóa tài khoản/role có hiệu lực ngay trong session theo policy đã chốt.

