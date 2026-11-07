# Phase 3 — Quyết định Owner cho vòng đời lượt khám

**Task:** `VIS-LIFECYCLE-DECISIONS-01`
**Trạng thái:** Quyết định điều chuyển hiện hành được đối soát ngày 06/11/2026 theo `VIS-TRANSFER-OWNER-DECISION-RECON-06`, giữ nguyên RECON-04/05 ngoài N2 đã được bổ sung; workflow chưa mở.
**Gate:** Không cấp quyền hoặc thay đổi gate từ tài liệu này. Luồng điều chuyển vẫn default-deny và phụ thuộc SEC-01 cùng acceptance implementation riêng.

## RECON-06 addendum — 2026-11-06 (current approved N2 policy)

**Authority:** Owner-confirmed `VIS-TRANSFER-OWNER-DECISION-RECON-06`, contract id `13d19fa9-eaed-404e-bdfc-4070daffd38a`. This note-policy supplement carries RECON-04 and RECON-05 forward unchanged; it does not reopen actor, role, status, scope, queue, linked-work, history/audit or replay decisions.

### N2 — ghi chú điều chuyển tùy chọn đã được Owner phê duyệt

- Ghi chú tùy chọn với mọi mã lý do đã duyệt. Giá trị thiếu/null vẫn là absent; không thêm mặc định và không ghép nhãn lý do nghiệp vụ vào ghi chú.
- Tại Application boundary trong tương lai, đổi TAB (U+0009), LF (U+000A), CR (U+000D), SPACE (U+0020), NBSP (U+00A0), NARROW NBSP (U+202F) thành ordinary space; gộp các chuỗi space và trim. Kết quả trống là absent; từ chối separator/whitespace khác.
- Chuẩn hóa nội dung không rỗng sang Unicode NFC. Cho phép Unicode letters (L), combining marks (M), decimal digits (Nd), punctuation (P), symbols (S) và ordinary space. Từ chối control khác danh sách mapping, format (bao gồm bidi và ZWJ), unpaired surrogate, private-use và unassigned. Implementation contract phải ghim Unicode category/version behavior.
- Tối đa 255 Unicode code points sau normalization; vượt giới hạn thì reject, không truncate. Bộ đếm UI dùng cùng đơn vị/giai đoạn; persistence phải round-trip nội dung hợp lệ không mất dữ liệu.
- Nhãn chính xác: **“Ghi chú vận hành (không bắt buộc)”**. Cảnh báo: **“Không nhập thông tin định danh, liên hệ, chẩn đoán hoặc kết quả khám/xét nghiệm.”** Không prefill từ dữ liệu bệnh nhân/lâm sàng. Hiển thị nội dung đã normalize trước khi submit; nội dung bị reject không được xuất hiện trong logs.
- Chính sách giảm thiểu rủi ro PHI, không bảo đảm ẩn danh và không thể bảo đảm người dùng không nhập PHI.

Mã lý do nghiệp vụ cố định vẫn tách biệt khỏi ghi chú, `AuditReasonCode`/auth audit reason (`NhatKyHeThong.maLyDo`), `lyDoKham` và `lyDoChuyenTuyen`. Physical field/table, storage placement và retention vẫn **OPEN**; approval N2 không phải DDL/schema authorization. Các quyết định triển khai queue, idempotency, linked-work và acceptance vẫn mở theo đúng phạm vi; không quyết định này cấp quyền hoặc mở gate. SEC-01 remains **OPEN**; transfer remains **default-deny**.

### Các quyết định triển khai còn mở — không suy diễn

1. **Lưu trữ ghi chú:** logical/physical field, table placement, access, capacity/encoding/round-trip implementation và retention (purpose, duration, hold/disposal, copies/backups). Unicode category/version behavior phải được ghim trong implementation contract. Không chọn schema hoặc retention.
2. **Queue persistence/cutover:** P1 + M3 là design basis; schema/DDL, indexes, locking, high-water reconciliation, migration/cutover và legacy semantics chưa được duyệt.
3. **Idempotency operation details:** key/retention durations, exact key format/scope, canonical command fields/version and note participation, receipt, wait/indeterminate/commit-resolution details and reservation/retry handling. N2 normalization does not settle the broader canonical command contract.
4. **Linked-work and workflow acceptance:** evidence sources, safety/handoff outcomes and separate security/workflow acceptance remain open.

