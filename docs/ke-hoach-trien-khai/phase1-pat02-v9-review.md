# Review V9 — Duy nhất liên kết bệnh nhân–cơ sở

**Phạm vi:** Đọc mã migration V9, đăng ký runner, test, schema và contract đã duyệt. Không sửa source/test, không kết nối database và không chạy migration.

**Lưu ý về thời điểm:** Phần review gốc dưới đây là snapshot report-only trước khi có kiểm thử runtime V9 và nối repository. Đến 06/11/2026, DEC-PAT-01 đã được Owner tái xác nhận; `BenhNhanCoSoRepository`/đăng ký tiếp đón đã được nối; SQL Server opt-in `MoiCoSoDuLieuDiCuSQLMayChuTest` đã chạy trên database synthetic `clinic_test_v9_20261106`. Addendum cuối báo cáo ghi trạng thái mới nhất để không nhầm snapshot cũ với kết quả hiện hành.

## Kết luận

Không phát hiện lỗi cú pháp hay sai khác contract trong mã V9 khi đọc tĩnh. Migration dùng preflight để từ chối dữ liệu legacy có cặp active trùng, sau đó tạo filtered unique index cho `isDeleted = 0`. Runner đăng ký V9 sau V8 và tách `GO` thành hai batch.

**Tình trạng tại thời điểm review gốc:** Chưa có bằng chứng thực thi SQL Server trong lượt review đó. Đây không còn là trạng thái hiện tại; xem addendum.

## Findings (xếp theo mức độ)

### P2 — Thiếu kiểm chứng runtime cho hành vi của filtered unique index

- `HoatDongBenhNhanCoSoDiCuTest` chỉ đọc file và assert các chuỗi SQL (`clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/HoatDongBenhNhanCoSoDiCuTest.java:14-26`). Tên test nói “FailsSafely”, nhưng test không tạo dữ liệu trùng và không chạy script trên SQL Server.
- Test runner SQL Server opt-in hiện có chạy V1–V9 và chỉ xác nhận bản ghi migration cùng metadata `is_unique = 1`, `has_filter = 1` (`MoiCoSoDuLieuDiCuSQLMayChuTest.java:37-48,77-106`). Nó không thử insert cặp active thứ hai, insert lại sau khi xóa mềm, hay restore bản ghi đã xóa.
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
- **SQL Server / `GO`:** `CREATE UNIQUE NONCLUSTERED INDEX ... WHERE isDeleted = 0` là hình thức filtered unique index phù hợp cho điều kiện này. `GO` đứng riêng sau batch preflight (`V9...sql:13`); `SQLMayChuLoTach` tách `GO` độc lập, không phân biệt hoa thường/khoảng trắng (`SQLMayChuLoTach.java:23-47,54-55`), và runner gửi từng batch theo thứ tự (`CoSoDuLieuDiCuChay.java:172-179`). Nhận định này là phân tích cú pháp tĩnh, không thay thế chạy SQL Server.
- **Thất bại duplicate legacy:** lỗi ở batch đầu làm batch tạo index không chạy. `executeMigration` ghi tracking sau khi tất cả batch thành công và commit; SQL exception dẫn tới rollback (`CoSoDuLieuDiCuChay.java:183-196`). Vì mỗi migration được commit riêng (`CoSoDuLieuDiCuChay.java:84-86,183-188`), rollback của V9 không hoàn tác migration trước đó đã commit; bảng tracking được khởi tạo/commit riêng nếu cần (`CoSoDuLieuDiCuChay.java:102-118`). Dữ liệu duplicate không bị sửa/xóa.
- **Soft delete:** dòng `isDeleted = 1` nằm ngoài predicate nên không chiếm unique key; có thể tạo liên kết active mới cho cùng cặp. Nếu restore dòng cũ trong lúc dòng mới còn active, unique index sẽ từ chối thao tác. Đây là semantics phù hợp contract, nhưng chưa được test runtime như nêu ở Finding P2.
- **Registration:** V9 nằm cuối danh sách sau V8 (`CoSoDuLieuDiCuChay.java:40-50`); test phản chiếu thứ tự đăng ký đủ V1–V9 (`QuyenMaTranDiCuKichBanTest.java:40-56`).

## Tình trạng test và kiểm định

| Hạng mục | Bằng chứng trong source | Kết quả review |
|---|---|---|
| Nội dung V9 | Assert preflight, error number, index và predicate trong test tĩnh (`HoatDongBenhNhanCoSoDiCuTest.java:14-26`) | Có xác nhận cấu trúc văn bản; không xác nhận T-SQL chạy được |
| Runner/batching/transaction | Fake JDBC kiểm tra generic split và rollback (`CoSoDuLieuDiCuChayTest.java:57-102,104-162`); đăng ký runner kiểm tra reflection (`QuyenMaTranDiCuKichBanTest.java:40-56`) | Không gọi SQL Server; test script mẫu thực thi V7, không phải V9 (`CoSoDuLieuDiCuChayTest.java:24-55`) |
| SQL Server integration | Opt-in test áp dụng V1–V9 và đọc catalog index (`MoiCoSoDuLieuDiCuSQLMayChuTest.java:37-48,77-106`) | Có đường kiểm thử runtime trong repository; chưa chạy trong lượt review, assertions không thử enforcement của dữ liệu |
| Lệnh lượt review | `git diff --check` | Chạy sau khi tạo báo cáo; kết quả ghi trong completion summary |

