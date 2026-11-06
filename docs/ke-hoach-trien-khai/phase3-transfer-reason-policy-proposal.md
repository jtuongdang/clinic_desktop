# VIS-TRANSFER-REASON-POLICY-01 — Transfer reason code taxonomy, note normalization, and privacy policy proposal

**Status:** Source-grounded proposal only, reconciled under approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-04` (superseding `VIS-TRANSFER-OWNER-DECISION-RECON-03` and `VIS-TRANSFER-SOURCE-RECON-01`). Snapshot: 2026-11-06. No Java, tests, migration, permission, policy or database changes. SEC-01 and all downstream gates remain OPEN; transfer remains default-deny pending its separately approved implementation and acceptance. SEC-01 Definition A does not itself accept future workflows ([baseline acceptance:4–9,23–32][sec01]).

**Contract:** This correction is authorized by task-supplied approved contract v1 `PHASE0-PHASE3-REVIEW-AND-TRACKING-DOCS-01`. The original proposal records `VIS-TRANSFER-OPEN-DECISION-PROPOSALS-01`; fixed transfer policy is reconciled under `VIS-TRANSFER-OWNER-DECISION-RECON-04`. The recommendations below are proposed technical policy choices for Owner review, not additional owner approvals. This document is explicitly non-binding and does not authorize a table, schema, audit mapping, grant or runtime workflow. Only this Markdown deliverable is in scope.

---

## 1. Fixed decisions and reconciliation

The task-supplied contract `VIS-TRANSFER-OWNER-DECISION-RECON-04` is the binding authority for the following owner-approved transfer rules; this proposal takes them as settled invariants and does not reopen them:

| Owner-approved rule | Reason and note policy consequence |
|---|---|
| Transfer reason is mandatory as a fixed reason code; a short note is optional. | Every transfer command must supply a valid fixed business reason code from a taxonomy still awaiting approval. An empty, null, or unlisted reason code must fail closed; illustrative error names `TRANSFER_REASON_REQUIRED` / `INVALID_TRANSFER_REASON_CODE` are proposals, not an approved API. A short note is strictly optional. |
| Clinical reason-for-examination fields (`lyDoKham`) must not be reused. | Transfer is an administrative/operational queue routing movement between clinic rooms, whereas `lyDoKham` is the clinical reason-for-examination field ([`LuotKham.java:41`][visit-domain]). `lyDoChuyenTuyen` is a separate clinical referral field in the end-of-examination section ([`LuotKham.java:66-71`][visit-domain]). Neither clinical field may be overwritten, appended to, or reused for queue transfer. |
| Preserve visit status, `ngayKham`, and examination timestamps during transfer. | Preserved visit status (`CHO_KHAM`, `DANG_KHAM`, or `CHO_CLS`), clinical visit date `ngayKham`, and existing examination timestamps (`thoiGianBatDauKham`, `thoiGianKetThucKham`) remain intact during transfer. Queue allocation and transfer logging must not alter the clinical visit timeline. |
| Destination queue date is a separate `queueDate` in `Asia/Ho_Chi_Minh`, distinct from `ngayKham`. | Allocate destination queue using the transfer date as a separate `queueDate` in `Asia/Ho_Chi_Minh`. Never mutate clinical `ngayKham`. |
| RECEPTIONIST transfers only `CHO_KHAM`; DOCTOR transfers only `CHO_KHAM`, `DANG_KHAM`, `CHO_CLS`. Other roles and final/completed, cancelled, soft-deleted, or EMR-locked visits are denied. | Persisted state must satisfy the approved eligibility rules. Additional role-to-reason restrictions remain proposals; a code never expands actor/status eligibility. |
| An active destination room belonging to the destination department is required. RECEPTIONIST may transfer between departments within the same facility; DOCTOR stays in the session-selected department. Transfer grants no destination patient/history/queue reads. | All taxonomy options remain bounded by these scope rules, including codes whose labels mention specialty or patient preference. |
| Allocate a new destination queue number, preserve existing priority, and never auto-promote. | A transfer reason cannot grant a priority change or authorize a queue-number reuse policy. |
| Preserve visit-linked service orders, prescriptions, results, invoices, and financial records. Do not automatically cancel or mutate them, alter billing, or create duplicate charges. | Reason codes must never imply automated billing recalculation, line-item voiding, or prescription cancellation. Any financial or clinical adjustment belongs to a separate authorized workflow. |
| Pending work follows only when safe with performing unit and responsibility intact; otherwise require completion/handoff or deny. Issued prescriptions, confirmed results and posted financial transactions are immutable. | A reason code cannot substitute for eligibility evidence or authorize linked-work adjustments. Transaction consistency, optimistic concurrency, domain invariants and exception-path tests remain required. |
| Technical/audit metadata uses UTC; clinic business time uses `Asia/Ho_Chi_Minh`. | Transfer event metadata records UTC timestamps, while effective operational day tracking uses `Asia/Ho_Chi_Minh`; clock and transaction sampling details remain unresolved. |

Provenance: approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-04`, 2026-11-06; cross-reference [`phase3-visit-transfer-contract-review.md:10-16`][contract-review] and [`phase3-visit-transfer-decision-gaps.md:83-90`][gap-memo].

