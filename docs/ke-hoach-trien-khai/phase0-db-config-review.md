# Phase 0: Desktop database configuration and release secret review

## Scope and conclusion

This is a report-only review of the desktop JDBC configuration path, local configuration guidance, Maven resource/shaded JAR rules, launch entry point, and related tests. No application code or tests were changed, no credential-bearing local configuration was opened, and no database was contacted.

The source supports environment overrides and a full JDBC connection string, but it does not provide a dedicated configuration test or an external configuration-file path at packaged runtime. The shaded desktop JAR is configured to exclude a resource named `db.properties`, while environment variables remain available. The SQL Server example explicitly enables encryption and trusts the server certificate; that is an example setting, not evidence of a verified TLS policy for other URLs or deployments. These facts do not establish release or production readiness.

## Proven source behavior

### Configuration sources and precedence

`DatabaseConnection` loads the classpath resource `db.properties` during class initialization. If present, it reads `db.url`, `db.username`, `db.password`, and `db.driver` (`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java:29-47`). It then replaces each of those values with the corresponding nonblank environment variable `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_DRIVER`; those environment values are trimmed (`DatabaseConnection.java:50-65`). A nonblank `DB_CONNECTION_STRING` is stored separately and takes precedence over the split fields when opening a connection (`DatabaseConnection.java:56-60,90-104`).

There is a second programmatic override path: `setConnectionString` takes precedence over the configured split fields, and `configure` replaces the URL, username, and password fields (`DatabaseConnection.java:68-83,90-104`). Configuration is static and loaded once at class initialization; changing the process environment afterward is not re-read by this class (`DatabaseConnection.java:29-31,38-61`).

`DB_DRIVER` and `db.driver` are loaded into `dbDriver`, but the shown connection path never reads that field. Driver registration instead infers a known driver from the JDBC URL prefix, silently ignores `ClassNotFoundException`, then delegates to `DriverManager` (`DatabaseConnection.java:23-27,43-46,51-54,90-125`). The sample `db.pool.maxSize` property has no consumer in this class (`clinic-infrastructure/src/main/resources/db.properties.example:7`).

The fallback connection path treats a null or empty URL as missing configuration and throws `SQLException`; if a URL exists, it supplies credentials only when username is nonempty, otherwise it calls `DriverManager.getConnection(url)` (`DatabaseConnection.java:97-106`). There is no explicit validation that the URL, username, or password are mutually complete, nor is malformed configuration surfaced at load time. Any exception during resource loading is silently ignored (`DatabaseConnection.java:38-48`).

### Local properties and packaging evidence

The example file advises copying it to `db.properties`, and its example SQL Server URL contains `encrypt=true;trustServerCertificate=true` (`clinic-infrastructure/src/main/resources/db.properties.example:1-6`). The root ignore file excludes the specific local resource path and `.env` files (`.gitignore:8-11`); ignore rules do not themselves prevent other copies or already tracked files from entering source control.

The infrastructure POM has no custom resource declaration, so Maven's standard resource handling applies to its resources (`clinic-infrastructure/pom.xml:11-41`). The desktop shade configuration excludes a resource whose archive path is `db.properties`, removes dependency signature metadata, and sets `vn.clinicmanagement.desktop.Main` as the manifest main class (`clinic-desktop/pom.xml:35-66`). The example file has the distinct name `db.properties.example`, so the stated exclusion does not match that name. Root inspection of the JDK 17 shaded artifact with `jar tf` confirmed `db.properties` is absent and `db.properties.example` is included. This verifies archive contents for this build only; it does not verify launch behavior or configuration discovery at runtime.

