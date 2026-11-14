# Operations and release runbook

**Scope:** local or controlled test deployment of Clinic Management Desktop. This is an evidence-based procedure, not a production release policy. No database, migration, backup, or restore operation was performed while preparing this document.

## Evidence labels

- **Source-proven** means the command or behavior is present in repository configuration/source. It proves the defined local build/test flow, not a release approval.
- **Illustrative template** means an operator must replace placeholders and adapt the operation to an approved, isolated synthetic test environment. It is not a ready-to-run command and must never point at clinic or production data.

Source links below are relative to this document. The project overview and current implementation limits are also recorded in [00 — Hiện trạng](ke-hoach-trien-khai/00-hien-trang.md).

## 1. Prerequisites and scope

- Use JDK 17 and Maven from the repository root. The parent POM sets `maven.compiler.release` to 17 and aggregates `clinic-domain`, `clinic-application`, `clinic-infrastructure`, and `clinic-desktop` ([root `pom.xml`](../pom.xml)). Maven Wrapper is not present in this checkout.
- **Owner-approved boundary — contract `18847587-233a-4f02-99cd-13e1eaf0736a` (2026-11-06):** `clinicDb` is only for launching the desktop through `run-app.sh`, manual visual smoke, and experiential entry. Automated tests and CI for every worker agent must never connect to or write to `clinicDb`; default Maven tests/verification remain in-memory or synthetic and require no SQL Server. Preserve the explicit assertion guard rejecting `clinicDb` in the employee SQL fixture ([`NhanVienKhoDuLieuSQLMayChuTest`](../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/identity/NhanVienRepositorySqlServerTest.java)).
- SQL Server is required for desktop runtime, manual visual smoke, and separately enabled SQL integration checks or migration rehearsals. Opt-in SQL tests may use only an explicitly selected `clinic_test_*` or `clinic_orchestrator_review_*` database and synthetic fixtures ([migration SQL Server test](../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/FreshDatabaseMigrationSqlServerTest.java), [reception SQL Server test](../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/reception/ReceptionSqlServerPersistenceTest.java)).
- Owner reports that local `clinicDb` already has migrations V1–V11; this is an Owner-provided assertion, not independently verified here. Do not run migrations as part of manual UI smoke. If the schema is not V1–V11, stop and use a separately authorized migration procedure.
- Never use patient data or a live clinic database for acceptance. Do not place credentials in shell history, source, logs, screenshots, or this runbook. Use an approved local secret-injection method and a dedicated least-privilege test login.
- Set the intended synthetic database explicitly and verify its identity out of band before enabling any opt-in test or running the migration runner. The runner applies pending scripts to whichever database the configured JDBC connection reaches.

## 2. Repeatable build, tests, and package

Run the configured full validation from the repository root:

```bash
mvn -Djava.awt.headless=true clean verify
```

This exact command is used by the Maven GitHub Actions workflow on push and pull request, with JDK 17 ([workflow](../.github/workflows/maven-verify.yml)). It cleans and verifies all four Maven modules; headless mode does not constitute a visible Swing UI smoke test. SQL Server tests remain skipped unless explicitly opted in.

Build the desktop shaded JAR:

```bash
mvn -pl clinic-desktop -am package
```

This command is documented in the root [README](../README.md) and the desktop POM binds Maven Shade to `package`, sets `vn.clinicmanagement.desktop.Main` as the manifest entry point, and excludes `db.properties` and dependency signature metadata ([desktop `pom.xml`](../clinic-desktop/pom.xml)). Expected artifact path is `clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar` for the current project version. The `.example` resource is a template and is not a secret store.

## 3. Database configuration and secret handling

The connection loader reads classpath `db.properties`, then applies nonblank `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment overrides. A nonblank `DB_CONNECTION_STRING` takes precedence over those split fields. Configuration is sampled once by the configuration object; start a fresh process after changing environment values ([`CoSoDuLieuKetNoi`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java), [`CoSoDuLieuCauHinh`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java)).

For the packaged JAR, the shaded archive excludes `db.properties`. The checked-in example says to copy to `db.properties` for development, but the loader only discovers a classpath resource; an adjacent external properties file is not a proven packaged-JAR configuration path. For a packaged local/test run, supply environment variables through a protected local environment/secret injector. Do not commit a local properties file or `.env`. The sample connection URL uses `trustServerCertificate=true`; this is example configuration, not evidence of an approved TLS/certificate policy ([example properties](../clinic-infrastructure/src/main/resources/db.properties.example), [DB configuration review](ke-hoach-trien-khai/phase0-db-config-review.md)).

**Manual app use of `clinicDb` (per contract `18847587-233a-4f02-99cd-13e1eaf0736a`):** `run-app.sh` only launches Java and inherits the process configuration; it does not select or verify a database. Before launching for manual visual smoke or experiential entry, operators must securely set and verify the effective `DB_URL` as `jdbc:sqlserver://localhost:1433;databaseName=clinicDb;...` and provide `DB_USERNAME` and `DB_PASSWORD` through a protected local environment/secret-injection method. Never echo, print, paste into command history, or record credentials or the full connection string. Confirm the effective target out of band before sign-in. This app-only exception does not permit automated tests or CI to connect to `clinicDb`.