## Historical RECON-05 addendum — 2026-11-06 (superseded for N2)

**Historical authority:** Owner-confirmed `VIS-TRANSFER-OWNER-DECISION-RECON-05` as understood before RECON-06. Its statement that note policy remained open is superseded by the RECON-06 addendum above; RECON-04/05 rules outside N2 remain unchanged.

### Approved business reasons and unchanged authorization boundaries

A transfer requires exactly one of these six business reason codes; the short note remains optional:

| Business reason code | Permitted actor roles |
|---|---|
| `RECEPTION_TRIAGE_CORRECTION` | `RECEPTIONIST` |
| `PATIENT_SPECIALTY_PREFERENCE` | `RECEPTIONIST`, `DOCTOR` |
| `CLINIC_LOAD_BALANCING` | `RECEPTIONIST`, `DOCTOR` |
| `CLINICAL_SPECIALTY_REFERRAL` | `DOCTOR` |
| `DIAGNOSTIC_EQUIPMENT_NEED` | `DOCTOR` |
| `CLINICIAN_EMERGENCY_HANDOFF` | `DOCTOR` |

These codes are business fields, never `AuditReasonCode` or `NhatKyHeThong.maLyDo`. Neither the business reason nor the free-text note may be placed in those auth/authorization reason fields. Do not reuse clinical `lyDoKham`.

The reason-role mapping is an additional restriction, not a grant or scope exception. For every reason, source and destination facility must equal the session facility. Only `RECEPTIONIST` may change department within that facility; `DOCTOR` must keep source and destination department equal to the selected session department. This includes `PATIENT_SPECIALTY_PREFERENCE`, load balancing, clinical referral and emergency handoff; their labels do not permit a DOCTOR cross-department transfer. All other roles remain denied. Transfer grants no destination patient, history or queue reads.

### RECON-04 safeguards retained

- `RECEPTIONIST`: only `CHO_KHAM`; `DOCTOR`: only `CHO_KHAM`, `DANG_KHAM`, `CHO_CLS`. Deny final/completed, cancelled, soft-deleted and EMR-locked visits.
- Require an active destination room belonging to the destination department. Allocate a new destination queue number; preserve priority without auto-promotion.
- Preserve visit status, clinical `ngayKham` and existing examination timestamps; queue allocation must not reset or rewrite the clinical timeline.
- Retain linked service orders, prescriptions, results, invoices and financial records. Do not automatically mutate/cancel them, change billing or create duplicate charges. Outstanding work may follow only when safety, performing unit and responsibility remain intact; otherwise block until completion or explicit safe handoff. Issued prescriptions, confirmed results and posted financial transactions remain immutable; adjustments require their separately authorized, reconciled and audited workflows.
- Retain transaction consistency, optimistic concurrency, domain invariants and exception-path acceptance. Cancellation and DOCTOR-only vitals-write boundaries remain unchanged.

### Queue allocation decisions

`queueDate` is separate from clinical `ngayKham`. Registration and transfer share one number stream per `(roomId, queueDate)`. Never reuse any committed number; visible gaps are allowed. Capture one SQL Server DB UTC instant at transaction start and convert that same instant to an `Asia/Ho_Chi_Minh` date for `queueDate`. Derive `queueDate` only from that captured instant, including transactions crossing clinic-local midnight. Technical/audit time remains UTC.

These decisions do not select a schema, persistence shape, counter/ledger mechanism, migration/cutover or legacy-data behavior.

### Immutable history, audit and retry outcomes

- Require a dedicated immutable transfer-history record **and** a successful security audit event in the same DB transaction as the transfer mutation and queue allocation. All required writes commit together or fail/roll back together; audit-only is no longer an option.
- A future audit action value may be `VISIT_TRANSFERRED`; adding an enum value alone does not wire or emit an event. Business reason/note storage remains separate from auth/authorization reason codes.
- Same key + same canonical payload after commit: reauthorize the actor before returning only the stored operation receipt. Do not repeat mutation, allocation, history, success audit or other side effects.
- Same key + different payload: return a conflict without writes.
- On timeout, retry with the same key and resolve whether the original transaction committed or rolled back before proceeding. Timeout alone never proves rollback; do not treat it as permission for a new operation.