`Main.main` starts the Swing login UI and does not configure a filesystem path for database properties (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/Main.java:14-33`). The README documents environment variables, copying the example into the infrastructure resources directory for local use, running migrations, and `mvn package`; it does not provide an executable JAR launch command or a packaged-JAR configuration-file mechanism (`README.md:5-10`). The connection loader uses only a classpath resource lookup (`DatabaseConnection.java:38-47`), so an adjacent file beside a released JAR is not proven to be discovered.

### TLS behavior

The checked-in example URL explicitly sets encryption on and `trustServerCertificate=true` (`db.properties.example:3`). That sample setting requests encryption while trusting the server certificate without normal certificate-chain validation. `DatabaseConnection` itself adds no TLS properties or defaults; it passes the selected URL to JDBC (`DatabaseConnection.java:90-104`). Therefore, effective TLS and certificate validation for a URL that omits these properties depends on the JDBC driver and deployment configuration, and was not verified here. The root POM pins the Microsoft SQL Server JDBC dependency to `12.6.1.jre11` (`pom.xml:20-27`), but its effective defaults were not tested in this report.

### Error and logging behavior

On missing configuration, `getConnection` returns a generic setup-oriented `SQLException` without identifying which required field is absent (`DatabaseConnection.java:97-106`). Resource-loading failures and driver class lookup failures are swallowed (`DatabaseConnection.java:38-48,112-125`). The optional `testConnection` helper prints the database product, version, connected user on success, and the raw `SQLException` message on failure (`DatabaseConnection.java:132-150`). Ordinary callers receive SQL exceptions from `DriverManager`; this class does not log them (`DatabaseConnection.java:90-107`). Whether callers elsewhere log exception text was not exhaustively audited.

## Current configuration test evidence

No dedicated test for `DatabaseConnection` configuration loading or environment precedence appears in the repository test file inventory/search. Existing persistence unit tests use fake JDBC or inspect/set the `CUSTOM_CONNECTION_STRING` field (`clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunnerTest.java:24-25,104-105`; `clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImplTest.java:204-209`). Those do not establish environment precedence or packaged resource behavior.

The live SQL Server tests are gated by explicit system-property opt-in: the reception persistence test checks `clinic.test.sqlserver` before loading local configuration and migrating (`clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/reception/ReceptionSqlServerPersistenceTest.java:57-76,224-247`); the process restart test uses the same opt-in (`ReceptionSqlServerProcessRestartTest.java:66-79,149-172`); migration and audit tests also check their opt-in properties (`clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/FreshDatabaseMigrationSqlServerTest.java:39-60`; `AuditSqlServerPersistenceTest.java:78-94,284-299`). This review did not enable those tests or read their local credentials.

## Gaps and unverified assumptions

- **Secret loading and release:** Environment input and a classpath resource are supported. There is no external file path configurable for a released JAR in the inspected loader. The shade exclusion is configuration evidence only; actual assembled artifact contents and launch behavior still need a repeatable artifact check. Whether deployment tooling injects environment values securely is outside repository evidence.
- **Precedence edge cases:** Individual overrides ignore null/blank values and trim environment values, while property-file values are used as loaded. The full connection string is checked for blankness but stored without trimming. Interactions for partial credentials, whitespace in properties, conflicting individual overrides, and a full URL containing credentials have no focused tests (`DatabaseConnection.java:43-65`).
- **Driver setting:** `DB_DRIVER` is accepted syntactically but unused when connecting. Unknown JDBC URL schemes receive no explicit driver-class attempt in `autoRegisterDriver` (`DatabaseConnection.java:112-125`).
- **TLS:** The checked-in example trusts the server certificate. The repository does not establish a deployment-wide certificate-validation requirement, and driver behavior for URLs without TLS properties is unverified. Choosing the required TLS policy needs deployment requirements/owner input.
- **Malformed or missing configuration:** Load errors are swallowed; partial settings can progress to driver invocation and fail later. There is no startup-time validation or focused test for missing/malformed fields (`DatabaseConnection.java:38-48,97-106`).
- **Logging and diagnostics:** `testConnection` can print raw driver exception detail and connected-user metadata. No redaction behavior is shown here. It remains unverified whether downstream callers expose or log the exception message.
- **Reproducible launch:** README describes `mvn package` but not a concrete command for launching the shaded JAR with external configuration. The classpath-only lookup makes an adjacent properties file unsupported by the inspected implementation (`README.md:7-10`; `DatabaseConnection.java:38-47`; `clinic-desktop/pom.xml:58-60`).

## Smallest recommended implementation and test sequence

Keep configuration local to the desktop deployment unless requirements establish a need for a managed secret service. First extract a small, side-effect-free configuration resolver (or equivalent test seam) that represents the currently intended precedence, validates required fields, and produces safe diagnostics that never include the URL or credentials. Decide with the deployment owner whether to keep a classpath resource for development, add an explicitly named external properties path for the JAR, or use environment-only configuration; document the same selected launch path for both cases. Remove or implement `DB_DRIVER` consistently. Make the TLS choice explicit in deployment documentation and test the selected URL properties without contacting a database.

Then add focused tests using synthetic values and a fake JDBC driver:

1. Test classpath/local properties as base configuration; test each nonblank `DB_*` override; test precedence of `DB_CONNECTION_STRING`; test blank and partial values, property whitespace, and missing/malformed configuration. Assert that exception/log output does not contain synthetic password or connection-string marker values.
2. For local desktop development, verify a clean-checkout launch recipe using the chosen local source and an explicitly synthetic configuration. Confirm invalid configuration fails with a concise actionable message before the login flow attempts persistence. Keep SQL Server tests opt-in and target only an explicitly named synthetic database when a database test is separately authorized.
3. For the packaged JAR, build under JDK 17, inspect archive entry names to confirm no `db.properties` is present, and launch the shaded artifact with the documented external configuration source. Use the fake driver/configuration seam for a no-network smoke test of resolution and driver handoff; do not connect to SQL Server for this configuration test.
4. Add a distinct TLS configuration test asserting the chosen URL options and certificate-validation behavior at the configuration boundary. A real certificate/handshake test belongs in a separately authorized environment test.

These steps validate behavior without selecting a production policy or claiming production readiness.

## Guidance read

- `docs/PROJECT_CONVENTIONS.md` — supplied in the task guidance; no repository copy was found.
- `README.md`
- `DOMAIN_ANALYSIS.md`
- `ERD_DIAGRAM.md`
- `UI_CODING_STANDARDS.md`
- `docs/ke-hoach-trien-khai/README.md`
- `docs/ke-hoach-trien-khai/phase0-platform-base-review.md` and `docs/ke-hoach-trien-khai/phase0-error-mapping-review.md` — existing report patterns.
- Repository `AGENTS.md` files were searched for and none were found.

## Validation and scope

- Read-only source review: `DatabaseConnection.java`, `db.properties.example`, `.gitignore`, root and module POMs, `README.md`, application entry point, relevant persistence tests, and existing Phase 0 report format. No local `db.properties` was opened; no credential values or database were accessed.
- The review task's first direct Maven attempt failed before compilation because the shared local repository lock for `maven-resources-plugin:3.5.0` could not be opened. The Orchestrator's configured JDK 17 validation then passed (task `af98fc52-c251-44fd-9316-2929389f4ca7`, scope check passed). Root independently reran `mvn -Djava.awt.headless=true clean verify` after the review: **BUILD SUCCESS, 209 tests, 0 failures/errors, 4 expected SQL Server opt-in skips**, all five Maven projects. No shared dependency cache was moved or altered.
- Root inspected `clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar` using JDK 17 `jar tf`: the archive contains `db.properties.example` and contains no entry named `db.properties`. This check proves only artifact contents; the JAR was not run against a database.
- JDK check: `java_home -V` found JDK 17.0.17; the default Java was JDK 27, so the Maven command explicitly selected JDK 17.
- The Codex task's source-level scope check passed with only its report in its task diff. Root subsequently updated the phase summary, task board, and index to integrate the report.
