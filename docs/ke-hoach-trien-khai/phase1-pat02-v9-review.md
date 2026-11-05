# Review V9 — Duy nhất liên kết bệnh nhân–cơ sở

**Phạm vi:** Đọc mã migration V9, đăng ký runner, test, schema và contract đã duyệt. Không sửa source/test, không kết nối database và không chạy migration.

## Kết luận

Không phát hiện lỗi cú pháp hay sai khác contract trong mã V9 khi đọc tĩnh. Migration dùng preflight để từ chối dữ liệu legacy có cặp active trùng, sau đó tạo filtered unique index cho `isDeleted = 0`. Runner đăng ký V9 sau V8 và tách `GO` thành hai batch.

**Tình trạng nghiệm thu:** Chưa có bằng chứng trong lượt review này về thực thi trên SQL Server. Test hiện tại không chứng minh SQL Server thật từ chối insert trùng active hoặc cho phép tái liên kết sau soft delete. Không xem sự hiện diện của index trong code/test nguồn là SQL Server runtime acceptance.

## Findings (xếp theo mức độ)

### P2 — Thiếu kiểm chứng runtime cho hành vi của filtered unique index

- `ActivePatientFacilityMigrationTest` chỉ đọc file và assert các chuỗi SQL (`clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/ActivePatientFacilityMigrationTest.java:14-26`). Tên test nói “FailsSafely”, nhưng test không tạo dữ liệu trùng và không chạy script trên SQL Server.
- Test runner SQL Server opt-in hiện có chạy V1–V9 và chỉ xác nhận bản ghi migration cùng metadata `is_unique = 1`, `has_filter = 1` (`FreshDatabaseMigrationSqlServerTest.java:37-48,77-106`). Nó không thử insert cặp active thứ hai, insert lại sau khi xóa mềm, hay restore bản ghi đã xóa.
- Test fake JDBC về batch/rollback tổng quát không thực thi cú pháp hay semantics SQL Server; phần chạy migration thật ở test trên chỉ được bật với `clinic.sqlserver.migration.enabled=true`.
- **Tác động:** chưa có bằng chứng test cho chặn trùng đồng thời/ở DB, quy tắc tái kích hoạt, hoặc việc rollback preflight khi có duplicate legacy. Đây là gap kiểm chứng, không phải lỗi đã xác nhận trong SQL.
- **Bằng chứng cần bổ sung trước khi kết luận runtime:** chạy opt-in trên database SQL Server thử nghiệm được chọn rõ ràng; xác nhận V9 fail với hai dòng active cùng cặp mà không ghi migration thành công/không còn index dở dang; sau xử lý dữ liệu thì migration thành công; kiểm tra insert active trùng bị từ chối, cặp có thể được tái sử dụng khi liên kết cũ soft-delete, và restore lại bị từ chối nếu gây trùng. Ghi lại lệnh, kết quả và môi trường thử nghiệm. Không dùng dữ liệu bệnh nhân thật.

### P2 — Chưa có luồng repository/use case và ánh xạ lỗi cho vi phạm duy nhất

- Contract Phase 2 nói rõ use case/repository vẫn phải ánh xạ vi phạm index thành kết quả nghiệp vụ khi được triển khai (`phase2-implementation-contract.md:13`). Quyết định gốc cũng yêu cầu không tự xóa/gộp dòng legacy (`phase2-role-permission-matrix-proposal.md:220`).
- Trong source Java, `BenhNhanCoSo` hiện là domain entity (`clinic-domain/src/main/java/vn/clinicmanagement/domain/patient/BenhNhanCoSo.java:6-13`); `rg` không tìm thấy persistence adapter hoặc use case ghi liên kết này. Board PAT-02 cũng ghi thiếu facility/history persistence coverage (`03-model-task-board.md:66`).
- **Tác động/phạm vi:** database constraint là hàng rào cuối cùng đúng với contract, nhưng thao tác ghi PAT-02 khi được nối sẽ chưa có kết quả nghiệp vụ thân thiện nếu chỉ để `SQLException` thoát ra. Đây là công việc PAT-02 còn mở, không phải sai sót của migration V9 độc lập.
- **Khuyến nghị:** khi triển khai persistence, map lỗi unique index có tên `UX_BenhNhanCoSo_Active_Patient_Facility` sang lỗi nghiệp vụ ổn định; không bắt mọi lỗi SQL như lỗi duplicate. Bổ sung test cho mapping cùng lúc với test persistence phù hợp.

