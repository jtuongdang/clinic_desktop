# Restored Desktop: Design Baseline and Workflow Coverage

**Review scope:** read-only comparison of `6aabb85` (original/main baseline), `63c6d71` (pre-restoration feature tree), and `982c1f6` (restored desktop tree). This report is not a visual acceptance result. The Tech Lead launched a separate build of `6aabb85` for the Owner to inspect. The worker performed static review.

## Findings

The original design baseline is the desktop source at `6aabb85`. The restored `982c1f6` login, dashboard, reception, and doctor panel source files are byte-identical to that baseline. This preserves the original composition, but it also restores the earlier, mostly demonstration-oriented wiring: the feature-tree desktop composition roots, session lifecycle, and a substantial desktop test suite are absent. The application, domain, and infrastructure trees (including their tests) are unchanged between `63c6d71` and `982c1f6`; retaining those backend capabilities does not mean the restored screens call them.

The source is the authority for what the baseline actually renders. `UI_CODING_STANDARDS.md` is the intended design/acceptance constraint, but it describes some elements beyond what the baseline source currently implements. For example, the standard describes a three-column doctor workspace and login role/scope selectors; the source has a two-pane doctor workspace and a basic credential form. Preserve the current source layout unless the Owner explicitly approves a redesign; do not mistake aspirational documentation for an already-rendered baseline.

## Stable original design by screen

| Area | Baseline elements to keep stable | Source evidence and qualification |
|---|---|---|
| Login | Fixed 440×390, centered, non-resizable white window; centered EHC heading/subtitle; username then password; one prominent primary login button; clear vertical grouping and focus/keyboard flow. | `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java:31-98` at `6aabb85` (same in `982c1f6`). The code does **not** render a logo or role/scope selectors, although the standards describe them at `UI_CODING_STANDARDS.md:186-200`. |
| Dashboard / shell | Maximized role-aware shell; north Ribbon; center `CardLayout` workspace; south MDI tab strip above the status bar; Ribbon selection follows the active tab. Keep the existing top/center/bottom hierarchy and avoid replacing it with independent feature windows. | `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java:48-98` and the Ribbon builders from `:100` onward; design contract `UI_CODING_STANDARDS.md:159-180`. The standard calls the status bar nine-cell; the source installs `StatusBarPanel` at `:72-73`. |
| Reception | `BaseViewPanel` view identity; top action toolbar; vertically split intake form above room-status tables (split at 218 px); room selection highlights the corresponding room; bottom shortcut bar. Preserve the form/status relationship and footer, not merely the form fields. | `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java:20-54`; `UI_CODING_STANDARDS.md:369-387`. |
| Doctor | `BaseViewPanel` treatment view; clinical action bar at top; left queue beside right work area (split at 250 px); patient tab with clinical summary/form and history; the five existing tabs, including their explicit placeholder states. | `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java:22-35,83-145`; component roles in `UI_CODING_STANDARDS.md:223-269`. **Actual source is two-pane, not the three-column design described at `:223-229`.** |

Cross-screen invariants from `UI_CODING_STANDARDS.md`: use `AppFonts` and `UIConstants`, use the Ribbon/`BaseViewPanel`/`WorkspaceManager` patterns for views, keep the MDI tabs and status bar, avoid emoji as primary icons, fit 1280×800 and 1440×900, keep long work off Swing's EDT, and preserve natural keyboard navigation and popup dismissal (`:54-62,66-150,646-657`). The baseline contains emoji button icons in `MainDashboardFrame`; therefore, this is a documented pre-existing mismatch, not a reason to silently redesign the Owner's visual baseline.

## Runtime and data wiring: retained versus absent

### Retained outside the desktop module

The `clinic-application`, `clinic-domain`, and `clinic-infrastructure` trees are unchanged from `63c6d71` to `982c1f6`. They retain use cases and persistence adapters for authentication/session completion, authorization/navigation permissions, patient registration/search, reception queues/rooms, doctor queues, patient history, and audit. Their application/domain/infrastructure tests also remain. Examples include `RegisterPatientHandler`, `SearchPatientsHandler`, `GetReceptionQueueHandler`, `GetActiveReceptionRoomsHandler`, `GetDoctorQueueHandler`, `GetPatientHistoryHandler`, and the patient, facility, reception, identity, and audit JDBC repositories. These are backend capabilities; their existence is not proof of desktop integration.

### Restored runtime limitations and removed desktop integration

