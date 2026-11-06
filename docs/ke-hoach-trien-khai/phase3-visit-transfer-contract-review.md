# VIS-TRANSFER-REVIEW-01 — Visit transfer workflow contract review

**Status:** Documentation-only RECON-05 contract addendum, 2026-11-06; prior source-review snapshot retained below. No application code, tests, schema, grants, runtime configuration, database or patient data changed or accessed.

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

### RECON-05 guidance, retrieval and validation record — 2026-11-06

- Read the task-supplied guidance before editing: `docs/PROJECT_CONVENTIONS.md` (Orchestrator baseline), `README.md`, and supplied excerpts of `DOMAIN_ANALYSIS.md` and `ERD_DIAGRAM.md`.
- Read supplied retrieved excerpts under `docs/ke-hoach-trien-khai/`: `phase3-transfer-linked-work-eligibility-review.md`, `phase3-transfer-reason-policy-proposal.md`, `phase3-transfer-queue-allocation-proposal.md`, `phase3-transfer-queue-history-persistence-proposal.md`, and `phase3-visit-transfer-contract-review.md`. Read the supplied approved RECON-05 contract, retaining RECON-04.
- **RAG first:** before any local repository file reads, executed `/Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py --repo clinic-desktop --limit 5 'VIS-TRANSFER-OWNER-DECISION-RECON-05 RECON-04 transfer reason queueDate immutable history audit idempotency receipt'`. Exit 1: `sqlite3.OperationalError: unable to open database file` at `PRAGMA journal_mode=WAL`; no new excerpts returned. Used the task-supplied RAG excerpts and Owner contract as fallback. No index/cache workaround or clinic DB access.
- Current local content was read only from this review, [decision-gap memo](phase3-visit-transfer-decision-gaps.md) and [lifecycle owner decisions](phase3-visit-lifecycle-owner-decisions.md). Existing implementation patterns were reviewed through their retained evidence maps, not by opening non-allowlisted source files. Prior validation claims below are historical, not current test results.
- Validation: `git diff --check` passed (exit 0, no output). A read-only inline `python3` check passed for the exact six-code role matrix, RECON-04/05 fact/open-decision/gate coverage, identical current addenda, preservation of all original snapshot bodies against `git show HEAD:<allowlisted path>`, exactly three allowlisted changed paths and no untracked files. No runtime tests are added or run because this task changes documentation only. Remaining implementation acceptance above is not claimed as passed.

## Historical transfer reconciliation and prior review record — through RECON-04

The transfer reconciliation below is a historical snapshot superseded by RECON-05 on 2026-11-06. Its source inventory, guidance/validation claims and acceptance proposals are preserved. Transfer references below to “current”, “still open” or choices awaiting approval describe that dated snapshot only; the RECON-05 addendum above controls. Unrelated lifecycle decisions/proposals remain unchanged and are not settled by RECON-05. The old source findings have not been revalidated against implementation files for this documentation-only task.

**Prior reconciliation snapshot — 06/11/2026:** `VIS-TRANSFER-OWNER-DECISION-RECON-03` superseded the pending-choice language in the original review snapshot. Its six open choices are retained as a dated historical snapshot; the source inventory remains a review snapshot.

