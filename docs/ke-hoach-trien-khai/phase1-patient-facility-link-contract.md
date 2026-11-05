# Phase 1 — Patient–facility link contract

**Status:** Implementation contract derived from owner-approved DEC-PAT-01 and existing reception/session authorization behavior. Owner reconfirmed on 2026-11-06 that each patient–facility pair may have only one active link. It does not approve cross-facility patient access.

## Purpose

Ensure every successfully registered patient has an active `BenhNhanCoSo` link to the facility selected in the authenticated session. Registration already creates a scoped visit at that facility; persisting the relation keeps the patient-facility model consistent and prevents repeated registrations from creating duplicate links.

## Required behavior

- `RegisterPatientHandler` ensures the link inside the existing registration transaction, after the patient has a persisted ID and before success is returned.
- Use `AuthorizationContext.facilityId()` as the facility and `AuthorizationContext.employeeId()` as the creator. Do not accept facility/actor IDs from editable form values.
- The operation is idempotent: an active `(maBenhNhan, maCoSo)` link is left unchanged. If only soft-deleted links exist, insert a new row; do not revive an old row or reuse its ID.
- The uniqueness invariant is per exact pair: the same patient may have active links to different facilities, but may not have two active links to the same facility.
- V9's filtered unique index is the database invariant. The repository must serialize the ensure operation safely under concurrent transactions and record one UnitOfWork change only when it inserts a row.
- A link write failure fails the full registration transaction, including patient, insurance, visit/queue, and their existing audit entries.
- Link creation is part of the already-audited registration event; do not add an unapproved audit action code.
- The relation is not an authorization grant. Patient reads/updates continue to use the current exact facility+department authorization and visit checks; do not expose patients in another department/facility through `BenhNhanCoSo`.
- Do not implement, load, or display `TienSuBenh` in this task. Its write policy is DOCTOR-only and remains default-deny until its own guarded/audited workflow is accepted.

## Acceptance

1. Application tests cover new patient, in-scope existing patient, already-linked patient, exact facility/actor propagation, and rollback when ensuring the link fails.
2. Infrastructure unit tests cover parameter binding, active-link idempotency, soft-delete semantics, transaction requirement, and UnitOfWork accounting.
3. SQL Server opt-in acceptance runs the real composition on synthetic data, verifies one active link after registration/re-registration, and confirms the generated link carries the selected facility and actor.
4. No change expands role grants, patient search/read scope, history access, or other clinical workflows.
5. JDK 17 `mvn -Djava.awt.headless=true clean verify` and `git diff --check` pass. SQL Server acceptance is reported separately if the dedicated disposable database is unavailable.
