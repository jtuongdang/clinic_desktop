# Phase 1–3 Readiness and Owner Gate Summary

**For:** Owner  
**Snapshot:** `0b68800` (`feature/patient-reception-workflow`), 2026-11-07  
**Decision:** Phase 1–2 code/test evidence is ready for Owner visual acceptance, but `SEC-01`, `REC-01`, and `PAT-02` remain open/default-deny. Phase 3 has useful domain/application building blocks, but Owner policy and implementation decisions remain prerequisites to migrations and handlers.

## 1. Reactor and Desktop verification

- **Reactor:** 622 tests run — 130 domain, 235 application, 125 infrastructure, and 132 desktop; **0 failures, 0 errors, 6 expected opt-in SQL Server skips** (4 in infrastructure + 2 in desktop).
- **Wave 17 commits recorded:**
  - `764510c` (`bc4b80c3` — `copilot-cli`): Added `CoSoTest` (`3/3 PASS`), `KhoaTest` (`3/3 PASS`), and `PhongKhamTest` (`3/3 PASS`) in `clinic-domain`.
  - `af52771` (`a86f61e8` — `google-antigravity-cli-claude-sonnet-02507d`): Synced task board, completion review, and handover docs for Wave 16.
  - `0b68800` (`ee1dcb40` — `copilot-cli`): Added `ChanDoanTest` (`3/3 PASS`), `SinhHieuTest` (`3/3 PASS`), and `ICDTest` (`3/3 PASS`) in `clinic-domain`.
- **Wave 16 commits recorded:**
  - `267c176` (`83da02cd` — `copilot-cli`): Added `BenhNhanTest` (`6/6 PASS`), `BaoHiemTest` (`3/3 PASS`), and `BenhNhanCoSoTest` (`3/3 PASS`) in `clinic-domain`.
  - `512eeff` (`dcbd3de6` — `google-antigravity-cli-claude-sonnet-02507d`): Synced task board, completion review, and handover docs for Wave 15.
- **Wave 15 commits recorded:**
  - `4c98449` (`27a6d80c` — `copilot-cli`): Added `DesktopWorkerDiagnosticsTest` (`3/3 PASS`) and `DocxTableTest` (`4/4 PASS`) in `clinic-desktop`.
  - `bd5db20` (`c2e19e6f` — `antigravity-cli`): Added `AuditMetadataValidatorTest` (`43/43 PASS`) and `ApprovedRoleCodesTest` (`43/43 PASS`) in `clinic-application`.
  - `d10cd04` (`92979f03` — `google-antigravity-cli-claude-sonnet-02507d`): Synced task board, completion review, and handover docs for Wave 14.
- **UI preservation:** the `6aabb85` Swing layout is reported **100% preserved**. `DoctorStationUiPreservationTest` guards the 8-column read-only history table, nine action buttons, 250 px split pane, and five tabs.
- **Package:** the baseline reports the shaded JAR ready at `clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar`. The JAR was not present in this isolated worktree when checked, so its presence here is not independently verified.
- Automated verification is synthetic/headless. This summary did not run the application, run tests, or connect to `localhost:1433` / `clinicDb`.

## 2. Phase 1–2 Owner gates: `SEC-01`, `REC-01`, and `PAT-02`

### `SEC-01` — implementation evidence complete; Owner gate open

- **Implemented and tested:** the documented SEC-01 code findings are resolved: login/session validation and safe failure messages; verified session context for user details; restricted session mutation; dashboard lock-overlay and Ribbon state handling; and protected reception reads. Evidence includes `LoginSessionFlowTest` **8/8**, `DashboardNavigationPolicyTest` **6/6**, `UserSessionContextTest` **8/8**, and `ReceptionProtectedReadWiringTest` **1/1**.
- **Still required:** Owner visual smoke and explicit acceptance. Headless results and source review do not close `SEC-01`.
- **Navigation remains default-deny:** `DashboardNavigationPolicy` currently allows only `home` (when granted), `user`, and `help`; other routes are denied. Owner must explicitly decide whether to open the `"reception"` route after visual smoke. Do not treat successful smoke as implicit authorization to enable it.

### `REC-01` — reception hardening implemented and covered; Owner gate open

The documented implementation slices are complete and have focused test evidence:

| Slice | Implemented behavior | Focused evidence |
|---|---|---|
| P0.2 | Preserve existing patient/insurance records, stamp insert actor, strictly parse dates, and use clinic-local dates. | `BaoHiemRepositoryImplTest` **10/10**; `RegisterPatientHandlerTest` **32/32**; `PatientRegistrationFormPanelTest` **7/7** |
| P1.3 | Bind active room selections to actual room IDs; disambiguate duplicate labels and clear stale options. | `ReceptionPanelActiveRoomBindingTest` **7/7** |
| P1.4 / P1.7 | Delegate form reset, wire F1/F2 actions, use clinic date, populate room summaries, and clear stale summaries on failure. | `ReceptionPanelFormResetAndSummaryTest` **8/8**; `ReceptionActionBindingsTest` **2/2** |
| P1.5 / P1.6 | Invalidate stale worker results across view/session changes, cap search results, and lock save-in-progress form state. | `ReceptionPanelWorkerAndSearchHardeningTest` **12/12** |
| P2.8 | Show safe failure messages and distinct audit correlation IDs. | `ReceptionPanelMessagePolicyTest` **12/12** |

- **Still required:** Owner runs `./run-app.sh` against `clinicDb`, follows `desktop-owner-visual-smoke-checklist.md`, records visual/interaction results, and explicitly signs off. This is an Owner-only step; automated tests must remain isolated from `clinicDb`.
- The route decision above is separate: keep `"reception"` denied unless the Owner explicitly approves opening it after smoke.

### `PAT-02` — read composition and preservation ready; panel wiring not authorized

- **Ready building blocks:** `DoctorCompositionRoot` composes `GetDoctorQueueHandler` and `GetPatientHistoryHandler`; `DoctorCompositionRootTest` is **2/2**, and `DoctorStationUiPreservationTest` is **3/3**. The preserved Doctor UI remains byte-identical to `6aabb85` per the wiring plan.
- **One Owner decision before wiring `DoctorStationPanel`:** approve a truthful mapping from patient-level `TienSuBenh` (`LoaiTienSu`, `MoTa`) into the existing eight encounter-history columns, **or** approve using/adding the correct encounter-history read query. These are different data shapes: do not fabricate encounter dates, rooms, services, or diagnoses, and do not silently relabel the table.
- **Gate:** Until that mapping/query and wiring scope are approved, keep the Doctor panel unwired. Any subsequent wiring must remain read-only, authorized/audited, off the Swing EDT, preserve the UI invariants, and receive its own acceptance.

### Owner checklist — Phase 1–2

- [ ] Run `./run-app.sh` against `clinicDb` and complete `desktop-owner-visual-smoke-checklist.md`.
- [ ] Record visual/interaction acceptance for the login/session/dashboard and reception flows; explicitly sign off or reject `SEC-01` and `REC-01`.
- [ ] Separately decide whether `"reception"` may be opened in `DashboardNavigationPolicy`; retain default-deny absent explicit approval.
- [ ] For `PAT-02`, approve the `TienSuBenh`-to-eight-column mapping backed by real source data, or approve a correct encounter-history query, before wiring `DoctorStationPanel`.

## 3. Phase 3 gate: `VIS-01` lifecycle/transfer and `APPT-01` appointments

### Existing domain/application building blocks

- `LuotKhamTest`: **23/23**
- `TransferReasonCodeTest`: **36/36**
- `AuditActionCodeTransferTest`: **3/3**
- `LichHenTest`: **1/1**

These are domain/test foundations, not proof of end-to-end persistence or workflow readiness. Appointment handlers/adapters and transfer lifecycle handlers/persistence acceptance are not complete; `SEC-01` remains an independent hard gate.

### `VIS-01` — Owner checklist before lifecycle/transfer migrations or handlers

**Visit cancellation and vitals decisions still open in the lifecycle decision record:**

- [ ] Define the trusted, transaction-consistent eligibility source for “no active service orders and no posted charges” before implementing cancellation; dependency failure or ambiguous data must deny. Preserve the approved boundary: only `RECEPTIONIST` may cancel a `CHO_KHAM` visit, with confirmation, reason, and audit.
- [ ] Decide cancellation reason semantics: fixed reason codes versus required free text, including privacy/validation rules. Do not infer the taxonomy from a recommendation.
- [ ] Decide whether repeated vitals are append-only measurements with history/revision or an overwrite of one current row.
- [ ] Decide whether `measuredAt` is clinic-local business time (`Asia/Ho_Chi_Minh`) or only UTC technical metadata is stored; retain UTC created/updated/audit metadata.
- [ ] Decide whether vitals may be read by `RECEPTIONIST` or only by `DOCTOR`; the approved DOCTOR-only write boundary remains unchanged.

