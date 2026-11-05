# Phân hệ 03 — Hồ sơ bệnh nhân, BHYT và Tiếp đón

**Trạng thái:** `[~] Code và regression tests đã commit local; còn kiểm thử tích hợp và nghiệm thu end-to-end`
**Phase:** 1; quản trị nhân sự mở rộng ở Phase 8.
**Domain:** `patient`, `identity`, `facility`, `appointment`.

> Code snapshot: `ef602a9`. Test inventory: 16 source files / 68 discovered cases (domain 3/16, application 5/26, infrastructure 5/18, desktop 3/8); three SQL Server tests are opt-in and skipped by default, two cover reception. `mvn clean verify` passed on JDK 17 with 0 failures/errors. SQL Server synthetic E2E save/search/queue/reload passed separately; manual checks also passed multi-row rollback and queue/CCCD concurrency. Phase 1 remains in progress because full Swing app/UI E2E, privacy/audit and full business acceptance remain open. JDK 17 smoke started the Swing AWT event loop, but the window itself was not observed.

## Hiện có trong source

- **Application:** `RegisterPatientHandler`, `SearchPatientsHandler`, `GetReceptionQueueHandler` cùng command/query tương ứng.
- **Infrastructure:** `BenhNhanRepositoryImpl`, `BaoHiemRepositoryImpl`, `PhongKhamRepositoryImpl`, `ReceptionQueueRepositoryImpl`; `DatabaseMigrationRunner` đăng ký V1–V4.
- **Desktop:** `ReceptionCompositionRoot`, `ReceptionPanel`, `PatientRegistrationFormPanel`, `ClinicRoomStatusPanel`; các thao tác tải, tìm kiếm và lưu reception dùng `SwingWorker`.
- **Luồng lưu:** handler validate dữ liệu bắt buộc/phòng/BHYT và dùng `TransactionBoundary`; ghi bệnh nhân, thẻ BHYT nếu có, rồi tạo lượt khám/queue. Test opt-in đã xác nhận persist/search/queue/reload bằng composition mới trên SQL Server synthetic. Manual failure injection xác nhận rollback không còn active patient/BHYT/visit rows.
- **Tìm kiếm:** query repository theo keyword (mã bệnh nhân, CCCD, số điện thoại, họ tên theo logic repository), giới hạn 50 kết quả và ghép BHYT gần nhất. Save/search/reload qua composition mới và JVM process con độc lập đã pass; chưa phải restart toàn bộ ứng dụng Swing.

## Bằng chứng đã có

- Tại `ef602a9` có **16 test sources và 68 test cases được phát hiện**: domain 3/16, application 5/26, infrastructure 5/18, desktop 3/8; ba SQL Server opt-in tests bị skip trong default verify (hai test reception, một fresh-migration thuộc Phase 0). `mvn clean verify` pass trên JDK 17 với 0 failures/errors. `ReceptionSqlServerPersistenceTest` pass riêng 1/1 trên DB synthetic, xác nhận đăng ký+search+queue+reload và update patient+BHYT; process-restart probe pass riêng 1/1. Test suite không chứng minh Phase 2 access/audit.
- `PhoneNumberTest` kiểm tra định dạng số di động Việt Nam, prefix `0`/`+84`, dấu phân cách và đầu vào sai; đây chỉ là validation domain.
- `V4__Enforce_Active_Patient_CCCD_Uniqueness.sql` dừng nếu tồn tại CCCD trùng trên hồ sơ active trước khi tạo unique index. Test application kiểm tra nhánh duplicate không ghi BHYT/lượt khám; test infrastructure dùng lỗi tổng hợp xác nhận chỉ ánh xạ đúng index CCCD và không lộ giá trị trùng.
- UI source nhận lỗi duplicate CCCD, hướng dẫn tìm hồ sơ hiện hữu, chạy tìm kiếm với CCCD và giữ form để tiếp tục. `PatientRegistrationFormPanelTest` bổ sung headless component coverage; commit `ea397f9` sửa giả định chọn phòng để kết quả test deterministic. Đây không phải end-to-end UI/JDBC test.
- Evidence commits trong local history: `ef602a9` (fresh-database migration test), `3e3fe6f` (distinct JVM persistence probe), `99d709b`, `75a218d`, `fcfc62b`, `9ff7ddd`, `3f6ba31`, `ea397f9`, `264bbae`, `ff2045a`. Migration runner đã pass trên database synthetic trống riêng; DB này được giữ với demo seed nhưng 0 patient rows. Migration failure-safety vẫn cần acceptance. Manual checks xác nhận rollback multi-table; 8 queue allocations đồng thời nhận số khác nhau; hai CCCD giống nhau có một success và một lỗi nghiệp vụ. Test patient records được soft-delete.

