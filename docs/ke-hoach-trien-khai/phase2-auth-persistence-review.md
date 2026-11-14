# Phase 2 authentication and persistence review

**Review scope:** Current Java implementation of authentication, password verification, employee/role persistence and wiring, transaction handling, authorization/session/audit evidence, and the related domain and Phase 2 documentation. This is a source review; no Java code or policy was changed.

## Executive assessment

Authentication is wired from the desktop login form through an application handler, PBKDF2-HMAC-SHA256 verification, an employee JDBC repository, and a JDBC transaction that records successful login time. Unit tests cover handler/verifier behavior using synthetic values. This supports only a login foundation. The Phase 2 gate remains unmet: no application-level permission or data-scope enforcement, trusted authorization context, or demonstrated sensitive-access audit path was found. There are also persistence-level gaps around selected roles, account-state races, and auth-specific integration evidence.

## Verified behavior

| Area | Evidence and observed behavior |
| --- | --- |
| Login use case | `clinic-application/src/main/java/vn/clinicmanagement/application/auth/AuthenticateNguoiDungHandler.java` — `AuthenticateNguoiDungHandler.handle` trims the username via `AuthenticateNguoiDungCommand`, loads an employee, rejects missing, inactive, or soft-deleted records, verifies the password, requires at least one nonblank role name, then returns `DaXacThucNguoiDung`. Missing, inactive, deleted, and wrong-password cases share `AUTH_INVALID` and a generic message; role absence and infrastructure failures have separate messages/codes. |
| Password handling | `clinic-application/src/main/java/vn/clinicmanagement/application/auth/AuthenticateNguoiDungCommand.java` — `AuthenticateNguoiDungCommand` copies the supplied `char[]`, exposes copies, and clears its owned buffer on close. `AuthenticateNguoiDungHandler.handle` clears its working copy and closes the command in `finally`. `clinic-application/src/main/java/vn/clinicmanagement/application/auth/PBKDF2MatKhauBoBam.java` — `PBKDF2MatKhauBoBam.verify` accepts the `pbkdf2_sha256$iterations$salt$hash` format, enforces 100,000–1,000,000 iterations, 16–64 byte salts and a 256-bit expected digest, and compares derived and expected digests with `MessageDigest.isEqual`. It fails closed for malformed/unsupported input and clears the `PBEKeySpec` password. |
| Employee and role lookup | `clinic-domain/src/main/java/vn/clinicmanagement/domain/repositories/INhanVienRepository.java` — `INhanVienRepository` defines username lookup, role-name lookup, and successful-login recording. `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepository.java` — `getByUsername` uses a parameterized case-insensitive username query and filters `isDeleted=0`; `getRoleNames` joins employee-role and role tables and filters soft-deleted join/role rows; `recordSuccessfulLogin` updates `dangNhapCuoi` only if the employee remains active and not soft-deleted. |
| Persistence mapping | `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql` — `NhanVien` contains nullable username and password hash columns, an active flag and login timestamp; username is unique. `VaiTro`, `NhanVienVaiTro`, `PhanQuyenCoSo` and `MaTranPhanQuyen` tables exist with foreign keys and soft-delete metadata. `clinic-domain/src/main/java/vn/clinicmanagement/domain/identity/NhanVien.java` maps the account fields, while `VaiTro`, `NhanVienVaiTro`, `PhanQuyenCoSo` and `clinic-domain/src/main/java/vn/clinicmanagement/domain/system/MaTranPhanQuyen.java` are plain domain entities. |
| Composition and transaction | `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/XacThucGhepNoiGoc.java` — `createHandler` wires `JdbcUnitCuaWork`, `JdbcGiaoDichQuanLy`, `JdbcKetNoiNhaCungCap`, `NhanVienRepository`, and `PBKDF2MatKhauBoBam`. `AuthenticateNguoiDungHandler.handle` calls `GiaoDichRanhGioi.inTransaction` for `recordSuccessfulLogin` after the reads and password verification. `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcGiaoDichQuanLy.java` — `inTransaction` shares a thread-bound connection, commits successful results, rolls back failures/exceptions, and clears thread-local state. The auth transaction therefore covers the login timestamp write, not the preceding account/role reads. |
| UI session | `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/DangNhapBieuMau.java` — `onLogin` obtains and clears the UI password array, invokes the handler on a `SwingWorker`, then writes user ID, username, full name and role display name into `NguoiDungPhien`. `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/NguoiDungPhien.java` — `NguoiDungPhien` is a mutable process-wide singleton with public setters and a `clearSession` method. |
| Existing auth tests | `clinic-application/src/test/java/vn/clinicmanagement/application/auth/AuthenticateNguoiDungHandlerTest.java` covers generic invalid-credential results, active/deleted state, no-role rejection, transaction invocation, repository/transaction failures, and command/verifier buffer clearing with synthetic data and test doubles. `clinic-application/src/test/java/vn/clinicmanagement/application/auth/PBKDF2MatKhauBoBamTest.java` covers deterministic synthetic vectors, wrong/tampered passwords, malformed encodings, salt length, and iteration bounds. |
| Project status/docs | `docs/ke-hoach-trien-khai/02-cac-dot-trien-khai.md` marks Phase 2 incomplete and names trusted actor/session, account-derived role/scope, application and persistence checks, audit, and allow/deny tests as requirements. `docs/ke-hoach-trien-khai/phan-he/08-co-so-bao-mat-cau-hinh.md` records login wiring but leaves password/session policy, centralized checks, audit, and access administration open. `README.md` identifies seeded demo credentials as demo-only and instructs rotation before real data use. `DOMAIN_ANALYSIS.md` and `ERD_DIAGRAM.md` describe identity, role, scope and permission models; these descriptions establish modeled concepts, not implemented enforcement. |