**Transfer decisions already approved — do not reopen:** the six fixed reason codes and role limits; same-facility and role/department scope; eligible visit states; active destination room; new destination queue number without priority promotion; preservation of visit status and clinical timestamps; separate local `queueDate`; optional note policy N2; both immutable transfer history **and** successful security audit in the same transaction; and reauthorization plus the approved replay/conflict/timeout outcomes. Transfer remains default-deny.

**Transfer implementation decisions still open:**

- [ ] Approve the note’s physical storage/table, access, capacity/encoding/round-trip, and retention. Pin Unicode category/version behavior in the implementation contract; N2 approval does not authorize DDL.
- [ ] Approve queue persistence and cutover details: schema/DDL, indexes, counter/ledger and locking, high-water reconciliation, migration/cutover, and legacy allocation semantics. The shared `(roomId, queueDate)` sequence and no reuse of committed numbers are policy; the storage mechanism is not.
- [ ] Specify idempotency key format/scope/lifetime, canonical payload/version and note participation, receipt contents, duplicate wait/indeterminate behavior, commit resolution, and failed-attempt reservation/expiry.
- [ ] Identify trustworthy linked-work eligibility and handoff evidence, then approve transfer-specific security/workflow acceptance. Reason codes alone do not prove linked work is safe.
- [ ] Before enabling any transfer workflow, pass the separate authorization, atomicity/rollback, concurrency, queue-boundary, replay, and synthetic-data acceptance. No decision here grants access or authorizes runtime enablement.

### `APPT-01` — Owner checklist before appointment migrations or handlers

**Already approved:** `APPOINTMENT:XEM` is read-only, `TAO` creates in `CHO_XAC_NHAN`, and `SUA` edits/reschedules only. Cancellation, confirmation, check-in, no-show/reversal, and conversion to `LuotKham` remain DENY. Capacity is one appointment per doctor in the selected facility/department pair, intervals are half-open `[start,end)`, overbooking is denied, and a room is not an implicit capacity resource. Deposits, payments, refunds, billing, and patient self-service remain DENY.

- [ ] Specify required fields and appointment-source mapping.
- [ ] Decide positive duration/appointment types and working hours, breaks, and holidays.
- [ ] Decide which statuses and soft-deleted appointments consume capacity and when capacity is released.
- [ ] Decide reschedule limits and cancellation reason/cutoff policy.
- [ ] Decide check-in window and late-arrival handling; define no-show policy.
- [ ] Decide conversion preconditions and idempotency before creating/linking a `LuotKham`.
- [ ] Decide whether notifications are allowed and, if so, consent, recipient/channel, content, timing, and retry policy. No external notification is approved now.
- [ ] Approve time-zone persistence details while retaining `Asia/Ho_Chi_Minh` business time and UTC technical/audit metadata.
- [ ] Approve schema/audit design, concurrency strategy, and workflow/UI acceptance. Demonstrate capacity and lifecycle behavior with synthetic SQL Server tests before acceptance.

### Phase 3 implementation gate

- [ ] Resolve the applicable Owner decisions above before selecting migrations, persistence shapes, or handlers; do not infer missing policy from existing models, enums, grants, or historical proposals.
- [ ] Keep `VIS-01` transfer and `APPT-01` default-deny until `SEC-01`, implementation acceptance, and the relevant Owner workflow gates are satisfied.
- [ ] Use synthetic data for tests and acceptance. No worker test may connect to `clinicDb`.

## Evidence basis and limits

This summary consolidates `03-model-task-board.md`, `phase1-rec01-and-sec01-shift-completion-review.md`, `phase1-doctor-ui-preservation-and-wiring-plan.md`, `phase3-visit-lifecycle-owner-decisions.md`, `phase3-visit-transfer-decision-gaps.md`, `phase3-appointment-owner-decisions.md`, and `desktop-owner-visual-smoke-checklist.md`. The requested reactor totals and JAR-ready status are the `0b68800` snapshot (622 tests run, 6 opt-in skips) supplied for this task; the local JAR was absent at inspection, and no new test/build or live-database verification was performed.
