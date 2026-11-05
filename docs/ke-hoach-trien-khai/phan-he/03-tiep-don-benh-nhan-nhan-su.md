# Phân hệ 03 — Hồ sơ bệnh nhân, BHYT và Tiếp đón

**Trạng thái:** `[~] Code và regression tests đã commit local; còn kiểm thử tích hợp và nghiệm thu end-to-end`
**Phase:** 1; quản trị nhân sự mở rộng ở Phase 8.
**Domain:** `patient`, `identity`, `facility`, `appointment`.

> Current evidence (06/11/2026): root JDK 17 `mvn -Djava.awt.headless=true clean verify` passes 203 tests, 0 failures/errors, 4 SQL Server opt-in skips. PAT-FIELDS-01 proves all 15 reception-form fields map through the command and patient SELECT/INSERT/UPDATE paths. V9 adds one-active-link-per-patient/facility enforcement; its opt-in SQL test now covers duplicate rejection and soft-delete replacement in code but has not run on SQL Server. Patient-facility persistence wiring is in progress. Phase 1 remains open: full Swing UI-to-DB E2E, V9 SQL Server execution, clinical-history authorization/workflow, and broader business acceptance are incomplete. Earlier JDK 17 smoke started the Swing AWT event loop, but the window itself was not observed.

## Hiện có trong source

- **Application:** `RegisterPatientHandler`, `SearchPatientsHandler`, `GetReceptionQueueHandler` cùng command/query tương ứng.
- **Infrastructure:** `BenhNhanRepositoryImpl`, `BaoHiemRepositoryImpl`, `PhongKhamRepositoryImpl`, `ReceptionQueueRepositoryImpl`; `DatabaseMigrationRunner` đăng ký V1–V9. `BenhNhanCoSo` write adapter and its registration wiring are being implemented under `PAT-FACILITY-WIRE-01`.
- **Desktop:** `ReceptionCompositionRoot`, `ReceptionPanel`, `PatientRegistrationFormPanel`, `ClinicRoomStatusPanel`; các thao tác tải, tìm kiếm và lưu reception dùng `SwingWorker`.
- **Luồng lưu:** handler validate dữ liệu bắt buộc/phòng/BHYT và dùng `TransactionBoundary`; ghi bệnh nhân, thẻ BHYT nếu có, rồi tạo lượt khám/queue. Contract bổ sung yêu cầu tạo/idempotently ensure `BenhNhanCoSo` trong cùng transaction, lấy facility/actor từ authenticated context; adapter wiring chưa tích hợp. Test opt-in trước đó xác nhận persist/search/queue/reload bằng composition mới trên SQL Server synthetic. Manual failure injection xác nhận rollback không còn active patient/BHYT/visit rows.
- **Tìm kiếm:** query repository theo keyword (mã bệnh nhân, CCCD, số điện thoại, họ tên theo logic repository), giới hạn 50 kết quả và ghép BHYT gần nhất. Save/search/reload qua composition mới và JVM process con độc lập đã pass; chưa phải restart toàn bộ ứng dụng Swing.

## Bằng chứng đã có

- Tại `0d91903` có **18 test sources và 78 test cases được phát hiện**: domain 4/21, application 5/26, infrastructure 5/21, desktop 4/10; ba SQL Server opt-in tests bị skip trong default verify (hai test reception, một fresh-migration thuộc Phase 0). Root `mvn clean verify` pass trên JDK 17 với 0 failures/errors. `ReceptionSqlServerPersistenceTest` pass riêng 1/1 trên DB synthetic, xác nhận đăng ký+search+queue+reload và update patient+BHYT; process-restart probe pass riêng 1/1. Test suite không chứng minh Phase 2 access/audit.
- `PhoneNumberTest` kiểm tra định dạng số di động Việt Nam, prefix `0`/`+84`, dấu phân cách và đầu vào sai; đây chỉ là validation domain.
- `V4__Enforce_Active_Patient_CCCD_Uniqueness.sql` dừng nếu tồn tại CCCD trùng trên hồ sơ active trước khi tạo unique index. Test application kiểm tra nhánh duplicate không ghi BHYT/lượt khám; test infrastructure dùng lỗi tổng hợp xác nhận chỉ ánh xạ đúng index CCCD và không lộ giá trị trùng.
- `V9__Enforce_Active_Patient_Facility_Uniqueness.sql` dừng an toàn nếu cặp bệnh nhân–cơ sở đang hoạt động bị trùng; unique filtered index bảo vệ ghi đồng thời. Static review `phase1-pat02-v9-review.md` không tìm thấy lỗi migration, nhưng SQL Server runtime behavior chưa được chạy.
- `PAT-FIELDS-01` (`b92efd0`) covers all 15 fields currently exposed by the reception form across form-to-command conversion and repository SELECT/INSERT/UPDATE mapping. It does not cover fields that are not present on that form, credentials, or `TienSuBenh`.
- UI source nhận lỗi duplicate CCCD, hướng dẫn tìm hồ sơ hiện hữu, chạy tìm kiếm với CCCD và giữ form để tiếp tục. `PatientRegistrationFormPanelTest` bổ sung headless component coverage; commit `ea397f9` sửa giả định chọn phòng để kết quả test deterministic. Đây không phải end-to-end UI/JDBC test.
- Evidence commits trong local history: `ef602a9` (fresh-database migration test), `3e3fe6f` (distinct JVM persistence probe), `99d709b`, `75a218d`, `fcfc62b`, `9ff7ddd`, `3f6ba31`, `ea397f9`, `264bbae`, `ff2045a`, `5d68840` (migration rollback safety), `a3071fc` (domain-event collection), `9b5ff17` (safe registration error mapping), `2d062f8`/`0d91903` (staff role/scope assignments). Migration runner đã pass trên database synthetic trống riêng; DB này được giữ với demo seed nhưng 0 patient rows. Manual checks xác nhận rollback multi-table; 8 queue allocations đồng thời nhận số khác nhau; hai CCCD giống nhau có một success và một lỗi nghiệp vụ. Test patient records được soft-delete.

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
- [~] V1–V9 have code-level migration coverage; V9 duplicate-active-link preflight/index behavior still needs an opt-in run on a dedicated empty SQL Server test database. Existing V4 active-CCCD SQL Server evidence is separate and does not prove V9.
- [ ] Kiểm thử UI-to-DB E2E: lỗi không xóa form, DB I/O ngoài EDT và queue refresh sau đăng ký. Headless panel test không chứng minh luồng tích hợp này.
- [x] Xác nhận 15 trường form ánh xạ đúng command/entity/patient SQL SELECT/INSERT/UPDATE (`PAT-FIELDS-01`, `b92efd0`). Mở rộng sang các field không có trên form là việc riêng, không được suy ra từ test này.
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
