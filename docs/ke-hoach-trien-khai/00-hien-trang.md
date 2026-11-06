# 00 — Hiện trạng và nguyên tắc phân tích

**Cập nhật:** 06/11/2026
**Phạm vi:** Clinic Management Desktop — Java, Maven, JDBC, SQL Server, Java Swing.

> Trạng thái phản ánh source, task board và lịch sử Git được đối chiếu trên checkout local của nhánh `feature/patient-reception-workflow` ngày 06/11/2026. Task worktree đã được tích hợp cục bộ vào nhánh feature; code đã commit trên máy vẫn chưa đồng nghĩa với đã được đẩy lên remote, phát hành, hoặc nghiệm thu end-to-end.

## Hiện trạng theo lớp

| Lớp | Bằng chứng trong source | Đánh giá hiện tại |
|---|---|---|
| `clinic-domain` | Model theo patient, identity, facility, appointment, examination, prescription, inventory, billing, service, system và các miền khác; repository interfaces. | Nền model rộng. Sự tồn tại của model không chứng minh luồng nghiệp vụ hoàn chỉnh. |
| `clinic-application` | `AuthenticateUserHandler`; reception handlers `RegisterPatientHandler`, `SearchPatientsHandler`, `GetReceptionQueueHandler`; `InProcessDomainEventPublisher`; `TransactionBoundary`, `CommandHandler`, `QueryHandler`. | Auth và một phần Tiếp đón có use case. Publisher sự kiện đồng bộ độc lập có 6 test tổng hợp, nhưng chưa được nối vào thu thập aggregate, transaction boundary hoặc handler nghiệp vụ. Các use case khác còn cần nối, kiểm thử và nghiệm thu. |
| `clinic-infrastructure` | JDBC, `DatabaseMigrationRunner`, `JdbcTransactionManager`, `JdbcUnitOfWork`; repositories `NhanVienRepositoryImpl`, `BenhNhanRepositoryImpl`, `BaoHiemRepositoryImpl`, `PhongKhamRepositoryImpl`, `ReceptionQueueRepositoryImpl`. Runner đăng ký migration V1–V9. | Có persistence cho auth và một phần Tiếp đón. Đã có bằng chứng SQL Server opt-in cho bootstrap schema V1–V9 và composition Tiếp đón trên DB tổng hợp riêng; không bao quát toàn bộ tiêu chí nghiệp vụ. |
| `clinic-desktop` | Swing UI cho auth, dashboard, reception, doctor/examination, paraclinical, pharmacy, billing, catalog, reports và system. Tiếp đón nối handlers qua `ReceptionCompositionRoot`; các thao tác tìm kiếm/tải/lưu dùng `SwingWorker`. Có headless form tests và SQL Server opt-in persistence/process tests. | Độ hoàn thiện khác nhau. Có headless form-to-persistence evidence cho Tiếp đón, nhưng cửa sổ Swing chưa được quan sát trực tiếp; tương tác toàn bộ desktop UI và `ReceptionPanel` UI-to-database acceptance vẫn chưa được xác minh. |

## Migration và kiểm thử trong checkout

