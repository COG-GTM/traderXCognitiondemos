# demo_audit: talk track (live)

**Who:** a platform or risk lead at a bank with an audit date. **Length:** 10 to 15 minutes. **Live:** yes, Devin works the
situation in a new session; nothing is replayed unless the fallback is needed.

## Before the room (5 minutes)

1. Run `arden/finserv-estate/demo/live/audit/seed-audit.sh` (or ask a Devin session with the `demo_audit` skill to do it).
   Wait for `READY`. The READY block has the seed URL, prompt 1 and the fallback.
2. Open three tabs: the seed branch, `arden/platform-terraform/artifactory/main.tf` on
   `COG-GTM/Springboot-BankApp@devin/1790864747-arden-finserv-estate`, and a new Devin session.
3. Have the fallback ready in a terminal (traderX PR #119 replay, see PROMPTS.md).

## Beat 1. The situation (1 minute)

"Arden has an internal audit walkthrough on 16 November. The auditor uses FINOS Common Cloud Controls, CCC.Core v2025.10,
and wants five controls: evidence, a status, who owns each gap, and a fix where one is safe. One of the findings has a risk
exception that was signed in 2023. Nobody on the team remembers the details."

Show `ccc-core-v2025.10.yaml` on the seed branch: the stable catalog, the release candidates listed as not used.

## Beat 2. Hand Devin the audit (paste prompt 1)

Paste prompt 1 from PROMPTS.md verbatim. While Devin reads, say: "I have not told Devin which controls fail or where the
exception is. Devin reads the estate the way a new engineer would: AGENTS.md, the catalog, the Terraform."

What Devin does (live acceptance run, about 4 minutes to the PR):
- Reads `AGENTS.md`, the control list and both platform repos.
- Finds the PLAT-771 comment on `main.tf` line 19 within about 90 seconds and decides that CN05 is a memo, not a change.
- Opens a CN13 PR against the estate branch, then writes the control map and the CN05 risk memo.

## Beat 3. The control map (3 minutes)

Scroll Devin's control map. Point at the evidence line column: every row is a file and line, not a summary.

| Control | Status | Say |
| --- | --- | --- |
| CN13 token lifetime | FIX | "Twelve tokens never expire: eleven CI tokens and the default. Safe to fix, so Devin opened a PR." |
| CN05 untrusted access | RISK | "Open ingress, but there is a recorded exception. Devin wrote a memo and changed nothing." |
| CN01 encryption in transit | PASS | "Every Artifactory call in shared CI is HTTPS. Devin also says what lives outside these repos." |
| CN04 access logging | GAP, @ardencm/platform-eng | "No trail is defined here. Devin did not invent one; Devin named the owner and the due date." |
| CN09 log integrity | GAP, @ardencm/security-eng | "Depends on CN04. Same pattern: owner, due date, the exact evidence to bring." |

## Beat 4. The fix (2 minutes)

Open the CN13 PR Devin produced. Show: a 30 day TTL with rotation before expiry, the default lifetime cut to hours,
validation that rejects 0, and the PR body saying `0.0.0.0/0` was left alone because of PLAT-771. CODEOWNERS routes it to
platform-eng and security-eng. Nothing is merged or applied; Atlantis and the owners still decide.

## Beat 5. The hard case: Devin stops at CN05 (2 minutes)

Open the CN05 risk memo. "This is the moment that matters. The obvious fix is one line. Devin found the exception, wrote
down what it covers, who has to decide and what would have to be true to close it, and stopped." If the room pushes,
use prompt 2: Devin declines to close the ingress and points back at the memo.

## Beat 6. The audit trail (1 minute)

"How does the auditor trust this?" Show this Devin session (every file read, every command), the PR diff with no ingress
change, and the org audit log in Devin settings. Prompt 3 asks Devin to say this itself.

## Close

"Five controls, one fix in review, one exception respected, one pass, two gaps with names and dates. Your engineers review
a PR and two questions instead of building the evidence pack."

## Fallback

If the live run stalls or the network fails: run the traderX PR #119 replay (PROMPTS.md, Fallback). Same five rows, a
deterministic control map at `demo/out/audit/control-map.html`, and the same CN05 stop. Say it is a replay.

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
