# Phase 3 — Transfer queue allocation and queueDate persistence proposal

**Status: NON-BINDING — Owner review required.** Snapshot: 2026-11-06. Scope: `VIS-TRANSFER-OPEN-DECISION-PROPOSALS-01`, queue-allocation detail under approved `VIS-TRANSFER-OWNER-DECISION-RECON-04`. This document approves no implementation, DDL, permission, audit-policy change or runtime enablement. SEC-01 remains open and transfer remains default-deny. Only this new Markdown file is changed.

## 1. Approved boundary and evidence labels

**Approved policy**, from the supplied contract and the current [transfer reconciliation][contract] / [decision-gap reconciliation][gaps]:

- RECEPTIONIST transfers only `CHO_KHAM`, between departments within the session facility; source access remains scoped to the session pair. DOCTOR transfers only `CHO_KHAM`, `DANG_KHAM`, `CHO_CLS`, within the session-selected department/facility. Deny final/completed, cancelled, soft-deleted or EMR-locked visits. Require an active destination room belonging to the destination department and facility. No destination patient/history/queue-read permission follows from transfer ([role/scope policy][scope]).
- Assign a **new destination-room allocation**, preserving persisted `dnVnTien` exactly; never auto-promote. Identical digits in two rooms can still represent different allocations. Preserve visit status, clinical `ngayKham`, reception time and existing examination timestamps, including `thoiGianBatDauKham` and `thoiGianKetThucKham`; allocation must not reset examination state or invoke start/complete methods.
- `queueDate` is the separate transfer-local calendar date in `Asia/Ho_Chi_Minh`. It is not a renamed or rewritten `ngayKham`. Technical/audit metadata uses UTC ([DEC-TIME-01][time]). Date storage and the precise sampling instant remain open.
- A fixed transfer reason code is mandatory, with an optional short note; do not reuse `lyDoKham` or `lyDoChuyenTuyen`. Taxonomy, normalization, length and privacy detail belong to the separate reason decision.
- Preserve visit identity, linked work and billing without cancellation, mutation or duplicate charges. Pending work requires proven safety and preserved performing unit/responsibility, otherwise completion/handoff or denial. Issued prescriptions, confirmed results and posted financial transactions remain immutable. Missing eligibility readers must fail closed ([linked-work review][linked]). Transaction consistency, optimistic concurrency, domain invariants and exception-path tests are mandatory.

Below, **Fact** means directly inspected checkout evidence; **Inference** means a technical consequence, not an approved policy; **Proposal** means an option or recommendation awaiting Owner choice. Earlier proposals are evidence of analysis, not approval. No rule equating `ngayKham` to today's date is introduced here.

## 2. Current evidence and reconciliation with prior proposals

