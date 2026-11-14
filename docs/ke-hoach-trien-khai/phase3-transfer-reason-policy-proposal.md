# VIS-TRANSFER-REASON-POLICY-01 — Transfer reason code taxonomy, note normalization, and privacy policy proposal

**Status:** Documentation-only RECON-06 supplement dated 2026-11-06. The six reason codes/roles and N2 optional-note policy are Owner-approved; the proposal and alternatives below are retained as historical material. RECON-04/05 remain controlling and unchanged outside the N2 decisions expressly resolved by RECON-06. No Java, tests, migration, permission, schema or runtime changes. SEC-01 and downstream gates remain OPEN; transfer remains default-deny pending separately authorized implementation and acceptance. SEC-01 Definition A does not itself accept future workflows ([baseline acceptance:4–9,23–32][sec01]).

**Contract:** Current authority is the task-supplied Owner-approved v1 `VIS-TRANSFER-OWNER-DECISION-RECON-06` (`13d19fa9-eaed-404e-bdfc-4070daffd38a`), received 2026-11-06, carrying forward RECON-04/05 unchanged except for the explicit N2 note-policy decisions below. Earlier `PHASE0-PHASE3-REVIEW-AND-TRACKING-DOCS-01` / `VIS-TRANSFER-OPEN-DECISION-PROPOSALS-01` material and the RECON-05 proposal sections below are historical. RECON-06 does not select a physical field/table or retention policy, authorize DDL, permissions, runtime enablement or gate closure. Only this Markdown deliverable is in scope.

## RECON-06 addendum — 2026-11-06 (current approved N2 policy)

**Authority:** Owner-confirmed `VIS-TRANSFER-OWNER-DECISION-RECON-06`, contract id `13d19fa9-eaed-404e-bdfc-4070daffd38a`. This supplement controls over the RECON-05 proposal snapshot below for N2 only and carries RECON-04/05 forward unchanged.

### N2 — Owner-approved optional transfer note

- The note is optional for every approved reason. Missing/null remains absent; do not add a default or concatenate the fixed business reason label. Keep the fixed business reason separate from the note, `KiemToanLyDoMa`/authorization audit reason fields (including `NhatKyHeThong.maLyDo`), `lyDoKham`, and `lyDoChuyenTuyen`.
- At the future Application boundary, map TAB (U+0009), LF (U+000A), CR (U+000D), SPACE (U+0020), NBSP (U+00A0), and NARROW NBSP (U+202F) to ordinary space; collapse runs and trim. A blank result is absent. Reject other separators/whitespace.
- Normalize non-empty text to Unicode NFC. Permit Unicode letters (L), combining marks (M), decimal digits (Nd), punctuation (P), symbols (S), and ordinary space. Reject other controls, format characters (including bidi controls/overrides and ZWJ), unpaired surrogates, private-use and unassigned characters. Pin Unicode category/version behavior in the implementation contract.
- Maximum 255 Unicode code points after normalization. Reject excess; never truncate. The UI counter uses the same unit and stage, and persistence must round-trip permitted text without loss.
- Label the field exactly **“Ghi chú vận hành (không bắt buộc)”** and warn: **“Không nhập thông tin định danh, liên hệ, chẩn đoán hoặc kết quả khám/xét nghiệm.”** Do not prefill patient or clinical information. Show normalized text before submit. Rejected note content must not appear in logs.
- This policy minimizes PHI risk; it cannot guarantee anonymity or prevent PHI from being entered.

The physical note field/table, storage placement and retention remain open. N2 is design authority only: it does not authorize DDL, migrations, runtime behavior, permissions or grants. Broader idempotency canonical-command fields and their normalization are not settled by this note-policy approval. SEC-01 remains **OPEN** and transfer remains **default-deny**.

## Historical RECON-05 follow-up addendum — 2026-11-06 (superseded for N2)

This section records the pre-RECON-06 reading only. Its statements that note policy remained unapproved are historical; the RECON-06 addendum above is current for N2. RECON-04/05 rules outside that supplement remain unchanged.

**Approved facts, not proposals:** RECON-05 resolves the six-code role matrix in section 3, requires dedicated immutable transfer history **plus** successful security audit in the mutation/allocation DB transaction, and fixes queue allocation and replay outcomes summarized in section 1. RECON-04's mandatory fixed business reason and **optional** short note remain unchanged. Business reason and note must never enter `KiemToanLyDoMa` or auth/authorization `NhatKyHeThong.maLyDo`; keep the fixed code separate from free text. No code or note expands actor/status/scope eligibility, destination reads, linked-work changes or billing authority.

**Remaining proposal:** Sections 4–7 recommend a bounded, single-line, optional operational note and compare a more restrictive alternative. Length, whitespace, allowed characters, privacy handling, UI/log behavior, exact logical/physical storage field and retention boundaries all still need separate Owner approval. Neither the recommendation nor this dated reconciliation constitutes acceptance. SEC-01 stays **OPEN** and runtime transfer stays **default-deny** until the separately required guards, persistence, audit, concurrency, rollback, security and workflow acceptance pass.