---

## 2. Inspected implementation and current limits of the evidence

Repository citation paths resolve relative to this document; their line anchors were checked against this checkout. External primary references for string units are identified separately in section 4.

| Area | Inspected source facts | Consequence / gap |
|---|---|---|
| Clinical complaint fields | [`LuotKham.java:41,71`][visit-domain] defines separate `lyDoKham` and `lyDoChuyenTuyen` string fields. | These clinical fields are distinct from an internal transfer reason. Reusing them would conflict with the approved transfer contract; no statutory compliance conclusion is made here. |
| Audit reason codes | [`AuditReasonCode.java:3-13`][audit-reasons] lists 9 codes: `AUTH_INVALID`, `AUTH_THROTTLED`, `AUTH_NO_ROLE`, `AUTH_NO_SCOPE`, `AUTH_FORBIDDEN`, `SCOPE_MISMATCH`, `ACCOUNT_REVOKED`, `EMR_LOCKED`, `REOPEN_CLINICAL_ADDENDUM`. | `AuditReasonCode` is dedicated to authentication, authorization denial, and EMR unlock exceptions. It contains zero codes for queue routing, reception triage, or clinical room transfers. |
| Audit action codes | [`AuditActionCode.java:3-54`][audit-actions] defines `VISIT_REGISTERED` and various EMR/auth actions. | There is no `VISIT_TRANSFERRED` action code currently defined. |
| System audit entry | [`SystemAuditEntry.java:6-24`][system-entry] defines structured actor/scope, action/resource, outcome, `AuditReasonCode reasonCode`, UTC time, workstation and correlation fields. | `reasonCode` is typed as the security/EMR audit enum, not a business transfer reason. There is no field for free-text notes, JSON payloads, or before/after transfer details. |
| Audit persistence and sanitization | [`JdbcAuditRepositoryImpl.java:18-27,113-165`][audit-writer] inserts `NULL` for `chiTiet` and `lyDo`, and binds the typed audit `reasonCode().name()` (or null) into `maLyDo`. | This writer excludes free text. Business transfer codes must remain separate from `AuditReasonCode` / `NhatKyHeThong.maLyDo`; the existing audit column supplies no width contract for a separate transfer-reason field. |
| Audit database schema | [`V6__Enhance_Audit_Tables_Append_Only.sql:17-24,69-109`][audit-immutable] defines `maLyDo VARCHAR(100) NULL`, append-only state checks and triggers rejecting `UPDATE` and `DELETE` on both audit tables. | This is evidence of audit-column width and append-only enforcement, not transfer storage approval or a permanent-retention duration. Retention duration remains unresolved and purge remains prohibited ([baseline acceptance:27-28][sec01]). |
| Desktop UI prototype | [`PatientQueueContextMenu.java:71-75`][ui-menu], [`ExaminationPanel.java:1080-1091`][ui-panel], and [`TransferRoomSelectDialog.java:15-34`][ui-dialog] implement a mock popup selecting room labels from a hardcoded array. | The UI prototype has no reason dropdown, no note text area, no validation, and performs no backend call. |

