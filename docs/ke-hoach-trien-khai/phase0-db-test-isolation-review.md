# Phase 0: Independent review of employee SQL test isolation

**Date:** 2026-11-06  
**Target:** [`NhanVienRepositorySqlServerTest.java`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java)  
**Status:** Read-only security and correctness review complete; no production or test code modified.

---

## 1. Executive summary

This review evaluates the test-only connection isolation changes implemented in [`NhanVienRepositorySqlServerTest.java`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java) (task `PLAT-DB-TEST-SAFE-01`). The test suite validates SQL Server persistence mapping and transaction semantics for [`NhanVienRepositoryImpl`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositoryImpl.java), specifically covering `getByUsername`, `getRoleNames`, and `recordSuccessfulLogin`.

Prior database integration fixtures in this repository (e.g., [`FreshDatabaseMigrationSqlServerTest.java`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/FreshDatabaseMigrationSqlServerTest.java#L53-L57) and [`AuditSqlServerPersistenceTest.java`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/AuditSqlServerPersistenceTest.java#L309-L313)) loaded classpath or local `db.properties` and performed regex replacements on `db.url` without strictly prohibiting fallback to developer or application credentials. 

In contrast, the inspected [`NhanVienRepositorySqlServerTest.java`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java) establishes strict connection isolation:
1. **Strict Opt-In Ordering:** All execution is gated behind `-Dclinic.test.sqlserver=true` and `-Dclinic.test.sqlserver.databaseName=<synthetic_name>`. The assumption check and database name validation execute prior to retrieving environment variables or establishing any network connection ([lines 76–81](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L76-L81)).
2. **Dedicated Test Inputs Only:** Connection parameters are exclusively retrieved from dedicated test variables: `CLINIC_TEST_SQLSERVER_URL`, `CLINIC_TEST_SQLSERVER_USERNAME`, and `CLINIC_TEST_SQLSERVER_PASSWORD` ([lines 445–447](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L445-L447)). The test completely rejects application environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_CONNECTION_STRING`) and local `db.properties` ([lines 570–595](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L570-L595)).
3. **URL Selector Disambiguation and Credential Rejection:** The JDBC URL parser mandates exactly one database selector matching the system property, verifies the server host structure, and explicitly forbids embedded credentials and alternate authentication schemes ([lines 462–515](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L462-L515)).
4. **Mandatory Live Catalog Check Before Migrations/Writes:** The fixture connects and executes `SELECT DB_NAME()` to verify the connected catalog identity *before* running schema migrations or inserting fixture records ([lines 85–95](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L85-L95)).
5. **Robust Error Redaction and Teardown:** Diagnostic causes are suppressed on catalog connection failures, console logging during migration is silenced, and synthetic records are explicitly deleted in `@AfterEach` ([lines 90–93, 399–441, 517–528](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L90-L93)).

**Remaining Risk / Policy Gap:** The URL validation logic preserves TLS parameters if supplied by the caller, but does **not** affirmatively validate or enforce the project's approved mandatory TLS encryption and server identity policy (`PHASE0-DB-DEPLOY-DECISION-RECON-02`). Furthermore, no SQL Server persistence acceptance is claimed by this report because live database execution was not performed.

---

## 2. Mandatory RAG and source retrieval

In accordance with project conventions, document retrieval and context verification were performed across the repository:
1. **Employee SQL Fixture:** Examined [`NhanVienRepositorySqlServerTest.java`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L57-L655), [`phase0-repository-contract-review.md`](phase0-repository-contract-review.md), [`phase2-auth-persistence-review.md`](phase2-auth-persistence-review.md), and task board item `PLAT-DB-TEST-SAFE-01` in [`03-model-task-board.md`](03-model-task-board.md#L69).
2. **Operations Runbook Synthetic DB Safety:** Examined [`OPERATIONS_RUNBOOK.md`](../OPERATIONS_RUNBOOK.md#L12-L18) (§1, §3, §4, §6). The runbook specifies that opt-in tests must restrict execution strictly to synthetic disposable databases matching `clinic_test_*`, never connect to `clinicDb` or clinic data, require out-of-band catalog verification, and treat migration and teardown as destructive operations requiring preflight validation.
3. **SQL Server TLS Policy:** Examined [`phase0-deployment-config-tls-decision-proposal.md`](phase0-deployment-config-tls-decision-proposal.md#L41-L46) and [`phase0-db-config-resolution-contract.md`](phase0-db-config-resolution-contract.md). Owner decision `PHASE0-DB-DEPLOY-DECISION-RECON-02` (confirmed 2026-11-06) mandates TLS transport encryption with validation of both server certificate and hostname identity (`trustServerCertificate=false`). Disabling encryption or setting `trustServerCertificate=true` is explicitly prohibited across all environments.
4. **NhanVien Repository Contract and Implementation:** Examined [`INhanVienRepository.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/repositories/INhanVienRepository.java#L7-L25) and [`NhanVienRepositoryImpl.java`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositoryImpl.java#L44-L265), tracing method implementations, query predicates (`isDeleted = 0`), transaction enforcement (`connections.isTransactionActive()`), and unit-of-work tracking (`unitOfWork.recordChanges(1)`).

*Note on RAG Execution:* Tool execution via command shell (`git status`, `ls`, `git diff`) was restricted by user sandbox deny rules; full document inspection was conducted via direct file reads of the authoritative documents.

---

## 3. Verified implementation facts

### 3.1. Opt-in ordering and fail-fast sequence
The initialization lifecycle in [`NhanVienRepositorySqlServerTest.java`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java) implements a fail-fast ordering:
- **Step 1 (Opt-in assumption):** Line 76 verifies `Boolean.parseBoolean(System.getProperty(OPT_IN_PROPERTY))`. If `clinic.test.sqlserver` is not `true`, JUnit skips the entire test class before accessing system properties, environment variables, or classes related to database credentials.
- **Step 2 (Database name preflight):** Lines 80–81 read `clinic.test.sqlserver.databaseName` and pass it to `assertSyntheticDatabaseName(databaseName)` ([lines 102–109](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L102-L109)). This assertion requires:
  - The name must be non-null and non-blank.
  - The name must not equal `clinicDb` (case-insensitive).
  - The name must match regex `(?:clinic_orchestrator_review_|clinic_test_)[A-Za-z0-9_]+`.
- **Step 3 (Target URL and credential construction):** Only after Steps 1 and 2 succeed does line 84 call `testConnectionString(...)`.
- **Per-test safety:** `@BeforeEach setUpRepository()` ([lines 118–126](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L118-L126)) and `@AfterEach tearDownSyntheticData()` ([lines 128–134](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L128-L134)) both guard their execution with the opt-in assumption check.

### 3.2. Dedicated `CLINIC_TEST_SQLSERVER_*` inputs vs application configuration
- **Isolation from production/application configuration:** Line 445–447 explicitly retrieve:
  - `CLINIC_TEST_SQLSERVER_URL`
  - `CLINIC_TEST_SQLSERVER_USERNAME`
  - `CLINIC_TEST_SQLSERVER_PASSWORD`
- **Application configuration bypassed:** The fixture does not read `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_CONNECTION_STRING`, or `DB_DRIVER`.
- **Override mechanism in `DatabaseConnection`:** Line 84 invokes `DatabaseConnection.setConnectionString(connectionString)`. Under [`DatabaseConnection.java`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L59-L62), setting a non-empty `CUSTOM_CONNECTION_STRING` switches `DatabaseConfiguration` to `ConnectionMode.FULL_CONNECTION_STRING` ([`DatabaseConfiguration.java`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L69-L74)), bypassing classpath `db.properties` and normal application environment variables.
- **Verification via unit guard tests:** The companion test class [`NhanVienSqlServerTargetGuardTest`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L532-L655) executes without SQL Server opt-in:
  - `usesOnlyDedicatedInputs_andEscapesCredentialsWithoutTrimmingSecrets` ([lines 549–567](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L549-L567)) verifies that exactly and only the three dedicated keys are requested.
  - `rejectsMissingOrBlankDedicatedInputs_evenWhenApplicationEnvironmentIsComplete` ([lines 569–595](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L569-L595)) asserts that if any dedicated key is missing or blank, the fixture throws `IllegalStateException` even when a full set of `DB_*` variables is present.

### 3.3. URL selector disambiguation and credential rejection
In `syntheticDatabaseUrl(configuredUrl, databaseName)` ([lines 462–515](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L462-L515)):
- **Scheme and host validation:** Enforces `jdbc:sqlserver://` prefix ([line 464](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L464)) and checks that the server segment preceding the first semicolon contains no whitespace or special characters (`{}=@/?#`) ([lines 471–474](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L471-L474)).
- **Parameter parsing and duplicate detection:** Semicolon-delimited parameters are parsed with regex `URL_PARAMETER` ([lines 63–64, 477–488](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L63-L64)). Duplicate properties are strictly rejected via a tracking set ([line 491](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L491)).
- **Prohibited authentication parameters:** Embedded credentials and alternate authentication parameters are rejected ([lines 492–495](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L492-L495)), including:
  `user`, `username`, `password`, `integratedsecurity`, `authentication`, `authenticationscheme`, `accesstoken`, `accesstokencallbackclass`.
- **Database selector enforcement:** Properties named `databaseName` or `database` are counted ([lines 498–507](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L498-L507)). Braced values (e.g., `{clinic_test_foo}`) are unwrapped and unescaped. The method requires `databaseSelectors == 1` and mandates that the value exactly match the `-Dclinic.test.sqlserver.databaseName` system property. Multiple selectors or mismatched targets fail closed ([lines 503–506, 511–513](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L503-L506)).
- **Credential escaping:** Lines 450–451 construct the final connection string using JDBC brace escaping:
  `user={` + username.replace("}", "}}") + `};password={` + credentialValue.replace("}", "}}") + `}`.

### 3.4. Exact `SELECT DB_NAME()` check before migrations and writes
The ordering within `setUpDatabaseFixture()` ([lines 85–95](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L85-L95)) guarantees database identity before any modification occurs:
1. Opens a live connection via `DatabaseConnection.getConnection()`.
2. Executes `SELECT DB_NAME()` on the active connection.
3. Compares the returned catalog name against `databaseName` via `assertDatabaseIdentity(databaseName, rs.getString(1))` ([lines 89, 111–115](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L89)).
4. `assertDatabaseIdentity` re-verifies that the returned catalog matches the synthetic naming pattern and equals the expected name.
5. Only if the identity assertion passes does the test proceed to `runMigrationsWithoutLogging()` ([line 94](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L94)).
6. If the check fails or throws `SQLException`, no migrations and no write operations are executed.

### 3.5. Error redaction and diagnostics safety
- **Suppression of driver diagnostics on setup failure:** In lines 90–93:
  ```java
  } catch (SQLException exception) {
      // Driver diagnostics can contain URL/user/secret material. Do not retain the cause.
      throw new IllegalStateException("Dedicated SQL Server test connection or catalog check failed");
  }
  ```
  The caught `SQLException` is explicitly not set as the exception cause, preventing JDBC connection strings, credentials, or network hostnames from leaking into stack traces or CI logs.
- **Migration output suppression:** `runMigrationsWithoutLogging()` ([lines 517–528](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L517-L528)) temporarily redirects both `System.out` and `System.err` to `OutputStream.nullOutputStream()`, ensuring no connection errors or schema messages leak during migration execution.
- **Validation error safety:** All exception messages in `testConnectionString` and `syntheticDatabaseUrl` use fixed strings without formatting the input URL or credentials into the message text ([lines 457, 465, 469, 473, 487, 494, 504, 512](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L457)).
- **Companion guard assertions:** `NhanVienSqlServerTargetGuardTest.rejectsMismatchedOrAmbiguousDedicatedUrl_withoutExposingInputs` ([lines 597–614](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L597-L614)) explicitly asserts that thrown exceptions contain none of the input values and have null causes.

### 3.6. Teardown and synthetic data cleanup
- **Fixture tracking:** Synthetic rows created during tests are tracked in `generatedEmployeeIds` and `generatedRoleIds` ([lines 66–67, 363, 378](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L66-L67)).
- **Explicit transactional deletion in `@AfterEach`:** `cleanupGeneratedRecords()` ([lines 399–441](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L399-L441)) opens a connection, sets `autoCommit = false`, and deletes:
  - Join records in `NhanVienVaiTro` referencing generated role IDs.
  - Role records in `VaiTro` referencing generated role IDs.
  - Join records in `NhanVienVaiTro` referencing generated employee IDs.
  - Employee records in `NhanVien` referencing generated employee IDs.
  - Calls `commit()`, with rollback upon error.
- **Failure resilience:** Exceptions during teardown are caught and suppressed (`catch (Exception ignored)`) so they do not obscure test assertion failures. The tracking lists are always cleared in `finally` ([line 438](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L438)).
- **Connection override reset in `@AfterAll`:** `clearTestConnectionOverride()` ([lines 97–100](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L97-L100)) executes `DatabaseConnection.setConnectionString("")`, resetting the static connection state in `DatabaseConnection` and `DatabaseConfiguration`.

---

## 4. Risks and findings

| Finding ID | Topic | Description | Severity | Proposed follow-up |
|---|---|---|---|---|
| **FINDING-TEST-01** | **TLS Policy Enforcement Gap** | `syntheticDatabaseUrl` allows and preserves arbitrary URL properties (such as TLS settings), but does not affirmatively require `encrypt=true` or `trustServerCertificate=false`. A URL specifying `trustServerCertificate=true` or omitting encryption is accepted by the validator. This diverges from Owner contract `PHASE0-DB-DEPLOY-DECISION-RECON-02`, which explicitly disallows `trustServerCertificate=true` across all deployment profiles. | **Medium** | In a future test-hardening task, update `syntheticDatabaseUrl` to: (1) reject `trustServerCertificate=true`, (2) require `encrypt=true`, and (3) require valid certificate identity options (`trustServerCertificate=false`). |
| **FINDING-TEST-02** | **SQL Persistence Acceptance Status** | Although `NhanVienSqlServerTargetGuardTest` validates the target guard logic synthetically, `NhanVienRepositorySqlServerTest` has not been executed against a live SQL Server instance in this review. Live persistence behavior, trigger interactions, and index enforcement on SQL Server remain unverified. | **Low / Informational** | Do not claim SQL persistence acceptance for `NhanVienRepositoryImpl`. Mark live SQL Server persistence as pending until authorized execution against an isolated synthetic SQL Server instance (`clinic_test_*`). |
| **FINDING-TEST-03** | **Teardown Scope Limitations** | `cleanupGeneratedRecords()` deletes records tracked in `generatedEmployeeIds` and `generatedRoleIds`. If a test were to create auxiliary records (e.g. audit entries or department mappings) without tracking their IDs, those records would remain in the test database. Currently, the test methods only generate synthetic employees and roles, so scope leakage is contained. | **Low / Informational** | Ensure any future tests added to this fixture track all created entities or run inside non-committing transactions where feasible. |

---

## 5. Scope and validation

- **Allowed Scope Validation:** Only [`docs/ke-hoach-trien-khai/phase0-db-test-isolation-review.md`](phase0-db-test-isolation-review.md) is created. No production code, test code, build configuration, or existing documentation was modified.
- **Zero Live Connection / Zero Secret Access:** No real SQL Server connection was opened, no local `db.properties` was read, and no `DB_*` environment credentials were accessed.
- **Test Integrity:** The existing test implementation was examined purely via static analysis and synthetic guard test evidence. All identified issues are documented as findings above without altering existing code.

---

## 6. Guidance files read

In accordance with project conventions, the following guidance and documentation files were read and applied:
1. `docs/PROJECT_CONVENTIONS.md` (Orchestrator baseline)
2. `README.md`
3. `DOMAIN_ANALYSIS.md`
4. `ERD_DIAGRAM.md`
5. `docs/OPERATIONS_RUNBOOK.md`
6. `docs/ke-hoach-trien-khai/README.md`
7. `docs/ke-hoach-trien-khai/03-model-task-board.md`
8. `docs/ke-hoach-trien-khai/phase0-repository-contract-review.md`
9. `docs/ke-hoach-trien-khai/phase0-db-config-review.md`
10. `docs/ke-hoach-trien-khai/phase0-db-config-resolution-contract.md`
11. `docs/ke-hoach-trien-khai/phase0-deployment-config-tls-decision-proposal.md`
12. `docs/ke-hoach-trien-khai/phase2-auth-persistence-review.md`
13. `docs/ke-hoach-trien-khai/phase2-authz-enforcement-review.md`
14. `docs/ke-hoach-trien-khai/phase2-login-throttle-implementation-review.md`

---

## 7. Dated addendum — TLS fixture guard reconciliation (2026-11-06)

This addendum preserves the review above as its original historical snapshot. **FINDING-TEST-01** described the then-current gap and retains its original **Medium** severity as a historical assessment; it is now **resolved at the synthetic test-fixture URL-guard boundary only**, by integrated commit `45bd425` (task `d76d146d-b4ea-4e40-8057-835e38b5c4e4`).

The accepted contract `PHASE0-DB-DEPLOY-DECISION-RECON-02` still requires encrypted SQL Server connections and certificate plus server-identity validation, rejects `trustServerCertificate` bypass, and requires failures for invalid/untrusted/expired/mismatched certificates ([resolution contract, line 9](phase0-db-config-resolution-contract.md#L9)). The current `syntheticDatabaseUrl` guard rejects non-`true` `encrypt` and non-`false` `trustServerCertificate` values (lines 498–502), then requires both properties (lines 516–520); it also rejects malformed URL properties (lines 486–488) ([current fixture source, lines 463–522](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L463-L522)). `NhanVienSqlServerTargetGuardTest` accepts valid explicit TLS properties (lines 546–565) and rejects missing, disabled, malformed, or bypass values with synthetic inputs (lines 568–585) ([guard tests](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L546-L585)).

This evidence establishes URL-guard behavior only. Microsoft documents `encrypt=true` as requesting TLS encryption and describes certificate-name validation when `trustServerCertificate=false` as JDBC connection-property behavior ([Microsoft JDBC connection properties: `encrypt`, `hostNameInCertificate`](https://learn.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties?view=sql-server-ver17#encrypt)). That mechanism description is not evidence of a negotiated TLS handshake, certificate-chain/name validation against a live server, or employee SQL persistence acceptance. No SQL Server was connected for this reconciliation; **FINDING-TEST-02 therefore remains Low / Informational and live employee persistence remains unverified**. The supplied full JDK 17 validation result for the integrated change was 443 tests, 0 failures/errors, 6 SQL Server opt-in skips, and 77 Surefire XML reports; these test results do not constitute live database or TLS-handshake acceptance. **FINDING-TEST-03 remains Low / Informational**, unchanged by this evidence.

No database, local properties, or credentials were accessed for this documentation change.

### Guidance read for this addendum

- `docs/PROJECT_CONVENTIONS.md` (Orchestrator baseline supplied in the task)
- `README.md`, `DOMAIN_ANALYSIS.md`, and `ERD_DIAGRAM.md` (supplied in the task)
- `docs/ke-hoach-trien-khai/phase0-repository-contract-review.md` (retrieved excerpt supplied in the task)
- `docs/ke-hoach-trien-khai/phase0-db-test-isolation-review.md` (target review and retrieved excerpts supplied in the task)
- `docs/ke-hoach-trien-khai/phase0-db-config-resolution-contract.md` and `docs/ke-hoach-trien-khai/phase0-deployment-config-tls-decision-proposal.md` (retrieved excerpts supplied in the task)
- Microsoft Learn, *Set the Connection Properties - JDBC Driver for SQL Server* (connection-property mechanism reference)
