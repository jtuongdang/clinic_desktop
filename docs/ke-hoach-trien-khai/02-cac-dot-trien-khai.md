# 02 — Lộ trình triển khai theo phase

Lộ trình có **đúng 10 phase, đánh số 0–9**. Thứ tự dưới đây là contract cho roadmap; việc model hoặc UI đã có không đồng nghĩa phase đã nghiệm thu.

## Phase 0 — Nền tảng

**Trạng thái:** `[~]` Nền tảng và wiring cơ bản có trong source; validation/release chưa hoàn tất.
**Phụ thuộc:** Không.
**Phạm vi:** Maven modules, application shell, schema/migrations, JDBC, transaction/unit of work và cấu hình môi trường.
**Bằng chứng mới:** `JdbcTransactionManagerTest` kiểm tra commit/rollback/cleanup; `DatabaseMigrationRunnerTest` và `SqlServerBatchSplitterTest` kiểm tra tách batch `GO`; DateRange/Money regression tests có ở `3f6ba31`. Tại code snapshot `9ff7ddd`, inventory là 13 test source/64 tests (domain 3/16, application 5/26, infrastructure 4/17, desktop 1/5). `mvn clean verify` pass: 64 tests, 0 failures/errors/skips, năm Maven module thành công.
**Còn lại:** Xác nhận migrations trên SQL Server thử nghiệm sạch; cấu hình secret. Các kiểm thử transaction/splitter hiện có không chứng minh migration hay reception E2E trên SQL Server. Identity/access baseline nằm ở Phase 2.
**Gate:** build/migrations/repository transaction có thể tái lập trong môi trường thử nghiệm.

## Phase 1 — Bệnh nhân và tiếp đón

**Trạng thái:** `[~]` Một phần workflow đã nối; regression tests có trong branch; chưa nghiệm thu E2E.
**Phụ thuộc:** Phase 0.
**Phạm vi:** `BenhNhan`, `BaoHiem`, `BenhNhanCoSo`, `TienSuBenh`, lựa chọn `CoSo`/`Khoa`/`PhongKham`, đăng ký/tìm kiếm/queue `LuotKham`.
**Bằng chứng hiện tại tại code snapshot `9ff7ddd`:** Có 13 test source/64 test cases: domain 3/16, application 5/26, infrastructure 4/17, desktop 1/5. Commits liên quan gồm `ff2045a` transaction tests, `264bbae` migration runner/GO splitter tests, `ea397f9` headless reception form tests, `3f6ba31` DateRange/Money tests và `9ff7ddd` authentication tests. Application/JDBC/UI wiring hiện diện. Đây là test/review evidence, chưa phải Phase 1 acceptance.
**Còn lại:** SQL Server save/search/queue và reload sau restart; cập nhật hồ sơ/BHYT; rollback đa bảng của luồng reception; queue/CCCD concurrency; UI integration/E2E và review. Không có bằng chứng SQL Server E2E, restart hoặc concurrency; Phase 1 exit criteria vẫn unmet.

## Phase 2 — Baseline identity, access checks và audit

**Trạng thái:** `[ ]` Đăng nhập nền tảng có trong source; baseline kiểm soát truy cập/audit chưa được xác minh.
**Phụ thuộc:** Phase 0; phải hoàn tất trước khi mở các workflow nhạy cảm.
**Phạm vi tối thiểu bắt buộc:** định danh actor/session tin cậy; role và data scope lấy từ tài khoản; kiểm tra quyền tại application/use case và persistence boundary phù hợp; audit truy cập/thay đổi nhạy cảm; kiểm thử allow/deny và truy vết.
**Gate bắt buộc:** Phase 2 là prerequisite trực tiếp của Phase 4 (EMR), Phase 6 (prescription/inventory/pharmacy) và Phase 7 (billing). UI ẩn/disable không thay thế authorization ở backend. Không bật các luồng nhạy cảm với dữ liệu thật khi gate chưa đạt.
**Còn lại:** xác minh password/session policy, role/scope, kiểm tra quyền tập trung, audit đăng nhập/xem/sửa EMR/cấp phát/thu tiền và test quyền/audit.

**Bằng chứng auth unit:** `AuthenticateUserHandlerTest` và `Pbkdf2PasswordHasherTest` được thêm tại commit `9ff7ddd`; các test này kiểm tra handler/verifier và không chứng minh persistence, access scope hoặc audit. SEC-01 vẫn chưa được chấp nhận.

## Phase 3 — Lịch hẹn và vòng đời lượt khám

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 1 và Phase 2.
**Phạm vi:** `LichHen`, `NguonLich`, `LuotKham`, `SinhHieu`; đặt/đổi/hủy, check-in/no-show, queue lifecycle, vitals.
**Còn lại:** chống tạo lượt khám trùng; ghi người/thời điểm đo; chuyển trạng thái hợp lệ; quyền theo phạm vi và audit; persist/reload/concurrency tests.

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

## Quy tắc chuyển phase

- Chỉ chuyển sang trạng thái hoàn tất khi workflow có bằng chứng theo tiêu chí phase, gồm quyền/audit khi dữ liệu nhạy cảm.
- Phase 2 là gate trước mọi thao tác EMR, prescription/pharmacy và billing; Phase 8 chỉ chứa quản trị nhân sự và mở rộng.
- Task-level status và bằng chứng theo từng model được theo dõi tại [03 — Model task board](03-model-task-board.md).
- Test task có thể hoàn tất khi đúng phạm vi test đã có kết quả xác nhận; việc đó không tự hoàn tất phase hoặc workflow.
