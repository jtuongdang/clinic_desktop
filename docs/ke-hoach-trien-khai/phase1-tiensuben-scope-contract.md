# Phase 1 — `TienSuBenh` row scope contract

**Decision date:** 2026-11-06  
**Status:** Owner-approved policy; V10 storage and guarded/audited application read/write foundations accepted locally; UI and SQL Server acceptance pending
**Related tasks:** `PAT-02`, `PAT-HISTORY-REVIEW-01`, `PAT-HISTORY-SCOPE-01`, `PAT-HISTORY-READ-01`, `PAT-HISTORY-WRITE-01`

## Approved policy

- Every newly created history row is owned by one exact `(maCoSo, maKhoa)` pair.
- Only a `DOCTOR` session selecting that exact facility+department pair may read the row. Matching only the facility, patient, encounter, or role is insufficient.
- Reads of the approved categories (allergy, chronic condition, prior surgery) query only rows owned by the session pair. A successful read is audited in an independent transaction after the query using action `PATIENT_VIEW`, resource `PATIENT`, and the requested patient ID as `resourceId`; the audit records actor, exact facility+department pair, UTC time and correlation ID, never clinical narrative. The audit must commit before the application returns data. Audit failure means no history response.
- Every new row receives scope from the trusted session context, not request payload. Do not derive it from `BenhNhanCoSo`, the most recent encounter, or client-supplied fields.
- A legacy row without verified ownership remains inaccessible. Do not auto-backfill from current facility, recent `LuotKham`, or inferred department. Backfill is permitted only after trustworthy provenance is established and an explicitly scoped reconciliation is reviewed.
- Other history categories, delete, post-lock editing and `AUDIT_READ` remain denied. This contract grants no access until implementation and acceptance pass.

## Current source facts

Before V10, `TienSuBenh` had patient ID, category and narrative only, with no history repository. `Khoa` belongs to `CoSo` through `Khoa.maCoSo`; the stored pair must be validated as a real facility/department pair, not as two unrelated foreign keys. `PAT-HISTORY-UI-01` replaced a mock encounter-history table with an explicit unavailable state; it did not implement a `TienSuBenh` read/write workflow.

## Implementation sequence

1. **Scope storage (`PAT-HISTORY-SCOPE-01`):** add forward migration V10 with nullable facility/department columns for legacy compatibility. Add a pair check (both null for legacy or both present), referential integrity proving that the department belongs to the facility, and a query-supporting index. Never invent a backfill. New domain/application writes must require both positive IDs and persistence must reject a missing/partial scope for new writes. Null-scoped rows must be excluded from every clinical read. This phase does not enable a history workflow.
2. **Read/write use cases:** add a dedicated history repository port/adapter and command/query boundaries. Revalidate active session, `DOCTOR` actor, `PATIENT:XEM` grant, exact selected pair, target patient and approved categories in Application. `PATIENT:XEM` alone is insufficient: non-DOCTOR roles remain denied. Use audit action `PATIENT_VIEW` with resource type `PATIENT` and patient ID. Inserts take scope from trusted session context. Queries include both scope IDs and exclude null legacy rows in SQL. Missing/ambiguous session, unsupported type or mismatched scope deny safely.
3. **Audit and atomicity:** audit every approved mutation in the same transaction as the mutation. For reads, execute the exact-scope query, append the successful-read audit in an independent transaction, and return results only after audit commit. Query/scope/audit failure means no history response. Do not expose audit-trail browsing.
4. **Doctor UI:** add a dedicated `Tiền sử bệnh` view for only approved categories, display only query results, and clear content immediately on patient/scope change, logout, or failure. No cache/fallback may preserve data across scopes. Keep encounter history distinct.
5. **Acceptance:** domain invariants; migration/bootstrap and legacy-null checks on synthetic SQL Server; repository filtering across two facilities/departments for one synthetic patient; exact-pair DOCTOR allow and mismatched/non-DOCTOR deny; no query/response on denied scope; audit commit-before-response and fail-closed audit failure; mutation+audit rollback; UI stale-data clearing. SQL Server tests remain opt-in and use only named disposable `clinic_test_*` databases.

