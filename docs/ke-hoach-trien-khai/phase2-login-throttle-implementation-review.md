# Phase 2 — Independent login-throttle implementation review

**Task:** `SEC-LOCKOUT-REVIEW-01`
**Review date:** 2026-11-06
**Scope:** Report-only review of the Owner-approved account throttle contract against the checked-out authentication, transaction, SQL Server persistence, composition, audit, and migration architecture. No implementation branch changes were inspected beyond the source present in this worktree.

## Conclusion

**[Policy]** The controlling policy is approved option B: five bad credentials within a 15-minute observation window do not incur delay; the next attempt starts a 30-second delay, subsequent bad attempts double the delay up to 15 minutes; there is no hard lockout; success resets the sequence; and 24 hours without another failure expires it. State is account-based in SQL Server, persisted in UTC, and required audit failure denies authentication. Disabled/deleted employees must not be reactivated.

**[Observed implementation]** The current authentication path has no throttle state or throttle decision. Credential lookup and verification occur outside the transaction used later to finalize a session. The transaction argument accepted by `AuthenticateNguoiDungHandler` is not retained or used. Existing invalid-login audit is persisted independently. Existing generic credential failures, role/scope selection, password handling, and successful-login finalization provide useful patterns, but they do not provide the required atomic throttle transition.

**[Recommendation]** Treat the credential check, throttle-state read/decision, password verification, failure/reset update, and required account-attributed audit as one serialized SQL Server transition. The final implementation must fail closed if that transition or its required audit cannot commit, and must not return an authenticated principal from stale or uncommitted state. Resolve the owner questions below before making edge semantics implicit.

## Labels and source precedence

- **[Policy]** means an explicit requirement in the supplied approved contract or the Owner-approved policy record.
- **[Observed implementation]** means behavior directly present in the checked-out source.
- **[Inference]** means a risk or consequence derived from those sources, not a further policy decision.
- **[Recommendation]** means an implementation/review action that preserves the approved policy.

**[Policy]** `phase2-login-lockout-policy-proposal.md` records Owner approval of option B on 2026-11-06 (lines 4, 31). The task-supplied approved contract v1 governs this review. The older `phase2-implementation-contract.md` still says login lockout is not approved (line 50); **[Inference]** that statement is stale relative to the newer approval and must not be treated as a veto or as a new policy decision. This review does not amend either source document.

## Findings

### P0 — Account state transition must be serialized across desktop clients

**[Observed implementation]** `AuthenticateNguoiDungHandler.handle` calls `getByUsername`, then verifies the password, without entering its `GiaoDichRanhGioi` (which is accepted but ignored by both transaction-taking constructors); see `AuthenticateNguoiDungHandler.java` lines 38–45, 57–81. `NhanVienRepository.getByUsername` is a normal query with no row lock (lines 49–53). The JDBC transaction manager keeps the active connection in a `ThreadLocal` (lines 14–15, 27–43), and the connection provider only reuses it on that thread (`JdbcKetNoiNhaCungCap.java` lines 12–20). The existing transaction in `CompleteDangNhapPhienHandler` is later in the flow, after password verification and role/scope choice (`CompleteDangNhapPhienHandler.java` lines 102–120).

**[Inference]** A process-local counter, an unlocked read followed by a separate update, or an update performed after password verification can lose simultaneous increments or allow concurrent requests to pass the same pre-throttle state. A later successful-login transaction does not close this race.

**[Recommendation]** Establish one SQL transaction per credential attempt and serialize per employee/account through a row-level lock/conditional update or equivalent SQL Server primitive. Keep the decision and the state mutation in that same transaction; handle updates affecting zero/multiple rows as unsafe persistence outcomes, not successful authentication. Recheck `hoatDong` and `isDeleted` inside the serialized attempt. Avoid sleeping while holding a database transaction/row lock; persist and enforce the eligibility time across clients instead of trusting a desktop timer.

### P1 — Threshold, observation-window, and delay boundaries need executable definitions

**[Policy]** The approved wording establishes five failures in 15 minutes, a first 30-second delay on the next attempt, exponential growth capped at 15 minutes, and expiry after 24 hours without another failure (`phase2-login-lockout-policy-proposal.md` lines 24, 31, 48; supplied contract acceptance 1–2).

**[Inference]** The source has no state model from which to infer whether the 15-minute window is rolling or fixed, how an event exactly on the 15-minute boundary is treated, whether repeated requests during an active delay count as “successive bad attempts,” or whether the 24-hour boundary expires at `>=` or only after 24 hours. “Attempt 6 starts at 30 seconds” also needs a precise transition: whether the 30-second wait is imposed before verifying that request or after its failed result. These interpretations can produce materially different behavior.