## Gaps and security-sensitive risks

These are observations about current evidence, not a choice of authorization policy.

1. **No demonstrated authorization enforcement.** A source search found no application service consuming `MaTranPhanQuyen`, `HanhDongQuyen` or `PhamViDuLieu` to allow/deny use cases, and no permission/scope check at the persistence boundary. `NguoiDungPhien` exposes a role string, but that alone does not enforce access. Hiding or disabling UI controls cannot establish the Phase 2 gate.
2. **Role selection is ambiguous.** `getRoleNames` returns every non-deleted role ordered by `vt.id`, and `AuthenticateNguoiDungHandler.handle` selects the first nonblank name. With multiple assigned roles, the selected role is an implementation-order artifact. The schema/domain has no explicit active flag on `VaiTro`; only soft-delete status is checked. Do not treat this behavior as an approved precedence or role policy.
3. **Read/write race and incomplete snapshot.** Username and role reads happen outside `GiaoDichRanhGioi`; account/role assignments may change between verification, role lookup, and successful-login recording. The timestamp update rechecks employee active/deleted state but does not validate the role assignment or return a current authorization snapshot. Transaction isolation/locking requirements are unspecified.
4. **Authentication fetches excess employee data.** `getByUsername` uses the broad `SELECT_COLUMNS` list, which includes contact/address and compensation fields as well as `passwordHash`, although the login decision needs a smaller credential/account projection. This expands sensitive-data exposure in memory and the query result.
5. **No failure throttling or lockout evidence.** No failed-attempt counter, delay, lockout, or related policy is implemented in the reviewed login flow/schema. Whether/how to apply throttling is a business/security policy decision; an implementation task should wait for that decision rather than invent thresholds.
6. **Different timing paths may reveal account state.** Missing, inactive and deleted accounts skip PBKDF2, while an active account with an incorrect password performs it. The visible error text is generic, but execution time may differ. Existing unit assertions do not measure timing or test a dummy-hash strategy.
7. **Password lifecycle is incomplete in the reviewed auth surface.** `PBKDF2MatKhauBoBam` verifies only; no creation, reset, change, or rehash-on-login workflow is present in the reviewed auth package. The accepted encoding bounds are not demonstrated as a product-wide password policy, and the upper iteration bound is a format bound, not proof of deployment performance suitability.
8. **Mutable session is not a server-side authorization boundary.** `NguoiDungPhien` allows arbitrary setters for identity and role and has no expiry, revalidation, or authorization checks. This may be adequate as desktop UI state but cannot establish trusted actor identity for sensitive application operations. Logout/session invalidation behavior is not connected to an authorization lifecycle in the reviewed flow.
9. **Audit trail is not connected to authentication/sensitive reads.** `recordSuccessfulLogin` stores only `dangNhapCuoi`; no evidence was found of an auth event with outcome or of EMR view/change, prescription/dispensing, billing, or permission-change audit writes. `NhatKyHeThong` and `NhatKyHoSoBenhAn` exist in the model/schema, but table existence is not proof of a write path, attribution, integrity, or review controls.
10. **Auth persistence lacks focused integration evidence.** The located authentication tests use fakes/proxies; no `NhanVienRepository` SQL test or authentication-specific SQL Server persistence test was found. Thus real collation behavior, schema mapping, role joins, account-state race behavior, and commit/rollback of `dangNhapCuoi` are not covered by the cited unit evidence.
11. **No username/hash lifecycle constraints are demonstrated beyond current DDL.** `username` and `passwordHash` are nullable in V1 (username uniqueness applies to non-null values in SQL Server), and application creation/reset paths were not in review scope/evidence. Current login rejects blank command usernames, but enrollment, normalization/case uniqueness, and malformed/missing stored hash handling need persistence-level tests and explicit lifecycle requirements.

