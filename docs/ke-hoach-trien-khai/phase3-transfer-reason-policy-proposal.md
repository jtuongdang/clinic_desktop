# VIS-TRANSFER-REASON-POLICY-01 — Transfer reason code taxonomy, note normalization, and privacy policy proposal

**Status:** Source-grounded proposal only, reconciled under approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-04` (superseding `VIS-TRANSFER-OWNER-DECISION-RECON-03` and `VIS-TRANSFER-SOURCE-RECON-01`). Snapshot: 2026-11-06. No Java, tests, migration, permission, policy or database changes. Transfer remains default-deny pending its separately approved implementation and acceptance. SEC-01 Definition A does not itself accept future workflows ([baseline acceptance:4–9,23–32][sec01]).

**Contract:** Task-supplied approved contract v1 `VIS-TRANSFER-OPEN-DECISION-PROPOSALS-01` & `VIS-TRANSFER-OWNER-DECISION-RECON-04`. The recommendations below are proposed technical policy choices for Owner review, not additional owner approvals. This document is explicitly non-binding and does not present recommendations as approved. Only this Markdown deliverable is in scope.

---

## 1. Fixed decisions and reconciliation

The task-supplied contract `VIS-TRANSFER-OWNER-DECISION-RECON-04` is the binding authority for the following owner-approved transfer rules; this proposal takes them as settled invariants and does not reopen them:

| Owner-approved rule | Reason and note policy consequence |
|---|---|
| Transfer reason is mandatory as a fixed reason code; a short note is optional. | Every transfer command must supply a valid fixed reason code from an approved taxonomy. An empty, null, or unlisted reason code fails validation closed (`TRANSFER_REASON_REQUIRED` / `INVALID_TRANSFER_REASON_CODE`). A short note is strictly optional. |
| Clinical reason-for-examination fields (`lyDoKham`) must not be reused. | Transfer is an administrative/operational queue routing movement between clinic rooms, whereas `lyDoKham` represents the patient's chief clinical complaint upon initial presentation ([`LuotKham.java:41`][visit-domain]). `lyDoChuyenTuyen` ([`LuotKham.java:71`][visit-domain]) is an external hospital referral note under MoH Form BA 01/BV. Neither clinical field may be overwritten, appended to, or reused for queue transfer. |
| Preserve visit status, `ngayKham`, and examination timestamps during transfer. | Preserved visit status (`CHO_KHAM`, `DANG_KHAM`, or `CHO_CLS`), clinical visit date `ngayKham`, and existing examination timestamps (`thoiGianBatDauKham`, `thoiGianKetThucKham`) remain intact during transfer. Queue allocation and transfer logging must not alter the clinical visit timeline. |
| Destination queue date is a separate `queueDate` in `Asia/Ho_Chi_Minh`, distinct from `ngayKham`. | Allocate destination queue using the transfer date as a separate `queueDate` in `Asia/Ho_Chi_Minh`. Never mutate clinical `ngayKham`. |
| RECEPTIONIST transfers only `CHO_KHAM`; DOCTOR transfers only `CHO_KHAM`, `DANG_KHAM`, `CHO_CLS`. Final/completed, cancelled, deleted, or EMR-locked visits are denied. | Preflight validation and transaction CAS verify persisted state. Reason code selection must align with the operational role executing the transfer. |
| An active destination room is required. RECEPTIONIST may transfer between departments within the same facility; DOCTOR stays in the session-selected department. Transfer grants no destination patient reads. | Reason codes must reflect room-to-room or department-to-department handoff realities without leaking destination queue or clinical patient data to unauthorized actors. |
| Preserve visit-linked service orders, prescriptions, results, invoices, and financial records. Do not automatically cancel or mutate them, alter billing, or create duplicate charges. | Reason codes must never imply automated billing recalculation, line-item voiding, or prescription cancellation. Any financial or clinical adjustment belongs to a separate authorized workflow. |
| Technical/audit metadata uses UTC; clinic business time uses `Asia/Ho_Chi_Minh`. | Transfer event metadata records UTC timestamps (`Instant.now()`), while effective operational day tracking uses `Asia/Ho_Chi_Minh`. |

Provenance: approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-04`, 2026-11-06; cross-reference [`phase3-visit-transfer-contract-review.md:10-16`][contract-review] and [`phase3-visit-transfer-decision-gaps.md:83-90`][gap-memo].

---

## 2. Inspected implementation and current limits of the evidence

