# Phase 1 — Doctor UI Visual Smoke Runbook (Synthetic Data)

**Document Status:** Manual visual smoke verification runbook for synthetic test data only.

**Execution Disclaimer:** This document provides operational verification instructions for an operator. It does not claim that the smoke test has been executed in this environment. Headless verification (`mvn -Djava.awt.headless=true clean verify`) runs off-screen and does not constitute visual Swing UI observation ([`docs/OPERATIONS_RUNBOOK.md#L27,L89-91`](../OPERATIONS_RUNBOOK.md#L27), [`docs/ke-hoach-trien-khai/phase1-tiensuben-scope-contract.md#L5-L6,L30,L53`](phase1-tiensuben-scope-contract.md#L5)).

**Safety & Data Boundaries:** Synthetic data only. Never inspect, expose, or record real credentials or secrets ([`docs/PROJECT_CONVENTIONS.md#L112-L115`](../PROJECT_CONVENTIONS.md#L112)). Never connect to production databases or `clinicDb` ([`docs/OPERATIONS_RUNBOOK.md#L16,L57-58`](../OPERATIONS_RUNBOOK.md#L16)). Do not create or drop databases as part of this runbook ([`docs/ke-hoach-trien-khai/phase1-tiensuben-scope-contract.md#L35,L53`](phase1-tiensuben-scope-contract.md#L35)).

---

## 1. Scope, Purpose, and Governing Contracts

This runbook defines the step-by-step manual smoke verification procedure for the Doctor Station user interface ([`DoctorStationPanel`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java)) within the Clinic Desktop application.

### Verified Capabilities in Scope
1. **Launch & Authentication Prerequisites:** Starting the packaged desktop application with externalized environment configuration and establishing an authenticated `DOCTOR` session bound to an exact synthetic facility and department pair.
2. **Today's Scoped Team Queue:** Verifying the department-level team queue displays only active encounters for the current business date and exact session scope, constrained strictly to the three approved active statuses: `CHO_KHAM`, `DANG_KHAM`, and `CHO_CLS`.
3. **Queue Invariant & Ordering Checks:** Verifying exclusion of cancelled visits (`DA_HUY`), completed visits (`DA_KHAM`, `KET_LUAN`, `HOAN_THANH`), visits from other dates/departments, and soft-deleted records.
4. **Patient Selection & Bound ID Handling:** Selecting queue rows to bind persisted IDs (`patientId`, `visitId`) without text parsing or DTO inference.
5. **Independently Authorized & Audited Patient History Read:** Invoking asynchronous `GetPatientHistoryQuery` off the Event Dispatch Thread (EDT) under role `DOCTOR` and permission `PATIENT:XEM`, ensuring independent commit of audit event `PATIENT_VIEW` before history rendering.
6. **Clinical Category Invariants & Empty State:** Rendering only approved history categories (`BENH_NEN`, `DI_UNG`, `PHAU_THUAT`) matching exact session ownership scope; presenting verified empty-state messaging when no records exist without synthesizing clinical data from visits.
7. **Error Handling & Race Condition Prevention:** Confirming immediate UI clearing and generic error display on query or audit failure; validating that rapid queue selection changes discard stale asynchronous responses.
8. **Read-Only UI Enforcement:** Confirming non-editable tables and complete absence of create/update/delete mutation controls pending encounter-locking policy resolution.
9. **Session Lock & Logout Clearing:** Validating synchronous clearing of protected queue and history data on session lock (idle/absolute) and complete disposal on logout.

### Governing Documents & References
- **Task Contract:** `Approved contract v1: PAT-DOCTOR-UI-SMOKE-RUNBOOK-01 Contract`
- **UI Integration Contract:** [`docs/ke-hoach-trien-khai/phase1-doctor-history-ui-contract.md`](phase1-doctor-history-ui-contract.md)
- **Scope Contract:** [`docs/ke-hoach-trien-khai/phase1-tiensuben-scope-contract.md`](phase1-tiensuben-scope-contract.md)
- **Operations Runbook:** [`docs/OPERATIONS_RUNBOOK.md`](../OPERATIONS_RUNBOOK.md)
- **Read Audit Decision:** [`docs/ke-hoach-trien-khai/phase2-read-audit-decision.md`](phase2-read-audit-decision.md)
- **Desktop UI Standards:** [`UI_CODING_STANDARDS.md`](../../UI_CODING_STANDARDS.md)

---

## 2. Prerequisites and Unverified Environmental Assumptions

### 2.1 Tracked and Verified Repository Prerequisites
- **Java Development Kit:** JDK 17 ([`pom.xml#L27-L35`](../../pom.xml#L27)).
- **Build Artifact:** Packaged desktop shaded JAR built using:
  ```bash
  mvn -pl clinic-desktop -am package
  ```
  Expected output artifact: `clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar` ([`README.md#L16,L26`](../../README.md#L16), [`clinic-desktop/pom.xml#L80-L100`](../../clinic-desktop/pom.xml#L80)).
- **Database Schema Level:** SQL Server migrations applied up to version V10 ([`V10__TienSuBenh_Scope.sql`](../../clinic-infrastructure/src/main/resources/migrations/V10__TienSuBenh_Scope.sql), [`FreshDatabaseMigrationSqlServerTest`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/FreshDatabaseMigrationSqlServerTest.java)).
- **Permission Grants:** Default seed includes grants `DOCTOR:RECEPTION:XEM` and `DOCTOR:PATIENT:XEM` ([`V8__Seed_Approved_Permission_Grants.sql#L19-L28`](../../clinic-infrastructure/src/main/resources/migrations/V8__Seed_Approved_Permission_Grants.sql#L19)).

### 2.2 Unverified Environmental Prerequisites (Must Be Provided Out-of-Band)
The following prerequisites cannot be verified from tracked files and must be confirmed by the test environment operator before executing this runbook. **Never invent credentials or inspect local secrets.**

1. **Target Synthetic Test Database:**
   - Must be an isolated, non-production SQL Server test database whose name matches `clinic_test_*` ([`docs/OPERATIONS_RUNBOOK.md#L15,L43-51`](../OPERATIONS_RUNBOOK.md#L15)).
   - Do NOT run migrations against, modify, or connect to `clinicDb` or any clinic operational environment.
   - Database connection parameters must be exported into environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` or `DB_CONNECTION_STRING`) via a secure local secret store or terminal session without committing or echoing values ([`README.md#L28-L41`](../../README.md#L28), [`docs/OPERATIONS_RUNBOOK.md#L39-51`](../OPERATIONS_RUNBOOK.md#L39)).
2. **Authorized Synthetic DOCTOR Account:**
   - A dedicated synthetic staff account (`NhanVien`) with:
     - Active status (`trangThai = 'HOAT_DONG'`, `isDeleted = 0`).
     - Assigned role `DOCTOR` (`vaiTro = 'DOCTOR'`).
     - Explicit work assignment (`NhanVienCoSo` / `NhanVienKhoa`) to a designated synthetic facility (e.g. `maCoSo = 1` or test `11`) and department (e.g. `maKhoa = 1` or test `21`).
   - Credentials must be verified out-of-band by the operator.
3. **Synthetic Test Data Fixtures in Database:**
   To thoroughly execute all verification steps, the synthetic database must contain:
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

### Step 3.1: Configure Process Environment
Set database connectivity parameters in the active shell using out-of-band synthetic values ([`docs/OPERATIONS_RUNBOOK.md#L45-51`](../OPERATIONS_RUNBOOK.md#L45)). Do not echo or log secrets.

```bash
# Illustrative template only — replace placeholders with verified test values
export DB_URL='jdbc:sqlserver://<approved-test-host>:1433;databaseName=clinic_test_<unique_suffix>;encrypt=true;<approved-options>'
export DB_USERNAME='<test-doctor-login>'
export DB_PASSWORD='<test-doctor-secret>'
```

### Step 3.2: Launch Packaged Desktop Application
Execute the desktop shaded JAR from the repository root:

```bash
java -jar clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar
```
*(Reference: [`README.md#L24-L26`](../../README.md#L24), [`docs/OPERATIONS_RUNBOOK.md#L82`](../OPERATIONS_RUNBOOK.md#L82), [`vn.clinicmanagement.desktop.Main#L14-L24`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/Main.java#L14)).*

**Expected Visual Result:**
- The Swing application initializes on the Event Dispatch Thread (EDT).
- The Login Form dialog appears titled: `"Đăng nhập - Phần Mềm Quản Lý Bệnh Viện EHC"` ([`LoginForm.java#L54`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L54)).
- Input fields for username (`txtUsername`) and password (`txtPassword`) are displayed and focused.

### Step 3.3: Authenticate Synthetic DOCTOR Session
1. Enter the designated synthetic doctor username and password.
2. Click `"ĐĂNG NHẬP HỆ THỐNG"` ([`LoginForm.java#L106-L115`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L106)).
3. **Role Selection Dialog:** If prompted with multiple roles, select `DOCTOR` ([`LoginForm.java#L157-L158`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L157)).
4. **Work Scope Dialog:** If prompted with multiple scopes, select the exact target synthetic `(maCoSo, maKhoa)` pair ([`LoginForm.java#L159-L160`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L159)).

**Expected Functional Result:**
- `CompleteLoginSessionHandler` writes audit event `USER_LOGIN` and returns `CompletedLoginSession` ([`LoginForm.java#L185-L189`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L185)).
- `SessionLifecycleService` activates with state `ACTIVE`, binding employee, role `DOCTOR`, and exact facility/department scope ([`LoginForm.java#L197-L200`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L197), [`SessionLifecycleService.java#L158-L185`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L158)).
- The `LoginForm` closes and the `MainDashboardFrame` appears ([`MainDashboardFrame.java#L64-L153`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L64)).

### Step 3.4: Navigate to Doctor Station
1. In the top ribbon navigation bar of `MainDashboardFrame`, locate the `"Điều Trị"` (👨‍⚕️) module button ([`MainDashboardFrame.java#L162-L163`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L162)).
2. Click `"Điều Trị"`.

**Expected Visual Result:**
- The workspace opens `DoctorStationPanel` ([`DoctorStationPanel.java#L60-L102`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L60)).
- Top action bar displays: `"Chưa chọn người bệnh."` and a `"Làm mới hàng đợi"` button ([`DoctorActionBar.java#L13-L28`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java#L13)).
- Left sidebar displays the queue pane with title `"Hàng đợi · <current-date-dd/MM/yyyy>"` ([`DoctorQueueSidebar.java#L36`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L36)).
- Center tabbed pane opens tab `"Bệnh nhân"` with empty clinical summary fields and history status label `"Chưa có dữ liệu tiền sử bệnh đã xác thực."` ([`DoctorHistoryPanel.java#L14-L16`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L14)).

---

## 4. Today's Team Queue & Status Filtering Smoke Verification

### Step 4.1: Verify Queue Scope and Active Statuses
1. Observe the left sidebar table in `DoctorQueueSidebar` ([`DoctorQueueSidebar.java#L41-L67`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L41)).
2. Confirm the header reflects today's date formatted as `dd/MM/yyyy` according to `Asia/Ho_Chi_Minh` business time ([`DoctorStationPanel.java#L149`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L149), [`DoctorQueueSidebar.java#L19-L36`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L19)).
3. Verify that the table lists only encounters having one of the **three approved active statuses**:
   - `CHO_KHAM` (Chờ khám)
   - `DANG_KHAM` (Đang khám)
   - `CHO_CLS` (Chờ cận lâm sàng)
   *(Reference: SQL predicate in [`ReceptionQueueRepositoryImpl.java#L164`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L164): `AND l.trangThai IN ('CHO_KHAM','DANG_KHAM','CHO_CLS')`; Owner decision in [`phase1-doctor-history-ui-contract.md#L49`](phase1-doctor-history-ui-contract.md#L49)).*

### Step 4.2: Verify Queue Exclusions & Ordering
1. **Exclusion Check:** Confirm that none of the following appear in the queue:
   - Cancelled visits (`DA_HUY`).
   - Completed / closed visits (`DA_KHAM`, `KET_LUAN`, `HOAN_THANH`).
   - Visits from previous or future dates.
   - Visits belonging to a different facility or department.
   - Soft-deleted encounters (`LuotKham.isDeleted = 1`) or soft-deleted patients (`BenhNhan.isDeleted = 1`).
2. **Team Queue Scope:** Confirm all examination rooms within the session's department appear (not filtered by individual doctor ID, as `maBacSi` is not assigned at reception; [`ReceptionQueueRepositoryImpl.java#L158-L166`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L158), [`phase1-doctor-history-ui-contract.md#L26,L49`](phase1-doctor-history-ui-contract.md#L26)).
3. **Ordering Check:** Confirm rows are sorted in order: priority first (`dnVnTien DESC`), queue number (`soThuTu`), and visit ID (`id`) ([`ReceptionQueueRepositoryImpl.java#L165`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L165)).
4. **Footer Status:** Confirm the bottom status label shows `"<count> lượt đang hoạt động"` (or `"Không có lượt khám đang hoạt động."` if queue is empty; [`DoctorQueueSidebar.java#L135`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L135)).
5. **Refresh Action:** Click `"Làm mới hàng đợi"` in `DoctorActionBar` ([`DoctorActionBar.java#L14,L42-L47`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java#L14)). Verify the table refreshes smoothly without UI stutter, executing `GetDoctorQueueQuery` on a background thread off the EDT ([`DoctorStationPanel.java#L152-L160`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L152)).

---

## 5. Patient Selection & Separately Audited History Read Verification

### Step 5.1: Select Patient Row
1. Click on the queue row corresponding to Visit A (patient with recorded history).
2. **Immediate Synchronous UI Update:**
   - Selection extracts the bound `ReceptionQueueItem` containing immutable `patientId` and `visitId` directly from the model (never parsed from formatted display text; [`DoctorQueueSidebar.java#L100-L103`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L100), [`DoctorStationPanel.java#L198-L202`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L198)).
   - The top `DoctorActionBar` updates immediately with: `<patientName> · <patientCode> · Lượt khám <visitId> · <queueNumber> · <status>` ([`DoctorActionBar.java#L36-L40`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java#L36)).
   - `DoctorClinicalForm` displays patient name, patient code, visit ID, queue number, room name, and status ([`DoctorClinicalForm.java#L36-L47`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorClinicalForm.java#L36)).
   - `DoctorHistoryPanel` synchronously clears any previously displayed history before the async query starts ([`DoctorStationPanel.java#L199`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L199)).

### Step 5.2: Asynchronous History Query & Independent Audit
1. `DoctorStationPanel` executes `GetPatientHistoryQuery` on a worker thread via `SwingWorker.doInBackground()` ([`DoctorStationPanel.java#L213-L221`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L213)).
2. **Authorization & Audit Gate Verification:**
   - History query requires role `DOCTOR` and permission `PATIENT:XEM` against current exact `(facilityId, departmentId)` ([`GetPatientHistoryHandler.java#L41-L56`](../../clinic-application/src/main/java/vn/clinicmanagement/application/patienthistory/GetPatientHistoryHandler.java#L41)).
   - Query executes on `TienSuBenhRepositoryImpl` matching `maBenhNhan = ?`, `maCoSo = ?`, `maKhoa = ?`, `isDeleted = 0`, and approved categories ([`TienSuBenhRepositoryImpl.java#L33-L50`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/patient/TienSuBenhRepositoryImpl.java#L33)).
   - Upon successful query execution, `GetPatientHistoryHandler` writes an independent audit event:
     - Action: `PATIENT_VIEW`
     - Resource: `PATIENT`
     - Resource ID: `<patientId>`
     - Scope: exact session `maCoSo` and `maKhoa`, UTC timestamp, correlation UUID ([`GetPatientHistoryHandler.java#L69-L78`](../../clinic-application/src/main/java/vn/clinicmanagement/application/patienthistory/GetPatientHistoryHandler.java#L69), [`phase1-tiensuben-scope-contract.md#L11,L44`](phase1-tiensuben-scope-contract.md#L11)).
   - The audit transaction commits BEFORE history data is returned to the UI layer ([`phase2-read-audit-decision.md#L8-L10`](phase2-read-audit-decision.md#L8)).

### Step 5.3: Verify History Display & Approved Categories
1. In `DoctorHistoryPanel`, observe the rendered history table ([`DoctorHistoryPanel.java#L60-L70`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L60)).
2. Confirm the table has two columns: `"Loại tiền sử"` and `"Nội dung"` ([`DoctorHistoryPanel.java#L17-L18`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L17)).
3. Confirm that all displayed rows belong strictly to the approved categories:
   - `BENH_NEN` (Bệnh nền)
   - `DI_UNG` (Dị ứng)
   - `PHAU_THUAT` (Phẫu thuật)
   *(Reference: Client validation in [`DoctorStationPanel.java#L305-L319`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L305); Domain enum in [`LoaiTienSu.java`](../../clinic-domain/src/main/java/vn/clinicmanagement/domain/common/enums/LoaiTienSu.java)).*
4. Confirm no legacy null-scope records or records owned by other facilities/departments appear.

---

## 6. Empty State, Error Handling, and Rapid Selection Switch Verification

### Step 6.1: Verify Patient with No History (Empty State)
1. In the queue table, select Visit B (patient with zero history records).
2. Observe the center `DoctorHistoryPanel`.
3. **Expected Visual Result:**
   - The history table is removed/hidden.
   - The panel displays the verified empty-state label: `"Chưa có dữ liệu tiền sử bệnh đã xác thực."` ([`DoctorHistoryPanel.java#L14,L56-L58`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L14)).
   - **Crucial Invariant Check:** No diagnosis from the current visit (`LuotKham`), admission date, or placeholder DTO text is synthesized or fabricated into the history area ([`DoctorHistoryPanelTest.java#L35-L42`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanelTest.java#L35), [`phase1-doctor-history-ui-contract.md#L10,L37`](phase1-doctor-history-ui-contract.md#L10)).

### Step 6.2: Verify Query Failure / Authorization Denial Handling
1. In a simulated failure scenario (e.g. simulated network interruption, revoking permission, or database audit failure):
2. Select a patient row.
3. **Expected Visual Result:**
   - History table is cleared immediately; no stale history from the previous patient remains visible ([`DoctorStationPanel.java#L243-L248`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L243), [`DoctorStationPanelTest.java#L112-L122`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L112)).
   - History status label displays generic message: `"Không thể tải tiền sử bệnh."` ([`DoctorHistoryPanel.java#L72-L75`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L72)).
   - No sensitive database exception messages, stack traces, or raw error codes are displayed to the user ([`DoctorStationPanelTest.java#L120-L121`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L120)).

### Step 6.3: Verify Rapid Selection Switching (Race Condition Prevention)
1. Rapidly click Visit A and immediately click Visit B before Visit A's background query finishes.
2. Observe the final rendered state in `DoctorHistoryPanel` and `DoctorClinicalForm`.
3. **Expected Visual Result:**
   - Each selection increments internal `requestGeneration` ([`DoctorStationPanel.java#L197`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L197)).
   - When Visit A's worker completes, it detects that `generation != requestGeneration` or `selectedItem.patientId() != item.patientId()` and cleanly discards the response ([`DoctorStationPanel.java#L225-L229`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L225)).
   - Only Visit B's data is displayed. Visit A's history never overwrites Visit B's empty or current state ([`DoctorStationPanelTest.java#L125-L154`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L125)).

### Step 6.4: Verify Queue Deselection
1. Deselect the currently selected row (e.g. Ctrl+Click on the selected table row).
2. **Expected Visual Result:**
   - `DoctorActionBar` resets to: `"Chưa chọn người bệnh."` ([`DoctorActionBar.java#L33`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorActionBar.java#L33)).
   - `DoctorClinicalForm` clears all fields ([`DoctorClinicalForm.java#L49-L56`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorClinicalForm.java#L49)).
   - `DoctorHistoryPanel` clears table and resets to `"Chưa có dữ liệu tiền sử bệnh đã xác thực."` ([`DoctorHistoryPanel.java#L77-L80`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L77), [`DoctorStationPanelTest.java#L85-L87`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L85)).

---

## 7. Read-Only Controls & Clinical Boundary Verification

### Step 7.1: Verify Non-Editable History & Queue Tables
1. Double click on cells in the `DoctorQueueSidebar` table. Confirm cells cannot be edited ([`DoctorQueueSidebar.java#L43-L45`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L43)).
2. Select Visit A to display history rows.
3. Double click on `"Loại tiền sử"` or `"Nội dung"` cells in the `DoctorHistoryPanel` table.
4. **Expected Visual Result:** Cells cannot be edited in place ([`DoctorHistoryPanel.java#L20-L22`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L20), [`DoctorHistoryPanelTest.java#L28-L29`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanelTest.java#L28)).

### Step 7.2: Verify Absence of Mutation Controls
1. Inspect `DoctorHistoryPanel`, `DoctorClinicalForm`, and `DoctorActionBar`.
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
   - No mock or fake clinical data is fabricated ([`DoctorStationPanel.java#L89-L96,L321-L327`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L89)).

---

## 8. Session Lock & Logout Stale-Data Clearing Verification

### Step 8.1: Verify Immediate Clearing on Session Lock
1. With Visit A selected (queue and history rows displayed on screen), trigger a session lock condition:
   - Allow idle timeout to elapse (e.g. configured idle threshold or test lock trigger).
   - Alternatively, invoke session lock in test mode.
2. When `SessionLifecycleService` transitions to `LOCKED_IDLE` or `LOCKED_ABSOLUTE` ([`SessionLifecycleService.java#L254-L270`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/SessionLifecycleService.java#L254)):
   - `MainDashboardFrame.showSessionLock()` is invoked on the EDT ([`MainDashboardFrame.java#L204-L209`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L204)).
   - It synchronously calls `doctorStation.clearSensitiveData()` ([`MainDashboardFrame.java#L211-L215`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L211), [`DoctorStationPanel.java#L183-L190`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L183)).
3. **Expected Visual Result:**
   - The full-screen `SessionLockOverlay` covers the application window ([`MainDashboardFrame.java#L206-L208`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L206)).
   - Behind the overlay:
     - Queue rows are completely cleared (`queueSidebar.clearQueue()`; [`DoctorStationPanel.java#L280`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L280)).
     - History rows are completely cleared (`historyPanel.clearHistory()`; [`DoctorStationPanel.java#L279`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L279)).
     - Clinical form and action bar are reset to unselected state ([`DoctorStationPanel.java#L277-L278`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L277)).
     - No patient data or clinical history remains in memory or on components.

### Step 8.2: Re-authenticate from Lock
1. In the `SessionLockOverlay`, enter the doctor account password and submit ([`MainDashboardFrame.java#L223-L257`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L223)).
2. **Expected Visual Result:**
   - Upon successful re-authentication, the lock overlay disappears.
   - `DoctorStationPanel`'s session monitor (`sessionMonitor`) detects the active session and automatically triggers `refreshData()` ([`DoctorStationPanel.java#L253-L263`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L253)).
   - Fresh queue data for today's date is reloaded.
   - Patient details and history remain unselected until a queue row is explicitly chosen.

### Step 8.3: Verify Logout Cleanup and Stale Response Discard
1. In `MainDashboardFrame`, click the `"Kết Thúc"` (🚪) button in the ribbon or trigger logout ([`MainDashboardFrame.java#L185-L189,L260-L271`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L185)).
2. Confirm the logout prompt dialog.
3. **Expected Visual & Functional Result:**
   - `stopDoctorSessionMonitoring()` calls `doctorStation.onHide()`, which stops the session monitor, sets `observedSession = null`, and clears all protected data ([`MainDashboardFrame.java#L267`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L267), [`DoctorStationPanel.java#L116-L124`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L116)).
   - `sessionLifecycle.clear()` is called, invalidating the session tokens ([`MainDashboardFrame.java#L268`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L268)).
   - `MainDashboardFrame` is disposed and closed.
   - The application returns to a clean `LoginForm` ([`MainDashboardFrame.java#L270`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L270)).
   - **Post-Logout Async Guard:** If any asynchronous query finishes after logout, `done()` verifies `!isCurrentSession(authorization)` and discards the payload, preventing any data leak into subsequent screens or sessions ([`DoctorStationPanel.java#L165-L168,L230-L233`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L165), [`DoctorStationPanelTest.java#L156-L182`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L156)).

---

## 9. Opt-In Database Audit Read-Back Verification (Synthetic Test DB Only)

After completing the visual steps in a controlled test environment, an operator with database read permissions may verify the written audit log in the synthetic database `clinic_test_*`. **This step is strictly opt-in and must NEVER be run against production or live patient databases.**

### Step 9.1: Verify Queue Read Audit Event
Execute query on `NhatKyHeThong`:

```sql
SELECT TOP 5
    id, hanhDong, tenBang, khoaChinh, maNhanVien, maCoSo, maKhoa, thoiDiem, chiTiet
FROM NhatKyHeThong
WHERE hanhDong = 'PATIENT_VIEW'
  AND tenBang IS NULL
ORDER BY id DESC;
```
*(Reference: [`ReceptionReadAudit.java#L34-L55`](../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/ReceptionReadAudit.java#L34), [`phase2-reception-read-authorization-contract.md#L16,L26`](phase2-reception-read-authorization-contract.md#L16)).*

**Expected Audit Evidence:**
- `hanhDong`: `'PATIENT_VIEW'`
- `tenBang`: `NULL` (queue reads audit the operation without enumerating individual patient IDs)
- `maNhanVien`: matches the synthetic doctor ID.
- `maCoSo` & `maKhoa`: match the exact synthetic session scope.
- `thoiDiem`: UTC timestamp of the queue load.

### Step 9.2: Verify Patient History Read Audit Event
Execute query on `NhatKyHeThong`:

```sql
SELECT TOP 5
    id, hanhDong, tenBang, khoaChinh, maNhanVien, maCoSo, maKhoa, thoiDiem, chiTiet
FROM NhatKyHeThong
WHERE hanhDong = 'PATIENT_VIEW'
  AND tenBang = 'PATIENT'
ORDER BY id DESC;
```
*(Reference: [`GetPatientHistoryHandler.java#L69-L78`](../../clinic-application/src/main/java/vn/clinicmanagement/application/patienthistory/GetPatientHistoryHandler.java#L69), [`phase1-tiensuben-scope-contract.md#L11,L44`](phase1-tiensuben-scope-contract.md#L11)).*

**Expected Audit Evidence:**
- `hanhDong`: `'PATIENT_VIEW'`
- `tenBang`: `'PATIENT'`
- `khoaChinh`: `<patientId>` matching the selected synthetic patient.
- `maNhanVien`: matches the synthetic doctor ID.
- `maCoSo` & `maKhoa`: match the exact session scope.
- `chiTiet`: contains actor/correlation metadata; **MUST NOT contain clinical narrative (`moTa`), diagnosis, or PII** ([`phase1-tiensuben-scope-contract.md#L11,L44`](phase1-tiensuben-scope-contract.md#L11)).

---

## 10. Manual Smoke Verification Checklist Matrix

The operator records pass/fail observations against this matrix during test execution:

| Step # | Smoke Verification Item | Expected Observation / Invariant | Exact Source Citation | Pass / Fail |
|---|---|---|---|---|
| **1** | Application Launch | `LoginForm` displays on EDT; database connects via environment variables without hardcoded secrets | [`Main.java#L14-L24`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/Main.java#L14); [`LoginForm.java#L54`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L54) | [ ] |
| **2** | Doctor Authentication | Authenticates synthetic DOCTOR with exact `(maCoSo, maKhoa)` scope; opens `MainDashboardFrame` | [`LoginForm.java#L157-L200`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java#L157) | [ ] |
| **3** | Station Navigation | Ribbon button `"Điều Trị"` opens `DoctorStationPanel` with queue sidebar, action bar, and tabs | [`MainDashboardFrame.java#L162-L163`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L162); [`DoctorStationPanel.java#L60-L102`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L60) | [ ] |
| **4** | Queue Scope & Date | Header shows today's date (`dd/MM/yyyy`); queue loads department team encounters off EDT | [`DoctorQueueSidebar.java#L36,L150`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L36); [`DoctorStationPanel.java#L149-L160`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L149) | [ ] |
| **5** | Three Active Statuses | Queue contains strictly `CHO_KHAM`, `DANG_KHAM`, `CHO_CLS`; excludes `DA_HUY`, completed, other dates/departments | [`ReceptionQueueRepositoryImpl.java#L163-L165`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/reception/ReceptionQueueRepositoryImpl.java#L163) | [ ] |
| **6** | Patient Selection | Selecting row extracts bound `patientId`/`visitId`; updates action bar and clinical form | [`DoctorQueueSidebar.java#L100-L103`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorQueueSidebar.java#L100); [`DoctorClinicalForm.java#L36-L47`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorClinicalForm.java#L36) | [ ] |
| **7** | History Clear on Switch | Previous history is immediately cleared before asynchronous query starts | [`DoctorStationPanel.java#L199`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L199); [`DoctorHistoryPanelTest.java#L32-L34`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanelTest.java#L32) | [ ] |
| **8** | Audited History Read | Query runs under `PATIENT:XEM`; independent audit `PATIENT_VIEW` commits before rendering | [`GetPatientHistoryHandler.java#L41-L78`](../../clinic-application/src/main/java/vn/clinicmanagement/application/patienthistory/GetPatientHistoryHandler.java#L41); [`phase1-tiensuben-scope-contract.md#L11`](phase1-tiensuben-scope-contract.md#L11) | [ ] |
| **9** | Approved Categories | Table displays only `BENH_NEN`, `DI_UNG`, `PHAU_THUAT` matching exact session scope | [`DoctorStationPanel.java#L305-L319`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L305); [`DoctorHistoryPanel.java#L60-L67`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L60) | [ ] |
| **10** | Empty History State | Patient without history shows `"Chưa có dữ liệu tiền sử bệnh đã xác thực."`; no synthesized visit data | [`DoctorHistoryPanel.java#L14,L57`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L14); [`DoctorHistoryPanelTest.java#L35-L42`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanelTest.java#L35) | [ ] |
| **11** | Failure / Error State | On query/audit denial or DB drop: history clears; shows generic `"Không thể tải tiền sử bệnh."` | [`DoctorHistoryPanel.java#L72-L75`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L72); [`DoctorStationPanelTest.java#L113-L122`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L113) | [ ] |
| **12** | Rapid Selection Switch | Rapidly switching patient selection discards stale async worker responses via generation counter | [`DoctorStationPanel.java#L225-L229`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L225); [`DoctorStationPanelTest.java#L125-L154`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L125) | [ ] |
| **13** | Read-Only Controls | Queue and history table cells are non-editable; no create, edit, delete, or save buttons exist | [`DoctorHistoryPanel.java#L20-L22`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanel.java#L20); [`DoctorHistoryPanelTest.java#L85-L93`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/components/DoctorHistoryPanelTest.java#L85) | [ ] |
| **14** | Sub-Tabs Placeholders | Tabs "Công khám", "Thuốc", "Xử trí", "Phản hồi" display explicit placeholder messages | [`DoctorStationPanel.java#L89-L96,L321-L327`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L89) | [ ] |
| **15** | Session Lock Clearing | Session lock immediately clears queue, clinical form, and history behind lock overlay | [`MainDashboardFrame.java#L204-L215`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L204); [`DoctorStationPanel.java#L183-L190`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L183) | [ ] |
| **16** | Re-authentication | Unlocking session automatically refreshes today's queue; selection remains clear | [`MainDashboardFrame.java#L223-L257`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L223); [`DoctorStationPanel.java#L253-L263`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L253) | [ ] |
| **17** | Logout Disposal | Logging out stops monitor, clears all protected data, disposes frame, returns to `LoginForm` | [`MainDashboardFrame.java#L260-L271`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java#L260); [`DoctorStationPanel.java#L116-L124`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L116) | [ ] |
| **18** | Post-Logout Leak Guard | Late worker responses arriving after logout are discarded by `isCurrentSession()` check | [`DoctorStationPanel.java#L165-L168,L230-L233`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanel.java#L165); [`DoctorStationPanelTest.java#L156-L182`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/doctor/DoctorStationPanelTest.java#L156) | [ ] |

---

## 11. Cleanup and Environmental Boundaries

1. **Session Termination:** Ensure the desktop application has been closed and `LoginForm` exited.
2. **Environment Variable Cleanup:** Clear exported database credentials from the active shell:
   ```bash
   unset DB_URL DB_USERNAME DB_PASSWORD DB_CONNECTION_STRING
   ```
3. **Synthetic Database Retention:** Do not drop or truncate the synthetic test database. Test fixtures created for manual smoke testing should be maintained or reset according to the operator's synthetic fixture policy.
4. **No Git / Remote Side Effects:** Do not commit local environment files, test logs, or connection strings. Never push, merge, or force-push branches as part of smoke testing.
