# 00 — Hiện trạng và nguyên tắc phân tích

**Cập nhật:** 05/11/2026
**Phạm vi:** Clinic Management Desktop — Java, Maven, JDBC, SQL Server, Java Swing.

> Trạng thái phản ánh source và lịch sử Git quan sát được trong checkout. Code đã commit trên máy vẫn chưa đồng nghĩa với đã được đẩy lên remote, phát hành, hoặc nghiệm thu end-to-end.

## Hiện trạng theo lớp

| Lớp | Bằng chứng trong source | Đánh giá hiện tại |
|---|---|---|
| `clinic-domain` | Model theo patient, identity, facility, appointment, examination, prescription, inventory, billing, service, system và các miền khác; repository interfaces. | Nền model rộng. Sự tồn tại của model không chứng minh luồng nghiệp vụ hoàn chỉnh. |
| `clinic-application` | `AuthenticateUserHandler`; reception handlers `RegisterPatientHandler`, `SearchPatientsHandler`, `GetReceptionQueueHandler`; `TransactionBoundary`, `CommandHandler`, `QueryHandler`. | Auth và một phần Tiếp đón có use case. Các use case khác còn cần nối, kiểm thử và nghiệm thu. |
| `clinic-infrastructure` | JDBC, `DatabaseMigrationRunner`, `JdbcTransactionManager`, `JdbcUnitOfWork`; repositories `NhanVienRepositoryImpl`, `BenhNhanRepositoryImpl`, `BaoHiemRepositoryImpl`, `PhongKhamRepositoryImpl`, `ReceptionQueueRepositoryImpl`. | Có persistence cho auth và một phần Tiếp đón. Chưa có bằng chứng tích hợp SQL Server cho toàn bộ tiêu chí Tiếp đón. |
| `clinic-desktop` | Swing UI cho auth, dashboard, reception, doctor/examination, paraclinical, pharmacy, billing, catalog, reports và system. Tiếp đón nối handlers qua `ReceptionCompositionRoot`; các thao tác tìm kiếm/tải/lưu dùng `SwingWorker`. Có `PatientRegistrationFormPanelTest` chạy headless. | Độ hoàn thiện khác nhau. Panel/wiring và test component không tự chứng minh persistence/restart/E2E. |

## Migration và kiểm thử trong checkout

- Migration runner đăng ký tuần tự bốn file: `V1__Initial_Clinic_Schema.sql`, `V2__Seed_Initial_Data.sql`, `V3__Repair_Demo_Account_Passwords.sql`, `V4__Enforce_Active_Patient_CCCD_Uniqueness.sql`.
- `DatabaseMigrationRunner` dùng `SqlServerBatchSplitter` để tách `GO` đứng riêng trên dòng; `DatabaseMigrationRunnerTest` và `SqlServerBatchSplitterTest` kiểm tra runner/splitter bằng test tự chứa. Chưa có bằng chứng chạy migration trên SQL Server thử nghiệm.
- Tại code snapshot `3f6ba31` có **11 test source / 55 test cases**: domain 3 source/16 tests (`DateRangeTest`, `MoneyTest`, `PhoneNumberTest`); application 3/17 (`RegisterPatientDuplicateCccdTest`, `RegisterPatientHandlerTest`, `SearchAndReceptionQueueHandlerTest`); infrastructure 4/17 (`BenhNhanRepositoryImplTest`, `DatabaseMigrationRunnerTest`, `JdbcTransactionManagerTest`, `SqlServerBatchSplitterTest`); desktop 1/5 (`PatientRegistrationFormPanelTest`).
- `mvn clean verify` chạy sau khi thêm `DateRangeTest`/`MoneyTest` và trên code snapshot `3f6ba31` đã pass: 55 tests, 0 failures/errors/skips; cả năm Maven module thành công. Commit tài liệu hiện tại không thay đổi source/test.
- Các test unit/headless không chứng minh SQL Server save/search/queue, migration trên SQL Server, đọc lại sau restart, rollback đa bảng trong quy trình tiếp đón, hoặc queue/CCCD concurrency.

## Tiếp đón và trạng thái Git

- `ReceptionCompositionRoot` nối `RegisterPatientHandler`, `SearchPatientsHandler`, `GetReceptionQueueHandler` với JDBC repositories. Handler đăng ký ghi bệnh nhân, bảo hiểm tùy chọn và lượt khám trong transaction boundary.
- Search query tra cứu hồ sơ và BHYT gần nhất; queue repository ghi lượt khám, cấp số theo phòng/ngày bằng khóa giao dịch SQL, và đọc hàng đợi/tóm tắt phòng. Các hành vi này có trong source nhưng chưa có E2E SQL Server/restart acceptance.
- Feature HEAD tại thời điểm cập nhật là `3f6ba31` (`DateRange`/`Money` regression tests), kế tiếp các commit `ea397f9` (headless reception form tests, deterministic room assumption corrected), `264bbae` (SQL Server GO splitter/migration runner tests), `ff2045a` (JDBC transaction tests) và `dba0da9` (model task board). Đây là bằng chứng commit/test local; không phải phát hành hay phase acceptance.
- Số liệu inventory được đối chiếu với `src/test` ở cả bốn module; `mvn clean verify` xác nhận 55 tests pass tại source snapshot `3f6ba31`.

## Nguyên tắc đánh giá hoàn thiện

1. Đánh giá theo luồng **màn hình → application use case → domain rule → repository/JDBC → CSDL → đọc lại/hiển thị**, không dựa riêng vào sự tồn tại của model hoặc panel.
2. Chỉ đánh dấu hoàn tất khi có tiêu chí nghiệp vụ, xử lý lỗi, kiểm thử phù hợp và bằng chứng chạy được. Ghi riêng trạng thái code local, commit, test và nghiệm thu.
3. UI không gọi JDBC trực tiếp. I/O và tác vụ dài chạy ngoài Swing EDT; cập nhật UI trên EDT.
4. Các thao tác ghi nhiều bảng phải dùng transaction, có rollback và test tính nguyên tử. `JdbcTransactionManagerTest` hiện bao phủ commit/rollback/cleanup tại boundary transaction; điều đó không đồng nghĩa đã test rollback của reception nhiều bảng trên SQL Server.
5. Dùng dữ liệu tổng hợp trong kiểm thử; không đưa dữ liệu bệnh nhân thật vào fixture, log hoặc báo cáo phát triển.
6. Quy tắc BHYT, thanh toán, thuốc, quyền truy cập và biểu mẫu pháp quy phải được xác nhận với người phụ trách nghiệp vụ trước khi tuyên bố tuân thủ.

## Nguồn đối chiếu

- `clinic-domain/src/main/java/vn/clinicmanagement/domain/` và `clinic-domain/src/test/`
- `clinic-application/src/main/java/vn/clinicmanagement/application/` và `clinic-application/src/test/`
- `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/`, `clinic-infrastructure/src/main/resources/migrations/` và `clinic-infrastructure/src/test/`
- `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/` và `clinic-desktop/src/test/`
- `README.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `UI_CODING_STANDARDS.md`
