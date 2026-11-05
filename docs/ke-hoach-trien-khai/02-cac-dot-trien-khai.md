# 02 — Lộ trình triển khai theo phase

Lộ trình có **đúng 10 phase, đánh số 0–9**. Thứ tự dưới đây là contract cho roadmap; việc model hoặc UI đã có không đồng nghĩa phase đã nghiệm thu.

## Phase 0 — Nền tảng

**Trạng thái:** `[~]` Nền tảng và wiring cơ bản có trong source; validation/release chưa hoàn tất.
**Phụ thuộc:** Không.
**Phạm vi:** Maven modules, application shell, schema/migrations, JDBC, transaction/unit of work và cấu hình môi trường.
**Bằng chứng cập nhật (06/11/2026):** Root JDK 17 default `mvn -Djava.awt.headless=true clean verify` pass **246 tests, 0 failures, 0 errors, 4 skipped** trên bốn module có test; cả bốn skip là SQL Server opt-in và không chạy trong verify mặc định. Các lượt SQL Server synthetic opt-in được ghi riêng: `FreshDatabaseMigrationSqlServerTest` pass 1/1 trên `clinic_test_v9_20261106` khi áp dụng V1–V9; lần chạy mới trên database rỗng disposable `clinic_test_v9_preflight_20261106` cũng pass 1/1 với preflight duplicate legacy, bảo toàn rows/tracking/index khi V9 thất bại và retry thành công sau soft delete. Không lượt opt-in nào được gộp vào 246 test mặc định. Unit tests bao phủ lỗi batch/tracking migration rollback, không commit và giữ exception gốc. Kiểm tra artifact trước đó xác nhận shaded JAR không chứa `db.properties` hay metadata chữ ký dependency và có `db.properties.example`.
**Còn lại:** Secret delivery/rotation, TLS profile, external configuration/launch, backup/restore và release operations chưa được nghiệm thu; cửa sổ Swing chưa được quan sát trực tiếp vì host bị khóa. Migration failure-safety unit coverage, transaction boundary tests và fresh-database migration không thay cho diễn tập nâng cấp/rollback vận hành hoặc nghiệm thu Phase 0.
**Tài liệu bằng chứng:** [`00-hien-trang.md`](00-hien-trang.md), [`PROJECT_COMPLETENESS_REPORT.md`](../../PROJECT_COMPLETENESS_REPORT.md), [`phase0-db-config-review.md`](phase0-db-config-review.md), [`phase0-db-config-resolution-contract.md`](phase0-db-config-resolution-contract.md), [`phase0-domain-events-review.md`](phase0-domain-events-review.md), [`phase0-repository-contract-review.md`](phase0-repository-contract-review.md).
**Gate:** build/migrations/repository transaction có thể tái lập trong môi trường thử nghiệm.

## Phase 1 — Bệnh nhân và tiếp đón