### Historical RECON-05 remaining decisions snapshot — superseded for N2

1. **Optional note:** length, normalization, privacy safeguards and exact storage field.
2. **Persistence and cutover:** shape/DDL for `queueDate`, the shared allocator, retained allocations, immutable history and idempotency receipts; counter/ledger implementation, migration, cutover and legacy semantics.
3. **Idempotency details:** key scope/lifetime; canonical payload field list and normalization; exact receipt data; concurrent duplicate waiting, wait deadline, commit-resolution mechanism and indeterminate response; reservation of failed/denied attempts. Approved replay/conflict/timeout outcomes above are fixed.
4. **Linked work and acceptance:** trustworthy eligibility readers, linked-work/handoff evidence and workflow acceptance; transfer-specific security acceptance. Reason codes do not prove linked-work safety.

**SEC-01 remains OPEN; runtime transfer remains default-deny.** No runtime enablement, permissions/grants, destination reads, history-reader grant, billing changes, note policy or migration is authorized. Every required guard, persistence, audit, concurrency, rollback and workflow acceptance must pass before a separately authorized runtime implementation can be enabled. Future acceptance uses synthetic data and must cover the six-code role matrix, unchanged status/scope/room and linked-work invariants, shared registration/transfer allocation, local-date boundary, committed-number non-reuse, atomic rollback, authorized stored-receipt replay, write-free payload conflict and timeout/duplicate commit resolution.

## Historical transfer reconciliation and prior review record — through RECON-04

The transfer reconciliation below is a historical snapshot through RECON-04, superseded by RECON-05 and then supplemented by RECON-06 on 2026-11-06. Its source inventory, guidance/validation claims and acceptance proposals are preserved. References to “current”, “still open” or choices awaiting approval describe those dated snapshots only; the RECON-06 addendum above controls. Unrelated lifecycle decisions/proposals remain unchanged. The old source findings have not been revalidated against implementation files for this documentation-only task.

## Chính sách đã duyệt — không hỏi lại

- `RECEPTIONIST` chỉ hủy lượt `CHO_KHAM` trước khi có chỉ định hoặc thu phí; cần xác nhận, lý do và audit.
- Điều chuyển: `RECEPTIONIST` chỉ được chuyển lượt `CHO_KHAM`; `DOCTOR` được chuyển lượt `CHO_KHAM`, `DANG_KHAM` hoặc `CHO_CLS`. Lượt final/completed, đã hủy, soft-deleted hoặc EMR-locked bị từ chối.
- Ranh giới actor/scope: `RECEPTIONIST` chỉ chuyển giữa các khoa trong cùng cơ sở; `DOCTOR` chỉ chuyển trong khoa đã chọn. Điều chuyển không cấp quyền đọc hồ sơ, lịch sử hoặc hàng đợi khoa đích.
- Phòng đích bắt buộc, phải active và thuộc khoa đích.
- Cấp số thứ tự mới tại queue đích, giữ nguyên mức ưu tiên hiện tại và không tự nâng ưu tiên.
- Khi điều chuyển, giữ nguyên trạng thái lượt, `ngayKham` và các timestamp khám hiện có. Phân bổ queue đích không thay đổi ngày khám lâm sàng hoặc timeline khám.
- Dùng ngày điều chuyển địa phương `Asia/Ho_Chi_Minh` làm ngày queue riêng `queueDate`, tách biệt và không ghi đè `ngayKham`; metadata kỹ thuật/audit vẫn dùng UTC.
- Lý do điều chuyển bắt buộc dùng mã lý do cố định; ghi chú ngắn là tùy chọn. Không tái sử dụng trường lý do khám lâm sàng `lyDoKham`.
- Giữ nguyên liên kết của chỉ định dịch vụ, đơn thuốc, kết quả, hóa đơn và hồ sơ tài chính; không tự hủy/sửa, không đổi billing và không tạo khoản thu trùng. Pending work chỉ được theo lượt nếu chứng minh an toàn và giữ nguyên đơn vị thực hiện/trách nhiệm; nếu không thì phải hoàn tất/handoff hoặc từ chối chuyển.
- Đơn thuốc đã phát hành, kết quả đã xác nhận và giao dịch tài chính đã post là bất biến; mọi điều chỉnh thuộc workflow riêng được cấp quyền, đối soát và audit.
- Bảo đảm nhất quán transaction, optimistic concurrency, domain invariants và kiểm thử exception paths.
- Metadata kỹ thuật/audit dùng UTC; thời điểm nghiệp vụ dùng `Asia/Ho_Chi_Minh`.
- Chỉ `DOCTOR` được ghi/sửa sinh hiệu.
- Mỗi phiên sử dụng một role và một cặp cơ sở+khoa. Hành động nhạy cảm có guard ở Application, không dựa vào ẩn nút UI.

