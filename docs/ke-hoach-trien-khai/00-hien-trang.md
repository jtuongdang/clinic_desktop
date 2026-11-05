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
| `clinic-desktop` | Swing UI cho auth, dashboard, reception, doctor/examination, paraclinical, pharmacy, billing, catalog, reports và system. Tiếp đón nối handlers qua `ReceptionCompositionRoot`; các thao tác tìm kiếm/tải/lưu dùng `SwingWorker`. | Độ hoàn thiện khác nhau. Panel hoặc wiring không tự chứng minh persistence/restart/E2E. Không có test file trong `clinic-desktop/src/test`. |

## Migration và kiểm thử trong checkout

- Migration runner đăng ký tuần tự bốn file: `V1__Initial_Clinic_Schema.sql`, `V2__Seed_Initial_Data.sql`, `V3__Repair_Demo_Account_Passwords.sql`, `V4__Enforce_Active_Patient_CCCD_Uniqueness.sql`.
- Runner hiện có tách batch tại `GO` đứng riêng trên một dòng (regex hỗ trợ LF/CRLF). Không có migration-runner test trong feature branch hiện tại và chưa có bằng chứng chạy migration trên SQL Server thử nghiệm. Một commit task địa phương `74490b3` thêm `SqlServerBatchSplitter` cùng test, nhưng ref đó không nằm trong ancestry của `feature/patient-reception-workflow`; không tính test ấy vào trạng thái branch này.
- Có đúng năm test source trong các module hiện rà soát:
  - Domain: `PhoneNumberTest` — định dạng/prefix số di động Việt Nam và đầu vào không hợp lệ.
  - Application: `RegisterPatientDuplicateCccdTest`, `RegisterPatientHandlerTest`, `SearchAndReceptionQueueHandlerTest` — nhánh duplicate; đăng ký; tìm kiếm và queue handlers.
  - Infrastructure: `BenhNhanRepositoryImplTest` — ánh xạ lỗi unique index CCCD, không nhầm unique key mã bệnh nhân, không để lộ giá trị trùng.
- Chưa có test desktop. Năm test trên không chứng minh SQL Server save/search/queue, đọc lại sau restart, rollback nhiều bảng khi lỗi ghi tổng quát, hoặc queue concurrency.

## Tiếp đón và trạng thái Git

- `ReceptionCompositionRoot` nối `RegisterPatientHandler`, `SearchPatientsHandler`, `GetReceptionQueueHandler` với JDBC repositories. Handler đăng ký ghi bệnh nhân, bảo hiểm tùy chọn và lượt khám trong transaction boundary.
- Search query tra cứu hồ sơ và BHYT gần nhất; queue repository ghi lượt khám, cấp số theo phòng/ngày bằng khóa giao dịch SQL, và đọc hàng đợi/tóm tắt phòng. Các hành vi này có trong source nhưng chưa có E2E SQL Server/restart acceptance.
- Task checkout đang ở `orchestrator/add-model-level-task-board-and-secure-phase-dependen` commit `58ac034`; tại snapshot này có đúng năm test source: 1 domain, 3 application, 1 infrastructure, 0 desktop. Trong lúc cập nhật, ref local `feature/patient-reception-workflow` đã tiến thêm tới `ff2045a` (JDBC transaction manager test), ngoài snapshot task checkout; ref mới có thêm một infrastructure test. Gần nhất trên feature ref là `ff2045a`, `58ac034`, `e6bbc2e`, `11c800c`, `37adb7e`. Vì scope cho phép chỉ sửa tài liệu, source test mới không được copy vào task checkout. Commit GO-splitting/test `74490b3` vẫn ở ref local riêng, không thuộc ancestry feature.
- Remote refs hiện có `origin/main` và `origin/feature/jdbc-transaction-tests-maven-ci`; không có remote ref cho `feature/patient-reception-workflow`. Các commit nêu trên là local, chưa có bằng chứng được publish hoặc phát hành.

## Nguyên tắc đánh giá hoàn thiện

1. Đánh giá theo luồng **màn hình → application use case → domain rule → repository/JDBC → CSDL → đọc lại/hiển thị**, không dựa riêng vào sự tồn tại của model hoặc panel.
2. Chỉ đánh dấu hoàn tất khi có tiêu chí nghiệp vụ, xử lý lỗi, kiểm thử phù hợp và bằng chứng chạy được. Ghi riêng trạng thái code local, commit, test và nghiệm thu.
3. UI không gọi JDBC trực tiếp. I/O và tác vụ dài chạy ngoài Swing EDT; cập nhật UI trên EDT.
4. Các thao tác ghi nhiều bảng phải dùng transaction, có rollback và test tính nguyên tử.
5. Dùng dữ liệu tổng hợp trong kiểm thử; không đưa dữ liệu bệnh nhân thật vào fixture, log hoặc báo cáo phát triển.
6. Quy tắc BHYT, thanh toán, thuốc, quyền truy cập và biểu mẫu pháp quy phải được xác nhận với người phụ trách nghiệp vụ trước khi tuyên bố tuân thủ.

## Nguồn đối chiếu

- `clinic-domain/src/main/java/vn/clinicmanagement/domain/` và `clinic-domain/src/test/`
- `clinic-application/src/main/java/vn/clinicmanagement/application/` và `clinic-application/src/test/`
- `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/`, `clinic-infrastructure/src/main/resources/migrations/` và `clinic-infrastructure/src/test/`
- `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/` và `clinic-desktop/src/test/`
- `README.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `UI_CODING_STANDARDS.md`
