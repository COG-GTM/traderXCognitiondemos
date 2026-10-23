---
name: demo_incident
description: Set up the FS Golden Demo "incident before the London open" (Arden client-portal valuation 5xx, INC-2026-0925-01). Use when a presenter asks to prepare, seed, reset or rehearse demo_incident, the Arden incident demo, or the client-portal / ArdenFeed v2.3 demo. Seeds the BEFORE branch, prepares this machine, prints READY with the seed branch URL, prompt 1 and the fallback.
---

# demo_incident: incident before the London open

The presenter works an incident live with Devin. A Datadog monitor says the client-portal valuation API is failing and
blames last night's deploy 31.4.2. Devin has to prove the deploy is innocent, find the real cause (ArdenFeed schema 2.3
nests `px` as `{ v, ccy }`), open the smallest fix PR and write the incident record.

## Steps

1. Run the setup script that sits next to this file (in a checkout of the kit branch it is
   `plugins/demo_incident/skills/demo_incident/setup.sh`):

   ```bash
   bash setup.sh            # reseed demo/arden-live-before, prepare Node 20, verify BEFORE, serve the broken portal
   ```

   It needs `gh` signed in with push access to `COG-GTM/traderXCognitiondemos` and `sudo` only if Node 20+ is missing.
   Reseeding force-resets `demo/arden-live-before` and closes PRs left on it by earlier runs (it is shared by the
   Arden live demos: fleetwide, incident, audit, t1). Set `DEMO_INCIDENT_SKIP_SEED=1` to verify and serve without
   reseeding. It prints `READY` only at the very end; anything else is a failure with the step that failed.

2. Read the `READY` banner back to the presenter: seed branch URL, playbook id, prompt 1, the BEFORE portal URL, and the
   fallback.

3. The presenter opens a new Devin session on `COG-GTM/traderXCognitiondemos`, attaches playbook
   `playbook-abc2582e18fd45558d56838a14101336` (FS incident response), and pastes prompt 1 from `PROMPTS.md`
   (same folder). `TALK-TRACK.md` says what to say while Devin works and which beats are live or pre-built.

4. Fallback if the live session is slow: `bash setup.sh --fallback` serves the pre-built fix (PR #122) on port 3201 so
   the presenter can show valued rows, and PR #122 / #118 hold the finished fix, ruled-out evidence and tests.
   Never merge, deploy or post to Slack from this demo.

## Do not

- Do not run Devin's investigation for the presenter; this skill only sets up the situation.
- Do not touch PRs #118 and #122 (they are the fallback) or the other Arden demos' files.