**Supersession map:** Section 3 Options 1 and 3 and the former taxonomy/role choices D1/D2 are resolved historical material, not current alternatives. Former D4's history-vs-audit-only choice is superseded by required history **and** audit; exact fields/persistence/retention are still open. Code-only would contradict the approved optional note and is not an option here. The former 255-character recommendation was incomplete; section 4 now supplies a concrete proposal, not a binding normalization/canonicalization contract.

---

## 1. Fixed decisions and reconciliation

The task-supplied contracts `VIS-TRANSFER-OWNER-DECISION-RECON-04` and `VIS-TRANSFER-OWNER-DECISION-RECON-05` are the binding authority for the following owner-approved transfer rules; this proposal takes them as settled invariants and does not reopen them:

| Owner-approved rule | Reason and note policy consequence |
|---|---|
| Transfer reason is mandatory as a fixed reason code; a short note is optional. | Every transfer command must supply a valid fixed business reason code from the approved six-code role matrix in section 3. An empty, null, or unlisted reason code must fail closed; illustrative error names `TRANSFER_REASON_REQUIRED` / `INVALID_TRANSFER_REASON_CODE` are proposals, not an approved API. A short note is strictly optional. |
| Clinical reason-for-examination fields (`lyDoKham`) must not be reused. | Transfer is an administrative/operational queue routing movement between clinic rooms, whereas `lyDoKham` is the clinical reason-for-examination field ([`LuotKham.java:41`][visit-domain]). `lyDoChuyenTuyen` is a separate clinical referral field in the end-of-examination section ([`LuotKham.java:66-71`][visit-domain]). Neither clinical field may be overwritten, appended to, or reused for queue transfer. |
| Preserve visit status, `ngayKham`, and examination timestamps during transfer. | Preserved visit status (`CHO_KHAM`, `DANG_KHAM`, or `CHO_CLS`), clinical visit date `ngayKham`, and existing examination timestamps (`thoiGianBatDauKham`, `thoiGianKetThucKham`) remain intact during transfer. Queue allocation and transfer logging must not alter the clinical visit timeline. |
| Destination queue date is a separate `queueDate` in `Asia/Ho_Chi_Minh`, distinct from `ngayKham`. | Share one `(roomId, queueDate)` room/day number stream between registration and transfer. Never reuse a committed number; visible gaps are allowed. Capture one SQL Server UTC instant at transaction start and derive `queueDate` in `Asia/Ho_Chi_Minh`. Never mutate clinical `ngayKham`. Schema, allocator implementation, cutover and legacy-data behavior remain open. |
| RECEPTIONIST transfers only `CHO_KHAM`; DOCTOR transfers only `CHO_KHAM`, `DANG_KHAM`, `CHO_CLS`. Other roles and final/completed, cancelled, soft-deleted, or EMR-locked visits are denied. | Persisted state must satisfy the approved eligibility rules. The six-code role restrictions are approved; a code never expands actor/status eligibility. |
| An active destination room belonging to the destination department is required. RECEPTIONIST may transfer between departments within the same facility; DOCTOR stays in the session-selected department. Transfer grants no destination patient/history/queue reads. | All approved codes remain bounded by these scope rules, including codes whose labels mention specialty or patient preference. |
| Allocate a new destination queue number, preserve existing priority, and never auto-promote. | A transfer reason cannot grant a priority change or authorize a queue-number reuse policy. |
| Preserve visit-linked service orders, prescriptions, results, invoices, and financial records. Do not automatically cancel or mutate them, alter billing, or create duplicate charges. | Reason codes must never imply automated billing recalculation, line-item voiding, or prescription cancellation. Any financial or clinical adjustment belongs to a separate authorized workflow. |
| Pending work follows only when safe with performing unit and responsibility intact; otherwise require completion/handoff or deny. Issued prescriptions, confirmed results and posted financial transactions are immutable. | A reason code cannot substitute for eligibility evidence or authorize linked-work adjustments. Transaction consistency, optimistic concurrency, domain invariants and exception-path tests remain required. |
| Dedicated immutable transfer history plus successful security audit are required in the same DB transaction as mutation/allocation. | Required history and audit writes fail/roll back together. Exact storage fields remain open. A future action may be `VISIT_TRANSFERRED`; an enum addition alone does not wire or emit an event. |
| Idempotent committed replay requires reauthorization. | Same key + same canonical payload returns the stored receipt without repeated side effects, allocation, history or success audit. Same key + different payload conflicts without writes. After timeout, retry with the same key and determine commit vs rollback before proceeding; timeout alone never proves rollback. Key scope/lifetime, canonical fields/normalization, receipt, wait/indeterminate semantics and reservation of failed/denied attempts remain open. |
| Technical/audit metadata uses UTC; clinic business time uses `Asia/Ho_Chi_Minh`. | Transfer event metadata records UTC timestamps, while effective operational day tracking uses `Asia/Ho_Chi_Minh`; RECON-05 fixes the transaction-start SQL Server UTC instant and local-date derivation; persistence details remain unresolved. |

Provenance: task-supplied approved `VIS-TRANSFER-OWNER-DECISION-RECON-05`, 2026-11-06, retaining RECON-04; cross-reference [`phase3-visit-transfer-contract-review.md:10-16`][contract-review] and [`phase3-visit-transfer-decision-gaps.md:83-90`][gap-memo].

---

## 2. Inspected implementation and current limits of the evidence