**[Recommendation]** Add fixed-clock acceptance cases around all boundaries and make assertions against the persisted next-eligible instant and public result. Use the contract’s exponential schedule through the cap (30, 60, 120, 240, 480, then 900 seconds, remaining at the cap) as a testable derivation, not as a new policy. Do not expose a countdown or remaining delay to the UI.

### P1 — Failure audit must be fail-closed and consistent with the throttle transaction

**[Observed implementation]** `AUTH_LOGIN_FAILED` and `AUTH_INVALID` exist (`KiemToanHanhDongMa.java` line 5; `KiemToanLyDoMa.java` line 4). Throttle has no dedicated reason code yet. `AuthenticateNguoiDungHandler` calls `appendIndependently` for invalid credentials (`AuthenticateNguoiDungHandler.java` lines 77–81, 119–138), and catches audit failures into `AUTH_UNAVAILABLE` (lines 61–68, 109–112). `JdbcKiemToanRepository` supports both current-transaction and independent inserts (lines 43–57); independent audit commits its own connection (lines 78–94). Its system-event insert persists structured action/reason and null detail rather than credential text (lines 103–129). V6 makes the audit tables append-only (`V6__Enhance_Audit_Tables_Append_Only.sql`, trigger definitions near the end).

**[Inference]** The current independent audit is fail-closed for authentication, but if the new counter is committed separately from that audit, an audit failure can leave throttle state changed without its required audit, or a state failure can leave a committed audit with no corresponding state transition. The contract requires audit of failure/throttle and no success when state cannot be updated safely.

**[Recommendation]** For known-account failures and throttle decisions, append the approved structured audit in the same transaction as the state transition, so either both commit or neither does. Continue to fail closed on insert/commit failure. Unknown usernames have no employee row to lock or update; preserve the generic response and use an anonymous audit identity without persisting the submitted username. Add the approved throttle reason/action metadata using controlled codes only—never a password, hash, username, delay value, or raw exception. Verify the exact existing database constraints and audit enum compatibility before choosing a code.

### P1 — Successful reset must align with the existing two-stage login flow

**[Observed implementation]** `AuthenticateNguoiDungHandler` returns `KetQua.success(DaXacThucNguoiDung)` after credentials and assignments are read, before any role/scope is selected (`AuthenticateNguoiDungHandler.java` lines 84–108). `DangNhapBieuMau` then asks for role and scope and can be cancelled before finalization (`DangNhapBieuMau.java` lines 149–163, 266–276). `CompleteDangNhapPhienHandler` revalidates the employee and assignments, records last login, and writes success audit within a transaction (lines 102–132).

**[Inference]** Resetting throttle state only in finalization means a correct password can be accepted by the credential handler but not clear the sequence if the user cancels selection; resetting earlier means the sequence is cleared before a completed session and its success audit. The contract’s phrase “successful authentication” does not choose between these existing milestones. Concurrent wrong attempts between the two handlers can also reorder the observed sequence unless the reset point is explicit and atomic.

**[Recommendation]** Obtain an Owner clarification on whether success means valid credential verification or completed role/scope/session establishment. Make the reset and any required audit atomic at that chosen milestone; do not change role, scope, session, or navigation behavior as part of this throttle task.

### P1 — Generic response should cover throttle without disclosing account state

**[Observed implementation]** Missing, inactive/deleted, and wrong-password cases share `AUTH_INVALID` and the same message in `AuthenticateNguoiDungHandler.java` lines 25–27 and 73–81. `DangNhapBieuMau` renders the `KetQua` message (lines 149–152). Empty fields are separately validated in the UI (line 129), before a username is submitted. Missing role/scope have distinct messages after credentials validate (handler lines 89–104), consistent with the existing role/scope flow rather than the four cases named by the throttle contract.

**[Inference]** The throttle must use the exact generic credential failure code/message used by the other listed cases; a new “wait N seconds,” “account throttled,” or account-specific response would violate the contract. The existing separate `AUTH_UNAVAILABLE` error is a storage/availability outcome, not one of the four credential outcomes, and should remain safe and free of raw exception details.

**[Recommendation]** Assert equality of both application error code and UI message for synthetic unknown, wrong-password, inactive, and throttled cases. Also assert no exception, username, delay, or account-existence detail leaks through diagnostics or the displayed response. Preserve the existing empty-field UX and post-credential role/scope selection unless separately approved.

### P1 — Disabled/deleted employees must stay denied; decide only necessary counter semantics