The Owner separately authorized `TrustServerCertificate=true` only for a one-off local development/manual connection. It is not committed application configuration or a worker SQL-test profile, and it is not evidence of certificate validation. Deployment still requires encrypted transport with validation of the server certificate and identity.

**Illustrative template only — placeholders are not credentials or an executable endpoint.** SQL opt-in tests must use an explicitly selected synthetic database named `clinic_test_*` or `clinic_orchestrator_review_*`; set secret values out of band rather than pasting them into a terminal command or shell history.

```text
DB_URL = jdbc:sqlserver://<approved-test-host>:1433;databaseName=clinic_test_<unique_suffix>;encrypt=true;<approved-certificate-options>
DB_USERNAME = <test-login-from-secret-store>
DB_PASSWORD = <test-secret-from-secret-store>
```

Alternatively, the implementation accepts `DB_CONNECTION_STRING` in place of the split values. Do not record or print its contents. `DB_DRIVER` and the example `db.pool.maxSize` are not used by the inspected connection path; do not rely on them for configuration ([`CoSoDuLieuKetNoi`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java), [Phase 0 configuration review](ke-hoach-trien-khai/phase0-db-config-review.md)).

## 4. Migration preflight and execution

### Preflight

1. Confirm this is an isolated synthetic acceptance database, not `clinicDb`, a clinic database, or any production system. Confirm the selected server and database using the approved DBA/client procedure without printing credentials or full connection strings.
2. Confirm the test DB has no unrelated objects or data. The fresh migration integration test refuses a selected database containing user objects and restricts the name to `clinic_test_*`; it is opt-in evidence, not a guarantee for manual runs ([`MoiCoSoDuLieuDiCuSQLMayChuTest`](../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/FreshDatabaseMigrationSqlServerTest.java)).
3. Review the pending migration SQL files and establish a tested backup plus a restore destination before changing a database. Confirm no competing migration process is running.
4. Ensure the process environment is configured before starting Java. Recheck the target database identity; the runner does not accept a separate database-name argument.

### Run the migration runner

The runner registers V1 through V11, creates `__schema_migrations` when missing, skips scripts recorded as successful, splits SQL Server `GO` batch separators, and records each successful script. It commits the migration tracking-table creation and each script separately; a script failure rolls back that script transaction and reports failure. This is not an all-migrations atomic transaction ([`CoSoDuLieuDiCuChay`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunner.java), [migration resources](../clinic-infrastructure/src/main/resources/migrations/)).

After building the shaded artifact and configuring the synthetic test environment, the following invocation is **source-proven** by the README and runner entry point. It connects to the database selected by the process configuration and can change that database; execute only after the preflight above and separate authorization for the controlled test migration:

```bash
java -cp clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar \
  vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuDiCuChay
```

The current runner returns a nonzero process exit status on migration failure ([`CoSoDuLieuDiCuChay`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunner.java)). A successful exit is necessary but not sufficient: verify all expected migration names in `__schema_migrations`, inspect representative expected schema/seed state, and ensure the test application can read back synthetic records. Avoid capturing credential-bearing diagnostic context.

**Opt-in SQL Server checks:** current integration tests require explicit test properties and a configured test connection. The fresh schema test uses `clinic.sqlserver.migration.enabled=true`; the reception test uses `clinic.test.sqlserver=true`. These are destructive-capable paths. Select only an explicitly named `clinic_test_*` or `clinic_orchestrator_review_*` synthetic target and verify its identity out of band before enabling the test. After a run, delete only rows and temporary objects created by that run. Drop a database only if that test run created it solely as its disposable target; never drop or truncate `clinicDb` or a pre-existing shared test database. Preserve the fixture's negative guard that rejects `clinicDb`. These checks were not run for this runbook task.

## 5. Launch and basic post-launch checks

For manual visual smoke or exploratory data entry, use local `clinicDb`. From the repository root, prepare the classes used by the local launcher before injecting the app credentials:

```bash
mvn -pl clinic-desktop -am -DskipTests package
```

