#!/usr/bin/env bash
# Demo 3: investigate an ArdenFeed schema incident, test the deploy lead, and prepare a fix branch.
#
#   demo/run-demo3.sh
#   FS_SKIP_RESET=1 demo/run-demo3.sh
#   FS_INCIDENT_SOURCE=datadog DD_API_KEY=... DD_APP_KEY=... demo/run-demo3.sh --input alert.json
#
# The 31.4.2 deploy is the false lead. ArdenFeed schema 2.3 is the cause. Nothing is deployed.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/../scripts/lib.sh"
cd "$HERE/.."

[[ -n "${FS_SKIP_RESET:-}" && -d "${FS_WORKSPACE:-$HOME/finserv-workspace}/client-portal/.git" ]] || scripts/reset.sh

pause_between_blocks() {
  python3 - <<'PY'
import os
import time
pace = float(os.environ.get("DEMO_PACE", "2"))
if pace < 0:
    raise SystemExit("DEMO_PACE must be zero or greater")
if pace:
    time.sleep(pace)
PY
}

log "0/4 alert: Datadog monitor context and the last deploy"
python3 demo/incident/ingest.py "$@"
python3 - <<'PY'
import json
from datetime import datetime
data = json.load(open("demo/out/incident/incident-input.json"))
alert = data["alert"]
deploy = data["deploys"][0]
deployed_at = datetime.fromisoformat(deploy["at"].replace("Z", "+00:00")).strftime("%H:%M")
print(f"ALERT: {alert['title']}")
print(f"Last deploy: {deploy['version']} {deploy['summary']}, {deployed_at}")
print(f"Fired at: {alert['fired_at']}")
PY
pause_between_blocks

log "1/4 investigation: reproduce, test the deploy lead, identify the input change, prepare the fix"
python3 demo/incident/investigate.py
pause_between_blocks

log "2/4 report: deterministic timeline, incident record, and RCA"
python3 demo/incident/report.py
pause_between_blocks

log "3/4 artifacts"
echo "  input           demo/out/incident/incident-input.json"
echo "  investigation   demo/out/incident/investigation.json"
echo "  PR artifact     demo/out/incident/prs/client-portal/{PR.md,change.patch}"
echo "  timeline        demo/reports/demo3-incident-timeline.html"
echo "  incident record demo/reports/demo3-incident-record.md"
echo "  RCA             demo/reports/demo3-rca.md"
echo "  branch          incident/inc-2026-0925-01-ardenfeed-v23 in ${FS_WORKSPACE:-$HOME/finserv-workspace}/client-portal (not deployed)"
