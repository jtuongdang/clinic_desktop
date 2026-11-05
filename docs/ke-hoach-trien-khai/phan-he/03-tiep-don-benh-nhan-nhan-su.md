# Phân hệ 03 — Hồ sơ bệnh nhân, BHYT và Tiếp đón

**Trạng thái:** `[~] Có triển khai trong checkout; chờ commit và nghiệm thu`
**Đợt:** 1; quản trị nhân sự mở rộng ở Đợt 7.
**Domain:** `patient`, `identity`, `facility`, `appointment`.

> Các repository/use case/UI của Tiếp đón hiện có thay đổi chưa commit. Trạng thái này mô tả checkout đang rà soát, không khẳng định branch đã chia sẻ hoặc release đã có chức năng.

## Hiện có trong source

- **Application:** `RegisterPatientCommand`/`RegisterPatientHandler`, `SearchPatientsQuery`/handler, use case đọc hàng đợi.
- **Infrastructure:** repository bệnh nhân, bảo hiểm, phòng khám và hàng đợi JDBC.
- **Desktop:** `ReceptionCompositionRoot`, `ReceptionPanel`, `PatientRegistrationFormPanel`, `ClinicRoomStatusPanel` và các thành phần toolbar/footer.
- **Luồng lưu:** handler kiểm tra dữ liệu bắt buộc, bệnh nhân và BHYT; trong transaction ghi bệnh nhân, thẻ bảo hiểm, lượt khám và số thứ tự hàng đợi.
- **Tìm kiếm:** theo mã bệnh nhân, CCCD, số điện thoại hoặc họ tên; chi tiết thực tế tùy truy vấn hiện triển khai.

## Quy tắc nghiệp vụ cần bảo vệ

1. Hồ sơ hiện hữu phải được chọn/cập nhật, không tạo bệnh nhân thứ hai chỉ vì tên hoặc cách viết khác nhau.
2. CCCD trùng phải trả thông báo nghiệp vụ hướng dẫn tìm hồ sơ cũ. Kiểm tra trước khi ghi hiện chưa đủ để chống hai yêu cầu đồng thời; cần unique index phù hợp ở SQL Server và ánh xạ lỗi unique.
3. Đăng ký, cập nhật bảo hiểm và tạo lượt khám phải nguyên tử: lỗi ở bất kỳ bước nào thì rollback toàn bộ.
4. Chỉ nhận phòng khám đang hoạt động và có quan hệ cơ sở/khoa hợp lệ.
5. Thông tin BHYT cần validation theo trường bắt buộc và ngày hiệu lực; quy tắc hưởng/quyền lợi phải được xác nhận bởi nghiệp vụ, không suy diễn chỉ từ giá trị form.
6. Kiểm tra tuổi, số điện thoại, CCCD, lịch sử chỉnh sửa và quyền xem dữ liệu theo policy được thống nhất.

## Công việc còn lại

- [ ] Commit/review phần thay đổi Tiếp đón vào branch nguồn dùng để triển khai.
- [ ] Thêm unit test cho validation, cập nhật hồ sơ cũ, dữ liệu BHYT và các nhánh lỗi.
- [ ] Chạy integration test trên SQL Server thử nghiệm cho save/search/queue và rollback.
- [ ] Bảo đảm chống trùng CCCD ở DB; migration phải dừng an toàn nếu dữ liệu mẫu hiện có xung đột, không tự xóa/gộp.
- [ ] Kiểm thử UI: lỗi không xóa form, thao tác DB ngoài EDT, refresh queue sau đăng ký.
- [ ] Xác nhận các trường form ánh xạ đúng schema/entity; không dùng nhãn “đồng bộ 100%” nếu chưa có test mapping.
- [ ] Xác nhận xem `ClinicRoomStatusPanel` có yêu cầu refresh/polling, hành vi khi DB mất kết nối và quy trình tiếp tục.

## Nhân sự

`NhanVien` và đăng nhập nền tảng đã có repository/application wiring trong source. CRUD nhân sự, phân ca, khóa/mở tài khoản, quản lý vai trò và phân quyền vẫn thuộc backlog quản trị ở Đợt 7/Phân hệ 08; không xem việc đăng nhập hoạt động là đã hoàn tất quản trị nhân sự.

## Nghiệm thu Đợt 1

- [ ] Đăng ký bệnh nhân tổng hợp trên DB thử nghiệm, restart ứng dụng và tìm lại chính xác.
- [ ] Chọn hồ sơ cũ và cập nhật; không nhân đôi hồ sơ.
- [ ] Tạo lượt khám, cấp queue number không trùng trong cùng phạm vi phòng/ngày.
- [ ] Mô phỏng lỗi ghi và xác nhận patient/insurance/visit cùng rollback.
- [ ] Hai đăng ký đồng thời cùng CCCD không thể tạo hai hồ sơ active.
- [ ] Kiểm tra form giữ dữ liệu và hiển thị lỗi thân thiện.
