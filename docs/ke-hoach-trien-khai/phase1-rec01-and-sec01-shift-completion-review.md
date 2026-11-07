# REC-01 and SEC-01 Desktop Hardening Completion Review

**Review date:** 2026-11-07  
**Reviewed baseline:** `feature/patient-reception-workflow` at `e663e24`  
**Review type:** Read-only source/commit verification and focused synthetic headless tests.

This review verifies the requested REC-01 and SEC-01 hardening slices against the current source, cited commits, focused test classes, and the existing completion reviews. It does not claim Owner visual acceptance or close any gate.

## REC-01 completion evidence

| Item | Verified implementation and evidence | Commit and focused tests |
|---|---|---|
| **P0.2 — Existing patient and insurance preservation; insert audit actor; strict dates and clinic timezone** | Existing patient updates reload the in-scope patient and apply submitted fields onto that record. Insurance updates fetch the current card and apply form fields to it, preserving its record identity and fields not represented by the form (including insurance type) ([`RegisterPatientHandler`](../../clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java)). New patient/insurance inserts set `createdBy` in the same handler. Form date parsing uses strict resolution; reception timestamps and queue-date sourcing use `Asia/Ho_Chi_Minh` ([`PatientRegistrationFormPanel`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/PatientRegistrationFormPanel.java), [`ReceptionPanel`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java)). | `563f4d6` (task `1a2d14bf`): synthetic JDBC coverage in [`BaoHiemRepositoryImplTest`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/patient/BaoHiemRepositoryImplTest.java), **10/10**. `1062f79` (task `5e21267a`): `createdBy` on new patient/insurance inserts, covered by [`RegisterPatientHandlerTest`](../../clinic-application/src/test/java/vn/clinicmanagement/application/reception/RegisterPatientHandlerTest.java), **32/32**. `1235e91` (task `92f0c249`): strict form-date parsing and clinic timezone; [`PatientRegistrationFormPanelTest`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/reception/components/PatientRegistrationFormPanelTest.java), **7/7**. |
| **P1.3 — Active room identity binding** | Active room entities are passed to the form so the selected room resolves to its actual ID ([`ReceptionPanel`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java)). Duplicate room labels are disambiguated, and empty or failed catalog refreshes clear stale room identity/options rather than leaving a saveable stale selection. | `035e815` (task `bbafd718`) and `13178f9` (task `a81c2ec3`); [`ReceptionPanelActiveRoomBindingTest`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanelActiveRoomBindingTest.java), **7/7**. |
| **P1.4 / P1.7 — Form reset, shortcuts, clinic date, room summaries and failure clearing** | `ReceptionPanel.clearForm()` delegates to `formPanel.clearForm()`; footer F1/F2 actions are bound and respect saving state. Queue dates use the clinic timezone; room summaries are populated from refresh results and stale summaries are cleared after queue-refresh failure ([`ReceptionPanel`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java)). | `2efc3a6` (task `b58dc6e5`), `096e8c0` (task `940e7d44`) and `81bf3a4` (task `aa3d1f92`); [`ReceptionPanelFormResetAndSummaryTest`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanelFormResetAndSummaryTest.java), **8/8**, and [`ReceptionActionBindingsTest`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/reception/components/ReceptionActionBindingsTest.java), **2/2**. |
| **P1.5 / P1.6 — Worker/session and search/save hardening** | Refresh and search completions are guarded by `viewSessionEpoch`, `refreshSequence`, and `searchSequence`; hide/session lifecycle changes invalidate stale work. Context is sourced through `SessionLifecycleService`, not fabricated singleton defaults. Search results are capped at 50, and save locks the form while the request is in flight ([`ReceptionPanel`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java)). | `4e4035d` (task `dcfadcaf`), `1239784` (task `29019069`) and `644a904` (task `1d345a43`); [`ReceptionPanelWorkerAndSearchHardeningTest`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanelWorkerAndSearchHardeningTest.java), **12/12**. |
| **P2.8 — Safe failure messages and correlation IDs** | `AUTH_SCOPE_MISMATCH` and `OUT_OF_SCOPE` map to safe scope messaging; `isSafeMessage` applies the hardened sanitizer ([`ReceptionPanelMessagePolicy`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanelMessagePolicy.java)); separately audited operations receive distinct correlation UUIDs. | `947853d` (task `162726ca`) and `4e4035d` (task `dcfadcaf`); [`ReceptionPanelMessagePolicyTest`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanelMessagePolicyTest.java), **12/12**. |

## SEC-01 completion evidence

| Finding | Verified implementation and evidence | Commit and focused tests |
|---|---|---|
| **FINDING-SEC-01-01 — Headless LoginForm/MainDashboardFrame helpers** | Headless tests cover LoginForm credential validation, error-message selection and login/session helper behavior ([`LoginForm`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java)); dashboard helper tests cover verified user-info formatting, Ribbon state synchronization and lock-overlay visibility/input blocking ([`MainDashboardFrame`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java)). | `527ba96` (task `1fa427ea`), `074aa71` (task `c7b085c5`) and `030e043` (task `cfe04e73`); [`LoginSessionFlowTest`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/auth/LoginSessionFlowTest.java), **8/8**, and [`DashboardNavigationPolicyTest`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/dashboard/DashboardNavigationPolicyTest.java), **6/6**. |
| **FINDING-SEC-01-02 / 03 — Session mutation and user-dialog context source** | `UserSession.setStationId` and `setWorkstationId` are package-private; `MainDashboardFrame` reads user-dialog identity from the immutable `CompletedLoginSession` rather than the mutable singleton ([`UserSession`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/core/session/UserSession.java), [`MainDashboardFrame`](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/dashboard/MainDashboardFrame.java)). | `b4e29f6` (task `bea9a713`); [`UserSessionContextTest`](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/core/session/UserSessionContextTest.java), **8/8**. |

## Verification run and gate preservation

The focused Maven run also included `ReceptionCompositionRootTest` (**5/5**) and the Application registration-handler tests above. Across the selected classes, **117 tests passed** (32 Application, 10 infrastructure, 75 desktop); **0 failures, 0 errors, 0 skipped**. Fixtures were synthetic/headless, including JDBC proxies; no live database was used.

```sh
mvn -o -B -ntp -Daether.syncContext.named.factory=rwlock-local \
  -pl clinic-desktop,clinic-infrastructure -am \
  -Djava.awt.headless=true -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=BaoHiemRepositoryImplTest,PatientRegistrationFormPanelTest,RegisterPatientHandlerTest,ReceptionCompositionRootTest,ReceptionPanelActiveRoomBindingTest,ReceptionPanelFormResetAndSummaryTest,ReceptionActionBindingsTest,ReceptionPanelWorkerAndSearchHardeningTest,ReceptionPanelMessagePolicyTest,LoginSessionFlowTest,DashboardNavigationPolicyTest,UserSessionContextTest \
  test
```

**Gate decision is unchanged:** `SEC-01`, `PAT-02`, and `REC-01` remain **`[~] In progress` (default-deny)** pending Owner visual smoke acceptance on `clinicDb`. Headless tests and static review do not substitute for that acceptance. This review did not launch the application, connect to `clinicDb`, or perform live-system verification. The existing Owner smoke checklist remains unaccepted.

## Review inputs

- [`phase1-rec01-desktop-evidence-review.md`](phase1-rec01-desktop-evidence-review.md)
- [`phase2-sec01-desktop-auth-review.md`](phase2-sec01-desktop-auth-review.md)
- [`03-model-task-board.md`](03-model-task-board.md)
- [`desktop-owner-visual-smoke-checklist.md`](desktop-owner-visual-smoke-checklist.md)
