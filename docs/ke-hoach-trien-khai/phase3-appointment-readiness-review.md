# APPT-REVIEW-01 — Phase 3 appointment domain and workflow readiness

**Status:** Report-only source review; no production/test code, schema, grants, or business policy changed.
**Gate:** `SEC-01` remains incomplete. Do not implement or enable `APPT-01` until the security gate and its acceptance are complete.
**Scope:** Booking, rescheduling, cancellation, duplicate/overbooking, facility/department/doctor scope, check-in/no-show, visit conversion, time, reminders/notifications, payment, audit/privacy, UI/print, persistence, use cases, and tests.
**Method:** Static source/document review only. No database connection, database query, or real patient data used.

## Current status addendum — 2026-11-06

This addendum reconciles the owner-decision ledger with Owner-confirmed contract `APPT-OWNER-DECISION-RECON-02` (`84c00d27-e4e1-463b-9964-67543c9ccb28`). It records policy only and does not authorize implementation.

### Approved action mapping

- Use existing `APPOINTMENT:XEM` for read only; existing `APPOINTMENT:TAO` for creation only, with new appointments starting at `CHO_XAC_NHAN`; and existing `APPOINTMENT:SUA` for editing and rescheduling only.
- Cancellation, confirmation, check-in, no-show/reversal, and conversion to `LuotKham` remain **DENY / unimplemented** pending separate action-specific guard, actor, transition, and acceptance decisions.
- Existing grants and exact selected facility/department session scope are unchanged. `CASHIER:XEM` remains read only; no grant is added or broadened. Patient self-service remains **DENY / unimplemented**.

### Approved capacity basis

- Allow one appointment at a time per doctor within the selected facility/department pair. Any overlapping interval for that doctor conflicts; intervals are half-open `[start,end)`, so adjacent appointments are allowed.
- No room is required as an appointment capacity resource for this MVP basis. Overbooking is denied; no waitlist or exception role is approved.

### Still open before implementation

Required fields and source mapping; positive duration and appointment types; working hours, breaks, and holidays; which statuses and soft-deleted records occupy capacity and when capacity releases; reschedule limits; cancellation cutoff/reason policy; check-in window and late-arrival handling; no-show policy; conversion preconditions, atomic uniqueness, and idempotency; notifications, consent, content, and privacy; time-zone persistence; and audit, persistence, concurrency, rollback, and UI workflow acceptance. Appointment deposits, payment, refunds, and billing remain **DENY**.

`SEC-01` remains **OPEN / default-deny**. Appointment work remains blocked pending SEC-01 baseline acceptance and separate APPT security, persistence, concurrency, audit, rollback, and visual acceptance.

See the [current appointment owner-decision proposal](phase3-appointment-owner-decisions.md) for the approved contract and its evidence. Read-only inspection of `LichHen`, `ILichHenRepository`, `TrangThaiLichHen`, and V8 grants confirms source facts only; it does not change policy or authorize implementation. RAG retrieval was attempted first, but the sandbox denied permission to run the external script and returned no task-specific excerpts; no workaround, database access, or implementation change was made.

## Historical report snapshot — original review content

The report body below is preserved as the original review snapshot. Its open-decision ledger is historical, not a statement of current owner-decision status; consult the current-status addendum above.

## Evidence labels

- **[FACT]** Directly visible in the cited source or document.
- **[APPROVED POLICY]** Owner-approved contract/policy; it is not evidence that a workflow is implemented.
- **[INFERENCE]** Technical conclusion or recommendation derived from the cited facts/policy.
- **[OWNER DECISION]** Appointment-specific policy still needing an explicit decision.
- **[ACCEPTANCE GATE]** Observable implementation/test condition; it does not authorize implementation before `SEC-01`.

## Readiness conclusion

