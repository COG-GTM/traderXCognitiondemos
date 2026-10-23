# demo_fleetwide: talk track (live, 5-finding stage run)

**Story:** Arden's Artifactory is hit by CVE-2026-82329 (SEC-2026-0912). Snyk reports 17 findings across 10 repos in
3 GitHub repos. One Devin session reads the scanner output and starts one child Devin session per finding. Each child
fixes its finding the way that team would: baseline tests, a minimal fix, contract tests, a PR for CODEOWNERS. Devin
does not blindly take the scanner's upgrade.

**Before the meeting (2 min):** run `setup-fleetwide.sh` (or the `demo_fleetwide` skill) and keep the READY block open.
Open a new Devin session in the web app with the three COG-GTM repos.

## Timings (measured, live run on 2026-10-23, session f8d2e3d9, estate 938e38f)

All times from the moment prompt 1 is sent. The children run in parallel. The machine was prepared with a real
(non-dry) `setup-fleetwide.sh` run first (34s on a warm machine).

| Clock | What appears on screen | What the presenter says |
|---|---|---|
| 0:00 | Paste prompt 1 | "This is the Snyk webhook for the incident. 17 findings; we will do 5 now so you can watch every one land." |
| 0:07 | Devin acknowledges and starts reading AGENTS.md | "Devin reads the repo's own instructions first, the same way a new engineer would." |
| 1:27 | The 5 child sessions start, each with the remediation playbook | "One child Devin per finding. No finding is grouped with another." |
| 1:43 | **Fan-out plan**: 5 rows, each with host repo, branch, scanner suggestion, CODEOWNERS and a link to its child session | Walk the table, then open the settlement-instruction-service child. |
| 1:45 to 7:00 | In that child: Maven setup, baseline 3/3, the scanner's core-dates 1.5.0 fails `CustodyGatewayContractTest` (tag 64 becomes `2027-10-13`, the custodian expects `20271013`), then 1.4.1 | "This one is the trap. The scanner says take core-dates 1.5.0. Devin ran the custodian's contract test: 1.5.0 changes the FIX settlement-date format and the custodian would reject every instruction." |
| 6:10 | First STATUS line: risk-lib `fixed` (traderX PR) | Open it if the hard case is still working: regression test fails on the old code, passes now. |
| 7:01 | platform-terraform `fixed` (Springboot-BankApp PR) | |
| 7:03 | **Hard case lands**: `STATUS=fixed-with-deviation`, STPS PR pinning core-dates 1.4.1 plus a new `CoreDatesWireFormatTest`, sign-off requested from @ardencm/settlements-core, not merged | "Smallest version that clears the CVE without touching the wire format, a test so the next upgrade cannot break it silently, and a human signs off." |
| 7:46 to 7:50 | custody-gateway and fx-funding-service `fixed` (STPS PRs). **Last child STATUS at 7:50** | Open one PR: test evidence, residual risk, CODEOWNERS, nothing merged. |
| 8:45 | **Burn-down table** in the parent, with a call-out for the child that did not take the scanner's upgrade | "Five findings, five PRs ready for review, one judgement call surfaced to a human, under nine minutes." |

Cost: about 9.7 ACUs (parent 2.1, children 1.3 to 1.7 each).

The first stage run (session ed6ecc1b, before estate commit 938e38f) took 9m20s to the burn-down; its hard-case child
reported `false-positive` (backup beat below).

Budget **10 minutes** for the beat plus discussion; prompt 2 (dashboard) adds about 2 minutes and is optional.

## The hard case: what Devin actually says (live run)

Since estate commit 938e38f (STPS `devin/1790864747-arden-finserv-estate`), core-dates 1.4.0 embeds snakeyaml 1.33,
shaded under `com.ardencm.posttrade.dates.internal.snakeyaml`. Spring Boot's snakeyaml 2.2 cannot replace it, so the
finding is real and only a core-dates upgrade clears it. `setup-fleetwide.sh` checks this before printing READY.

In the stage run (session f8d2e3d9, child 7a244969) the hard-case child took **5m35s** from child start to STATUS
line (7:03 on the stage clock). A rerun of that child alone with the exact child prompt (session 510e80f7) took 5m54s.

```
STATUS=fixed-with-deviation PR=https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/51
TESTS="baseline 3/3 pass; attempt 1 core-dates 1.5.0: Tests run: 3, Failures: 3 (CustodyGatewayContractTest tag 64
ISO-8601); attempt 2 core-dates 1.4.1: Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS"
WHY="The scanner's core-dates 1.5.0 changes FIX tag 64/75 to ISO-8601 and breaks the custody-gateway contract, so per
ADR 0007 I pinned the 1.4.1 security patch (embedded snakeyaml 2.2, no wire change) and added CoreDatesWireFormatTest ..."
```

Open STPS PR #51 and show: the `dependencyManagement` pin to 1.4.1 with the reason in a comment, ADR 0007 quoted, the
failed 1.5.0 attempt (`tag 64 must match ^[0-9]{8}$ but was 2027-10-13`), the embedded snakeyaml now `version=2.2`,
the new `CoreDatesWireFormatTest` (it fails on 1.5.0), and "do not merge without @ardencm/settlements-core approval".

> "The scanner's upgrade would have fixed the CVE and broken settlements. Devin took the patch release instead,
> proved both with tests, and left the sign-off to the owning team."

### Backup beat: "Devin checks the scanner before trusting it"

Before the estate change, the same child (session 9b992d79, stage run ed6ecc1b) found that the resolved build
already shipped snakeyaml 2.2 and reported `STATUS=false-positive` with no PR, while still proving that 1.5.0 breaks
tag 64 and 1.4.1 passes. Use it if the customer asks how Devin handles scanner noise:

> "When the vulnerable version is not actually in the build, Devin says so and opens nothing, instead of shipping a
> risky upgrade to make a dashboard go green."

## Full 17

Not run live. The stage run cost about 9.7 ACUs (parent about 2, children 1.3 to 1.7 each) and took 8m45s. Because children
run in parallel the full 17 should land in a similar wall time if the org's concurrent-session limit allows 17 at
once (not verified), at roughly 3 times the cost. Recommendation: do not run 17 in a meeting. The 5 carry the whole
story (one finding per kind plus the hard case); show 17 as a pre-run burn-down if the customer asks for scale.

## Fallback

1. Pre-run live session (same prompt, 2026-10-23, estate 938e38f; `setup-fleetwide.sh` closes its PRs before a new
   live run, the closed PRs still show the diff and checks):
   https://devin-gtm.devinenterprise.com/sessions/f8d2e3d9c1b44829acd2fa2d9fd95e07 . Walk the fan-out plan, the
   burn-down and the PRs: STPS #51 (hard case, core-dates 1.4.1), #52 and #53, Springboot-BankApp #337, traderX #139.
   Backup beat (false positive, pre-938e38f): https://devin-gtm.devinenterprise.com/sessions/9b992d79ad124e0681c3aaf9b0769f3a
   in stage run https://devin-gtm.devinenterprise.com/sessions/ed6ecc1b1526439482afae95bae00eee .
2. Deterministic replay, no Devin run:
   `git checkout devin/1790864747-arden-finserv-estate && arden/finserv-estate/demo/run-demo1.sh`

## Not proven

- Full 17 fan-out and the org's concurrent-session limit.
- `setup-fleetwide.sh` on a cold machine (the real run was on a warm `~/.m2`; a cold reset.sh adds Maven downloads).
- Re-creating `demo/arden-live-before`: the real run kept the seed because demo_incident has an open PR on it
  (traderX #135), which `seed-situation.sh` would close.
