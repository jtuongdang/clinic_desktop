# Operations and release runbook

**Scope:** local or controlled test deployment of Clinic Management Desktop. This is an evidence-based procedure, not a production release policy. No database, migration, backup, or restore operation was performed while preparing this document.

## Evidence labels

- **Source-proven** means the command or behavior is present in repository configuration/source. It proves the defined local build/test flow, not a release approval.
- **Illustrative template** means an operator must replace placeholders and adapt the operation to an approved, isolated synthetic test environment. It is not a ready-to-run command and must never point at clinic or production data.

Source links below are relative to this document. The project overview and current implementation limits are also recorded in [00 — Hiện trạng](ke-hoach-trien-khai/00-hien-trang.md).

## 1. Prerequisites and scope

- Use JDK 17 and Maven from the repository root. The parent POM sets `maven.compiler.release` to 17 and aggregates `clinic-domain`, `clinic-application`, `clinic-infrastructure`, and `clinic-desktop` ([root `pom.xml`](../pom.xml)). Maven Wrapper is not present in this checkout.
- A SQL Server is needed only for separately authorized opt-in integration checks or a controlled migration rehearsal. The default Maven verify does not need a database. Opt-in integration tests have explicit flags and enforce a `clinic_test_*` database name; use synthetic test data only ([migration SQL Server test](../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/FreshDatabaseMigrationSqlServerTest.java), [reception SQL Server test](../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/reception/ReceptionSqlServerPersistenceTest.java)).
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

The connection loader reads classpath `db.properties`, then applies nonblank `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment overrides. A nonblank `DB_CONNECTION_STRING` takes precedence over those split fields. Configuration is sampled once by the configuration object; start a fresh process after changing environment values ([`DatabaseConnection`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java), [`DatabaseConfiguration`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java)).

For the packaged JAR, the shaded archive excludes `db.properties`. The checked-in example says to copy to `db.properties` for development, but the loader only discovers a classpath resource; an adjacent external properties file is not a proven packaged-JAR configuration path. For a packaged local/test run, supply environment variables through a protected local environment/secret injector. Do not commit a local properties file or `.env`. The sample connection URL uses `trustServerCertificate=true`; this is example configuration, not evidence of an approved TLS/certificate policy ([example properties](../clinic-infrastructure/src/main/resources/db.properties.example), [DB configuration review](ke-hoach-trien-khai/phase0-db-config-review.md)).

**Illustrative template only — placeholders are not credentials or an executable endpoint.** Use only an approved synthetic database name matching `clinic_test_*`; set secret values out of band rather than pasting them into a terminal command or shell history.

```text
DB_URL = jdbc:sqlserver://<approved-test-host>:1433;databaseName=clinic_test_<unique_suffix>;encrypt=true;<approved-certificate-options>
DB_USERNAME = <test-login-from-secret-store>
DB_PASSWORD = <test-secret-from-secret-store>
```

Alternatively, the implementation accepts `DB_CONNECTION_STRING` in place of the split values. Do not record or print its contents. `DB_DRIVER` and the example `db.pool.maxSize` are not used by the inspected connection path; do not rely on them for configuration ([`DatabaseConnection`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java), [Phase 0 configuration review](ke-hoach-trien-khai/phase0-db-config-review.md)).

## 4. Migration preflight and execution

### Preflight

1. Confirm this is an isolated synthetic acceptance database, not `clinicDb`, a clinic database, or any production system. Confirm the selected server and database using the approved DBA/client procedure without printing credentials or full connection strings.
2. Confirm the test DB has no unrelated objects or data. The fresh migration integration test refuses a selected database containing user objects and restricts the name to `clinic_test_*`; it is opt-in evidence, not a guarantee for manual runs ([`FreshDatabaseMigrationSqlServerTest`](../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/FreshDatabaseMigrationSqlServerTest.java)).
3. Review the pending migration SQL files and establish a tested backup plus a restore destination before changing a database. Confirm no competing migration process is running.
4. Ensure the process environment is configured before starting Java. Recheck the target database identity; the runner does not accept a separate database-name argument.

### Run the migration runner

The runner registers V1 through V9, creates `__schema_migrations` when missing, skips scripts recorded as successful, splits SQL Server `GO` batch separators, and records each successful script. It commits the migration tracking-table creation and each script separately; a script failure rolls back that script transaction and reports failure. This is not an all-migrations atomic transaction ([`DatabaseMigrationRunner`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunner.java), [migration resources](../clinic-infrastructure/src/main/resources/migrations/)).

After building the shaded artifact and configuring the synthetic test environment, the following invocation is **source-proven** by the README and runner entry point. It connects to the database selected by the process configuration and can change that database; execute only after the preflight above and separate authorization for the controlled test migration:

```bash
java -cp clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar \
  vn.clinicmanagement.infrastructure.persistence.DatabaseMigrationRunner
