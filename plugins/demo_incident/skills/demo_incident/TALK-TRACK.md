# demo_incident: talk track (about 12 minutes on stage)

Situation: Arden Capital Markets, 06:12 UTC, London opens at 07:00. The client-portal valuation page is failing and the
Datadog monitor blames last night's deploy 31.4.2. Everyone's instinct is to roll back. Devin works the incident live.

Measured on the rehearsal run (fresh session, playbook attached, prompt 1 pasted as is):
fix PR opened 3 min 19 s after the prompt; root cause and ruled-out evidence reported at 5 min 6 s; incident pack
(thread draft, timeline, incident record, RCA) attached and the session done at 9 min 34 s. Plan for 5 to 12 minutes of Devin time.

| Time | Beat | Live or pre-built | What the presenter shows and says |
|---|---|---|---|
| 0:00 | The page | Live (setup.sh) | Open `http://localhost:3200/api/portfolio/valuation`: 500 `valuation_failed`. Settlement summary still 200. "Clients can see settlements but not what their portfolio is worth, 48 minutes before the open." |
| 1:00 | The alert | Pre-built fixture | Open `monitor-webhook.json` in the seed branch: the monitor text points at deploy 31.4.2. "The obvious move is a rollback. Let's see whether Devin agrees." |
| 1:30 | Hand it to Devin | Live | New Devin session, playbook FS incident response attached, paste prompt 1 from `PROMPTS.md`. |
| 2:00 | Devin reads the room | Live | Devin reads AGENTS.md and the playbook, ingests monitor, logs, trace and deploy events, reproduces the 500 locally. Point at the shell: same `TypeError: n.px.toFixed is not a function` as production. |
| 3:00 | The hard case: is it the deploy? | Live | Devin checks out the pre-deploy 31.4.1 build and replays the live batch: it fails the same way. 31.4.2 served healthy requests for about 9 hours before 06:00. "A rollback would have burned 20 minutes and fixed nothing." Optional follow-up: "Would a rollback to 31.4.1 have fixed this? Show me the replay." |
| 5:00 | The real cause | Live | Devin finds the ArdenFeed vendor notice (schema 2.3, effective 06:00 UTC, `px` becomes `{ v, ccy }`) and `consumers.yaml` still listing client-portal on 2.2. "The deploy was a bystander. The upstream contract changed under us." |
| 6:00 | Smallest fix | Live | Open Devin's PR: one function (`normalizeQuote`) accepts 2.2 and 2.3 and throws on anything else, plus regression, equivalence and unknown-shape tests. Nothing merged, nothing deployed: the CODEOWNERS decide. |
| 8:00 | After | Live or fallback | Run the fix branch on the same v2.3 batch: 200 with valued rows (GB0002634946 148,920.00 GBP; CH0012032048 216,720.00 CHF; US0378331005 345,600.00 USD). Devin also records a browser walk-through. |
| 9:00 | Paperwork | Live | Devin's attachments: incident-channel thread draft, timeline from telemetry (first error 06:00:41, not 06:12), incident record with classification and client impact left to named humans, RCA. Nothing posted to Slack. |
| 11:00 | Close | Talk | "Evidence before action, the deploy ruled out by replay, a one-function fix with tests, and the record written while the engineers decide." |

## If it goes wrong

- Devin is still working at 8:00: keep talking through the beats Devin has done, then
  `bash setup.sh --fallback` serves the pre-built fix (PR #122) on port 3201 and PR #122 / #118 hold the finished
  evidence. Say plainly that this is an earlier run of the same incident.
- Devin accepts the deploy as the cause: ask the follow-up "Before you fix anything: what is your evidence that 31.4.2
  is or is not the cause?" The playbook requires the replay; Devin will run it.
- A reviewer asks about a v2.3 quote whose deprecated quote-level `ccy` disagrees with `px.ccy`: the rehearsal fix
  rejects it (fails closed, the request returns 500) rather than guessing a currency. Devin Review flagged this on
  the rehearsal PR as a possible false failure. Both are defensible; it is a good "you decide, not the bot" moment.

## Do not

Merge, deploy, roll back or post to Slack. Do not reseed `demo/arden-live-before` during another presenter's Arden
demo: the seed closes PRs on that branch.