## Guidance đã đọc

- `docs/PROJECT_CONVENTIONS.md` — nội dung Mandatory project guidance được Orchestrator cung cấp trong task.
- `README.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `UI_CODING_STANDARDS.md`.
- `docs/ke-hoach-trien-khai/README.md`, `00-hien-trang.md`, `02-cac-dot-trien-khai.md`, `03-model-task-board.md`, `phase2-implementation-contract.md`, `phase2-role-permission-matrix-proposal.md`.

Các tài liệu roadmap xác định Phase 1/PAT-02 còn đang triển khai và yêu cầu đánh giá dựa trên bằng chứng; UI coding standards không đặt quy tắc riêng ảnh hưởng đến migration này.

## Addendum — trạng thái sau nghiệm thu runtime (06/11/2026)

- Owner tái xác nhận DEC-PAT-01: mỗi cặp (`maBenhNhan`, `maCoSo`) có tối đa một dòng active; bệnh nhân vẫn có thể liên kết tới các cơ sở khác nhau. Migration không tự sửa dữ liệu legacy trùng.
- `MoiCoSoDuLieuDiCuSQLMayChuTest` chạy opt-in trên SQL Server database synthetic `clinic_test_v9_20261106`, áp dụng V1–V9 và xác nhận index filtered được tạo, insert active duplicate bị SQL Server từ chối (2601), soft-delete rồi tạo lại liên kết active thành công, và cả hai bản ghi lịch sử được giữ lại. Một lượt opt-in độc lập tiếp theo đã kiểm chứng thêm preflight duplicate legacy; xem bằng chứng và lệnh cụ thể bên dưới.
- Migration V9 kiểm tra duplicate legacy trước khi tạo index và dừng với lỗi 51090. Lượt runtime preflight xác nhận đường lỗi này không ghi tracking migration thành công, không tạo index, không sửa/xóa hai dòng synthetic; sau khi soft-delete một dòng thì retry V9 thành công.
- `MoiCoSoDuLieuDiCuSQLMayChuTest` được mở rộng trong task `PAT-V9-PREFLIGHT-01` để tạo duplicate legacy synthetic trên DB rỗng và chạy migration runner. Lệnh chạy thành công là `mvn -pl clinic-infrastructure -am -Djava.awt.headless=true -Dclinic.sqlserver.migration.enabled=true -Dclinic.sqlserver.test.database=clinic_test_v9_preflight_20261106 -Dtest=MoiCoSoDuLieuDiCuSQLMayChuTest -Dsurefire.failIfNoSpecifiedTests=false test`; kết quả **1/1 pass** trên database SQL Server mới, rỗng và disposable `clinic_test_v9_preflight_20261106`. Test áp dụng V1–V8, tạo hai liên kết active tổng hợp trùng trước V9, xác nhận preflight failure giữ nguyên rows/tracking/index, soft-delete một dòng synthetic rồi retry thành công, hoàn tất V1–V9 và kiểm tra uniqueness/soft-delete. Đây là runtime acceptance cho nhánh preflight, không phải production migration rehearsal.
- Root JDK 17 default `mvn -Djava.awt.headless=true clean verify` pass **246 tests, 0 failures, 0 errors, 4 skipped** trên bốn module có test; các SQL Server opt-in tests được skip và không nằm trong số mặc định. Chỉ synthetic data được sử dụng; không database hiện hữu nào bị sửa hoặc drop.
- Repository `BenhNhanCoSoRepository` và registration wiring đã được triển khai; acceptance `PAT-FACILITY-WIRE-01`/`REC-UI-DB-01` xác nhận ensure trong transaction, actor/facility từ session và readback tổng hợp. Đường đọc quyền vẫn theo exact facility+department/visit, liên kết không cấp quyền truy cập.
- Root JDK 17 `mvn -Djava.awt.headless=true clean verify` gần nhất pass **246 tests, 0 failures, 0 errors, 4 skipped**; SQL Server opt-in được báo cáo riêng, không tính trong con số mặc định.

**Trạng thái hiện tại:** DEC-PAT-01 được giữ nguyên. V9 uniqueness/soft-delete và legacy-duplicate preflight đều có bằng chứng SQL Server runtime opt-in, nhưng chỉ đóng `PAT-FACILITY-01` và `PAT-V9-PREFLIGHT-01` trong đúng phạm vi các acceptance test nêu trên. `PAT-02`, Phase 1 và `SEC-01` vẫn đang tiến hành/chưa qua gate; không có căn cứ coi đây là production migration rehearsal hoặc nghiệm thu migration trên cơ sở dữ liệu hiện hữu.