**Trạng thái:** `[~]` Persistence E2E, cross-process reload, transaction/concurrency và headless form-to-SQL acceptance có bằng chứng trên SQL Server synthetic; full `ReceptionPanel` interaction/visual acceptance và các luồng nghiệp vụ còn lại chưa nghiệm thu.
**Phụ thuộc:** Phase 0.
**Phạm vi:** `BenhNhan`, `BaoHiem`, `BenhNhanCoSo`, `TienSuBenh`, lựa chọn `CoSo`/`Khoa`/`PhongKham`, đăng ký/tìm kiếm/queue `LuotKham`.
**Quyết định Owner:** DEC-PAT-01 xác nhận mỗi cặp bệnh nhân–cơ sở chỉ có **một liên kết đang hoạt động**; một bệnh nhân vẫn có thể liên kết với nhiều cơ sở khác nhau. Quy tắc được cưỡng chế bằng filtered unique index V9 `UX_BenhNhanCoSo_Active_Patient_Facility`; migration preflight dừng nếu phát hiện cặp active trùng legacy thay vì tự sửa dữ liệu. Owner tái xác nhận quyết định ngày 06/11/2026 trong [`phase1-patient-facility-link-contract.md`](phase1-patient-facility-link-contract.md). SQL Server synthetic opt-in xác nhận migration V1–V9 và hành vi unique index (từ chối cặp active trùng, cho phép tạo liên kết mới sau soft delete); không phải lượt verify mặc định.
**Bằng chứng cập nhật (06/11/2026):** Root JDK 17 default `mvn -Djava.awt.headless=true clean verify` pass **246 tests, 0 failures, 0 errors, 4 SQL Server opt-in skips** trên bốn module có test; các kiểm tra SQL Server được báo riêng. `FreshDatabaseMigrationSqlServerTest` opt-in pass **1/1** với lệnh `mvn -pl clinic-infrastructure -am -Djava.awt.headless=true -Dclinic.sqlserver.migration.enabled=true -Dclinic.sqlserver.test.database=clinic_test_v9_preflight_20261106 -Dtest=FreshDatabaseMigrationSqlServerTest -Dsurefire.failIfNoSpecifiedTests=false test` trên database SQL Server rỗng disposable mới `clinic_test_v9_preflight_20261106`. Test áp dụng V1–V9, kiểm tra active uniqueness/soft-delete, thêm duplicate legacy tổng hợp trước V9, xác nhận preflight failure giữ nguyên hai row và không tạo tracking/index, rồi soft-delete một row và retry V9 thành công. Không dùng dữ liệu thật; không database hiện hữu nào bị sửa hoặc drop; đây là runtime acceptance opt-in riêng, không phải production migration rehearsal. Trên database tổng hợp `clinic_test_v9_20261106`, `ReceptionSqlServerPersistenceTest` opt-in pass 1/1; test điền `PatientRegistrationFormPanel` trên EDT, tạo command bằng conversion method thật, gọi composition/handler thật, rồi đọc lại các trường bệnh nhân và BHYT, liên kết cơ sở và hàng đợi. Fixture chỉ dùng dữ liệu tổng hợp và soft-delete các dòng do test tạo. Các kiểm tra SQL synthetic trước đó còn bao gồm second-JVM readback, multi-table rollback và queue/CCCD concurrency. Đây là form-to-persistence headless acceptance, không phải kiểm tra trực quan hay toàn bộ `ReceptionPanel`.
**Còn lại:** Cửa sổ Swing chưa được quan sát trực tiếp vì host bị khóa; full `ReceptionPanel` button/worker/dialog và visual review còn mở. Queue lifecycle/transfer chưa hoàn tất. Audit/scope cho patient registration, search và queue có bằng chứng riêng trong Phase 2; không suy rộng thành privacy/audit đầy đủ cho mọi model. `PAT-01` và `PAT-02` vẫn `[~]` theo task board; `PAT-FACILITY-01` và `PAT-V9-PREFLIGHT-01` hoàn tất trong phạm vi test/runtime acceptance của chúng. Phase 1 vẫn `[~]` và exit criteria chưa đạt. `BenhNhanCoSo` không phải quyền truy cập. Medical-history workflow `TienSuBenh` chưa có repository/use case/UI guarded-audited; DEC-AUTHZ-11 chỉ chốt DOCTOR được tạo/sửa ba loại dị ứng, bệnh nền và phẫu thuật, nhưng quyền đọc, ownership/scope của từng dòng và encounter binding/anchor, hành vi sau khóa, cùng policy actor cho tiền sử gia đình/thói quen sinh hoạt vẫn chưa được Owner quyết định. Giữ history workflow default-deny; không suy ra quyền từ `PATIENT:SUA`, liên kết cơ sở hoặc V9.
**Bằng chứng bổ sung:** [`phase1-pat02-v9-review.md`](phase1-pat02-v9-review.md), [`phase1-tiensuben-review.md`](phase1-tiensuben-review.md), [`phase3-visit-lifecycle-review.md`](phase3-visit-lifecycle-review.md). Phase 1 exit criteria vẫn chưa đạt.

## Phase 2 — Baseline identity, access checks và audit

**Trạng thái:** `[~]` Identity/session, default-deny evaluator, approved grants và một số protected reception paths đã được triển khai, nhưng `SEC-01` vẫn in progress và gate chưa đạt.
**Phụ thuộc:** Phase 0; phải hoàn tất trước khi mở các workflow nhạy cảm.
**Phạm vi tối thiểu bắt buộc:** định danh actor/session tin cậy; role và data scope lấy từ tài khoản; kiểm tra quyền tại application/use case và persistence boundary phù hợp; audit truy cập/thay đổi nhạy cảm; kiểm thử allow/deny và truy vết.
**Gate bắt buộc:** Phase 2 là prerequisite trực tiếp của Phase 4 (EMR), Phase 6 (prescription/inventory/pharmacy) và Phase 7 (billing). UI ẩn/disable không thay thế authorization ở backend. Không bật các luồng nhạy cảm với dữ liệu thật khi gate chưa đạt.