At `63c6d71`, `ReceptionCompositionRoot` assembled patient, insurance, facility, room, queue, audit, authorization, registration, search, and queue/room query services. `DoctorCompositionRoot` assembled the doctor queue and patient-history readers with authorization and audit. The corresponding roots are deleted at `982c1f6`. In the restored source, `ReceptionPanel.refreshData()` is empty (`ReceptionPanel.java:56-64`), while `DoctorStationPanel` initializes a hard-coded `PatientExamDto` list (`:30-80`) and shows placeholder content in four of its five tabs (`:120-124,139-143`).

The pre-restoration login completed a role and work-scope selection, invoked `CompleteLoginSessionHandler`, loaded navigation permissions, and started `SessionLifecycleService` before opening the dashboard (`63c6d71` `LoginForm.java:155-234`). The restored login creates only `AuthenticateUserHandler` through `AuthenticationCompositionRoot` and writes the returned user to the simpler `UserSession` (`982c1f6` `LoginForm.java:25,100-136`). `SessionLifecycleService`, `SessionLockOverlay`, and `DashboardNavigationPolicy` are deleted; the restored dashboard accepts a username/role and chooses its Ribbon from a role-name check. Thus backend session, authorization, and workflow services remain available, but this desktop path no longer demonstrates the completed-session lifecycle, scoped navigation, lock/logout clearing, or reception/doctor reads and writes.

The restore also removes `DesktopWorkerDiagnostics` and changes startup fallback in `Main.java` from narrow reflective/FlatLaf failure handling to `catch (Throwable)`. The pre-restoration `clinic-desktop/pom.xml` explicitly excluded `db.properties` from the shaded artifact; that exclusion is absent in `982c1f6`. These are bounded runtime-hardening items to review before considering the desktop restored end-to-end.

### Removed desktop test evidence

The diff removes **21 desktop test files**. Notable losses are `SessionLifecycleServiceTest` (415 lines), `DoctorStationPanelTest` (742), `ReceptionPanelSessionListenerTest` (600), `ReceptionSqlServerPersistenceTest` (853), `ReceptionSqlServerProcessRestartTest` (322), and `PatientRegistrationFormPanelTest` (584), plus login/worker diagnostics, navigation policy, session-lock, doctor history/composition, reception draft/date/message/protected-read, shortcut, and look-and-feel tests. This removes evidence for wiring, session behavior, form handling, restart persistence, and worker/UI boundaries—not the underlying application and persistence implementation.

The two removed SQL Server reception tests are historical opt-in integration evidence, not permission for ordinary tests to target `clinicDb`. Any replacement must preserve the approved separation: default tests use synthetic/in-memory data; SQL opt-in tests target only a dedicated `clinic_test_*` or `clinic_orchestrator_review_*` database. `clinicDb` remains reserved for explicit Owner-run desktop smoke.

## Old board and report evidence: treat as snapshots

`docs/ke-hoach-trien-khai/03-model-task-board.md` is identical at `63c6d71` and `982c1f6`; its unchanged contents do not attest to the restored source. In particular:

| Board evidence at `63c6d71` | What remains usable | What must be re-established against `982c1f6` |
|---|---|---|
| `SEC-LOGIN-01`, `SEC-UI-01`, `SEC-AUDIT-01B`, `SEC-REVIEW-UI-01` (board lines 90-112) | Application authentication, authorization, navigation-permission, and audit handlers/tests remain. | Login role/scope completion, permission-driven module visibility, session lifecycle, and audit lifecycle wiring in the desktop. |
| `REC-UI-DB-01`, `REC-UI-TEST-01`, `PAT-UPDATE-UI-TEST-01` (lines 122-127) | Registration/search handlers, repositories, and backend tests remain. | Form-to-handler-to-database integration and the deleted headless form tests. `ReceptionPanel.refreshData()` is currently empty. |
| `PAT-HISTORY-UI-01`, `PAT-DOCTOR-SESSION-LISTENER-01`, `PAT-DOCTOR-UI-01` and related doctor queue/history rows (lines 120, 143-146) | Queue/history handlers, JDBC reads, authorization/audit logic, and backend tests remain. | Doctor queue/history panel wiring, session replacement/lock clearing, async loading/race behavior, and the deleted doctor UI tests. |
| `PAT-01`, `REC-01`, `PAT-DOCTOR-QUEUE-01` and SQL persistence rows (lines 118-142) | Reassess backend-only portions from the still-present source and tests; those do not become false merely because UI wiring was removed. | Any evidence whose acceptance relied on invoking the old desktop composition or form. Keep row-level UI and backend completion separate. |
| Board test summary and open visual gate (lines 195-199) | Historical report of the pre-restoration tree; it explicitly says reception visual/interaction smoke remained open. | A fresh run on the restored commit. The pre-restoration Maven results, including the later 443-test snapshot with six opt-in skips, apply to earlier revisions. |

