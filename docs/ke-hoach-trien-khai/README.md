# Kế hoạch phân tích và triển khai theo phân hệ

> Tài liệu làm việc để theo dõi việc đưa Clinic Management Desktop từ prototype UI/Domain sang các luồng nghiệp vụ có thể dùng dữ liệu thật. Nội dung đối chiếu mã nguồn hiện có trong `clinic-domain`, `clinic-desktop`, `clinic-application`, `clinic-infrastructure` và migration SQL.

## Cách sử dụng

1. Bắt đầu từ [Hiện trạng và nguyên tắc phân tích](00-hien-trang.md).
2. Dùng [Ma trận Domain–màn hình và phần cần bổ sung](01-ma-tran-model-man-hinh.md) để chọn phạm vi.
3. Quản lý thứ tự, phụ thuộc và điều kiện nghiệm thu trong [Các đợt triển khai](02-cac-dot-trien-khai.md).
4. Theo dõi model/task, bằng chứng và gate trong [03 — Model task board](03-model-task-board.md); các review Phase 0 được liên kết ở đây.
5. Mỗi phân hệ có một trang backlog riêng trong `phan-he/`; cập nhật trạng thái và người phụ trách tại đó khi bắt đầu làm.

## Quy ước trạng thái

- `[ ] Chưa làm` — chưa có triển khai tương ứng.
- `[~] Đang làm` — đang phát triển; cần nêu người phụ trách và nhánh/PR khi có.
- `[x] Hoàn tất` — code, kiểm thử và nghiệm thu đều đạt tiêu chí trong trang phân hệ.
- `[!] Chặn` — phụ thuộc hoặc quyết định nghiệp vụ chưa được giải quyết; ghi rõ nguyên nhân.

