# Phân hệ 03 — Hồ sơ bệnh nhân, BHYT và Tiếp đón

**Trạng thái:** `[~] Code đã commit local; còn kiểm thử tích hợp và nghiệm thu end-to-end`
**Phase:** 1; quản trị nhân sự mở rộng ở Phase 8.
**Domain:** `patient`, `identity`, `facility`, `appointment`.

> Feature branch local `feature/patient-reception-workflow` hiện ở `37adb7e`; remote không có ref tương ứng. Code đã commit local không đồng nghĩa đã publish hoặc nghiệm thu. Không đánh dấu Phase 1 hoàn tất vì save/search/queue SQL Server, rollback/restart và E2E chưa được chứng minh.

## Hiện có trong source

- **Application:** `RegisterPatientHandler`, `SearchPatientsHandler`, `GetReceptionQueueHandler` cùng command/query tương ứng.
- **Infrastructure:** `BenhNhanRepositoryImpl`, `BaoHiemRepositoryImpl`, `PhongKhamRepositoryImpl`, `ReceptionQueueRepositoryImpl`; `DatabaseMigrationRunner` đăng ký V1–V4.
- **Desktop:** `ReceptionCompositionRoot`, `ReceptionPanel`, `PatientRegistrationFormPanel`, `ClinicRoomStatusPanel`; các thao tác tải, tìm kiếm và lưu reception dùng `SwingWorker`.
- **Luồng lưu theo source:** handler validate dữ liệu bắt buộc/phòng/BHYT và dùng `TransactionBoundary`; ghi bệnh nhân, thẻ BHYT nếu có, rồi tạo lượt khám/queue. Chưa có kiểm thử SQL Server xác nhận dữ liệu persist và rollback thực tế.
- **Tìm kiếm theo source:** query repository theo keyword (mã bệnh nhân, CCCD, số điện thoại, họ tên theo logic repository), giới hạn 50 kết quả và ghép BHYT gần nhất. Chưa có acceptance save/search/reload sau restart.

## Bằng chứng đã có

- `PhoneNumberTest` kiểm tra định dạng số di động Việt Nam, prefix `0`/`+84`, dấu phân cách và đầu vào sai; đây chỉ là validation domain.
- `V4__Enforce_Active_Patient_CCCD_Uniqueness.sql` dừng nếu tồn tại CCCD trùng trên hồ sơ active trước khi tạo unique index. Test application kiểm tra nhánh duplicate không ghi BHYT/lượt khám; test infrastructure dùng lỗi tổng hợp xác nhận chỉ ánh xạ đúng index CCCD và không lộ giá trị trùng.
- UI source nhận lỗi duplicate CCCD, hướng dẫn tìm hồ sơ hiện hữu, chạy tìm kiếm với CCCD và giữ form để tiếp tục. Hiện chưa có desktop test cho tương tác này.
- Có ba test source trên feature branch: một domain, một application, một infrastructure; không có desktop test. Commit `74490b3` với `SqlServerBatchSplitter` và test nằm trên ref task địa phương riêng, không thuộc ancestry feature branch hiện tại.

## Quy tắc nghiệp vụ cần bảo vệ

1. Hồ sơ hiện hữu phải được chọn/cập nhật, không tạo bệnh nhân thứ hai chỉ vì tên hoặc cách viết khác nhau.
2. CCCD trùng trả thông báo nghiệp vụ hướng dẫn tìm hồ sơ cũ; unique index active CCCD và ánh xạ lỗi đã có trong source, nhưng cần xác nhận concurrency trên SQL Server thử nghiệm.
3. Đăng ký, cập nhật bảo hiểm và tạo lượt khám phải nguyên tử: lỗi ở bất kỳ bước nào thì rollback toàn bộ.
4. Chỉ nhận phòng khám đang hoạt động và có quan hệ cơ sở/khoa hợp lệ.
5. Thông tin BHYT cần validation theo trường bắt buộc và ngày hiệu lực; quy tắc hưởng/quyền lợi phải được xác nhận bởi nghiệp vụ, không suy diễn chỉ từ giá trị form.
6. Kiểm tra tuổi, số điện thoại, CCCD, lịch sử chỉnh sửa và quyền xem dữ liệu theo policy được thống nhất.

## Công việc còn lại

- [ ] Review và publish các commit local lên branch nguồn dùng để triển khai.
- [ ] Bổ sung unit test cho cập nhật hồ sơ cũ, dữ liệu BHYT và các nhánh lỗi còn thiếu.
- [ ] Chạy integration test trên SQL Server thử nghiệm cho save/search/queue và rollback.
- [ ] Chạy migration V4 trên SQL Server thử nghiệm có kiểm soát; xác nhận migration dừng an toàn khi có CCCD active trùng và không tự xóa/gộp dữ liệu.
- [ ] Kiểm thử UI: lỗi không xóa form, thao tác DB ngoài EDT, refresh queue sau đăng ký.
- [ ] Xác nhận các trường form ánh xạ đúng schema/entity; không dùng nhãn “đồng bộ 100%” nếu chưa có test mapping.
- [ ] Xác nhận xem `ClinicRoomStatusPanel` có yêu cầu refresh/polling, hành vi khi DB mất kết nối và quy trình tiếp tục.

## Nhân sự

`NhanVien` và đăng nhập nền tảng đã có repository/application wiring trong source. CRUD nhân sự, phân ca, khóa/mở tài khoản, quản lý vai trò và phân quyền vẫn thuộc backlog quản trị ở Phase 8; không xem việc đăng nhập hoạt động là đã hoàn tất quản trị nhân sự.

## Nghiệm thu Phase 1

- [ ] Đăng ký bệnh nhân tổng hợp trên DB thử nghiệm, restart ứng dụng và tìm lại chính xác.
- [ ] Chọn hồ sơ cũ và cập nhật; không nhân đôi hồ sơ.
- [ ] Tạo lượt khám, cấp queue number không trùng trong cùng phạm vi phòng/ngày.
- [ ] Mô phỏng lỗi ghi và xác nhận patient/insurance/visit cùng rollback.
- [ ] Hai đăng ký đồng thời cùng CCCD không thể tạo hai hồ sơ active.
- [ ] Kiểm tra form giữ dữ liệu và hiển thị lỗi thân thiện.