The pre-restoration `phase1-doctor-ui-visual-smoke-runbook.md` likewise describes the old session-aware doctor path; it explicitly says the runbook itself is not evidence that smoke ran. Reconcile its source references and acceptance matrix after wiring is restored. Do not treat either the board or runbook as proof of current visual acceptance. The manual visual comparison remains open.

## Owner visual comparison checklist

Compare screenshots of the `6aabb85` baseline and a future candidate in the same environment, with the same window size, display scaling, Look & Feel, and synthetic account/fixture state. Use 1280×800 and 1440×900. Record only pass/fail and screen region/difference category; do not capture or transcribe credentials, real patient information, or clinical narratives. No database or application launch is part of this source review.

- [ ] **Login:** 440×390, centered/non-resizable window; heading, subtitle, field order, spacing, primary button and focus are consistent. Tab order is natural; Enter behavior, loading/error feedback, and popup dismissal work. Record any newly added role/scope UI as an intentional approved difference, not as baseline.
- [ ] **Shell/dashboard:** role-appropriate Ribbon, workspace center, bottom MDI tabs, and status bar retain their locations and hierarchy. Switch/open/close tabs; confirm Ribbon selection follows the active tab and the shell does not clip at either target size.
- [ ] **Reception:** toolbar, top intake form, lower room-status area, selected-room highlight, and footer/shortcut strip remain in the baseline order and relative proportions. Check empty/loading/error states and keyboard flow with synthetic fixtures only.
- [ ] **Doctor:** action bar, left queue, right work area, selected patient summary/history, and five tab labels/states retain the source baseline structure. Check queue selection and placeholder states without displaying or recording real patient data. Do not accept the standards' three-column proposal as a silent change to this two-pane baseline.
- [ ] **Cross-screen:** typography uses `AppFonts`; colors/spacing use `UIConstants`; important icons are not platform emoji; text and controls fit without clipping; long operations do not freeze the UI; tab, Enter, Escape, and focus behavior are predictable.
- [ ] **Decision:** mark visual acceptance only after the Owner compares both captures and records each difference as either accepted intentional change or regression. Compilation/headless tests alone do not satisfy the visual gate.

## Bounded follow-up

1. **Restore desktop composition, without changing layout:** reconnect login to completed role/scope session and permission-based navigation; reattach reception registration/search/room/queue actions; reattach doctor queue/history reads and session-clear behavior. Preserve the current source geometry and asynchronous EDT boundaries.
2. **Restore synthetic regression coverage:** replace/adapt the removed session, navigation, reception-form/wiring, doctor queue/history, and worker-race tests. Keep default Maven/CI tests independent of SQL Server and `clinicDb`; explicitly guard all opt-in targets.
3. **Reconcile evidence after implementation:** update the board/runbook to separate backend acceptance from desktop integration; rerun the applicable tests on the candidate and obtain Owner visual comparison before closing UI gates.
4. **Review restore-specific runtime hardening:** retain the shaded-jar `db.properties` exclusion, narrow startup error handling rather than swallowing `Throwable`, and restore bounded worker diagnostics where appropriate.

No application source, tests, configuration, or database were changed for this report.

## Guidance and evidence sources

Guidance supplied and read: `docs/PROJECT_CONVENTIONS.md`, `README.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `docs/ke-hoach-trien-khai/README.md`, `UI_CODING_STANDARDS.md`, `docs/ke-hoach-trien-khai/01-ma-tran-model-man-hinh.md`, `docs/ke-hoach-trien-khai/00-hien-trang.md`, and `PROJECT_COMPLETENESS_REPORT.md`.

Mandatory project RAG retrieval was unavailable in this session (no RAG connector was exposed). Per the task instruction, this review used the supplied RAG excerpts and tracked sources. Source comparisons were read-only across the three commits. The worker performed source review only. The Orchestrator Maven validation ran on `982c1f6` and failed at the existing three Desktop authentication compilation errors. The worktree contained only this new Markdown file. The Tech Lead checked its source and scope before local integration.
