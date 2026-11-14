# Phase 1 — Doctor UI Visual Smoke Runbook (Synthetic Data)

**Document Status:** Manual visual smoke verification runbook for synthetic test data on Owner-designated local `clinicDb`.

**Execution Disclaimer:** This document provides operational verification instructions for an operator. It does not claim that the smoke test has been executed in this environment. Headless verification (`mvn -Djava.awt.headless=true clean verify`) runs off-screen and does not constitute visual Swing UI observation ([`docs/OPERATIONS_RUNBOOK.md#L27,L89-91`](../OPERATIONS_RUNBOOK.md#L27), [`docs/ke-hoach-trien-khai/phase1-tiensuben-scope-contract.md#L5-L6,L30,L53`](phase1-tiensuben-scope-contract.md#L5)). Record actual observations in the [SEC-01 baseline checklist](phase2-sec01-baseline-acceptance.md) when checking security behavior.

**Safety & Data Boundaries:**
- **Manual Visual Smoke Target:** Under approved Owner contract `18847587-233a-4f02-99cd-13e1eaf0736a`, manual visual smoke testing is authorized to use the Owner-designated local database `clinicDb` at `localhost:1433`.
- **Automated Test Prohibition:** Automated unit/integration tests and CI/CD pipelines (codex-local, copilot-cli, antigravity-cli) are STRICTLY PROHIBITED from connecting to or modifying `clinicDb`. Automated tests remain in-memory or restricted to opt-in `clinic_test_*` / `clinic_orchestrator_review_*` synthetic databases with explicit `clinicDb` denial guards ([`phase0-employee-sqlserver-local-test-path-review.md#L256-L265`](phase0-employee-sqlserver-local-test-path-review.md#L256)).
- **Synthetic Fixtures Only:** Synthetic data only. Never use real patient data or production databases ([Operations runbook](../OPERATIONS_RUNBOOK.md#3-database-configuration-and-secret-handling)).
- **Database Immutability:** Do not create, drop, or truncate `clinicDb` or pre-existing databases. Do not run migrations during smoke testing ([`docs/ke-hoach-trien-khai/phase1-tiensuben-scope-contract.md#L35,L53`](phase1-tiensuben-scope-contract.md#L35)).
- **Credential Protection:** Never inspect, expose, or commit real credentials or secrets ([Operations runbook](../OPERATIONS_RUNBOOK.md#3-database-configuration-and-secret-handling)).

---

## 1. Scope, Purpose, and Governing Contracts

This runbook defines the step-by-step manual smoke verification procedure for the Doctor Station user interface ([`BacSiTramManHinh`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java)) within the Clinic Desktop application.

### Capabilities to verify
1. **Launch & Authentication Prerequisites:** Building the desktop classes, configuring `DB_URL` pointing explicitly to `clinicDb` on `localhost:1433`, launching via `run-app.sh`, and establishing an authenticated `DOCTOR` session bound to an exact synthetic facility and department pair.
2. **Today's Scoped Team Queue:** Verifying the department-level team queue displays only active encounters for the current business date and exact session scope, constrained strictly to the three approved active statuses: `CHO_KHAM`, `DANG_KHAM`, and `CHO_CLS`.
3. **Queue Invariant & Ordering Checks:** Verifying exclusion of cancelled visits (`DA_HUY`), completed visits (`DA_KHAM`, `KET_LUAN`, `HOAN_THANH`), visits from other dates/departments, and soft-deleted records.
4. **Patient Selection & Bound ID Handling:** Selecting queue rows to bind persisted IDs (`patientId`, `visitId`) without text parsing or DTO inference.
5. **Independently Authorized & Audited Patient History Read:** Invoking asynchronous `GetBenhNhanTienSuQuery` off the Event Dispatch Thread (EDT) under role `DOCTOR` and permission `PATIENT:XEM`, ensuring independent commit of audit event `PATIENT_VIEW` before history rendering.
6. **Clinical Category Invariants & Empty State:** Rendering only approved history categories (`BENH_NEN`, `DI_UNG`, `PHAU_THUAT`) matching exact session ownership scope; presenting verified empty-state messaging when no records exist without synthesizing clinical data from visits.
7. **Error Handling & Race Condition Prevention:** Confirming immediate UI clearing and generic error display on query or audit failure; validating that rapid queue selection changes discard stale asynchronous responses.
8. **Read-Only UI Enforcement:** Confirming non-editable tables and complete absence of create/update/delete mutation controls pending encounter-locking policy resolution.
9. **Session Lock & Logout Clearing:** Validating synchronous clearing of protected queue and history data on session lock (idle/absolute) and complete disposal on logout.

### Governing Documents & References
- **Owner Separation Contract:** Approved contract `18847587-233a-4f02-99cd-13e1eaf0736a` (Owner-approved clinicDb runtime and synthetic-only worker test separation; [`docs/ke-hoach-trien-khai/phase0-employee-sqlserver-local-test-path-review.md#L256-L265`](phase0-employee-sqlserver-local-test-path-review.md#L256))
- **Task Contract:** `Approved contract v1: PAT-DOCTOR-UI-SMOKE-RUNBOOK-01 Contract`
- **UI Integration Contract:** [`docs/ke-hoach-trien-khai/phase1-doctor-history-ui-contract.md`](phase1-doctor-history-ui-contract.md)
- **Scope Contract:** [`docs/ke-hoach-trien-khai/phase1-tiensuben-scope-contract.md`](phase1-tiensuben-scope-contract.md)
- **Operations Runbook:** [`docs/OPERATIONS_RUNBOOK.md`](../OPERATIONS_RUNBOOK.md)
- **Read Audit Decision:** [`docs/ke-hoach-trien-khai/phase2-read-audit-decision.md`](phase2-read-audit-decision.md)
- **Desktop UI Standards:** [`UI_CODING_STANDARDS.md`](../../UI_CODING_STANDARDS.md)

---

## 2. Prerequisites and Unverified Environmental Assumptions

### 2.1 Build and launcher prerequisites
- **Java Development Kit:** JDK 17 ([`pom.xml#L27-L35`](../../pom.xml#L27)).
- **Build Artifact:** Packaged desktop shaded JAR built using:
  ```bash
  mvn -pl clinic-desktop -am -DskipTests package
  ```
  Expected output artifact: `clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar` ([`README.md#L16,L26`](../../README.md#L16), [`clinic-desktop/pom.xml#L80-L100`](../../clinic-desktop/pom.xml#L80)).
- **Application Launcher:** `run-app.sh` starts the desktop JVM with the compiled module classes and inherits the launch shell configuration.
- **Permission Grants:** Default seed includes grants `DOCTOR:RECEPTION:XEM` and `DOCTOR:PATIENT:XEM` ([`V8__Seed_Approved_Permission_Grants.sql#L19-L28`](../../clinic-infrastructure/src/main/resources/migrations/V8__Seed_Approved_Permission_Grants.sql#L19)).

### 2.2 Unverified Environmental Prerequisites (Must Be Provided Out-of-Band)
The operator confirms the database, credentials and synthetic records locally before starting.

1. **Target Local Database (`clinicDb` @ `localhost:1433`):**
   - Under approved Owner contract `18847587-233a-4f02-99cd-13e1eaf0736a`, manual visual smoke testing uses the Owner-designated local database `clinicDb` at `localhost:1433`.
   - **Automated tests and CI/CD remain strictly prohibited from accessing `clinicDb`.** Use in-memory tests by default. Select an explicit `clinic_test_*` or `clinic_orchestrator_review_*` catalog for SQL opt-in fixtures, subject to each fixture’s target guard.
   - Database connection parameters must be explicitly configured by the operator via environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` or `DB_CONNECTION_STRING`) using a protected local secret store or terminal session without committing or echoing values ([`README.md#L28-L41`](../../README.md#L28), [`docs/OPERATIONS_RUNBOOK.md#L39-51`](../OPERATIONS_RUNBOOK.md#L39)).
   - **One-off Local TLS Exception Notice:**
     > [!WARNING]
     > If using `TrustServerCertificate=true` for local development connections, this is strictly a **one-off local development/manual test Owner exception** authorized under contract `18847587-233a-4f02-99cd-13e1eaf0736a`.
     > It is **NEVER** an approved committed configuration, worker test profile setting, or deployment configuration.
     > Strict deployment TLS validation requirements (encrypted transport with validated certificate and server identity under Owner Decision `PHASE0-DB-DEPLOY-DECISION-RECON-02`) remain in full force.

2. **Read-Only V1–V11 Migration Metadata Preflight (DO NOT RUN MIGRATIONS):**
   - The Owner reports that migrations V1 through V11 are already applied on `clinicDb`. This is an **external assertion** and must be treated as unverified until checked.
   - Prior to launching the UI, the operator must execute a **read-only** check against the migration history table:
     ```sql
     SELECT DB_NAME() AS database_name;
     SELECT script_name, success
     FROM dbo.__schema_migrations
     ORDER BY id;
     ```
   - **Mismatched Migration Stop Rule:** Proceed only if the catalog is `clinicDb` and every script registered by the current runner for V1 through V11 is recorded with `success = 1`. [Migration runner](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunner.java)
   - **DO NOT RUN MIGRATIONS:** Do not run `CoSoDuLieuDiCuChay` or apply migration scripts during this smoke procedure. Any required migration must be executed through a separately authorized migration procedure.

3. **Authorized Synthetic DOCTOR Account:**
   - A dedicated synthetic staff account (`NhanVien`) in `clinicDb` with:
     - Active status (`trangThai = 'HOAT_DONG'`, `isDeleted = 0`).
     - Assigned role `DOCTOR` (`vaiTro = 'DOCTOR'`).
     - Explicit work assignment (`NhanVienCoSo` / `NhanVienKhoa`) to a designated synthetic facility (e.g. `maCoSo = 1` or test `11`) and department (e.g. `maKhoa = 1` or test `21`).
   - Credentials must be verified out-of-band by the operator without recording secrets.

4. **Synthetic Test Data Fixtures in `clinicDb`:**
   Use the following synthetic records for the verification steps:
   - **Queue Records for Today (`ngayKham = CURRENT_DATE` in Asia/Ho_Chi_Minh):**
     - Visit A: Status `CHO_KHAM`, registered in target facility/department.
     - Visit B: Status `DANG_KHAM`, registered in target facility/department.
     - Visit C: Status `CHO_CLS`, registered in target facility/department.
   - **Queue Negative/Exclusion Records (Must NOT appear):**
     - Visit D: Status `DA_HUY` (Cancelled) for today in target facility/department.
     - Visit E: Status `DA_KHAM` (Completed) for today in target facility/department.
     - Visit F: Status `KET_LUAN` (Concluded) for today in target facility/department.
     - Visit G: Active status (`CHO_KHAM`) for yesterday or tomorrow.
     - Visit H: Active status (`CHO_KHAM`) for a different facility or department.
     - Visit I: Soft-deleted visit (`isDeleted = 1`) or visit for soft-deleted patient (`BenhNhan.isDeleted = 1`).
   - **Patient History Records (`TienSuBenh`):**
     - For Visit A's patient: At least 2 active history records with approved categories (`BENH_NEN`, `DI_UNG`, or `PHAU_THUAT`) owned by the exact target `(maCoSo, maKhoa)` pair.
     - For Visit B's patient: Zero history records (to verify the verified empty state).
     - For Visit C's patient: History records belonging to a different facility/department or with unapproved categories (to verify they are excluded from read).

---

## 3. Launch and Authentication Smoke Procedure

### Step 3.1: Configure the app shell

Use the compiled classes from section 2.1. Configure the following split settings in a shell reserved for this app session:

```bash
unset DB_CONNECTION_STRING
export DB_URL='jdbc:sqlserver://localhost:1433;databaseName=clinicDb;encrypt=true;trustServerCertificate=false'
```

Inject the SQL login through `DB_USERNAME` and `DB_PASSWORD` from your protected local settings. Enter the separate desktop DOCTOR account on the login screen. A full `DB_CONNECTION_STRING` takes precedence when present. Use a trusted server certificate for the URL above. The Owner-authorized one-off connection to the local Docker instance can use `trustServerCertificate=true` in that shell for that session. [Operations runbook](../OPERATIONS_RUNBOOK.md#3-database-configuration-and-secret-handling)

### Step 3.2: Launch the desktop

From the repository root, start the existing launcher:

```bash
./run-app.sh
```

**Expected Visual Result:**
- The Swing application initializes on the Event Dispatch Thread (EDT).
- The Login Form dialog appears titled: `"Đăng nhập - Phần Mềm Quản Lý Bệnh Viện EHC"` ([`DangNhapBieuMau.java#L54`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L54)).
- Input fields for username (`txtUsername`) and password (`txtPassword`) are displayed and focused.

### Step 3.3: Authenticate Synthetic DOCTOR Session
1. Enter the designated synthetic doctor username and password.
2. Click `"ĐĂNG NHẬP HỆ THỐNG"` ([`DangNhapBieuMau.java#L106-L115`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L106)).
3. **Role Selection Dialog:** If prompted with multiple roles, select `DOCTOR` ([`DangNhapBieuMau.java#L157-L158`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L157)).
4. **Work Scope Dialog:** If prompted with multiple scopes, select the exact target synthetic `(maCoSo, maKhoa)` pair ([`DangNhapBieuMau.java#L159-L160`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L159)).

**Expected Functional Result:**
- `CompleteDangNhapPhienHandler` writes audit event `USER_LOGIN` and returns `DaHoanTatDangNhapPhien` ([`DangNhapBieuMau.java#L185-L189`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L185)).
- `PhienVongDoiDichVu` activates with state `ACTIVE`, binding employee, role `DOCTOR`, and exact facility/department scope ([`DangNhapBieuMau.java#L197-L200`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L197), [`PhienVongDoiDichVu.java#L158-L185`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L158)).
- The `DangNhapBieuMau` closes and the `MainBangDieuKhienCuaSo` appears ([`MainBangDieuKhienCuaSo.java#L64-L153`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L64)).

### Step 3.4: Navigate to Doctor Station
1. In the top ribbon navigation bar of `MainBangDieuKhienCuaSo`, locate the `"Điều Trị"` (👨‍⚕️) module button ([`MainBangDieuKhienCuaSo.java#L162-L163`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L162)).
2. Click `"Điều Trị"`.

**Expected Visual Result:**
- The workspace opens `BacSiTramManHinh` ([`BacSiTramManHinh.java#L60-L102`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L60)).
- Top action bar displays: `"Chưa chọn người bệnh."` and a `"Làm mới hàng đợi"` button ([`BacSiHanhDongThanh.java#L13-L28`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java#L13)).
- Left sidebar displays the queue pane with title `"Hàng đợi · <current-date-dd/MM/yyyy>"` ([`BacSiHangDoiThanhBen.java#L36`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L36)).
- Center tabbed pane opens tab `"Bệnh nhân"` with empty clinical summary fields and history status label `"Chưa có dữ liệu tiền sử bệnh đã xác thực."` ([`BacSiTienSuManHinh.java#L14-L16`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L14)).

---

## 4. Today's Team Queue & Status Filtering Smoke Verification

### Step 4.1: Verify Queue Scope and Active Statuses
1. Observe the left sidebar table in `BacSiHangDoiThanhBen` ([`BacSiHangDoiThanhBen.java#L41-L67`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L41)).
2. Confirm the header reflects today's date formatted as `dd/MM/yyyy` according to `Asia/Ho_Chi_Minh` business time ([`BacSiTramManHinh.java#L149`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L149), [`BacSiHangDoiThanhBen.java#L19-L36`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L19)).
3. Verify that the table lists only encounters having one of the **three approved active statuses**:
   - `CHO_KHAM` (Chờ khám)
   - `DANG_KHAM` (Đang khám)
   - `CHO_CLS` (Chờ cận lâm sàng)
   *(Reference: SQL predicate in [`TiepDonHangDoiRepository.java#L164`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/TiepDonHangDoiRepository.java#L164): `AND l.trangThai IN ('CHO_KHAM','DANG_KHAM','CHO_CLS')`; Owner decision in [`phase1-doctor-history-ui-contract.md#L49`](phase1-doctor-history-ui-contract.md#L49)).*

### Step 4.2: Verify Queue Exclusions & Ordering
1. **Exclusion Check:** Confirm that none of the following appear in the queue:
   - Cancelled visits (`DA_HUY`).
   - Completed / closed visits (`DA_KHAM`, `KET_LUAN`, `HOAN_THANH`).
   - Visits from previous or future dates.
   - Visits belonging to a different facility or department.
   - Soft-deleted encounters (`LuotKham.isDeleted = 1`) or soft-deleted patients (`BenhNhan.isDeleted = 1`).
2. **Team Queue Scope:** Confirm all examination rooms within the session's department appear (not filtered by individual doctor ID, as `maBacSi` is not assigned at reception; [`TiepDonHangDoiRepository.java#L158-L166`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/TiepDonHangDoiRepository.java#L158), [`phase1-doctor-history-ui-contract.md#L26,L49`](phase1-doctor-history-ui-contract.md#L26)).
3. **Ordering Check:** Confirm rows are sorted in order: priority first (`dnVnTien DESC`), queue number (`soThuTu`), and visit ID (`id`) ([`TiepDonHangDoiRepository.java#L165`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/TiepDonHangDoiRepository.java#L165)).
4. **Footer Status:** Confirm the bottom status label shows `"<count> lượt đang hoạt động"` (or `"Không có lượt khám đang hoạt động."` if queue is empty; [`BacSiHangDoiThanhBen.java#L135`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L135)).
5. **Refresh Action:** Click `"Làm mới hàng đợi"` in `BacSiHanhDongThanh` ([`BacSiHanhDongThanh.java#L14,L42-L47`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java#L14)). Verify the table refreshes smoothly without UI stutter, executing `GetBacSiHangDoiQuery` on a background thread off the EDT ([`BacSiTramManHinh.java#L152-L160`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L152)).

---

## 5. Patient Selection & Separately Audited History Read Verification

### Step 5.1: Select Patient Row
1. Click on the queue row corresponding to Visit A (patient with recorded history).
2. **Immediate Synchronous UI Update:**
   - Selection extracts the bound `TiepDonHangDoiMatHang` containing immutable `patientId` and `visitId` directly from the model (never parsed from formatted display text; [`BacSiHangDoiThanhBen.java#L100-L103`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L100), [`BacSiTramManHinh.java#L198-L202`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L198)).
   - The top `BacSiHanhDongThanh` updates immediately with: `<patientName> · <patientCode> · Lượt khám <visitId> · <queueNumber> · <status>` ([`BacSiHanhDongThanh.java#L36-L40`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java#L36)).
   - `BacSiLamSangBieuMau` displays patient name, patient code, visit ID, queue number, room name, and status ([`BacSiLamSangBieuMau.java#L36-L47`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorClinicalForm.java#L36)).
   - `BacSiTienSuManHinh` synchronously clears any previously displayed history before the async query starts ([`BacSiTramManHinh.java#L199`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L199)).

### Step 5.2: Asynchronous History Query & Independent Audit
1. `BacSiTramManHinh` executes `GetBenhNhanTienSuQuery` on a worker thread via `SwingWorker.doInBackground()` ([`BacSiTramManHinh.java#L213-L221`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L213)).
2. **Authorization & Audit Gate Verification:**
   - History query requires role `DOCTOR` and permission `PATIENT:XEM` against current exact `(facilityId, departmentId)` ([`GetBenhNhanTienSuHandler.java#L41-L56`](../../clinic-application/src/main/java/vn/clinicmanagement/application/patienthistory/GetPatientHistoryHandler.java#L41)).
   - Query executes on `TienSuBenhRepository` matching `maBenhNhan = ?`, `maCoSo = ?`, `maKhoa = ?`, `isDeleted = 0`, and approved categories ([`TienSuBenhRepository.java#L33-L50`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/patient/TienSuBenhRepository.java#L33)).
   - Upon successful query execution, `GetBenhNhanTienSuHandler` writes an independent audit event:
     - Action: `PATIENT_VIEW`
     - Resource: `PATIENT`
     - Resource ID: `<patientId>`
     - Scope: exact session `maCoSo` and `maKhoa`, UTC timestamp, correlation UUID ([`GetBenhNhanTienSuHandler.java#L69-L78`](../../clinic-application/src/main/java/vn/clinicmanagement/application/patienthistory/GetPatientHistoryHandler.java#L69), [`phase1-tiensuben-scope-contract.md#L11,L44`](phase1-tiensuben-scope-contract.md#L11)).
   - The audit transaction commits BEFORE history data is returned to the UI layer ([`phase2-read-audit-decision.md#L8-L10`](phase2-read-audit-decision.md#L8)).

### Step 5.3: Verify History Display & Approved Categories
1. In `BacSiTienSuManHinh`, observe the rendered history table ([`BacSiTienSuManHinh.java#L60-L70`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L60)).
2. Confirm the table has two columns: `"Loại tiền sử"` and `"Nội dung"` ([`BacSiTienSuManHinh.java#L17-L18`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L17)).
3. Confirm that all displayed rows belong strictly to the approved categories:
   - `BENH_NEN` (Bệnh nền)
   - `DI_UNG` (Dị ứng)
   - `PHAU_THUAT` (Phẫu thuật)
   *(Reference: Client validation in [`BacSiTramManHinh.java#L305-L319`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L305); Domain enum in [`LoaiTienSu.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/LoaiTienSu.java)).*
4. Confirm no legacy null-scope records or records owned by other facilities/departments appear.

---

## 6. Empty State, Error Handling, and Rapid Selection Switch Verification

### Step 6.1: Verify Patient with No History (Empty State)
1. In the queue table, select Visit B (patient with zero history records).
2. Observe the center `BacSiTienSuManHinh`.
3. **Expected Visual Result:**
   - The history table is removed/hidden.
   - The panel displays the verified empty-state label: `"Chưa có dữ liệu tiền sử bệnh đã xác thực."` ([`BacSiTienSuManHinh.java#L14,L56-L58`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L14)).
   - **Crucial Invariant Check:** No diagnosis from the current visit (`LuotKham`), admission date, or placeholder DTO text is synthesized or fabricated into the history area ([`DoctorHistoryPanelTest.java#L35-L42`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanelTest.java#L35), [`phase1-doctor-history-ui-contract.md#L10,L37`](phase1-doctor-history-ui-contract.md#L10)).

### Step 6.2: Verify Query Failure / Authorization Denial Handling
Use an existing authorized denial scenario on `clinicDb` if one is available. Perform network fault injection, permission mutations or database audit failure simulation only in a separately isolated environment. Mark an unavailable scenario as not run.

**Expected Visual Result:**
   - History table is cleared immediately; no stale history from the previous patient remains visible ([`BacSiTramManHinh.java#L243-L248`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L243), [`DoctorStationPanelTest.java#L112-L122`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L112)).
   - History status label displays generic message: `"Không thể tải tiền sử bệnh."` ([`BacSiTienSuManHinh.java#L72-L75`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L72)).
   - No sensitive database exception messages, stack traces, or raw error codes are displayed to the user ([`DoctorStationPanelTest.java#L120-L121`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L120)).

### Step 6.3: Verify Rapid Selection Switching (Race Condition Prevention)
1. Rapidly click Visit A and immediately click Visit B before Visit A's background query finishes.
2. Observe the final rendered state in `BacSiTienSuManHinh` and `BacSiLamSangBieuMau`.
3. **Expected Visual Result:**
   - Each selection increments internal `requestGeneration` ([`BacSiTramManHinh.java#L197`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L197)).
   - When Visit A's worker completes, it detects that `generation != requestGeneration` or `selectedItem.patientId() != item.patientId()` and cleanly discards the response ([`BacSiTramManHinh.java#L225-L229`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L225)).
   - Only Visit B's data is displayed. Visit A's history never overwrites Visit B's empty or current state ([`DoctorStationPanelTest.java#L125-L154`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L125)).

### Step 6.4: Verify Queue Deselection
1. Deselect the currently selected row (e.g. Ctrl+Click on the selected table row).
2. **Expected Visual Result:**
   - `BacSiHanhDongThanh` resets to: `"Chưa chọn người bệnh."` ([`BacSiHanhDongThanh.java#L33`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java#L33)).
   - `BacSiLamSangBieuMau` clears all fields ([`BacSiLamSangBieuMau.java#L49-L56`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorClinicalForm.java#L49)).
   - `BacSiTienSuManHinh` clears table and resets to `"Chưa có dữ liệu tiền sử bệnh đã xác thực."` ([`BacSiTienSuManHinh.java#L77-L80`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L77), [`DoctorStationPanelTest.java#L85-L87`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L85)).

---

## 7. Read-Only Controls & Clinical Boundary Verification

### Step 7.1: Verify Non-Editable History & Queue Tables
1. Double click on cells in the `BacSiHangDoiThanhBen` table. Confirm cells cannot be edited ([`BacSiHangDoiThanhBen.java#L43-L45`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L43)).
2. Select Visit A to display history rows.
3. Double click on `"Loại tiền sử"` or `"Nội dung"` cells in the `BacSiTienSuManHinh` table.
4. **Expected Visual Result:** Cells cannot be edited in place ([`BacSiTienSuManHinh.java#L20-L22`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L20), [`DoctorHistoryPanelTest.java#L28-L29`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanelTest.java#L28)).

### Step 7.2: Verify Absence of Mutation Controls
1. Inspect `BacSiTienSuManHinh`, `BacSiLamSangBieuMau`, and `BacSiHanhDongThanh`.
2. **Expected Visual Result:**
   - There are NO create, update, delete, save, or unlock buttons (e.g. no buttons matching "Thêm", "Lưu", "Sửa", "Xóa", "Mở khóa", "Unlock", "Edit", "Delete"; [`DoctorHistoryPanelTest.java#L85-L93`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanelTest.java#L85)).
   - The station is strictly read-only pending server-side encounter-lock and visit-binding policy approval ([`phase1-doctor-history-ui-contract.md#L36,L53,L59`](phase1-doctor-history-ui-contract.md#L36)).

### Step 7.3: Inspect Unwired Sub-Tabs
1. Click the remaining tabs in the center tabbed pane:
   - `"Công khám"`
   - `"Thuốc"`
   - `"Xử trí"`
   - `"Phản hồi"`
2. **Expected Visual Result:**
   - Each tab displays an explicit, centered placeholder label:
     - `"Chưa có dữ liệu công khám được nối."`
     - `"Chưa có dữ liệu đơn thuốc được nối."`
     - `"Chưa có dữ liệu xử trí được nối."`
     - `"Chưa có dữ liệu cận lâm sàng được nối."`
   - No mock or fake clinical data is fabricated ([`BacSiTramManHinh.java#L89-L96,L321-L327`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L89)).

---

## 8. Session Lock & Logout Stale-Data Clearing Verification

### Step 8.1: Verify Immediate Clearing on Session Lock
1. With Visit A selected (queue and history rows displayed on screen), trigger a session lock condition:
   - Allow the configured idle timeout to elapse.
2. When `PhienVongDoiDichVu` transitions to `LOCKED_IDLE` or `LOCKED_ABSOLUTE` ([`PhienVongDoiDichVu.java#L254-L270`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L254)):
   - `MainBangDieuKhienCuaSo.showSessionLock()` is invoked on the EDT ([`MainBangDieuKhienCuaSo.java#L204-L209`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L204)).
   - It synchronously calls `doctorStation.clearSensitiveData()` ([`MainBangDieuKhienCuaSo.java#L211-L215`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L211), [`BacSiTramManHinh.java#L183-L190`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L183)).
3. **Expected Visual Result:**
   - The full-screen `PhienKhoaLopPhu` covers the application window ([`MainBangDieuKhienCuaSo.java#L206-L208`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L206)).
   - Behind the overlay:
     - Queue rows are completely cleared (`queueSidebar.clearQueue()`; [`BacSiTramManHinh.java#L280`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L280)).
     - History rows are completely cleared (`historyPanel.clearHistory()`; [`BacSiTramManHinh.java#L279`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L279)).
     - Clinical form and action bar are reset to unselected state ([`BacSiTramManHinh.java#L277-L278`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L277)).
     - No patient details or history are visible on the locked screen.

### Step 8.2: Re-authenticate from Lock
1. In the `PhienKhoaLopPhu`, enter the doctor account password and submit ([`MainBangDieuKhienCuaSo.java#L223-L257`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L223)).
2. **Expected Visual Result:**
   - Upon successful re-authentication, the lock overlay disappears.
   - `BacSiTramManHinh`'s session monitor (`sessionMonitor`) detects the active session and automatically triggers `refreshData()` ([`BacSiTramManHinh.java#L253-L263`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L253)).
   - Fresh queue data for today's date is reloaded.
   - Patient details and history remain unselected until a queue row is explicitly chosen.

### Step 8.3: Verify Logout Cleanup and Stale Response Discard
1. In `MainBangDieuKhienCuaSo`, click the `"Kết Thúc"` (🚪) button in the ribbon or trigger logout ([`MainBangDieuKhienCuaSo.java#L185-L189,L260-L271`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L185)).
2. Confirm the logout prompt dialog.
3. **Expected Visual & Functional Result:**
   - `stopDoctorSessionMonitoring()` calls `doctorStation.onHide()`, which stops the session monitor, sets `observedSession = null`, and clears all protected data ([`MainBangDieuKhienCuaSo.java#L267`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L267), [`BacSiTramManHinh.java#L116-L124`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L116)).
   - `sessionLifecycle.clear()` is called, invalidating the session tokens ([`MainBangDieuKhienCuaSo.java#L268`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L268)).
   - `MainBangDieuKhienCuaSo` is disposed and closed.
   - The application returns to a clean `DangNhapBieuMau` ([`MainBangDieuKhienCuaSo.java#L270`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L270)).
   - **Post-Logout Async Guard:** If any asynchronous query finishes after logout, `done()` verifies `!isCurrentSession(authorization)` and discards the payload, preventing any data leak into subsequent screens or sessions ([`BacSiTramManHinh.java#L165-L168,L230-L233`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L165), [`DoctorStationPanelTest.java#L156-L182`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L156)).

---

## 9. Optional audit metadata read-back

An authorized operator may use read-only access to `NhatKyHeThong` in local `clinicDb` to confirm audit metadata for the synthetic actor, exact facility and department, and the time window of the manual session. Check the queue-read event and the `PATIENT_VIEW` event for the synthetic patient. Record only whether the expected audit metadata was present. Keep audit payloads and patient identifiers out of the smoke report.

---

## 10. Manual Smoke Verification Checklist Matrix

The operator records pass/fail observations against this matrix during test execution:

| Step # | Smoke Verification Item | Expected Observation / Invariant | Exact Source Citation | Pass / Fail |
|---|---|---|---|---|
| **1** | Application Launch | `DangNhapBieuMau` displays on EDT; database connects to local `clinicDb` via explicit environment variables without hardcoded secrets | [`Main.java#L14-L24`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/Main.java#L14); [`DangNhapBieuMau.java#L54`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L54) | [ ] |
| **2** | Doctor Authentication | Authenticates synthetic DOCTOR with exact `(maCoSo, maKhoa)` scope; opens `MainBangDieuKhienCuaSo` | [`DangNhapBieuMau.java#L157-L200`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L157) | [ ] |
| **3** | Station Navigation | Ribbon button `"Điều Trị"` opens `BacSiTramManHinh` with queue sidebar, action bar, and tabs | [`MainBangDieuKhienCuaSo.java#L162-L163`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L162); [`BacSiTramManHinh.java#L60-L102`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L60) | [ ] |
| **4** | Queue Scope & Date | Header shows today's date (`dd/MM/yyyy`); queue loads department team encounters off EDT | [`BacSiHangDoiThanhBen.java#L36,L150`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L36); [`BacSiTramManHinh.java#L149-L160`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L149) | [ ] |
| **5** | Three Active Statuses | Queue contains strictly `CHO_KHAM`, `DANG_KHAM`, `CHO_CLS`; excludes `DA_HUY`, completed, other dates/departments | [`TiepDonHangDoiRepository.java#L163-L165`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/TiepDonHangDoiRepository.java#L163) | [ ] |
| **6** | Patient Selection | Selecting row extracts bound `patientId`/`visitId`; updates action bar and clinical form | [`BacSiHangDoiThanhBen.java#L100-L103`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L100); [`BacSiLamSangBieuMau.java#L36-L47`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorClinicalForm.java#L36) | [ ] |
| **7** | History Clear on Switch | Previous history clears as the next request starts. The panel shows `Đang tải tiền sử bệnh...` while the request is pending | [`BacSiTramManHinh.java#L199`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L199); [`DoctorHistoryPanelTest.java#L32-L34`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanelTest.java#L32) | [ ] |
| **8** | Audited History Read | Query runs under `PATIENT:XEM`; independent audit `PATIENT_VIEW` commits before rendering | [`GetBenhNhanTienSuHandler.java#L41-L78`](../../clinic-application/src/main/java/vn/clinicmanagement/application/patienthistory/GetPatientHistoryHandler.java#L41); [`phase1-tiensuben-scope-contract.md#L11`](phase1-tiensuben-scope-contract.md#L11) | [ ] |
| **9** | Approved Categories | Table displays only `BENH_NEN`, `DI_UNG`, `PHAU_THUAT` matching exact session scope | [`BacSiTramManHinh.java#L305-L319`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L305); [`BacSiTienSuManHinh.java#L60-L67`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L60) | [ ] |
| **10** | Empty History State | After a successful empty response, the panel shows `"Chưa có dữ liệu tiền sử bệnh đã xác thực."`; no synthesized visit data | [`BacSiTienSuManHinh.java#L14,L57`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L14); [`DoctorHistoryPanelTest.java#L35-L42`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanelTest.java#L35) | [ ] |
| **11** | Failure / Error State | On query or audit denial in an authorized scenario: history clears; shows generic `"Không thể tải tiền sử bệnh."` | [`BacSiTienSuManHinh.java#L72-L75`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L72); [`DoctorStationPanelTest.java#L113-L122`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L113) | [ ] |
| **12** | Rapid Selection Switch | Rapidly switching patient selection discards stale async worker responses via generation counter | [`BacSiTramManHinh.java#L225-L229`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L225); [`DoctorStationPanelTest.java#L125-L154`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L125) | [ ] |
| **13** | Read-Only Controls | Queue and history table cells are non-editable; no create, edit, delete, or save buttons exist | [`BacSiTienSuManHinh.java#L20-L22`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L20); [`DoctorHistoryPanelTest.java#L85-L93`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanelTest.java#L85) | [ ] |
| **14** | Sub-Tabs Placeholders | Tabs "Công khám", "Thuốc", "Xử trí", "Phản hồi" display explicit placeholder messages | [`BacSiTramManHinh.java#L89-L96,L321-L327`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L89) | [ ] |
| **15** | Session Lock Clearing | Session lock immediately clears queue, clinical form, and history behind lock overlay | [`MainBangDieuKhienCuaSo.java#L204-L215`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L204); [`BacSiTramManHinh.java#L183-L190`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L183) | [ ] |
| **16** | Re-authentication | Unlocking session automatically refreshes today's queue; selection remains clear | [`MainBangDieuKhienCuaSo.java#L223-L257`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L223); [`BacSiTramManHinh.java#L253-L263`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L253) | [ ] |
| **17** | Logout Disposal | Logging out stops monitor, clears all protected data, disposes frame, returns to `DangNhapBieuMau` | [`MainBangDieuKhienCuaSo.java#L260-L271`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L260); [`BacSiTramManHinh.java#L116-L124`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L116) | [ ] |
| **18** | Post-Logout Leak Guard | Late worker responses arriving after logout are discarded by `isCurrentSession()` check | [`BacSiTramManHinh.java#L165-L168,L230-L233`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L165); [`DoctorStationPanelTest.java#L156-L182`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L156) | [ ] |

### Operator Verification Log & Reporting Guidelines

When executing manual visual smoke testing, the operator records observation results in the following format:

```text
================================================================================
MANUAL VISUAL SMOKE OBSERVATION RECORD
================================================================================
Contract Reference : Owner Contract 18847587-233a-4f02-99cd-13e1eaf0736a
Target Database    : clinicDb @ localhost:1433
Build / Revision   : <commit-sha-or-build-timestamp>
Execution Date/Time: <YYYY-MM-DD HH:mm:ss Asia/Ho_Chi_Minh>
Operator Role/Scope: DOCTOR @ (maCoSo = <id>, maKhoa = <id>)
Preflight V1-V11   : [ ] PASS (V1-V11 verified read-only) / [ ] FAIL (Mismatched)
Matrix Evaluation  : Steps 1-18 Pass/Fail recorded
Summary / Outcome  : <Pass / Fail description without PII or secrets>
================================================================================
```

> [!IMPORTANT]
> **No-PII and Security Gate Enforcement Rules:**
> 1. **Zero Real Patient Data / PII:** Use synthetic fixtures only. Never record or transcribe patient names, CCCD numbers, phone numbers, addresses, clinical narrative (`moTa`), or diagnosis text.
> 2. **Zero Credentials:** Never log or record database passwords, connection string credentials, or user account passwords.
> 3. **No Premature Visual Acceptance Claim:** Do not claim visual acceptance or mark gate `SEC-01` complete/closed in runbooks, reports, or task boards based solely on documentation updates or off-screen headless test execution (`mvn clean verify`). Gate `SEC-01` remains OPEN / default-deny until the operator completes and records the baseline checklist.

---

## 11. Cleanup and Environmental Boundaries

1. **Session Termination:** Ensure the desktop application has been closed and `DangNhapBieuMau` exited.
2. **Environment Variable Cleanup:** Clear exported database credentials from the active shell:
   ```bash
   unset DB_URL DB_USERNAME DB_PASSWORD DB_CONNECTION_STRING
   ```
3. **Database Integrity Preservation:**
   - Under Owner contract `18847587-233a-4f02-99cd-13e1eaf0736a`, `clinicDb` is the Owner-designated local development database.
   - Do NOT drop, truncate, or re-initialize `clinicDb`.
   - Handle synthetic records created for the session through the approved application workflow.
4. **Automated Test / CI Boundary:**
   - Automated unit/integration tests and CI workers MUST NOT connect to or write to `clinicDb`.
   - Automated tests must continue using synthetic-only fixtures and target guards (`assertFalse("clinicDb".equalsIgnoreCase(databaseName.trim()), …)`).
5. **No Git / Remote Side Effects:** Do not commit local environment files, test logs, or connection strings. Never push, merge, or force-push branches as part of smoke testing.
