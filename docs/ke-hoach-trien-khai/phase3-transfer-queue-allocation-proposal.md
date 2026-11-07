# Phase 3 — Transfer queue allocation and queueDate persistence proposal

**Status: OWNER-APPROVED DESIGN BASIS (P1 + M3) — DDL / MIGRATION UNAUTHORIZED.** Snapshot: 2026-11-06. Scope: `VIS-TRANSFER-OPEN-DECISION-PROPOSALS-01`, updated under approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-06` (contract id `13d19fa9-eaed-404e-bdfc-4070daffd38a`), carrying forward `VIS-TRANSFER-OWNER-DECISION-RECON-04` and `VIS-TRANSFER-OWNER-DECISION-RECON-05` unchanged. P1 and M3 are approved as architecture design basis only; physical DDL, schema, migration scripts, runtime enablement, and permission grants remain strictly unauthorized. SEC-01 remains OPEN and transfer remains default-deny. Only allowlisted proposal Markdown files are changed.

## RECON-06 supplement — 2026-11-06 (current approved contract: P1 + M3 design basis)

**Authority:** Owner-confirmed `VIS-TRANSFER-OWNER-DECISION-RECON-06` (contract id `13d19fa9-eaed-404e-bdfc-4070daffd38a`), received 2026-11-06, following RAG-grounded proposal review. This decision supplement controls as the latest authority, carrying forward `VIS-TRANSFER-OWNER-DECISION-RECON-04` and `VIS-TRANSFER-OWNER-DECISION-RECON-05` unchanged. It records only the choices explicitly approved below. It does not authorize DDL, SQL migrations, runtime transfer, permission grants, queue reads, push, PR, or merge. SEC-01 remains OPEN and transfer remains default-deny.

### 1. Approved queue persistence design basis: P1 + M3

Owner approved only the following **P1** and **M3** concepts as the architecture design basis. Earlier wording that included physical SQL details within the approval bullets is superseded by this clarification; the technical analysis is retained below as illustrative proposals requiring design review.

1. **P1 approved as persistence design basis:** Current `queueDate` and a pointer to the current allocation on `LuotKham`, with an immutable allocation ledger. No physical pointer name, type, nullability, FK/check or index is selected.
   - **Proposal rationale, not additional approval:** Inline current queue data could support frequent desktop UI reads ([`ReceptionQueueRepositoryImpl.getQueueList`][reads] and [`GetDoctorQueueHandler`][doctor]) without requiring mandatory multi-table joins on every refresh.
2. **M3 approved as allocator design basis:** A transactional per `(roomId, queueDate)` high-water counter plus immutable `QueueAllocation` ledger. The tuple describes the logical sequence namespace, not approved SQL key syntax.
   - **Binding policy:** Allocations must be unique within the shared room/day sequence and committed numbers must never be reused. Exact database enforcement remains open. Counter field meanings, initialization/increment/rollback semantics, locking and SQL statements require further design review.
3. **Settled RECON-05 policies implemented without modification:**
   - **Shared room/day sequence:** Registration and transfer share exactly one number sequence per `(roomId, queueDate)`. No partitioning by priority, status, or role.
   - **No reuse of committed numbers & visible gaps allowed:** Committed sequence numbers are **never reused** within a room/day, even if a visit is subsequently transferred out, cancelled, completed, or soft-deleted. A visit returning A → B → A consumes a fresh allocation in A. Visible gaps in the queue sequence are explicitly permitted. Numbers in confirmed rolled-back transactions were never committed and may be allocated by subsequent attempts; no uncommitted number may be exposed via receipt, printing, or event.
   - **Single DB UTC clock authority:** Exactly one SQL Server DB UTC instant captured on the transaction connection at transaction start (before acquiring destination queue locks) determines the `Asia/Ho_Chi_Minh` queue date throughout the attempt, even if lock wait crosses clinic-local midnight.
   - **Atomic mutation, allocation, history, and audit:** Transfer visit projection update, destination queue allocation, immutable transfer history (`LichSuDieuChuyenLuotKham`), and security audit (`NhatKyHeThong`, action `VISIT_TRANSFERRED`) must commit atomically or roll back atomically in the same database transaction.
4. **Preserved high-water cutover safeguard (fail closed on unknown history):**
   - Migration design must establish the committed high-water mark from all trustworthy persisted allocation evidence, including soft-deleted rows/assignments and any retained historical assignment record.
   - If hard deletion, purging, overwritten assignments, or missing historical commits make the high-water unprovable, **cutover and both issuers must stop for the affected room/day** until a verifiable reconciliation source or a separately approved safe boundary exists. Legacy unknowns fail closed; do not infer, estimate, or fabricate history.

### 2. Explicit distinction: architecture design basis vs. still-open implementation decisions

This approval establishes **only an architecture design direction** for further design. It does **NOT** authorize DDL, migrations, or runtime execution. The following technical implementation areas remain **explicitly OPEN and unapproved**:

- **Physical DDL and schema:** Column data types, nullability, table definitions, naming, and foreign key definitions.
- **Indexes and constraints:** Exact index compositions, filtered index predicates, unique key syntax, FKs and checks. Required room/day uniqueness and committed-number no-reuse remain binding policy; their exact database enforcement is open.
- **Counter semantics, initialization and locking:** Exact stored high-water representation, initial value, increment/return/rollback behavior, precise locking hints, deadlock retry policies, transaction isolation configurations, and lock timeout parameters, subject to the binding no-reuse and atomicity policies.
- **High-water reconciliation procedure:** Detailed SQL scripts or data verification tooling for cutover auditing.
- **Migration version and scripts:** Migration version number (e.g. `V12`), runner registration in `DatabaseMigrationRunner`, and script contents.
- **Cutover window and writer coordination:** Operational execution plan, write barrier timing, and simultaneous deployment of registration and transfer adapters.
- **Rollback and error recovery:** Precise transaction rollback handling and client error contracts.
- **Legacy-row semantics:** Handling of pre-cutover visits with `NULL` queue dates upon first transfer.

### 3. Status of architecture alternatives

Earlier architecture alternatives are formally categorized as follows (preserving historical analysis snapshots):

| Category | Option | Status under RECON-06 | Description / Decision |
|---|---|---|---|
| **Persistence Shape** | **P1** (Inline visit `queueDate` + allocation pointer) | **OWNER-APPROVED (Concepts Only)** | Current `queueDate` and current-allocation pointer on `LuotKham`, with an immutable allocation ledger. Exact SQL names/types, relationships, indexes and DDL remain illustrative/unapproved. |
| Persistence Shape | **P2** (One current queue-assignment side table) | **NOT SELECTED (Historical Alternative)** | Preserved as historical analysis. Clean separation of concerns, but requires additional join on all queue reads and adds sync overhead. |
| Persistence Shape | **P3** (Retained allocations with pointer only) | **NOT SELECTED (Historical Alternative)** | Preserved as historical analysis. Avoids duplicate date storage, but requires join on every queue query, degrading read performance. |
| Persistence Shape | **P4** (Deriving date from history/audit) | **REJECTED (Historical Alternative)** | Fragile, unindexable, requires scanning history for current state; rejected for desktop UI. |
| **Allocator Mechanism** | **M1** (Current MAX over `LuotKham`) | **REJECTED (Violates Policy)** | Rejected by RECON-05/RECON-06. Moving or deleting visits lowers the maximum and reuses numbers, violating no-reuse invariant. |
| Allocator Mechanism | **M2** (MAX over `QueueAllocation` ledger) | **NOT SELECTED (Historical Alternative)** | Preserved as historical analysis. Feasible no-reuse design, but range locking on ledger index under high concurrency creates lock contention/escalation risk. |
| **Allocator Mechanism** | **M3** (Transactional counter + immutable ledger) | **OWNER-APPROVED (Concepts Only)** | Transactional per room/day high-water counter plus immutable `QueueAllocation` ledger. Exact counter semantics, SQL keys, lock hints, constraints, indexes and DDL remain illustrative/unapproved. |
| Allocator Mechanism | **M4** (SQL Server native SEQUENCE) | **NOT SELECTED (Historical Alternative)** | Preserved as historical analysis. Sequences cannot be dynamically keyed by `(roomId, queueDate)` without dynamic DDL per room/day; rollbacks discard numbers unpredictably. |

## Corrective review — 2026-11-06: legacy high-water mark cutover

This nonbinding correction supersedes the earlier active/nondeleted-only seed recommendation and qualifies the cutover discussion below. RECON-05 remains binding: never reuse a committed number; gaps are allowed; registration and transfer share one `(roomId, queueDate)` stream; derive `queueDate` from one DB UTC instant captured at transaction start using `Asia/Ho_Chi_Minh`.

A maximum over retained rows proves only the largest **observed** assignment unless evidence completeness or a safe upper boundary is verified. Include soft-deleted visits/allocations and any existing historical assignment record; do not assume such history exists or infer missing assignments, dates or timezones. If hard deletion, overwritten assignments or commits without retained evidence may conceal a higher number, **stop cutover and both issuers for that room/day** until a verifiable reconciliation source or an explicit owner-approved safe boundary is established. The proposed seed cannot guarantee no reuse while evidence is incomplete.

Reconciliation from a verifiable retained source, a justified upper boundary, or deferral to a demonstrably unused future room/day are alternatives for Owner review, not approvals. A boundary must identify the room/day, evidence and rationale proving future allocations exceed all prior committed numbers; arbitrary headroom, resetting to zero or accepting an unknown maximum does not meet RECON-05. Missing history remains unknown even after a boundary is approved. DDL, operational cutover and runtime enablement remain unauthorized.

## RECON-05 addendum — 2026-11-06 (carried forward by RECON-06)

**Authority:** Owner-confirmed `VIS-TRANSFER-OWNER-DECISION-RECON-05`, received 2026-11-06, following RAG-grounded proposals. This addendum controls over historical snapshots and prior proposals below, carrying forward `VIS-TRANSFER-OWNER-DECISION-RECON-04` without reopening its approved rules.

### 1. Binding facts and settled policies under RECON-05

The following decisions are approved and binding; they must not be treated as open options:

1. **Queue allocation policy and date separation:** `queueDate` is strictly separate from clinical `ngayKham`. Destination queue allocation derives its date from transfer execution and never mutates clinical `ngayKham`. Visit status, clinical `ngayKham`, reception time, and existing examination timestamps (`thoiGianBatDauKham`, `thoiGianKetThucKham`) remain preserved and intact. Priority (`dnVnTien`) is preserved unchanged without auto-promotion.
2. **Shared room/day sequence namespace:** Exactly one number stream per `(roomId, queueDate)` is shared by patient registration and visit transfer. Registration and transfer allocate from the same sequence. No partition by priority, status, or issuer role is permitted.
3. **No-reuse of committed numbers and gap allowance:** Committed sequence numbers are **never reused** within a room/day, even if a visit is subsequently transferred out, cancelled, completed, or soft-deleted. A visit returning A → B → A consumes a fresh allocation in A. Visible gaps in the queue sequence are explicitly allowed. Numbers in confirmed rolled-back transactions were never committed and may be allocated by subsequent attempts; no uncommitted number may be exposed via receipt, printing, or event.
4. **Transaction clock authority and midnight sampling:** Exactly **one SQL Server DB UTC instant** is captured on the transaction connection at transaction start (before acquiring destination queue locks) and converted to a clinic-local calendar date using `Asia/Ho_Chi_Minh` for `queueDate`. That single derived date governs the entire transaction attempt, even if lock contention or transaction execution crosses clinic-local midnight. Technical/audit metadata remains UTC.
5. **Atomic transfer mutation, allocation, history, and security audit:** A dedicated immutable transfer-history record (`LichSuDieuChuyenLuotKham`) and a successful security audit event (`NhatKyHeThong`, action `VISIT_TRANSFERRED`) are mandatory in the **same database transaction** as the visit mutation and queue allocation. All required writes commit together or roll back together. Audit-only logging is no longer permitted.
6. **Transfer reason taxonomy:** Exactly one of the six approved role-partitioned business reason codes is mandatory (`RECEPTION_TRIAGE_CORRECTION` for RECEPTIONIST only; `PATIENT_SPECIALTY_PREFERENCE` and `CLINIC_LOAD_BALANCING` for RECEPTIONIST or DOCTOR within scope; `CLINICAL_SPECIALTY_REFERRAL`, `DIAGNOSTIC_EQUIPMENT_NEED`, `CLINICIAN_EMERGENCY_HANDOFF` for DOCTOR only). The transfer reason is a business field and must never be placed into auth/authorization `AuditReasonCode` or `NhatKyHeThong.maLyDo`.
7. **Idempotency outcomes:** Same request key + same canonical payload after commit returns the stored operation receipt after reauthorizing the actor, without repeating allocation, mutation, history, or success audit. Same key + different payload returns a conflict without writes. On timeout or lost connection, retry with the same key and determine whether the transaction committed or rolled back before proceeding; timeout alone never proves rollback.

### 2. Concrete recommended persistence, allocator, transaction, and cutover design

The following analysis is retained as an **illustrative technical proposal requiring design review**, not an Owner-approved physical design. In particular, `maCapSoHienTai`, `queueDate DATE NULL`, `BIGINT`, `QueueDayCounter`, `UNIQUE (maPhongKham, queueDate, soThuTu)`, FKs/checks, index compositions, `UPDLOCK, HOLDLOCK`, and exact counter initialization/increment/return/rollback semantics are unapproved examples. The P1/M3 conceptual direction and binding policies above control; none of the SQL or operational steps below authorizes DDL, migration or runtime work.

#### 2.1. P1 conceptual basis — illustrative physical persistence proposal

- **Approval status:** Owner-approved architecture design basis under RECON-06 (`VIS-TRANSFER-OWNER-DECISION-RECON-06`). Physical DDL, schema column data types, constraints, indexes, and migrations remain unapproved and strictly open.
- **Illustrative physical design, not approved:** Store current `queueDate DATE NULL` directly on `LuotKham` alongside current `maPhongKham`, `soThuTu`, and `soThuTuDisplay`, accompanied by `maCapSoHienTai BIGINT NULL` referencing an immutable `QueueAllocation` record.
- **Rationale:** Desktop queue readers ([`ReceptionQueueRepositoryImpl.getQueueList`][reads] and [`GetDoctorQueueHandler`][doctor]) query active room queues with high frequency. Storing `queueDate` inline allows immediate indexing on `(queueDate, maPhongKham, trangThai)` without introducing mandatory multi-table joins on every queue refresh.
- **Proposed integrity enforcement, not approved FKs/checks:** The current visit fields (`maPhongKham`, `queueDate`, `soThuTu`, `soThuTuDisplay`) are a synchronized projection of the authoritative `QueueAllocation` ledger. Both must be updated atomically in the same transaction, protected by database foreign key and transactional checks ensuring the allocation belongs to the visit.
- **Alternatives analyzed:**
  * *P2 (One current queue-assignment side table — NOT SELECTED / Historical alternative):* Cleanly isolates queue fields from clinical `LuotKham`, but requires an additional join on all desktop queue screens and complicates existing queries. Preserved as a historical analysis snapshot.
  * *P3 (Retained allocations with pointer only — NOT SELECTED / Historical alternative):* Stores `queueDate` exclusively in `QueueAllocation` with only an FK on `LuotKham`. Avoids duplicate date storage, but makes every queue reader join `QueueAllocation`, impacting query performance. Preserved as a historical analysis snapshot.
  * *P4 (Deriving date from history/audit — REJECTED / Historical alternative):* Fragile, unindexable, requires scanning history for current state; rejected as unsuitable for high-frequency desktop UI. Preserved as a historical analysis snapshot.

#### 2.2. M3 conceptual basis — illustrative physical allocator proposal

- **Approval status:** Owner-approved architecture design basis under RECON-06 (`VIS-TRANSFER-OWNER-DECISION-RECON-06`). Physical DDL, locking configuration, counter initialization SQL, and migrations remain unapproved and strictly open.
- **Illustrative physical design, not approved:** Propose a mutable coordination table `QueueDayCounter` paired with an immutable ledger table `QueueAllocation`.
  * `QueueDayCounter`: PK `(maPhongKham INT, queueDate DATE)`, `lastNumber INT NOT NULL`, `version BIGINT NOT NULL`, `updatedAt DATETIME2 NOT NULL`. Tracks the durable high-water mark for each room/day stream.
  * `QueueAllocation`: PK `id BIGINT IDENTITY`, `maLuotKham INT NOT NULL`, `maPhongKham INT NOT NULL`, `queueDate DATE NOT NULL`, `soThuTu INT NOT NULL`, `soThuTuDisplay VARCHAR(10) NOT NULL`, `loaiPhanBo VARCHAR(20) NOT NULL` (`TIEP_NHAN` or `DIEU_CHUYEN`), `maThaoTac NVARCHAR(100) NOT NULL`, `thoiGianCapUTC DATETIME2 NOT NULL`.
  * Proposed unique constraint syntax (not approved): `UNIQUE (maPhongKham, queueDate, soThuTu)` across all issued allocations. Uniqueness is required policy; this database enforcement choice still requires review.
- **Illustrative allocation sequence — exact counter semantics and SQL require review:**
  1. On the active transaction connection, query `QueueDayCounter` with `SELECT lastNumber FROM QueueDayCounter WITH (UPDLOCK, HOLDLOCK) WHERE maPhongKham = @targetRoom AND queueDate = @queueDate`.
  2. If missing, require the verified cutover baseline in Section 2.5; absence of a counter is not proof of first issuance. Insert `lastNumber = 0` with range protection only for a provably unused room/day; otherwise initialize from its verified high-water mark/safe boundary or fail closed.
  3. Atomically increment: `UPDATE QueueDayCounter SET lastNumber = lastNumber + 1, updatedAt = @nowUtc OUTPUT INSERTED.lastNumber WHERE maPhongKham = @targetRoom AND queueDate = @queueDate`.
  4. Format display string (minimum 2 digits, e.g. `%02d`, preserving larger numbers like `100` without truncation).
  5. Insert the immutable row into `QueueAllocation`.
- **Alternatives analyzed:**
  * *M1 (Current MAX over LuotKham — REJECTED):* Rejected by RECON-05/RECON-06. Moving or deleting visits lowers the maximum and reuses numbers, violating the binding no-reuse invariant.
  * *M2 (MAX over QueueAllocation ledger — NOT SELECTED / Historical alternative):* Feasible, but scanning/locking the ledger index under high concurrency creates range locks and potential lock escalation. Preserved as a historical analysis snapshot.
  * *M4 (SQL Server native SEQUENCE — NOT SELECTED / Historical alternative):* SQL Server sequences cannot be dynamically keyed by `(roomId, queueDate)` without dynamic DDL per room/day or cyclical resets; sequence rollbacks discard numbers unpredictably. Preserved as a historical analysis snapshot.

#### 2.3. Concurrency, locking, and atomic execution order

The illustrative sequence below proposes a deterministic resource order for review against concurrent bidirectional transfers A → B and B → A and shared registration/transfer allocation. It is not an approved lock hierarchy or a demonstrated deadlock-prevention guarantee; exact hints and cross-writer coordination remain open:

1. **Replay key check:** Check the idempotency request key with `UPDLOCK, HOLDLOCK`. If already committed, return the stored receipt.
2. **Visit CAS and row lock:** Execute conditional CAS update on `LuotKham` matching ID, `expectedVersion`, source facility, source department, null-safe source room, and eligible status. This locks the visit row and increments `version = version + 1`. If 0 rows are affected, abort immediately with a concurrency conflict.
3. **Target hierarchy verification:** Verify destination room, department, and facility are active and non-deleted via join on `PhongKham`, `Khoa`, `CoSo`.
4. **Counter acquisition:** Acquire `UPDLOCK, HOLDLOCK` on `QueueDayCounter` for `(destinationRoomId, queueDate)`.
5. **Allocation insert:** Insert new `QueueAllocation` record.
6. **Visit projection update:** Update `LuotKham` destination room, department, `queueDate`, `soThuTu`, `soThuTuDisplay`, `maCapSoHienTai`, and UTC timestamp.
7. **Transfer history insert:** Insert immutable record into `LichSuDieuChuyenLuotKham`.
8. **Security audit insert:** Insert `NhatKyHeThong` row with action `VISIT_TRANSFERRED`, correlation UUID, and actor context via `appendInCurrentTransaction`.
9. **Outermost commit:** The transaction commits all changes atomically. Only after outermost commit succeeds is the operation receipt exposed to the client.

#### 2.4. Rollback and failure recovery strategy
- **Statement/constraint failure:** If any statement fails (e.g. CAS version mismatch, unique constraint violation on `QueueAllocation`, target hierarchy inactive, or audit failure), `JdbcTransactionManager` issues `connection.rollback()`.
- **Complete undo:** Rollback restores visit fields, version, leaves no row in `QueueAllocation`, `LichSuDieuChuyenLuotKham`, or `NhatKyHeThong`, and rolls back the counter increment in `QueueDayCounter`. The unissued number is never exposed and remains available for subsequent transactions.
- **Indeterminate commit outcome:** If a network disconnect, client crash, or timeout occurs during commit, the client must treat the outcome as indeterminate. The client must retry using the **same request key**. The server checks the key; if committed, it returns the existing receipt; if absent and the previous transaction rolled back, it safely allows execution. Speculative compensating transfers are strictly prohibited.

#### 2.5. Migration, cutover sequence, and legacy data backfill
1. **Runner integration:** Migration must be written as a forward versioned script (e.g. `V12__Queue_Allocation_And_Transfer_History.sql`) and registered in [`DatabaseMigrationRunner.MIGRATION_SCRIPTS`][migrations].
2. **Preflight evidence and conflicts:** Inventory all trustworthy persisted allocation evidence: current and historical visits regardless of status or `isDeleted`, soft-deleted visits/allocations, retained allocation ledgers and any existing historical assignment record. Verify committed assignment identity, number, room and queue-day attribution; do not silently substitute clinical `ngayKham` or guess a legacy timezone. After correlating copies of the same proven assignment, group by `(roomId, queueDate, soThuTu)`: multiple distinct assignments sharing a tuple, conflicting copies, null/nonpositive numbers, unknown dates, orphaned rooms or corrupted versions block the affected room/day. Also reject multiple current assignments for a visit or disagreement between its current room/date/number and a retained current-assignment link. Do not merge, delete or renumber tickets to hide conflicts.
3. **Controlled writer barrier:** Pause all registration, transfer and direct/legacy allocation writers, or use an equivalent reviewed serialization barrier. Repeat preflight against the final stable snapshot before seeding and keep the barrier through synchronized cutover. Check the clinic-local current date from the single captured DB UTC instant; include any room/day that can receive allocations, not only active rooms. A day newly reachable after midnight needs its own verified baseline.
4. **Counter seeding (corrected):** For each `(roomId, queueDate)` stream to be enabled, establish `QueueDayCounter.lastNumber` at least as high as every trustworthy persisted committed assignment from step 2 and any verified retained high-water mark; never lower an existing counter. Use zero only when the absence of any prior committed allocation is verified for that room/day. Soft deletion, cancellation, completion or transfer out never removes evidence from this calculation. If hard-deleted records or commits without retained evidence may leave the committed-number high-water unproved, stop that room/day until a verifiable reconciliation source or owner-approved safe boundary is established as described in the corrective review. An empty result or observed MAX alone is insufficient; do not infer missing history or claim a no-reuse guarantee with incomplete evidence. Preserve legacy provenance without fabricating allocation/history rows.
5. **Synchronized allocator cutover:** Deploy the updated registration adapter ([`ReceptionQueueRepositoryImpl`][registration]) and the transfer handler simultaneously. Both must allocate numbers via the new `QueueDayCounter` + `QueueAllocation` mechanism.
6. **Queue query cutover:** Update queue queries ([`ReceptionQueueRepositoryImpl.getQueueList`][reads] and [`GetDoctorQueueHandler`][doctor]) to query by `queueDate` (with fallback to `ngayKham` only for historical visits predating cutover).
7. **Legacy transfers:** When a legacy visit created before cutover is transferred for the first time, its source allocation link is stored as `NULL` and old queue number is preserved as informational data without fabricating historical allocation records.

### 3. Grounding in actual SQL Server schema and domain constraints

- **SQL Server schema facts:**
  * [`LuotKham` table (V1:329–380)][schema] defines `ngayKham DATE NOT NULL`, `soThuTu INT`, `soThuTuDisplay VARCHAR(10)`, `maPhongKham INT`, `trangThai VARCHAR(20) NOT NULL`, and `version BIGINT NOT NULL DEFAULT 0`.
  * Index [`IX_LuotKham_HangDoi` (V1:1142)][index] indexes `(ngayKham, maPhongKham, trangThai)` with filter `isDeleted = 0`. This index is non-unique and currently uses `ngayKham`.
  * Migration runner [`DatabaseMigrationRunner`][migrations] executes and commits each migration script in an independent transaction ([lines 187–200][migration-tx]). Multi-script transactions or runner rollbacks do not exist.
- **Domain and application constraints:**
  * [`clinic-domain`][domain] adheres strictly to DDD Onion Architecture with zero external dependencies (no JDBC, Spring, or UI imports). Entities like `LuotKham` and `BaseEntity` manage domain state and invariants; persistence adapters live in `clinic-infrastructure`.
  * Transaction management in [`JdbcTransactionManager`][tx] uses thread-local connections. Nested calls participate in the outer transaction; any failure marks the transaction rollback-only. There is no automated transaction retry or deadlock resolution loop in the infrastructure layer.
  * Audit logging via [`JdbcAuditRepositoryImpl`][audit] and [`SystemAuditEntry`][audit-dto] mandates that mutation audit records participate in the current transaction via `appendInCurrentTransaction`. Audit timestamps are strictly UTC `Instant`.

### 4. Synthetic SQL Server validation matrix

The following validation matrix uses synthetic data only and is not passing evidence in this task. Policy outcomes (uniqueness, no-reuse, shared sequence, date sampling, atomicity and fail-closed cutover) are binding. SQL hints/keys, zero-based counters, contiguous sample numbers, numeric limits and exact rollback assertions are illustrative checks for the proposed physical design, subject to review; gaps are allowed.

| Test ID | Scenario | Execution conditions | Expected invariant & SQL Server assertion |
|---|---|---|---|
| **VAL-ALLOC-01** | Concurrent registration and transfer to same room/day | 2 concurrent transactions (1 registration, 1 transfer) target room 101 on synthetic date `2026-11-10`. | Both transactions serialize via `UPDLOCK, HOLDLOCK` on `QueueDayCounter`. `lastNumber` increments from 0 to 2. `QueueAllocation` contains numbers 1 and 2 with distinct `loaiPhanBo`. Zero duplicate numbers. |
| **VAL-ALLOC-02** | Concurrent transfers of different visits to same room/day | 5 concurrent transfer threads target room 102 on synthetic date `2026-11-10`. | All 5 succeed with sequential numbers 1..5. `QueueDayCounter.lastNumber = 5`. `UNIQUE(maPhongKham, queueDate, soThuTu)` is satisfied. |
| **VAL-ALLOC-03** | Visit CAS race on same visit | 2 concurrent transfers attempt to move visit 5001 (version 3) to room 101 and room 102 simultaneously. | Exactly 1 transaction updates `LuotKham` (affected rows = 1, version becomes 4). Losing transaction gets affected rows = 0, rolls back completely, and leaves no reservation in destination counter or ledger. |
| **VAL-ALLOC-04** | Vacate highest ticket and verify no-reuse | Visit with number 8 in room 101 transfers to room 102. Subsequent registration occurs in room 101. | Next number allocated in room 101 is 9, NOT 8. `QueueDayCounter.lastNumber = 9`. Visible gap at 8 in room 101 queue display. |
| **VAL-ALLOC-05** | Transaction rollback on audit failure | Transfer transaction updates visit, increments counter, inserts allocation, but security audit insertion fails (simulated error). | Complete transaction rollback. `LuotKham` version and room remain unchanged. `QueueDayCounter` increment rolls back. `QueueAllocation` row is discarded. |
| **VAL-ALLOC-06** | Midnight crossing transaction | Transaction samples DB UTC clock at `16:59:59.999Z` (23:59:59.999 clinic time, Nov 10). Lock wait delays commit until `17:00:02Z` (00:00:02 clinic time, Nov 11). | Stored `queueDate` is `2026-11-10`. Allocation, counter key, visit `queueDate`, and receipt consistently record Nov 10. Clinical `ngayKham` is preserved. |
| **VAL-ALLOC-07** | Idempotent replay with same key and payload | Transfer commits successfully. Client resubmits identical request key and canonical payload. | Reauthorization succeeds. Original stored receipt returned immediately. `QueueDayCounter.lastNumber` unchanged. No new row in `QueueAllocation`, `LichSuDieuChuyenLuotKham`, or `NhatKyHeThong`. |
| **VAL-ALLOC-08** | Key reuse with different payload | Client submits previously used request key with a different destination room. | Request rejected with HTTP 409 / stable conflict error. Zero database mutations. |
| **VAL-ALLOC-09** | First-transfer of legacy visit | Legacy visit with `queueDate = NULL` and `soThuTu = 3` transfers to room 105. | Destination allocation receives new number from room 105 counter. Source allocation recorded as `NULL`. Clinical `ngayKham` preserved. |
| **VAL-ALLOC-10** | Integer counter exhaustion guard | Counter reaches `Integer.MAX_VALUE`. Subsequent allocation attempted. | Allocation fails with explicit overflow exception. Transaction rolls back cleanly without integer wrap-around to negative values. |
| **VAL-CUTOVER-01** | Deleted/historical evidence exceeds current MAX | Synthetic room 101/day D: current visit number 7, soft-deleted visit 12, retained soft-deleted allocation 14, historical assignment 16 after its visit moved away. Completeness verified. | Seed at least 16; next committed registration/transfer number exceeds 16. With no verified earlier counter/boundary above 16, seed 16 and next number 17. Never seed 7 or discard deleted evidence. |
| **VAL-CUTOVER-02** | Missing evidence or false empty baseline | Synthetic room/day with possible hard deletion, overwritten assignment or acknowledged commit lacking retained evidence; also an empty retained set without completeness proof. | Preflight blocks cutover and both issuers for that stream; no zero/MAX fallback, fabricated history or no-reuse guarantee. Resume only after verifiable reconciliation or documented owner-approved safe boundary; preserve unknown history. |
| **VAL-CUTOVER-03** | Duplicate and current-assignment conflicts | Two distinct committed assignments share room/day/number (including one deleted); or one visit has two current links / a current projection disagrees with its link's room/date/number. | Preflight detects each conflict and blocks the affected streams before uniqueness/seeding. Two corroborating records of the same proven assignment alone are not a duplicate issuance; ambiguous identity still blocks. No automatic repair. |
| **VAL-CUTOVER-04** | Current-date barrier and midnight | For DB-derived clinic day D, a legacy writer adds number 19 after an initial scan; include a visit with clinical date D-1 but verified queue day D. Barrier then yields a stable snapshot; opening D+1 follows midnight. | Final preflight detects stale seed/current-day conflicts, includes 19 in D and does not misclassify by clinical date. Seed never decreases a higher retained counter. D+1 starts at zero only with verified absence of prior issuance; otherwise reconcile/block it. |

### 5. Remaining open implementation choices

While Owner confirmed P1 and M3 as the architecture design basis under `VIS-TRANSFER-OWNER-DECISION-RECON-06`, this does **not** authorize schema creation or migration. The following implementation choices remain explicitly open; no DDL or runtime enablement is authorized:

1. **Physical DDL and schema definition:** Exact SQL Server data types, column names, nullability constraints, and table schemas for `QueueDayCounter`, `QueueAllocation`, and `LuotKham` queue fields.
2. **Indexes and database constraints:** Specific index definitions (e.g. `(queueDate, maPhongKham, trangThai)`), foreign key constraints between `LuotKham.maCapSoHienTai` and `QueueAllocation`, and unique index declarations.
3. **QA-07 (Lock ordering, concurrency & timeout policy):** Detailed configuration of lock timeouts, deadlock retry limits, transaction isolation parameters, and locking hints in infrastructure adapters.
4. **QA-08 (Cutover baseline & legacy high-water reconciliation):** Acceptance of the preflight legacy verification baseline, reconciliation scripts for historical room/day records, fail-closed handling when committed numbers cannot be proven, and the coordinated cutover window.
5. **Migration version, scripts & writer coordination:** Forward migration versioning (e.g. `V12`), integration with `DatabaseMigrationRunner`, writer barrier timing, and synchronized cutover for registration and transfer adapters.
6. **Rollback & legacy semantics:** Indeterminate commit handling, client error contracts, and legacy visits (`queueDate = NULL`) upon their first transfer.
7. **Transfer note operational limits:** Pinning Unicode version/category behavior, field placement/retention, and Application boundary validation under approved policy N2.
8. **Idempotency storage & operational parameters:** Exact key format/entropy, numeric key retention/reconciliation window, canonical fingerprinting algorithm, duplicate wait timeout deadline, and commit-resolution mechanism after process loss.
9. **Linked-work readers & workflow acceptance:** Implementation of readers to verify pending orders/results safety before transfer can be permitted in runtime.

**Gate status: SEC-01 remains OPEN; runtime transfer remains default-deny.** No permissions, grants, destination reads, or migrations are authorized.

---

## Earlier source-review analysis — retained historical/illustrative material

The analysis in Sections 1–8 below is preserved from earlier review snapshots. Its recommendations, SQL examples and statements that policies remained open are superseded wherever they differ from the current RECON-06 supplement above. Shared room/day uniqueness, committed-number no-reuse, gaps allowed, one DB UTC instant at transaction start and atomicity remain binding. P1/M3 approval covers only their stated concepts; physical SQL choices still require review. Section 9 is the current allocation status table.

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

| Option | Shape and benefits | Consequences / integrity and access costs | Status under RECON-06 |
|---|---|---|---|
| P1 — Current date inline on visit | Concept: current `queueDate` and current-allocation pointer on `LuotKham`, with immutable allocation ledger. Illustrative SQL type only: `DATE` beside current room/number/display; initial registration uses its registration business date, transfer replaces only queue assignment/date. Simple date filtering and indexing; clinical date remains intact. | Alone, this overwrites previous assignments and cannot reserve departed numbers. Needs a durable allocator/reservation store for a no-reuse policy, plus a link to the current allocation if that store is chosen. Date, room, number and display must change atomically. | **OWNER-APPROVED (Concepts Only)** — Current date + pointer on visit with immutable ledger; SQL type, nullability and enforcement in this analysis remain illustrative/unapproved. |
| P2 — One current queue-assignment side table | Unique visit reference holds current room, `queueDate`, number/display. Separates clinical and queue concerns and can avoid adding date to clinical rows. | Reader joins and legacy mapping; current visit location/number fields would be projections requiring atomic synchronization or a separately approved removal. Replacing the side row still loses issued-number history unless reservations are retained. Missing/duplicate assignment must be an explicit error, not an empty queue. | **NOT SELECTED (Historical Alternative)** — Preserved snapshot; rejected due to reader join overhead on all desktop queue screens. |
| P3 — Retained allocations with current pointer | Immutable allocation rows contain room/date/number; visit points to exactly one current allocation. `queueDate` is read through that pointer; past allocations retain reservations. | Clear current-versus-past identity, but adds joins, pointer ownership constraints and projection consistency with existing visit fields. Never find current assignment by `MAX(time)`; ties, retries and later moves make that ambiguous. | **NOT SELECTED (Historical Alternative)** — Preserved snapshot; rejected due to mandatory joins impacting queue query performance. |
| P4 — Derive date from transfer timestamp or audit on every read | A structured durable transfer record could supply the latest effective instant/date without an inline date. | Requires authoritative current-operation identity, initial-registration fallback and indexing; timezone interpretation becomes part of every reader. Existing audit DTO/writer lacks necessary facts. Not deployable using current audit unchanged; fragile as the primary queue read model. | **REJECTED (Historical Alternative)** — Preserved snapshot; rejected as unsuitable and fragile for desktop UI. |

**Owner approval status — P1 approved as design basis under RECON-06.** Store current `queueDate` directly on `LuotKham` for reads; store an allocation reference linking that visit to the exact retained room/date/number assignment. An equivalent constrained relationship may be chosen during later schema review. The ledger tuple is authoritative issuance evidence; the current visit tuple is a projection updated in the same transaction, with reviewed database enforcement that the allocation belongs to that visit and matches room/date/number/display. A bare FK to an unrelated allocation ID is insufficient. This deliberately duplicates current data for read simplicity and adds synchronization cost; alternatives (P2, P3, P4) are not selected and remain historical snapshots. Physical DDL/migration is not authorized by this design basis approval.

Illustrative records for the recommended bundle — SQL types, keys, FKs/checks and exact counter semantics below require design review; logical uniqueness is binding policy:

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

| Mechanism option | Consequences and evidence limits | Status under RECON-06 |
|---|---|---|
| M1 — Continue MAX over current visits | Smallest apparent change, but moving/deleting rows releases apparent capacity. Even adding a current-row unique index cannot preserve numbers after rows leave. Does not support the recommended committed no-reuse policy. | **REJECTED (Violates Policy)** — Accidental reuse violates RECON-05/RECON-06 committed no-reuse invariant. |
| M2 — MAX over retained allocation ledger | Feasible no-reuse design if all issuers retain rows and serialize the indexed room/day range within the transaction, including empty ranges; unique key is a final collision defense. Increasing ledger volume and lock plans need measurement. | **NOT SELECTED (Historical Alternative)** — Preserved snapshot; range locking under high concurrency poses lock escalation risk compared to M3 counter. |
| M3 — Transactional room/day counter plus ledger | **Approved design basis.** Counter avoids repeatedly scanning issued allocations; ledger provides ownership/reservation evidence and unique tuple. Counter increment, ledger insert and visit mutation must commit or roll back together. Costs an extra coordination row and a ledger write. | **OWNER-APPROVED (Design Basis Only)** — Architecture preference under RECON-06. Physical schema/DDL, index definitions, and migrations remain unapproved. |
| M4 — Dedicated database sequence/reserved blocks | Candidate only if Owner accepts a separately specified gap/reservation lifecycle, room/day reset strategy and operational management. No native sequence exists in inspected source. Engine rollback/cache behavior and exhaustion must be verified in the later implementation; do not assume transaction rollback restores issued values. | **NOT SELECTED (Historical Alternative)** — Preserved snapshot; lacks native room/day dynamic keying and sequence rollbacks discard numbers unpredictably. |

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

For the recommended design, apply corrected Section 2.5 and its synthetic preflight assertions: include all trustworthy persisted evidence, including soft-deleted visits/allocations and any existing historical assignment record. Previous moves or purges cannot be reconstructed from current MAX. Block each unproved room/day until a verifiable reconciliation source or owner-approved safe boundary is established; an observed maximum or unspecified baseline is insufficient. No automatic clinical-date rewrite, deletion, renumbering or fabricated historical allocation. This task performed no data profiling or clinic DB access.

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

## 9. Status of allocation choices: RECON-06 approvals and remaining open decisions

The approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-06` (contract id `13d19fa9-eaed-404e-bdfc-4070daffd38a`) confirmed P1 and M3 as the architecture design basis, building upon RECON-04 and RECON-05 policies (QA-02, QA-04, QA-05), while explicitly preserving schema, DDL, locking, and cutover details as open implementation choices:

| ID | Topic & decision | Status under RECON-06 | Approved policy / Open scope |
|---|---|---|---|
| QA-01 | Where is current `queueDate` persisted: P1 inline visit, P2 side table, P3 allocation pointer only, or P4 structured-history derivation? | **APPROVED AS DESIGN BASIS (P1 concepts only)** | Current `queueDate` and current-allocation pointer on `LuotKham`, with immutable allocation ledger. `maCapSoHienTai`, SQL types/nullability, FKs/checks, indexes and exact DDL are illustrative proposals requiring review, not approvals. |
| QA-02 | Which sequence namespace: N1 room/day, N2 department/day, N3 broader/day? Shared across registration and transfer? | **APPROVED (RECON-05/RECON-06)** | **N1 room/day:** Exactly one number stream per `(roomId, queueDate)` shared by registration and transfer. Hierarchy is validated metadata, not a counter reset key. |
| QA-03 | Which coordination/reservation mechanism: M2 retained-ledger MAX, M3 counter+ledger, or M4 sequence? | **APPROVED AS DESIGN BASIS (M3 concepts only)** | Transactional per room/day high-water counter plus immutable `QueueAllocation` ledger. Room/day uniqueness and no-reuse are binding policy; exact database enforcement, counter semantics, initialization/locking, reconciliation tooling and migrations remain OPEN. |
| QA-04 | May any **committed** allocation be reused after move/cancellation/completion/deletion? Are gaps acceptable? | **APPROVED (RECON-05/RECON-06)** | **R2 no-reuse; gaps allowed:** Committed numbers are never reused within the same room/day; visible gaps are permitted. Confirmed-aborted unissued numbers may be allocated later. |
| QA-05 | Which clock authority and capture point define `queueDate`? | **APPROVED (RECON-05/RECON-06)** | **DB UTC instant at transaction start:** Exactly one SQL Server DB UTC instant sampled at transaction start and converted to `Asia/Ho_Chi_Minh` calendar date for `queueDate`. Bound for the entire transaction even if crossing midnight. |
| QA-06 | After confirmed rollback, does a fresh attempt sample a new date or retain the original attempt date? | **OPEN (Proposal)** | **Recommend fresh sample:** Fresh DB UTC sample after full revalidation. Committed allocations never change date upon replay/recovery. |
| QA-07 | Which consistency strategy protects hierarchy/linked-work until commit? | **OPEN (Proposal)** | **Recommend CAS + targeted locking:** One connection/transaction, original-version CAS on `LuotKham`, targeted protection for target hierarchy, and deterministic resource order. |
| QA-08 | Legacy high-water evidence, reconciliation tooling and coordinated cutover | **Safeguard BINDING; physical procedure OPEN** | Establish high-water from all trustworthy committed evidence, including soft-deleted assignments. Block cutover and both issuers for an unprovable room/day until verifiable reconciliation or a separately approved safe boundary exists. No invented history. Reconciliation SQL/tooling, writer coordination and cutover execution remain unapproved. |

