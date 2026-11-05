# Phase 2 — Implementation contract

**Trạng thái:** Hợp đồng kỹ thuật cho các policy được duyệt ngày 05/11/2026. Các task Phase 2 phải đọc file này trước khi sửa code.
**Policy nguồn:** [phase2-security-policy-proposal.md](phase2-security-policy-proposal.md).

## 1. Quyết định nghiệp vụ được duyệt

- Mỗi phiên chỉ có một role đã gán cho tài khoản; role/action không được phép thì từ chối mặc định.
- Mỗi phiên chọn một cặp cơ sở/khoa lấy từ assignment còn hiệu lực. Cặp hợp lệ phải đồng thời có assignment cơ sở, assignment khoa và quan hệ `Khoa.maCoSo = PhanQuyenCoSo.maCoSo`; cơ sở/khoa inactive hoặc soft-deleted không được chọn.
- Tự khóa sau 15 phút không thao tác; tuổi thọ tuyệt đối tối đa 12 giờ. Auto-lock che workspace, giữ draft, và chỉ mở lại sau xác thực cùng tài khoản/role. Logout chủ động cảnh báo draft chưa lưu, xóa context và đóng workspace.
- Thay đổi role/scope/account có hiệu lực ở thao tác nhạy cảm kế tiếp; Application phải kiểm tra trạng thái mới từ nguồn tin cậy.
- Audit cho các nhóm sự kiện ở policy source; thay đổi nghiệp vụ và audit phải cùng transaction. Audit write failure rollback cả thao tác nghiệp vụ. Audit chỉ append; không purge đến khi có retention được duyệt.
- Tra cứu audit yêu cầu quyền riêng `AUDIT_READ`, mặc định deny. Không tự gán quyền này cho role nào trong seed.
- Không ghi password/hash/token/connection string, CCCD/BHYT đầy đủ hoặc narrative lâm sàng tự do vào audit/log.

## 2. Ranh giới tin cậy

- Login UI chỉ thu credential và lựa chọn từ danh sách roles/scopes do Application trả về; UI không tự tạo role, employee ID hoặc scope.
- Tạo một immutable authenticated context sau khi credential, role assignment và scope assignment đều được xác minh. Context tối thiểu: employee ID, role ID/code, facility ID, department ID, issued-at, last-activity, absolute-expiry và session ID.
- Command/query nhạy cảm nhận context từ composition/application boundary. Không dùng setter công khai của UI như nguồn xác thực. Role/scope phải được xác nhận lại theo assignment/permission matrix trước tác vụ nhạy cảm.
- UI chỉ ẩn/hiện thao tác theo quyền để hỗ trợ người dùng; authorization luôn được kiểm tra ở Application.
- Mọi ví dụ, fixture, integration test dùng dữ liệu synthetic; không đọc hoặc ghi hồ sơ bệnh nhân thật.

## 3. Thứ tự thực hiện

