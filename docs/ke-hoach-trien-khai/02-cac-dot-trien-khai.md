# 02 — Lộ trình triển khai theo phase

Lộ trình có **đúng 10 phase, đánh số 0–9**. Thứ tự dưới đây là contract cho roadmap; việc model hoặc UI đã có không đồng nghĩa phase đã nghiệm thu.

## Phase 0 — Nền tảng

**Trạng thái:** `[~]` Nền tảng và wiring cơ bản có trong source; validation/release chưa hoàn tất.
**Phụ thuộc:** Không.
**Phạm vi:** Maven modules, application shell, schema/migrations, JDBC, transaction/unit of work và cấu hình môi trường.
**Bằng chứng cập nhật (06/11/2026):** Root JDK 17 `mvn -Djava.awt.headless=true clean verify` pass 209 tests, 0 failures/errors, 4 SQL Server opt-in skips, cả năm module. Migration failure-safety có unit tests cho batch/tracking rollback, không commit khi lỗi và bảo toàn exception gốc; fresh SQL Server synthetic test chạy V1–V9 pass 1/1 trên DB `clinic_test_v9_20261106`. Shaded JAR trước đây đã xác nhận không chứa metadata chữ ký dependency hoặc `db.properties` local. Login/UI hiện đọc secrets từ local config/environment; deployment secret lifecycle chưa được nghiệm thu.
**Còn lại:** Cấu hình/release vận hành (secret delivery/rotation, TLS profile, backup/restore) và quan sát cửa sổ Swing chưa hoàn tất; Mac hiện khóa nên visual smoke bị chặn. Migration failure-safety unit coverage đã có, nhưng không thay cho diễn tập upgrade/rollback operational. Transaction boundary test và fresh DB test không đồng nghĩa đã chấp nhận toàn Phase 0. Identity/access baseline nằm ở Phase 2.
**Gate:** build/migrations/repository transaction có thể tái lập trong môi trường thử nghiệm.

## Phase 1 — Bệnh nhân và tiếp đón

**Trạng thái:** `[~]` Persistence E2E, cross-process reload và transaction/concurrency đã có bằng chứng trên SQL Server synthetic; full Swing UI, security/privacy/audit và nghiệp vụ chưa nghiệm thu.
**Phụ thuộc:** Phase 0.
**Phạm vi:** `BenhNhan`, `BaoHiem`, `BenhNhanCoSo`, `TienSuBenh`, lựa chọn `CoSo`/`Khoa`/`PhongKham`, đăng ký/tìm kiếm/queue `LuotKham`.
**Bằng chứng hiện tại tại `0d91903`:** 18 test source/78 test cases được phát hiện (domain 4/21, application 5/26, infrastructure 5/21, desktop 4/10); default verify có 3 SQL Server opt-in tests skipped, trong đó 2 thuộc Tiếp đón. Registration/update E2E và second-JVM readback probe pass trên SQL Server synthetic. Manual SQL checks xác nhận multi-table rollback, queue numbering dưới 8 đăng ký đồng thời và ánh xạ duplicate CCCD trong tranh chấp. Tất cả test patient data được soft-delete. Safe UI registration error mapping và staff role/scope assignment reads có unit tests. Đây là evidence task, chưa phải Phase 1 acceptance.
**Cập nhật bằng chứng (06/11/2026):** JDK 17 root `mvn -Djava.awt.headless=true clean verify` pass 209 tests, 0 failures/errors, 4 SQL Server opt-in skips. Trên DB SQL Server synthetic riêng `clinic_test_v9_20261106`, migration V1–V9 chạy pass 1/1, xác nhận V9 chặn duplicate active patient/facility và cho tạo lại sau soft delete. Reception composition E2E pass 1/1 với actor tổng hợp có role `RECEPTIONIST` và scope đúng; xác nhận đăng ký/tìm/cập nhật/queue, metadata của liên kết cơ sở và tính idempotent. E2E tìm và sửa lỗi SQL Server trong truy vấn assigned scopes (`SELECT DISTINCT` + `ORDER BY` cột chưa chọn). Dữ liệu test được soft-delete theo ID. Không chạm `clinicDb`.
**Còn lại:** full Swing UI-to-DB workflow/visual review chưa qua; Mac host đang khóa nên cửa sổ đăng nhập chưa thể quan sát. Quy tắc privacy/audit và tiền sử lâm sàng còn cần hoàn tất theo policy đã duyệt. Phase 1 exit criteria vẫn unmet.
**Review tiền sử hoàn tất:** `PAT-HISTORY-REVIEW-01` (`99f7197a-82a7-488d-97b1-10a660511d63`, Copilot retry sau quota 429 của Antigravity), report [`phase1-tiensuben-review.md`](phase1-tiensuben-review.md), commit `3a84a62`. Còn ba quyết định Owner về quyền đọc, gắn scope/encounter và sửa sau khi khóa; tới khi chốt, history workflow giữ default-deny.

