# PAT-02 — Doctor UI preservation and read-wiring plan

**Status:** Review and future-slice blueprint only. Wiring is not authorized by this document.
**Baseline:** `6aabb85` (main UI), restored into the current branch at `982c1f6`; current worktree `HEAD` is `eb52b71`.
**Scope:** Preserve the Doctor station’s existing presentation while planning a separately owner-approved, read-only queue and history integration.

## 1. Baseline evidence and source-of-truth

The Doctor module paths at current `HEAD` compare identical to `6aabb85`. The pre-restore tree at `50c53ce` differs substantially from that baseline across the panel and its components. Commit `50c53ce` itself changes only `BacSiTramManHinh.java` (session invalidation and worker-generation handling); it did not itself edit the four component classes. The UI regressions were accumulated in the pre-restore tree and were removed when `982c1f6` restored the Doctor module from main.

There are two important mismatches between the task’s examples and the source:

* `BacSiHanhDongThanh` contains **nine** buttons: seven clinical/workflow actions (`Làm mới`, `Tìm Kiếm`, `Bệnh Án`, `Dịch Vụ`, `Thuốc/VT`, `Phác Đồ ĐT`, `Thanh Toán`) plus `Menu` and `In`. Thus “7 buttons” accurately describes the seven workflow actions only. Preservation means retaining the complete original nine-button bar, in order, not reducing it to seven.
* Baseline `BacSiTienSuManHinh` has **eight** columns; the pre-restore component had **two** (`Loại tiền sử`, `Nội dung`), not seven. Keep the eight-column baseline schema exactly.

The pre-restore `BacSiGhepNoiGoc.java` existed at `50c53ce`, but `982c1f6` deleted it. At commit `eb52b71`, non-UI `BacSiGhepNoiGoc.java` and its dedicated test `BacSiGhepNoiGocTest.java` (**2/2 PASS**) have been restored into the codebase. In addition, `BacSiTramGiaoDienBaoToanTest.java` (**3/3 PASS**) was added at commit `f04315d` (alongside `TiepDonDuocBaoVeReadKetNoiTest` **1/1 PASS**) to programmatically verify and enforce the UI preservation invariants, while `BacSiTramManHinh` and its 4 UI components (`BacSiHanhDongThanh`, `BacSiLamSangBieuMau`, `BacSiTienSuManHinh`, `BacSiHangDoiThanhBen`) remain 100% byte-identical to `6aabb85`. No `DoctorStationController.java` exists in the current tree; future wiring will introduce and bind the controller to these components once owner-approved.

## 2. UI invariants to preserve from `6aabb85`

Wiring may replace demo values with real, authorized values and show existing-area loading/empty/error states, but must not change component hierarchy, dimensions, layout managers, control inventory/order, table schema, or tab structure. “250 px” below is the original initial divider location; Swing may allow user resizing at runtime.

### Main panel and split geometry

* `BacSiTramManHinh`: white `BorderLayout`; `BacSiHanhDongThanh` remains at `BorderLayout.NORTH`.
* Center is a horizontal `JSplitPane`, initial divider location **250**, divider size **4**, and no split-pane border. The existing queue sidebar remains left; the right work panel remains right.
* Right work panel contains a `JTabbedPane` with the original five tabs and labels/order: `Bệnh Nhân`, `Công Khám(1)`, `Thuốc(2)`, `Xử Trí`, `Phản Hồi`.
* The `Bệnh Nhân` tab remains a white `BorderLayout(0, 4)` panel with `EmptyBorder(8, 12, 8, 12)`, `BacSiLamSangBieuMau` in `NORTH`, and `BacSiTienSuManHinh` in `CENTER`. Do not add a nested tab pane to the history panel.

### Action bar

Retain the `BorderLayout` action bar, its top summary/banner and all **nine** original buttons in this exact order: `Menu`, `In`, `Làm mới`, `Tìm Kiếm`, `Bệnh Án`, `Dịch Vụ`, `Thuốc/VT`, `Phác Đồ ĐT`, `Thanh Toán`. Preserve labels, icons, print highlight, spacing, fonts, banner lines, and the BHYT badge/rate. The seven workflow buttons must not be mistaken for the total button count.