## Interim behavior

`PAT-HISTORY-READ-01` implements a guarded application query and JDBC read path; it requires DOCTOR, active exact-scope authorization and `PATIENT:XEM`, and it writes `PATIENT_VIEW` audit before returning data. The query is not wired to the Doctor UI because the current station still lacks real patient IDs and injected session context. Create/edit now has an application command/repository foundation in `PAT-HISTORY-WRITE-01`, guarded to DOCTOR and the exact patient/session scope with same-transaction audit; its tests passed locally, but SQL Server persistence/rollback acceptance and UI wiring remain pending. The Doctor UI must continue to show unavailable state and must not synthesize history from the current encounter DTO. No history read/write screen workflow is enabled yet.


## Storage foundation evidence — 2026-11-06

V10 is integrated on `feature/patient-reception-workflow`. SQL Server opt-in `FreshDatabaseMigrationSqlServerTest` passed 1/1 on disposable `clinic_test_v10_history_scope_final_20261106`: V1–V10 applied; a legacy row inserted before V10 retained null scope; new unscoped inserts were rejected; a mismatched department/facility pair was rejected; a matching pair was accepted. No legacy backfill was performed. Root JDK 17 `mvn -Djava.awt.headless=true clean verify` passed 283 tests at the storage-task snapshot, 0 failures/errors, 4 SQL Server opt-in skips (61 reports). Subsequent `PAT-HISTORY-READ-01` added a read port/adapter and guarded query. `PAT-HISTORY-WRITE-01` added a DOCTOR-only save command and JDBC create/update path with same-transaction audit. Root `mvn -Djava.awt.headless=true clean verify` now passes 304 tests, 0 failures/errors, 4 opt-in skips (64 reports). No SQL Server runtime write/rollback test or UI wiring has been accepted. Two earlier disposable DBs from failed assertion iterations were left untouched and were not dropped.

## Read foundation evidence — 2026-11-06

`PAT-HISTORY-READ-01` (Copilot CLI; agent commit `98c1409`, local integration `ba69e1e`) added the read port/JDBC adapter and `GetPatientHistoryQuery` handler. Application tests cover exact DOCTOR/session/grant gates, actor mismatch, query-before-audit and fail-closed audit errors; repository tests cover active rows, exact pair, approved categories and mapping. Audit uses `PATIENT_VIEW`/`PATIENT`, patient ID as resource ID, exact scope and correlation metadata, without clinical narrative. Root JDK 17 `mvn -Djava.awt.headless=true clean verify` passed 295 tests, 0 failures/errors, 4 opt-in skips (63 reports). The read foundation does not connect the Doctor UI or accept the full history workflow.


## Audit event mapping — Owner-approved 2026-11-06

Use the existing `PATIENT_VIEW` action and `PATIENT` resource. Set `resourceId` to the requested patient ID; include the authenticated actor and exact facility/department scope, UTC timestamp and correlation ID. Do not store `moTa` or other clinical narrative in audit. Commit this independent audit transaction before returning query results.


## Write foundation evidence — 2026-11-06

`PAT-HISTORY-WRITE-01` (Codex CLI, Orchestrator task `5b829c1c-e781-4926-9726-ca165ce64b5b`) added a command and JDBC create/update methods. Application revalidates DOCTOR + active exact-scope `PATIENT:XEM`, verifies the patient through an active visit in the same pair, and for updates selects only an active history row by history/patient/pair and expected version. New ownership scope comes from session context; audit uses `PATIENT_UPDATED`/`PATIENT` in the same transaction, without narrative. Synthetic application/JDBC tests pass. The Orchestrator agent validation exhausted its single recovery after test compilation errors; root repaired the shared-port fixtures and test imports, also set new row `createdAt` via `SYSUTCDATETIME()`, then focused tests and full JDK 17 verify passed. Integrated application commit is `0cdef02`; follow-up commit `77b5214` replaces JDBC generated-key retrieval with SQL Server `OUTPUT ... INTO` so the V10 insert trigger is supported, and keeps creation metadata UTC. Targeted JDBC tests and the final root verify pass. SQL Server runtime write/rollback and UI remain pending. No PR/push.`
