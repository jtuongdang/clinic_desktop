# Phase 0 — Domain event transaction boundary review

**Task:** `PLAT-EVENT-BOUNDARY-REVIEW-01`; approved contract v1 accepted.

**Snapshot:** 2026-11-06, local source at `fbb95763a4b820225b37433e315def21cecc9351`.

**Status:** Report only. Options below require Owner decisions and a separate implementation contract. This review does not complete `PLAT-EVENT-01`, audit, or any clinical workflow.

## Scope and conclusion

The current implementation has three separate primitives: an aggregate-local event buffer, a JDBC transaction boundary with a row-count Unit of Work, and a synchronous in-process dispatcher. There is no production collector connecting them. The dispatcher does not establish delivery relative to commit or rollback. Source and test evidence are distinguished below; source inspection is not SQL Server runtime acceptance.

The approved output is this file only. No Java, tests, schema, consumers, business events, grants, or other documents are changed. No database, external system, push, PR, or merge is involved. The task's report-only contract takes precedence over the generic checklist instruction to add tests: existing synthetic tests are inspected and a focused run is attempted; future tests are described as proposals.

## Guidance read and evidence method

Read before editing:

- `docs/PROJECT_CONVENTIONS.md` — Orchestrator baseline supplied in the task; no repository copy exists. Applied single-file scope, source-first review, inward dependencies, synthetic data, and truthful validation reporting.
- `README.md` — supplied guidance and current repository version; Java 17/Maven multi-module build and configuration boundaries.
- `DOMAIN_ANALYSIS.md` — supplied guidance and current architecture/common/events/repository sections, especially lines 42–48, 90–133 and 302–323. Descriptive claims of completeness are checked against Java rather than adopted as acceptance.
- `ERD_DIAGRAM.md` — supplied guidance and repository schema context; no schema design or changes are authorized here.
- `UI_CODING_STANDARDS.md` — supplied guidance and current threading/architecture sections, especially lines 61 and 521–530; I/O stays off Swing EDT and UI changes belong on EDT.
- `docs/ke-hoach-trien-khai/README.md` and `00-hien-trang.md` — completion requires evidence for a working flow; primitives do not close a phase.
- `docs/ke-hoach-trien-khai/phase0-domain-events-review.md` — historical review plus dispatcher/UTC addenda, reconciled with current source. The historical claims that no publisher exists and that event snapshots/null handling are missing no longer describe this checkout.
- `docs/ke-hoach-trien-khai/03-model-task-board.md` — event-related entries/addenda inspected for parent-task scope; no statuses changed.
- `docs/ke-hoach-trien-khai/phase0-db-config-resolution-contract.md` — inspected as an existing report-only document pattern.

Searched repository and relevant ancestor directories for `AGENTS.md`; none found. `CONTRIBUTING.md`, `ARCHITECTURE.md`, `docs/DEVELOPMENT_CONVENTIONS.md`, and `docs/README.md` are absent. Missing repository guidance is reported, not synthesized.

Java citations use repository-relative links with exact line ranges. Searches cover all four modules' `src` trees. Negative wiring findings are bounded to those trees at this snapshot, not a claim about future or external consumers.

## Implemented behavior

### Event collection and identity

