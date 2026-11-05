# Phase 0: remaining Swing error handling audit

> **Report-only audit:** current checkout on `feature/patient-reception-workflow`. No production code or tests were changed. Scope was `clinic-desktop/src/main/java`; completed reception, startup, `ExaminationPanel`, authentication-handler diagnostics, and DOCX save-error fixes were treated as already implemented, not as current defects.

## Conclusion

One confirmed gap remains at the desktop asynchronous failure boundary: several `SwingWorker` completion handlers show fixed, user-safe messages but discard unexpected exception details without restricted diagnostics. This is distinct from the completed authentication-handler logging: it affects exceptions that escape the handler or occur in desktop orchestration.

No current defect was found in the login form's display of the existing `Result` messages: the UI does not show stack traces or arbitrary caught exception text. No other in-scope, wired user action was found to map application `Result` failures or exception details into an unsafe dialog. Several other panels are presentation-only in this checkout and have no corresponding application error boundary to audit.

## Finding

| ID | Severity / classification | Source | Current observable behavior and risk | Safe bounded remediation | Focused regression-test proposal |
|---|---|---|---|---|---|
| SWING-ERR-01 | **Medium — confirmed diagnostic gap** | `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java:166-167, 238-241`; `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java:218-231` | Both login workers catch `ExecutionException` and show a fixed generic dialog; dashboard reauthentication catches `Exception` and sets a fixed generic overlay message. These paths do not log the cause, so an unexpected exception escaping the application handlers or thrown during desktop orchestration is not diagnosable from the desktop boundary. In `MainDashboardFrame`, the broad catch also handles `InterruptedException` without restoring the interrupt flag. Current user-visible text remains generic and does not expose exception details. The completed authentication handlers' own diagnostics do not cover failures that escape their calls or arise in the surrounding Swing worker/completion code. | Before discarding an unexpected failure, emit a bounded restricted diagnostic using the project's existing safe-diagnostic pattern: operation/result code, exception type, and capped stack frames only; do not log `Throwable.getMessage()`, `toString()`, credentials, or user/session payloads. Keep the existing fixed UI messages. Handle `InterruptedException` separately in dashboard reauthentication and restore the interrupt flag. | With synthetic exceptions containing a unique marker, exercise both login completion paths and dashboard reauthentication. Assert the user-facing result remains the existing fixed message, diagnostics are emitted with bounded safe context but never contain the marker or exception message, and the dashboard interruption path restores the flag and clears its busy state. |

## Already-safe behavior and exclusions

- `LoginForm` displays the `ApplicationError.message` for failed `Result`s at `LoginForm.java:147-151, 204-210`; it does not display exception text from its `ExecutionException` catches. The task board records the authentication handler's stable safe results and restricted diagnostics as complete (`docs/ke-hoach-trien-khai/03-model-task-board.md`, `PLAT-ERROR-AUTH-01`). This audit found no current unsafe result message; preserve that contract and test it when changing UI mapping.
- `MainDashboardFrame.java:224-230` gives reauthentication failures a fixed generic message rather than exposing the underlying failure.
- `SessionLifecycleService.java:236-238, 399-409` reports audit-write failure through a safe diagnostic and returns a generic reauthentication failure; this completed behavior is not raised as a defect.
- `DocxManager.java:41-45, 66-70, 75-77` translates save `IOException`s to a fixed user message. `DocxManagerTest` verifies path and OS detail redaction. The task board records `PLAT-ERROR-DOCX-01` complete. The export entry points have no production call sites in the current desktop source search, so possible builder failures were not presented as a current user-observable defect.
- `HomeWelcomePanel.java:34-56` treats the optional hero-image load as a best-effort decoration and paints a vector fallback when loading fails; this is intentional fallback behavior, not a user-action failure.
- `BillingInvoicePanel` and `SystemConfigPanel` currently present prototype interactions rather than wired application operations; no missing `Result`/exception translation finding is inferred from those views.
- The historical `phase0-error-mapping-review.md` is explicitly a snapshot and was not reused as evidence of current defects. Its reception findings are excluded because the relevant fixes are complete.

## Guidance and validation

Guidance read: task-supplied `docs/PROJECT_CONVENTIONS.md`, `README.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, and `UI_CODING_STANDARDS.md`; repository `docs/ke-hoach-trien-khai/README.md`, `00-hien-trang.md`, `03-model-task-board.md`, and `phase0-error-mapping-review.md`. A repository `AGENTS.md` was searched for and not present.

Source checks used targeted searches of `clinic-desktop/src/main/java` for `Result`, `catch`, exception-message forwarding, Swing error dialogs, and diagnostic logging; no database, patient data, secrets, or external service was accessed.

`mvn -pl clinic-desktop -am -Djava.awt.headless=true verify` completed successfully (exit code 0) using OpenJDK 27.0.1. No JDK 17 executable was available on `PATH`, so the JDK 17-specific acceptance criterion remains unverified. `git diff --check` passed; a trailing-whitespace search found no matches. The only changed path is this report. No dependency cache was copied, moved, or deleted.