The following implementation evidence and citations are retained from the earlier review, not newly verified source facts in this follow-up. Only the allowlisted proposal was opened locally; no Java, SQL or other source document was read. Existing implementation patterns were inspected through this retained evidence map and task-supplied retrieval excerpts. Source line anchors and absence findings below are historical; they do not prove current runtime behavior. External string-unit references are retained for context, not newly retrieved.

| Area | Inspected source facts | Consequence / gap |
|---|---|---|
| Clinical complaint fields | [`LuotKham.java:41,71`][visit-domain] defines separate `lyDoKham` and `lyDoChuyenTuyen` string fields. | These clinical fields are distinct from an internal transfer reason. Reusing them would conflict with the approved transfer contract; no statutory compliance conclusion is made here. |
| Audit reason codes | [`KiemToanLyDoMa.java:3-13`][audit-reasons] lists 9 codes: `AUTH_INVALID`, `AUTH_THROTTLED`, `AUTH_NO_ROLE`, `AUTH_NO_SCOPE`, `AUTH_FORBIDDEN`, `SCOPE_MISMATCH`, `ACCOUNT_REVOKED`, `EMR_LOCKED`, `REOPEN_CLINICAL_ADDENDUM`. | `KiemToanLyDoMa` is dedicated to authentication, authorization denial, and EMR unlock exceptions. It contains zero codes for queue routing, reception triage, or clinical room transfers. |
| Audit action codes | [`KiemToanHanhDongMa.java:3-54`][audit-actions] defines `VISIT_REGISTERED` and various EMR/auth actions. | There is no `VISIT_TRANSFERRED` action code currently defined. |
| System audit entry | [`HeThongKiemToanNhapLieu.java:6-24`][system-entry] defines structured actor/scope, action/resource, outcome, `KiemToanLyDoMa reasonCode`, UTC time, workstation and correlation fields. | `reasonCode` is typed as the security/EMR audit enum, not a business transfer reason. There is no field for free-text notes, JSON payloads, or before/after transfer details. |
| Audit persistence and sanitization | [`JdbcKiemToanRepository.java:18-27,113-165`][audit-writer] inserts `NULL` for `chiTiet` and `lyDo`, and binds the typed audit `reasonCode().name()` (or null) into `maLyDo`. | This writer excludes free text. Business transfer codes must remain separate from `KiemToanLyDoMa` / `NhatKyHeThong.maLyDo`; the existing audit column supplies no width contract for a separate transfer-reason field. |
| Audit database schema | [`V6__Enhance_Audit_Tables_Append_Only.sql:17-24,69-109`][audit-immutable] defines `maLyDo VARCHAR(100) NULL`, append-only state checks and triggers rejecting `UPDATE` and `DELETE` on both audit tables. | This is evidence of audit-column width and append-only enforcement, not transfer storage approval or a permanent-retention duration. Retention duration remains unresolved and purge remains prohibited ([baseline acceptance:27-28][sec01]). |
| Desktop UI prototype | [`BenhNhanHangDoiNguCanhMenu.java:71-75`][ui-menu], [`KhamBenhManHinh.java:1080-1091`][ui-panel], and [`TransferPhongSelectHopThoai.java:15-34`][ui-dialog] implement a mock popup selecting room labels from a hardcoded array. | The UI prototype has no reason dropdown, no note text area, no validation, and performs no backend call. |

Historical absence finding: no transfer reason enum, validation service, or normalization logic was found in `clinic-domain`, `clinic-application`, or `clinic-infrastructure`.

---

## 3. Approved six-code taxonomy and superseded historical options

RECON-05 approves Option 2 and its permitted roles. Options 1 and 3 and their comparison are retained only as historical rationale; they are not open choices. No taxonomy or role amendment is proposed.

### Historical Option 1 — superseded, not approved (4 codes)

Defines a compact, universal set of 4 codes applicable across all outpatient clinics.

| Reason Code | Display Label (VN) | Operational Scenario | Allowed Actors |
|---|---|---|---|
| `WRONG_ROOM_ALLOCATION` | Tiếp đón xếp nhầm phòng | Patient was assigned to the wrong clinic room during check-in/triage due to administrative error. | `RECEPTIONIST`, `DOCTOR` |
| `SPECIALTY_CONSULTATION` | Khám chuyên khoa phù hợp | Initial evaluation shows patient requires a different clinical specialty or subspecialty room. | `DOCTOR` |
| `OVERLOAD_REBALANCING` | Điều phối giảm tải phòng | Active room queue is excessively long; shifting patient to an equivalent parallel examination room. | `RECEPTIONIST`, `DOCTOR` |
| `PATIENT_REQUEST` | Bệnh nhân yêu cầu đổi phòng | Patient or family requests a change of examining room, doctor, or specialty service. | `RECEPTIONIST`, `DOCTOR` |

- **Tradeoffs:**
  - *Pros:* Minimum complexity; straightforward to implement as a simple Java enum (`TransferLyDoMa`); low cognitive burden on staff; clean, unified management reporting.
  - *Cons:* Lacks granularity; does not distinguish between administrative triage mistakes and clinical equipment dependencies; does not restrict clinical referral decisions exclusively to clinicians.

