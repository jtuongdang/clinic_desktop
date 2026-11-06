# VIS-TRANSFER-DECISION-GAP-01 — Phase 3 Visit Transfer Decision-Gap Memo

**Tài liệu:** Bản ghi nhận khoảng trống quyết định nghiệp vụ cho luồng điều chuyển lượt khám (Visit Transfer Decision-Gap Memo)<br>
**Mã task:** `VIS-TRANSFER-DECISION-GAP-01`<br>
**Trạng thái:** Báo cáo phân tích kỹ thuật; các lựa chọn điều chuyển dưới đây đã được đối soát theo quyết định Owner ngày 06/11/2026. Không thay đổi source code, schema migration, ma trận phân quyền, hoặc kích hoạt runtime workflow.<br>
**Gate an ninh:** Mọi workflow điều chuyển tiếp tục bị từ chối (`DENY`) trong runtime theo cổng an ninh SEC-01 ([`phase2-implementation-contract.md:32-50`](phase2-implementation-contract.md#L32-L50)).

## RECON-05 addendum — 2026-11-06 (current approved contract)

**Authority:** Owner-confirmed `VIS-TRANSFER-OWNER-DECISION-RECON-05`, received 2026-11-06, following RAG-grounded proposals. This addendum controls over the historical snapshots below and carries forward `VIS-TRANSFER-OWNER-DECISION-RECON-04` without reopening its approved rules.

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

### Remaining decisions and evidence — no answers inferred

1. **Optional note:** length, normalization, privacy safeguards and exact storage field.
2. **Persistence and cutover:** shape/DDL for `queueDate`, the shared allocator, retained allocations, immutable history and idempotency receipts; counter/ledger implementation, migration, cutover and legacy semantics.
3. **Idempotency details:** key scope/lifetime; canonical payload field list and normalization; exact receipt data; concurrent duplicate waiting, wait deadline, commit-resolution mechanism and indeterminate response; reservation of failed/denied attempts. Approved replay/conflict/timeout outcomes above are fixed.
4. **Linked work and acceptance:** trustworthy eligibility readers, linked-work/handoff evidence and workflow acceptance; transfer-specific security acceptance. Reason codes do not prove linked-work safety.

**SEC-01 remains OPEN; runtime transfer remains default-deny.** No runtime enablement, permissions/grants, destination reads, history-reader grant, billing changes, note policy or migration is authorized. Every required guard, persistence, audit, concurrency, rollback and workflow acceptance must pass before a separately authorized runtime implementation can be enabled. Future acceptance uses synthetic data and must cover the six-code role matrix, unchanged status/scope/room and linked-work invariants, shared registration/transfer allocation, local-date boundary, committed-number non-reuse, atomic rollback, authorized stored-receipt replay, write-free payload conflict and timeout/duplicate commit resolution.

## Historical transfer reconciliation and prior review record — through RECON-04

The transfer reconciliation below is a historical snapshot superseded by RECON-05 on 2026-11-06. Its source inventory, guidance/validation claims and acceptance proposals are preserved. Transfer references below to “current”, “still open” or choices awaiting approval describe that dated snapshot only; the RECON-05 addendum above controls. Unrelated lifecycle decisions/proposals remain unchanged and are not settled by RECON-05. The old source findings have not been revalidated against implementation files for this documentation-only task.

> **Prior snapshot — 06/11/2026:** Owner approved `VIS-TRANSFER-OWNER-DECISION-RECON-03`; its six open choices are retained as a dated historical snapshot.
>
> **Current supersession — 2026-11-06:** `VIS-TRANSFER-OWNER-DECISION-RECON-04` supersedes RECON-03 for visit status/timestamps, reason requirement, and queue date. Section 4 is the current ledger; only its four enumerated implementation areas remain unresolved. Historical options are not current proposals or implementation authority. Transfer remains default-deny under SEC-01.

---

## 1. Tóm tắt điều hành & Mục tiêu tài liệu

Tài liệu này đối soát hiện trạng mã nguồn và tài liệu Phase 3 với quyết định Owner đã phê duyệt theo contract hiện hành `VIS-TRANSFER-OWNER-DECISION-RECON-04`, nhằm tách chính sách cố định khỏi các lựa chọn triển khai còn mở cho điều chuyển lượt khám (`LuotKham`). Snapshot sáu lựa chọn mở của RECON-03 được giữ lại như lịch sử.

Tài liệu tuân thủ nghiêm ngặt 3 nguyên tắc:
1. **Không mở lại các chính sách đã duyệt (settled policies):** Giữ nguyên vai trò, phạm vi (role/scope), ranh giới hủy lượt và ranh giới ghi sinh hiệu làm biên cố định.
2. **Không tự quyết định thay Owner:** Chỉ trình bày các lựa chọn khả thi kèm tác động kỹ thuật/kiến trúc cụ thể.
3. **Phân định minh bạch:** Tách bạch rõ ràng giữa **Thực tế mã nguồn (Fact)**, **Chính sách đã duyệt (Policy)**, và **Suy luận kỹ thuật (Inference)**.

---

## 2. Ranh giới chính sách đã phê duyệt — Cố định, không mở lại

Các ranh giới sau đây đã được Owner phê duyệt chính thức tại [`phase2-role-permission-matrix-approval.md:1-27`](phase2-role-permission-matrix-approval.md#L1-L27) và tái khẳng định tại [`phase3-visit-lifecycle-owner-decisions.md:7-15`](phase3-visit-lifecycle-owner-decisions.md#L7-L15). Các mục này đóng vai trò là tiền đề bất biến (invariants) cho luồng điều chuyển:

* **Chính sách điều chuyển lượt khám (`DEC-AUTHZ-04b`):**
  - `RECEPTIONIST` (Tiếp đón): Được phép điều chuyển lượt khám giữa các khoa trong **cùng một cơ sở** (`source.facilityId == destination.facilityId == session.facilityId`). Nghiêm cấm điều chuyển xuyên cơ sở.
  - `DOCTOR` (Bác sĩ): Chỉ được phép điều chuyển lượt khám **trong nội bộ khoa** mà phiên làm việc đã chọn (`source.facilityId == destination.facilityId == session.facilityId` và `source.departmentId == destination.departmentId == session.departmentId`, ví dụ: chuyển giữa các phòng khám trong cùng khoa).
  - Các vai trò khác (`ADMIN`, `PHARMACIST`, `CASHIER`, `LAB_TECH`): Hoàn toàn bị cấm (`DENY`) thực hiện điều chuyển.
  - **Ranh giới cô lập dữ liệu (`DEC-AUTHZ-03`):** Thao tác điều chuyển là một tác vụ cập nhật vị trí/trạng thái có guard chuyên biệt; thao tác này **tuyệt đối không cấp quyền đọc** hồ sơ bệnh nhân, lịch sử khám hay hàng đợi của khoa/phòng đích cho người thực hiện.
* Các quy tắc điều chuyển bổ sung được Owner duyệt ngày 06/11/2026 theo `VIS-TRANSFER-OWNER-DECISION-RECON-04`: RECEPTIONIST chỉ chuyển `CHO_KHAM`; DOCTOR chuyển `CHO_KHAM`, `DANG_KHAM`, `CHO_CLS`; final/completed, hủy, soft-deleted hoặc EMR-locked đều bị từ chối. Phòng đích bắt buộc, active và thuộc khoa đích. Cấp số queue mới tại đích, giữ nguyên priority, không auto-promote.
* Khi chuyển, giữ nguyên trạng thái, `ngayKham` và examination timestamps hiện có. Dùng `queueDate` riêng bằng ngày địa phương `Asia/Ho_Chi_Minh`; không ghi đè ngày khám lâm sàng. Lý do bắt buộc là fixed reason code, short note tùy chọn; không dùng lại `lyDoKham`.
* Giữ nguyên các liên kết dịch vụ, đơn thuốc, kết quả, hóa đơn và tài chính; không tự hủy/sửa liên kết, đổi billing hay tạo khoản thu trùng. Pending work chỉ được theo lượt nếu an toàn và đơn vị thực hiện/trách nhiệm vẫn nguyên vẹn; nếu không thì cần hoàn tất/handoff hoặc từ chối. Đơn thuốc đã phát hành, kết quả đã xác nhận và giao dịch tài chính đã post là bất biến; điều chỉnh thuộc workflow riêng được cấp quyền, đối soát và audit.
* Bắt buộc nhất quán transaction, optimistic concurrency, domain invariants và exception-path tests. Metadata kỹ thuật/audit dùng UTC; thời gian nghiệp vụ dùng `Asia/Ho_Chi_Minh`.
* **Ranh giới hủy lượt khám (`DEC-AUTHZ-04a`):**
  - Chỉ duy nhất `RECEPTIONIST` được hủy lượt khám.
  - Lượt khám chỉ hợp lệ để hủy khi đang ở trạng thái `CHO_KHAM` và **chưa có bất kỳ chỉ định cận lâm sàng/dịch vụ nào cũng như chưa phát sinh bất kỳ khoản thu phí/hóa đơn nào**. Bắt buộc xác nhận, nhập lý do và ghi audit. Ranh giới này độc lập và không bị nới lỏng bởi luồng điều chuyển.
* **Ranh giới ghi nhận sinh hiệu (`DEC-AUTHZ-01`):**
  - Chỉ duy nhất `DOCTOR` được ghi hoặc sửa sinh hiệu (`SinhHieu`) trong phạm vi khoa phòng phiên làm việc (`KHOA_PHONG`); `RECEPTIONIST` và các vai trò khác bị từ chối tuyệt đối. Ranh giới này không bị ảnh hưởng hay gộp chung vào màn hình điều chuyển.
* **Quy tắc phiên làm việc đơn (`Session Scope Boundary`):**
  - Mỗi phiên làm việc của nhân viên chỉ gắn với duy nhất 1 vai trò và 1 cặp `(facilityId, departmentId)`. Mọi kiểm tra quyền thông thường đều dựa trên cặp này tại Application boundary.

---

## 3. Đối chiếu hiện trạng: Fact, Policy và Inference

### 3.1. Thực tế mã nguồn hiện tại (Fact)

| Thành phần | Vị trí mã nguồn | Thực tế ghi nhận |
|---|---|---|
| **Prototype điều chuyển trên UI** | [`PatientQueueContextMenu.java:71-75`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/menus/PatientQueueContextMenu.java#L71-L75)<br>[`ExaminationPanel.java:1080-1091`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/ExaminationPanel.java#L1080-L1091) | Menu chuột phải "Chuyển Bệnh Nhân Sang Phòng Khác" mở [`TransferRoomSelectDialog.java:15-34`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/dialogs/TransferRoomSelectDialog.java#L15-L34) chứa danh sách tên phòng hardcode dạng chuỗi (`"Phòng 101 - Khám Nội 1"`,...). Nút "Lưu" chỉ đóng dialog và mở `TransferSlipPrintPreviewDialog` để in phiếu; **hoàn toàn không gọi Application service, không gửi ID lượt khám, không kiểm tra quyền và không cập nhật CSDL**. |
| **Giao diện tiếp đón** | [`ReceptionPanel.java:77-115`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L77-L115) | Màn hình tiếp đón chỉ tải danh sách phòng khám và hàng đợi trong ngày theo scope phiên; **chưa có bất kỳ nút, menu hay hành động điều chuyển nào**. |
| **Mô hình Domain `LuotKham`** | [`LuotKham.java:29-55,88-124`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L29-L55) | Thực thể lưu `maCoSo`, `maKhoa`, `maPhongKham` (nullable), `trangThai`, `soThuTu`, `soThuTuDisplay`, `dnVnTien`, và cờ `emrLocked`. Có các phương thức `startExam()`, `completeExam()`, `lockEmr()`, nhưng **không có phương thức `transfer()` hay Domain Event nào liên quan đến điều chuyển**. Trạng thái enum [`TrangThaiLuotKham.java:3-10`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/TrangThaiLuotKham.java#L3-L10) liệt kê giá trị đơn thuần, không có state machine. |
| **Cơ sở dữ liệu & Schema** | [`V1__Initial_Clinic_Schema.sql:329-380`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L329-L380) | Bảng `LuotKham` có các cột `maCoSo`, `maKhoa`, `maPhongKham`, `version`, `trangThai`. **Không có bảng lịch sử điều chuyển, không lưu vết khoa/phòng nguồn, người thực hiện, thời điểm chuyển, hoặc lý do chuyển**. |
| **Cổng phân quyền (Authorization)** | [`AuthorizationService.java:51-54`](../../clinic-application/src/main/java/vn/clinicmanagement/application/authorization/AuthorizationService.java#L51-L54) | Phương thức `evaluate()` từ chối mọi yêu cầu có `targetFacilityId != context.facilityId() || targetDepartmentId != context.departmentId()` với mã `TARGET_MISMATCH`. Cơ chế đánh giá chung này **không thể xử lý yêu cầu chuyển liên khoa cùng cơ sở của `RECEPTIONIST`**, đòi hỏi một guard riêng cho use case điều chuyển. |
| **Cổng tiếp đón & Truy vấn hàng đợi** | [`ReceptionQueuePort.java:9-15`](../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/ReceptionQueuePort.java#L9-L15)<br>[`ReceptionQueueRepositoryImpl.java:125-155`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L125-L155) | Cổng hàng đợi chỉ hỗ trợ đăng ký mới (`registerPatient`) và đọc hàng đợi (`getReceptionQueue`, `getRoomSummaries`). Thứ tự hàng đợi được sắp xếp cố định theo `ORDER BY l.dnVnTien DESC, l.soThuTu, l.id` cho đúng cặp `(maCoSo, maKhoa)` của ngày khám; **chưa có hàm cập nhật vị trí/hàng đợi khi chuyển**. |
| **Hệ thống Audit** | [`AuditActionCode.java:23-34`](../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditActionCode.java#L23-L34) | Đã có mã hành động `VISIT_REGISTERED`, nhưng **chưa có mã hành động `VISIT_TRANSFERRED`**. |

### 3.2. Chính sách đã phê duyệt (Policy)

* `DEC-AUTHZ-04b`: `RECEPTIONIST` chỉ chuyển cùng cơ sở; `DOCTOR` chỉ trong khoa đã chọn; không chuyển xuyên cơ sở, không cấp quyền đọc dữ liệu khoa đích ([`phase2-role-permission-matrix-approval.md:8-18`](phase2-role-permission-matrix-approval.md#L8-L18)).
* `VIS-TRANSFER-OWNER-DECISION-RECON-04`, Owner approved 2026-11-06: carried-forward actor/scope, status eligibility, target-room, queue-number/priority, linked-work, immutability, transaction/concurrency/invariant/test rules remain approved. New decisions preserve status/`ngayKham`/examination timestamps; require fixed reason code with optional short note; and use separate local-date `queueDate`. See [`phase3-visit-transfer-contract-review.md`](phase3-visit-transfer-contract-review.md).
* `DEC-TIME-01`: Technical/audit metadata uses UTC and clinic business time uses `Asia/Ho_Chi_Minh` ([`phase2-implementation-contract.md:8-12`](phase2-implementation-contract.md#L8-L12)); `queueDate` uses the transfer-local business date, while persistence and transaction sampling details remain open.
* Cổng an ninh `SEC-01`: Giữ trạng thái từ chối mặc định (`DENY`) ở tầng runtime cho tới khi implementation contract, guard, audit và bộ kiểm thử đạt chuẩn ([`phase2-implementation-contract.md:32-50`](phase2-implementation-contract.md#L32-L50)).

### 3.3. Suy luận kỹ thuật (Inference)

* Để thực hiện điều chuyển mà không phá vỡ tính đóng băng của `AuthorizationService.evaluate()` (vốn bảo vệ toàn bộ các thao tác thông thường), cần một **Transfer Authorization Guard chuyên biệt** trong tầng Application để kiểm tra đồng thời cặp nguồn và cặp đích theo ma trận `DEC-AUTHZ-04b`.
* Luồng điều chuyển cần giữ kết quả tối thiểu và **không trả về thông tin danh sách bệnh nhân hoặc chi tiết hàng đợi của khoa đích**, phù hợp nguyên tắc không cấp quyền đọc liên khoa.
* Luồng điều chuyển phải bảo đảm transaction consistency trong `TransactionBoundary`. Cách lưu trữ lịch sử (bản ghi riêng, audit-only, hay cả hai) vẫn là lựa chọn mở; chỉ ghi các artifact theo contract Owner sẽ chốt sau, và lỗi ở bất kỳ write bắt buộc nào phải fail-closed/rollback.

---

## 4. Đối soát quyết định Owner — chính sách cố định và lựa chọn còn mở

Provenance: current approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-04`, 2026-11-06; cross-reference [`phase3-visit-transfer-contract-review.md`](phase3-visit-transfer-contract-review.md) and [`phase3-visit-lifecycle-owner-decisions.md`](phase3-visit-lifecycle-owner-decisions.md). The earlier RECON-03 ledger is preserved as a dated historical snapshot.

**Đã duyệt — không còn là khoảng trống:** actor-specific source statuses and scope; no destination patient/history/queue reads; final/completed, cancelled, soft-deleted, and EMR-locked visits are denied; required active destination room belonging to the target department; new destination queue number with existing priority preserved and no auto-promotion; preserve visit status, `ngayKham`, and existing examination timestamps; mandatory fixed reason code with optional short note; separate transfer-local `queueDate` in `Asia/Ho_Chi_Minh`; linked-record preservation, no automatic mutation/cancellation/billing change/duplicate charges; safe pending-work handoff or completion/denial; immutability of issued prescriptions, confirmed results, and posted financial transactions; transaction consistency, optimistic concurrency, domain invariants, exception-path tests; UTC technical/audit metadata. Queue allocation does not rewrite the clinical visit date/timeline.

**Chỉ còn bốn nhóm chi tiết triển khai mở:**

1. **Reason taxonomy and note safeguards:** The fixed-code taxonomy, optional short note length/normalization constraints, and privacy rules. The fixed-code requirement and note optionality are settled; do not reuse clinical `lyDoKham`.
2. **Queue allocation implementation:** Detailed persistence shape, sequence namespace, allocation reuse/gap policy, and exact transaction sampling semantics for approved `queueDate`. The queue date is the transfer-local business date and remains separate from `ngayKham`.
3. **Transfer history:** Dedicated immutable transfer history, audit-only, or both.
4. **Idempotent replay:** Replay outcome semantics, including a key reused with a different payload; do not infer replay behavior.

The prior RECON-03 ledger had the six open choices reproduced in the historical snapshot below. RECON-04 resolves the reason requirement, queue date, and visit status/timestamp questions as stated above; the remaining open details are enumerated in the current four groups. Implementation evidence for safely resolving linked-work state and handoff is not yet complete. Keep transfer fail-closed until it can prove the approved requirements. This document does not authorize code, migration, grants, or runtime enablement.

### Historical open-choice snapshot — RECON-03 (06/11/2026; superseded)

At this dated snapshot, these six implementation choices remained open; this list is preserved for provenance only and is not the current ledger:

1. **Transfer reason:** Whether a reason was required, the allowed reason-code set, and free-text rules.
2. **Effective-date boundary:** The effective-date boundary and queue business date.
3. **Queue sequence namespace/reuse:** Sequence namespace and number reuse guarantees.
4. **Visit status/timestamps:** Whether status/timestamps would be preserved or transitioned.
5. **Transfer history:** Dedicated immutable transfer history, audit-only, or both.
6. **Idempotent replay:** Replay outcome semantics, including reuse of a key with a different payload.

## Historical pre-approval options (superseded 06/11/2026)

The option analysis below records the report snapshot before Owner approval. It is retained for provenance only: no old option or recommendation remains a current unresolved decision or implementation instruction. The approved section 4 above controls.

Historical snapshot: the original report presented seven candidate gap areas and options before the contract was approved. The options and recommendation language below are superseded; the RECON-03 six-item open list and the pre-approval options do not replace the current RECON-04 ledger in section 4.

### Khoảng trống 1: Trạng thái lượt khám đủ điều kiện điều chuyển (Transfer-Eligible Statuses)
* **Hiện trạng nguồn:** `TrangThaiLuotKham` có các giá trị: `CHO_KHAM`, `DANG_KHAM`, `DA_KHAM`, `KET_LUAN`, `HOAN_THANH`, `DA_HUY`, `CHO_CLS` ([`TrangThaiLuotKham.java:3-10`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/TrangThaiLuotKham.java#L3-L10)). `LuotKham.java:43` có cờ `emrLocked`. Chính sách `DEC-AUTHZ-04b` chỉ nêu quyền của actor, chưa quy định lượt khám ở trạng thái nào thì được phép chuyển.
* **Các phương án lựa chọn:**
  * **Phương án 1A (Khắt khe - Chỉ hàng đợi chờ khám):** Chỉ cho phép điều chuyển khi lượt khám ở trạng thái `CHO_KHAM`. Nếu bác sĩ đã nhấn bắt đầu khám (`DANG_KHAM`) hoặc lượt đã kết thúc/hủy/khóa EMR thì từ chối.
  * **Phương án 1B (Linh hoạt lâm sàng theo vai trò):**
    - `RECEPTIONIST`: Chỉ được chuyển lượt ở trạng thái `CHO_KHAM`.
    - `DOCTOR`: Được chuyển lượt ở trạng thái `CHO_KHAM`, `DANG_KHAM`, hoặc `CHO_CLS` (phục vụ hội chẩn hoặc chuyển phòng chuyên môn trong khoa). Tuyệt đối cấm chuyển lượt ở trạng thái `HOAN_THANH`, `DA_HUY`, hoặc khi `emrLocked == true`.
  * **Phương án 1C (Toàn diện kèm trạng thái chuyển tiếp):** Cho phép cả `CHO_KHAM` và `DANG_KHAM`, nhưng khi chuyển, hệ thống tự động reset trạng thái lượt khám về `CHO_KHAM` tại đích và xóa thời gian `thoiGianBatDauKham`.
* **Tác động triển khai:**
  - Quy định logic kiểm tra invariant trong `LuotKham.transferTo(...)` hoặc `TransferVisitHandler`.
  - Định nghĩa các mã lỗi nghiệp vụ trả về giao diện: `VISIT_STATUS_INELIGIBLE`, `EMR_LOCKED_CANNOT_TRANSFER`.

---

### Khoảng trống 2: Yêu cầu về phòng khám đích (Destination Room Requirement & Hierarchy)
* **Hiện trạng nguồn:** Trong CSDL và Domain, trường `maPhongKham` là **nullable** ([`V1__Initial_Clinic_Schema.sql:336`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L336), [`LuotKham.java:42`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L42)). Phòng khám gắn trực tiếp với khoa thông qua `PhongKham.maKhoa` ([`PhongKham.java:4-21`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/facility/PhongKham.java#L4-L21)).
* **Các phương án lựa chọn:**
  * **Phương án 2A (Bắt buộc chỉ định phòng đích):** Người thực hiện chuyển (cả Tiếp đón và Bác sĩ) bắt buộc phải chọn cụ thể một phòng khám đích (`destinationRoomId` không được null). Phòng này phải đang hoạt động (`hoatDong == true`) và thuộc đúng khoa đích.
  * **Phương án 2B (Tùy chọn phòng theo phân cấp):**
    - Chuyển liên khoa (`RECEPTIONIST`): Chỉ bắt buộc chọn khoa đích (`destinationDepartmentId`); phòng đích (`destinationRoomId`) có thể để trống (null) để bộ phận điều phối tại khoa đích tự phân phòng sau.
    - Chuyển nội bộ khoa (`DOCTOR`): Bắt buộc phải chọn phòng khám đích cụ thể khác với phòng hiện tại.
* **Tác động triển khai:**
  - Ảnh hưởng trực tiếp đến validation của `TransferVisitCommand` (cho phép hay không cho phép `destinationRoomId == null`).
  - Định nghĩa câu truy vấn xác thực phòng khám đích: `SELECT COUNT(*) FROM PhongKham WHERE id = ? AND maKhoa = ? AND hoatDong = 1`.
  - Giao diện người dùng: Dropdown chọn phòng là bắt buộc hay tùy chọn.

---

### Khoảng trống 3: Quy tắc cấp số thứ tự, độ ưu tiên và vị trí hàng đợi tại đích (Queue Numbering & Priority)
* **Hiện trạng nguồn:** Hàng đợi được sắp xếp theo `dnVnTien DESC, soThuTu, id` ([`ReceptionQueueRepositoryImpl.java:154`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L154)). Hiện tại số thứ tự `soThuTu` được cấp tăng dần trong ngày khi tiếp đón đăng ký mới.
* **Các phương án lựa chọn:**
  * **Phương án 3A (Cấp số mới tại đích):** Cấp số thứ tự (`soThuTu`) mới nhất theo bộ đếm hàng đợi của khoa/phòng đích trong ngày. Số cũ tại khoa nguồn được lưu trữ vào lịch sử điều chuyển.
  * **Phương án 3B (Bảo lưu số cũ):** Giữ nguyên giá trị `soThuTu` ban đầu của bệnh nhân, chỉ thay đổi `maKhoa`/`maPhongKham`. Bệnh nhân sẽ đứng ở vị trí tương ứng với số cũ trong danh sách của phòng đích.
  * **Phương án 3C (Ưu tiên tiếp nhận điều chuyển):** Cấp số mới tại đích nhưng tự động bật cờ ưu tiên (`dnVnTien = true` hoặc cờ `isPriorityTransfer = true`) để bệnh nhân được đưa lên đầu danh sách chờ của khoa/phòng mới, tránh việc bệnh nhân phải chờ lại từ đầu.
* **Tác động triển khai:**
  - Nếu chọn 3A hoặc 3C: Cần phương thức cấp số nguyên tử `getNextQueueNumber(date, facilityId, departmentId, roomId)` trong repository và cập nhật lại cả `soThuTuDisplay`.
  - Nếu chọn 3B: Có nguy cơ trùng số thứ tự (`soThuTu`) hiển thị giữa hai bệnh nhân khác nhau tại cùng một phòng đích.

---

### Khoảng trống 4: Ràng buộc với chỉ định dịch vụ, đơn thuốc và thanh toán đang phát sinh (Orders & Billing Barriers)
* **Hiện trạng nguồn:** Thực thể `LuotKham` có quan hệ với `ChiDinhDichVu`, `DonThuoc`, và `HoaDon` ([`LuotKham.java:82-84`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L82-L84)). Chính sách hủy lượt (`DEC-AUTHZ-04a`) cấm hủy khi đã có chỉ định/thanh toán, nhưng chính sách điều chuyển (`DEC-AUTHZ-04b`) chưa quy định xử lý các khoản mục này ra sao khi chuyển khoa.
* **Các phương án lựa chọn:**
  * **Phương án 4A (Chặn tuyệt đối nếu có dịch vụ/chi phí):** Từ chối điều chuyển nếu lượt khám đã có bất kỳ chỉ định dịch vụ (`ChiDinhDichVu`), đơn thuốc (`DonThuoc`) hoặc hóa đơn (`HoaDon`) nào chưa hoàn tất hoặc đã phát sinh chi phí. Bắt buộc khoa nguồn phải hoàn tất hoặc hủy toàn bộ dịch vụ trước khi chuyển.
  * **Phương án 4B (Cho phép chuyển, dữ liệu gắn theo lượt khám):** Cho phép điều chuyển bình thường. Toàn bộ chỉ định và đơn thuốc đã kê ở khoa nguồn được giữ nguyên trong hồ sơ bệnh án của lượt khám đó, bác sĩ khoa đích tiếp tục theo dõi điều trị.
  * **Phương án 4C (Phân tách theo loại điều chuyển):**
    - Chuyển nội bộ khoa (`DOCTOR`): Cho phép chuyển phòng, toàn bộ chỉ định giữ nguyên.
    - Chuyển liên khoa (`RECEPTIONIST`): Chỉ cho phép khi chưa có chỉ định hoặc toàn bộ chỉ định đã có kết quả trả về.
* **Tác động triển khai:**
  - Yêu cầu xây dựng query kiểm tra tính toàn vẹn phụ thuộc trong cùng giao tác điều chuyển: `hasActiveOrders(visitId)`, `hasPendingBills(visitId)`.
  - Tác động đến báo cáo doanh thu và phân bổ thù lao bác sĩ/khoa phòng trong tương lai.

---

### Khoảng trống 5: Mốc thời gian hiệu lực nghiệp vụ và giới hạn ngày khám (Effective Time & Date Boundaries)
* **Hiện trạng nguồn:** `LuotKham.ngayKham` là kiểu `LocalDate` ([`LuotKham.java:38`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L38)). Chính sách thời gian `DEC-TIME-01` quy định giờ phòng khám dùng múi giờ `Asia/Ho_Chi_Minh`, thời gian hệ thống dùng UTC.
* **Các phương án lựa chọn:**
  * **Phương án 5A (Chỉ cho phép điều chuyển trong ngày):** Chỉ cho phép điều chuyển các lượt khám có `ngayKham` trùng với ngày hiện tại của hệ thống (theo giờ `Asia/Ho_Chi_Minh`). Lượt khám của các ngày trước đó không thể điều chuyển.
  * **Phương án 5B (Cho phép chuyển qua ngày kèm cập nhật ngày khám):** Nếu điều chuyển qua ngày (ví dụ ca trực đêm), cho phép cập nhật `ngayKham` sang ngày hiện tại, lưu vết `originalNgayKham` vào lịch sử.
* **Tác động triển khai:**
  - Logic xác thực trong Application: `if (!visit.getNgayKham().equals(currentDateInVietnam())) throw new TransferEligibilityException(...)`.
  - Đảm bảo tính nhất quán của hàng đợi tiếp đón (vốn truy vấn theo `WHERE ngayKham = ?`).

---

### Khoảng trống 6: Lưu trữ lịch sử điều chuyển, phân loại lý do và Payload Audit
* **Hiện trạng nguồn:** `AuditActionCode` chưa có mã chuyển lượt. Bảng `LuotKham` chỉ có các trường vị trí hiện tại, không có bảng con lưu lịch sử chuyển vị trí ([`V1__Initial_Clinic_Schema.sql:329-380`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L329-L380)).
* **Các phương án lựa chọn:**
  * **Phương án 6A (Chỉ dùng Audit Trail):**
    - Bổ sung `VISIT_TRANSFERRED` vào `AuditActionCode`.
    - Toàn bộ thông tin nguồn/đích (`sourceFacilityId`, `sourceDepartmentId`, `sourceRoomId`, `targetDepartmentId`, `targetRoomId`, `reasonCode`, `oldQueueNumber`, `newQueueNumber`) được lưu dưới dạng JSON trong metadata của bảng `AuditLog`. Không thay đổi schema bảng nghiệp vụ.
  * **Phương án 6B (Bảng lịch sử quan hệ chuyên biệt + Audit):**
    - Tạo bảng mới `LichSuDieuChuyenLuotKham` (Migration Flyway mới) để phục vụ truy vấn lâm sàng và in ấn phiếu chuyển viện/chuyển khoa.
    - Đồng thời vẫn ghi bản ghi `VISIT_TRANSFERRED` vào `AuditLog` để phục vụ giám sát an ninh.
  * **Quy chuẩn về lý do điều chuyển (Reason Semantics):**
    - *Lựa chọn R1 (Mã cố định + Ghi chú tùy chọn):* Chọn lý do từ danh mục mã chuẩn hóa (ví dụ: `CHUYEN_KHOA_CHUYEN_MON`, `YEU_CAU_BENH_NHAN`, `PHONG_KHAM_QUA_TAI`, `SAI_SOT_TIEP_DON`) kèm ghi chú bổ sung không chứa PII.
    - *Lựa chọn R2 (Văn bản tự do bắt buộc):* Bắt buộc nhập text tối thiểu 10 ký tự, tối đa 255 ký tự.
* **Tác động triển khai:**
  - Nếu chọn 6A: Không cần migration CSDL, triển khai nhanh và gọn nhẹ.
  - Nếu chọn 6B: Cần tạo migration mới, định nghĩa Entity `LichSuDieuChuyen` và repository tương ứng.
  - Mã lý do chuẩn hóa (R1) giúp ngăn chặn rò rỉ thông tin y tế nhạy cảm (PHI) vào log kiểm toán.

---

### Khoảng trống 7: Kiểm soát đồng thời (Concurrency), Khóa lạc quan và Tính lũy nghiệm (Idempotency)
* **Hiện trạng nguồn:** Bảng `LuotKham` có cột `version BIGINT DEFAULT 0` phục vụ khóa lạc quan ([`V1__Initial_Clinic_Schema.sql:367`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L367), [`BaseEntity.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/BaseEntity.java)).
* **Các phương án lựa chọn:**
  * **Phương án 7A (Khóa lạc quan CAS + Idempotency Key):**
    - Lệnh điều chuyển `TransferVisitCommand` gửi kèm `expectedVersion` và một `idempotencyKey` sinh từ UI.
    - Cập nhật bằng câu lệnh Compare-And-Swap (CAS): `UPDATE LuotKham SET ..., version = version + 1 WHERE id = ? AND version = ?`.
    - Nếu trùng `idempotencyKey` cùng payload: Trả về kết quả thành công của giao tác trước (lũy nghiệm), không thực hiện lại side effect.
    - Nếu version không khớp: Trả về lỗi xung đột `CONCURRENCY_CONFLICT`, yêu cầu client reload dữ liệu.
  * **Phương án 7B (Khóa bi quan hàng - Row-level Locking):**
    - Sử dụng `SELECT ... WITH (UPDLOCK, ROWLOCK)` trong transaction của SQL Server để tuần tự hóa các thao tác điều chuyển trên cùng một lượt khám.
* **Tác động triển khai:**
  - Ngăn ngừa tình trạng Lost Update (hai người dùng cùng chuyển một bệnh nhân đến hai nơi khác nhau cùng lúc).
  - Đảm bảo an toàn tuyệt đối cho số thứ tự hàng đợi và tính toàn vẹn của giao tác.

---

### Historical option summary (superseded 06/11/2026)

| Khoảng trống quyết định | Tùy chọn A | Tùy chọn B | Tùy chọn C | Tác động kỹ thuật chính |
|---|---|---|---|---|
| **1. Trạng thái đủ điều kiện** | Chỉ `CHO_KHAM` | `CHO_KHAM` (Tiếp đón) + `DANG_KHAM`/`CHO_CLS` (Bác sĩ) | Cho phép cả hai, tự động reset về `CHO_KHAM` | Domain guard invariant, mapping lỗi trạng thái |
| **2. Phòng khám đích** | Bắt buộc mọi trường hợp | Bắt buộc với Bác sĩ; Tùy chọn với Tiếp đón | — | Command DTO nullable, query kiểm tra phòng active |
| **3. Số thứ tự & Hàng đợi** | Cấp số mới tại đích | Giữ nguyên số cũ | Cấp số mới kèm cờ ưu tiên (`dnVnTien = true`) | Cơ chế sinh sequence hàng đợi trong CSDL, sắp xếp queue |
| **4. Dịch vụ & Thu phí** | Chặn nếu có dịch vụ/thu phí | Cho phép, giữ nguyên dịch vụ | Khác nhau giữa Bác sĩ và Tiếp đón | Query phụ thuộc liên module, phân bổ chi phí |
| **5. Giới hạn ngày khám** | Chỉ chuyển trong ngày | Cho phép chuyển qua ngày | — | So khớp múi giờ `Asia/Ho_Chi_Minh`, invariant ngày |
| **6. Lưu vết & Lý do** | Chỉ ghi Audit Log JSON | Tạo bảng lịch sử riêng + Audit | — (Lý do: Mã enum R1 vs Text R2) | Quyết định có hay không có Flyway migration mới |
| **7. Đồng thời & Idempotency** | Khóa lạc quan CAS + Idempotency | Khóa bi quan `UPDLOCK` | — | Câu lệnh UPDATE SQL, xử lý retry trên Desktop UI |

---

### Historical proposed acceptance gates (superseded 06/11/2026)

The following criteria were drafted against the pre-approval options; they are retained as a historical snapshot, not an active implementation contract. Any implementation task must use the approved policy and open-choice list in current section 4, must not assume a resulting visit status or replay behavior, and must remain gated/default-deny.

1. **Phân quyền và bảo mật (Authorization):**
   - Bộ kiểm thử độc lập chứng minh: `RECEPTIONIST` chuyển khác cơ sở -> `DENIED`; `DOCTOR` chuyển khác khoa -> `DENIED`; vai trò khác -> `DENIED`.
   - Chứng minh bằng unit test và test giả lập (mocks/fakes): Quá trình điều chuyển **không gọi bất kỳ hàm đọc bệnh nhân (`getById`, `search`) hay hàm đọc hàng đợi của khoa đích**. Biên nhận trả về chỉ chứa thông tin tối giản của giao tác.
2. **Giao tác nguyên tử & Fail-closed:**
   - Cập nhật vị trí `LuotKham` và ghi nhận `AuditLog` phải nằm chung trong một `TransactionBoundary`.
   - Kiểm thử mô phỏng lỗi khi ghi audit: Toàn bộ cập nhật vị trí và số thứ tự phải được rollback hoàn toàn về trạng thái ban đầu.
3. **Đồng thời & Lũy nghiệm (Concurrency & Idempotency):**
   - Hai request điều chuyển đồng thời trên cùng một version lượt khám: Đúng 1 request thành công, request còn lại nhận lỗi xung đột rõ ràng, không làm hỏng dữ liệu hàng đợi.
4. **Không rò rỉ dữ liệu nhạy cảm:**
   - Payload audit chỉ chứa mã định danh, mã lý do, thời gian và số phiên; không chứa thông tin lâm sàng tự do hoặc PII thừa.

---

## 7. Tài liệu hướng dẫn và căn cứ đã đọc (Guidance Read)

Các tài liệu hướng dẫn bắt buộc đã được đọc kỹ trước khi lập báo cáo:
- `docs/PROJECT_CONVENTIONS.md` (Quy ước phát triển dự án và làm việc của Agent).
- `README.md` (Tổng quan kiến trúc dự án và thiết lập môi trường).
- `DOMAIN_ANALYSIS.md` (Báo cáo phân tích toàn diện package `vn.clinicmanagement.domain`).
- `ERD_DIAGRAM.md` (Sơ đồ thực thể quan hệ CSDL EHC HIS).
- `UI_CODING_STANDARDS.md` (Quy chuẩn thiết kế giao diện và màn hình Desktop).
- `docs/ke-hoach-trien-khai/phase2-role-permission-matrix-approval.md` (Phê duyệt ma trận phân quyền Phase 2).
- `docs/ke-hoach-trien-khai/phase2-implementation-contract.md` (Hợp đồng triển khai Phase 2 & Cổng SEC-01).
- `docs/ke-hoach-trien-khai/phase3-visit-transfer-contract-review.md` (Báo cáo review hợp đồng luồng điều chuyển).
- `docs/ke-hoach-trien-khai/phase3-visit-lifecycle-owner-decisions.md` (Đề xuất quyết định Owner vòng đời lượt khám).
- `docs/ke-hoach-trien-khai/phase3-visit-lifecycle-review.md` (Review vòng đời lượt khám).