## Missing test evidence

Existing tests establish useful application-level behavior, but do not show:

- `NhanVienRepository.getByUsername`, `getRoleNames`, and `recordSuccessfulLogin` against SQL Server test schema, including deleted join/role rows, case behavior, and missing/disabled rows.
- Authentication transaction persistence: successful timestamp commit; failed timestamp write rollback; failure result / thrown repository exception rollback; reuse of the shared transaction connection across repository calls.
- A concurrent account disable, soft delete, password change, or role removal between credential verification and session establishment, with expected behavior defined first.
- Deterministic behavior for multiple roles, duplicate role memberships, null/blank role values, or the intended active-role semantics. Tests must follow a product-approved policy, not codify current ID ordering by accident.
- Authorization allow/deny tests for each sensitive application command/query and data scope at the relevant persistence boundary, once role/action/scope policy is approved.
- Audit tests proving actor, action, target reference, outcome and timestamp for login outcomes and sensitive record operations, without recording passwords, hashes, tokens, or unnecessary patient data.
- Session lifecycle tests for login, logout, failed login, stale/revoked account or role state, and prevention of caller-set identity being trusted by application use cases.
- Timing/throttling tests once the security policy is agreed; avoid brittle wall-clock assertions and test observable counters/delay state where appropriate.
- Password lifecycle tests for credential creation/reset/change, malformed legacy hashes and any migration/rehash policy if those workflows are introduced.

## Small implementation tasks (policy-neutral)

1. Add a narrow credential lookup projection (account ID, username, hash, active/deleted state and required display fields) instead of hydrating payroll/contact fields during login; test mapping using synthetic data.
2. Add infrastructure tests for the existing employee-auth SQL statements and transaction commit/rollback with a disposable synthetic SQL Server database or isolated JDBC test harness consistent with repository conventions.
3. Define an application authorization port and a single use-case guard that accepts actor, requested operation and resource context; keep role/action/scope decisions injectable/configured until product owners approve policy. Add allow/deny contract tests with synthetic identities and resources.
4. Carry immutable authenticated identity/context into application operations rather than treating mutable UI fields or role display labels as proof of authorization. Add tests that unauthenticated or fabricated contexts are rejected at the application boundary.
5. Define an audit port and event shape with privacy-minimal identifiers/outcomes; connect login and sensitive commands/queries only after deciding which events and access rules are required. Test completeness and absence of credential/patient payload leakage.
6. Ask product/security owners to specify multi-role resolution, account/session revocation, failed-login throttling, and password create/reset/change/rehash policies. Then capture those decisions in tests before implementing their behavior.

## Assumptions and boundaries

- This report treats code and checked-in docs as the evidence base; it does not infer uninspected runtime deployment settings or database behavior.
- “Active role” in the handler currently means a nonblank name returned by a query excluding soft-deleted join and role rows. No additional role activation or scope rule is inferred.
- No decision is made here about role precedence, role combinations, facility/department scope, patient-specific access, lockout thresholds, session duration, or required audit events.
- Demo seed credentials described in `README.md` are not reproduced here and are not evidence of production credential safety.
- Test evidence is described from test source; this review did not execute tests or connect to a database.

## Guidance read

- Orchestrator baseline supplied in task: `docs/PROJECT_CONVENTIONS.md`.
- `README.md`.
- `DOMAIN_ANALYSIS.md`.
- `ERD_DIAGRAM.md`.
- `UI_CODING_STANDARDS.md`.
- Relevant roadmap/domain guidance: `docs/ke-hoach-trien-khai/02-cac-dot-trien-khai.md` and `docs/ke-hoach-trien-khai/phan-he/08-co-so-bao-mat-cau-hinh.md`.
- No repository `AGENTS.md` was found in the worktree.

## Validation record

- Review commands: source/document searches with `rg`; read relevant Java, SQL migration, and Markdown files with `sed`; checked `git status --short` before editing.
- Tests/build: not run; this task requests a source review and report, and no implementation was made.
- Files changed by this task: only `docs/ke-hoach-trien-khai/phase2-auth-persistence-review.md`.