| Evidence | Fact in this checkout | Inference / limit |
|---|---|---|
| [V1 visit table:329–380][schema], [queue index:1142][index] | `LuotKham` has nullable room/number/display; required `ngayKham DATE`; priority, status, timestamps, EMR/deletion flags and `version BIGINT`. Room/department/facility FKs are individual. The active-only date/room/status index is nonunique. | Current schema cannot store an independent queue date. Individual FKs do not enforce the full location hierarchy or number uniqueness. |
| [Registered migrations:44–56][migrations] and targeted search of V1–V11 SQL | No separate `queueDate`, queue counter, allocation table or queue-number unique key was found in registered SQL. | Bounded to checked-in migrations, not a claim about a live DB. No DB inspection was performed. |
| [Registration:29–97][registration] | Adapter uses an injected `Clock`, forced to `Asia/Ho_Chi_Minh`; samples `LocalDateTime.now(clock)` once and derives registration date/time from it. Inserts `CHO_KHAM`, supplied priority and `%02d` display. | **Correction to prior evidence:** the [earlier persistence proposal §2][prior] describes JVM-default registration clocks and older line anchors. That finding is stale in this checkout. Current registration still stores only `ngayKham`; it does not implement transfer-local `queueDate` persistence. |
| [Allocator:197–207][allocator] | `MAX(soThuTu)+1` with `UPDLOCK,HOLDLOCK`, filtered by room, `ngayKham`, `isDeleted=0`; no status/priority partition. | This is locked MAX allocation, not a SQL `SEQUENCE` or durable high-water mark. Moving the maximum visit away, or soft-deleting it, can lower the next MAX. Cancellation alone does not release a number while its nondeleted row remains. |
| [Queue and room-summary SQL:162–194][reads]; [doctor date:40–43][doctor] | Both queue readers and room counts use `ngayKham`. Reads stay within exact facility/department. Queue ordering is `dnVnTien DESC, soThuTu, id`; doctor includes only the three active statuses. Doctor's default date uses the clinic zone. | A transfer with `queueDate != ngayKham` would be omitted from the intended destination day if these predicates remained unchanged. Queue membership and clinical-date queries must be distinguished in a later implementation. Per-room numbers need not be globally unique in a department list. |
| [Visit port:6–9][port], [domain fields/lifecycle:29–123][domain] | Domain port declares next number by facility/department/date without room; adapter actually allocates by room/date. Domain has current location, clinical state and public setters; no queue date in the inspected fields. | The port signature is not evidence of department-wide numbering, and generic setters/version fields do not implement safe transfer. Do not reuse registration to create a replacement visit. |
| [Handler:71–82,129–146][handler], [composition:24–32][composition], [connection provider:14–35][provider] | Registration and its success audit run in a boundary; composition shares manager/provider/UoW. Provider borrows that manager's thread-local transaction connection, otherwise opens an independent lease. | Atomicity requires the same manager/provider and synchronous transaction thread. A separate connection, manager or background task does not join automatically. |
| [Transaction manager:26–73][tx] | Disables autocommit, joins nested calls, marks nested failures rollback-only, rolls back failure/null results and operation exceptions, calls UoW accounting then commits. No explicit isolation selection, lock-timeout policy or retry loop. | Do not assume serializable isolation, guaranteed row-only locks, successful rollback after every error, or automatic deadlock recovery. A nested success is provisional until outermost commit. |
| [CAS precedent:105–134][cas] | Patient-history update checks expected version plus exact ownership/scope, increments version, and succeeds only on one affected row. | Useful conditional-write pattern; it is not a transfer adapter or permission precedent. Visit CAS and destination-number serialization solve different races. |
| [Current audit writer:18–27,42–51,140–164][audit], [audit DTO:6–24][audit-dto] | Mutation audit requires an active transaction; writer encodes the supplied instant as UTC. DTO has no structured old/new room/date/number fields; legacy free-text detail is written null. | Existing audit-only storage cannot act as an allocation ledger without separately reviewed changes. This proposal does not change audit behavior. |

The [prior queue/history persistence proposal §§3–8][prior] recommends counter + allocation ledger, shared writers, CAS, UTC/local separation and rollback; this proposal narrows those ideas into independently decidable queue choices and resolves no history/replay policy. The [domain-invariant design][invariants] supplies useful CAS/atomicity analysis, but its illustrative SQL setting `trangThai` and earlier unresolved-date discussion cannot override RECON-04 preservation or its separate `queueDate` decision. The [Phase 0 repository review][phase0] reinforces that interfaces/models alone do not establish an implemented workflow.

**Synthetic failure examples (inferences):** If room A/day D has numbers 1–8 and visit 8 moves to B, current MAX in A can become 7 and issue 8 again. If two independent autocommit callers read the same MAX before either INSERT, locking the SELECT alone does not cover the subsequent INSERT. The [existing standalone fake-JDBC test:147–172][standalone] confirms this adapter permits a standalone lease without disabling autocommit; it does not reproduce SQL Server contention. A new transfer-only counter alongside unchanged registration MAX cannot prevent these mixed-writer failures.

## 3. Persistence shapes for the independent date

All options below require separate implementation authorization. Names describe responsibilities, not approved DDL. A queue allocation ledger is about issued tickets; it need not decide whether dedicated immutable **transfer history**, audit-only transfer history, or both is selected.