### Clinical form

Retain the existing white `GridBagLayout`, six grid columns, insets, label/value styling, row spacing, and all **11 rows**:

1. Patient code, patient name, gender.
2. Date of birth, birth year, occupation.
3. Ethnicity, nationality, telephone.
4. Address, workplace.
5. Contact person, reception time, reception type.
6. Patient category, insurance number, insurance rate.
7. Initial registered facility, insurance validity, insurance route.
8. Medical-record number, admission number, treatment code.
9. Department/room, treating doctor, medical-record category.
10. Admission date, discharge date, diagnosis.
11. Comorbidities, disposition.

The row helpers’ three-field/two-field arrangements and current field positions are part of the layout contract. Updating values must not add, remove, reorder, or relabel fields.

### History table and container

Retain the title and bordered `BorderLayout` history panel, read-only `DefaultTableModel`, `CoKieuBang.create(...)`, font, row height **24**, and scroll pane preferred size **500 × 160**. Keep exactly these eight columns and order:

1. `Ngày Vào`
2. `Hình Thức Vào`
3. `Khoa`
4. `Phòng`
5. `Ngày Ra`
6. `Xử Trí`
7. `Dịch`
8. `Chẩn Đoán`

The `JTabbedPane` is the parent work area in `BacSiTramManHinh`; `BacSiTienSuManHinh` itself is not a tabbed pane. Replacing the demo rows is allowed only as data replacement, not as a reason to change this table, its headers, its containing tab, or geometry.

## 3. What regressed in the pre-restore tree and why it was reverted

The side-by-side comparison of `6aabb85` and `50c53ce` shows these user-visible regressions:

| Area | `6aabb85` baseline | Pre-restore tree at `50c53ce` | Regression |
|---|---|---|---|
| Split pane | Initial divider at 250 | Initial divider at 280 | Sidebar widened and the right pane narrowed. |
| Action bar | Nine original icon-and-label buttons plus patient/BHYT banner | Replaced by a selected-patient text summary and one `Làm mới hàng đợi` button | Removed eight original controls, icons, banner lines and insurance-rate display. |
| Clinical form | 11 rows of patient, insurance, visit and clinical fields | Reduced to six summary labels (patient, code, visit, queue number, room, status) | Removed the detailed form and its original visual hierarchy. |
| History panel | Eight-column read-only `CoKieuBang` of encounter-history presentation | Two-column plain `JTable` for `Loại tiền sử` and `Nội dung`, with different title, row height and status/loading UI | Changed the table’s count, semantics, styling and sizing. |
| Queue sidebar | Existing filter, two-column table and footer selectors | New title/date, three-column queue table and loading/status elements | Changed sidebar controls and presentation. |
| Tabs | Five named tabs with numbered `Công Khám(1)` and `Thuốc(2)` labels | Five tabs with changed labels and wording | Changed tab labels and presentation semantics. |

`982c1f6` is the restoration point: it removed the pre-restore composition root and restored `BacSiTramManHinh`, `BacSiHanhDongThanh`, `BacSiLamSangBieuMau`, `BacSiTienSuManHinh`, `BacSiHangDoiThanhBen`, and the demo `BenhNhanKhamDuLieu` from main. The source at current `HEAD` is again identical to `6aabb85` for the Doctor module. Do not attribute all of the regressions to the single `50c53ce` commit: that commit’s own patch is panel worker/session hardening; the regressions are in its pre-restore tree relative to the main UI baseline.

## 4. Owner-approved surgical wiring blueprint

This sequence is a future implementation plan, not authorization to change Java code. All steps must preserve the invariants above and keep database access behind application handlers. No Swing event handler may call JDBC or a repository directly.