| Component | Current behavior and exact evidence | Limit |
|---|---|---|
| `BaseEntity` buffer | `transient List<IDomainEvent>` at [BaseEntity.java:28][buffer]. Registration rejects null and appends; `getDomainEvents()` copies to an unmodifiable snapshot; `pullDomainEvents()` copies and clears immediately; `clearDomainEvents()` discards pending entries ([BaseEntity.java:103–125][collection]). | These methods neither persist nor publish. Snapshot immutability protects the list, not arbitrary event payload objects. There is no transactional acknowledgement, rollback restoration, or cross-aggregate ordering. |
| Aggregate/UoW contracts | [IAggregateRoot.java:1–3][aggregate] is a marker; [IUnitOfWork.java:1–6][uow-port] exposes only `saveChanges()` and `saveEntities()`. [JdbcUnitOfWork.java:9–29][uow] counts positive changed rows in a `ThreadLocal`, then removes the count. | No aggregate tracking, event queue, commit callback, or durable event storage is supplied by these contracts. `saveEntities()` means a positive count was drained, not that a DB commit or event delivery succeeded. |
| Metadata | [IDomainEvent.java:9–18][metadata-port] exposes UUID and UTC-valued `LocalDateTime`; [AbstractDomainEvent.java:9–25][metadata] generates UUID per object and derives UTC fields from an injectable clock, defaulting to `Clock.systemUTC()`. | Occurrence time is construction time, not commit time. UUID is event identity, not aggregate identity or a command idempotency key. Reconstructing an event generates a new UUID. |
| Identity/payload timing | [BenhNhan.java:51–64][patient] has an empty no-arg constructor and a parameterized constructor registering an existing `BenhNhanRegisteredEvent` with ID fallback `0`. Its payload fields are final copied values ([BenhNhanRegisteredEvent.java:3–17][patient-event]). JDBC assigns the generated patient ID during insert, before commit ([BenhNhanRepositoryImpl.java:156–178][patient-insert]). | Collecting that already-created event after insert/commit does not repair its copied `patientId=0`. Later entity setters do not update copied name/identity values. Valid final ID and payload acceptance remain separate from dispatch timing. |
| Actual reception path | The form uses `new BenhNhan()` and setters ([PatientRegistrationFormPanel.java:346–364][form]); [RegisterPatientHandler.java:71–146][registration] adds/updates it and writes the visit within `inTransaction`. | A collector alone would not make this path emit the constructor event. This report does not authorize changing emission points or creating additional events. |

The current event-buffer tests cover registration, null rejection, immutable snapshots, draining and repeated empty pull ([BaseEntityDomainEventsTest.java:15–74][collection-test]). UTC conversion and null-clock tests are at [AbstractDomainEventTest.java:17–35][metadata-test]. Existing aggregate tests also assert start/completion/lock event payloads and invariant failures ([LuotKhamTest.java:27–167][visit-test]), using **preassigned** visit/patient/doctor IDs at [LuotKhamTest.java:170–176][visit-fixture]. These are meaningful domain assertions; they do not establish post-insert identity mapping or transaction-bound delivery. The corresponding existing emission points are [LuotKham.java:88–123][visit].

### Transaction boundary and dispatcher

`TransactionBoundary` accepts a `Supplier<Result<T>>` and requires failed results to roll back ([TransactionBoundary.java:5–8][boundary]). The JDBC implementation behaves as follows:

1. A nested invocation reuses the current thread's connection. A null/failed nested result or nested `RuntimeException` marks the outer transaction rollback-only; the nested call never commits ([JdbcTransactionManager.java:27–37][nested]). There are no nested savepoints or inner commit notifications.
2. The outer invocation opens a connection, disables autocommit, attaches thread state, resets the row count, and executes the supplier. A supplier `RuntimeException` triggers an explicit rollback attempt and returns generic `TRANSACTION_FAILED`, even if rollback itself throws `SQLException` ([JdbcTransactionManager.java:39–55][operation-failure]).
3. Null/failed results and rollback-only trigger rollback. An otherwise successful result with rollback-only becomes `TRANSACTION_ROLLBACK_ONLY`. Success drains the row count and then calls `connection.commit()` ([JdbcTransactionManager.java:56–66][commit]). JDBC writes already happened inside the supplier; `saveChanges()` is not deferred persistence.
4. The outer catch maps `SQLException` or `RuntimeException` to generic failure. `finally` removes connection/rollback flags and clears only UoW row state ([JdbcTransactionManager.java:67–73][cleanup]). It does not restore mutated entity fields or manage aggregate event buffers.

Repositories borrow the same connection only through the same manager/provider on the same thread; outside it, the provider opens an independent connection ([JdbcConnectionProvider.java:14–35][provider]). Reception composition shares that manager/provider/UoW and creates no publisher ([ReceptionCompositionRoot.java:20–37][composition]). Moving handlers to another thread would lose this transaction association.