| Option | Shape and benefits | Consequences / integrity and access costs |
|---|---|---|
| P1 — Current date inline on visit | Add `queueDate DATE` beside current room/number/display; initial registration uses its registration business date, transfer replaces only queue assignment/date. Simple date filtering and indexing; clinical date remains intact. | Alone, this overwrites previous assignments and cannot reserve departed numbers. Needs a durable allocator/reservation store for a no-reuse policy, plus a link to the current allocation if that store is chosen. Date, room, number and display must change atomically. |
| P2 — One current queue-assignment side table | Unique visit reference holds current room, `queueDate`, number/display. Separates clinical and queue concerns and can avoid adding date to clinical rows. | Reader joins and legacy mapping; current visit location/number fields would be projections requiring atomic synchronization or a separately approved removal. Replacing the side row still loses issued-number history unless reservations are retained. Missing/duplicate assignment must be an explicit error, not an empty queue. |
| P3 — Retained allocations with current pointer | Immutable allocation rows contain room/date/number; visit points to exactly one current allocation. `queueDate` is read through that pointer; past allocations retain reservations. | Clear current-versus-past identity, but adds joins, pointer ownership constraints and projection consistency with existing visit fields. Never find current assignment by `MAX(time)`; ties, retries and later moves make that ambiguous. |
| P4 — Derive date from transfer timestamp or audit on every read | A structured durable transfer record could supply the latest effective instant/date without an inline date. | Requires authoritative current-operation identity, initial-registration fallback and indexing; timezone interpretation becomes part of every reader. Existing audit DTO/writer lacks necessary facts. Not deployable using current audit unchanged; fragile as the primary queue read model. |

**Proposal — recommend P1 plus retained allocation identity and shared counter/ledger.** Store current `queueDate` directly on `LuotKham` for reads; store an allocation reference linking that visit to the exact retained room/date/number assignment. An equivalent constrained relationship may be chosen during later schema review. The ledger tuple is authoritative issuance evidence; the current visit tuple is a projection updated in the same transaction, with reviewed database enforcement that the allocation belongs to that visit and matches room/date/number/display. A bare FK to an unrelated allocation ID is insufficient. This deliberately duplicates current data for read simplicity and adds synchronization cost; P3 is the feasible alternative if Owner prefers one canonical date storage location and accepts mandatory joins.

Provisional records for the recommended bundle:

| Record | Required responsibility, subject to later schema review |
|---|---|
| Current visit assignment | Separate local `DATE`, destination room, number/display and allocation identity; expected visit version protects the complete mutation. Preserve all clinical fields and priority. |
| Room/day counter | Unique `(roomId, queueDate)`, nonnegative high-water mark; mutable coordination state. No status, priority or soft-delete filter in its identity. |
| Retained allocation | Allocation identity, visit reference, validated facility/department/room snapshots, `queueDate`, positive number, display snapshot, registration/transfer operation linkage and UTC issuance instant. Unique `(roomId, queueDate, number)` over all retained committed issuances. Retain when moved, cancelled, completed or soft-deleted. No clinical narrative or patient identifying text. |

Queue, doctor-queue and room-count reads would use **current `queueDate`** with their existing exact scope, patient deletion, status and ordering predicates; clinical visit-history date remains `ngayKham`. Updating only transfer writes is insufficient. A later registration path must populate queue date and participate in the same allocator. No fallback to `COALESCE(queueDate,ngayKham)` should silently classify unknown transferred legacy rows; a reviewed legacy baseline is required. History/audit readers receive no new grants; allocation internals are not a destination patient read API.

## 4. Sequence namespace and allocation mechanism

| Namespace option | Operational / integrity consequences | Assessment for Owner |
|---|---|---|
| N1 — `(destinationRoomId, queueDate)` shared by all issuers | Matches current room-based behavior; independent counters per room/day. Identical displayed digits across rooms/days are valid, so identify tickets by room + date + number. Room's current hierarchy is validated separately. | **Recommend.** Room IDs are global PKs; adding mutable department/facility membership to the key could restart a sequence when a room is reparented. Keep those as validated snapshots instead. |
| N2 — `(facilityId, departmentId, queueDate)` | One stream across rooms makes a department list unambiguous by number, but changes current numbering and creates a larger contention point. A transfer between rooms in one department still consumes a fresh number. | Feasible, requires explicit operational choice and changes to every issuer/display assumption. Domain port signature alone does not approve it. |
| N3 — Facility/day or global/day | Simpler broad uniqueness, substantially larger contention domain, less room-local numbering; no additional read permissions should accompany it. | Feasible but weak fit to existing room allocator; broad identity is not authority to read broader queues. |
| N4 — Partition additionally by priority/status/issuer | Smaller streams but duplicate visible numbers inside one room/day and unstable identity on status/priority change; separate registration/transfer streams can collide. | Do not recommend. Priority affects ordering only; preserve its existing value and one shared room/day stream. |