**[Observed implementation]** `NhanVienRepository.getByUsername` filters out soft-deleted employees (lines 49–53); `AuthenticateNguoiDungHandler` denies inactive/deleted rows before password verification (lines 73–81). The existing test covers missing, inactive, deleted, and incorrect-password results as the same public error (`AuthenticateNguoiDungHandlerTest.java` lines 87–146).

**[Policy]** The approved policy explicitly says not to reactivate disabled/deleted employees and forbids a manual unlock workflow (`phase2-login-lockout-policy-proposal.md` line 31).

**[Inference]** A throttle reset or migration must never modify `hoatDong`, `isDeleted`, or their administrative metadata. For deleted accounts the current lookup cannot recover an employee ID, so do not widen lookup visibility solely to create a throttle record or expose existence. Whether failed attempts against an inactive account should mutate throttle metadata is not specified by the approved contract.

**[Recommendation]** Preserve active/deleted filtering and assert those flags remain unchanged through throttle, expiry, successful-login, and migration paths. If inactive-account counter mutation would affect implementation behavior, ask the Owner rather than infer a policy.

### P2 — UTC must include comparison authority, not just stored representation

**[Policy]** The approved policy requires UTC metadata (`phase2-login-lockout-policy-proposal.md` lines 11, 40); the broader contract specifies UTC for technical timestamps and notes that SQL Server `DATETIME2` has no timezone (`phase2-implementation-contract.md` line 8).

**[Observed implementation]** The composition root supplies `Clock.systemUTC()` (`XacThucGhepNoiGoc.java` lines 25–34). Existing successful-login finalization converts `Clock.instant()` to UTC `LocalDateTime` before binding to `DATETIME2` (`CompleteDangNhapPhienHandler.java` lines 97–103; `NhanVienRepository.java` lines 248–258, 367–374). Elsewhere, ordinary employee updates use SQL Server `SYSDATETIME()` (`NhanVienRepository.java` lines 112–114).

**[Inference]** UTC storage alone does not ensure consistent decisions if each desktop clock is the authority for window/cooldown comparisons; clock skew can prematurely expire or extend a delay. A `DATETIME2` value is only UTC by convention and adapter discipline.

**[Recommendation]** Document one authoritative clock for persisted policy transitions and use it consistently for comparison and writes. Prefer a single SQL Server UTC instant per serialized state transition, or demonstrate that all clients use a sufficiently trusted/injected clock; do not mix local `SYSDATETIME()` and UTC values. Retain fixed-clock tests for the policy calculation and verify round-trip UTC interpretation in persistence tests. **[Owner question]** The contract does not explicitly identify whether SQL Server or application `Clock` is authoritative.

### P2 — Migration must be registered and tested as an upgrade, not only on an empty database

**[Observed implementation]** V1 creates `NhanVien` with existing employee, password, active/deleted, version, and audit fields but no throttle state (`V1__Initial_Clinic_Schema.sql` lines 101–128). The runner currently registers V1–V10 (`CoSoDuLieuDiCuChay.java` lines 43–54) and executes/records migrations transactionally (lines 64–93, 185–194). The fresh database SQL Server test requires a `clinic_test_*` database and checks V1–V10 (`MoiCoSoDuLieuDiCuSQLMayChuTest.java` lines 33–53 and subsequent assertions). V6 already added nullable audit metadata columns.

**[Inference]** A new script not added to the runner list will never run through the normal application migration path. Existing synthetic employee rows are not covered by an empty-database migration check. If code requiring new columns ships before the migration is applied, fail-closed behavior may become a login outage, so deployment ordering matters.

**[Recommendation]** Add a new forward-only, nullable/default-safe migration after V10; preserve all existing employee values and never rewrite V1 or seed behavior. Register it in the runner, update migration record/schema expectations, and test both fresh migration and upgrade of a populated synthetic database with exact pre/post employee comparisons. Verify repeat-run behavior and schema compatibility. Run runtime acceptance only against an explicitly named `clinic_test_*` SQL Server database; this review made no database connection.

## Risk-ranked implementation/review checklist