## Quy tắc nghiệp vụ cần bảo vệ

1. Hồ sơ hiện hữu phải được chọn/cập nhật, không tạo bệnh nhân thứ hai chỉ vì tên hoặc cách viết khác nhau.
2. CCCD trùng trả thông báo nghiệp vụ hướng dẫn tìm hồ sơ cũ; unique index active CCCD và ánh xạ lỗi đã qua kiểm tra concurrency synthetic trên SQL Server.
3. Đăng ký, cập nhật bảo hiểm và tạo lượt khám phải nguyên tử: lỗi ở bất kỳ bước nào thì rollback toàn bộ.
4. Chỉ nhận phòng khám đang hoạt động và có quan hệ cơ sở/khoa hợp lệ.
5. Thông tin BHYT cần validation theo trường bắt buộc và ngày hiệu lực; quy tắc hưởng/quyền lợi phải được xác nhận bởi nghiệp vụ, không suy diễn chỉ từ giá trị form.
6. Kiểm tra tuổi, số điện thoại, CCCD, lịch sử chỉnh sửa và quyền xem dữ liệu theo policy được thống nhất.

## Công việc còn lại

- [ ] Review/publish branch khi các gate của Phase 1 được chấp nhận; các commit hiện vẫn local trên `feature/patient-reception-workflow`.
- [x] Handler/form đã hỗ trợ chọn và cập nhật hồ sơ cũ/BHYT; application, headless form và SQL Server synthetic tests xác nhận update path và reload, commits `75a218d`/`99d709b`.
- [x] Chạy integration test SQL Server synthetic cho save/search/update/queue/reload và rollback đa bảng; queue/CCCD concurrency đã kiểm tra thủ công. Test tự động opt-in: `ReceptionSqlServerPersistenceTest` (commit `fcfc62b`).
- [~] V1–V4 chạy qua test trên SQL Server synthetic hiện hữu và index active CCCD đã được xác minh; còn cần migration trên DB trống và fixture có active duplicate để chứng minh dừng an toàn, không xóa/gộp dữ liệu.
- [ ] Kiểm thử UI-to-DB E2E: lỗi không xóa form, DB I/O ngoài EDT và queue refresh sau đăng ký. Headless panel test không chứng minh luồng tích hợp này.
- [ ] Xác nhận các trường form ánh xạ đúng schema/entity; không dùng nhãn “đồng bộ 100%” nếu chưa có test mapping.
- [ ] Xác nhận xem `ClinicRoomStatusPanel` có yêu cầu refresh/polling, hành vi khi DB mất kết nối và quy trình tiếp tục.

## Nhân sự

`NhanVien` và đăng nhập nền tảng đã có repository/application wiring trong source. CRUD nhân sự, phân ca, khóa/mở tài khoản, quản lý vai trò và phân quyền vẫn thuộc backlog quản trị ở Phase 8; không xem việc đăng nhập hoạt động là đã hoàn tất quản trị nhân sự.

## Nghiệm thu Phase 1

- [x] Đăng ký patient synthetic trên SQL Server, đọc lại search/BHYT/queue từ JVM process thứ hai qua opt-in test `ReceptionSqlServerProcessRestartTest` (`3e3fe6f`). Đây xác nhận cross-process persistence, chưa kiểm restart cửa sổ Swing.
- [ ] Chọn hồ sơ cũ và cập nhật; không nhân đôi hồ sơ.
- [ ] Tạo lượt khám, cấp queue number không trùng trong cùng phạm vi phòng/ngày.
- [ ] Mô phỏng lỗi ghi và xác nhận patient/insurance/visit cùng rollback.
- [ ] Hai đăng ký đồng thời cùng CCCD không thể tạo hai hồ sơ active.
- [ ] Kiểm tra form giữ dữ liệu và hiển thị lỗi thân thiện.
