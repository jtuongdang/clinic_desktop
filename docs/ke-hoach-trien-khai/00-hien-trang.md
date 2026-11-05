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
| `clinic-desktop` | Swing UI cho auth, dashboard, reception, doctor/examination, paraclinical, pharmacy, billing, catalog, reports và system. Tiếp đón nối handlers qua `ReceptionCompositionRoot`; các thao tác tìm kiếm/tải/lưu dùng `SwingWorker`. Có headless form tests và SQL Server opt-in persistence/process tests. | Độ hoàn thiện khác nhau. Các test repository/process không tự chứng minh thao tác end-to-end trên cửa sổ Swing. |

## Migration và kiểm thử trong checkout

- Migration runner đăng ký tuần tự bốn file: `V1__Initial_Clinic_Schema.sql`, `V2__Seed_Initial_Data.sql`, `V3__Repair_Demo_Account_Passwords.sql`, `V4__Enforce_Active_Patient_CCCD_Uniqueness.sql`.
- `DatabaseMigrationRunner` dùng `SqlServerBatchSplitter` để tách `GO` đứng riêng trên dòng; unit tests kiểm tra runner/splitter. Test `FreshDatabaseMigrationSqlServerTest` đã bootstrap thành công một SQL Server database synthetic hoàn toàn trống, chạy tuần tự V1–V4, xác nhận 4 migration records, seed counts và unique index V4. Database test `clinic_test_migration_bootstrap_20261105` được giữ lại, có 0 patient rows; migration failure-safety vẫn chưa được kiểm tra.
- Tại source snapshot `ef602a9` có **16 test source / 68 test cases**: domain 3/16, application 5/26, infrastructure 5/18, desktop 3/8. Ba SQL Server integration tests là opt-in và được skip khi chạy mặc định.
- `mvn clean verify` trên source snapshot `ef602a9` bằng JDK 17 pass: 68 tests được phát hiện, 0 failures/errors, 3 skips (ba test SQL Server opt-in); cả năm Maven module thành công. Shaded JAR đã được kiểm tra không còn metadata chữ ký dependency hoặc `db.properties` cục bộ; sau đó `mvn clean` đã dọn target chứa cấu hình local. Auth tests kiểm tra handler/verifier bằng fixture tổng hợp, không xác nhận access scope hay audit.
- SQL Server synthetic `clinic_orchestrator_review_20261105`: registration/update E2E pass 1/1 và JVM process-restart probe pass 1/1; child JVM khác xác nhận patient/BHYT/visit/queue sau khi parent JVM ghi. Test rows được soft-delete theo ID. Manual SQL checks cũng xác nhận rollback đa bảng, 8 queue registrations đồng thời có số riêng, và hai đăng ký cùng CCCD chỉ có một thành công. Đây chưa phải restart/interaction của ứng dụng Swing đầy đủ.

## Tiếp đón và trạng thái Git

- `ReceptionCompositionRoot` nối các handler với JDBC. Test opt-in đã ghi patient+BHYT+visit+queue, tìm theo CCCD và đọc lại bằng composition mới. `ReceptionSqlServerProcessRestartTest` còn khởi động JVM con riêng và xác nhận search/BHYT/queue từ process thứ hai; đây chưa phải restart toàn bộ ứng dụng Swing.
- SQL Server verification bổ sung: rollback đa bảng khi insert visit lỗi để lại 0 active rows cho patient/BHYT/visit; 8 đăng ký đồng thời nhận queue number duy nhất; hai đăng ký cùng CCCD cho đúng 1 commit và lỗi còn lại được ánh xạ `PATIENT_DUPLICATE_CCCD`. Các bản ghi test đã soft-delete.
- Feature code snapshot là `ef602a9` (fresh-database migration integration test); bao gồm packaging fix `df5a8da` và test JVM process-restart `3e3fe6f`, cùng các commit trước đó `99d709b`, `75a218d`, `fcfc62b`, `9ff7ddd`, `3f6ba31`, `ea397f9`, `264bbae`, `ff2045a`. Đây là bằng chứng commit/test local; không phải phát hành hay nghiệm thu Phase 0/1.
- Còn thiếu: xác minh cửa sổ và thao tác toàn bộ ứng dụng Swing/UI-to-database E2E, migration failure-safety, authorization/privacy/audit. Tiến trình Swing đã khởi tạo AWT event loop khi smoke test JDK 17, nhưng cửa sổ chưa được quan sát trực tiếp.

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