`InProcessDomainEventPublisher` stores instance-local registrations, rejects null arguments, and dispatches only the exact runtime event class in registration order on the caller's thread. No registration is a no-op. A handler exception propagates unchanged and stops later handlers ([InProcessDomainEventPublisher.java:18–54][publisher]). It offers no retry, deduplication, aggregate draining, transaction handling, durable acknowledgement, or declared concurrent-registration guarantee. Tests cover these dispatch rules, including original exception identity and instance isolation ([InProcessDomainEventPublisherTest.java:17–95][publisher-test]). A successful `publish()` with no handlers is therefore not evidence of a business effect.

Search for `getDomainEvents`, `pullDomainEvents`, `clearDomainEvents`, publisher/handler types, `registerHandler` and `.publish(` finds no production collector, publisher instantiation, or registered production consumer in the four modules. Buffer access beyond its implementation is in tests; the production publisher type is an isolated primitive. There is no existing end-to-end collect → dispatch → commit sequence to endorse.

Existing reception audit is explicit application orchestration: success audit calls `appendInCurrentTransaction` ([RegisterPatientHandler.java:177–184][audit-call]); denial audit uses an independent append ([RegisterPatientHandler.java:155–169][audit-denial]). The JDBC audit adapter requires an active transaction for the former and opens/commits its own connection for the latter ([JdbcAuditRepositoryImpl.java:42–111][audit-adapter]). These are separate from domain-event consumers. This review neither replaces that behavior nor claims broader audit completion.

## Failure and rollback implications

| Scenario | Established behavior / bounded implication |
|---|---|
| Operation returns failure/null; nested failure is ignored | Current manager explicitly rolls back the outer connection. Tests assert failed/null results and rollback-only for nested failed results and runtime failures ([JdbcTransactionManagerTest.java:33–58,108–142][transaction-tests]). A future collector must not dispatch committed effects at inner return. |
| Supplier throws `RuntimeException`; rollback also fails | An explicit rollback is attempted; the client receives generic failure and thread/UoW state is cleaned ([JdbcTransactionManagerTest.java:60–106][rollback-tests]). A failed rollback attempt does not prove DB rollback succeeded. Aggregate state and its events remain outside this cleanup. |
| Commit or resource close throws | Source catches these failures and returns `TRANSACTION_FAILED`, with no explicit rollback in that outer catch ([JdbcTransactionManager.java:64–73][commit-cleanup]). If commit acknowledgement is lost, the DB outcome can be unknown; close could fail after commit succeeded. Current synthetic connection supports setup/rollback faults, but does not inject commit/close faults ([JdbcTransactionManagerTest.java:210–259][connection-double]). Do not equate every returned failure with proven absence of writes or automatically replay the command. |
| Proposed handler before commit fails | If invoked inside the supplier and the failure escapes as a runtime exception, current rollback handling applies; a failure converted to `Result.failure` also triggers rollback. Placing a future dispatch call beside `saveChanges()` outside the supplier's inner try would **not** get that explicit rollback path. A future design must define rollback handling for the entire pre-commit phase. Already completed handler effects roll back only when they use the same JDBC transaction; UI, external I/O and independent DB writes cannot be undone by it. |
| Proposed handler after commit fails or process exits | The committed business write cannot be undone by a dispatcher exception. In-memory events can be lost between commit and delivery; earlier handlers may have completed before a later failure. Retrying a batch may repeat those effects. No exactly-once guarantee exists. |
| Events pulled before failure, or entity reused after rollback | Pull already emptied the aggregate; retaining events instead leaves stale events/state on a reused entity. Current transaction cleanup does neither event restoration nor object rollback. A future contract must specify transaction-owned pending batches and disposal/reload on failure, rather than silently reusing mutated objects. |

Sequential cleanup/connection reuse is covered at [JdbcTransactionManagerTest.java:144–203][cleanup-tests]. The reception audit-failure test asserts staged writes are discarded ([RegisterPatientHandlerTest.java:703–718][audit-test]), but uses a local `TransactionProbe` ([RegisterPatientHandlerTest.java:1029–1053][transaction-probe]); it is not JDBC or SQL Server evidence for event dispatch. No inspected test combines the real dispatcher, aggregate collection and the JDBC boundary.