All linked paths resolve relative to this document to repository files. Line anchors were checked against this checkout:

| Area | Inspected source facts | Consequence / gap |
|---|---|---|
| Clinical complaint fields | [`LuotKham.java:41,71`][visit-domain] defines `private String lyDoKham = "";` (patient chief complaint) and `private String lyDoChuyenTuyen = "";` (inter-hospital referral justification per BA 01/BV). | These fields belong to the core EMR clinical narrative. Storing internal room-transfer reasons here corrupts patient medical records and violates statutory EMR regulations. |
| Audit reason codes | [`AuditReasonCode.java:3-13`][audit-reasons] lists 9 codes: `AUTH_INVALID`, `AUTH_THROTTLED`, `AUTH_NO_ROLE`, `AUTH_NO_SCOPE`, `AUTH_FORBIDDEN`, `SCOPE_MISMATCH`, `ACCOUNT_REVOKED`, `EMR_LOCKED`, `REOPEN_CLINICAL_ADDENDUM`. | `AuditReasonCode` is dedicated to authentication, authorization denial, and EMR unlock exceptions. It contains zero codes for queue routing, reception triage, or clinical room transfers. |
| Audit action codes | [`AuditActionCode.java:23-34`][audit-actions] defines `VISIT_REGISTERED` and various EMR/auth actions. | There is no `VISIT_TRANSFERRED` action code currently defined. |
| System audit entry | [`SystemAuditEntry.java:6-24`][system-entry] defines a compact Java record with `actorEmployeeId`, `roleId`, `roleCode`, `facilityId`, `departmentId`, `actionCode`, `resourceType`, `resourceId`, `outcome`, `reasonCode`, `occurredAtUtc`, `workstationId`, and `correlationId`. | The audit entry is strictly structured. It has no field for free-text notes, JSON payloads, or before/after transfer details. |
| Audit persistence and sanitization | [`JdbcAuditRepositoryImpl.java:18-27,140-165`][audit-writer] inserts into `NhatKyHeThong` and `NhatKyHoSoBenhAn`, hardcoding `NULL` for `chiTiet` and `lyDo` columns, while binding `reasonCode` into `maLyDo VARCHAR(100)`. | Existing infrastructure intentionally suppresses free-text details to prevent accidental PII/PHI leakage into immutable audit logs. Any transfer reason code must fit `VARCHAR(100)`, and free-text notes must not be placed into security audit logs. |
| Audit database schema | [`V6__Enhance_Audit_Tables_Append_Only.sql:17-24,69-99`][audit-immutable] constrains `maLyDo VARCHAR(100) NULL` and enforces strict append-only triggers rejecting `UPDATE` and `DELETE`. | Transfer reason codes stored in audit tables are immutable and permanently retained. Free text cannot be modified or redacted once committed. |
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

Partitions reason codes according to operational role capabilities under `DEC-AUTHZ-04b`. Receptionists cannot assert clinical necessity, and doctors cannot cite front-desk triage error for ongoing examinations.

| Reason Code | Display Label (VN) | Operational Definition | Role Eligibility |
|---|---|---|---|
| `RECEPTION_TRIAGE_CORRECTION` | Tiếp đón phân luồng nhầm | Front desk misrouted patient during reception; corrected before consultation begins. | `RECEPTIONIST` only |
| `PATIENT_SPECIALTY_PREFERENCE` | Người bệnh đổi nguyện vọng | Patient requests switching department or examination type while waiting in `CHO_KHAM`. | `RECEPTIONIST`, `DOCTOR` |
| `CLINIC_LOAD_BALANCING` | Điều phối lưu lượng hàng đợi | Shift patient between parallel rooms in the same department to balance wait times. | `RECEPTIONIST`, `DOCTOR` |
| `CLINICAL_SPECIALTY_REFERRAL` | Chuyển khám chuyên khoa | Examining doctor determines patient pathology requires another specialty room/department. | `DOCTOR` only |
| `DIAGNOSTIC_EQUIPMENT_NEED` | Cần trang thiết bị chuyên dụng | Room lacks specific diagnostic instrument (e.g., ENT endoscope, ultrasound machine) available in target room. | `DOCTOR` only |
| `CLINICIAN_EMERGENCY_HANDOFF` | Bác sĩ bàn giao đột xuất | Doctor is called to an emergency, hospital round, or sudden absence; handoff of active queue. | `DOCTOR` only |