### Option 2 — APPROVED by RECON-05 (6 codes; resolved)

These role-to-code restrictions are approved within the fixed actor/status/scope boundaries of `DEC-AUTHZ-04b` and RECON-04. Every DOCTOR option remains within the session-selected department; only RECEPTIONIST may change departments within the same facility. Labels and operational examples do not add eligibility rules or broaden scope.

| Reason Code | Display Label (VN) | Operational Definition | Role Eligibility |
|---|---|---|---|
| `RECEPTION_TRIAGE_CORRECTION` | Tiếp đón phân luồng nhầm | Front desk misrouted patient during reception; corrected before consultation begins. | `RECEPTIONIST` only |
| `PATIENT_SPECIALTY_PREFERENCE` | Người bệnh đổi nguyện vọng | Patient requests another room within approved actor/status/scope rules; a department change is eligible only for `RECEPTIONIST` within the same facility. | `RECEPTIONIST`, `DOCTOR` |
| `CLINIC_LOAD_BALANCING` | Điều phối lưu lượng hàng đợi | Balance room queues within the already-approved actor/status/scope rules. | `RECEPTIONIST`, `DOCTOR` |
| `CLINICAL_SPECIALTY_REFERRAL` | Chuyển khám chuyên khoa | Examining doctor requests another suitable specialty room within the session-selected department; deny a destination outside it. | `DOCTOR` only |
| `DIAGNOSTIC_EQUIPMENT_NEED` | Cần trang thiết bị chuyên dụng | Room lacks specific diagnostic instrument (e.g., ENT endoscope, ultrasound machine) available in target room. | `DOCTOR` only |
| `CLINICIAN_EMERGENCY_HANDOFF` | Bác sĩ bàn giao đột xuất | Doctor is called to an emergency, hospital round, or sudden absence; handoff of active queue. | `DOCTOR` only |

- **Tradeoffs:**
  - *Pros:* Distinguishes front-desk triage corrections from in-department clinical handoffs for reporting, while preserving approved transfer boundaries.
  - *Cons:* Slightly larger code set; application validation guard must verify actor role against code eligibility.

### Historical Option 3 — superseded, not approved (configurable catalog)

Proposes 4 top-level system categories in Java (`ADMINISTRATIVE`, `CLINICAL`, `LOAD_MANAGEMENT`, `PATIENT_REQUEST`), with granular sub-reasons in a hypothetical table or configurable catalog (`DanhMucLyDoDieuChuyen`). The name and schema are unapproved.

- **Tradeoffs:**
  - *Pros:* High extensibility; large multi-facility hospital systems can add custom sub-reasons without code changes or migrations.
  - *Cons:* Introduces substantial relational complexity, additional repository queries, caching needs, and catalog governance overhead. At current Phase 3 maturity, this represents speculative over-engineering before basic transfer capability exists.

### Historical taxonomy comparison — decision resolved by RECON-05

| Criteria | Option 1 (4 Core Codes) | Option 2 (Role-Partitioned 6 Codes) | Option 3 (Two-Tier Configurable) |
|---|---|---|---|
| **Simplicity & Implementation Effort** | Very High (trivial enum) | High (single enum + role validator) | Low (requires DB catalog + maintenance UI) |
| **Role-to-code restrictions (historical comparison; Option 2 approved)** | As listed above | Role-eligible validation | Dependent on catalog design |
| **Reporting & KPI Value** | Moderate (broad categories) | High (clear triage vs clinical vs contingency metrics) | Very High (customizable) |
| **Extensibility without Migration** | Low (enum requires code release) | Moderate (stable core set covers operations) | High (runtime configuration) |
| **Business Reason Storage (unresolved)** | Separate business code | Separate business code | Separate category/sub-code representation |

The approved six business codes never map into `KiemToanLyDoMa` or `NhatKyHeThong.maLyDo`. RECON-05 requires history plus security audit; exact fields, correlation and the operational payload design remain open.

---

## 4. Historical optional-note proposal and conservative alternative — superseded by RECON-06

The following preserves the pre-approval proposal snapshot, not current alternatives or implementation authority. N2 is approved by RECON-06 above; N1 is not an available alternative. The note stays optional for every approved code, including clinical handoff codes. Missing, null, empty or normalized-blank input must not require staff to invent text. The fixed business code is mandatory and remains a separate value. No clinical narrative is needed to justify an otherwise eligible transfer.

### Historical Recommendation N2: bounded operational note

Recommend the following minimal deterministic policy for Owner review, applied at the future Application boundary as well as reflected in the UI. These steps describe a candidate **note** normalization policy; they do not settle the idempotency canonical field list or payload normalization.