- **[FACT]** Appointment domain artifacts exist: `LichHen`, `TrangThaiLichHen`, `NguonLich`, `LichHenDaDatSuKien`, `ILichHenRepository`, notification/payment ports, and a V1 table. Their existence does not constitute a usable workflow (`clinic-domain/src/main/java/vn/clinicmanagement/domain/appointment/LichHen.java:13-65`; `clinic-domain/src/main/java/vn/clinicmanagement/domain/repositories/ILichHenRepository.java:6-9`; `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql:294-324`).
- **[FACT]** Repository symbol searches across `clinic-application`, `clinic-infrastructure`, and `clinic-desktop` found no appointment application handler/use case, `ILichHenRepository` adapter, notification/payment adapter, or appointment persistence test. The only appointment-specific test found is `LichHenTest`, which checks preservation of a nullable creator and no lifecycle or persistence behavior (`clinic-domain/src/test/java/vn/clinicmanagement/domain/appointment/LichHenTest.java:8-20`).
- **[FACT]** The roadmap says Phase 3 has no end-to-end acceptance evidence, and `APPT-01` remains `[ ] Planned`; the approved dependency list includes `SEC-01` (`docs/ke-hoach-trien-khai/02-cac-dot-trien-khai.md:45-51`; `docs/ke-hoach-trien-khai/03-model-task-board.md:100-103`).
- **[INFERENCE]** No appointment workflow is available through Application/UI/persistence. Existing `APPOINTMENT` grants do not themselves implement a use case; do not extend them to patient self-service, reminders, deposits, or unspecified transitions. Existing menu labels, print previews, domain setters, and an FK are not authorization or workflow boundaries.

## Source map: appointment model, contract, and schema