Nguồn phê duyệt điều chuyển: `VIS-TRANSFER-OWNER-DECISION-RECON-04`, 2026-11-06; xem [transfer contract review](phase3-visit-transfer-contract-review.md) và [decision-gap memo](phase3-visit-transfer-decision-gaps.md). Hợp đồng này cập nhật các quyết định trước trong `VIS-TRANSFER-OWNER-DECISION-RECON-03`; actor/scope nền tảng được ghi trong [ma trận role/action đã duyệt](phase2-role-permission-matrix-approval.md), thời gian kỹ thuật/nghiệp vụ trong [Phase 2 implementation contract](phase2-implementation-contract.md). `VIS-01` hiện chỉ có các mảnh Domain và UI prototype; chưa có handler, repository lifecycle, audit events hoặc SQL acceptance ([review vòng đời lượt khám](phase3-visit-lifecycle-review.md), [transfer review](phase3-visit-transfer-contract-review.md)).

## Quyết định điều chuyển còn mở

Chỉ các mục sau đây vẫn chưa được quyết định theo contract `VIS-TRANSFER-OWNER-DECISION-RECON-04`. Không suy diễn chi tiết triển khai chưa được duyệt:

| Chủ đề còn mở | Phạm vi chưa được quyết định |
|---|---|
| Mã lý do và ghi chú | Danh mục mã lý do; ràng buộc độ dài/chuẩn hóa của ghi chú; quy tắc privacy. |
| Chi tiết phân bổ queue | Namespace sequence, reuse/gap policy và chính xác thời điểm transaction lấy mẫu `queueDate`; hình dạng persistence chưa được quyết định. |
| Lịch sử điều chuyển | Lưu lịch sử bất biến riêng, chỉ audit, hay cả hai. |
| Replay idempotent | Semantics khi replay, bao gồm cùng key nhưng payload khác; không suy diễn kết quả. |

Status, `ngayKham`, examination timestamps, tính bắt buộc của mã lý do, `queueDate` tách biệt theo ngày địa phương và ranh giới timezone đã được quyết định; không đưa chúng trở lại danh sách mở. Các lựa chọn trước đây về hủy và sinh hiệu thuộc quyết định vòng đời riêng. Snapshot sáu lựa chọn mở theo `VIS-TRANSFER-OWNER-DECISION-RECON-03` ngày 06/11/2026 được giữ tại [transfer contract review](phase3-visit-transfer-contract-review.md) và [decision-gap memo](phase3-visit-transfer-decision-gaps.md), nay đã được RECON-04 thay thế.

## Các quyết định vòng đời khác (ngoài contract điều chuyển)