1. **Optional input:** Missing/null becomes absent. Do not add a default note or concatenate the reason label into it.
2. **Whitespace:** Map only TAB (U+0009), LF (U+000A), CR (U+000D), SPACE (U+0020), NBSP (U+00A0) and NARROW NBSP (U+202F) to ordinary U+0020 spaces. Collapse runs of those spaces and trim both ends. A CR/LF pair therefore becomes one separator, never silent word concatenation. Normalize an empty result to absent. Other whitespace/separators are rejected rather than relying on differing `trim()`, `isBlank()` or regex definitions.
3. **Unicode:** Normalize nonempty text to NFC before counting. Permit Unicode letters (L), combining marks (M), decimal digits (Nd), punctuation (P), symbols (S) and ordinary U+0020 space. Reject all other categories, including controls not mapped in step 2, format characters (including bidi overrides and zero-width joiners), unpaired surrogates, private-use and unassigned characters. Pin the Unicode category/version behavior in the later implementation contract. Simple symbol/emoji code points can pass; sequences containing disallowed format characters cannot. No HTML/Markdown interpretation is authorized.
4. **Length:** Allow at most **255 Unicode code points after normalization**; reject excess with a field-level error, never silently truncate. The UI counter must use the same unit/stage and explain normalization. A code point is not a user-perceived grapheme or a Java UTF-16 code unit. Storage validation must independently guarantee lossless round trips for the permitted repertoire.
5. **Privacy/UI:** Show the fixed code separately and label the note “Ghi chú vận hành (không bắt buộc)”. Display guidance: “Không nhập thông tin định danh, liên hệ, chẩn đoán hoặc kết quả khám/xét nghiệm.” Provide no patient-derived suggestions, clinical prefill or mandatory-note rule. Show normalized text for review before submit; validation errors must not echo the rejected text into logs.

This allows short Vietnamese operational text with diacritics and common punctuation without a complex language-specific whitelist. It does **not** prevent PHI. Recommend guidance and data minimization first; no new regex blocker is recommended until separately specified and reviewed (section 5).

### Historical conservative alternative N1: superseded; not approved

Use the same absent/whitespace/NFC pipeline, but limit to **100 Unicode code points** and allow only Unicode L/M/Nd, U+0020 and ASCII punctuation U+0021–U+007E that falls in category P. Other symbols, emoji and non-ASCII punctuation are rejected with an actionable inline message. Vietnamese letters/combining marks remain supported. This stricter repertoire reduces accidental formatting and free-text volume; it still cannot establish anonymity or prohibit diagnosis text automatically.

| Historical comparison only | N2 — proposal snapshot | N1 — proposal snapshot |
|---|---|---|
| UX | 255-point counter; ordinary Unicode punctuation/symbols accepted; pasted mapped whitespace becomes one line. Unsupported controls/format characters produce a field error. | 100-point counter; staff may need to shorten a note or replace typographic punctuation/symbols. Do not force a note when it is omitted. |
| Storage capacity, not schema selection | Up to 510 UTF-16 code units for 255 supplementary code points; capacity/encoding/collation must be reviewed independently. | Upper bound of 200 UTF-16 code units for 100 permitted code points if any are supplementary; a 100-point limit is not automatically a 100-unit storage capacity. |
| Test consequences | Normalized lengths 254/255/256; supplementary symbols accepted, zero-width-joiner sequences rejected; Vietnamese NFC equivalence and mapped whitespace. | Normalized lengths 99/100/101; Vietnamese accepted, typographic punctuation and symbols rejected; same blank/whitespace cases. |
| Privacy/retention | More free-text capacity increases potential sensitive content; requires the same access/log/retention decisions as N1. | Less text and a narrower repertoire reduce volume, but names/clinical information can still fit. No weaker privacy controls are justified. |

The limits do not derive from audit `maLyDo` width. Java UTF-16 and code-point units differ ([retained Java reference][java-string]); SQL Server Unicode storage capacity also has distinct units ([retained SQL Server reference][sql-nvarchar]). **No column, SQL type, width, collation, table or DDL is selected.** Removing notes entirely would amend RECON-04/05 and is not an alternative in this proposal.

## 5. Historical privacy-risk proposals and UI/log/storage boundaries

These are retained recommendations from the pre-RECON-06 proposal, not additional approved requirements or current open policy choices. They are not a legal compliance claim or a PHI-detection guarantee.

| Risk | Proposed boundary / handling | Remaining Owner choice |
|---|---|---|
| Staff can enter identifying or clinical narrative despite a short limit. | Keep notes operational; no names, identifiers, contact information, diagnoses, results or copied chart text. Use guidance and staff workflow review. Synthetic examples only, e.g. `Phong du phong san sang`. | Approve wording, training/escalation process and handling of an accidentally submitted sensitive note. |
| Numeric heuristics can reject harmless operational text or miss sensitive content. | Do not infer safety from a non-match. Names, clinical narrative and obfuscated identifiers evade simple regexes; numeric matches can be false positives. Any later advisory or rejection rule needs exact matching/normalization, synthetic cases and separate approval. | Guidance only (recommended initially), advisory detection, or specified rejection. No heuristic is approved here. |
| UI, debug output, exceptions or telemetry can duplicate text. | Treat the note as plain text; no rich rendering. No note/body in application/security logs, analytics, crash reports, validation messages, audit `chiTiet`/`lyDo`, or clipboard/export automation. Do not log request bodies. Render only in an authorized transfer view if later approved; no new history-reader grant or destination read follows from this document. | Exact readers, display locations, masking, exports, draft lifetime and error/retry behavior. |
| Mandatory business code can be confused with a security reason or concatenated note. | Keep the approved code separate in command/history design. Neither code nor note belongs in `KiemToanLyDoMa` or auth/authorization `NhatKyHeThong.maLyDo`. Security audit stays structured and free of note text; use only separately approved correlation metadata. | Exact payload/field mapping and receipt exposure; no business-to-auth reason mapping. |
| Notes may be copied into receipts, replay storage or backups beyond their intended lifetime. | Recommend one authoritative note value associated with the required immutable operational history, not extra copies in receipts/logs. RECON-05 already mandates history **plus** security audit in the same transaction; required writes fail/roll back together. Do not silently drop a submitted note. | Persist the normalized note with history (recommended) or explicitly approve transient handling and communicate it before submission; exact logical field, physical mapping, null/absence representation, backup/replica/export boundaries remain unselected. |
| Immutability can conflict with correction, minimization or eventual disposal. | Do not mutate immutable transfer history or purge audit records as a side effect of this proposal. Do not infer permanent retention from append-only enforcement. Sensitive-note remediation needs a separately approved process compatible with immutability. | Retention owner, purpose, duration/start event, legal-hold handling, expiry/disposal, backup retention and any separately authorized remediation mechanism. No duration, deletion job or retention change is approved. |