**Current reconciliation — 2026-11-06:** Owner-approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-04` supersedes RECON-03 on the newly decided visit state/timestamps, transfer reason requirement, and queue date. The decisions below are current; only the four unresolved implementation areas listed under “Decisions still open” remain open. No implementation is authorized; the workflow remains default-deny behind SEC-01.

## Executive finding

The approved DEC-AUTHZ-04b actor/scope rule is not implemented as a visit-transfer workflow. The only transfer-labelled desktop path is an examination-screen prototype: it confirms a transfer, chooses a hard-coded room label, and opens a print preview. It does not submit a visit ID or update a visit. Reception has protected room and queue reads plus registration, but no transfer action. Domain and schema hold the visit's current facility, department, and optional room, but no transfer history or transfer-specific mutation contract exists. No transfer command, handler, persistence adapter method, audit action, or transfer test was found.

The approved rules remain fixed: `RECEPTIONIST` may transfer only `CHO_KHAM` visits between departments in the same facility; `DOCTOR` may transfer `CHO_KHAM`, `DANG_KHAM`, or `CHO_CLS` visits only within the selected department. Final/completed, cancelled, soft-deleted, or EMR-locked visits are denied. A destination room is required, active, and belongs to the destination department. Transfer grants no patient/history/queue read access in the destination department. Allocate a new destination queue number, preserve existing priority, and never auto-promote. Preserve links to service orders, prescriptions, results, invoices, and financial records; do not mutate/cancel them, alter billing, or create duplicate charges. Pending work may follow only when safe with performing unit and responsibility intact; otherwise require completion/handoff or deny. Issued prescriptions, confirmed results, and posted financial transactions are immutable; adjustments require a separate authorized, reconciled, audited workflow. Enforce transaction consistency, optimistic concurrency, domain invariants, and exception-path tests. Technical/audit metadata uses UTC; business time uses `Asia/Ho_Chi_Minh`.

The Owner-approved `VIS-TRANSFER-OWNER-DECISION-RECON-04` additionally requires preserving visit status, `ngayKham`, and existing examination timestamps. The transfer date in `Asia/Ho_Chi_Minh` is a separate queue date, `queueDate`; queue allocation must not rewrite the clinical visit date/timeline. A fixed reason code is mandatory and a short note is optional; do not reuse clinical `lyDoKham`. The code taxonomy, note length/normalization, privacy rules, detailed persistence, queue sequence/reuse and transaction sampling choices remain unresolved. Provenance: `VIS-TRANSFER-OWNER-DECISION-RECON-04`, approved 2026-11-06; see [decision-gap memo](phase3-visit-transfer-decision-gaps.md) and [lifecycle owner decisions](phase3-visit-lifecycle-owner-decisions.md). The workflow remains denied and gated by SEC-01; this reconciliation does not propose a grant or runtime enablement.

## Evidence key and approved policy

The Owner-approved role/scope decision is recorded at [`phase2-role-permission-matrix-approval.md:8-18`](phase2-role-permission-matrix-approval.md#L8-L18), including strict `(facilityId, departmentId)` session scope, the narrow RECEPTIONIST same-facility transfer exception, the DOCTOR same-department limit, no target-department patient read, and the requirement that workflows remain denied until guard/audit/tests pass. The implementation sequencing and SEC-01 hard gate are in [`phase2-implementation-contract.md:32-50`](phase2-implementation-contract.md#L32-L50). The role/action proposal identifies DEC-AUTHZ-04b as approved but unimplemented at [`phase2-role-permission-matrix-proposal.md:208-214`](phase2-role-permission-matrix-proposal.md#L208-L214). No policy decision is reopened here.

## Current source map

Paths below are repository-relative; linked line references point to the checkout reviewed.

| Area | Current evidence | Finding |
|---|---|---|
| Desktop transfer entry | [`PatientQueueContextMenu.java:71-75`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/menus/PatientQueueContextMenu.java#L71-L75) adds “Chuyển Bệnh Nhân Sang Phòng Khác” when the row is not marked examined. [`ExaminationPanel.java:1080-1091`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/ExaminationPanel.java#L1080-L1091) confirms, selects a room string, and opens `TransferSlipPrintPreviewDialog`; there is no Application call or saved result. | Prototype presentation/print path only; not a visit transfer. It is under examination UI, not reception UI. |
| Transfer room selector | [`TransferRoomSelectDialog.java:15-34,54-71,91-100`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/dialogs/TransferRoomSelectDialog.java#L15-L34) defines a static string list and returns the selected label. | No facility/department/room IDs, active-room query, or scope validation. “Lưu” only closes the dialog with a label. |
| Reception UI | [`ReceptionPanel.java:77-115`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L77-L115) obtains session contexts, asynchronously loads protected active rooms and the reception queue, then displays them. | Current reception panel wires read/registration behavior; no transfer command/action appears in this path. |
| Visit domain/state | [`LuotKham.java:29-54,73-84`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L29-L54) stores `maCoSo`, `maKhoa`, optional `maPhongKham`, status and queue fields. [`LuotKham.java:88-115,117-142`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L88-L115) has start, complete, and lock methods plus public state/scope setters, but no transfer method or transfer guard. [`TrangThaiLuotKham.java:3-10`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/TrangThaiLuotKham.java#L3-L10) lists statuses without a transition graph. | Current-location fields exist; no transfer lifecycle/event/history contract. General setters do not enforce DEC-AUTHZ-04b. |
| Room/department model | [`Khoa.java:6-26`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/facility/Khoa.java#L6-L26) links a department to a facility and lists rooms. [`PhongKham.java:4-21`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/facility/PhongKham.java#L4-L21) links a room to a department and carries an active flag. | Model can represent room-to-department-to-facility relationships; transfer-target resolution and validation are not implemented. |
| Domain repository port | [`ILuotKhamRepository.java:6-9`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/repositories/ILuotKhamRepository.java#L6-L9) declares visit-by-doctor/date and next queue number only. | No transfer/update contract is declared. No implementation of this port was found in the repository search. |
| Application reception port | [`ReceptionQueuePort.java:9-15`](../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/ReceptionQueuePort.java#L9-L15) supports visit creation, queue read, and room-summary read. | No transfer operation. This separate port is used by the reception adapter. |
| Authorization boundary | [`AuthorizationContext.java:5-17`](../../clinic-application/src/main/java/vn/clinicmanagement/application/authorization/AuthorizationContext.java#L5-L17) carries one immutable employee/role/facility/department/session context. [`AuthorizationService.java:34-50`](../../clinic-application/src/main/java/vn/clinicmanagement/application/authorization/AuthorizationService.java#L34-L50) denies invalid contexts and any request target pair different from the session pair. [`AuthorizationService.java:53-107`](../../clinic-application/src/main/java/vn/clinicmanagement/application/authorization/AuthorizationService.java#L53-L107) revalidates session, employee, assigned role/scope and a unique allowed `KHOA_PHONG` grant. | The general evaluator cannot express a cross-department destination while preserving its ordinary exact-pair rule. Transfer needs a narrowly scoped, separately specified guard; do not weaken read authorization or reuse an unscoped UI choice. |
| Existing protected write pattern | [`RegisterPatientHandler.java:61-80,97-141`](../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java#L61-L80) checks authorization and destination room facility/department within `TransactionBoundary`; it persists visit/related data and appends audit before success. [`TransactionBoundary.java:5-8`](../../clinic-application/src/main/java/vn/clinicmanagement/application/common/TransactionBoundary.java#L5-L8) specifies that failure results roll back. | Reusable architectural precedent, not a transfer implementation. The registration handler's target-scope check is strict same-pair and is not the DEC-AUTHZ-04b exception. |
| Reception JDBC adapter | [`ReceptionQueueRepositoryImpl.java:23-36`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L23-L36) implements create/read port. [`:37-81`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L37-L81) inserts new visits in `CHO_KHAM`; [`:125-147`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L125-L147) scopes queue and room summaries by facility/department, excludes deleted and cancelled visits, and sorts by priority, queue number, then ID. | No update/transfer persistence method or transfer history write. Existing queue order is evidence for current reads only; the approved transfer rule is a new destination number with preserved priority/no auto-promotion. Contract RECON-04 requires a separate local-date `queueDate`; persistence shape, sequence/reuse and exact transaction sampling remain open. |
| SQL schema | [`V1__Initial_Clinic_Schema.sql:31-64`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L31-L64) defines `Khoa(maCoSo)` and `PhongKham(maKhoa)` FKs. [`:327-380`](../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L327-L380) stores a visit's current `maCoSo`, `maKhoa`, nullable `maPhongKham`, status, queue number/display, business dates, and optimistic `version`; FKs are individual references. | Current pair/room can be stored, but schema has no source/target transfer record, actor/time/reason metadata, or composite invariant tying the room's department/facility to the visit. No migration is proposed by this report. |
| Audit contract | [`AuditWritePort.java:3-10`](../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditWritePort.java#L3-L10) exposes current-transaction and independent append operations. [`AuditActionCode.java:23-34`](../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/AuditActionCode.java#L23-L34) has `VISIT_REGISTERED` and EMR actions, but no visit-transfer action. | Generic writer exists; transfer event/action and its metadata semantics do not. A transfer write must fail closed and be atomic with the transfer. |
| Existing tests | [`ReceptionQueueRepositoryImplTest.java:37-60`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImplTest.java#L37-L60) checks scoped queue SQL; [`:62-90`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImplTest.java#L62-L90) checks synthetic visit insert transaction behavior. [`SearchAndReceptionQueueHandlerTest.java:23-120`](../../clinic-application/src/test/java/vn/clinicmanagement/application/reception/SearchAndReceptionQueueHandlerTest.java#L23-L120) covers protected reception reads, including denied scope, audit order, and fail-closed reads. | These are reception create/read tests, not transfer tests. Repository search found no transfer command/handler/SQL/update/UI test or transfer audit code. |

### Explicitly absent in the reviewed checkout

- No transfer visit command, request/result, handler/use case, dedicated eligibility/target query, or transfer-specific authorization guard.
- No `LuotKham` transfer method/event/history model; no `ILuotKhamRepository` transfer method or adapter; no update SQL for changing the visit's facility/department/room/queue.
- No transfer audit action/event code or defined audit reason/metadata payload.
- No reception transfer menu/action wired to Application. The only transfer-labelled path is the mock examination UI above.
- No transfer-focused domain, application, JDBC, SQL Server, or desktop interaction test. Existing reception tests do not prove transfer behavior.

Absence claims are bounded to repository-wide source searches for transfer names/actions and inspection of the visit, reception, authorization, audit, adapter, schema, and test paths cited above.

## Facts, approved policy, and inferences

**Facts:** Current visit stores one current facility/department/optional room and has a version column. Current reception registration writes a new visit and audit in a transaction. Reception queue reads and room summaries are exact facility/department scoped. The general authorization evaluator rejects target scope outside the session pair. The UI transfer selection is a string from a fixed list and only feeds a print preview.

**Approved policy:** The actor/scope rules above and the current `VIS-TRANSFER-OWNER-DECISION-RECON-04` rules are fixed. Runtime remains denied until the applicable security and implementation/test gates are satisfied.

**Implementation evidence (not policy):** The reviewed checkout still has no transfer command, transfer guard, persisted transfer mutation/history, or transfer-specific tests. Therefore it cannot yet prove active destination-room ownership, safe linked-work handoff, or transaction/concurrency invariants. Until the necessary evidence exists, implementation must fail closed. Any destination queue/patient data read remains separately protected. Do not infer the resulting visit status or replay behavior from source status, queue allocation, optimistic concurrency, or this review.

## Decisions still open

The Owner-approved contract leaves exactly these implementation choices unresolved; none reopens the approved transfer policy above:

1. **Reason detail:** The reason code taxonomy; note length/normalization constraints and privacy rules. A fixed reason code is mandatory and a short note is optional.
2. **Queue allocation detail:** Persistence shape, sequence namespace, allocation reuse/gap policy, and exact transaction sampling semantics for `queueDate`. `queueDate` itself is the transfer-local business date in `Asia/Ho_Chi_Minh`, distinct from preserved `ngayKham`.
3. **Transfer history:** Dedicated immutable transfer history, audit-only, or both.
4. **Idempotent replay:** Replay outcome semantics, including reuse of a key with a different payload. Do not infer a result.

Resolved by RECON-04: preserve visit status, `ngayKham`, and existing examination timestamps; require a fixed reason code with an optional short note; use separate local-date `queueDate`. Provenance: `VIS-TRANSFER-OWNER-DECISION-RECON-04`, approved 2026-11-06; RECON-03's six-item open list above is a historical snapshot, not current policy. Technical/audit metadata remains UTC. Cross-reference the [decision-gap memo](phase3-visit-transfer-decision-gaps.md) and [lifecycle owner decisions](phase3-visit-lifecycle-owner-decisions.md).

## Concrete acceptance gates for a later implementation task

All tests use synthetic fixtures only. These gates do not authorize work before SEC-01 is accepted.

### Application and authorization

- A transfer command accepts an authenticated application context and visit/destination identifiers; any reason and replay fields follow only choices later approved by the Owner. It never trusts role, source scope, patient identity, or destination ownership supplied only by the desktop.
- Revalidate active session, employee, selected role and assignment. RECEPTIONIST: allow only when source visit facility equals session facility and destination facility equals that same facility; allow department change only within it. DOCTOR: require source and destination department to equal the selected session department and facility to equal the selected facility. Deny every other role unless a separate approved policy later says otherwise.
- Explicit tests deny cross-facility targets for RECEPTIONIST and DOCTOR, deny a DOCTOR target department different from session department, deny source visit outside the permitted scope, and allow RECEPTIONIST same-facility cross-department plus DOCTOR same-department cases. Test missing/revoked session, role, assignment, malformed IDs, and dependency failures as default deny.
- Transfer cannot call patient search, patient detail, history, or destination queue read. Verify denied attempts and instrumented fakes show zero such repository calls. A transfer receipt must not return destination patient/queue contents; destination reads remain separately authorized and audited.
- Enforce the approved actor-specific source statuses and destination room rule. Preserve linked records without mutation; fail closed unless pending-work safety and responsibility handoff can be proven within the transaction. No UI-only enforcement.

### Transaction, audit, persistence

- Resolve source visit, validate its scope/version and the active destination room's department/facility relationship, allocate a new destination queue number while preserving priority, and apply the approved location mutation in one consistent `TransactionBoundary`.
- Failure writing any transfer/location/queue/history/audit artifact required by the later approved persistence choice must return failure and roll back the transaction; tests must prove no partial transfer.
- Record only metadata selected by the later approved audit contract; keep sensitive clinical narrative and unnecessary patient identifiers out of audit/log payloads. Whether to use dedicated immutable history, audit-only, or both remains open.
- JDBC acceptance proves scope/location/room invariants, optimistic concurrency, new destination queue number and preserved priority, linked-work immutability, transaction rollback, and exception-path behavior; read back persisted state after commit. If a schema change proves necessary, prepare a separate reviewed migration task; this report adds none.
- Tests must cover concurrency and optimistic version protection without choosing the unresolved idempotent replay outcome. Add replay-specific assertions only after the Owner settles those semantics.

### Desktop and tests

- Replace/retire the prototype path only in a separately authorized implementation task. The UI must choose from scoped, persisted destination data, display only information authorized for the current role/scope, and call the application command; hiding controls is usability only, never the guard.
- Show a clear success/failure result from the application. On success, refresh only the currently authorized source queue/read model; do not automatically open or read the target department queue. Retry behavior must not assume unresolved replay semantics.
- Add focused domain/application/JDBC and headless UI tests for the role/scope and approved source-status rules, no destination patient reads, active target room/department consistency, queue allocation/priority preservation, linked-work invariants, transaction rollback, optimistic concurrency, and success/failure UI feedback. SQL Server tests, if later required, must be explicitly opt-in and use synthetic data.

## Guidance and reviewed contracts

- Orchestrator-supplied guidance read: `docs/PROJECT_CONVENTIONS.md`, repository `README.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `UI_CODING_STANDARDS.md`.
- Repository instructions checked: searched for `AGENTS.md` under the parent checkout; none was found. The repository guidance files `docs/PROJECT_CONVENTIONS.md` and `docs/DEVELOPMENT_CONVENTIONS.md` were not present in the checkout inventory; the supplied Orchestrator baseline was used.
- Roadmap/task context read: `docs/ke-hoach-trien-khai/README.md`, `03-model-task-board.md`, `02-cac-dot-trien-khai.md`, `phase3-visit-lifecycle-review.md`.
- Phase 2 contracts/policy read: `phase2-implementation-contract.md`, `phase2-role-permission-matrix-approval.md`, `phase2-role-permission-matrix-proposal.md`, `phase2-authorization-evaluator-contract.md`, `phase2-authz-foundation-contract.md`, `phase2-authz-enforcement-review.md`, `phase2-audit-implementation-proposal.md`, `phase2-security-policy-proposal.md`.
- Other source-grounded lifecycle context read: `phase0-repository-contract-review.md` and `phase2-domain-audit-review.md`.

## Validation scope

This is a documentation-only change. No database or patient data access was performed. The requested repository `mvn -Djava.awt.headless=true clean verify` and `git diff --check` results are recorded by the task completion summary; no tests were added or modified.