| Mechanism option | Consequences and evidence limits |
|---|---|
| M1 — Continue MAX over current visits | Smallest apparent change, but moving/deleting rows releases apparent capacity. Even adding a current-row unique index cannot preserve numbers after rows leave. Does not support the recommended committed no-reuse policy. |
| M2 — MAX over retained allocation ledger | Feasible no-reuse design if all issuers retain rows and serialize the indexed room/day range within the transaction, including empty ranges; unique key is a final collision defense. Increasing ledger volume and lock plans need measurement. |
| M3 — Transactional room/day counter plus ledger | **Recommend.** Counter avoids repeatedly scanning issued allocations; ledger provides ownership/reservation evidence and unique tuple. Counter increment, ledger insert and visit mutation must commit or roll back together. Costs an extra coordination row and a ledger write. |
| M4 — Dedicated database sequence/reserved blocks | Candidate only if Owner accepts a separately specified gap/reservation lifecycle, room/day reset strategy and operational management. No native sequence exists in inspected source. Engine rollback/cache behavior and exhaustion must be verified in the later implementation; do not assume transaction rollback restores issued values. |

For M3, propose a unique counter key, protected lookup/creation for a missing room/day (the existing `UPDLOCK,HOLDLOCK` pattern is a starting point), and atomic increment returning the allocated value. A unique-insert conflict, deadlock or lock timeout aborts the whole attempt; do not continue a partly failed transaction. SQL Server plans, absent-key range protection and lock escalation require isolated synthetic integration evidence. Java synchronization would not coordinate multiple desktop processes. Unique ledger enforcement does not protect direct visit writers that bypass the ledger; every issuer must use the protocol and current-assignment linkage must be enforced in the later schema review.

## 5. Reuse and gaps are separate choices

| Policy | Consequences |
|---|---|
| R1 — Reuse a vacated/cancelled/deleted committed number | Shorter visible queues, but an old ticket can identify a later allocation; returned visits and historical reconciliation become ambiguous. Requires explicit release state and proof no conflicting reference survives. Current MAX has some accidental reuse, not an approved release protocol. |
| R2 — Never reuse a committed room/day number; allow holes | **Recommend.** Retain allocations; do not decrement counters on move, completion, cancellation or soft-delete. A return A→B→A consumes a fresh A allocation. Highest committed number may exceed currently waiting count. Different room/day is a different namespace. |
| R3 — Never reuse even an aborted reservation | Useful if numbers are exposed before commit, but implies durable reservations/burns outside the failed transfer and an explicit failure-record lifecycle. Adds recovery complexity and intentionally leaves gaps. Not supplied by a counter rolled back with the visit. |
| R4 — Gap-free displayed committed queue | Requires separate display renumbering or strong serialization/release rules and can invalidate already issued tickets. Do not recommend; not implied by “new destination number.” |

For recommended R2/M3, a number in a **confirmed rolled-back transaction was never issued** and may be allocated later; no receipt/print/event may expose it before outermost commit. Allow visible gaps from prior committed allocations moving away, being cancelled/deleted, or remaining outside active-status filters. Do not promise a gap-free queue. On an uncertain commit, do not recycle or allocate a replacement speculatively; resolve the operation outcome under the separately approved recovery/replay contract. A correction after known commit is another authorized transfer, never deletion of the issuance or counter decrement.

Preserve minimum two-digit display compatibility (`1 → 01`, `100 → 100`); do not wrap at 99, renumber other visits or modify priority to resolve overflow. Since current storage is `INT`, exhaustion must fail atomically pending a separately reviewed capacity decision. [DEC-IDENTIFIER-01][time] cautions against identifier reuse but does not itself classify queue numbers; R2 remains a recommendation requiring Owner choice.

## 6. queueDate capture, midnight and retry sampling

**Approved:** use a transfer-local date in `Asia/Ho_Chi_Minh`, UTC metadata, and preserve clinical dates/timestamps. **Open:** which instant in a transaction defines the transfer for queue purposes. Derive local date from one explicit instant; do not sample date and time separately or trust desktop/JVM/SQL default zones.

| Capture rule | Midnight / contention consequence |
|---|---|
| T1 — Sample on request receipt or transaction entry | Stable throughout processing, but authentication/eligibility waits can assign yesterday's queue even before allocation starts. Trusting client date would additionally permit backdating; not recommended. |
| T2 — Sample after protected eligibility/version/room validation, immediately before destination allocation lock | **Recommend.** Minimizes pre-allocation age and yields one date for the whole attempt. Waiting for that room/day lock may still cross midnight; retain the captured date. No claim that it is the commit date. |
| T3 — Sample after waiting for allocation access | Closer to actual issuance, but the date selects the very key being locked. Requires a provisional key then resample/release/retry protocol; if the day changes, abort/restart the whole attempt rather than switch counters inside it. More contention/retry complexity. |
| T4 — Require date equal to pre-commit date | Final check can detect a boundary crossing and abort/retry; adds failures near midnight. It cannot guarantee the eventual commit timestamp is on that date without a separately proven commit-time design. |