Follow the launch preflight to confirm `localhost:1433/clinicDb` and migrations V1–V11 using read-only metadata. In the app shell, use the protected connection settings from section 3. With split settings, clear any inherited `DB_CONNECTION_STRING` before setting `DB_URL`, because the full string takes precedence. Start the desktop with `./run-app.sh`. The local launcher uses the compiled module classes. [Doctor visual smoke procedure](ke-hoach-trien-khai/phase1-doctor-ui-visual-smoke-runbook.md#3-launch-and-authentication-smoke-procedure)

Sign in with an authorized app account assigned to the intended role and scope. Observe login, the relevant screen and logout using synthetic records. Record the revision, role, scope and result. For the security baseline, record the remaining observations against the SEC-01 checklist. [SEC-01 baseline acceptance](ke-hoach-trien-khai/phase2-sec01-baseline-acceptance.md)

## 6. Backup-before-change and restore rehearsal

The repository contains no backup scheduler, retention policy, restore automation, RPO/RTO, or backup acceptance procedure. Decide these with the responsible system/data owner before any non-disposable environment is considered. Do not infer a schedule or retention period from this runbook.

For a **controlled synthetic test rehearsal only**, before migration or another schema change:

1. Identify the exact synthetic source database and a separately provisioned backup destination with adequate access controls. Confirm the backup destination and available space with the operator.
2. Take a full backup using the approved SQL Server operations procedure. Record the database identity, backup artifact identifier/location, completion status, and time without recording credentials or sensitive records.
3. Restore that artifact into a separate isolated synthetic database. Do not overwrite the source database as part of a rehearsal.
4. Verify the restored database is online and run the approved integrity and application-level checks. Confirm the expected schema migration history and read back known synthetic fixtures. Record pass/fail and clean up only test-created rows and temporary objects. Drop a database only when that test run created it solely as a disposable target; do not drop pre-existing shared test databases or `clinicDb`.
5. Only after successful restore verification proceed with the authorized test change. If restore cannot be verified, stop and resolve the backup/restore gate before changing the test database.

**Illustrative SQL Server templates only; replace every placeholder using the DBA's approved procedure. These templates were not executed. They are restricted to a synthetic test DB and are not production-ready commands.** Backup paths and restore file mappings are server-specific; validate them with the DBA. `WITH REPLACE` can overwrite data, so do not add it or target an existing database unless the separate isolated restore destination has been positively verified and the DBA explicitly approves the operation.

```sql
BACKUP DATABASE [clinic_test_<unique_suffix>]
TO DISK = N'<approved_backup_path>/<unique_backup_name>.bak'
WITH CHECKSUM;
```

Choose a new, unique backup artifact path for each rehearsal; do not enable overwrite options for an existing backup artifact.

```sql
RESTORE VERIFYONLY
FROM DISK = N'<approved_backup_path>/<unique_backup_name>.bak'
WITH CHECKSUM;
```

`RESTORE VERIFYONLY` checks the backup media but does not prove a usable database restore. A real rehearsal must restore to a new isolated synthetic database using server-specific file mappings, then run integrity and application readback checks. Do not run a destructive restore command from this document.

## 7. Rollback limits and recovery

No automated down migration or reverse migration scripts were found. The runner only applies the ordered forward migrations V1–V11 and records successful scripts; do not manually delete tracking rows or attempt to reverse schema changes by editing history ([`CoSoDuLieuDiCuChay`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunner.java), [migration directory](../clinic-infrastructure/src/main/resources/migrations/)). Migration code has per-script failure rollback coverage, but that does not reverse earlier committed scripts or restore data changed by an already completed migration.

If a migration or release fails, stop further changes, preserve safe diagnostic evidence, identify which scripts completed, and involve the database/application owner. Recovery requires a verified pre-change backup and an approved restoration plan for an isolated target; restoring over a source is destructive and must be handled by the authorized DBA under a separately approved procedure. Application rollback may also require a compatible prior artifact and a schema compatibility decision. The repository does not establish that old application versions can run against the new schema. Do not claim rollback is complete until restored data and application checks pass.

## 8. Open operational gates

- REL-01 remains incomplete: CI-hosted evidence after an authorized push, controlled release/migration acceptance, and restore rehearsal are not established by this document.
- `SEC-01` remains `[~]` In progress; the recorded manual desktop visual smoke remains outstanding. Deployment TLS enforcement, runtime secret injection, and release access controls still need verification. The one-off local `TrustServerCertificate=true` exception is not deployment validation or committed/worker-test configuration.
- Backup ownership, approved storage/access controls, cadence, retention, restore frequency, RPO, RTO, and approval responsibilities are undecided. This runbook does not assign values or create policy.
- Database configuration is not validated at packaged-JAR launch in this task; external properties-file discovery is not implemented/proven. Confirm the selected environment injection and launch path in a controlled test environment before claiming packaged release readiness.

See [REL-01 on the model task board](ke-hoach-trien-khai/03-model-task-board.md) for the tracked evidence and remaining acceptance work.
