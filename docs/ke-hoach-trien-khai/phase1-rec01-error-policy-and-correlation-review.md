# REC-01 P2.8 — Safe error policy and correlation IDs review

**Review scope:** read-only review of the panel, message policy, focused policy tests, and the P2.8 evidence note. This is a requirement/gap review, not implementation or acceptance evidence. No database or clinicDb connection was made.

## Finding

P2.8 remains an open implementation gap. The code has a localized message policy, but panel errors do not consistently use it; read failures can be mistaken for empty data or silently ignored; and separately audited reads reuse correlation context. The existing evidence note identifies the same gaps and calls for panel-callback and per-operation correlation tests ([P2.8 evidence](phase1-rec01-desktop-evidence-review.md#L50-L52)).

| Area | Evidence and gap |
| --- | --- |
| Mapping and sanitization | `ReceptionPanelMessagePolicy` has safe messages for authorization and scope errors, but its switch maps `SCOPE_MISMATCH`/`TARGET_MISMATCH`, not `AUTH_SCOPE_MISMATCH`; unknown codes may display arbitrary details when they pass a SQL-keyword denylist. Validation details use that same denylist ([policy: constants and mappings](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanelMessagePolicy.java#L13-L32), [policy: mapping and detail handling](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanelMessagePolicy.java#L62-L112), [policy: sanitizer](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanelMessagePolicy.java#L115-L134)). Existing tests cover known mappings and SQL/exception-like validation text, but not `AUTH_SCOPE_MISMATCH`, unknown sensitive detail, or panel-level delivery ([tests](../../clinic-desktop/src/test/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanelMessagePolicyTest.java#L41-L105)). |
| Search failure handling | The worker collects only successful query results. If the combined list is empty, the callback reports “not found”; a handler denial/read-audit failure can therefore look like a successful empty search. An `ExecutionException` instead gets a fixed connection message. These paths do not distinguish full success, partial success, and failure ([panel](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L533-L595)). |
| Refresh failure handling | Queue and room tables are updated only for successful results. Failed `Result`s are ignored, and `ExecutionException` is swallowed; existing rows can remain visible without indicating the refresh failed ([panel](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L187-L234)). |
| Correlation IDs and safe wording | Refresh obtains one audit context for both queue and room reads, and search obtains one for both phone and CCCD reads; these are distinct audited operations and currently share that context ([panel](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L193-L207), [panel](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java#L546-L567)). Also, the transaction message says data was safely rolled back; that wording should only be used when rollback is known to have succeeded ([policy](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanelMessagePolicy.java#L27-L28), [policy](../../clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanelMessagePolicy.java#L87-L88)). |

## Minimal implementation checklist

- [ ] Route all result failures and worker exceptions through the message policy with operation-appropriate safe messages; map `AUTH_SCOPE_MISMATCH` to the established scope-denial message.
- [ ] Do not render arbitrary server details for unknown errors or validation. Prefer known, bounded field-validation messages; otherwise use a generic safe message. Do not include identifiers, credentials, exception text, or SQL in user-facing errors.
- [ ] Make search outcomes explicit: report “no match” only when every requested query succeeds and returns no matches; report failures separately, and identify partial results when one requested query fails and another succeeds.
- [ ] Handle queue and room refresh failures independently, including worker exceptions. Surface a safe load failure rather than silently retaining rows as if current.
- [ ] Give every independently audited handler invocation its own correlation ID (queue, room catalog, phone search, CCCD search), while preserving the authenticated actor and authorization scope. Keep correlation IDs out of user-visible detail unless an approved support-reference flow requires them.
- [ ] Replace rollback-certainty wording unless the transaction result guarantees rollback completed; report an uncertain outcome neutrally.

## Synthetic test checklist

- [ ] Policy maps `AUTH_SCOPE_MISMATCH` to the safe scope message; unknown codes with synthetic exception, SQL, credential, and identifier-like detail never expose that detail. Include safe and unsafe validation cases.
- [ ] Policy transaction wording does not claim successful rollback for an outcome that does not prove rollback.
- [ ] Panel search tests distinguish: all requested queries succeed with no matches; a denial/read-audit failure with no matches; one successful query plus one failed query; and a worker exception. Only the first case reports “no match”; partial results are marked as partial.
- [ ] Panel refresh tests inject queue failure, room failure, both failures, and worker exceptions; each produces safe feedback and does not present stale content as a successful refresh.
- [ ] Synthetic audit-context assertions verify distinct IDs for queue versus room reads and phone versus CCCD searches, while actor identity and scope remain unchanged.
- [ ] Use fakes/synthetic records only; no SQL Server or clinicDb connectivity.

**Status boundary:** This review does not claim REC-01, PAT-02, or SEC-01 closure. The checklist is proposed follow-up work, not evidence that the gaps have been fixed or accepted.