| Element | Source-grounded evidence |
|---|---|
| Appointment identity, scope, schedule | **[FACT]** `LichHen` has `maBenhNhan`, `maBacSi`, `maCoSo`, nullable `maKhoa`, `thoiGianBatDau`, and `thoiGianKetThuc` (`clinic-domain/src/main/java/vn/clinicmanagement/domain/appointment/LichHen.java:13-20`). It has navigation references to patient, doctor, facility and department, but no room ID (`:30-33`). |
| Status and descriptive/source data | **[FACT]** It defaults to `CHO_XAC_NHAN`; `lyDo` is an empty string and `nguonDat` is a `String` defaulting to `LE_TAN` (`LichHen.java:20-22`). `NguonLich` is a separate enum with `TU_DONG_THEO_LICH_MAU`, `PHAT_SINH_THU_CONG`, and `DOI_CA`; it is not the field type (`clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/NguonLich.java:3-19`). |
| Deposit/payment-related fields | **[FACT]** The entity initializes `chiPhiGiuCho` to `100000`, `daThanhToanGiuCho` to false, and has `soPhieuThu`, `ngayThanhToan`, and `nguoiThuTien` (`LichHen.java:23-27`). The V1 SQL default for `chiPhiGiuCho` is `0`, not the entity value; the V1 default for `daThanhToanGiuCho` is false (`clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql:307-312`). |
| Cancellation data | **[FACT]** `ghiChuHuy` is a nullable string (`LichHen.java:28`, `:95-96`). No cancellation actor/time or appointment-specific audit record is in the entity's declared fields (`LichHen.java:13-33`). |
| Inherited entity fields | **[FACT]** `CoSoThucThe<Integer>` contributes ID/version, created/updated/deleted actors and timestamps, soft-delete, and in-memory domain-event operations (`clinic-domain/src/main/java/vn/clinicmanagement/domain/common/CoSoThucThe.java:16-33`, `:38-79`, `:103-123`). Its timestamps are assigned using `LocalDateTime.now()` (`CoSoThucThe.java:30-32`, `:74-84`); this does not demonstrate appointment audit persistence. |
| Inherited field/method names | **[FACT]** Fields are `id`, `version`, `createdAt/By`, `updatedAt/By`, `isDeleted`, `deletedAt/By`, and transient `domainEvents`; operations include `markAsUpdated`, `softDelete`, `restore`, `registerDomainEvent`, `getDomainEvents`, `pullDomainEvents`, and `clearDomainEvents` (`CoSoThucThe.java:17-33`, `:74-123`). |
| Booking event | **[FACT]** `LichHen` constructor defaults status, stores the supplied fields, sets creator, and registers `LichHenDaDatSuKien` (`LichHen.java:35-47`). The event payload is appointment ID, patient ID, doctor ID and start time (`clinic-domain/src/main/java/vn/clinicmanagement/domain/common/events/LichHenDaDatSuKien.java:3-20`). When the constructor has no ID yet, it registers ID `0` (`LichHen.java:47`). No appointment event consumer/publisher wiring was found. |
| Status enum | **[FACT]** The values are `CHO_XAC_NHAN`, `DA_XAC_NHAN`, `DA_DEN`, `DA_HUY`, and `HOAN_THANH`; there is no no-show status or defined transition graph (`clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/TrangThaiLichHen.java:3-25`). |
| Enum operations | **[FACT]** `TrangThaiLichHen` and `NguonLich` expose `getValue()`, `getDescription()`, and `fromValue(int)`; these are code/description conversions, not workflow transitions (`clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/TrangThaiLichHen.java:10-25`; `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/NguonLich.java:8-25`). |
| Entity lifecycle methods | **[FACT]** `xacNhanThuTienMat` rejects only `DA_HUY`, sets payment fields/time/actor, and changes status to `DA_XAC_NHAN`; `huyLichHen` rejects only `DA_DEN` and `HOAN_THANH`, then sets `DA_HUY` and the reason (`LichHen.java:50-65`). All appointment properties also have public setters (`LichHen.java:67-104`). No method for rescheduling, check-in, no-show, or visit conversion is present. |
| Repository port | **[FACT]** `ILichHenRepository` only adds `getUpcomingAppointments(patientId)` and `countAppointmentsInSlot(doctorId,start,end)` to generic CRUD (`clinic-domain/src/main/java/vn/clinicmanagement/domain/repositories/ILichHenRepository.java:6-9`). The count signature contains no facility, department, room, status filter or capacity parameter. |
| Notification port | **[FACT]** `IThongBaoDichVu` declares confirmation, reminder, and cancellation-notice methods (`clinic-domain/src/main/java/vn/clinicmanagement/domain/appointment/IThongBaoDichVu.java:2-5`). No provider, delivery outcome, consent/opt-in, retry, scheduling, deduplication, or idempotency contract was found. |
| Payment port | **[FACT]** `IThanhToanCongKetNoiDichVu` declares URL creation and raw callback processing plus result records (`clinic-domain/src/main/java/vn/clinicmanagement/domain/appointment/IThanhToanCongKetNoiDichVu.java:3-7`). No adapter or consuming application use case was found. |
| Approved appointment grants | **[APPROVED POLICY]** V8 seeds `APPOINTMENT:XEM/TAO/SUA` for `DOCTOR` and `RECEPTIONIST`, and `APPOINTMENT:XEM` for `CASHIER` (`clinic-infrastructure/src/main/resources/migrations/V8__Seed_Approved_Permission_Grants.sql:23-25`, `:46-48`, `:61`). These are generic role/action grants under the approved `KHOA_PHONG` model; they are not handlers or business rules. The approved decision explicitly keeps appointment deposits denied (`phase2-role-permission-matrix-approval.md:10-18`). |
| V1 appointment table | **[FACT]** `LichHen` SQL stores appointment IDs, nullable department, start/end `DATETIME2`, status/source/reason, deposit and cancellation fields, shared version/audit/soft-delete fields, and FKs to patient/doctor/facility/department (`clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql:294-324`). It has no room, no-show, cancellation actor/time, reminder, notification, consent, or payment-provider callback/idempotency fields in that table. |
| V1 overlap/concurrency constraints | **[FACT]** The appointment table has no unique slot constraint in its V1 definition; the V1 index section defines an index for visit queues but no `LichHen` slot/overlap index or unique constraint (`V1__Initial_Clinic_Schema.sql:294-324`, `:1138-1144`). |
| Visit linkage | **[FACT]** V1 `LuotKham` has nullable `maLichHen` and a foreign key to `LichHen`, but this is not unique (`V1__Initial_Clinic_Schema.sql:329-380`). `LuotKham` has a nullable `maLichHen` property and setter (`clinic-domain/src/main/java/vn/clinicmanagement/domain/examination/LuotKham.java:28-31`, `:121-122`). The ERD depicts one appointment to zero-or-one visit (`ERD_DIAGRAM.md:129-140`), but the V1 FK does not enforce that cardinality. |
| Domain analysis and ERD | **[FACT]** The domain analysis describes the appointment entity, methods, notification/payment ports, and repository count contract (`DOMAIN_ANALYSIS.md:244-250`, `:323-327`). The ERD lists appointment fields but does not include `ghiChuHuy`, payment actor/time, or shared audit fields in its `LichHen` block (`ERD_DIAGRAM.md:676-691`); V1 is the more complete schema representation. |