The exact storage field is an explicit unresolved boundary: decide the logical owner and mapping separately from the fixed business code, and exclude clinical `lyDoKham` / `lyDoChuyenTuyen` and security reason fields. The historical name in the persistence proposal is not a table/column selection. Audit-only history is superseded and cannot satisfy RECON-05. Until field, access and retention choices are approved, there is no storage or runtime implementation authorization.

## 6. Historical migration and focused-testing considerations — future work only

No code, test fixture, migration, grant or runtime gate is changed here. These are proposed acceptance cases for a later authorized implementation, using synthetic data only:

- Verify the exact six-code role matrix independently of note validity. Omitted/null/empty/mapped-blank notes succeed only when every separately approved eligibility/authorization condition passes; DOCTOR stays in the selected department and RECEPTIONIST department transfers stay within the same facility. Reason/note never overrides status, room, linked-work or default-deny guards.
- Verify the selected normalization order, exact whitespace set, CR/LF word separation, NFC-equivalent Vietnamese inputs, supplementary characters, forbidden controls/bidi/format characters, isolated surrogates, all length boundaries and lossless storage round trips. UI and Application validation agree; rejection never truncates or logs text. Freeze Unicode behavior for consistent upgrades.
- Storage/cutover design must first choose logical/physical fields, Unicode capacity, absent representation, access and retention. Do not backfill fabricated notes or infer historical transfers from clinical fields. Existing visits/records without notes remain compatible. Legacy-data behavior, migration validation and rollback require separate approval; this proposal chooses none of them.
- Verify no raw note/business reason reaches security reason fields, free-text audit details, telemetry, exceptions or unauthorized views. Verify retention/copy boundaries only after approved, including backups/replicas/exports and receipt exposure. Guidance and numeric heuristics, if later chosen, need synthetic false-positive/negative cases.
- For future atomicity tests, failures in required history, success audit, allocation or mutation roll back together. Same key + same canonical payload after commit returns the stored receipt after reauthorization, without duplicate effects; different payload conflicts without writes. Resolve whether normalized note/absent equivalence participates in the canonical payload before implementing these tests. Do not infer replay equivalence from this recommendation. Test timeout recovery with the same key without assuming rollback.

Queue allocation, history persistence, idempotency design, linked-work evidence and transfer-specific security acceptance remain separate workstreams. RECON-05's shared room/day stream, non-reuse of committed numbers, visible gaps and transaction-start SQL UTC sampling remain fixed. Preserve status, `ngayKham`, examination timestamps, priority and linked records; no financial changes. No workflow acceptance is claimed by this test plan.

## 7. Historical Owner decision checklist — superseded by RECON-06

This checklist is a preserved pre-RECON-06 snapshot; its open N2 choices have since been resolved as stated above. Do not treat it as the current decision ledger. The exact six-code matrix remains approved, and mandatory immutable history **plus** successful security audit remains unchanged.

- [ ] **Length/repertoire:** Choose proposed N2 (255 normalized code points, L/M/Nd/P/S + space), conservative N1 (100, L/M/Nd + ASCII punctuation + space), or specify another bounded optional-note policy. Approve count unit, Unicode behavior, error handling and no truncation.
- [ ] **Normalization:** Approve or revise the exact whitespace mapping/collapse/trim set and NFC order, blank-to-absent handling and UI preview. Separately settle whether/how note participates in idempotency canonicalization; this checklist does not settle the other key/receipt/wait decisions.
- [ ] **Privacy/UI/logs:** Approve guidance, guidance-only vs specified detection, authorized displays/readers, draft/error handling and exclusion from logs/audit free text. No reader grant or destination read is implied.
- [ ] **Storage/retention:** Choose persisted normalized note vs explicitly disclosed transient handling; separately approve its exact logical/physical field, capacity/encoding, access, retention/start/hold/disposal and copy/backup/remediation boundaries. No database column/schema or duration has been selected.
- [ ] **Separate approval and acceptance:** Record a distinct Owner approval of the note policy and a later implementation/workflow acceptance with required security, atomicity, rollback, concurrency and migration evidence. SEC-01 remains OPEN and transfer runtime default-deny meanwhile; no task-board/gate status is changed here.

---

## 8. Validation and guidance record

