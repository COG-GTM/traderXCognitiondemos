#!/usr/bin/env bash
# Demo 1 — fleet-wide CVE remediation, end to end, deterministic.
#
#   demo/run-demo1.sh                # fixture webhook → parent → 17 children (dry-run) → PR artifacts → dashboard/report
#   FS_FANOUT_MODE=live demo/run-demo1.sh     # same, but fan-out POSTs real child sessions via DEVIN_API_KEY
#   FS_SCANNER_SOURCE=snyk-api SNYK_TOKEN=… SNYK_ORG_ID=… demo/run-demo1.sh   # real scanner, same normalizer
#
# Steps mirror what the webhook receiver (demo/scanner/webhook.py) runs when Snyk posts a project_snapshot.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/../scripts/lib.sh"
cd "$HERE/.."
export PATH="$HOME/.local/bin:$PATH"

MODE="${FS_FANOUT_MODE:-dry-run}"
SOURCE="${FS_SCANNER_SOURCE:-snyk-webhook}"

[[ -d "${FS_WORKSPACE:-$HOME/finserv-workspace}/settlement-instruction-service/.git" ]] || scripts/reset.sh

log "1/5 scanner input  ($SOURCE)"
python3 demo/scanner/normalize.py --source "$SOURCE" ${SNYK_ORG_ID:+--org "$SNYK_ORG_ID"}

log "2/5 parent fan-out ($MODE, one child per finding)"
python3 demo/scanner/fanout.py --mode "$MODE"

if [[ "$MODE" == "dry-run" ]]; then
  log "3/5 children: deterministic remediation in the workspace"
  python3 demo/scanner/remediate.py
else
  log "3/5 children: live sessions created — collect results with demo/scanner/collect.py when they finish"
fi

log "4/5 burn-down dashboard + SEC/DORA report"
python3 demo/scanner/report.py

log "5/5 artifacts"
echo "  findings        demo/out/scanner/findings.json"
echo "  fan-out plan    demo/out/scanner/fanout-plan.json  (+ prompts/ one per child)"
echo "  PR artifacts    demo/out/scanner/prs/<finding>/{PR.md,change.patch}"
echo "  dashboard       demo/out/scanner/burndown.html"
echo "  leave-behind    demo/reports/demo1-remediation-report.md"
echo
echo "hard case: $(python3 -c "import json;r=[x for x in json.load(open('demo/out/scanner/results.json')) if x['status']=='fixed-with-deviation'];print(', '.join(x['finding_id'] for x in r) or 'none')")"
