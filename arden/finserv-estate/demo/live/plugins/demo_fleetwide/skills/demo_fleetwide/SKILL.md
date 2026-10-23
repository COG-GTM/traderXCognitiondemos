---
name: demo_fleetwide
description: Set up the Arden fleet-wide CVE remediation demo (FS Golden Demo Bank, SEC-2026-0912, one Devin child session per Snyk finding). Use when a presenter says demo_fleetwide, "fleetwide demo", "CVE fan-out demo" or "seed the fleetwide situation". Runs the seed, prepares the machine, prints READY with the seed branch URL, prompt 1 and the fallback.
---

# demo_fleetwide

The presenter wants the live fleet-wide remediation situation ready. You set it up; you do not run the remediation
yourself. The remediation happens in a NEW Devin session the presenter opens and pastes prompt 1 into: that session
creates one child session per finding with playbook `playbook-1db82f8448944ee7b94282f275798b59`.

## Steps

1. Get the kit (needs git, gh, python3, curl, java 17; Maven is installed if missing):
   ```bash
   cd ~ && (test -d traderXCognitiondemos || git clone https://github.com/COG-GTM/traderXCognitiondemos.git)
   cd traderXCognitiondemos && git fetch origin devin/1792724600-demo-arden-live && git checkout -B demo-kit FETCH_HEAD
   ```
2. Run the setup. It fails loudly (`SETUP FAILED: <step>`) and prints READY only on success (about 2 minutes):
   ```bash
   arden/finserv-estate/demo/live/fleetwide/setup-fleetwide.sh
   ```
   It closes `security/*` PRs left by earlier live fleetwide runs in the three host repos (never the old
   `sec-2026-0912` PRs), writes the GCS Maven mirror, runs `FS_SOURCE_MODE=remote reset.sh`, proves the hard case
   locally (settlement-instruction-service green at BEFORE, red on core-dates 1.5.0 because tag 64 turns ISO, green on
   1.4.1), re-creates `demo/arden-live-before`, and checks the 5 stage findings are in the seeded fixture.
   Check only: `DEMO_FLEETWIDE_DRY_RUN=1` (closes nothing, pushes nothing). Seed only: `DEMO_FLEETWIDE_SKIP_MACHINE=1`.
3. Send the presenter the whole READY block verbatim: seed branch URL, prompt 1, fallback. Prompts (stage, full 17,
   dashboard) are in `arden/finserv-estate/demo/live/fleetwide/PROMPTS.md`; timings and beats in `TALK-TRACK.md`.
4. Stop. Do not paste prompt 1 into this session, do not create child sessions, do not open PRs.

## If the setup fails

Report the `SETUP FAILED:` line. Common causes: `gh` not authenticated; Maven Central 429 (the script writes the GCS
mirror, so check `~/.m2/settings.xml` was not overridden); the hard-case check failing means someone changed
core-dates or settlement-instruction-service on `devin/1790864747-arden-finserv-estate`: tell the presenter and offer
the fallback.

## Fallback

1. The pre-run live session listed under "Fallback" in `TALK-TRACK.md`: walk its fan-out plan, burn-down table and the
   five PRs (the settlement-instruction-service PR is the tag 64 / core-dates 1.4.1 deviation).
2. Deterministic replay, no Devin run:
   ```bash
   git checkout devin/1790864747-arden-finserv-estate && arden/finserv-estate/demo/run-demo1.sh
   ```

## Rules

Never merge, deploy, post to real Slack, or write to Notion. Never close or edit the `sec-2026-0912` PRs
(Securities-Trade-Processing-System #5 to #14, traderXCognitiondemos #115 to #117, Springboot-BankApp #316 to #320).