## Minimal decision matrix — proposed options, not approval

The following are alternatives for a later contract. They do not select an implementation or enable any consumer.

| Decision | Current state | Bounded options and tradeoff | Unresolved Owner choice |
|---|---|---|---|
| Collection owner/timing | Aggregate-local buffer; no collector. | Explicit application collection after writes is narrow but can miss aggregates; transaction-scoped tracking covers participating aggregates but adds lifecycle/API responsibilities. A pulled batch must have a defined owner through success/failure. Preserve per-aggregate order; cross-aggregate order and handler-produced events need an explicit bounded policy. | Which approved use cases/aggregates participate? Snapshot then clear, or pull into a transaction-owned batch? How are pending and newly raised events disposed on rollback? |
| Aggregate identity/payload | Copied payload may precede generated ID; real reception uses no-arg construction. | An approved post-identity creation point can produce final payload; preassigned stable identity would change identity allocation; deferred materialization would change the event contract. Simply delaying publication fixes none of the copied payload defects. | Which existing emission points and payload fields are valid, at what moment, and which IDs must be present? Any payload privacy/minimization rules require explicit approval; names/identity numbers are not automatically suitable for storage or logging. |
| Dispatch relative to commit | No wiring or callback. | Before outer commit: same-connection handler writes can be atomic, but hold locks longer and must fail the whole transaction. After confirmed commit **and cleanup**: suitable only for effects permitted to fail separately; creates a crash/loss window. Immediate publication at domain mutation exposes effects of later rollback and is unsuitable for a committed-effect guarantee. | For each future approved consumer, is its effect required to share rollback, or may it occur/fail after commit? No audit-to-event mapping is chosen here. |
| Handler failure/result | Dispatcher is synchronous and stops on first thrown exception. | Pre-commit failure should reach a rollback-capable boundary if atomicity is required. Post-commit failure needs a distinct delivery outcome so callers do not replay committed business writes. Continue-on-error would require changing the dispatcher contract and per-handler outcome tracking. | Required failure visibility, whether later handlers run, zero-handler policy, ordering and any reentrancy limits. Define treatment of commit-unknown outcomes separately from delivery failure. |
| Retry/outbox | No event retry or outbox is implemented; SQL resource search finds no outbox/event-store declarations in current migrations V1–V11. | Keep in-memory dispatch only if loss/retry limits are accepted. If durable delivery of a committed effect across restart becomes a requirement, consider outbox intent persisted in the **same** transaction and delivered after commit. That adds schema, serialization, relay, retention, recovery and operational ownership; delivery may duplicate, so consumers need idempotency. | Is durable delivery actually required, with what recovery time and duplicate tolerance? Choose retry scope, stable event/command identity and per-consumer idempotency before authorizing any retry/outbox work. |

**Review assessment:** current requirements authorize analysis and an isolated publisher foundation; they do not establish a durable-delivery requirement or an approved consumer. There is insufficient evidence to justify implementing retry/outbox now. This is a reason to leave that choice open, not proof that durability will never be needed. Blind command retry is especially unjustified after commit/close uncertainty. An outbox would not itself complete audit or clinical workflows.

For a proposed post-commit path, cleanup matters: `currentConnection` remains attached between `commit()` and `finally`, and provider activity is based only on connection presence. A callback inserted there could borrow a connection whose original transaction has already committed ([JdbcTransactionManager.java:64–76][commit-cleanup], [JdbcConnectionProvider.java:14–20][provider]). Post-commit handler writes need their own explicit transaction policy after cleanup; this report does not add one. Future UI consumers would also need a separate EDT handoff under the UI guidance, without transferring the JDBC transaction across threads.

## Proposed acceptance evidence for a later implementation contract

Use synthetic events/entities and injected connection doubles; do not add new clinical/business event definitions merely to exercise the boundary. These are gaps to test after Owner decisions, not tests added or passed by this review:

