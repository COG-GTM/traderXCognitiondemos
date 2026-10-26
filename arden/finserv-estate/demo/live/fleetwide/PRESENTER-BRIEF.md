# demo_fleetwide: fleet-wide CVE fix, one Devin per finding

**Goal:** Arden's Artifactory is hit by CVE-2026-82329 (SEC-2026-0912). Snyk reports 17 findings. Devin starts one child session per finding, each opens a tested PR, and the one risky upgrade is stopped for a human. 5 findings live, about 9 min.

**Say first:** "One parent Devin reads the scanner and hands each finding to its own Devin, like a team lead with five engineers."

## Tabs
1. [Live session](CHILD_SESSION_URL): started by the setup session; prompt 1 is already in it.
2. [Snyk snapshot (17 findings)](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/arden-live-before/arden/finserv-estate/demo/scanner/fixtures/snyk-webhook.json)
3. [Hard-case PR STPS #77, with video](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/77): always open, your default link.
4. Live PRs (appear from about 6:00): [STPS](https://github.com/COG-GTM/Securities-Trade-Processing-System/pulls?q=is%3Apr+is%3Aopen+head%3Asecurity), [traderX](https://github.com/COG-GTM/traderXCognitiondemos/pulls?q=is%3Apr+is%3Aopen+head%3Asecurity), [Springboot-BankApp](https://github.com/COG-GTM/Springboot-BankApp/pulls?q=is%3Apr+is%3Aopen+head%3Asecurity)

## Beats
1. **0:00 The ask (tab 2).** "17 findings across 10 repos. We'll do 5 live so you can watch every one land."
2. **0:10 Hand it to Devin (tab 1).** Devin reads the repo's AGENTS.md first, like a new engineer.
3. **1:45 Fan-out plan (tab 1).** One row per finding with repo, branch, owner and child-session link. Open the settlement-instruction-service child.
4. **2:00 to 7:00 The trap.** The scanner says take core-dates 1.5.0. Devin runs the custodian's contract test: FIX tag 64 turns into `2027-10-13` and every instruction would be rejected. Show the video in [#77](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/77).
5. **6:10 First PRs (tab 4).** risk-lib, then platform-terraform. Open one PR to show the test, the fix and the owner. Not open yet? Use [traderX #141](https://github.com/COG-GTM/traderXCognitiondemos/pull/141).
6. **7:00 Hard case lands.** The live settlement-instruction-service PR pins 1.4.1, adds a wire-format test and waits for @ardencm/settlements-core. Not open yet? Use [STPS #77](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/77/files).
7. **8:45 Burn-down (tab 1).** Five findings, five PRs, one call-out for the upgrade Devin refused.
- **Close.** "Five findings, five reviewable PRs, one judgement call handed to a human, in under nine minutes. Nothing merged."

## Prompts
Prompt 1 is already sent to tab 1. Optional follow-up, paste it into tab 1 after the burn-down (about 2 min):
```
Write demo/out/scanner/results.json from the children's STATUS lines in the shape demo/scanner/collect.py writes (one entry per finding, with pr_url and session_url), run demo/scanner/report.py, and attach demo/out/scanner/burndown.html and demo/reports/demo1-remediation-report.md.
```
Prompt 1 and the full-17 variant: [PROMPTS.md](https://github.com/COG-GTM/traderXCognitiondemos/blob/devin/1792725163-demo-fleetwide-plugin/arden/finserv-estate/demo/live/fleetwide/PROMPTS.md).

## Check
- [ ] 5 child sessions, 5 PRs, nothing merged.
- [ ] settlement-instruction-service: `fixed-with-deviation`, core-dates 1.4.1, not 1.5.0.
- [ ] Its PR shows 1.5.0 breaks FIX tag 64 and adds `CoreDatesWireFormatTest`.
- [ ] It waits for @ardencm/settlements-core sign-off.

**Fallback:** "Here's the run we did while preparing the demo." [Session](https://devin-gtm.devinenterprise.com/sessions/f8d2e3d9c1b44829acd2fa2d9fd95e07) and its PRs, always open: [STPS #77](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/77) (hard case), [#78](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/78), [#79](https://github.com/COG-GTM/Securities-Trade-Processing-System/pull/79), [traderX #141](https://github.com/COG-GTM/traderXCognitiondemos/pull/141), [Springboot-BankApp #339](https://github.com/COG-GTM/Springboot-BankApp/pull/339).