- **Tradeoffs:**
  - *Pros:* Enforces domain integrity and professional boundaries; aligns strictly with `DEC-AUTHZ-04b` (receptionists transfer `CHO_KHAM` across departments; doctors transfer active visits within department); produces actionable clinic quality and operational efficiency KPIs (distinguishing front-desk triage accuracy from in-clinic clinical referrals).
  - *Cons:* Slightly larger code set; application validation guard must verify actor role against code eligibility.

### Option 3: Two-Tier Extensible Taxonomy (System Category + Tenant Configuration)

Defines 4 top-level system categories in Java (`ADMINISTRATIVE`, `CLINICAL`, `LOAD_MANAGEMENT`, `PATIENT_REQUEST`), with granular sub-reasons stored in a database table or configurable catalog (`DanhMucLyDoDieuChuyen`).

- **Tradeoffs:**
  - *Pros:* High extensibility; large multi-facility hospital systems can add custom sub-reasons without code changes or migrations.
  - *Cons:* Introduces substantial relational complexity, additional repository queries, caching needs, and catalog governance overhead. At current Phase 3 maturity, this represents speculative over-engineering before basic transfer capability exists.

### Taxonomy Comparison Summary

| Criteria | Option 1 (4 Core Codes) | Option 2 (Role-Partitioned 6 Codes) | Option 3 (Two-Tier Configurable) |
|---|---|---|---|
| **Simplicity & Implementation Effort** | Very High (trivial enum) | High (single enum + role validator) | Low (requires DB catalog + maintenance UI) |
| **Role Boundary Enforcement** | None (all codes shared) | Strict (role-eligible validation) | Dependent on DB metadata |
| **Reporting & KPI Value** | Moderate (broad categories) | High (clear triage vs clinical vs contingency metrics) | Very High (customizable) |
| **Extensibility without Migration** | Low (enum requires code release) | Moderate (stable core set covers operations) | High (runtime configuration) |
| **Audit Log Compatibility** | Full (`VARCHAR(100)`) | Full (`VARCHAR(100)`) | Requires composite category + sub-code |

---

## 4. Bounded note length and normalization options

Approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-04` specifies that a short note is **optional**. Because free text introduces data quality, normalization, and privacy risks, constraints must be established.

### Note Length Boundaries

| Option | Max Length | Storage Type | Tradeoffs & Evaluation |
|---|---|---|---|
| **Option N1: Ultra-compact (100 chars)** | 100 characters | `VARCHAR(100)` | Matches existing `maLyDo` column width. However, 100 characters is often too restrictive in Vietnamese (e.g., `"Chuyển sang PK2 do BS Tuấn có máy nội soi tai mũi họng để kiểm tra dị vật thanh quản"` is 94 characters, leaving no margin). |
| **Option N2: Standard Bounded Note (255 chars — Recommended)** | 255 characters | `NVARCHAR(255)` | Standard single-line database field width. Comfortably accommodates necessary operational handover details in Vietnamese Unicode without permitting extensive narrative or clinical notes. |
| **Option N3: Strictly Disallowed (0 chars / Code only)** | 0 characters | N/A | Eliminates all free-text risks. However, eliminates operational communication between rooms, forcing doctors to rely on out-of-band oral communication. |

### Note Sanitization and Normalization Rules

If an optional short note is supplied, the Application command handler must apply the following deterministic normalization pipeline:

1. **Null and Blank Collapsing:**
   - If note is `null`, empty string `""`, or contains only whitespace (`note.isBlank()`), normalize to `null`.
2. **Whitespace and Control Character Sanitization:**
   - Strip all ASCII control characters (`\u0000`–`\u001F`, `\u007F`), including carriage returns (`\r`) and newlines (`\n`). Multi-line input is strictly rejected or collapsed into single spaces (`\\s+` → `" "`).
   - Trim leading and trailing whitespace (`note.trim()`).
3. **Unicode Normalization:**
   - Normalize string using Unicode Standard Form NFC (`Normalizer.normalize(note, Normalizer.Form.NFC)`) to ensure consistent indexing and character comparison for Vietnamese diacritics.
4. **Length Enforcement:**
   - Enforce character count $\le 255$ codepoints after trimming. Reject strings exceeding limit with `TRANSFER_NOTE_EXCEEDS_MAX_LENGTH`.

---

## 5. Privacy safeguards and audit minimization

Medical and operational logs in healthcare systems are subject to strict privacy segregation (Vietnam MoH Circular 46/2018/TT-BYT on Electronic Medical Records, Circular 54/2017/TT-BYT on HIT standards):

### Principle 1: Segregation of Clinical Narrative from Administrative Audit

- **The Problem:** Free-text transfer notes can inadvertently contain Protected Health Information (PHI) or Personally Identifiable Information (PII), such as patient diagnoses, HIV/infectious status, phone numbers, or citizen ID numbers (CCCD).
- **Existing Pattern:** [`JdbcAuditRepositoryImpl.java:18-27`][audit-writer] intentionally sets `chiTiet = NULL` and `lyDo = NULL` on all `NhatKyHeThong` and `NhatKyHoSoBenhAn` inserts. It stores only structured metadata and the enum code in `maLyDo VARCHAR(100)`.
- **Architectural Rule:** The optional short note **must NEVER be written into the security audit table `NhatKyHeThong`**.
  - `NhatKyHeThong.maLyDo` stores ONLY the fixed transfer reason enum name (e.g., `'CLINICAL_SPECIALTY_REFERRAL'`).
  - Security auditors inspecting system access see who transferred the visit, from where, to where, when, and under which authorized code, without exposing free-text operational notes.

### Principle 2: Storage of the Short Note in Dedicated Transfer History

- As proposed in [`phase3-transfer-queue-history-persistence-proposal.md:65`][persistence-proposal], the optional short note belongs exclusively in the dedicated immutable operational transfer history record (`LichSuDieuChuyenLuotKham.ghiChu NVARCHAR(255) NULL`).
- If the Owner ultimately decides on an **audit-only** persistence model without a dedicated transfer history table, the optional short note **must be discarded from persistence entirely**, or transfer reason policy must revert to code-only. Free-text notes must not bypass audit sanitization to be dumped into system audit tables.

### Principle 3: Application-Level PII / PHI Guard Heuristics

To actively discourage staff from typing sensitive identifiers into the optional short note, propose an application-level pre-validation check:
- **Reject numeric identifier patterns:** Reject notes matching Vietnamese CCCD pattern (`\\b\\d{12}\\b`) or phone number pattern (`\\b(0\\d{9})\\b`) with `TRANSFER_NOTE_CONTAINS_SENSITIVE_IDENTIFIER`.
- **UI Guidance:** The desktop transfer dialog must explicitly display: *"Ghi chú điều chuyển chỉ phục vụ điều phối hành chính/vận hành phòng khám; tuyệt đối không nhập thông tin chẩn đoán bệnh án, kết quả xét nghiệm hoặc số giấy tờ định danh."*

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
> 2. **Adopt Option N2 for Note Constraints:** Maximum 255 characters, single-line, NFC-normalized, whitespace-trimmed, nullable when blank.
> 3. **Adopt Strict Privacy Segregation:**
>    - Fixed reason code stored in both `NhatKyHeThong.maLyDo` and `LichSuDieuChuyenLuotKham.maLyDo`.
>    - Optional short note stored **only** in `LichSuDieuChuyenLuotKham.ghiChu`, completely excluded from `NhatKyHeThong`.
>    - Application pre-validation rejects 12-digit CCCD and 10-digit phone patterns in notes.

---

## 7. Exact unresolved Owner decisions

Before any code, command, or schema implementation can proceed, the Owner must explicitly resolve the following five questions. No decision is inferred or assumed approved:

| # | Unresolved Owner Decision Question | Options Requiring Explicit Choice |
|---|---|---|
| **D1** | **Which reason code taxonomy is approved for clinic operations?** | - **Option 1:** Minimal 4 Core Codes (`WRONG_ROOM_ALLOCATION`, `SPECIALTY_CONSULTATION`, `OVERLOAD_REBALANCING`, `PATIENT_REQUEST`).<br>- **Option 2:** Role-Partitioned 6 Codes (`RECEPTION_TRIAGE_CORRECTION`, `PATIENT_SPECIALTY_PREFERENCE`, `CLINIC_LOAD_BALANCING`, `CLINICAL_SPECIALTY_REFERRAL`, `DIAGNOSTIC_EQUIPMENT_NEED`, `CLINICIAN_EMERGENCY_HANDOFF`).<br>- **Option 3:** Two-Tier Extensible Catalog with database configuration. |
| **D2** | **Should role-to-code eligibility be enforced at the Application boundary?** | - **A (Enforced):** Commands fail closed if an actor supplies a code not authorized for their role (e.g., `RECEPTIONIST` submitting `CLINICAL_SPECIALTY_REFERRAL`).<br>- **B (Permissive):** All codes are available to all permitted transfer actors. |
| **D3** | **What is the maximum allowed length and character policy for the optional short note?** | - **A:** 255 characters (`NVARCHAR(255)`), single-line, NFC normalized.<br>- **B:** 100 characters (`VARCHAR(100)`), single-line.<br>- **C:** 0 characters (disallow free-text notes entirely; code only). |
| **D4** | **Where should the optional short note be persisted?** | - **A:** In dedicated `LichSuDieuChuyenLuotKham.ghiChu` only; never in `NhatKyHeThong`.<br>- **B:** Excluded from database persistence entirely (used only as a transient receipt message).<br>- **C:** Disallowed by choosing Code-Only policy (D3-C). |
| **D5** | **Should automated PII screening (regex for CCCD/phone) be enforced on the short note?** | - **A (Enforced):** Reject transfer command if note matches CCCD/phone pattern.<br>- **B (Advisory):** UI-only warning label without backend regex rejection. |

---

## 8. Validation and guidance record

### Guidance files read before editing

- Orchestrator baseline `docs/PROJECT_CONVENTIONS.md`.
- Repository `README.md`.
- `DOMAIN_ANALYSIS.md` (Domain package architecture, entity definitions, and enums).
- `ERD_DIAGRAM.md` (Database schema, entity relationships, and column structures).
- Approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-04` (superseding `VIS-TRANSFER-OWNER-DECISION-RECON-03`).
- `docs/ke-hoach-trien-khai/phase3-visit-transfer-contract-review.md` (Workflow contract review).
- `docs/ke-hoach-trien-khai/phase3-visit-transfer-decision-gaps.md` (Decision gap analysis memo).
- `docs/ke-hoach-trien-khai/phase3-transfer-queue-history-persistence-proposal.md` (Queue allocation and persistence proposal).
- `docs/ke-hoach-trien-khai/phase3-transfer-linked-work-eligibility-review.md` (Linked-work eligibility review).
- `docs/ke-hoach-trien-khai/phase3-visit-lifecycle-owner-decisions.md` (Visit lifecycle owner decisions).
- `docs/ke-hoach-trien-khai/phase2-role-permission-matrix-approval.md` (`DEC-AUTHZ-04b`).
- `docs/ke-hoach-trien-khai/phase2-implementation-contract.md` (`DEC-TIME-01`, `SEC-01`).