## Related application, persistence, and navigation slice found

- **[FACT]** Reception's existing `RegisterBenhNhanHandler` creates a visit in its protected transaction and audits `VISIT_REGISTERED` (`clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterBenhNhanHandler.java:97-130`). Its `TiepDonHangDoiCongKetNoi.registerVisit` parameters contain no appointment ID (`clinic-application/src/main/java/vn/clinicmanagement/application/reception/TiepDonHangDoiCongKetNoi.java:9-14`); `TiepDonHangDoiRepository` inserts a new `LuotKham` without `maLichHen` (`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/TiepDonHangDoiRepository.java:34-48`). This is reception registration, not appointment conversion.
- **[FACT]** `DieuHuongQuyenHandler` recognizes `APPOINTMENT` as an approved module code (`clinic-application/src/main/java/vn/clinicmanagement/application/authorization/DieuHuongQuyenHandler.java:18-22`), but `BangDieuKhienDieuHuongChinhSach` maps grants only to existing home/reception/treatment/system/catalog/billing/paraclinical/pharmacy keys and has no appointment key (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/BangDieuKhienDieuHuongChinhSach.java:29-42`). Thus an `XEM` grant does not reveal an appointment screen.

## Lifecycle and technical risk assessment

### Booking, update, and cancellation

- **[FACT]** Construction registers a booked event and starts at `CHO_XAC_NHAN`; confirmation is coupled to a cash-deposit method; cancellation has only two terminal-state guards (`LichHen.java:35-65`).
- **[INFERENCE]** The constructor does not validate start-before-end, required IDs, valid department/facility/doctor relationships, or source/status combinations. Public setters can mutate timing, scope and status without transition checks (`LichHen.java:67-104`). These are domain invariant gaps, not approved booking policy.
- **[FACT]** No explicit reschedule/update operation or appointment-specific application command was found. **[ACCEPTANCE GATE]** Define guarded transitions and atomically persist changes with version/current-state checks; test valid/invalid transitions, concurrent edits, cancellation reason requirements and rollback behavior using synthetic data.

### Duplicate booking and overbooking

- **[FACT]** The repository count contract accepts doctor and a time window only, while the V1 table has no slot uniqueness constraint (`ILichHenRepository.java:6-9`; `V1__Initial_Clinic_Schema.sql:294-324`).
- **[INFERENCE]** A separate count-then-insert can race; no atomic slot reservation, conflict resource, capacity or overlap semantics are evident. A doctor-only count may not honor facility/department or room-specific schedules and cannot determine which statuses release capacity.
- **[HISTORICAL OWNER DECISION — core resource, interval, and overbooking choices superseded by RECON-02]** Define what consumes capacity (doctor, room, or both), appointment duration/overlap boundaries, whether capacity may exceed one, and which statuses/soft-deleted appointments count. Do not assume one appointment per doctor/time slot.
- **[ACCEPTANCE GATE]** Add deterministic concurrent booking tests proving that simultaneous requests cannot exceed the approved capacity, including overlapping intervals, cancellation/reschedule races, separate facilities/departments/resources, and database uniqueness/transaction/version behavior. A preflight count without an atomic persistence guarantee is insufficient.

### Facility, department, and doctor scope

- **[APPROVED POLICY]** A session has exactly one role and one selected facility/department pair; normal reads/writes must match both IDs. No broad scope is implied (`phase2-role-permission-matrix-approval.md:10-13`; `phase2-implementation-contract.md:15-18`).
- **[APPROVED POLICY]** Session idle timeout is 15 minutes and absolute expiry is 12 hours; re-authentication retains the same account/role/scope, and sensitive operations must validate the active session (`phase2-session-implementation-contract.md:1-23`; `phase2-reception-write-authorization-contract.md:9-16`). Appointment workflows must use this session contract, not invent an appointment-specific timeout or context.
- **[FACT]** `LichHen.maKhoa` is nullable and there is no room property, although `maCoSo` and doctor are stored (`LichHen.java:14-18`, `:30-33`; V1 `:298-324`).
- **[INFERENCE]** A missing department must not become an all-department query or bypass the exact-pair policy. Appointment lookup/booking needs a trusted session context and target-pair validation at the Application boundary, with doctor assignment and facility/department relationship checked. Reception cancel/transfer policy is not permission for appointment cancellation, editing, transfer, or reading.
- **[ACCEPTANCE GATE]** Prove allow only for the exact active session pair and deny wrong department, wrong facility, inactive/mismatched doctor, missing scope, and stale/revoked session for every appointment read and write. Keep patient-facing access default-deny unless separately approved and guarded.

### Check-in, no-show, and appointment-to-visit conversion

- **[FACT]** Appointment status has `DA_DEN` but no no-show value; `LuotKham` has a nullable appointment FK, with no conversion method in either domain entity (`TrangThaiLichHen.java:3-9`; `LichHen.java:50-104`; `LuotKham.java:28-31`, `:121-122`).
- **[FACT]** The ERD intends at most one visit per appointment, but V1 permits multiple visit rows to reference one appointment because `maLichHen` is only an FK (`ERD_DIAGRAM.md:129-140`; `V1__Initial_Clinic_Schema.sql:329-380`).
- **[OWNER DECISION]** Define check-in window/late-arrival behavior, no-show eligibility/timing/reversal, and whether an appointment may produce a visit after cancellation or no-show. These must not be inferred from `DA_DEN` or visit queue status.
- **[ACCEPTANCE GATE]** Implement conversion as one transaction with an idempotency key/unique appointment-to-visit invariant and stable replay result. Test repeated requests, concurrent conversion, rollback between visit creation and appointment transition, and cross-process readback. Do not treat an FK or UI click suppression as idempotency.

### Time-zone handling

- **[APPROVED POLICY]** `DEC-TIME-01` says clinic business time uses `Asia/Ho_Chi_Minh`; technical/audit timestamps compared across systems use UTC. `DATETIME2` has no timezone and adapters must interpret it consistently (`phase2-implementation-contract.md:5-18`).
- **[FACT]** Appointment business times and payment time are `LocalDateTime`; V1 uses `DATETIME2`; no time-zone identifier or offset is stored (`LichHen.java:18-19`, `:26`; `V1__Initial_Clinic_Schema.sql:302-310`). `CoSoThucThe` and `TruuTuongMienNghiepVuSuKien` use `LocalDateTime.now()` (`CoSoThucThe.java:30-32`, `:74-84`; `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/events/TruuTuongMienNghiepVuSuKien.java:3-13`).
- **[INFERENCE]** The current appointment source does not establish UTC metadata or the approved clinic zone; JVM/server local-zone defaults can therefore produce inconsistent interpretation. Do not silently convert legacy/local values.
- **[ACCEPTANCE GATE]** Test persistence/reload and event/audit timestamps under a non-UTC JVM default; assert business appointment instants round-trip as `Asia/Ho_Chi_Minh` and technical metadata is UTC, using an injectable clock and explicit conversion at adapter boundaries.

### Reminders, notification contracts, and external integration

- **[FACT]** The notification port exposes three void calls; the booked event carries appointment/patient/doctor IDs and a local start time (`IThongBaoDichVu.java:2-5`; `LichHenDaDatSuKien.java:3-20`). No event dispatch/handler or provider implementation uses them.
- **[OWNER DECISION]** Decide whether reminders are allowed, recipient/contact eligibility and consent, timing, channels, content/privacy limits, failure/retry behavior, and whether cancellation notices are required. Provider/vendor selection belongs to a separately approved integration contract.
- **[INFERENCE]** Do not send confirmation, reminder, or cancellation messages until the policy, guarded outbox/deduplication behavior, privacy review, and integration are approved. Do not place patient identifiers or narrative in logs or notification payloads beyond an approved minimum.
- **[ACCEPTANCE GATE]** Add contract tests for consent/default-deny, recipient/scope, duplicate event/retry, cancellation/reschedule races, delivery failure, and safe payload redaction. No external delivery is enabled by this review.

### Payment and downpayment

- **[APPROVED POLICY]** Phase 2 approval explicitly says appointment deposit collection has no approved actor/flow and remains `DENY` (`phase2-role-permission-matrix-approval.md:14`, `:18-20`; `phase2-implementation-contract.md:6-18`).
- **[FACT]** The entity has a `100000` in-memory default while V1 has a `0` SQL default; `xacNhanThuTienMat` marks payment confirmed from a receipt string and actor ID without verifying a receipt, payment result, or authorization (`LichHen.java:23-27`, `:50-57`; V1 `:307-312`). A gateway contract exists but has no adapter/use case (`IThanhToanCongKetNoiDichVu.java:3-7`).
- **[INFERENCE]** These domain defaults and methods do not approve or implement a deposit policy. No deposit collection, cash confirmation, online gateway, callback processing, or payment URL may be exposed by `APPT-01` until explicit owner policy and `SEC-01` acceptance.
- **[OWNER DECISION]** If deposits are desired, define whether required, amount/default/waiver/refund rules, authorized role and separation of duties, receipt linkage, gateway/callback validation, reconciliation and idempotency. Until then keep payment/downpayment unimplemented and default-deny.
- **[ACCEPTANCE GATE]** Test verified provider callbacks, signature/amount/appointment matching, replay/idempotency, actor/receipt audit, transaction failure and no false `DA_XAC_NHAN`; no live gateway test in this task.

### Audit and privacy

- **[APPROVED POLICY]** Sensitive mutation and audit write share a transaction; audit failure rolls back the operation. Audit is append-only, no automatic purge absent a retention decision, `AUDIT_READ` is denied, and audit/logs must not contain full identifiers or clinical narrative (`phase2-implementation-contract.md:15-18`). For protected reads, only return data after independent successful-read audit commit; audit failure is fail-closed and metadata excludes patient identifiers/narrative (`phase2-read-audit-decision.md:5-20`).
- **[APPROVED POLICY]** Phase 2's reception cancellation/transfer decisions apply to `LuotKham`, not appointments: only the defined receptionist may cancel an eligible `CHO_KHAM` visit with confirmation/reason/audit; transfer is a narrowly guarded same-facility workflow that does not grant destination reads (`phase2-role-permission-matrix-approval.md:10-14`). They do not decide appointment actors or appointment cancellation rules.
- **[FACT]** `LichHen` has inherited audit fields but no appointment-specific guard, audit action or application audit transaction was found (`CoSoThucThe.java:16-33`; appointment symbol search; `LichHen.java:50-65`). The booked event carries a patient ID (`LichHenDaDatSuKien.java:4-20`).
- **[INFERENCE]** Appointment schedule and patient linkage are sensitive. Reads and mutations must be default-deny, exact-pair scoped, audited under approved access/audit contracts, and fail closed on audit failure. Audit/event metadata must minimize patient data; no `AUDIT_READ` screen or permission can be inferred.
- **[ACCEPTANCE GATE]** Test exact actor/role/scope/action/resource/correlation metadata, append-only behavior, mutation rollback on audit failure, audited reads before response, no cross-scope data leakage, and redaction of patient identifiers/contact details/reasons/narratives.

## UI and print entry points

| Entry point | What source shows—and does not show |
|---|---|
| Appointment calendar/booking screen | **[FACT]** No dedicated appointment calendar or booking form was found in the desktop sources; the subsystem backlog itself says there is no independent appointment screen and calls out a calendar plus create/reschedule/cancel/conflict handling as future work (`docs/ke-hoach-trien-khai/phan-he/04-lich-hen-kham-emr.md:7-20`). |
| Queue context menu | **[FACT]** `BenhNhanHangDoiNguCanhMenu` displays “Đặt Hẹn” (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/menus/BenhNhanHangDoiNguCanhMenu.java:77-78`) and dispatches menu text to its consumer (`:100-148`). `KhamBenhManHinh` routes the action to `handlePatientQueueAction`; the appointment label falls through to a generic “performing function” dialog rather than an appointment use case (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/KhamBenhManHinh.java:1053-1056`, `:1059-1112`). |
| Follow-up appointment print preview | **[FACT]** `DieuTriXuTriTheManHinh.openFollowUpAppointmentDialog()` opens a `TheoTiepLichHenPrintHopThoai`; it supplies formatted date/time strings rather than creating or loading a `LichHen` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/examination/components/tabs/DieuTriXuTriTheManHinh.java:957-984`). UI standards describe this as a follow-up appointment print preview, not a booking workflow (`UI_CODING_STANDARDS.md:337-341`). |
| DOCX appointment slip | **[FACT]** `DOCXExportDichVu.buildAppointmentSlipDocx(patient, visit, ...)` prints a follow-up date from `LuotKham.ngayHenTaiKham` (or its own fallback), not a persisted `LichHen` (`clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/docx/DOCXExportDichVu.java:160-177`). No call site was found. |
| No-show/check-in UI | **[FACT]** No appointment-specific check-in/no-show UI was found. The existing reception queue/read workflow is not an appointment check-in workflow (`docs/ke-hoach-trien-khai/02-cac-dot-trien-khai.md:45-51`; `docs/ke-hoach-trien-khai/phan-he/04-lich-hen-kham-emr.md:7-20`). |

