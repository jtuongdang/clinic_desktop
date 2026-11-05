# 00 — Hiện trạng và nguyên tắc phân tích

**Cập nhật:** 06/11/2026
**Phạm vi:** Clinic Management Desktop — Java, Maven, JDBC, SQL Server, Java Swing.

> Trạng thái phản ánh source và lịch sử Git quan sát được trong checkout. Code đã commit trên máy vẫn chưa đồng nghĩa với đã được đẩy lên remote, phát hành, hoặc nghiệm thu end-to-end.

## Hiện trạng theo lớp

| Lớp | Bằng chứng trong source | Đánh giá hiện tại |
|---|---|---|
| `clinic-domain` | Model theo patient, identity, facility, appointment, examination, prescription, inventory, billing, service, system và các miền khác; repository interfaces. | Nền model rộng. Sự tồn tại của model không chứng minh luồng nghiệp vụ hoàn chỉnh. |
| `clinic-application` | `AuthenticateUserHandler`; reception handlers `RegisterPatientHandler`, `SearchPatientsHandler`, `GetReceptionQueueHandler`; `TransactionBoundary`, `CommandHandler`, `QueryHandler`. | Auth và một phần Tiếp đón có use case. Các use case khác còn cần nối, kiểm thử và nghiệm thu. |
| `clinic-infrastructure` | JDBC, `DatabaseMigrationRunner`, `JdbcTransactionManager`, `JdbcUnitOfWork`; repositories `NhanVienRepositoryImpl`, `BenhNhanRepositoryImpl`, `BaoHiemRepositoryImpl`, `PhongKhamRepositoryImpl`, `ReceptionQueueRepositoryImpl`. Runner đăng ký migration V1–V9. | Có persistence cho auth và một phần Tiếp đón. Đã có bằng chứng SQL Server opt-in cho bootstrap schema V1–V9 và composition Tiếp đón trên DB tổng hợp riêng; không bao quát toàn bộ tiêu chí nghiệp vụ. |
| `clinic-desktop` | Swing UI cho auth, dashboard, reception, doctor/examination, paraclinical, pharmacy, billing, catalog, reports và system. Tiếp đón nối handlers qua `ReceptionCompositionRoot`; các thao tác tìm kiếm/tải/lưu dùng `SwingWorker`. Có headless form tests và SQL Server opt-in persistence/process tests. | Độ hoàn thiện khác nhau. Cửa sổ Swing chưa được quan sát trực tiếp; tương tác toàn bộ desktop UI và UI-to-database E2E vẫn chưa được xác minh. |

## Migration và kiểm thử trong checkout

- `DatabaseMigrationRunner` đăng ký tuần tự V1–V9 (`V1__Initial_Clinic_Schema.sql` đến `V9__Enforce_Active_Patient_Facility_Uniqueness.sql`) và dùng `SqlServerBatchSplitter` để tách `GO` đứng riêng trên dòng.
- **Default verify:** bằng chứng chạy đầy đủ gần nhất với JDK 17 là `mvn -Djava.awt.headless=true clean verify`: **237 tests, 0 failures, 0 errors, 4 skipped**; cả 4 skip là SQL Server opt-in tests được bỏ qua có chủ đích khi chạy mặc định. Không tính các test skip này là đã chạy trong lượt verify mặc định.
- **SQL Server opt-in riêng:** `FreshDatabaseMigrationSqlServerTest` đã chạy thành công migration V1–V9 trên DB mới, tổng hợp `clinic_test_v9_20261106`; `ReceptionSqlServerPersistenceTest` đã xác minh luồng Tiếp đón qua `ReceptionCompositionRoot` trên DB tổng hợp này. Đây là bằng chứng tích hợp có phạm vi cụ thể, không thay thế các skip của lượt verify mặc định và không chứng minh toàn bộ UI desktop.
- Failure-safety migration không còn là khoảng trống hoàn toàn: `DatabaseMigrationRunnerTest` kiểm tra batch lỗi và lỗi ghi migration tracking đều rollback, không commit; đồng thời giữ lỗi migration gốc nếu rollback cũng lỗi. Đây là coverage unit với JDBC giả lập, không phải bằng chứng rằng mọi kịch bản rollback khi chạy migration thực tế trên SQL Server đã được kiểm chứng.

## Tiếp đón và trạng thái Git

- `ReceptionCompositionRoot` nối các handler Tiếp đón với JDBC; bằng chứng opt-in trên `clinic_test_v9_20261106` bao gồm migration mới V1–V9 và E2E composition cho Tiếp đón với dữ liệu tổng hợp. Không đưa credentials hay dữ liệu cá nhân vào tài liệu.
- Code của feature đã được commit cục bộ trên `feature/patient-reception-workflow`; không có bằng chứng đã push hoặc phát hành. Các kết quả nêu trên không đồng nghĩa Phase 0, Phase 1 hoặc Phase 2 hoàn tất.
- **UI chưa được xác minh:** do máy Mac đang khóa, cửa sổ Swing chưa được quan sát trực tiếp. Khởi tạo AWT/headless form tests không chứng minh hiển thị cửa sổ hay thao tác end-to-end trên toàn desktop UI; UI-to-database E2E vẫn chưa được xác minh.
- Các giới hạn khác vẫn gồm nghiệm thu nghiệp vụ toàn diện và xác minh authorization/privacy/audit theo từng luồng; không suy ra hoàn tất phase từ source, test hoặc một lượt tích hợp có phạm vi giới hạn.

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