### RECON-06 supplement — 2026-11-06

- Read the task-supplied mandatory guidance: Orchestrator baseline `docs/PROJECT_CONVENTIONS.md`, `README.md`, and supplied excerpts of `DOMAIN_ANALYSIS.md` and `ERD_DIAGRAM.md`. Read supplied retrieved excerpts of `phase3-transfer-linked-work-eligibility-review.md`, `phase3-transfer-reason-policy-proposal.md`, `phase3-visit-lifecycle-owner-decisions.md`, `phase3-visit-transfer-decision-gaps.md`, and `phase3-visit-transfer-contract-review.md` under `docs/ke-hoach-trien-khai/`. Read Owner-approved `VIS-TRANSFER-OWNER-DECISION-RECON-06`, retaining RECON-04/05.
- **RAG first:** Attempted the project RAG search before local repository reads: `python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py --repo clinic-desktop --limit 8 'VIS-TRANSFER-OWNER-DECISION-RECON-06 N2 optional transfer note normalization Unicode categories UI privacy RECON-05 transfer policy'`. The tool reported that permission could not be obtained in non-interactive mode; no new excerpts were returned. Used the task-supplied approved contract and retrieved excerpts as authority; no index/cache workaround or database access.
- Inspected existing implementation within the read-only scope: `TransferLyDoMa` and its test establish the six-code taxonomy/role matrix; `KiemToanHanhDongMa` contains `VISIT_TRANSFERRED`. A bounded Java search found no N2 note normalization, note UI, logging or storage implementation. This documentation task does not authorize implementation.
- **Validation:** `git diff --check` and read-only document/scope assertions are recorded after edits below. No application tests are appropriate for this documentation-only change.
- **Acceptance:** N2 policy is marked Owner-approved without selecting storage or retention; superseded proposal text is explicitly historical; RECON-04/05 boundaries, SEC-01 OPEN and default-deny are retained. The live RAG retrieval remains blocked as above.

### Historical RECON-05 follow-up — 2026-11-06 (superseded for N2)

- Read task-supplied Mandatory guidance before editing: Orchestrator baseline `docs/PROJECT_CONVENTIONS.md`, `README.md`, supplied excerpts of `DOMAIN_ANALYSIS.md` and `ERD_DIAGRAM.md`; supplied retrieved excerpts of `phase3-visit-transfer-contract-review.md`, `phase3-transfer-linked-work-eligibility-review.md`, `phase3-visit-lifecycle-owner-decisions.md` and `phase3-visit-transfer-decision-gaps.md` (under `docs/ke-hoach-trien-khai/`). Read task-supplied approved RECON-05 contract, retaining RECON-04. Full truncated guidance was not opened outside the allowlist.
- **RAG first:** Before any local repository read/search, ran `/Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py --repo clinic-desktop --limit 5 'VIS-TRANSFER-OWNER-DECISION-RECON-05 optional transfer note reason taxonomy normalization privacy KiemToanLyDoMa maLyDo'`. Exit 1: `sqlite3.OperationalError: unable to open database file` at `PRAGMA journal_mode=WAL`; no new excerpts returned. Used supplied retrieval/contract and the allowlisted proposal as fallback. No index/cache workaround or database access. RAG-first ordering satisfied; live retrieval blocked.
- Local document read scope: only this proposal, including its historical implementation evidence map. `rg --files -g AGENTS.md -g '!target' -g '!node_modules'` returned exit 1/no matches in the worktree; no additional guidance file was opened. Historical source/link checks below were not rerun or claimed as current verification.
- Validation: `git diff --check` passed (exit 0, no output). Read-only inline `python3` document assertions passed (exit 0): exact single-file tracked/untracked scope, dated addendum and supersession, exact six-code role matrix, RECON-04/05 boundaries, note optionality/recommendation/alternative, five unchecked Owner decisions, and Markdown reference definitions. Source targets/line anchors were not reopened or revalidated. No application tests run for this document-only change; future synthetic acceptance cases in section 6 remain unexecuted.
- Acceptance: **PASS** allowlisted-only change plus dated addendum/supersession; **PASS** approved code/role and scope preservation with note policy unapproved; **PASS** privacy/recommendation/conservative alternative/checklist with no schema selection or runtime enablement; **PASS** RAG-first ordering and clean diff check. **Blocker:** live RAG retrieval failed as above; fallback evidence is limited to supplied excerpts, contract and retained proposal context. Separate note-policy approval and workflow acceptance are still required; SEC-01 remains OPEN/default-deny.

### Historical guidance and validation record — superseded execution snapshot

The remaining record describes the earlier review only. Its file reads, source inspections, commands and PASS claims are not current follow-up execution evidence.

### Historical guidance files read before editing