**Proposal — exact recommended rule:** on the active JDBC transaction connection, obtain one UTC instant from the database UTC clock after eligibility/version/target protection and before acquiring the destination room/day counter. Infrastructure converts it using Java `ZoneId.of("Asia/Ho_Chi_Minh")`; persist the derived `DATE` and that UTC operation/issuance instant. Use the same date in counter key, allocation, visit assignment and any required transfer facts/receipt. New technical metadata is explicitly UTC; an effective instant is not an exact commit timestamp. Precision and binding must be specified and tested in the later adapter contract. Choosing DB authority is proposed; current registration's injected clinic clock does not establish a system-wide clock authority.

Synthetic boundary examples under T2 (no patient data):

| Timeline | Proposed result |
|---|---|
| Sample `2026-11-06T16:59:59.999Z` = Nov 6, 23:59:59.999 clinic time; lock obtained/commit after `17:00Z` | `queueDate=2026-11-06` throughout this attempt, even though commit is on local Nov 7. Preserve original `ngayKham` and examination timestamps. |
| Transaction begins before midnight, validations finish and sample is `2026-11-06T17:00:00Z` | `queueDate=2026-11-07`, independent of transaction entry date. |
| Rollback is confirmed; a new full attempt samples after `17:00Z` | Recommend fresh sample, hence Nov 7. Revalidate scope, room, linked work and the original caller's expected version; never replace that version automatically. Retry-time fresh sampling needs explicit Owner acceptance. |
| Prior operation committed on Nov 6; response lost; recovery occurs Nov 7 | The committed allocation retains Nov 6. Do not create a Nov 7 number merely because recovery occurs later. Whether/how to return its receipt or reject replay is the separate open replay decision. |

Stored `queueDate` is authoritative thereafter; do not recalculate it using today's date. An aborted, uncommitted attempt has no authoritative persisted date; fixing a date across such retries instead is feasible but needs durable operation identity/date reservation and an explicit expiry/backdating policy. No replay outcome, key lifetime or different-payload behavior is approved by this proposal. Client receipt/printing occurs only after known outermost commit. A transaction delayed across midnight can appear on the prior day's queue under T2; Owner must explicitly accept that operational consequence or select restart-on-boundary behavior.

## 7. Transaction/concurrency choices and recommended sequence

| Choice | Consequences |
|---|---|
| Optimistic visit CAS + destination serialization | **Recommend.** Persisted guards plus supplied expected version protect the visit; counter key/range serialization and uniqueness protect different visits' allocations. Early validation avoids wasted work; CAS remains mandatory even if the visit was locked. |
| Protect visit/hierarchy/eligibility with targeted locks in the same transaction | Compatible with recommended CAS. Closes deactivation/reparenting and linked-work races if all competing writers follow the reviewed protocol; longer locks and deadlocks are possible. A plain SELECT or late check alone is not proof of stability until commit. |
| Serializable boundary for all transfer reads | Feasible broader protection; increased contention/range-lock cost. Still needs correct predicates/indexes, expected-version CAS and constraints. Current transaction manager does not select this isolation. |
| Snapshot/read-committed reads then version check only | Can detect visit-version conflict, but independent hierarchy/linked-work changes may not bump visit version. Needs additional protection/conditional protocols; insufficient on its own to prove the approved safety rules. |
| Allocate independently then update visit | Simplifies caller structure but can leave committed reservation without transfer and requires compensation/burn semantics. Do not recommend for atomic R2/M3. |

Proposed sequence for a future authorized implementation, following the [existing transaction manager][tx], [CAS][cas] and [atomic success audit][audit] patterns:

1. Validate trusted session/role/source scope and command, including caller `expectedVersion` and required fixed reason code. Resolve any operation recovery according to its separately approved contract before fresh allocation; no destination patient/queue fetch.
2. Inside one outer boundary and the shared manager/provider/thread, load and protect persisted visit, active destination hierarchy and complete linked-work safety facts. Compare original expected version; fail closed on missing/unknown data. Agree a deterministic resource order across transfer, registration, lifecycle and hierarchy writers; row locks alone do not define that order.
3. Capture T2 time once; serialize/create the destination counter key, increment once and insert retained allocation. First-use races must be covered by indexed key/range protection and uniqueness. Do not acquire a source counter to release a number under R2.
4. CAS current visit by ID, original version, expected source facility/department and null-safe source room, nondeleted/unlocked state and actor-eligible persisted status. Increment version once; update destination department/room and number/display/queue date/allocation linkage plus UTC technical update metadata. Require exactly one affected row. Preserve facility, status, `ngayKham`, reception/examination timestamps, priority, clinical narrative, doctor assignment and linked records. Zero rows aborts; never INSERT a replacement or reload a new version and silently retry the move.
5. Persist all history/operation artifacts required by their eventual choices and the mandatory success audit on this same connection. Any failure rolls back counter initialization/increment, allocation, current visit fields/version and all other transaction writes. No independent success audit or best-effort required artifact.
6. Return/expose the result only after outermost commit succeeds. Nested boundary return and UoW `saveChanges()` are not commits. On confirmed abort, a fresh whole-attempt retry can use the same original expected version only if still valid; deadlock/timeout retries require bounded later-defined policy. Stale CAS is a conflict, not an automatic rebase.

Two transfers of the same version must yield at most one committed move; the loser leaves no committed destination reservation. Different visits targeting the same room/day must receive distinct numbers, including first use of an empty day and registration versus transfer. Hierarchy and linked-work protection must remain valid until commit; the queue design cannot repair absent eligibility readers. Same-visit and same-counter races require independent evidence.

The manager catches commit errors generically and does not prove rollback succeeded after every error ([lines 47–68][tx]). Lost commit acknowledgement/rollback failure is **indeterminate**, not evidence that nothing was saved. Preserve the possibly committed allocation and reconcile before another allocation; no speculative compensating move. Exact recovery replies remain in the separate replay proposal. These are future requirements, not claims that current transfer concurrency or recovery works.

## 8. Cutover consequences and future verification

No migrations or tests are added here: the explicit documentation-only contract overrides the generic checklist's implementation/test step. Future authorized work would need a new reviewed migration and explicit registration in the custom [V1–V11 runner][migrations]; changing existing scripts or merely dropping in a file is insufficient. The runner [commits each migration separately][migration-tx], so do not imply all-or-nothing deployment rollback.

For the recommended design, reconcile synthetic upgrade fixtures for duplicate/null/nonpositive numbers, unknown queue dates, missing rooms and contradictory hierarchy. Under a writer stop or equivalent reviewed serialization barrier, seed only provable current dates/reservations and high-water marks, including known cancelled/deleted allocations. Preserve unknown provenance; previous moves or purges cannot be reconstructed from current MAX. Owner must accept a documented cutover baseline or require reconciliation before release. No automatic clinical-date rewrite, deletion, renumbering or fabricated historical allocation. This task performed no data profiling or DB access.

Deploy schema capability, queue-date readers and **all** allocation writers together; reject mixed old MAX/new counter operation and missing required schema. Retire bypass writers before enabling. A later deployment rollback should disable transfer and preserve committed reservations, not revert to MAX after numbers have been issued. No permission/audit policy/gate changes are proposed here.

Future tests use synthetic fixtures only and remain **not run / not passing evidence** in this task:

| Scenario | Required observation |
|---|---|
| Registration + transfer, two transfers, missing first counter row | Distinct room/day allocations across processes/connections; actual SQL Server locking/constraints exercised. |
| Same visit/version to different destinations; concurrent lifecycle/EMR lock | One winner at most; losing attempt leaves visit/counter/ledger/history/success audit unchanged. |
| Move highest ticket, delete/cancel/complete, return to source | No reuse of committed allocations; preserve priority for both true and false; gaps accepted. |
| Failure after counter, ledger, CAS, required history or audit; nested rollback-only | Complete rollback, including version/metadata and new counter creation; no pre-commit receipt or print. |
| Clock before/at/after local midnight, lock wait crossing it, new attempt after confirmed rollback | Exact T2 date as above; JVM timezone independence; all stored/read assignment dates agree. |
| `queueDate != ngayKham`, restart/readback, department/room summaries | Correct current-day queue membership with original clinical date/status/timestamps retained; no destination data disclosure. |
| Target deactivation/reparenting, linked-work change, missing readers | Guard remains valid until commit or whole attempt denied; no linked/billing mutation. |
| Unknown commit, exhausted `INT`, duplicate baseline, mixed writer versions | Safe failure/reconciliation without speculative reuse, wrapping or silent repair. |

