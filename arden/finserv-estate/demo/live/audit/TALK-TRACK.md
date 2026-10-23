# demo_audit: talk track (live)

**Who:** a platform or risk lead at a bank with an audit coming. **Length:** about 15 minutes, with Devin working live for about 9.

## Before the room (2 minutes)

Start a Devin session with the `FS Golden Demo: demo_audit` playbook (`!demo_audit`). It resets the demo and sends you
these 3 tabs plus the prompt:

1. [Control list](https://github.com/COG-GTM/traderXCognitiondemos/blob/demo/arden-live-before/arden/finserv-estate/demo/audit/fixtures/ccc-core-v2025.10.yaml): the 5 controls the auditor asked about.
2. [Artifactory config, line 19](https://github.com/COG-GTM/Springboot-BankApp/blob/devin/1790864747-arden-finserv-estate/arden/platform-terraform/artifactory/main.tf#L19): the open firewall rule and the 2023 exception (PLAT-771) that covers it.
3. [Devin home](https://devin-gtm.devinenterprise.com/): start a new session here at beat 2 and paste prompt 1 ([PROMPTS.md](PROMPTS.md)).

## Beats

1. **The ask (tab 1).** "Arden's auditor wants five security controls checked: proof, an owner for every gap, and a fix where it's safe. One finding has an old exception nobody remembers."
2. **Hand it to Devin (tab 3).** Paste the prompt. "I haven't told Devin what fails or where the exception is. It reads the code like a new engineer would."
3. **The results (about 9 minutes later).** "Every answer points to a file and line: one fixed, one passed, two gaps with named owners, and one left alone on purpose."
4. **The fix.** Open the PR link Devin posts. "Access tokens that never expired now expire. Devin opened the PR; your people still review and merge it."
5. **The hard case (tab 2).** Show the risk memo in tab 3. "The easy fix is one line, but a signed exception covers it, so Devin wrote a memo for the owner instead of touching it."
6. **Trust.** "The auditor can see every file Devin read and every command it ran, and the PR shows the firewall rule unchanged."

**Close.** "Five controls answered in under ten minutes. Your engineers review one PR and assign two gaps."

If the room pushes on the firewall rule, paste prompt 2: Devin declines and points back at the memo.

## What the results mean

| Control | Result | In plain words |
| --- | --- | --- |
| CN13 token lifetime | Fixed (PR) | Twelve access tokens never expired. Safe to fix, so Devin opened a PR. |
| CN05 untrusted access | Risk memo | The firewall is open to the internet, but a signed exception covers it. No change. |
| CN01 encryption in transit | Pass | Every call is encrypted (HTTPS). |
| CN04 access logging | Gap, @ardencm/platform-eng | No audit trail is defined here. Devin named the owner. |
| CN09 log integrity | Gap, @ardencm/security-eng | Depends on CN04. Devin named the owner. |

## Fallback

If the live run stalls, replay [traderX PR #119](https://github.com/COG-GTM/traderXCognitiondemos/pull/119). Same results, no
Devin run. Say it is a replay.

```bash
git fetch origin devin/fs-audit-1791848400-ccc-traceability && git checkout devin/fs-audit-1791848400-ccc-traceability
arden/finserv-estate/demo/setup-demo4.sh && arden/finserv-estate/demo/run-demo4.sh
```

## Acceptance evidence (live run 1)

| Claim | Evidence | Status |
| --- | --- | --- |
| Prompt 1 verbatim produces the map | https://devin-gtm.devinenterprise.com/sessions/90f0d0c771dd4a108391061bfae8ece9 | Verified |
| CN13 FIX with a Devin PR | https://github.com/COG-GTM/Springboot-BankApp/pull/333 (open, not merged) | Verified |
| CN05 RISK, no ingress change | PR #333 diff leaves `main.tf` ingress unchanged; risk memo in session | Verified |
| CN01 PASS, CN04/CN09 GAP with owners | Control map in session | Verified |
| Org audit log beat | Devin settings audit log, admin view | Not proven in run 1 |
| Wall time | Prompt 02:54:23 UTC, PLAT-771 found 02:55:46, PR #333 02:58:09, final map 03:03:11 (8m48s) | Verified |
| Recording | None made for run 1 | Gap |

Run 1 also flagged, unprompted: CVE-2026-82329 on Artifactory 7.111.19 raises the CN05 stakes; `version = local.artifactory_version`
is invalid Terraform (left alone, CAB pin); PR #333 overlaps #327 (only one should land). PLAT-771 itself was not opened
(no Jira in the session), and the memo says so. Use these as live talking points.