- Orchestrator-supplied baseline `docs/PROJECT_CONVENTIONS.md` (repository-local file absent).
- Repository `README.md`.
- Relevant sections of `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md` and `UI_CODING_STANDARDS.md` (architecture, visit/clinical fields, audit model/schema and UI boundaries).
- `docs/ke-hoach-trien-khai/README.md` (roadmap status conventions).
- Task-supplied approved contract `PHASE0-PHASE3-REVIEW-AND-TRACKING-DOCS-01`; current Owner decisions `VIS-TRANSFER-OWNER-DECISION-RECON-04` verified in the reconciliation documents below.
- `docs/ke-hoach-trien-khai/phase3-visit-transfer-contract-review.md` (Workflow contract review).
- `docs/ke-hoach-trien-khai/phase3-visit-transfer-decision-gaps.md` (Decision gap analysis memo).
- `docs/ke-hoach-trien-khai/phase3-transfer-queue-history-persistence-proposal.md` (Queue allocation and persistence proposal).
- `docs/ke-hoach-trien-khai/phase3-transfer-linked-work-eligibility-review.md` (Linked-work eligibility review).
- `docs/ke-hoach-trien-khai/phase3-visit-lifecycle-owner-decisions.md` (Visit lifecycle owner decisions).
- `docs/ke-hoach-trien-khai/phase2-role-permission-matrix-approval.md` (`DEC-AUTHZ-04b`).
- `docs/ke-hoach-trien-khai/phase2-implementation-contract.md` (`DEC-TIME-01`, `SEC-01`).
- `docs/ke-hoach-trien-khai/phase2-sec01-baseline-acceptance.md` (OPEN visual acceptance, future-workflow denial, audit-reader denial and unresolved retention).
- Task-supplied retrieved excerpts from `docs/ke-hoach-trien-khai/phase3-transfer-linked-work-eligibility-review.md`, `phase3-transfer-reason-policy-proposal.md`, `phase3-transfer-domain-invariant-design.md`, `phase3-transfer-queue-allocation-proposal.md`, `phase1-doctor-history-ui-independent-review.md` and `phase3-transfer-queue-history-persistence-proposal.md` (all under `docs/ke-hoach-trien-khai/`). These supplied excerpts were read as excerpts, not treated as current execution results.

Guidance inventory: no applicable `AGENTS.md` found at the target's checkout ancestors or checked filesystem ancestors. Repository-local `docs/PROJECT_CONVENTIONS.md`, `docs/DEVELOPMENT_CONVENTIONS.md`, `CONTRIBUTING.md`, `docs/README.md` and `ARCHITECTURE.md` are absent. The supplied baseline and available repository guidance above were used. Source citations below were inspected directly; the two external primary references establish only Java/SQL string units, not an implementation or legal contract.

### Historical RAG and environment execution record

- **Earlier review snapshot, 2026-11-06 — RAG first:** Before opening repository documents or running `rg`/`grep`, executed `/Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py --repo clinic-desktop --limit 5 'điều chuyển lý do nghiệp vụ ghi chú KiemToanLyDoMa NhatKyHeThong maLyDo Unicode UTF-16 NVARCHAR VIS-TRANSFER-OWNER-DECISION-RECON-04'`.
  - *Result:* Exit 1, `sqlite3.OperationalError: unable to open database file`, during Orchestrator configuration/index connection setup at `PRAGMA journal_mode=WAL`; no search excerpts returned.
  - *Primary guidance:* Used the task-supplied retrieved excerpts, then checked current Owner-decision and source evidence directly. No index/cache workaround, permission escalation, clinic DB connection or patient data access.
- **Document checks:** `git diff --check` passes (exit 0, no output). Read-only Python checks verify reference definitions, local citation target existence/line bounds, required correction coverage and exact single-file scope. Citation claims were also reviewed against the cited source; these checks do not establish runtime acceptance.
- **Scope compliance:** Only `docs/ke-hoach-trien-khai/phase3-transfer-reason-policy-proposal.md` is modified. No source, tests, schema, permission, audit/runtime configuration or gate changes. `run-app.sh` was absent in this checkout and was not created or otherwise touched. No build/application tests run: documentation-only scope. No push, PR or merge.
- **Acceptance:** PASS for allowlisted scope, business/security reason separation, removal of unsupported legal claims, explicit unresolved length/storage mapping, heuristic regex limitations, fixed Owner-policy preservation and non-binding storage options. SEC-01 and downstream gates remain OPEN/default-deny. RAG execution was attempted first; retrieval remains blocked as recorded above.

---

[sec01]: phase2-sec01-baseline-acceptance.md#L4-L32
[contract-review]: phase3-visit-transfer-contract-review.md
[gap-memo]: phase3-visit-transfer-decision-gaps.md
[persistence-proposal]: phase3-transfer-queue-history-persistence-proposal.md#L59-L65
[visit-domain]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L41-L71
[audit-reasons]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditReasonCode.java#L3-L13
[audit-actions]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditActionCode.java#L3-L54
[system-entry]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/SystemAuditEntry.java#L6-L24
[audit-writer]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/system/JdbcKiemToanRepository.java#L18-L165
[audit-immutable]: ../../clinic-infrastructure/src/main/resources/migrations/V6__Enhance_Audit_Tables_Append_Only.sql#L17-L109
[ui-menu]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/menus/PatientQueueContextMenu.java#L71-L75
[ui-panel]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/ExaminationPanel.java#L1080-L1091
[ui-dialog]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/dialogs/TransferRoomSelectDialog.java#L15-L34
[java-string]: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html
[sql-nvarchar]: https://learn.microsoft.com/en-us/sql/t-sql/data-types/nchar-and-nvarchar-transact-sql?view=sql-server-ver17