**[INFERENCE]** Print previews and generic menu dispatch can produce a slip/placeholder without a persisted appointment. They must not be treated as evidence of booking, check-in, reminder or conversion, or made to expose patient data outside approved access controls.

## Historical owner-decision ledger — original review snapshot

The six entries below are preserved from the original review and are not the current ledger. Entry 1's action mapping and entry 3's core capacity/resource/overbooking choices were resolved by `APPT-OWNER-DECISION-RECON-02`; remaining details not decided there stay open as summarized in the current-status addendum. The already-approved session, scope, audit, visit-cancel and visit-transfer decisions are not reopened here.

1. **HISTORICAL — action mapping resolved by RECON-02. Action semantics, not role invention:** Approved generic grants exist for `APPOINTMENT:XEM/TAO/SUA` (DOCTOR and RECEPTIONIST) and `XEM` (CASHIER), all within the approved exact-pair scope. Owner must clarify which specific lifecycle operations those generic actions cover—especially cancel, confirm, check-in, no-show, and conversion—and their guards. No patient-facing role/access is approved; keep patient self-service default-deny/unimplemented. Do not add or broaden grants.
2. **Booking policy:** Required fields, valid duration/hours, scheduling/resource assignment, reschedule limits, cancellation cutoff/reason/refund behavior, and whether a department/room is mandatory.
3. **HISTORICAL — core capacity basis resolved by RECON-02; status/soft-delete/release details remain open. Capacity/conflict policy:** Capacity resource(s), slot boundaries/overlap semantics, statuses occupying capacity, waitlist/overbooking exceptions, and the role authorized to approve any exception.
4. **Attendance/conversion policy:** Check-in timing/late arrival, no-show timing/reversal, handling of cancelled/no-show appointments, and whether/when a booking can be converted to a visit.
5. **Reminder/notification policy:** Whether delivery is allowed; channels, consent/recipient, timing, approved content, retention, retries and external provider.
6. **Deposit/payment policy:** Whether deposits exist at all and, if so, amount/default, actor, cash/refund/waiver process, gateway/callback requirements and receipt/reconciliation. Current approval says DENY; no default amount is authorized.