Chỉ đánh dấu hoàn tất khi có luồng sử dụng được từ màn hình đến lưu trữ và đọc lại dữ liệu, kèm kiểm thử phù hợp. Có panel/mockup hoặc entity không đồng nghĩa với phân hệ đã hoàn thành.

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [00 — Hiện trạng](00-hien-trang.md) | Kiến trúc thực tế, mức hoàn thiện, nguyên tắc phân tích và giới hạn. |
| [01 — Ma trận model–màn hình](01-ma-tran-model-man-hinh.md) | Domain models, UI hiện có, màn hình cần bổ sung, mức ưu tiên. |
| [02 — Các đợt triển khai](02-cac-dot-trien-khai.md) | Lộ trình theo dependency, deliverable và điều kiện qua đợt. |
| [03 — Model task board](03-model-task-board.md) | Trạng thái task theo model family, dependency, bằng chứng và blocker. |
| [Operations and release runbook](../OPERATIONS_RUNBOOK.md) | Hướng dẫn build/test, cấu hình, migration, launch và backup/restore rehearsal cho local hoặc controlled test deployment; ghi rõ gate chưa đạt. |
| [Phase 0 — Review domain events](phase0-domain-events-review.md) | Vòng đời sự kiện miền, transaction timing, persistence, UI và test gaps. |
| [Phase 0 — Review repository contracts](phase0-repository-contract-review.md) | Map model repository từ domain port tới adapter, use case, transaction và test. |
| [Phase 0 — Value Object owner decisions](phase0-value-object-owner-decisions.md) | Các lựa chọn cần Owner chốt trước khi nối `DateRange`, `PhoneNumber` và `Money` vào form/handler/persistence. |
| [Phase 3 — Appointment owner decisions](phase3-appointment-owner-decisions.md) | Các quyết định booking, capacity, status/check-in, notification và deposit trước APPT-01; deposit tiếp tục DENY. |
| [Phase 3 — Visit lifecycle owner decisions](phase3-visit-lifecycle-owner-decisions.md) | Làm rõ operational contract cho hủy, điều chuyển và sinh hiệu; giữ nguyên role policies đã duyệt. |
| [Phase 0 — Review error mapping](phase0-error-mapping-review.md) | Map lỗi từ domain/Application/JDBC tới thông báo Swing và rủi ro dữ liệu. |
| [Phase 2 — Security implementation contract](phase2-implementation-contract.md) | Ranh giới tin cậy, role/scope/session/audit policy đã duyệt và thứ tự task implementation. |
| [Phase 2 — Security gate closure review](phase2-security-exit-gate-review.md) | Report-only đối chiếu SEC-01, bằng chứng hiện có, dependency vòng giữa baseline và workflow về sau; đề xuất hai định nghĩa gate và sequence nghiệm thu không cấp quyền. |
| [Phase 2 — SEC-01 baseline acceptance](phase2-sec01-baseline-acceptance.md) | Checklist baseline theo Definition A đã duyệt; phân biệt acceptance đạt với visual smoke còn mở và các workflow tương lai tiếp tục default-deny. |
| [Phase 2 — Login lockout policy](phase2-login-lockout-policy-proposal.md) | Hiện trạng và các phương án giới hạn đăng nhập sai cần Owner duyệt trước khi migration/use case. |
| [Phase 1 — Review migration V9](phase1-pat02-v9-review.md) | Review độc lập về unique active patient/facility link, migration safety và runtime test gaps. |
| [Phase 1 — Patient–facility link contract](phase1-patient-facility-link-contract.md) | Hợp đồng triển khai idempotent link trong transaction tiếp nhận, không mở rộng quyền truy cập. |
| [Phase 1 — Review tiền sử lâm sàng](phase1-tiensuben-review.md) | Đối chiếu policy bác sĩ ghi/sửa với scope, lượt khám, audit, khóa EMR và các quyết định Owner còn thiếu. |
| [Phase 3 — Review vòng đời lượt khám](phase3-visit-lifecycle-review.md) | Map policy hủy/chuyển/sinh hiệu tới state machine, authorization, persistence, audit và màn hình hiện có. |
| [Phase 3 — Visit transfer contract review](phase3-visit-transfer-contract-review.md) | Report-only đối chiếu DEC-AUTHZ-04b với transfer entry point, state, queue, authorization, audit, persistence, các owner decisions còn mở và acceptance gates; giữ SEC-01 làm hard gate. |
| [Phase 3 — Appointment readiness review](phase3-appointment-readiness-review.md) | Report-only review về domain, UI, persistence, concurrency, scope, lifecycle, time, notification/payment, audit và các quyết định APPT còn mở; `SEC-01` vẫn là gate. |
| [Phase 0 — Review cấu hình DB và secret](phase0-db-config-review.md) | Đối chiếu precedence environment/config, TLS, logging và cách chạy JAR đóng gói; không truy cập DB hay secret. |
| [Phase 0 — Contract testable DB configuration](phase0-db-config-resolution-contract.md) | Ghi nhận precedence hiện tại, call-order edge cases và test matrix cấu hình tổng hợp; chưa chọn TLS hay external-JAR policy. |
| [03 — Tiếp đón, bệnh nhân, nhân sự](phan-he/03-tiep-don-benh-nhan-nhan-su.md) | Patient, insurance, identity, staff, shift/attendance. |
| [04 — Lịch hẹn và khám/EMR](phan-he/04-lich-hen-kham-emr.md) | Appointment, visit, diagnosis, vitals, prescriptions, care plan. |
| [05 — Dịch vụ và cận lâm sàng](phan-he/05-dich-vu-can-lam-sang.md) | Catalog services, orders, lab/imaging result workflow. |
| [06 — Kho và nhà thuốc](phan-he/06-kho-nha-thuoc.md) | Item, supplier, procurement, batches, FEFO, dispensing, stocktake. |
| [07 — Viện phí và payroll](phan-he/07-vien-phi-payroll.md) | Invoice, receipts, patient debt, payment and payroll. |
| [08 — Cơ sở, bảo mật và cấu hình](phan-he/08-co-so-bao-mat-cau-hinh.md) | Facility, user/role/permissions, audit, configuration and integration. |
| [09 — Danh mục, báo cáo và tài liệu](phan-he/09-danh-muc-bao-cao-tai-lieu.md) | Catalog CRUD, reporting and print/export integrations. |

## Ưu tiên tổng thể

1. Review, commit và kiểm thử vertical slice Tiếp đón đang có trong checkout; xác nhận transaction, persistence, queue và lỗi dữ liệu trùng.
2. Xây lịch hẹn, vòng đời lượt khám, sinh hiệu và luồng EMR có lưu trữ thật.
3. Hoàn thiện chỉ định/kết quả CLS, kho/cấp phát và viện phí theo phụ thuộc nghiệp vụ.
4. Bổ sung quyền tập trung, audit, quản trị, báo cáo và hardening cấu hình trước khi cân nhắc dữ liệu thật.

Tài liệu task pilot cho AI coding agents: [AI_AGENT_PILOT_TASKS.md](../AI_AGENT_PILOT_TASKS.md).

