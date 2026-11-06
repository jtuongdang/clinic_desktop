# Phase 1 — `TienSuBenh` row scope contract

**Decision date:** 2026-11-06  
**Status:** Owner-approved policy; implementation pending  
**Related tasks:** `PAT-02`, `PAT-HISTORY-REVIEW-01`, `PAT-HISTORY-SCOPE-01`

## Approved policy

- Every newly created history row is owned by one exact `(maCoSo, maKhoa)` pair.
- Only a `DOCTOR` session selecting that exact facility+department pair may read the row. Matching only the facility, patient, encounter, or role is insufficient.
- Reads of the approved categories (allergy, chronic condition, prior surgery) query only rows owned by the session pair. A successful read is audited in an independent transaction after the query; the audit must commit before the application returns data. Audit failure means no history response.
- Every new row receives scope from the trusted session context, not request payload. Do not derive it from `BenhNhanCoSo`, the most recent encounter, or client-supplied fields.
- A legacy row without verified ownership remains inaccessible. Do not auto-backfill from current facility, recent `LuotKham`, or inferred department. Backfill is permitted only after trustworthy provenance is established and an explicitly scoped reconciliation is reviewed.
- Other history categories, delete, post-lock editing and `AUDIT_READ` remain denied. This contract grants no access until implementation and acceptance pass.

## Current source facts

`TienSuBenh` currently has patient ID, category and narrative only. V1 table `TienSuBenh` has no facility/department columns or history repository. `Khoa` belongs to `CoSo` through `Khoa.maCoSo`; the stored pair must be validated as a real facility/department pair, not as two unrelated foreign keys. `PAT-HISTORY-UI-01` replaced a mock encounter-history table with an explicit unavailable state; it did not implement `TienSuBenh`.

## Implementation sequence

1. **Scope storage (`PAT-HISTORY-SCOPE-01`):** add forward migration V10 with nullable facility/department columns for legacy compatibility. Add a pair check (both null for legacy or both present), referential integrity proving that the department belongs to the facility, and a query-supporting index. Never invent a backfill. New domain/application writes must require both positive IDs and persistence must reject a missing/partial scope for new writes. Null-scoped rows must be excluded from every clinical read. This phase does not enable a history workflow.
2. **Read/write use cases:** add a dedicated history repository port/adapter and command/query boundaries. Revalidate session, exact selected pair, DOCTOR actor, target patient and approved categories in Application. Inserts take scope from trusted session context. Queries include both scope IDs and exclude null legacy rows in SQL. Missing/ambiguous session, unsupported type or mismatched scope deny safely.
3. **Audit and atomicity:** audit every approved mutation in the same transaction as the mutation. For reads, execute the exact-scope query, append the successful-read audit in an independent transaction, and return results only after audit commit. Query/scope/audit failure means no history response. Do not expose audit-trail browsing.
4. **Doctor UI:** add a dedicated `Tiền sử bệnh` view for only approved categories, display only query results, and clear content immediately on patient/scope change, logout, or failure. No cache/fallback may preserve data across scopes. Keep encounter history distinct.
5. **Acceptance:** domain invariants; migration/bootstrap and legacy-null checks on synthetic SQL Server; repository filtering across two facilities/departments for one synthetic patient; exact-pair DOCTOR allow and mismatched/non-DOCTOR deny; no query/response on denied scope; audit commit-before-response and fail-closed audit failure; mutation+audit rollback; UI stale-data clearing. SQL Server tests remain opt-in and use only named disposable `clinic_test_*` databases.

## Interim behavior

Until each required step is implemented and accepted, all history reads and writes remain default-deny. The Doctor UI may show unavailable state; it must not synthesize history from the current encounter DTO.