Absence finding: no transfer reason enum, validation service, or normalization logic currently exists in `clinic-domain`, `clinic-application`, or `clinic-infrastructure`.

---

## 3. Transfer reason code taxonomy options

The approved contract requires a **fixed reason code**. Below are three practical taxonomy options evaluated against clinic operations, role governance, reporting utility, and extensibility.

### Option 1: Minimal Coarse-Grained Operational Taxonomy (4 Core Codes)

Defines a compact, universal set of 4 codes applicable across all outpatient clinics.

| Reason Code | Display Label (VN) | Operational Scenario | Allowed Actors |
|---|---|---|---|
| `WRONG_ROOM_ALLOCATION` | Tiếp đón xếp nhầm phòng | Patient was assigned to the wrong clinic room during check-in/triage due to administrative error. | `RECEPTIONIST`, `DOCTOR` |
| `SPECIALTY_CONSULTATION` | Khám chuyên khoa phù hợp | Initial evaluation shows patient requires a different clinical specialty or subspecialty room. | `DOCTOR` |
| `OVERLOAD_REBALANCING` | Điều phối giảm tải phòng | Active room queue is excessively long; shifting patient to an equivalent parallel examination room. | `RECEPTIONIST`, `DOCTOR` |
| `PATIENT_REQUEST` | Bệnh nhân yêu cầu đổi phòng | Patient or family requests a change of examining room, doctor, or specialty service. | `RECEPTIONIST`, `DOCTOR` |

- **Tradeoffs:**
  - *Pros:* Minimum complexity; straightforward to implement as a simple Java enum (`TransferReasonCode`); low cognitive burden on staff; clean, unified management reporting.
  - *Cons:* Lacks granularity; does not distinguish between administrative triage mistakes and clinical equipment dependencies; does not restrict clinical referral decisions exclusively to clinicians.

### Option 2: Role-Partitioned Operational Taxonomy (6 Codes — Recommended)

Proposes additional role-to-code eligibility within the fixed actor/status/scope boundaries of `DEC-AUTHZ-04b` and RECON-04. These additional restrictions are not approved policy. Every DOCTOR option remains within the session-selected department; no specialty label authorizes a cross-department transfer.

| Reason Code | Display Label (VN) | Operational Definition | Role Eligibility |
|---|---|---|---|
| `RECEPTION_TRIAGE_CORRECTION` | Tiếp đón phân luồng nhầm | Front desk misrouted patient during reception; corrected before consultation begins. | `RECEPTIONIST` only |
| `PATIENT_SPECIALTY_PREFERENCE` | Người bệnh đổi nguyện vọng | Patient requests another room while waiting in `CHO_KHAM`; a department change is eligible only for `RECEPTIONIST` within the same facility. | `RECEPTIONIST`, `DOCTOR` |
| `CLINIC_LOAD_BALANCING` | Điều phối lưu lượng hàng đợi | Shift patient between parallel rooms in the same department to balance wait times. | `RECEPTIONIST`, `DOCTOR` |
| `CLINICAL_SPECIALTY_REFERRAL` | Chuyển khám chuyên khoa | Examining doctor requests another suitable specialty room within the session-selected department; deny a destination outside it. | `DOCTOR` only |
| `DIAGNOSTIC_EQUIPMENT_NEED` | Cần trang thiết bị chuyên dụng | Room lacks specific diagnostic instrument (e.g., ENT endoscope, ultrasound machine) available in target room. | `DOCTOR` only |
| `CLINICIAN_EMERGENCY_HANDOFF` | Bác sĩ bàn giao đột xuất | Doctor is called to an emergency, hospital round, or sudden absence; handoff of active queue. | `DOCTOR` only |

- **Tradeoffs:**
  - *Pros:* Could distinguish front-desk triage corrections from in-department clinical handoffs for reporting, while preserving approved transfer boundaries.
  - *Cons:* Slightly larger code set; application validation guard must verify actor role against code eligibility.

