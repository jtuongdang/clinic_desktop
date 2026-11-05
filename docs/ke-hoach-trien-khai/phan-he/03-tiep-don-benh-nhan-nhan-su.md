# Phân hệ 03 — Hồ sơ bệnh nhân, BHYT và Tiếp đón

**Trạng thái:** `[~] Code và regression tests đã commit local; còn kiểm thử tích hợp và nghiệm thu end-to-end`
**Phase:** 1; quản trị nhân sự mở rộng ở Phase 8.
**Domain:** `patient`, `identity`, `facility`, `appointment`.

> Code snapshot at task creation was `3f6ba31`; current feature then received documentation-only commit `dbad2fd`. Test inventory: 11 source files / 55 test cases (domain 3/16, application 3/17, infrastructure 4/17, desktop 1/5). Recent commits include `3f6ba31`, `ea397f9`, `264bbae`, `ff2045a`, `dba0da9`. `mvn clean verify` after DateRange/Money additions passed with 55 tests and zero failures/errors/skips. Code/test commits do not equal phase acceptance. Phase 1 remains in progress because SQL Server save/search/queue, restart/recovery, multi-row reception rollback and E2E remain unproven.

## Hiện có trong source

- **Application:** `RegisterPatientHandler`, `SearchPatientsHandler`, `GetReceptionQueueHandler` cùng command/query tương ứng.
- **Infrastructure:** `BenhNhanRepositoryImpl`, `BaoHiemRepositoryImpl`, `PhongKhamRepositoryImpl`, `ReceptionQueueRepositoryImpl`; `DatabaseMigrationRunner` đăng ký V1–V4.
- **Desktop:** `ReceptionCompositionRoot`, `ReceptionPanel`, `PatientRegistrationFormPanel`, `ClinicRoomStatusPanel`; các thao tác tải, tìm kiếm và lưu reception dùng `SwingWorker`.
- **Luồng lưu theo source:** handler validate dữ liệu bắt buộc/phòng/BHYT và dùng `TransactionBoundary`; ghi bệnh nhân, thẻ BHYT nếu có, rồi tạo lượt khám/queue. Chưa có kiểm thử SQL Server xác nhận dữ liệu persist và rollback thực tế.
- **Tìm kiếm theo source:** query repository theo keyword (mã bệnh nhân, CCCD, số điện thoại, họ tên theo logic repository), giới hạn 50 kết quả và ghép BHYT gần nhất. Chưa có acceptance save/search/reload sau restart.

## Bằng chứng đã có

- Tại feature HEAD có **11 test source và 55 test cases** theo kết quả gần nhất được cung cấp: **3 domain / 16** (`DateRangeTest`, `MoneyTest`, `PhoneNumberTest`), **3 application / 17** (`RegisterPatientDuplicateCccdTest`, `RegisterPatientHandlerTest`, `SearchAndReceptionQueueHandlerTest`), **4 infrastructure / 17** (`BenhNhanRepositoryImplTest`, `DatabaseMigrationRunnerTest`, `JdbcTransactionManagerTest`, `SqlServerBatchSplitterTest`), **1 desktop / 5** (`PatientRegistrationFormPanelTest`). Targeted tests passed. Những test này bao phủ validation, handler, transaction boundary, batch splitter/runner, repository mapping và headless form; không chứng minh SQL Server reception E2E.
- `PhoneNumberTest` kiểm tra định dạng số di động Việt Nam, prefix `0`/`+84`, dấu phân cách và đầu vào sai; đây chỉ là validation domain.
- `V4__Enforce_Active_Patient_CCCD_Uniqueness.sql` dừng nếu tồn tại CCCD trùng trên hồ sơ active trước khi tạo unique index. Test application kiểm tra nhánh duplicate không ghi BHYT/lượt khám; test infrastructure dùng lỗi tổng hợp xác nhận chỉ ánh xạ đúng index CCCD và không lộ giá trị trùng.
- UI source nhận lỗi duplicate CCCD, hướng dẫn tìm hồ sơ hiện hữu, chạy tìm kiếm với CCCD và giữ form để tiếp tục. `PatientRegistrationFormPanelTest` bổ sung headless component coverage; commit `ea397f9` sửa giả định chọn phòng để kết quả test deterministic. Đây không phải end-to-end UI/JDBC test.
- Evidence commits trong current history: `3f6ba31` (DateRange/Money), `ea397f9` (headless reception form), `264bbae` (GO splitter/migration runner), `ff2045a` (JDBC transaction), `dba0da9` (model task board); cùng các commit trước đó về handler/search/duplicate mapping. GO splitter test hiện thuộc feature HEAD; vẫn chưa có migration run trên SQL Server.

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
- [ ] Kiểm thử UI/E2E: xác nhận lỗi không xóa form trong luồng tích hợp, thao tác DB ngoài EDT và refresh queue sau đăng ký. `PatientRegistrationFormPanelTest` đã cover một phần tương tác form headless nhưng không chứng minh các luồng tích hợp này.
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