1. **Approve source and controller ownership first.** Reconcile the current tree with historical wiring: non-UI `BacSiGhepNoiGoc.java` and `BacSiGhepNoiGocTest.java` (**2/2 PASS**) have now been restored at commit `eb52b71`, constructing guarded `GetBacSiHangDoiHandler` and `GetBenhNhanTienSuHandler` instances without database connections. No `DoctorStationController.java` exists yet in the current tree. Owner approval will govern when and how a dedicated controller is introduced and connected to `BacSiTramManHinh`.
2. **Compose the existing protected reads.** Follow the existing `clinic-application` contracts: `GetBacSiHangDoiHandler` with `GetBacSiHangDoiQuery` for the active scoped doctor queue, and `GetBenhNhanTienSuHandler` with `GetBenhNhanTienSuQuery` for patient-level `TienSuBenh`. Build the handlers at the approved desktop composition boundary; inject query functions/ports into the controller. UI code receives no repository, `Connection`, or JDBC provider.
3. **Load the queue read-only and off the EDT.** On panel activation/refresh, capture the authenticated `PhanQuyenNguCanh` and matching `KiemToanTacNhanNguCanh`, validate the actor/scope, and request the clinic business date in `Asia/Ho_Chi_Minh`. Invoke the queue handler in a `SwingWorker` (or established equivalent), not on the EDT. Render only returned `TiepDonHangDoiMatHang` values in the existing queue table; preserve its component geometry and controls. Treat denied, failed, null, or invalid results as unavailable—never substitute fabricated patients.
4. **Bind selection to the exact returned item.** Selection should retain the selected `TiepDonHangDoiMatHang` (including patient and visit identifiers), not infer identity from display text or row index. On selection, update the existing banner/form values through a view mapping, clear prior history immediately, and request history only for the selected `patientId` under the same authorization/audit scope. A null selection clears the patient-dependent values/data without altering their layout.
5. **Gate the history projection on a truthful data contract.** `GetBenhNhanTienSuHandler` returns patient-level `TienSuBenh` entries (`LoaiTienSu` and `MoTa`), while the preserved eight columns describe visit/encounter history (`Ngày Vào`, admission type, department, room, discharge, disposition, service, diagnosis). Those are not equivalent data shapes. Before implementing projection, the Owner must approve a field-to-column mapping backed by actual source data, or approve using/adding the correct read query for encounter history. Keep the exact existing eight headers and table; do not invent dates, visits, facilities, rooms, procedures, or diagnoses, and do not silently relabel the panel. Until the mismatch is resolved, do not claim that patient-history rows are valid encounter-history rows.
6. **Keep asynchronous results current.** Use monotonically increasing request epochs/tokens. Advance the queue epoch on refresh, scope/session change, deactivation/clear; advance the history epoch on patient selection, clear, and scope/session change. Each worker captures its epoch plus the relevant authorization/session identity and patient/visit identity. In `done()` on the EDT, publish only if the worker is not cancelled, its epoch still matches, the session/actor still matches, and— for history— the same patient remains selected. Cancellation is advisory; the epoch/identity checks are the correctness guard. Do not let an older history result overwrite a newer selection.
7. **Surface safe errors with correlation.** Keep loading/empty/unavailable feedback within the existing queue/history display areas; do not resize or replace the components. For a failed `KetQua`, show a concise safe message and stable error code, plus the operation’s existing `KiemToanTacNhanNguCanh.correlationId` so support can find diagnostics. Never display raw exception text, SQL, stack traces, or sensitive patient data in an error dialog. If no correlation ID is available, show the generic safe message and code without inventing an ID. Authorization denial should be handled as a safe unavailable/denied state, not as success or as a data fallback.
8. **Verify the preservation contract before merge.** Add/adjust focused synthetic-data tests for query mapping, result invalidation (stale queue/history workers and changed patient/session), safe error content, and the eight-column model/header contract. Baseline UI preservation tests are now active: `BacSiTramGiaoDienBaoToanTest.java` (**3/3 PASS**) was introduced at commit `f04315d` (alongside `TiepDonDuocBaoVeReadKetNoiTest` **1/1 PASS**), programmatically asserting the 8-column read-only history table schema, the 9 action buttons in exact sequence, and the horizontal split geometry (divider 250 px, size 4 px) with 5 tabs. Non-UI wiring is verified by `BacSiGhepNoiGocTest.java` (**2/2 PASS**) restored at `eb52b71`. Reject any diff that alters component bounds/layout, divider default, button count/order, form fields, table columns, or tab structure, ensuring `BacSiTramManHinh` and its 4 UI components stay 100% byte-identical to `6aabb85`. Run only isolated tests; do not connect to `localhost:1433` or `clinicDb`.