- `DatabaseMigrationRunner` đăng ký tuần tự V1–V9 (`V1__Initial_Clinic_Schema.sql` đến `V9__Enforce_Active_Patient_Facility_Uniqueness.sql`) và dùng `SqlServerBatchSplitter` để tách `GO` đứng riêng trên dòng.
- **Default verify baseline được ghi nhận:** configured JDK 17 `mvn -Djava.awt.headless=true clean verify` kết thúc `BUILD SUCCESS`: **254 tests, 0 failures, 0 errors, 4 skipped** trên bốn module có test (57 Surefire XML reports). Bốn skip là `FreshDatabaseMigrationSqlServerTest`, `AuditSqlServerPersistenceTest`, `ReceptionSqlServerPersistenceTest` và `ReceptionSqlServerProcessRestartTest`; đây là SQL Server opt-in, không thực thi trong verify mặc định. Baseline gồm 6 synthetic tests cho `InProcessDomainEventPublisher` và 2 tests cho UTC event metadata; publisher/consumers/storage chưa được nối vào thu thập/dispatch theo transaction hoặc workflow nghiệp vụ.
- **SQL Server opt-in được task board ghi nhận riêng:** `FreshDatabaseMigrationSqlServerTest` pass 1/1 trên database tổng hợp mới `clinic_test_v9_20261106`, áp dụng V1–V9 và kiểm tra unique active patient/facility; lượt preflight runtime mới pass **1/1** trên database rỗng disposable mới `clinic_test_v9_preflight_20261106` với lệnh `mvn -pl clinic-infrastructure -am -Djava.awt.headless=true -Dclinic.sqlserver.migration.enabled=true -Dclinic.sqlserver.test.database=clinic_test_v9_preflight_20261106 -Dtest=FreshDatabaseMigrationSqlServerTest -Dsurefire.failIfNoSpecifiedTests=false test`. Test áp dụng V1–V9, kiểm tra unique active links và soft-delete, tạo duplicate legacy tổng hợp trước V9, xác nhận khi preflight fail thì hai row/tracking/index không đổi, rồi soft-delete một row và retry V9 thành công. `ReceptionSqlServerPersistenceTest` pass 1/1 trên `clinic_test_v9_20261106`, kiểm tra form đăng ký headless qua handler/composition, persistence/readback bệnh nhân và BHYT, liên kết cơ sở, tìm kiếm và queue; `AuditSqlServerPersistenceTest` pass 1/1 trên database tổng hợp `clinic_test_audit_20261105`, kiểm tra append-only V6 và rollback đăng ký khi audit insert bị từ chối. Task board còn ghi nhận các kiểm tra Tiếp đón opt-in riêng về đọc lại qua process thứ hai, rollback nhiều bảng và đồng thời queue/CCCD. Các kết quả SQL Server opt-in này tách biệt khỏi baseline mặc định **254/0/0/4**; bốn SQL Server opt-in tests vẫn được skip trong lượt mặc định. Chỉ dùng fixture tổng hợp; không database hiện hữu nào bị sửa hoặc drop. Preflight test không phải production migration rehearsal.
- Failure-safety migration không còn là khoảng trống hoàn toàn: `DatabaseMigrationRunnerTest` kiểm tra batch lỗi và lỗi ghi migration tracking đều rollback, không commit; đồng thời giữ lỗi migration gốc nếu rollback cũng lỗi. Đây là coverage unit với JDBC giả lập, không phải bằng chứng rằng mọi kịch bản rollback khi chạy migration thực tế trên SQL Server đã được kiểm chứng.

## Tiếp đón và trạng thái Git

- `ReceptionCompositionRoot` nối các handler Tiếp đón với JDBC; task board ghi nhận các kiểm tra SQL Server tổng hợp riêng trên `clinic_test_v9_20261106`, `clinic_test_v9_preflight_20261106` và `clinic_test_audit_20261105`. Test source giới hạn database và dùng fixture tổng hợp; lần cập nhật tài liệu này không kết nối database. Không đưa credentials hay dữ liệu cá nhân vào tài liệu.
- Nhánh dự án `feature/patient-reception-workflow` chứa các commit task được tích hợp cục bộ; không có bằng chứng push, release hoặc GitHub-hosted CI run. Kết quả build/test không đồng nghĩa Phase 0, Phase 1 hoặc Phase 2 hoàn tất.
- **UI chưa được xác minh trực tiếp:** cửa sổ Swing chưa được quan sát; headless form-to-persistence evidence không chứng minh hiển thị/layout, tương tác đầy đủ `ReceptionPanel`, hay toàn bộ UI-to-database acceptance.
- Task board vẫn để `PAT-01`, `PAT-02`, `REC-01` và `SEC-01` ở trạng thái `[~]`. Các gate còn mở gồm visual/interaction acceptance, queue lifecycle/transfer, Doctor UI tiền sử và các quyết định/policy security chưa triển khai; vì vậy Phase 0, Phase 1, Phase 2 và project tổng thể chưa được xác nhận hoàn tất. Phase 3–9 vẫn theo roadmap, không được đóng chỉ vì build hoặc migration pass.
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
- `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunner.java`, `clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/{FreshDatabaseMigrationSqlServerTest,AuditSqlServerPersistenceTest,DatabaseMigrationRunnerTest}.java`, và `clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/reception/{ReceptionSqlServerPersistenceTest,ReceptionSqlServerProcessRestartTest}.java`
- `docs/ke-hoach-trien-khai/02-cac-dot-trien-khai.md` và `docs/ke-hoach-trien-khai/03-model-task-board.md`
- `README.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `UI_CODING_STANDARDS.md`


## Evidence addendum — patient history SQL Server acceptance (2026-11-06)

`PAT-HISTORY-SQL-01` is complete locally. On newly created disposable `clinic_test_patient_history_20261106`, V1–V10 migration passed 1/1 and `PatientHistorySqlServerPersistenceTest` passed 1/1, covering V10 trigger-safe insert, exact scope/category filtering, update/version, read audit `PATIENT_VIEW`/`PATIENT`, and mutation/audit rollback. Only synthetic fixture rows and its temporary trigger were cleaned; the database remains. The current root default `mvn -Djava.awt.headless=true clean verify` baseline is 305 tests, 0 failures/errors, 5 opt-in skips (65 Surefire reports). The Doctor history UI is still not wired because the station has mock DTOs without real patient/session context; Phase 1 remains open. No push or PR.