Reason-code taxonomy is approved as a 6-code role-partitioned business taxonomy; optional note policy N2 is approved as design direction, while exact Unicode limits, field placement, and retention remain open. Dedicated immutable transfer history plus same-transaction security audit is approved under RECON-05; audit-only is closed. Idempotency direction is approved under RECON-06 (high-entropy key, canonical command, stored receipt on same key+payload, conflict on same key+different payload); detailed key format, wait deadlines, and operational recovery remain open. SEC-01 remains OPEN and runtime transfer remains default-deny.

## 10. Historical guidance, retrieval and validation record

The following record belongs to the prior proposal update, not this boundary correction. Its blanket PASS assessments did not resolve the physical-detail approval contradiction; the current RECON-06 clarification above supersedes that interpretation.

Read before editing:
1. Orchestrator baseline: `docs/PROJECT_CONVENTIONS.md`.
2. Repository `README.md`.
3. Architecture and domain specifications: `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `UI_CODING_STANDARDS.md`.
4. Runbook and plans: `docs/OPERATIONS_RUNBOOK.md`, `docs/ke-hoach-trien-khai/README.md`.
5. Retrieved phase reviews: `docs/ke-hoach-trien-khai/phase3-visit-transfer-contract-review.md`, `docs/ke-hoach-trien-khai/phase3-transfer-linked-work-eligibility-review.md`, `docs/ke-hoach-trien-khai/phase3-visit-lifecycle-owner-decisions.md`, `docs/ke-hoach-trien-khai/phase0-db-config-remaining-decisions-proposal.md`, `docs/ke-hoach-trien-khai/phase3-transfer-reason-policy-proposal.md`, `docs/ke-hoach-trien-khai/phase3-visit-transfer-decision-gaps.md`, `docs/ke-hoach-trien-khai/phase3-transfer-queue-history-persistence-proposal.md`, `docs/ke-hoach-trien-khai/phase3-transfer-domain-invariant-design.md`, and `docs/ke-hoach-trien-khai/phase0-repository-contract-review.md`.
6. Approved contracts: `VIS-TRANSFER-OWNER-DECISION-RECON-06` (contract id `13d19fa9-eaed-404e-bdfc-4070daffd38a`), carrying forward `VIS-TRANSFER-OWNER-DECISION-RECON-05` and `VIS-TRANSFER-OWNER-DECISION-RECON-04`.

No applicable `AGENTS.md` was found in checkout inventory, target ancestors, or filesystem ancestors checked. Repository-local `docs/DEVELOPMENT_CONVENTIONS.md`, `CONTRIBUTING.md`, `docs/README.md`, and `ARCHITECTURE.md` were absent; the supplied baseline was used.

**RAG first execution record (2026-11-06):**
Executed `/Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py --repo clinic-desktop --limit 5 'VIS-TRANSFER-OWNER-DECISION-RECON-06 P1 M3 queue persistence allocation proposal'`.
- *Result:* Command execution was blocked by the user-configured terminal sandbox deny rule (`permission check failed for command ...: Permission denied for command ... Matches user-configured deny rule`).
- *Primary guidance:* Used task-supplied retrieved excerpts, approved contract `VIS-TRANSFER-OWNER-DECISION-RECON-06`, and direct inspection of allowlisted files. No sandbox circumvention, privilege escalation, clinic DB connection, or patient data access.

Validation for this deliverable:
- Only allowlisted files modified (`docs/ke-hoach-trien-khai/phase3-transfer-queue-allocation-proposal.md` and `docs/ke-hoach-trien-khai/phase3-transfer-queue-history-persistence-proposal.md`).
- Formatting checks: clean markdown, valid line anchors, no trailing whitespace.
- Document-only scope: no Java, SQL migrations, runtime permissions, or gate changes. SEC-01 remains OPEN / default-deny.
- Synthetic validation matrix provided without using real PHI or patient data.

| Acceptance criterion | Result and evidence |
|---|---|
| Only allowlisted proposal docs change | PASS — Edits confined strictly to the allowlisted files. |
| Dated RECON-06 supplement included | PASS — RECON-06 supplement dated 2026-11-06 included under authority of contract id `13d19fa9-eaed-404e-bdfc-4070daffd38a`. |
| P1 and M3 marked Owner-approved design direction, not schema/migration approval | PASS — P1 and M3 explicitly labeled as architecture design basis only; physical DDL, schema, and migrations remain unapproved. |
| Historical alternatives relabeled | PASS — P2, P3, P4 and M1, M2, M4 formally categorized as not selected / historical alternatives or rejected, preserving analysis snapshots. |
| No-reuse/high-water correction remains correct and legacy unknowns fail closed | PASS — High-water cutover safeguard preserved; cutover stops on unproven history until verifiable reconciliation or approved boundary. |
| Remaining DDL and cutover implementation decisions clearly open | PASS — Physical DDL, indexes, constraints, locking hints, counter initialization, migration version, cutover window, and writer coordination remain open. |
| No Java, SQL migration, runtime behavior, grants or gate changes | PASS — Pure documentation analysis; SEC-01 remains OPEN/default-deny. |

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