### RAG and environment execution record

- **Project RAG search execution attempt:** Performed command execution `/Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py "visit transfer reason code note safeguards lyDoKham" --limit 5`.
  - *Result:* Blocked by sandbox security policy (`Permission denied for command ... Matches user-configured deny rule`).
  - *Mitigation & Primary Guidance:* Transparently recorded. The Orchestrator supplied full relevant RAG passages and approved contracts (`VIS-TRANSFER-OWNER-DECISION-RECON-04`) in task context; citations and source facts were directly substantiated against repository sources using filesystem read tools.
- **Git diff and command checks:** `git status` and `git diff --check` commands are blocked by sandbox deny rules. Formatting, whitespace, link anchors, and file scope were validated directly.
- **Scope compliance:** Exactly one allowlisted file was created: `docs/ke-hoach-trien-khai/phase3-transfer-reason-policy-proposal.md`. No Java code, tests, schema migrations, permissions, or runtime gates were modified.

---

[sec01]: phase2-sec01-baseline-acceptance.md#L4-L32
[contract-review]: phase3-visit-transfer-contract-review.md#L10-L16
[gap-memo]: phase3-visit-transfer-decision-gaps.md#L83-L90
[persistence-proposal]: phase3-transfer-queue-history-persistence-proposal.md#L65
[visit-domain]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L41-L71
[audit-reasons]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditReasonCode.java#L3-L13
[audit-actions]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditActionCode.java#L23-L34
[system-entry]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/SystemAuditEntry.java#L6-L24
[audit-writer]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/system/JdbcAuditRepositoryImpl.java#L18-L165
[audit-immutable]: ../../clinic-infrastructure/src/main/resources/migrations/V6__Enhance_Audit_Tables_Append_Only.sql#L17-L99
[ui-menu]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/menus/PatientQueueContextMenu.java#L71-L75
[ui-panel]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/ExaminationPanel.java#L1080-L1091
[ui-dialog]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/dialogs/TransferRoomSelectDialog.java#L15-L34