## 5. Evidence and limits of this review

Evidence inspected: current Doctor module versus `6aabb85`; `50c53ce` Doctor-module patch and full pre-restore tree comparison; `982c1f6` restore diff; historical `BacSiGhepNoiGoc`; current `GetBacSiHangDoiHandler`/query/`TiepDonHangDoiMatHang` and `GetBenhNhanTienSuHandler`/query; current Doctor UI components; and existing Doctor history contract and visual smoke runbook.

This change is documentation only. No Java files were edited, no tests/build were run, and no database or external system was contacted.

## 6. Wave 11 Verification Evidence

During Wave 11 (`eb52b71`), the Doctor module preservation and composition contracts were verified headlessly within the full 472-test Maven reactor build:

* **Reactor Test Suite Execution:** 472 tests executed across all modules (**472/472 PASS**, 0 failures, 0 errors, 0 skipped at `eb52b71`).
* **Non-UI Composition Root Restoration (`eb52b71`):**
  - Source: `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/doctor/BacSiGhepNoiGoc.java`.
  - Test Suite: `BacSiGhepNoiGocTest.java` (**2/2 PASS**).
  - Verified behaviors:
    1. Composes guarded and audited `GetBacSiHangDoiHandler` and `GetBenhNhanTienSuHandler` application handlers via `PhienVongDoiDichVu` without establishing live database connections or accessing `clinicDb`.
    2. Enforces strict null-safety checks rejecting missing dependencies.
* **UI Preservation Test Suite (`f04315d`):**
  - Test Suite: `BacSiTramGiaoDienBaoToanTest.java` (**3/3 PASS**).
  - Verified UI invariants:
    1. `historyTableKeepsOriginalReadOnlyEightColumnSchema`: Verifies the single `JTable` in `BacSiTienSuManHinh` preserves all 8 exact column headers (`Ngày Vào`, `Hình Thức Vào`, `Khoa`, `Phòng`, `Ngày Ra`, `Xử Trí`, `Dịch`, `Chẩn Đoán`) and all cells remain non-editable.
    2. `actionBarKeepsAllOriginalButtonsInOrder`: Verifies all 9 buttons (`Menu`, `In`, `Làm mới`, `Tìm Kiếm`, `Bệnh Án`, `Dịch Vụ`, `Thuốc/VT`, `Phác Đồ ĐT`, `Thanh Toán`) retain their exact labels and order on `BacSiHanhDongThanh`.
    3. `stationKeepsOriginalHorizontalSplitGeometryAndFiveTabs`: Verifies a single `JSplitPane` with `HORIZONTAL_SPLIT`, initial divider location `250`, divider size `4`, and `JTabbedPane` with all 5 original tabs (`Bệnh Nhân`, `Công Khám(1)`, `Thuốc(2)`, `Xử Trí`, `Phản Hồi`).
  - Added alongside `TiepDonDuocBaoVeReadKetNoiTest.java` (**1/1 PASS** at `f04315d`), verifying protected application handler read routing.
* **Byte-level UI Preservation:**
  - `BacSiTramManHinh.java` and its 4 UI components (`BacSiHanhDongThanh.java`, `BacSiLamSangBieuMau.java`, `BacSiTienSuManHinh.java`, `BacSiHangDoiThanhBen.java`) remain **100% byte-identical** to baseline commit `6aabb85`.
* **Database Isolation:** All headless tests run entirely with synthetic fixtures and in-memory test doubles; zero network traffic or JDBC connection to `localhost:1433` (`clinicDb`).