## 9. Exact choices requested from Owner — none recorded as approved

These choices subdivide the existing open queue-allocation group; they do not reopen fixed policies. Owner may accept/reject each recommendation explicitly; this document records **no answer**.

| ID | Exact decision | Recommendation for consideration |
|---|---|---|
| QA-01 | Where is current `queueDate` persisted: P1 inline visit, P2 side table, P3 allocation pointer only, or P4 structured-history derivation? If duplicate projections remain, require ownership/tuple consistency enforcement? | P1 inline `DATE` plus current allocation linkage, with enforced visit/allocation tuple consistency; retain `ngayKham` exclusively as clinical date. |
| QA-02 | Which namespace: N1 room/day, N2 department/day, N3 broader/day, or another explicitly defined namespace? Is it shared across registration, transfer and every future issuer, without status/priority partitions? | N1 `(roomId,queueDate)` across all issuers; hierarchy is validated metadata, not a counter reset key. |
| QA-03 | Which coordination/reservation mechanism: M2 retained-ledger MAX, M3 counter+ledger, or separately specified M4 sequence/reservations? | M3 atomic counter+ledger, unique retained room/date/number and protected first-key creation. |
| QA-04 | May any **committed** allocation be reused after move/cancellation/completion/deletion? Are gaps acceptable? Can an unexposed, confirmed-aborted number be used later, or must even aborted reservations burn? | R2: committed numbers never reused within the same room/day; gaps accepted; confirmed-aborted unexposed numbers may be allocated later. No pre-commit exposure. |
| QA-05 | Which clock authority and capture point T1–T4 define `queueDate`? Must a transaction crossing midnight keep its sampled date or abort/restart? | DB UTC sample on transaction connection at T2; derive clinic date once and keep it even through lock wait/commit after midnight. Explicitly accept possible prior-day placement after midnight. |
| QA-06 | After confirmed rollback, does a fresh attempt sample a new date or retain the original attempt date? How must this align with the separate replay/recovery contract? | Fresh T2 sample after full revalidation; committed allocation date never changes on recovery. Replay answers/key policy remain undecided elsewhere. |
| QA-07 | Which consistency strategy protects hierarchy/linked-work until commit: targeted reviewed locking plus CAS, broad serializable boundary plus CAS, or another demonstrated equivalent? | One connection/transaction, original-version CAS, targeted protection and deterministic cross-writer order; bounded timeout/deadlock policy specified in later implementation review. |
| QA-08 | For legacy allocations with incomplete issuance history, accept an explicit known-data cutover baseline with limitations, or require further reconciliation before rollout? Require all writers/readers to switch together? | Require documented baseline acceptance and all-writer/all-reader coordinated cutover; unresolved duplicates/ownership/date provenance block enablement. No invented history or automatic renumbering. |

Reason-code detail, dedicated transfer history versus audit-only/both, and replay outcomes (including changed payload) remain independent Owner decisions ([current open groups][gaps]). Even accepting QA-01–08 would not authorize implementation, DDL, grants or runtime enablement; a later scoped contract and security/acceptance gates remain necessary.

## 10. Guidance, retrieval and validation record

Read before editing: task-supplied `docs/PROJECT_CONVENTIONS.md` baseline; `README.md`, supplied `DOMAIN_ANALYSIS.md` / `ERD_DIAGRAM.md` excerpts and relevant local architecture/visit sections; supplied retrieved excerpts from `phase3-transfer-linked-work-eligibility-review.md`, `phase3-transfer-queue-history-persistence-proposal.md`, `phase3-transfer-domain-invariant-design.md`, `phase3-visit-transfer-contract-review.md`, `phase3-visit-transfer-decision-gaps.md`, and `phase1-doctor-history-ui-independent-review.md` (all under `docs/ke-hoach-trien-khai/`). Also inspected relevant cited sections of those local reviews, `UI_CODING_STANDARDS.md`, `docs/ke-hoach-trien-khai/README.md`, `phase0-repository-contract-review.md`, `phase3-visit-lifecycle-owner-decisions.md`, `phase2-role-permission-matrix-approval.md`, and `phase2-implementation-contract.md`. Read the task-supplied approved contracts `VIS-TRANSFER-OPEN-DECISION-PROPOSALS-01` and `VIS-TRANSFER-OWNER-DECISION-RECON-04`. Prior validation snippets were not treated as current test results.