## Phase 2 — Baseline identity, access checks và audit

**Trạng thái:** `[ ]` Đăng nhập nền tảng có trong source; baseline kiểm soát truy cập/audit chưa được xác minh.
**Phụ thuộc:** Phase 0; phải hoàn tất trước khi mở các workflow nhạy cảm.
**Phạm vi tối thiểu bắt buộc:** định danh actor/session tin cậy; role và data scope lấy từ tài khoản; kiểm tra quyền tại application/use case và persistence boundary phù hợp; audit truy cập/thay đổi nhạy cảm; kiểm thử allow/deny và truy vết.
**Gate bắt buộc:** Phase 2 là prerequisite trực tiếp của Phase 4 (EMR), Phase 6 (prescription/inventory/pharmacy) và Phase 7 (billing). UI ẩn/disable không thay thế authorization ở backend. Không bật các luồng nhạy cảm với dữ liệu thật khi gate chưa đạt.
**Còn lại:** xác minh password/session policy, role/scope, kiểm tra quyền tập trung, audit đăng nhập/xem/sửa EMR/cấp phát/thu tiền và test quyền/audit.

**Bằng chứng auth unit:** `AuthenticateUserHandlerTest` và `Pbkdf2PasswordHasherTest` được thêm tại commit `9ff7ddd`; các test này kiểm tra handler/verifier và không chứng minh persistence, access scope hoặc audit. SEC-01 vẫn chưa được chấp nhận.
**Rà soát kiến trúc:** Ba task độc lập đã qua review, build/scope validation và tích hợp local: [`phase2-auth-persistence-review.md`](phase2-auth-persistence-review.md) (Codex, `1d56186`), [`phase2-desktop-session-review.md`](phase2-desktop-session-review.md) (Copilot, `fcf5cf9`), và [`phase2-domain-audit-review.md`](phase2-domain-audit-review.md) (Antigravity, `367dd8f`). Chủ dự án đã xác nhận một role cho mỗi phiên, default-deny và scope theo cơ sở/khoa được gán. Đề xuất timeout/audit còn chờ duyệt tại [`phase2-security-policy-proposal.md`](phase2-security-policy-proposal.md); chưa có authorization/security code được triển khai.

## Phase 3 — Lịch hẹn và vòng đời lượt khám

**Trạng thái:** `[ ]` Chưa có bằng chứng nghiệm thu end-to-end.
**Phụ thuộc:** Phase 1 và Phase 2.
**Phạm vi:** `LichHen`, `NguonLich`, `LuotKham`, `SinhHieu`; đặt/đổi/hủy, check-in/no-show, queue lifecycle, vitals.
**Còn lại:** chống tạo lượt khám trùng; ghi người/thời điểm đo; chuyển trạng thái hợp lệ; quyền theo phạm vi và audit; persist/reload/concurrency tests.
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

## Quy tắc chuyển phase

- Chỉ chuyển sang trạng thái hoàn tất khi workflow có bằng chứng theo tiêu chí phase, gồm quyền/audit khi dữ liệu nhạy cảm.
- Phase 2 là gate trước mọi thao tác EMR, prescription/pharmacy và billing; Phase 8 chỉ chứa quản trị nhân sự và mở rộng.
- Task-level status và bằng chứng theo từng model được theo dõi tại [03 — Model task board](03-model-task-board.md).
- Test task có thể hoàn tất khi đúng phạm vi test đã có kết quả xác nhận; việc đó không tự hoàn tất phase hoặc workflow.