1. **SEC-DATA-01 — assignment reads (complete, `2d062f8` + `0d91903`):** repository đọc assigned role IDs/codes và cặp cơ sở/khoa hợp lệ, kèm tên cơ sở/khoa làm nhãn lựa chọn; fake-JDBC tests pass. Chưa thay UI/login.
2. **SEC-LOGIN-01 — role/scope selection (complete; `a5ff050`, `fcd8dda`, `367e065`):** chỉ sau khi credential hợp lệ, Application trả toàn bộ role/scope assignment; không chọn role đầu tiên và không truy vấn assignment trước auth. UI bắt buộc chọn khi có nhiều choice, chỉ auto-select khi có đúng một choice; hủy chọn không tạo session/dashboard. Thiếu role hoặc scope bị từ chối trước khi ghi nhận đăng nhập thành công. `UserSession` lưu role ID/code/name và facility/department ID/name; station/workstation là context riêng và được xóa khi logout/đăng nhập mới. Choice lists bất biến, nhãn UI không lộ database IDs. Auth regression tests cũ được giữ lại. Orchestrator và root `mvn clean verify` pass (88 tests; 3 SQL Server opt-in skips).
3. **SEC-AUTHZ-MATRIX-PROPOSAL-01 — approved (`phase2-role-permission-matrix-approval.md`):** Chủ dự án duyệt role/action baseline trong ma trận 5.2 ngày 05/11/2026. Mọi grant phải bị giới hạn về đúng cặp `KHOA_PHONG`; không cho access liên khoa (`DEC-AUTHZ-03` chọn cô lập nghiêm). `AUDIT_READ` deny tuyệt đối; không tự cấp/sửa quyền cho role đang đăng nhập. `DEC-AUTHZ-01/02/04/05/06/07/08` còn mở và các action liên quan giữ deny.
4. **SEC-SESSION-CONFIG-01 — complete (`0e4d8c6`, `f75a672`):** `ISystemRepository` read adapter, migration V5 seed idempotent và `DatabaseMigrationRunner` đăng ký V5 sau V4. Fresh-database opt-in acceptance kiểm tra đủ V1–V5, mỗi timeout key đúng một lần với giá trị 15/12; root JDK 17 full verify pass khi bật headless. SQL Server opt-in chưa chạy ở lượt tích hợp này.
5. **SEC-SESSION-01 — lifecycle complete (`51a2ae2`):** idle 15 phút, absolute 12 giờ, overlay giữ workspace/draft, re-auth cùng user/role/scope, logout và xóa context; fake-clock/headless tests pass. Full JDK 17 `mvn -Djava.awt.headless=true clean verify` pass (101 cases, 3 SQL Server opt-in skips). Desktop visual smoke check còn mở.
6. **SEC-AUDIT-DESIGN-01 — complete:** schema/API/task decomposition được ghi tại `phase2-audit-implementation-proposal.md`; implementation và SQL rollback acceptance được theo dõi riêng ở SEC-AUDIT-01A–01D. `AUDIT_READ` giữ deny/no-purge.
7. **SEC-AUTHZ-01A — foundation complete (`415f85d`, `a66acc7`):** action codes ổn định, domain/SQL defaults fail-closed, V7 constraint/index và giữ nguyên legacy rows; chưa seed grants, chưa bật guard. Root verify 134 tests pass, SQL Server V7 migration/persistence opt-in pass trên `clinic_test_audit_20261105`.
8. **SEC-AUTHZ-02A — evaluator complete (`86ff835`):** dùng immutable Application context và default-deny decision service; xác minh session active, employee, role assignment, exact pair, resource pair và permission ở mỗi request. Synthetic tests và root full verify pass: 147 tests, 0 failures/errors, 4 SQL Server opt-in skips. Chưa gắn vào UI/use cases.
9. **SEC-AUTHZ-02B — reception write guard (complete; `c74971c`, review fix `996dc62`):** nối evaluator vào patient/insurance/visit registration/update, revalidate session và scope ở Application; patient update lookup theo ID và cặp cơ sở+khoa, CCCD collision ngoài scope trả thông báo chung. Worktree và root `mvn -Djava.awt.headless=true clean verify` pass 156 tests, 0 failures/errors, 4 SQL Server opt-in skips. Không bao gồm patient/queue reads.
10. **SEC-AUTHZ-02C — protected read paths (complete; integrated `826c1e7`):** patient search, queue, room picker và room summaries được scope theo exact facility+department; sau query append successful-read audit bằng transaction độc lập trước khi trả dữ liệu; audit lỗi thì fail closed, không trả dữ liệu. Dùng authorization grants đã duyệt với scope `KHOA_PHONG`; unresolved DEC actions và `AUDIT_READ` luôn deny. Antigravity headless attempt không tạo code do adapter từ chối quyền command; Codex retry tạo code, review sửa blank-keyword case và root sửa hai SQL Server fixture để compile/context chính xác. Root JDK 17 `mvn -Djava.awt.headless=true clean verify` pass 165 tests, 0 failures/errors, 4 SQL Server opt-in skips; `git diff --check` pass. SQL Server persistence/restart opt-in chưa chạy lượt này.
11. **SEC-AUDIT-01A–01D — write path/integration (01A–01C complete; 01D complete):** V6 append-only writer, audit auth/session/reception trong transaction và SQL Server rollback test trên DB synthetic. Audit read vẫn bị từ chối.
12. **SEC-UI-01 — screens and acceptance:** nối scope selector, lock overlay, logout confirmation và permission-aware navigation; headless Swing tests rồi desktop smoke check.

## 4. Gates và chính sách chưa duyệt

- `SEC-01` phải qua unit, persistence/transaction integration và UI acceptance trước EMR, prescription/pharmacy, billing hoặc mở rộng sang dữ liệu thật.
- Login lockout sau đăng nhập sai chưa được duyệt. Không thêm cột, trigger, counter, thời lượng khóa hoặc UI cho lockout cho đến khi có quyết định riêng.
- Audit retention cụ thể chưa được quyết định. Giữ no-purge; `AUDIT_READ` được Chủ dự án duyệt là deny cho mọi role trong baseline hiện hành, không seed quyền đọc audit.
- Nếu source/schema hiện có không thể biểu đạt đúng policy, task phải dừng ở contract/report và báo cáo phương án trước khi mở rộng migration hoặc thay dữ liệu.