**Bằng chứng triển khai hiện có (06/11/2026):**
- Xác thực thông tin đăng nhập đã băm; sau đăng nhập, người dùng chọn một role và một cặp cơ sở/khoa đã gán trước khi tạo session. Session lifecycle thực thi idle lock 15 phút, absolute expiry 12 giờ, logout/re-auth và revalidation assignment theo policy đã duyệt.
- `AuthorizationService` kiểm tra active session, employee, role/scope assignment, grant duy nhất và target pair; thiếu/sai dữ liệu, scope rộng legacy, lỗi dependency và `AUDIT_READ` đều default-deny. V8 seed 57 approved ALLOW tuples theo `KHOA_PHONG`; không suy rộng quyền ngoài allowlist.
- Đăng ký/cập nhật reception bảo vệ patient, insurance và visit writes ở Application boundary, ràng buộc exact facility+department, audit mutation trong transaction và audit denial độc lập. Patient search, queue và room reads được scope theo cặp phiên; successful-read audit commit độc lập trước khi trả dữ liệu, lỗi audit thì fail closed. SQL Server synthetic opt-in `clinic_test_audit_20261105` pass 1/1 cho grant lookup, protected registration/rollback và protected search/queue audit; đây là evidence riêng, không thuộc default verify.
- `SEC-UI-01` permission-aware navigation có headless validation; desktop visual smoke vẫn chưa thực hiện. Bằng chứng và phân rã task hiện hành nằm trong [`03-model-task-board.md`](03-model-task-board.md), [`phase2-implementation-contract.md`](phase2-implementation-contract.md), [`phase2-role-permission-matrix-approval.md`](phase2-role-permission-matrix-approval.md) và các contract/review Phase 2.

**Đối chiếu task board:** `SEC-AUDIT-01D` hiện `[x]` sau khi root chạy lại đúng opt-in `AuditSqlServerPersistenceTest` và xác nhận 1/1 pass trên `clinic_test_audit_20261105`; phạm vi chỉ gồm migration/audit append-only và rollback registration khi audit insert bị từ chối. Điều này không đóng toàn bộ `SEC-01` hay cấp quyền đọc audit. Các số test trong từng task row là snapshot tại thời điểm task; baseline root hiện hành là 246/0/0/4 như Evidence snapshot của board.

**Còn lại theo `SEC-01` trên task board:** visual smoke; các workflow ngoài reception hiện được bảo vệ/kiểm chứng chưa đồng đều; onboarding/assignment administration; triển khai DEC-AUTHZ-04b transfer workflow và DEC-AUTHZ-05 billing exception workflow theo policy đã chốt; các DEC-AUTHZ-06/07/08 policy đã chốt nhưng vẫn deny tới khi guard/grant/workflow và test đạt. Thu cọc lịch hẹn chưa có actor/flow được duyệt và vẫn deny. Ngưỡng/thời gian khóa đăng nhập đang chờ Owner chọn phương án tại [`phase2-login-lockout-policy-proposal.md`](phase2-login-lockout-policy-proposal.md); retention duration còn mở trong khi policy đã duyệt vẫn là không purge tự động. Theo DEC-AUTHZ-10, `AUDIT_READ` đã được quyết định là DENY cho mọi role; không có quyền tra cứu audit được cấp. `EMR-01`, `RX-01`, `INV-01` và `BILL-01` chưa mở. Các thao tác ngoài protected reception slice không được coi là đã được bật an toàn.

Các policy đã duyệt vẫn giữ nguyên: đúng một role và một cặp scope mỗi phiên; thông thường phải khớp cả facility lẫn department; `KHOA_PHONG` là scope được phép; DEC-AUTHZ-01 DOCTOR-only ghi sinh hiệu; DEC-AUTHZ-02 mở khóa EMR cần DOCTOR thứ hai cùng khoa; DEC-AUTHZ-04a/04b hủy và điều chuyển theo guard/role đã chốt; DEC-AUTHZ-05 CASHIER thu theo giá, ngoại lệ cần CASHIER khác duyệt. Chốt policy không đồng nghĩa workflow/grant đã triển khai. `SEC-01` không hoàn tất cho đến khi các gate, kiểm thử và UI acceptance còn mở được nghiệm thu.

## Phase 3 — Lịch hẹn và vòng đời lượt khám

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 1 và Phase 2.
**Phạm vi:** `LichHen`, `NguonLich`, `LuotKham`, `SinhHieu`; đặt/đổi/hủy, check-in/no-show, queue lifecycle, vitals.
**Còn lại:** chống tạo lượt khám trùng; ghi người/thời điểm đo; chuyển trạng thái hợp lệ; authorization/audit riêng cho cancel, transfer và vitals; persist/reload/concurrency tests. Reception registration, queue reads, room reads và audit đã được nối cho slice hiện có, nhưng không cung cấp các lifecycle use cases này.
**Review hoàn tất:** [`phase3-visit-lifecycle-review.md`](phase3-visit-lifecycle-review.md), task `VIS-REVIEW-01` (`fbe75a15-9ad0-4956-aa29-ea101c1fd714`, Codex, commit `8358d67`). Policy hủy/chuyển/sinh hiệu đã duyệt nhưng chưa có workflow; implementation cần bổ sung guard, persistence, audit và tests. Các luồng Phase 3 vẫn đóng tới khi security gate và các dependency nghiệp vụ được hoàn tất.