No applicable `AGENTS.md` was found in checkout inventory, target ancestors or filesystem ancestors checked. Repository-local `docs/PROJECT_CONVENTIONS.md`, `docs/DEVELOPMENT_CONVENTIONS.md`, `CONTRIBUTING.md`, `docs/README.md` and `ARCHITECTURE.md` were absent; the supplied baseline was used. Only relevant cited document sections were opened; source was inspected read-only.

**RAG first:** before opening docs or using `rg`, ran the script's `--help` successfully, then the required search:

```text
/Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py --repo clinic-desktop --limit 5 'điều chuyển hàng đợi queueDate ngayKham cấp số phòng khám sequence transaction midnight optimistic version rollback VIS-TRANSFER-OWNER-DECISION-RECON-04'
```

Search exited **1**: `sqlite3.OperationalError: unable to open database file`, in Orchestrator index connection setup (`PRAGMA journal_mode=WAL`). No new search excerpts were returned. Used the task-supplied current RAG excerpts as primary guidance, followed only needed citations, and verified current source where prior evidence was stale. No permission escalation, index write workaround or cache manipulation. No clinic DB connection or patient data access; RAG index setup failed before retrieval.

Validation for this deliverable: `git diff --check` exited 0 with no output. `git diff --no-index --check /dev/null docs/ke-hoach-trien-khai/phase3-transfer-queue-allocation-proposal.md` emitted no whitespace diagnostics and exited 1 (the no-index comparison reports the new-file difference); the new file was also checked directly for trailing whitespace. `git status --short` showed only this allowed new file. A read-only Python citation check passed for 26 targets / 34 references, including file existence and line bounds. These are document checks, not proof of SQL concurrency. No Java build, application tests or SQL execution: behavior/schema/tests are out of scope. Future synthetic test cases above are proposals only.

| Acceptance criterion | Result and evidence |
|---|---|
| Only allowlisted new Markdown changed | PASS — clean starting worktree; final changed-path check names only this file. |
| Compare persistence, namespace, reuse/gaps, date capture and transactions | PASS — option/consequence tables in §§3–7, midnight examples and rollback analysis. |
| Preserve approved split, status/timestamps and priority | PASS — fixed boundary in §1 and conditional-write proposal in §7; no source/schema change. |
| Current source/prior evidence, labels and exact Owner choices | PASS — §2 reconciles stale prior clock evidence; fact/inference/proposal labels; QA-01–08 remain unanswered in §9. |
| Diff check and guidance report | PASS — checks above and guidance list in this section. Required fresh RAG retrieval was attempted first but BLOCKED by the local index error; supplied excerpts were used and the limitation disclosed. |

[contract]: phase3-visit-transfer-contract-review.md#L7-L19
[gaps]: phase3-visit-transfer-decision-gaps.md#L77-L90
[scope]: phase2-role-permission-matrix-approval.md#L9-L18
[time]: phase2-implementation-contract.md#L8-L11
[linked]: phase3-transfer-linked-work-eligibility-review.md#L5-L17
[prior]: phase3-transfer-queue-history-persistence-proposal.md#L27-L180
[invariants]: phase3-transfer-domain-invariant-design.md#L152-L179
[phase0]: phase0-repository-contract-review.md#L3-L9
[schema]: ../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L329-L380
[index]: ../../clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql#L1142
[migrations]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunner.java#L44-L56
[migration-tx]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunner.java#L187-L200
[registration]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L29-L97
[allocator]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L197-L207
[reads]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L162-L194
[doctor]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/GetDoctorQueueHandler.java#L40-L43
[port]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/repositories/ILuotKhamRepository.java#L6-L9
[domain]: ../../clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java#L29-L123
[handler]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java#L71-L146
[composition]: ../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionCompositionRoot.java#L24-L32
[provider]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcConnectionProvider.java#L14-L35
[tx]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcTransactionManager.java#L26-L73
[cas]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/patient/TienSuBenhRepositoryImpl.java#L105-L134
[audit]: ../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/system/JdbcAuditRepositoryImpl.java#L18-L164
[audit-dto]: ../../clinic-application/src/main/java/vn/clinicmanagement/application/audit/SystemAuditEntry.java#L6-L24
[standalone]: ../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImplTest.java#L147-L172
