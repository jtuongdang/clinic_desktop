# Phase 0: Independent Review of Employee SQL Server Fixture and Safe Local Test Path

**Date:** 2026-11-06  
**Document:** `docs/ke-hoach-trien-khai/phase0-employee-sqlserver-local-test-path-review.md`  
**Target:** [`NhanVienRepositorySqlServerTest.java`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java) and companion guard suite [`NhanVienSqlServerTargetGuardTest`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L540-L705)  
**Status:** Report-only review complete. No production code, test code, database schema, migration, or runtime configuration was modified; no database connection was opened; no credentials were read or generated; employee SQL Server persistence remains unverified.

---

## 1. Executive Summary

This independent review evaluates the test-only connection and security guards in [`NhanVienRepositorySqlServerTest.java`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java) following direct local SQL Server test attempts that requested `TrustServerCertificate=True`.

### Key Conclusions:
1. **Root Cause of Local Container Rejection:** The fixture's preflight URL guard (`syntheticDatabaseUrl`) enforces `encrypt=true` and `trustServerCertificate=false` ([lines 499–502, 519–521](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L499-L502)). Attempting to supply `trustServerCertificate=true` fails closed before any socket connection is opened. Conversely, pointing `trustServerCertificate=false` to an out-of-the-box local SQL Server container causes the Microsoft JDBC Driver to fail the TLS handshake (`PKIX path building failed: SunCertPathBuilderException`), because container self-signed certificates lack a trusted CA root in the Java runtime TrustStore and exhibit hostname mismatches.
2. **Current Code Can Run Without Weakening Policy:** Current repository code **does not need to be modified** to execute against local SQL Server. The existing guard explicitly permits `hostNameInCertificate` ([lines 547–556](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L547-L556)). Operators can test locally with full TLS validation by mounting a development certificate issued by a local development CA into the container and supplying a local Java truststore via JVM arguments (`-Djavax.net.ssl.trustStore=...`).
3. **Strict Separation of Demo/Public Data and Transport Security:** The presence of demo accounts or synthetic test data does **not** grant permission to relax TLS encryption or certificate validation. Owner decision `PHASE0-DB-DEPLOY-DECISION-RECON-02` strictly mandates encrypted transport and certificate/identity validation across all environments.
4. **Demonstrable Immunity to `clinicDb` Targeting:** The multi-layered guard (`assertSyntheticDatabaseName`, `syntheticDatabaseUrl`, and pre-migration `SELECT DB_NAME()` identity check) structurally eliminates any path to connect, migrate, or write to `clinicDb` or any database not prefixed with `clinic_test_` or `clinic_orchestrator_review_`.
5. **Acceptance Status:** Live employee SQL Server persistence has **not passed** and remains **unverified** (`FINDING-TEST-02` remains Open / Low-Informational).

---

## 2. Mandatory RAG and Source Retrieval Evidence

In accordance with project conventions, document retrieval and cross-referencing were conducted across the repository:

1. **Test Fixture and Guard Implementation:**
   - [`NhanVienRepositorySqlServerTest.java`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java):
     - Gated opt-in execution via `-Dclinic.test.sqlserver=true` ([lines 77–80](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L77-L80)).
     - Synthetic database naming validation ([lines 62–63, 103–110](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L62-L63)).
     - Dedicated environment variables `CLINIC_TEST_SQLSERVER_URL`, `CLINIC_TEST_SQLSERVER_USERNAME`, `CLINIC_TEST_SQLSERVER_PASSWORD` ([lines 444–461](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L444-L461)).
     - Target URL parameter validation, duplicate property rejection, and TLS enforcement ([lines 463–523](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L463-L523)).
     - Preflight active catalog validation via `SELECT DB_NAME()` before running migrations ([lines 86–95](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L86-L95)).
     - Diagnostic causa-suppression and migration stream silencing ([lines 91–94, 525–536](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L91-L94)).
     - Teardown tracking and cleanup of synthetic rows ([lines 67–68, 352–441](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L67-L68)).
   - Companion unit tests in [`NhanVienSqlServerTargetGuardTest`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L540-L705):
     - Verification of single database selector and TLS preservation ([lines 547–566](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L547-L566)).
     - Rejection of missing, disabled, malformed, or bypass TLS parameters without leaking secret markers ([lines 568–597](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L568-L597)).
     - Verification that application environment variables (`DB_*`) are ignored ([lines 620–645](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L620-L645)).
     - Enforcement of exact catalog matching and test-only prefix ([lines 696–704](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L696-L704)).