## Phase 4 — Clinical EMR

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 2 security gate và Phase 3.
**Phạm vi:** `ChanDoan`, `ICD`, `TienSuBenh`, phần lâm sàng của `LuotKham`, hoàn tất/khóa/mở EMR, `NhatKyHoSoBenhAn`.
**Còn lại:** lưu nháp/đọc lại, workflow ký/khóa, kiểm soát quyền theo actor/scope, audit và kiểm thử khóa/mở; tài liệu chỉ lấy từ dữ liệu đã persist.

## Phase 5 — Catalogs, services và CLS

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 3; các thao tác truy cập hồ sơ/kết quả phải dùng Phase 2.
**Phạm vi:** `ICD`, `DanhMucDichVu`, `ChiDinhDichVu`, `ChiTietKetQuaXetNghiem`, `DinhMucVatTuDichVu`; catalog và quy trình kết quả.
**Còn lại:** CRUD/hiệu lực danh mục, tạo/hủy chỉ định, nhập/duyệt kết quả, audit/version, quyết định lưu metadata hình ảnh.

## Phase 6 — Prescription, inventory và pharmacy

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 2 security gate, Phase 4; nguồn dịch vụ Phase 5 khi có tiêu hao.
**Phạm vi:** `DonThuoc`, `ChiTietDonThuoc`, `Item`, `Kho`, `TonKho`, nhập/xuất, supplier, FEFO, kiểm kê và transaction kho.
**Còn lại:** kiểm tra an toàn thuốc theo nghiệp vụ; duyệt/cấp phát; FEFO; chứng từ/audit; atomicity, concurrency và không âm kho.

## Phase 7 — Billing

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 2 security gate, Phase 3; nguồn thu từ Phase 5 và Phase 6.
**Phạm vi:** `HoaDon`, `ChiTietHoaDon`, `PhieuThu`, `CongNoBenhNhan`; lập hóa đơn, thu/hoàn/điều chỉnh, đối soát.
**Còn lại:** xác nhận quy tắc nghiệp vụ giá/BHYT/đồng chi trả; snapshot giá; giao dịch thu nhiều lần; chứng từ/audit; đối soát và retry/idempotency tests.

## Phase 8 — HR/payroll và extended administration

**Trạng thái:** `[ ]` CRUD/admin/payroll chưa có bằng chứng nghiệm thu hoàn chỉnh.
**Phụ thuộc:** Phase 2 cho quyền quản trị; Phase 7 và dữ liệu nhân sự/chấm công cho payroll.
**Phạm vi:** `NhanVien`, `VaiTro`, `ChucVu`, lịch ca/chấm công/nghỉ phép, payroll, cơ sở/khoa/phòng mở rộng, phân quyền quản trị, cấu hình và audit administration.
**Còn lại:** user/HR CRUD, role/scope administration, attendance/leave, payroll rules/approval/close, cấu hình, backup/restore. Phase này mở rộng quản trị; không trì hoãn security gate tối thiểu ở Phase 2.

## Phase 9 — Reports, documents, integrations và release hardening

**Trạng thái:** `[ ]` Chưa có bằng chứng phát hành được nghiệm thu.
**Phụ thuộc:** các phase cung cấp dữ liệu; Phase 2 cho mọi truy vấn/tài liệu nhạy cảm; Phase 8 cho cấu hình quản trị.
**Phạm vi:** báo cáo, DOCX/print/export, notification/payment/external integrations, hardening, CI/release/operations.
**Còn lại:** định nghĩa chỉ số và đối soát; kiểm tra mẫu tài liệu; quản lý secrets; contract/integration tests; build/release và hướng dẫn vận hành.
**CI:** `REL-CI-01` đã thêm `.github/workflows/maven-verify.yml`: push/PR sẽ chạy headless Maven verify trên JDK 17 bằng action major hiện hành; quyền `contents: read`, không secrets/SQL Server opt-in. YAML và Maven đã kiểm tra local. GitHub-hosted run chưa có vì branch chưa push; task được đánh dấu complete-local, không phải CI run đã nghiệm thu.

## Quy tắc chuyển phase

- Chỉ chuyển sang trạng thái hoàn tất khi workflow có bằng chứng theo tiêu chí phase, gồm quyền/audit khi dữ liệu nhạy cảm.
- Phase 2 là gate trước mọi thao tác EMR, prescription/pharmacy và billing; Phase 8 chỉ chứa quản trị nhân sự và mở rộng.
- Task-level status và bằng chứng theo từng model được theo dõi tại [03 — Model task board](03-model-task-board.md).
- Test task có thể hoàn tất khi đúng phạm vi test đã có kết quả xác nhận; việc đó không tự hoàn tất phase hoặc workflow.
