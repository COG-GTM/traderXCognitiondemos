#!/usr/bin/env bash
# Demo 3 — event-driven incident response: Datadog monitor → investigation → fix branch → incident record + RCA.
#
#   demo/run-demo3.sh                       # fixture alert, dry-run Slack (thread written to demo/out/incident/slack-thread.md)
#   FS_SKIP_RESET=1 demo/run-demo3.sh       # keep the current workspace
#   FS_INCIDENT_SOURCE=datadog DD_API_KEY=… DD_APP_KEY=… demo/run-demo3.sh --input alert.json   # real monitor webhook + Datadog API
#   FS_SLACK_MODE=live SLACK_BOT_TOKEN=… FS_SLACK_CHANNEL=C… demo/run-demo3.sh                # post the thread to a real channel
#
# The seeded incident: client-portal 31.4.2 (memoisation of the valuation row, deployed 21:05 the night before) looks
# responsible; the real cause is ArdenFeed schema 2.3 going live at 06:00 with px as an object. Nothing is deployed.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/../scripts/lib.sh"
cd "$HERE/.."

[[ -n "${FS_SKIP_RESET:-}" && -d "${FS_WORKSPACE:-$HOME/finserv-workspace}/client-portal/.git" ]] || scripts/reset.sh

log "0/4 the incident as the portal sees it (live batch = ArdenFeed 2.3)"
(cd "${FS_WORKSPACE:-$HOME/finserv-workspace}/client-portal" && ARDENFEED_BATCH=../market-data-feed-contracts/samples/latest-batch.json node -e '
  import("./src/valuation/valuation.js").then(async ({ valuePortfolio }) => {
    const { loadQuotes } = await import("./src/feed/client.js");
    const positions = JSON.parse(require("node:fs").readFileSync("fixtures/positions.json", "utf8"));
    try { valuePortfolio(positions, loadQuotes()); console.log("  (no error — reset first)"); }
    catch (e) { console.log("  GET /api/portfolio/valuation → 500  " + e.constructor.name + ": " + e.message); }
  });')

log "1/4 ingest: monitor alert + error logs + APM trace + deploy events → one incident input"
python3 demo/incident/ingest.py "$@"

log "2/4 investigate: reproduce → test the deploy lead → test the input lead → fix on a branch → what's left for humans"
python3 demo/incident/investigate.py

log "3/4 report: incident-channel thread, timeline, incident record, RCA"
python3 demo/incident/report.py

log "4/4 artifacts"
echo "  input           demo/out/incident/incident-input.json     (normalised alert/logs/trace/deploys)"
echo "  investigation   demo/out/incident/investigation.json      (5 steps, evidence + verdict each)"
echo "  slack thread    demo/out/incident/slack-thread.md         (what #inc-client-portal sees)"
echo "  PR artifact     demo/out/incident/prs/client-portal/{PR.md,change.patch}"
echo "  timeline        demo/reports/demo3-incident-timeline.html"
echo "  incident record demo/reports/demo3-incident-record.md     (classification left to ops risk)"
echo "  RCA             demo/reports/demo3-rca.md"
echo "  branch          incident/inc-2026-0925-01-ardenfeed-v23 in ${FS_WORKSPACE:-$HOME/finserv-workspace}/client-portal (not deployed)"