```

The current runner returns a nonzero process exit status on migration failure ([`DatabaseMigrationRunner`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunner.java)). A successful exit is necessary but not sufficient: verify all expected migration names in `__schema_migrations`, inspect representative expected schema/seed state, and ensure the test application can read back synthetic records. Avoid capturing credential-bearing diagnostic context.

**Opt-in SQL Server checks:** current integration tests require explicit test properties and a configured test connection. The fresh schema test uses `clinic.sqlserver.migration.enabled=true`; the reception test uses `clinic.test.sqlserver=true`. Both are destructive-capable integration paths and their fixtures may clean generated test rows. Inspect the test source and configure only a disposable `clinic_test_*` database before opting in. These checks were not run for this runbook task. Do not use undocumented flag combinations or point tests at an existing clinic database.

## 5. Launch and basic post-launch checks

Launch the packaged desktop app with the same protected test environment:

```bash
java -jar clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar
```

This is the README launch command; `Main` creates the login form on Swing's Event Dispatch Thread ([`Main`](../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/Main.java)).

For a controlled smoke check, use only the synthetic acceptance database and authorized synthetic test account. Confirm the login window renders, sign-in responds, the intended test configuration is active, and a narrow permitted workflow can create then read back synthetic data. Confirm normal logout/exit. Do not use or repeat migration seed credentials; establish account reset/rotation for the controlled test environment before acceptance. Record the environment and result without patient data, secrets, or a full connection string.

The headless Maven verify and existing SQL Server persistence tests do not prove that a human-visible desktop session works. The current evidence says the Mac host was locked and the Swing window was not visually observed; visual UI smoke and full UI-to-database acceptance remain open ([00 — Hiện trạng](ke-hoach-trien-khai/00-hien-trang.md)).

## 6. Backup-before-change and restore rehearsal

The repository contains no backup scheduler, retention policy, restore automation, RPO/RTO, or backup acceptance procedure. Decide these with the responsible system/data owner before any non-disposable environment is considered. Do not infer a schedule or retention period from this runbook.

For a **controlled synthetic test rehearsal only**, before migration or another schema change:

1. Identify the exact synthetic source database and a separately provisioned backup destination with adequate access controls. Confirm the backup destination and available space with the operator.
2. Take a full backup using the approved SQL Server operations procedure. Record the database identity, backup artifact identifier/location, completion status, and time without recording credentials or sensitive records.
3. Restore that artifact into a separate isolated synthetic database. Do not overwrite the source database as part of a rehearsal.
4. Verify the restored database is online and run the approved integrity and application-level checks. Confirm the expected schema migration history and read back known synthetic fixtures. Record pass/fail and cleanup through the environment owner.
5. Only after successful restore verification proceed with the authorized test change. If restore cannot be verified, stop and resolve the backup/restore gate before changing the test database.

**Illustrative SQL Server templates only; replace every placeholder using the DBA's approved procedure. These templates were not executed. They are restricted to a synthetic test DB and are not production-ready commands.** Backup paths and restore file mappings are server-specific; validate them with the DBA. `WITH REPLACE` can overwrite data, so do not add it or target an existing database unless the separate isolated restore destination has been positively verified and the DBA explicitly approves the operation.

```sql
BACKUP DATABASE [clinic_test_<unique_suffix>]
TO DISK = N'<approved_backup_path>/<unique_backup_name>.bak'
WITH CHECKSUM, INIT;
```

```sql
RESTORE VERIFYONLY
FROM DISK = N'<approved_backup_path>/<unique_backup_name>.bak'
WITH CHECKSUM;
```

`RESTORE VERIFYONLY` checks the backup media but does not prove a usable database restore. A real rehearsal must restore to a new isolated synthetic database using server-specific file mappings, then run integrity and application readback checks. Do not run a destructive restore command from this document.

## 7. Rollback limits and recovery

No automated down migration or reverse migration scripts were found. The runner only applies the ordered forward migrations V1–V9 and records successful scripts; do not manually delete tracking rows or attempt to reverse schema changes by editing history ([`DatabaseMigrationRunner`](../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunner.java), [migration directory](../clinic-infrastructure/src/main/resources/migrations/)). Migration code has per-script failure rollback coverage, but that does not reverse earlier committed scripts or restore data changed by an already completed migration.

If a migration or release fails, stop further changes, preserve safe diagnostic evidence, identify which scripts completed, and involve the database/application owner. Recovery requires a verified pre-change backup and an approved restoration plan for an isolated target; restoring over a source is destructive and must be handled by the authorized DBA under a separately approved procedure. Application rollback may also require a compatible prior artifact and a schema compatibility decision. The repository does not establish that old application versions can run against the new schema. Do not claim rollback is complete until restored data and application checks pass.

## 8. Open operational gates

- REL-01 remains incomplete: CI-hosted evidence after an authorized push, controlled release/migration acceptance, and restore rehearsal are not established by this document.
- Visible Swing UI smoke and full UI-to-database acceptance remain unverified because the host was locked during the recorded review.
- Security gates remain open: Phase 2 authorization is a hard dependency for sensitive workflows, and deployment TLS/certificate validation, runtime secret injection, and release access policy need explicit decisions/evidence. The example's certificate-trust option is not an approved policy.
- Backup ownership, approved storage/access controls, cadence, retention, restore frequency, RPO, RTO, and approval responsibilities are undecided. This runbook does not assign values or create policy.
- Database configuration is not validated at packaged-JAR launch in this task; external properties-file discovery is not implemented/proven. Confirm the selected environment injection and launch path in a controlled test environment before claiming packaged release readiness.

See [REL-01 on the model task board](ke-hoach-trien-khai/03-model-task-board.md) for the tracked evidence and remaining acceptance work.