1. Collection exactly once per participating aggregate/batch, immutable snapshots, ordering, empty/no-handler policy, nested success without early dispatch, and handler-produced events under the chosen bounded policy.
2. Final generated aggregate identity and accepted payload on approved real creation/update paths, including no-arg versus parameterized construction; reject stale/zero identity where the future contract requires it. Keep UTC metadata tests separate from commit timing.
3. No committed-effect dispatch on failed/null result, operation exception, nested rollback-only or rollback failure; no stale events on the next transaction or reused object. Verify entity reload/disposal policy rather than assume JDBC rollback restores memory.
4. Pre-commit handler failure rolls back all same-connection writes and stops/continues only as approved. Inject faults in the proposed collector/flush phase as well as in the supplier.
5. Commit failure, acknowledgement uncertainty and close-after-commit failure have explicit safe results; post-commit handler failure never causes business command replay or claims rollback. Verify cleanup before any post-commit handler acquires a connection.
6. Only if durable delivery is separately selected: synthetic crash/restart, rollback of outbox intent with business writes, duplicate delivery, stable identity, handler idempotency and bounded retry/exhaustion evidence. Real SQL Server acceptance would require a separately authorized synthetic environment; none is contacted here.

## Validation and acceptance record

Initial `git status --short` was clean. Source inspection used `rg --files`, `rg -n`, and `nl -ba` over the cited Java/tests and relevant guidance. Wiring search:

```sh
rg -n 'getDomainEvents|pullDomainEvents|clearDomainEvents|registerDomainEvent|IDomainEventPublisher|IDomainEventHandler|InProcessDomainEventPublisher|registerHandler|\.publish\(' clinic-{domain,application,infrastructure,desktop}/src
rg -n -i 'outbox|eventstore|event_store|domain.?event' clinic-infrastructure/src/main/resources -g '*.sql'
```

The first search confirms the bounded wiring findings above. The SQL search returns no matches (exit 1); this is a source search, not database inspection.

No Maven wrapper is present. The default Java was 27; the focused attempt selected installed JDK 17.0.17 with Maven 3.10.0 and offline mode:

```sh
env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home mvn -o -pl clinic-infrastructure -am -Djava.awt.headless=true -Dtest=BaseEntityDomainEventsTest,AbstractDomainEventTest,LuotKhamTest,InProcessDomainEventPublisherTest,JdbcTransactionManagerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

**Actual result:** exit 1, `BUILD FAILURE` before test execution. Maven could not open the lock file `/Users/jtuong/.m2/repository/.locks/artifact~org.apache.maven.plugins~maven-resources-plugin~pom~3.5.0.lock` after five attempts. Parent succeeded; domain failed during plugin resolution; application/infrastructure were skipped. The shared cache is outside sandbox writable roots. No cache was copied, moved, deleted or modified to work around this blocker. No tests are claimed to pass in this run; historical build totals are not substituted. No SQL opt-in tests were enabled.

| Acceptance criterion | Result and evidence |
|---|---|
| Only the approved report changes | PASS — final status/scope check lists only this Markdown file; no Java/test/schema changes. |
| Exact Java/test citations for collection, transactions and dispatcher | PASS — source links above identify methods and focused test assertions, with gaps and source-only conclusions stated. |
| Current behavior, options and Owner decisions separated | PASS — implemented behavior, failure analysis, decision matrix and future acceptance evidence are distinct; implementation remains unapproved. |
| No invented events, consumer wiring or audit/transaction completion claims | PASS — only existing event examples are analyzed; parent/workflow completion remains open. |
| `git diff --check`, report whitespace and source-link checks | PASS — checked after writing; because the report is new/untracked, `git diff --no-index --check -- /dev/null docs/ke-hoach-trien-khai/phase0-domain-event-transaction-boundary-review.md` also checks its content. Local citation validation checks target existence and Java line bounds. |
| Guidance and validation evidence reported | PASS — guidance inventory and actual Maven blocker are recorded here. Runtime validation remains BLOCKED as stated above. |

Final local checks: `git diff --check` exit 0; the new-file `git diff --no-index --check` command above exit 1 with no whitespace diagnostics (the file differs from `/dev/null`); Python source-reference validation PASS (36 Java links, all referenced targets and line bounds valid); Python scope/whitespace validation PASS (only this report changed/untracked; no trailing whitespace). The initial new-file check flagged two Markdown hard-break trailing spaces; those were removed before the final checks.

[buffer]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/BaseEntity.java#L28
[collection]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/BaseEntity.java#L103-L125
[aggregate]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/IAggregateRoot.java#L1-L3
[uow-port]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/IUnitOfWork.java#L1-L6
[uow]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcUnitOfWork.java#L9-L29
[metadata-port]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/events/IDomainEvent.java#L9-L18
[metadata]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/events/AbstractDomainEvent.java#L9-L25
[patient]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/patient/BenhNhan.java#L51-L64
[patient-event]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/events/BenhNhanRegisteredEvent.java#L3-L17
[patient-insert]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/patient/BenhNhanRepositoryImpl.java#L156-L178
[form]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/PatientRegistrationFormPanel.java#L346-L364
[registration]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java#L71-L146
[collection-test]: ../../clinic-domain/src/test/java/vn/clinicmanagement/domain/common/BaseEntityDomainEventsTest.java#L15-L74
[metadata-test]: ../../clinic-domain/src/test/java/vn/clinicmanagement/domain/common/events/AbstractDomainEventTest.java#L17-L35
[visit-test]: ../../clinic-domain/src/test/java/vn/clinicmanagement/domain/examination/LuotKhamTest.java#L27-L167
[visit-fixture]: ../../clinic-domain/src/test/java/vn/clinicmanagement/domain/examination/LuotKhamTest.java#L170-L176
[visit]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L88-L123
[boundary]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/common/TransactionBoundary.java#L5-L8
[nested]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcTransactionManager.java#L27-L37
[operation-failure]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcTransactionManager.java#L39-L55
[commit]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcTransactionManager.java#L56-L66
[cleanup]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcTransactionManager.java#L67-L73
[provider]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcConnectionProvider.java#L14-L35
[composition]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionCompositionRoot.java#L20-L37
[publisher]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/events/InProcessDomainEventPublisher.java#L18-L54
[publisher-test]: ../../clinic-application/src/test/java/vn/clinicmanagement/application/events/InProcessDomainEventPublisherTest.java#L17-L95
[audit-call]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java#L177-L184
[audit-denial]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java#L155-L169
[audit-adapter]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/system/JdbcAuditRepositoryImpl.java#L42-L111
[transaction-tests]: ../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/JdbcTransactionManagerTest.java#L33-L142
[rollback-tests]: ../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/JdbcTransactionManagerTest.java#L60-L106
[commit-cleanup]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcTransactionManager.java#L64-L76
[connection-double]: ../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/JdbcTransactionManagerTest.java#L210-L259
[cleanup-tests]: ../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/JdbcTransactionManagerTest.java#L144-L203
[audit-test]: ../../clinic-application/src/test/java/vn/clinicmanagement/application/reception/RegisterPatientHandlerTest.java#L703-L718
[transaction-probe]: ../../clinic-application/src/test/java/vn/clinicmanagement/application/reception/RegisterPatientHandlerTest.java#L1029-L1053

## Owner decision addendum — 2026-11-06

**Approved Orchestrator contract:** `a43850af-4577-4ce7-9fc8-48f0f4f8497c`.

**Decision basis:** Mandatory RAG was completed successfully in the Tech Lead checkout immediately before dispatch. The retrieved review decision matrix confirms there is no production collector or consumer; it lists isolated/in-memory dispatch, pre-commit DB-only effects, post-commit best effort with a crash/loss window, and a same-transaction outbox as alternatives that are not approved by default.

**Owner choice A:** Keep event buffers and the in-process publisher isolated until a concrete consumer/use case is approved.

The existing event foundation therefore remains isolated primitives; this decision does not wire collection or delivery, and `PLAT-EVENT-01` remains **not complete**. No events are claimed delivered, audited, or durable, and no workflow is enabled. The alternatives in the historical decision matrix remain unselected; this addendum does not approve any other option.

Consumer-specific decisions are deferred until such a consumer exists: the effect it performs, applicable privacy constraints, whether its effects participate in commit/rollback, and its failure, retry, and idempotency behavior. No collector, transaction/Unit of Work wiring, handler, retry, or outbox is authorized by this decision.