## Đối chiếu tính đúng đắn và an toàn

- **Contract:** DEC-PAT-01 chốt mỗi cặp (`maBenhNhan`, `maCoSo`) chỉ có một dòng chưa xóa mềm; duplicate legacy phải làm migration dừng, không tự chọn bản ghi xử lý. V9 đáp ứng bằng preflight `GROUP BY ... HAVING COUNT(*) > 1` trên `isDeleted = 0`, `THROW 51090` với thông điệp không chứa giá trị bệnh nhân/cơ sở, rồi tạo unique index filtered (`V9__Enforce_Active_Patient_Facility_Uniqueness.sql:1-17`; contract `phase2-implementation-contract.md:13`).
- **SQL Server / `GO`:** `CREATE UNIQUE NONCLUSTERED INDEX ... WHERE isDeleted = 0` là hình thức filtered unique index phù hợp cho điều kiện này. `GO` đứng riêng sau batch preflight (`V9...sql:13`); `SqlServerBatchSplitter` tách `GO` độc lập, không phân biệt hoa thường/khoảng trắng (`SqlServerBatchSplitter.java:23-47,54-55`), và runner gửi từng batch theo thứ tự (`DatabaseMigrationRunner.java:172-179`). Nhận định này là phân tích cú pháp tĩnh, không thay thế chạy SQL Server.
- **Thất bại duplicate legacy:** lỗi ở batch đầu làm batch tạo index không chạy. `executeMigration` ghi tracking sau khi tất cả batch thành công và commit; SQL exception dẫn tới rollback (`DatabaseMigrationRunner.java:183-196`). Vì mỗi migration được commit riêng (`DatabaseMigrationRunner.java:84-86,183-188`), rollback của V9 không hoàn tác migration trước đó đã commit; bảng tracking được khởi tạo/commit riêng nếu cần (`DatabaseMigrationRunner.java:102-118`). Dữ liệu duplicate không bị sửa/xóa.
- **Soft delete:** dòng `isDeleted = 1` nằm ngoài predicate nên không chiếm unique key; có thể tạo liên kết active mới cho cùng cặp. Nếu restore dòng cũ trong lúc dòng mới còn active, unique index sẽ từ chối thao tác. Đây là semantics phù hợp contract, nhưng chưa được test runtime như nêu ở Finding P2.
- **Registration:** V9 nằm cuối danh sách sau V8 (`DatabaseMigrationRunner.java:40-50`); test phản chiếu thứ tự đăng ký đủ V1–V9 (`PermissionMatrixMigrationScriptTest.java:40-56`).

## Tình trạng test và kiểm định

| Hạng mục | Bằng chứng trong source | Kết quả review |
|---|---|---|
| Nội dung V9 | Assert preflight, error number, index và predicate trong test tĩnh (`ActivePatientFacilityMigrationTest.java:14-26`) | Có xác nhận cấu trúc văn bản; không xác nhận T-SQL chạy được |
| Runner/batching/transaction | Fake JDBC kiểm tra generic split và rollback (`DatabaseMigrationRunnerTest.java:57-102,104-162`); đăng ký runner kiểm tra reflection (`PermissionMatrixMigrationScriptTest.java:40-56`) | Không gọi SQL Server; test script mẫu thực thi V7, không phải V9 (`DatabaseMigrationRunnerTest.java:24-55`) |
| SQL Server integration | Opt-in test áp dụng V1–V9 và đọc catalog index (`FreshDatabaseMigrationSqlServerTest.java:37-48,77-106`) | Có đường kiểm thử runtime trong repository; chưa chạy trong lượt review, assertions không thử enforcement của dữ liệu |
| Lệnh lượt review | `git diff --check` | Chạy sau khi tạo báo cáo; kết quả ghi trong completion summary |

## Guidance đã đọc

- `docs/PROJECT_CONVENTIONS.md` — nội dung Mandatory project guidance được Orchestrator cung cấp trong task.
- `README.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `UI_CODING_STANDARDS.md`.
- `docs/ke-hoach-trien-khai/README.md`, `00-hien-trang.md`, `02-cac-dot-trien-khai.md`, `03-model-task-board.md`, `phase2-implementation-contract.md`, `phase2-role-permission-matrix-proposal.md`.

Các tài liệu roadmap xác định Phase 1/PAT-02 còn đang triển khai và yêu cầu đánh giá dựa trên bằng chứng; UI coding standards không đặt quy tắc riêng ảnh hưởng đến migration này.