1. **P0 — Concurrency:** Use a single SQL Server-serialized per-account transition across separate client transactions. Prove concurrent failures cannot lose increments, and concurrent valid/invalid credentials follow a serial order without bypassing active throttle or reactivating a disabled row.
2. **P1 — State machine and time:** Cover attempts 1–5, attempt 6, exponential delay and 15-minute cap; exact 15-minute and 24-hour boundaries; active-delay behavior; reset after success; UTC persistence/round-trip; SQL failures and lock timeouts. Inject a fixed clock for deterministic policy tests.
3. **P1 — Audit atomicity and privacy:** For known accounts, verify throttle-state and failure/throttle audit commit or roll back together. Reject success if audit/state update fails. For unknown usernames verify anonymous metadata only. Assert no credential, hash, submitted username, raw exception, or remaining delay appears in audit.
4. **P1 — Public result:** Compare application result and UI message for unknown, wrong password, inactive, and throttled attempts. Verify no remaining-delay disclosure; preserve separate storage-unavailable behavior and existing role/scope selection behavior.
5. **P1 — Success and disabled accounts:** Test the chosen success/reset milestone and concurrent interleavings. Verify inactive/deleted accounts stay denied and their administrative status fields are unchanged. Resolve whether inactive attempts alter throttle metadata only if the implementation needs that rule.
6. **P2 — Migration:** Verify fresh and populated-database migration on synthetic `clinic_test_*` databases, existing-row preservation, migration registry entry, and persistence after process/connection restart. No production database or `clinicDb`.
7. **P2 — Integration gate:** Run targeted unit tests, SQL Server opt-in persistence tests with the exact database name recorded, then the contract-required root `mvn -Djava.awt.headless=true clean verify` and `git diff --check`. Do not claim SQL/runtime acceptance without actually running it.

## Owner questions (no policy decisions made here)

1. Does the 15-minute observation window roll from each failure or use a fixed window, and how are exact 15-minute/24-hour boundaries handled?
2. Does attempt 6 wait 30 seconds before credential verification or does its failed result start the wait? Do requests during an active delay count toward subsequent doubling, and is a correct password subject to the active delay?
3. Does “successful authentication” reset state after password verification or only after role/scope selection and session finalization?
4. Is SQL Server UTC time or the injected application `Clock` the authority for comparisons across clients?
5. Should attempts against inactive employees update throttle state, or are they simply denied/audited without changing it?

## Owner resolution — 2026-11-06

The Owner approved the review's proposed implementation semantics on 2026-11-06: use a fixed 15-minute window from the first failure; the request after five failures waits before password verification; requests while delay is active are audited without increasing the delay; a correct password for an active account clears throttle immediately after verification even if role/scope selection later denies login; SQL Server UTC time is authoritative. Inactive/deleted accounts are not reset/reactivated. These decisions supersede the related open questions above for implementation and are recorded in approved Orchestrator contract `c67f6998-c9d4-402a-9fd0-008f2616c9eb`, which governs the resumed task.

## Guidance and sources read

**Mandatory task-supplied guidance reviewed:** `docs/PROJECT_CONVENTIONS.md`, `README.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, and `UI_CODING_STANDARDS.md`. The task supplied excerpts. No repository `AGENTS.md`, `docs/PROJECT_CONVENTIONS.md`, `docs/DEVELOPMENT_CONVENTIONS.md`, or `docs/README.md` copy was found by the guidance-file search.

**Repository guidance and policy documents read:** `docs/ke-hoach-trien-khai/README.md`; `docs/ke-hoach-trien-khai/phase2-login-lockout-policy-proposal.md`; `docs/ke-hoach-trien-khai/phase2-implementation-contract.md`.

**Authentication/application sources:** `clinic-application/src/main/java/vn/clinicmanagement/application/auth/AuthenticateNguoiDungHandler.java`; `CompleteDangNhapPhienHandler.java`; `clinic-application/src/main/java/vn/clinicmanagement/application/audit/KiemToanHanhDongMa.java`; `KiemToanLyDoMa.java`; `KiemToanSieuDuLieuKiemTra.java`.

**Desktop/persistence/migration sources:** `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/XacThucGhepNoiGoc.java`; `DangNhapBieuMau.java`; `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepository.java`; `JdbcGiaoDichQuanLy.java`; `JdbcKetNoiNhaCungCap.java`; `CoSoDuLieuDiCuChay.java`; `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/system/JdbcKiemToanRepository.java`; `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql`; `V6__Enhance_Audit_Tables_Append_Only.sql`.

**Relevant tests read:** `clinic-application/src/test/java/vn/clinicmanagement/application/auth/AuthenticateNguoiDungHandlerTest.java`; `clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/MoiCoSoDuLieuDiCuSQLMayChuTest.java`; `KiemToanSQLMayChuLuuTruTest.java`; `clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienDangNhapKiemToanTinhNguyenTuTest.java`.

## Review-only validation

No Java, SQL, schema, test, database, task-status, or policy files were changed. No tests or database runtime acceptance were run because this task is report-only and explicitly forbids database connection. No push, pull request, or merge was performed.