## Concrete technical acceptance gates before any APPT implementation

1. **Security first:** Complete and accept `SEC-01` per roadmap before opening appointment or other sensitive workflows. UI visibility is not an authorization guard. The implementation contract states this gate is still incomplete (`phase2-implementation-contract.md:37-53`; `02-cac-dot-trien-khai.md:34-42`; `03-model-task-board.md:57`).
2. **Contract/model alignment:** Resolve the approved policies above; reconcile `NguonLich` enum versus the string field, the `100000` versus SQL `0` deposit defaults, nullable department, absent room/no-show/cancellation metadata, and ERD one-to-zero/one conversion versus non-unique SQL FK. Do not change schema or defaults without the approved contract.
3. **Guarded lifecycle:** Domain/Application tests for valid and invalid booking, reschedule, cancel, confirm, check-in, no-show and conversion transitions, with scope, role, current status, actor and reason enforced at the Application boundary.
4. **Persistence and concurrency:** Adapter plus synthetic database tests for create/update/readback, overlapping/concurrent booking under approved capacity, stale-version writes, cancellation/reschedule race, atomic audit/write, and repeat/concurrent appointment-to-visit conversion. No production DB/data.
5. **Timezone:** Deterministic clock tests and adapter round-trip prove clinic business times are `Asia/Ho_Chi_Minh` and technical timestamps UTC despite non-UTC JVM defaults; no implicit legacy conversion.
6. **Privacy/audit:** Allow/deny matrix for exact facility+department and role, protected read-audit-before-return, mutation/audit atomicity, fail-closed behavior, no `AUDIT_READ`, and synthetic fixtures with no real patient data.
7. **UI and integrations:** Headless plus desktop acceptance for appointment calendar/form/actions and print workflow wired to persisted use cases; notification is outbox/idempotent and consent-aware; external delivery/payment remains off until its contract and independent acceptance are approved.

## Guidance and sources read

- Orchestrator-supplied `docs/PROJECT_CONVENTIONS.md` guidance (the repository copy is absent), root `README.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, and `UI_CODING_STANDARDS.md`.
- `docs/ke-hoach-trien-khai/README.md`, `02-cac-dot-trien-khai.md`, `03-model-task-board.md`, and `phan-he/04-lich-hen-kham-emr.md`.
- Phase 2 contracts/policies: `phase2-implementation-contract.md`, `phase2-role-permission-matrix-approval.md`, `phase2-session-implementation-contract.md`, `phase2-reception-write-authorization-contract.md`, and `phase2-read-audit-decision.md`.
- Relevant source files and test listed by file-and-line evidence above; no database or real patient data accessed.
