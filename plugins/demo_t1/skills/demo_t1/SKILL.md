---
name: demo_t1
description: Set up the FS Golden Demo Bank T+1 settlement migration demo (Arden, ticket T1-2027) so a presenter can drive Devin live through inventory, tranche 1 migration, golden-file parity and the venue-calendar vs TARGET2 escalation. Use when asked to prepare, reset or run demo_t1 / the T+1 demo.
---

# demo_t1: T+1 settlement migration, live

The situation: Arden (synthetic bank, 20 repos) must move EU/UK/CH regular-way trades from T+2 to T+1 for a 2027-10-11 go-live.
The seed branch has the estate, the golden trade file and a parity checker, and no answers (no inventory, no migration, no reports).
The presenter pastes prompt 1 and Devin does the work live. The kit scripts are the presenter's independent check and the fallback.

## 1. Run seed and prepare the machine

On the presenter's (or Devin's) machine, with `gh` authenticated to COG-GTM:

```bash
git clone https://github.com/COG-GTM/traderXCognitiondemos.git && cd traderXCognitiondemos
git checkout devin/1792724600-demo-arden-live
arden/finserv-estate/demo/live/t1/setup-t1.sh
```

`setup-t1.sh` (strict mode, error trap; any failure prints `SETUP FAILED: <step>` and never READY):
1. checks git, gh, python3, Java 17, javac, Maven (installs Maven if absent), pyyaml;
2. writes the GCS Maven mirror to `~/.m2/settings.xml` (Maven Central returns 429 on Devin VMs);
3. runs `seed-t1.sh`: closes open PRs from earlier `T1-2027` runs, force-resets only `demo/t1-live-before` from the shared `demo/arden-live-before` seed plus the standalone `demo/t1/parity.py`, and verifies no answer paths are present;
4. runs `FS_SOURCE_MODE=remote arden/finserv-estate/scripts/reset.sh` (20 repos into `~/finserv-workspace`, core-dates 1.4.0 baseline into `~/.m2`);
5. verifies the BEFORE state, then prints READY.

## 2. READY output

The last thing printed on success:

```
READY: demo_t1 (T+1 settlement migration) seeded
  Seed branch: https://github.com/COG-GTM/traderXCognitiondemos/tree/demo/t1-live-before
  ... prompt 1 ...
  Talk track:  arden/finserv-estate/demo/live/t1/TALK-TRACK.md
  Your check:  python3 arden/finserv-estate/demo/t1/parity.py --migrated <jar from Devin's core-dates PR>
  Fallback:    arden/finserv-estate/demo/live/t1/fallback-t1.sh
```

Report the seed URL and prompt 1 to the presenter exactly as printed. If you see `SETUP FAILED`, report the step and stop; do not say the demo is ready.

## 3. Prompt 1

Start a Devin session (normal mode) with repos `COG-GTM/traderXCognitiondemos`, `COG-GTM/Securities-Trade-Processing-System`, `COG-GTM/Springboot-BankApp` and paste prompt 1 from `arden/finserv-estate/demo/live/t1/PROMPTS.md` (between the `prompt1` markers). It begins with `@playbook:playbook-cbb39530e1f447758488999e30c93083`; through the API, pass `playbook_id=playbook-cbb39530e1f447758488999e30c93083` instead.

Hold Devin to: 37 files inventoried with citations, tranche 1 draft PRs on core-dates, settlement-instruction-service and iso20022-fix-messages, parity over `demo/t1/golden/*.csv`, and trades ARD-27Q4-0021, 0022, 0027, 0028 escalated to @ardencm/market-ops and the Head of Post-Trade instead of decided. The calendar disagreement is the only hard case.

## 4. Fallback

If the live run stalls: `arden/finserv-estate/demo/live/t1/fallback-t1.sh` (about 2 minutes) runs the kit's inventory, tranche 1 migration and parity, and writes the reports under `arden/finserv-estate/demo/reports/`. Say they are the reference run, not Devin's live output.

## Rules

- Never merge any PR, deploy, or post to a real Slack channel.
- Do not install this plugin as part of the demo; it is set-up guidance.
- Reseeding is safe: it only touches `demo/t1-live-before` and open PRs that mention `T1-2027` on `devin/*` branches.
