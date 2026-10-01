# Arden Capital Markets — incident conventions (synthetic estate; upload as Knowledge, scope "when triaging a Datadog alert for an Arden service")

## Severity matrix
| Sev | Meaning | Examples | Who joins |
|---|---|---|---|
| 1 | Client-facing or settlement-affecting, no workaround | portal valuations down at market open; instruction generation failing | service owner team, market-ops, ops risk |
| 2 | Client-facing with workaround, or internal batch at risk of SLA | EOD pricing late but within re-run window | service owner team |
| 3 | Degraded, no client impact | elevated latency, single-host errors | on-call only |

Datadog priority P2 ≈ Sev 2 on receipt; the on-call may re-grade. Regulatory classification (DORA Art. 18) is **ops risk's** call, never the responder's.

## Escalation
- `#inc-<service>` is the incident channel; the monitor posts there. Devin posts findings in the same thread, never a new channel.
- Rollout / rollback: `release-bot` on approval from the CODEOWNERS team. Devin proposes; humans deploy.
- Vendor/feed issues: `@ardencm/market-data` owns the vendor relationship; consumer teams do not contact vendors directly.

## Investigation conventions
- Reproduce with the live input before forming a hypothesis.
- The latest deploy is a lead, not a finding: replay the pre-deploy build against the same input before blaming or clearing it.
- Prefer a 500 over a wrong number. Never coerce unknown input silently.
- Leave behind: thread summary, timeline, incident record (draft), RCA (draft). Human-owned fields are marked as such.
