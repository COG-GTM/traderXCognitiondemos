# demo_fleetwide: talk track (live, 5-finding stage run)

**Story:** Arden's Artifactory is hit by CVE-2026-82329 (SEC-2026-0912). Snyk reports 17 findings across 10 repos in
3 GitHub repos. One Devin session reads the scanner output and starts one child Devin session per finding. Each child
fixes its finding the way that team would: baseline tests, a minimal fix, contract tests, a PR for CODEOWNERS. Devin
does not blindly take the scanner's upgrade.

**Before the meeting (2 min):** run `setup-fleetwide.sh` (or the `demo_fleetwide` skill) and keep the READY block open.
Open a new Devin session in the web app with the three COG-GTM repos.

## Timings (measured, live run on 2026-10-23, session ed6ecc1b)

All times from the moment prompt 1 is sent. The children run in parallel.

| Clock | What appears on screen | What the presenter says |
|---|---|---|
| 0:00 | Paste prompt 1 | "This is the Snyk webhook for the incident. 17 findings; we will do 5 now so you can watch every one land." |
| 0:10 | Devin acknowledges and starts reading AGENTS.md | "Devin reads the repo's own instructions first, the same way a new engineer would." |
| 2:15 | **Fan-out plan**: 5 rows, each with host repo, branch, scanner suggestion and a link to its child session | "One child Devin per finding, each with the remediation playbook. No finding is grouped with another." |
| 2:30 to 5:30 | Open the settlement-instruction-service child: Maven setup, baseline tests, then the core-dates check | "This one is the trap. The scanner says take core-dates 1.5.0." |
| 5:30 | **Hard case found** in the child: 1.5.0 fails `CustodyGatewayContractTest` (tag 64 becomes `2027-10-13`, the custodian expects `20271013`); 1.4.1 passes | "Devin ran the custodian's contract test. 1.5.0 changes the FIX settlement-date format and the custodian would reject every instruction. Devin refuses it." |
| about 8:00 | **Hard case lands**: `STATUS=fixed-with-deviation`, STPS PR pinning core-dates 1.4.1 plus a new `CoreDatesWireFormatTest`, CODEOWNERS sign-off requested, not merged | "Smallest version that clears the CVE without touching the wire format, a test so the next upgrade cannot break it silently, and a human signs off." |
| 6:45 to 8:45 | The other four STATUS lines arrive: platform-terraform (SBA PR), risk-lib (traderX PR), fx-funding-service and custody-gateway (STPS PRs) | Open one PR: test evidence, residual risk, CODEOWNERS, nothing merged. |
| 9:20 | **Burn-down table** in the parent, with a call-out for the child that did not take the scanner's upgrade | "Five findings, four PRs ready for review, one judgement call surfaced to a human, under ten minutes." |

Budget **10 minutes** for the beat plus discussion; prompt 2 (dashboard) adds about 2 minutes and is optional.

## The hard case: what Devin actually says (live run)

Since estate commit 938e38f (STPS `devin/1790864747-arden-finserv-estate`), core-dates 1.4.0 embeds snakeyaml 1.33,
shaded under `com.ardencm.posttrade.dates.internal.snakeyaml`. Spring Boot's snakeyaml 2.2 cannot replace it, so the
finding is real and only a core-dates upgrade clears it. `setup-fleetwide.sh` checks this before printing READY.

Live rerun of the hard-case child with the exact child prompt (session 510e80f7, 2026-10-23): **5m54s** from child
start to STATUS line.

```
STATUS=fixed-with-deviation PR=https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/48
TESTS="Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 (baseline 3/3; scanner's 1.5.0 attempt 3/3 failed)"
WHY="Took core-dates 1.4.1 (ADR 0007 security patch, embedded snakeyaml 2.2) instead of the scanner's 1.5.0 because
1.5.0 changes FIX tag 64/75 to ISO-8601 and breaks CustodyGatewayContractTest, and added CoreDatesWireFormatTest ..."
```

Open PR #48 and show: the `dependencyManagement` pin to 1.4.1 with the reason in a comment, ADR 0007 quoted, the
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

Not run live. The stage run cost about 11 ACUs (parent about 2, children 1 to 2 each) and took 9m20s. Because children
run in parallel the full 17 should land in a similar wall time if the org's concurrent-session limit allows 17 at
once (not verified), at roughly 3 times the cost. Recommendation: do not run 17 in a meeting. The 5 carry the whole
story (one finding per kind plus the hard case); show 17 as a pre-run burn-down if the customer asks for scale.

## Fallback

1. Pre-run live session (same prompt, 2026-10-23; `setup-fleetwide.sh` closes its PRs before a new live run, the closed PRs still show the diff and checks):
   https://devin-gtm.devinenterprise.com/sessions/ed6ecc1b1526439482afae95bae00eee . Walk the fan-out plan, the
   burn-down and the PRs: STPS #37 and #38, Springboot-BankApp #334, traderX #136; the hard-case child is
   https://devin-gtm.devinenterprise.com/sessions/9b992d79ad124e0681c3aaf9b0769f3a (backup beat). The
   fixed-with-deviation hard case is https://devin-gtm.devinenterprise.com/sessions/510e80f760594134a06ab1e4d2dcd0d2
   with STPS #48.
2. Deterministic replay, no Devin run:
   `git checkout devin/1790864747-arden-finserv-estate && arden/finserv-estate/demo/run-demo1.sh`

## Not proven

- The full 5-finding stage run on the new estate: the hard-case child was rerun alone (5m54s); the other 4 children
  are unchanged by the estate commit, so the about 8:00 hard-case row and the burn-down time are estimates.
- Full 17 fan-out and the org's concurrent-session limit.
- `setup-fleetwide.sh` closing PRs from a previous live run (dry run only so far).