| Chủ đề | Lựa chọn | Khuyến nghị |
|---|---|---|
| Hủy lượt — phát hiện chỉ định/thu phí | A. Chỉ cho hủy khi không có chỉ định hoạt động và không có khoản thu/phiếu đã ghi nhận; kiểm tra cùng transaction. B. Cấm hủy khi tồn tại bất kỳ record chỉ định/thu nào, kể cả void/reversed. | A theo trạng thái nghiệp vụ hiện hành; tiêu chí “hoạt động/đã thu” phải tra dữ liệu nguồn. Hiện billing/đơn dịch vụ chưa có application workflow hoàn chỉnh nên giữ hủy DENY cho tới khi eligibility query có nguồn tin cậy. Lỗi dependency phải từ chối hủy. |
| Hủy lượt — audit | A. Lý do tự do bắt buộc, giới hạn độ dài và không nhập thông tin y khoa. B. Lý do chọn từ danh mục mã cố định, có ghi chú tùy chọn. | B giảm biến thiên và hạn chế PII; danh mục mã phải được nghiệp vụ thống nhất trước khi triển khai. |
| Sinh hiệu — lưu đo lặp | A. Mỗi lần đo là record mới, giữ lần đo trước; sửa chỉ bằng bác sĩ theo lý do. B. Một row hiện hành trên mỗi lượt khám, cập nhật ghi đè. | A tốt hơn cho lịch sử lâm sàng; cần thêm measured-at/measured-by/measurement ID nếu chưa được bảo đảm. B đơn giản hơn nhưng mất lịch sử đo. |
| Sinh hiệu — thời điểm | A. `measuredAt` là giờ nghiệp vụ `Asia/Ho_Chi_Minh`; created/updated/audit metadata là UTC. B. Chỉ lưu thời điểm metadata UTC. | A phân biệt thời điểm đo với thời điểm nhập; không backdate ngoài chính sách được duyệt. |
| Sinh hiệu — ai đọc | A. DOCTOR trong đúng pair cơ sở+khoa. B. RECEPTIONIST cũng được đọc trong khâu tiếp đón. | A là đề xuất bảo mật; Owner đã chốt DOCTOR-only cho ghi nhưng quyền đọc sinh hiệu chưa được chốt riêng. Mặc định giữ deny cho role chưa được duyệt. |

## Hướng triển khai đề xuất

1. **Không triển khai hủy trước khi xác định được nguồn dữ liệu chỉ định/thu phí.** Trong transaction kiểm tra current status/version, active orders và posted receipts; nếu service không sẵn sàng hoặc kết quả mơ hồ thì từ chối.
2. **Tách command:** `CancelVisitCommand`, `TransferVisitCommand`, `RecordVitalsCommand` với context lấy từ session; không làm một mutation tổng quát dựa trên enum/setter.
3. **Transfer persistence:** cập nhật location/queue theo quy tắc đã duyệt trong transaction nhất quán; lưu queue date ở `queueDate` riêng, không ghi đè `ngayKham` hoặc examination timestamps. Mã lý do cố định là bắt buộc, ghi chú ngắn tùy chọn; taxonomy, note constraints/privacy, transaction sampling và lựa chọn lịch sử riêng/audit-only/cả hai vẫn mở. Không tự chọn loại lưu vết hoặc triển khai migration.
4. **Sinh hiệu:** lưu actor và measurement time riêng; đọc lại theo authorization/audit. Nếu cho phép sửa, giữ lịch sử hoặc audit diff không chứa narrative thừa.
5. **Tắt các menu prototype** cho tới khi command và guard đã nối; UI chỉ là lớp thao tác, không thay authorization.

## Acceptance gates

- Unit/application tests cho guard trạng thái, đúng role/scope, denial reason-safe, order/charge eligibility và dependency failure.
- SQL Server opt-in synthetic tests cho optimistic concurrency, cấp số đích theo namespace/date đã được chốt, invariant liên kết work và rollback trên exception; kiểm thử history theo lựa chọn lưu trữ sẽ được Owner chốt riêng.
- Kiểm thử transfer không gọi patient search/detail/history hoặc queue read khoa đích.
- Kiểm thử sinh hiệu DOCTOR allow, các role khác deny; measured-at và actor đúng; từ chối số đo không hợp lệ; kiểm tra history/revision theo quyết định.
- Headless Swing tests, sau đó desktop visual smoke trên dữ liệu tổng hợp; test replay chỉ xác minh semantics sau khi được quyết định, không giả định kết quả. Giữ mọi workflow ngoài scope ở default-deny.

## Nguồn

- [`phase3-visit-lifecycle-review.md`](phase3-visit-lifecycle-review.md)
- [`phase3-visit-transfer-contract-review.md`](phase3-visit-transfer-contract-review.md)
- [`phase2-role-permission-matrix-approval.md`](phase2-role-permission-matrix-approval.md)
- [`phase2-implementation-contract.md`](phase2-implementation-contract.md)
- `clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/{LuotKham,SinhHieu}.java`
- `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql`