2. **Approved TLS Deployment Policy:**
   - [`phase0-deployment-config-tls-decision-proposal.md`](phase0-deployment-config-tls-decision-proposal.md#L41-L46) & [`phase0-db-config-resolution-contract.md`](phase0-db-config-resolution-contract.md):
     - Owner Decision `PHASE0-DB-DEPLOY-DECISION-RECON-02` (confirmed 2026-11-06) mandates TLS transport encryption with validation of both server certificate and server identity (`trustServerCertificate=false`).
     - "Explicitly not approved: `trustServerCertificate=true`, disabled encryption, reliance on JDBC driver-default fallback, or any environment-specific validation/encryption bypass" ([lines 43–45](phase0-deployment-config-tls-decision-proposal.md#L43-L45)).
     - Reconciled addendum in [`phase0-db-test-isolation-review.md`](phase0-db-test-isolation-review.md#L144-L152) confirms that commit `45bd425` aligned `NhanVienRepositorySqlServerTest` with this policy at the synthetic URL-guard boundary.

3. **Database Test Isolation Standards & Operations Runbook:**
   - [`OPERATIONS_RUNBOOK.md`](../OPERATIONS_RUNBOOK.md#L12-L18):
     - §1: Mandates strict synthetic-only databases matching `clinic_test_*`, never connecting to clinic or production data.
     - §3: Clarifies that `trustServerCertificate=true` in `db.properties.example` is an illustrative development template, not approved project policy.

---

## 3. Detailed Technical Analysis: Rejection of Local Self-Signed Certificates

When running tests against a local SQL Server container (such as official `mcr.microsoft.com/mssql/server` images), developers commonly encounter connection failures. The interaction between the fixture and the Microsoft JDBC driver produces two distinct failure points:

```
[Developer URL with trustServerCertificate=true]
                      |
                      v
       [NhanVienRepositorySqlServerTest]
       [Preflight Guard: syntheticDatabaseUrl]
                      |
        (trustServerCertificate == true?)
                     / \
              YES   /   \  NO (trustServerCertificate == false)
                   v     v
           [FAIL-CLOSED]  [Open JDBC Socket & Initiate TLS Handshake]
      (IllegalStateException)                            |
                                                         v
                                              [JVM TrustStore Check]
                                                         |
                                            (Is Certificate Chain Trusted?)
                                                        / \
                                                 NO    /   \  YES
                                                      v     v
                                              [TLS Handshake]  [Verify Hostname Identity]
                                              [FAILS: PKIX]              |
                                                                        / \
                                                                 FAIL  /   \ PASS
                                                                      v     v
                                                               [SSLHandshake] [Connected: SELECT DB_NAME()]
```

### 3.1. Preflight URL Guard Rejection
In [`NhanVienRepositorySqlServerTest.java`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L499-L502):
```java
if ((normalizedName.equals("encrypt") && !value.equalsIgnoreCase("true"))
        || (normalizedName.equals("trustservercertificate") && !value.equalsIgnoreCase("false"))) {
    throw new IllegalStateException("SQL Server test URL requires encrypt=true and trustServerCertificate=false");
}
```
If `CLINIC_TEST_SQLSERVER_URL` contains `trustServerCertificate=true` (or casing variations like `TrustServerCertificate=True`), `syntheticDatabaseUrl` intercepts the parameter during URL parsing. The method throws `IllegalStateException` immediately. The test aborts in preflight before establishing any TCP socket or sending credentials.

### 3.2. JDBC Driver TLS Handshake Failure
If the developer configures `trustServerCertificate=false;encrypt=true;` to satisfy the guard, the URL passes preflight. However, the Microsoft JDBC Driver then initiates TLS negotiation:
1. **Certificate Chain Trust:** By default, SQL Server containers dynamically generate a self-signed certificate upon container startup. This certificate is not signed by a Certificate Authority recognized by Java's default `cacerts` truststore (`$JAVA_HOME/lib/security/cacerts`). As a result, the JVM TrustManager aborts the handshake:
   `sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target`.
2. **Server Identity / Hostname Mismatch:** Even if a developer imports the container's generated certificate into a truststore, the self-signed certificate generated by SQL Server has its Subject Common Name (`CN`) set to the container's internal hostname (e.g. `CN=a1b2c3d4e5f6`). When the client connects via `localhost` or `127.0.0.1`, the JDBC driver verifies that the connected hostname matches the certificate's Subject Alternative Name (SAN) or CN. Because they differ, the driver rejects the connection:
   `com.microsoft.sqlserver.jdbc.SQLServerException: The driver could not establish a secure connection to SQL Server by using Secure Sockets Layer (SSL) encryption. Error: "The target server name is incorrect."`.

### 3.3. Policy Distinction: Mandatory TLS vs One-Off Local Bypass
Project policy firmly separates data sensitivity from transport security:
- **No Exception for Public/Synthetic Data:** While the test uses synthetic data and demo logins, Owner Decision `PHASE0-DB-DEPLOY-DECISION-RECON-02` explicitly prohibits relaxing transport encryption:
  > *"Explicitly not approved: `trustServerCertificate=true`, disabled encryption, reliance on JDBC driver-default fallback, or any environment-specific validation/encryption bypass."*
- **Fail-Closed Boundary:** Permitting `trustServerCertificate=true` in test fixtures introduces testing-production divergence, risks accidental propagation into operational configurations, and disables man-in-the-middle (MITM) protection. Public data does not justify weakening cryptographic guarantees.

---

## 4. Feasibility: Testing Current Code Without Policy Changes

**Can current code be tested against a local SQL Server without changing policy? YES.**

The existing test suite and URL guard already support fully compliant TLS validation without requiring any source code modifications.

### 4.1. Supported Feature: `hostNameInCertificate`
The URL parser in `syntheticDatabaseUrl` explicitly allows additional connection properties provided they are not in the forbidden credential/auth list (`user`, `password`, `integratedSecurity`, etc., [lines 492–497](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L492-L497)).

Specifically, `hostNameInCertificate` is accepted and preserved by the guard, as verified in [`NhanVienSqlServerTargetGuardTest`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L547-L556):
```java
String tls = TLS + ";hostNameInCertificate=synthetic-host";
String braced = ";applicationName={synthetic;database=clinicDb;escaped}}value}";
assertEquals(URL + ";databaseName=" + DATABASE_NAME + braced + tls,
        NhanVienRepositorySqlServerTest.syntheticDatabaseUrl(URL + ";databaseName=" + DATABASE_NAME
                + braced + tls, DATABASE_NAME));
```

### 4.2. Local Execution Procedure (Zero Code Changes)
To run the opt-in persistence test against a local container while complying with `trustServerCertificate=false`:
1. **Generate Local CA & Server Certificate:** Generate a local development CA and issue a certificate for SQL Server with Subject Alternative Names (SAN) including `localhost`, `127.0.0.1`, and `sqlserver.local`. Tools such as `mkcert` or OpenSSL can generate this in seconds.
2. **Mount Certificate in SQL Server Container:** Configure the SQL Server container (via `/var/opt/mssql/mssql.conf` or environment variables) to use the generated certificate and private key for TLS connections.
3. **Establish Java Trust:** Package the local CA root certificate into a dedicated test truststore (e.g. `test-truststore.p12`).
4. **Execute with Dedicated JVM Arguments:** Run Maven pointing to the dedicated truststore without altering default Java trust:
   ```bash
   mvn -pl clinic-infrastructure test \
     -Dtest=NhanVienRepositorySqlServerTest \
     -Dclinic.test.sqlserver=true \
     -Dclinic.test.sqlserver.databaseName=clinic_test_employee_persistence \
     -Djavax.net.ssl.trustStore=/path/to/test-truststore.p12 \
     -Djavax.net.ssl.trustStorePassword=testpass
   ```
   With environment variables set:
   ```bash
   export CLINIC_TEST_SQLSERVER_URL='jdbc:sqlserver://localhost:1433;databaseName=clinic_test_employee_persistence;encrypt=true;trustServerCertificate=false;hostNameInCertificate=localhost'
   export CLINIC_TEST_SQLSERVER_USERNAME='<test_user>'
   export CLINIC_TEST_SQLSERVER_PASSWORD='<test_password>'
   ```
This satisfies the URL guard, successfully completes the TLS handshake, validates server identity, and executes the suite in full compliance with project policy.

---

## 5. Comparative Evaluation of Bounded Options

Three potential paths forward are evaluated below:

| Dimension | Option 1: Trusted Dev Certificate with Identity Validation (Recommended) | Option 2: Test-Only Insecure Bypass Exception | Option 3: Defer Live SQL Persistence Acceptance |
| :--- | :--- | :--- | :--- |
| **Description** | Provision local container with a development certificate signed by a local dev CA; supply truststore via JVM `-Djavax.net.ssl.trustStore`. | Amend `syntheticDatabaseUrl` to allow `trustServerCertificate=true` only when a new explicit test flag is active (e.g. `-Dclinic.test.sqlserver.allowInsecureCert=true`). | Keep persistence tests skipped during default Maven builds; defer live acceptance until staging/CI with valid PKI is ready. |
| **Policy Impact** | **Fully Compliant:** Aligns 100% with `PHASE0-DB-DEPLOY-DECISION-RECON-02`. No policy change or owner exception required. | **Requires New Owner Decision:** Directly contradicts `PHASE0-DB-DEPLOY-DECISION-RECON-02` which explicitly barred `trustServerCertificate=true`. | **Fully Compliant:** Maintains current policy; test remains opt-in and skipped in default CI. |
| **Security & Soundness** | **High:** Verifies authentic TLS handshake and certificate validation mechanics identical to production. | **Low / Regressive:** Disables certificate verification, introduces security divergence, and risks accidental reuse. | **Neutral / Safe:** No security risk introduced; leaves live persistence acceptance unverified. |
| **Code Changes Required** | **Zero:** No changes to `NhanVienRepositorySqlServerTest.java` or `NhanVienSqlServerTargetGuardTest`. | **Non-Trivial:** Modify guard logic, update regex, update 7 guard unit tests, add security safeguards. | **Zero:** No code changes. |
| **Operational Burden** | Requires one-time generation of a local dev CA/cert and mounting into the local SQL container. | Requires Owner review and governance process to draft and approve an exception contract. | Zero immediate effort; leaves employee SQL acceptance pending. |
| **Status of FINDING-TEST-02** | Can be resolved to **Closed** once test passes against the secured local target. | Remains **Qualified/Open** (live persistence proven, but TLS validation unverified). | Remains **Open / Low-Informational**. |

### Recommendation
**Adopt Option 1 (Trusted Dev Certificate with Identity Validation)** as the standard local verification path, while relying on **Option 3 (Defer)** as the safe default fallback when an operator does not have a configured local PKI. **Reject Option 2**, as compromising transport security controls is unnecessary given that existing code already supports valid TLS options (`hostNameInCertificate`).

---

## 6. Synthetic Database, Configuration, and Cleanup Protections

The fixture implements defense-in-depth across five distinct protection layers to ensure safety during opt-in execution.

### 6.1. Layer 1: Opt-in Preflight Gate
- **Opt-in System Property:** Lines 77–80 check `Boolean.parseBoolean(System.getProperty("clinic.test.sqlserver"))`. If `false` or absent, JUnit skips the test suite immediately before evaluating environment variables or database properties.
- **Dedicated Database Property:** Line 81 requires `-Dclinic.test.sqlserver.databaseName`.

### 6.2. Layer 2: Target Database Name Assertion & Guard
Lines 103–110 define `assertSyntheticDatabaseName(databaseName)`:
```java
assertNotNull(databaseName, "An explicit synthetic SQL Server database name is required via -D" + DATABASE_PROPERTY);
assertFalse("clinicDb".equalsIgnoreCase(databaseName.trim()), "Must never connect to default clinicDb");
assertTrue(SAFE_DATABASE_NAME.matcher(databaseName).matches(),
        "SQL Server target must use an approved synthetic database name starting with clinic_test_ or clinic_orchestrator_review_");
```
- **Explicit Prohibition of `clinicDb`:** `assertFalse("clinicDb".equalsIgnoreCase(databaseName.trim()))` rejects any case variation (`clinicDb`, `CLINICDB`, `ClinicDb`).
- **Regex Enforcement:** `SAFE_DATABASE_NAME` enforces:
  `^(?:clinic_orchestrator_review_|clinic_test_)[A-Za-z0-9_]+$`
  Any database name not beginning with `clinic_test_` or `clinic_orchestrator_review_` fails closed.

### 6.3. Layer 3: Target URL Disambiguation and Injection Guard
In `syntheticDatabaseUrl(configuredUrl, databaseName)` ([lines 463–523](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L463-L523)):
- **Prefix & Host Structure:** Mandates `jdbc:sqlserver://` and verifies the host segment contains no whitespace or special characters (`{}=@/?#`).
- **Forbidden Authentication Properties:** Strictly forbids embedded credentials or alternate authentication mechanisms:
  `user`, `username`, `password`, `integratedSecurity`, `authentication`, `authenticationScheme`, `accessToken`, `accessTokenCallbackClass`.
- **Mandatory TLS Settings:** Requires `encrypt=true` and `trustServerCertificate=false`.
- **Database Selector Uniqueness & Exact Match:**
  - Extracts and normalizes properties named `databaseName` or `database`.
  - Unwraps JDBC braced escaping if present (e.g. `{clinic_test_db}`).
  - Enforces `databaseSelectors == 1` and `databaseName.equals(value)`.
  - Rejects missing, duplicate, mismatched, or conflicting selectors.

### 6.4. Demonstration: No `clinicDb` Path is Accepted by the Guard

| Attack / Misconfiguration Vector | Guard Evaluation & Source Defense | Outcome |
| :--- | :--- | :--- |
| **Vector 1:** Developer passes `-Dclinic.test.sqlserver.databaseName=clinicDb` | Line 106 executes: `assertFalse("clinicDb".equalsIgnoreCase(databaseName.trim()))`. | **FAILED CLOSED** (`AssertionError: Must never connect to default clinicDb`). |
| **Vector 2:** Developer passes `-Dclinic.test.sqlserver.databaseName=clinic_test_safe` but supplies URL `jdbc:sqlserver://host;databaseName=clinicDb;encrypt=true;trustServerCertificate=false` | Line 508 in `syntheticDatabaseUrl` checks: `!databaseName.equals(value)` (`"clinic_test_safe".equals("clinicDb")` is false). | **FAILED CLOSED** (`IllegalStateException: SQL Server URL must contain exactly one database selector matching -D...`). |
| **Vector 3:** Multiple selectors, e.g. `;databaseName=clinic_test_safe;database=clinicDb;...` | Lines 503, 508 count selectors. When second selector is encountered, `databaseSelectors > 1`. | **FAILED CLOSED** (`IllegalStateException`). |
| **Vector 4:** Smuggling `clinicDb` in braces inside other properties, e.g. `;applicationName={test;database=clinicDb};databaseName=clinic_test_safe...` | Semicolon/braced regex parser (`URL_PARAMETER`, line 64) treats `{test;database=clinicDb}` as the atomic value of `applicationName`. Parameter name `database` is not matched. | **SAFE:** `clinicDb` is treated as literal data, not a database selector. |
| **Vector 5:** SQL Server server-level routing or default login database redirects connection to `clinicDb` despite URL | Lines 86–90 connect and immediately execute: `SELECT DB_NAME()`. Lines 90, 112–116 execute: `assertDatabaseIdentity(databaseName, actualName)` which calls `assertSyntheticDatabaseName(actualName)`. | **FAILED CLOSED:** If the live catalog returns `clinicDb`, `assertSyntheticDatabaseName` throws `AssertionError` before migration runner or synthetic fixture writes execute. |

### 6.5. Layer 4: Isolation from Application Configuration and Secrets
- **Dedicated Environment Variables Only:** The fixture exclusively queries `CLINIC_TEST_SQLSERVER_URL`, `CLINIC_TEST_SQLSERVER_USERNAME`, and `CLINIC_TEST_SQLSERVER_PASSWORD` ([lines 446–448](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L446-L448)).
- **Application Environment Bypassed:** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_CONNECTION_STRING`, and classpath `db.properties` are completely ignored.
- **Static State Isolation:** Line 85 sets `DatabaseConnection.setConnectionString(testConnectionString)`. Line 100 in `@AfterAll` unconditionally resets the override with `DatabaseConnection.setConnectionString("")`.
- **Driver Diagnostics Redaction:** Lines 91–94 catch `SQLException` during catalog check and throw a generic `IllegalStateException` without chaining the cause, preventing credentials or hostnames from leaking into stack traces.
- **Migration Output Silencing:** Lines 525–536 redirect `System.out` and `System.err` to `OutputStream.nullOutputStream()` during schema migration execution.

### 6.6. Layer 5: Data Teardown Protections
- **Entity Tracking:** Generated IDs are tracked in `generatedEmployeeIds` and `generatedRoleIds` ([lines 67–68, 364, 379](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L67-L68)).
- **Transactional Cleanup in `@AfterEach`:** Lines 401–435 open a connection with `autoCommit=false` and delete records from `NhanVienVaiTro`, `VaiTro`, and `NhanVien` for all tracked IDs, committing upon success or rolling back upon failure.
- **Exception Suppression & List Clearance:** Teardown exceptions are ignored to prevent masking test failures; tracking lists are cleared unconditionally in `finally` ([line 439](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L439)).

---

## 7. Status of Employee SQL Persistence Acceptance

### Acceptance Determination: **UNVERIFIED / PENDING**
- Live employee SQL Server persistence has **not passed** and is **not accepted**.
- Static analysis and synthetic unit tests ([`NhanVienSqlServerTargetGuardTest`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java#L540-L705)) confirm that the URL guard, selector parsing, and credential isolation function as intended without contacting a database.
- However, `NhanVienRepositorySqlServerTest` has not been executed against an authorized, live SQL Server instance with valid TLS in this review.
- In accordance with [`phase0-db-test-isolation-review.md`](phase0-db-test-isolation-review.md#L111,L150):
  - **`FINDING-TEST-02` remains Open / Low-Informational.**
  - Employee SQL Server persistence acceptance remains contingent on live execution against an isolated synthetic database (`clinic_test_*`) with verified TLS negotiation.

---

## 8. Guidance Files Read

In accordance with project conventions and the task checklist, the following guidance files were read and applied:
1. `docs/PROJECT_CONVENTIONS.md` (Orchestrator baseline supplied in task)
2. `README.md` (Setup, shaded JAR configuration, SQL Server parameters)
3. `DOMAIN_ANALYSIS.md` (Package structure, entities, repository contracts)
4. `ERD_DIAGRAM.md` (Schema definitions, entity relationships)
5. `docs/OPERATIONS_RUNBOOK.md` (§1, §3: Synthetic database requirements, TLS guidance)
6. `docs/ke-hoach-trien-khai/phase0-db-test-isolation-review.md` (Isolation analysis, TLS reconciliation addendum)
7. `docs/ke-hoach-trien-khai/phase0-deployment-config-tls-decision-proposal.md` (Owner Decision `PHASE0-DB-DEPLOY-DECISION-RECON-02`)
8. `docs/ke-hoach-trien-khai/phase0-db-config-resolution-contract.md` (Database configuration and TLS resolution contract)
9. `docs/ke-hoach-trien-khai/03-model-task-board.md` (Task `PLAT-DB-TEST-SAFE-01` status and evidence)
10. `docs/ke-hoach-trien-khai/phase0-repository-contract-review.md` (Repository contracts and persistence evidence)
