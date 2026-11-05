# Phase 0: DB configuration resolution contract

## Scope and conclusion

This is a report-only architecture and contract review of `DatabaseConnection`, its callers, the example properties resource, README guidance, Maven packaging, and tests. No implementation or test code was changed. No local `db.properties`, environment values, or credential material were read, and no database was contacted.

The implementation already has a clear source order for the split settings: classpath `db.properties` values are the base, then nonblank `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` values replace them. A nonblank `DB_CONNECTION_STRING` is held separately and normally wins when opening a connection. Programmatic setters and lazy initialization create observable edge cases, however, and no focused tests currently define the complete resolution behavior. This report proposes a small pure resolver contract and synthetic tests; it does not authorize or apply implementation changes.

## Observed behavior

### Sources, precedence, and loading

`DatabaseConnection.loadConfiguration()` reads classpath resource `db.properties` and its `db.url`, `db.username`, `db.password`, and `db.driver` keys when the resource exists. It then reads the split environment keys `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_DRIVER`; null or blank values are ignored and accepted values are trimmed (`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java:35-58,67-70`). Property values are not trimmed by this code. Property resource and parse failures are silently ignored (`DatabaseConnection.java:35-45`). The checked-in example names these four values and also contains `db.pool.maxSize` (`clinic-infrastructure/src/main/resources/db.properties.example:1-7`).

The full environment key `DB_CONNECTION_STRING` is accepted only when nonblank, stored without trimming, and later used in preference to split settings (`DatabaseConnection.java:53-57,95-118`). On the normal first `getConnection()` path it wins over split configuration. No focused configuration test establishes precedence or whitespace semantics (`clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnectionTest.java:21-63` tests only diagnostic output with a synthetic JDBC driver).

Loading is lazy and guarded by a static synchronized once flag. `getConnection()` calls it after checking the programmatic full string; `configure()` calls it before replacing the split fields (`DatabaseConnection.java:60-65,76-88,95-119`). Consequently, the environment and classpath properties are sampled at most once. `setConnectionString()` does not trigger loading. If called with a nonempty value before the first `getConnection()`, its early return path bypasses loading altogether (`DatabaseConnection.java:76-78,95-103`).

### Programmatic interaction and connection handoff

`setConnectionString(value)` trims a non-null value and replaces the custom full string; null becomes empty. `configure(url, username, password)` lazily loads configuration, then replaces only the three split fields. It does not clear the custom full string (`DatabaseConnection.java:76-88`). Once loaded, a nonempty custom string wins before split settings. Clearing it with `setConnectionString("")` allows split settings to be used; however, clearing a pre-load setter and subsequently loading may pick up the environment full string. Conversely, setting a full string before first use skips lazy loading, so the same source values can behave differently depending on call order. These call-order effects are observed implementation details, not a desirable stable contract.

When no custom full string is selected, any non-null, nonempty URL is passed onward. Username is supplied only if non-null and nonempty; password completeness is not checked. With no username, the no-credentials `DriverManager.getConnection(url)` overload is used. A missing URL raises a generic `SQLException`; the code does not validate JDBC URL syntax or report incomplete credentials before driver handoff (`DatabaseConnection.java:109-118`).

`DB_DRIVER` and `db.driver` are assigned to `dbDriver`, but `dbDriver` is never read by the connection path. Driver selection is inferred from recognized URL prefixes; missing driver classes are ignored and `DriverManager` is called afterward (`DatabaseConnection.java:23-28,40-44,47-51,124-138`). The sample `db.pool.maxSize` is likewise unused by this class (`db.properties.example:7`).

