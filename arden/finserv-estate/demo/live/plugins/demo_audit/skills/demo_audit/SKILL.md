---
name: demo_audit
description: Seed the Arden FINOS CCC.Core v2025.10 audit readiness demo (FS Golden Demo Bank). Use when a presenter says demo_audit, "audit demo", "CCC audit demo" or "seed the audit situation". Runs the seed, prints READY with the seed branch URL and prompt 1, and gives the traderX PR #119 replay as the fallback.
---

# demo_audit

The presenter wants the live audit situation ready. You seed it; you do not run the audit yourself. The audit happens in
a NEW Devin session the presenter opens and pastes prompt 1 into.

## Steps

1. Get the kit (no other setup needed: git, gh, python3, curl):
   ```bash
   cd ~ && (test -d traderXCognitiondemos || git clone https://github.com/COG-GTM/traderXCognitiondemos.git)
   cd traderXCognitiondemos && git fetch origin devin/1792724600-demo-arden-live && git checkout -B demo-kit FETCH_HEAD
   ```
2. Run the seed. It fails loudly and prints READY only on success:
   ```bash
   arden/finserv-estate/demo/live/audit/seed-audit.sh
   ```
   It checks the Springboot-BankApp platform controls are at the BEFORE state, closes CN13 PRs from earlier live runs
   (never #316 to #320 or #327), and re-creates `demo/arden-live-before`. If the presenter wants a check only, use
   `DEMO_AUDIT_DRY_RUN=1`.
3. Send the presenter the whole READY block verbatim: seed branch URL, prompt 1, fallback. Prompt 1 is also in
   `arden/finserv-estate/demo/live/audit/PROMPTS.md`; the talk track is `arden/finserv-estate/demo/live/audit/TALK-TRACK.md`.
4. Stop. Do not paste prompt 1 into this session and do not open PRs.

## If the seed fails

Report the `SEED FAILED:` line. If the BEFORE state check fails, someone changed
`devin/1790864747-arden-finserv-estate` in COG-GTM/Springboot-BankApp; tell the presenter and offer the fallback.

## Fallback: traderX PR #119 replay

https://github.com/COG-GTM/traderXCognitiondemos/pull/119 , deterministic, no Devin run:
```bash
git fetch origin devin/fs-audit-1791848400-ccc-traceability && git checkout devin/fs-audit-1791848400-ccc-traceability
arden/finserv-estate/demo/setup-demo4.sh && arden/finserv-estate/demo/run-demo4.sh
```

## Rules

Never merge, deploy, post to real Slack, or write to Notion. Never close Springboot-BankApp #316 to #320 or #327.