### Option 3: Two-Tier Extensible Taxonomy (System Category + Tenant Configuration)

Proposes 4 top-level system categories in Java (`ADMINISTRATIVE`, `CLINICAL`, `LOAD_MANAGEMENT`, `PATIENT_REQUEST`), with granular sub-reasons in a hypothetical table or configurable catalog (`DanhMucLyDoDieuChuyen`). The name and schema are unapproved.

- **Tradeoffs:**
  - *Pros:* High extensibility; large multi-facility hospital systems can add custom sub-reasons without code changes or migrations.
  - *Cons:* Introduces substantial relational complexity, additional repository queries, caching needs, and catalog governance overhead. At current Phase 3 maturity, this represents speculative over-engineering before basic transfer capability exists.

### Taxonomy Comparison Summary

| Criteria | Option 1 (4 Core Codes) | Option 2 (Role-Partitioned 6 Codes) | Option 3 (Two-Tier Configurable) |
|---|---|---|---|
| **Simplicity & Implementation Effort** | Very High (trivial enum) | High (single enum + role validator) | Low (requires DB catalog + maintenance UI) |
| **Additional Role-to-Code Restrictions (proposed)** | As listed above | Role-eligible validation | Dependent on catalog design |
| **Reporting & KPI Value** | Moderate (broad categories) | High (clear triage vs clinical vs contingency metrics) | Very High (customizable) |
| **Extensibility without Migration** | Low (enum requires code release) | Moderate (stable core set covers operations) | High (runtime configuration) |
| **Business Reason Storage (unresolved)** | Separate business code | Separate business code | Separate category/sub-code representation |

None of these options maps business codes into `AuditReasonCode` or `NhatKyHeThong.maLyDo`. Transfer audit events, correlation and any separate operational payload contract require later review, regardless of which taxonomy is chosen.

---

## 4. Bounded note length and normalization options

Approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-04` specifies that a short note is **optional**. Because free text introduces data quality, normalization, and privacy risks, constraints must be established.

### Note Length Boundaries

| Option | Proposed Limit | Storage Decision | Tradeoffs & Evaluation |
|---|---|---|---|
| **Option N1: Ultra-compact (100)** | 100; counting unit requires approval | Unicode-capable storage and capacity unresolved | Less space for an operational note. This limit is independent of the security audit `maLyDo` column. |
| **Option N2: Standard Bounded Note (255 — Recommended)** | Propose 255 Unicode code points after normalization | SQL type, capacity and collation unresolved; not automatically `NVARCHAR(255)` | More room for a short Vietnamese note; approval must reconcile UI counting, Java validation and database capacity. |
| **Option N3: Code only (policy amendment required)** | 0; reject notes | N/A | Would remove the Owner-approved optional-note capability. Retained only as a possible explicit policy amendment, not an implementation choice within RECON-04. |

**Counting/storage mapping remains a decision.** Java 17 `String.length()` counts UTF-16 code units; `codePointCount(0, length())` counts Unicode code points ([Java 17 String documentation][java-string]). SQL Server `NVARCHAR(n)` sizes `n` in byte-pairs; with an SC-enabled collation, a supplementary code point consumes two such units ([SQL Server documentation][sql-nvarchar]). For example, 255 supplementary code points require 510 UTF-16 code units and cannot fit `NVARCHAR(255)`. A code point count also differs from a count of user-perceived grapheme clusters; NFC alone does not make those counts equal.

Owner/implementation review must choose the count unit, normalized counting stage, supplementary-character/collation policy and compatible storage capacity, with rejection behavior that avoids silent truncation. A capacity such as `NVARCHAR(510)` could be evaluated for a 255-code-point policy with supplementary support; no SQL declaration, collation or mapping is approved here. Neither business code length nor note length derives from audit `maLyDo VARCHAR(100)`.

### Note Sanitization and Normalization Rules

For later Application-boundary review, propose the following pipeline. It is not implemented or approved; whitespace handling and counting/storage decisions must be resolved before it becomes a deterministic contract:

1. **Null and Blank Collapsing:**
   - If note is `null`, empty string `""`, or contains only whitespace (`note.isBlank()`), normalize to `null`.
2. **Whitespace and Control Character Sanitization:**
   - Choose whether to reject controls/multi-line input or replace line breaks and selected whitespace with single spaces; do not silently concatenate words by deleting separators.
   - Specify the whitespace set and trimming method; `isBlank()`, `trim()` and a regex whitespace class are not interchangeable. Recheck for blank input after normalization.
3. **Unicode Normalization:**
   - Normalize string using Unicode Standard Form NFC (`Normalizer.normalize(note, Normalizer.Form.NFC)`) to ensure consistent indexing and character comparison for Vietnamese diacritics.
4. **Length Enforcement:**
   - If N2's code-point option is approved, count after all normalization and reject above 255; otherwise use the separately approved unit/limit. Check storage capacity independently. `TRANSFER_NOTE_EXCEEDS_MAX_LENGTH` is an illustrative proposed error name, not an existing contract.

---

## 5. Privacy safeguards and audit minimization

The safeguards below are non-binding engineering recommendations based on the current structured audit writer and the approved separation from clinical fields. They make no statutory or legal compliance claim.

### Principle 1: Segregation of Clinical Narrative from Administrative Audit

- **The Problem:** Free-text transfer notes can inadvertently contain Protected Health Information (PHI) or Personally Identifiable Information (PII), such as patient diagnoses, HIV/infectious status, phone numbers, or citizen ID numbers (CCCD).
- **Existing Pattern:** [`JdbcAuditRepositoryImpl.java:18-27,113-165`][audit-writer] sets `chiTiet = NULL` and `lyDo = NULL` on its inserts. It stores structured metadata and the typed audit enum name (or null) in `maLyDo`.
- **Proposed separation:** Keep optional notes outside the security audit tables and keep business transfer codes outside `AuditReasonCode` / `NhatKyHeThong.maLyDo`.
  - A business reason explains why staff requested a move. A security audit reason explains an authentication/authorization outcome or an existing EMR audit exception; these are distinct meanings even when both are represented by codes.
  - A future transfer audit event/action, minimal metadata and correlation to an operational record need a separately scoped contract. The current audit record does not contain both source and destination snapshots. No business-to-security reason mapping is proposed here.
  - This proposal grants no audit-reader access; `AUDIT_READ` remains denied ([baseline acceptance:27][sec01]).

### Principle 2: Storage of the Short Note in Dedicated Transfer History

- Prefer a separate operational history representation for the business reason and optional note if the Owner chooses dedicated history or both history and audit. [`phase3-transfer-queue-history-persistence-proposal.md:59-65`][persistence-proposal] proposes `LichSuDieuChuyenLuotKham` with a mandatory reason and optional note; it approves no table or `ghiChu` column/type/width.
- **Dedicated history, audit-only, or both remain unresolved.** An audit-only choice needs a new, correctly scoped design for retaining the required business reason independently of security reason semantics. The current audit API/schema is not evidence that audit-only transfer storage is supported. Optional-note persistence or transient handling also needs explicit review; do not silently discard notes or remove optional input. Free text must not bypass the existing audit writer's exclusion of free-text details.

### Principle 3: Application-Level PII / PHI Guard Heuristics

To actively discourage staff from typing sensitive identifiers into the optional short note, propose an application-level pre-validation check:

- **Heuristic examples only:** Regex text `\b[0-9]{12}\b` and `\b0[0-9]{9}\b` flags standalone 12-digit and leading-zero 10-digit sequences. These are not identifier validity checks or comprehensive PII/PHI screening. If rejection is chosen, `TRANSFER_NOTE_CONTAINS_SENSITIVE_IDENTIFIER` is only a proposed error name. These are regex expressions, not escaped Java string literals; matching mode, boundary behavior, Unicode handling and preprocessing need specification.
- **False positives:** Harmless operational numbers may match. A match does not establish that a note contains a CCCD or phone number.
- **False negatives:** Spaced/hyphenated or international-format numbers, non-ASCII digits, embedded/obfuscated identifiers, names, addresses, diagnoses and other sensitive narrative may be missed. A non-match does not establish that a note is safe or anonymous. Synthetic edge cases must inform any later enforcement decision.
- **Proposed UI guidance:** *"Ghi chú điều chuyển chỉ phục vụ điều phối hành chính/vận hành phòng khám; tuyệt đối không nhập thông tin chẩn đoán bệnh án, kết quả xét nghiệm hoặc số giấy tờ định danh."* Guidance and minimization remain necessary regardless of regex choice.

---

## 6. Recommendations for Owner consideration (Proposal Only)

The following specific recommendations are submitted for Owner consideration:

> [!IMPORTANT]
> **Proposal Recommendation Summary:**
> 1. **Adopt Option 2 (Role-Partitioned Operational Taxonomy of 6 Codes):**
>    - `RECEPTION_TRIAGE_CORRECTION` (Receptionist only)
>    - `PATIENT_SPECIALTY_PREFERENCE` (Receptionist, Doctor)
>    - `CLINIC_LOAD_BALANCING` (Receptionist, Doctor)
>    - `CLINICAL_SPECIALTY_REFERRAL` (Doctor only)
>    - `DIAGNOSTIC_EQUIPMENT_NEED` (Doctor only)
>    - `CLINICIAN_EMERGENCY_HANDOFF` (Doctor only)
> 2. **Consider Option N2 for Note Constraints:** Propose 255 Unicode code points after normalization, single-line, NFC-normalized, nullable when blank. Counting/storage mapping, collation and whitespace behavior still require explicit approval; `NVARCHAR(255)` is not an established mapping.
> 3. **Adopt Strict Privacy Segregation:**
>    - Keep the fixed business reason distinct from the security `AuditReasonCode` / `NhatKyHeThong.maLyDo` vocabulary; do not write transfer codes into those fields.
>    - Prefer separate operational history for the reason and optional note if approved; names, fields, storage model and capacity remain unresolved. Keep notes outside security audit tables.
>    - Consider heuristic identifier checks with explicit false-positive/negative limitations; rejection versus advisory behavior remains an Owner decision.

---

## 7. Exact unresolved Owner decisions

Before any code, command, or schema implementation can proceed, the Owner must explicitly resolve the following five questions. No decision is inferred or assumed approved:

| # | Unresolved Owner Decision Question | Options Requiring Explicit Choice |
|---|---|---|
| **D1** | **Which reason code taxonomy is approved for clinic operations?** | - **Option 1:** Minimal 4 Core Codes (`WRONG_ROOM_ALLOCATION`, `SPECIALTY_CONSULTATION`, `OVERLOAD_REBALANCING`, `PATIENT_REQUEST`).<br>- **Option 2:** Role-Partitioned 6 Codes (`RECEPTION_TRIAGE_CORRECTION`, `PATIENT_SPECIALTY_PREFERENCE`, `CLINIC_LOAD_BALANCING`, `CLINICAL_SPECIALTY_REFERRAL`, `DIAGNOSTIC_EQUIPMENT_NEED`, `CLINICIAN_EMERGENCY_HANDOFF`).<br>- **Option 3:** Two-Tier Extensible Catalog with database configuration. |
| **D2** | **Should role-to-code eligibility be enforced at the Application boundary?** | - **A (Enforced):** Commands fail closed if an actor supplies a code not authorized for their role (e.g., `RECEPTIONIST` submitting `CLINICAL_SPECIALTY_REFERRAL`).<br>- **B (Permissive):** All codes are available to all permitted transfer actors. |
| **D3** | **What is the maximum allowed length and character policy for the optional short note?** | - **A:** Proposed 255 code points, single-line, NFC normalized.<br>- **B:** Proposed 100, single-line; count unit still to choose.<br>For either, explicitly decide code points versus Java UTF-16 code units (and any grapheme requirement), normalization/whitespace behavior, storage capacity and SQL collation. No automatic `NVARCHAR(n)` mapping.<br>- **C:** Code-only is retained only as a possible explicit amendment to the approved optional-note policy, not an available implementation choice under RECON-04. |
| **D4** | **Where should the business reason and optional short note be persisted?** | Dedicated immutable history, audit-only, or both remain open. **A:** Prefer a separate operational record for reason/note (provisional name `LichSuDieuChuyenLuotKham`), with no approved fields/types. **B:** Evaluate transient-only note handling explicitly. An audit-only design must retain the mandatory business reason separately from security reason semantics; it is unsupported by the current audit API. No note or business code is to be inserted into security reason fields. D3-C requires a policy amendment. |
| **D5** | **Should heuristic identifier checks be enforced on the short note?** | - **A (Enforced):** Reject matches of explicitly defined numeric heuristics, accepting false positives and false negatives.<br>- **B (Advisory):** UI guidance without backend regex rejection. Neither option provides comprehensive PII/PHI screening. |

---

## 8. Validation and guidance record

### Guidance files read before editing

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

### RAG and environment execution record

- **Current review, 2026-11-06 — RAG first:** Before opening repository documents or running `rg`/`grep`, executed `/Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py --repo clinic-desktop --limit 5 'điều chuyển lý do nghiệp vụ ghi chú AuditReasonCode NhatKyHeThong maLyDo Unicode UTF-16 NVARCHAR VIS-TRANSFER-OWNER-DECISION-RECON-04'`.
  - *Result:* Exit 1, `sqlite3.OperationalError: unable to open database file`, during Orchestrator configuration/index connection setup at `PRAGMA journal_mode=WAL`; no search excerpts returned.
  - *Primary guidance:* Used the task-supplied retrieved excerpts, then checked current Owner-decision and source evidence directly. No index/cache workaround, permission escalation, clinic DB connection or patient data access.
- **Document checks:** `git diff --check` passes (exit 0, no output). Read-only Python checks verify reference definitions, local citation target existence/line bounds, required correction coverage and exact single-file scope. Citation claims were also reviewed against the cited source; these checks do not establish runtime acceptance.
- **Scope compliance:** Only `docs/ke-hoach-trien-khai/phase3-transfer-reason-policy-proposal.md` is modified. No source, tests, schema, permission, audit/runtime configuration or gate changes. `run-app.sh` was absent in this checkout and was not created or otherwise touched. No build/application tests run: documentation-only scope. No push, PR or merge.
- **Acceptance:** PASS for allowlisted scope, business/security reason separation, removal of unsupported legal claims, explicit unresolved length/storage mapping, heuristic regex limitations, fixed Owner-policy preservation and non-binding storage options. SEC-01 and downstream gates remain OPEN/default-deny. RAG execution was attempted first; retrieval remains blocked as recorded above.

---

[sec01]: phase2-sec01-baseline-acceptance.md#L4-L32
[contract-review]: phase3-visit-transfer-contract-review.md#L10-L16
[gap-memo]: phase3-visit-transfer-decision-gaps.md#L83-L90
[persistence-proposal]: phase3-transfer-queue-history-persistence-proposal.md#L59-L65
[visit-domain]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L41-L71
[audit-reasons]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditReasonCode.java#L3-L13
[audit-actions]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditActionCode.java#L3-L54
[system-entry]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/SystemAuditEntry.java#L6-L24
[audit-writer]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/system/JdbcAuditRepositoryImpl.java#L18-L165
[audit-immutable]: ../../clinic-infrastructure/src/main/resources/migrations/V6__Enhance_Audit_Tables_Append_Only.sql#L17-L109
[ui-menu]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/menus/PatientQueueContextMenu.java#L71-L75
[ui-panel]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/ExaminationPanel.java#L1080-L1091
[ui-dialog]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/dialogs/TransferRoomSelectDialog.java#L15-L34
[java-string]: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html
[sql-nvarchar]: https://learn.microsoft.com/en-us/sql/t-sql/data-types/nchar-and-nvarchar-transact-sql?view=sql-server-ver17