Production callers share `DatabaseConnection.getConnection()`: `JdbcConnectionProvider.acquire()` obtains standalone connections (`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcConnectionProvider.java:14-18`), the default transaction manager uses it (`JdbcTransactionManager.java:17-22`), migrations use it (`DatabaseMigrationRunner.java:55-61`), and desktop authentication composition roots supply it to JDBC adapters (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/AuthenticationCompositionRoot.java:25-43,53-58`).

### README, packaging, and boundaries

README documents split environment keys or `DB_CONNECTION_STRING`, copying the example to `db.properties` in infrastructure resources, migrations, and `mvn package`; it gives no packaged-JAR external configuration path or launch command (`README.md:5-10`). The infrastructure module uses the default Maven resource layout and declares JDBC/JUnit dependencies (`clinic-infrastructure/pom.xml:11-41`). The root POM is a four-module build and sets Java release 17 (`pom.xml:7-27`). The desktop shade configuration excludes an archive entry named `db.properties` and sets the main class (`clinic-desktop/pom.xml:35-61`). The loader only searches the classpath and has no external path parameter (`DatabaseConnection.java:35-45`). Whether deployments need a separate external-JAR configuration mechanism remains unresolved; this report does not recommend one.

The example SQL Server URL contains `encrypt=true;trustServerCertificate=true` (`db.properties.example:3`). The selected URL is passed through to JDBC without TLS defaults in `DatabaseConnection` (`DatabaseConnection.java:95-118`). This report makes no TLS/certificate-policy choice and does not establish behavior for URLs omitting TLS options.

Existing `DatabaseConnectionTest` uses a fake JDBC driver to check that connection-check output omits synthetic product/version/user and SQL exception details (`clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnectionTest.java:21-63,90-162`). It does not test configuration resolution. SQL Server integration tests in infrastructure and desktop require explicit system-property opt-in before their database setup (`FreshDatabaseMigrationSqlServerTest.java:39-49`; `AuditSqlServerPersistenceTest.java:78-94`; `ReceptionSqlServerPersistenceTest.java:65-76`; `ReceptionSqlServerProcessRestartTest.java:66-79`).

## Proposed stable contract (for review; no code change)

Use a package-private or otherwise narrow, side-effect-free resolver whose inputs are explicit: base properties, an injected map of environment-like values, and explicit programmatic overrides. It returns an immutable resolved value or a safe validation error; it performs no `System.getenv`, file/resource I/O, driver loading, logging, or JDBC calls. A thin adapter owns one-time classpath/environment sampling and delegates to that resolver.

Proposed source precedence for split fields is: classpath properties as base; each nonblank split `DB_*` override replaces only its matching property; then a call to `configure` replaces the split URL/user/password fields. For the full-string channel, a nonblank explicit `setConnectionString` value is the highest-precedence override, followed by nonblank `DB_CONNECTION_STRING`; either takes precedence over split fields. A blank `setConnectionString` clears only that programmatic override and reveals the next available source. `configure` updates split fields and does not erase an independently selected full string. Normalize accepted override values by trimming for blank detection and storing the trimmed value, consistently; do not silently trim property values unless that choice is explicitly approved. This resolves the current call-order dependency while preserving the apparent intent of both programmatic APIs.

For validation, require a nonblank selected full string, or a nonblank split URL. Recommend that split credentials be either both absent/blank or both present; reject one-sided username/password as an incomplete pair. Do not parse database-specific URL grammar or inject TLS properties in the resolver: pass the JDBC URL unchanged after the chosen whitespace rule and let the selected driver validate its dialect. Treat unreadable/malformed properties as a safe configuration error instead of silently converting them to “missing” only if the implementation owner accepts that behavior change. Neither JDBC URL validity nor resource-loading failure behavior is currently an established contract.

Lazy behavior should be a single snapshot per `DatabaseConnection` lifecycle: first resolution-triggering operation samples the classpath/environment inputs once; subsequent calls reuse that snapshot, while later programmatic calls have deterministic precedence. `setConnectionString` should not accidentally skip initialization; it should update the explicit override independently. Test this with injected source counters rather than mutating process environment or resetting global static state.

### Unresolved decisions

- **`DB_DRIVER`:** Observed as accepted but unused. Recommendation: remove the apparent setting (property/example and environment read) unless there is a supported nonstandard driver use case; if such a use case exists, define how explicit class loading relates to JDBC 4 service loading and implement it deliberately. Do not silently implement or remove it as part of this report.
- **Partial credentials:** Existing behavior allows an absent username and does not validate a missing password. The pair-required proposal above needs confirmation against supported JDBC authentication modes; alternatively document and test the intended valid combinations.
- **Malformed inputs and property-load failures:** There is no dialect-independent test for a malformed JDBC URL before the driver sees it. Agree whether only structural incompleteness is rejected by the resolver and leave driver syntax errors downstream, and whether unreadable properties fail closed or retain current ignore behavior.
- **TLS and certificate validation:** Policy remains unspecified. The sample's explicit options are not a deployment policy.
- **External JAR configuration:** No external configuration path is implemented or documented. Whether to add one remains a deployment decision.

## Focused synthetic test sequence

Add tests around the pure resolver first; use sentinel strings such as `synthetic-password-marker` and `synthetic-jdbc-marker`, never real credentials. Keep all input maps in memory. Test at least:

1. Properties alone resolve to the base split URL/user/password.
2. Each nonblank split environment override replaces only the corresponding property; whitespace-only values leave the base untouched; accepted override whitespace follows the agreed trim rule.
3. A nonblank full environment string wins over conflicting split values; a blank full string falls back to split values.
4. Programmatic `setConnectionString` wins over full environment and split sources; a blank setter clears that layer; `configure` replaces split fields; both call orders (`configure` then setter, setter then `configure`) resolve consistently, and `configure` does not erase a selected full string.
5. Partial split input: URL only (allowed only if both credentials absent); username without password and password without username are rejected under the proposed pair rule. Missing/blank URL is rejected. A syntactically malformed/dialect-unknown URL is forwarded unchanged to a fake JDBC boundary or gets a structural validation result, per the agreed boundary; it must never open a real connection.
6. Property values with surrounding whitespace and malformed/unreadable property input have explicit assertions for the selected contract; do not rely on a real resource or filesystem.
7. Resolution and error rendering do not contain either synthetic marker. The existing fake-driver redaction tests remain applicable to connection-check diagnostics (`DatabaseConnectionTest.java:24-63`).
8. One-time loading: a fake input source is invoked once across multiple resolutions; programmatic updates do not trigger re-reading. Avoid global environment mutation and static-state reflection.

Then route the resolved immutable configuration to a fake JDBC boundary that captures the selected URL/credential overload and counts invocations. Assert that invalid configuration invokes it zero times and that diagnostics omit sentinel markers. Do not contact SQL Server or enable the opt-in integration tests for these contract tests.

## Smallest implementation/test sequence

1. Agree the precedence and unresolved policy choices above, especially `DB_DRIVER`, partial credentials, property whitespace/load failures, TLS policy, and external configuration needs.
2. Extract the pure resolver and add in-memory synthetic tests for its matrix; retain a narrow loader adapter for classpath/environment sampling and test it through injected sources.
3. Add fake JDBC handoff tests for valid resolution, invalid zero-call behavior, and redaction; leave SQL Server tests gated.
4. Separately, if an external-JAR path is selected, document and test that launch/configuration path. If TLS policy is selected, test the configured options at the resolver boundary and reserve certificate handshake testing for an authorized integration environment.

This sequence establishes configuration behavior only. It does not establish deployment readiness.

## Guidance read

- `docs/PROJECT_CONVENTIONS.md` — supplied in the task guidance; no repository copy was found.
- `README.md` — supplied task guidance and repository lines 1-19 read.
- `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, and `UI_CODING_STANDARDS.md` — supplied task guidance.
- `docs/ke-hoach-trien-khai/README.md` — read in the repository.
- `docs/ke-hoach-trien-khai/phase0-db-config-review.md` — read for existing evidence/report context.
- Repository `AGENTS.md` files — searched for; none found.

## Validation and scope

- Inspected source, caller, test, README, example, and Maven POM references cited above. The local `db.properties` was excluded from all searches and not opened; environment values and credentials were not read; no database was contacted.
- No implementation or tests were changed. JDK 17.0.17 was selected explicitly. `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home /opt/homebrew/bin/mvn -Djava.awt.headless=true verify` was attempted and failed before compilation while Maven tried to open the shared local-repository lock for `maven-resources-plugin:3.5.0` (`~/.m2/repository/.locks/...3.5.0.lock`); the domain module failed and later modules were skipped. No cache was moved, deleted, or modified as a workaround. Thus JDK 17 Maven verify did not pass in this run.
- `git diff --check` passed; `git status --short` showed only this report changed.
