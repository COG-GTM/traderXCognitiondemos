#!/usr/bin/env bash
# Demo 1: fleet-wide CVE remediation, end to end, deterministic.
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
  log "3/5 children: live sessions created: collect results with demo/scanner/collect.py when they finish"
fi

log "4/5 burn-down dashboard + SEC/DORA report"
python3 demo/scanner/report.py

log "5/5 artifacts"
echo "  findings        demo/out/scanner/findings.json"
echo "  fan-out plan    demo/out/scanner/fanout-plan.json  (+ prompts/ one per child)"
echo "  PR artifacts    demo/out/scanner/prs/<finding>/{PR.md,change.patch}"
echo "  dashboard       demo/out/scanner/burndown.html"
echo "  leave-behind    demo/reports/demo1-remediation-report.md"
echo "  open             demo/reports/demo1-burndown.html"
echo
python3 - <<'PY'
import json
from pathlib import Path
from scanner.evidence import hard_case_evidence

results = json.load(open("demo/out/scanner/results.json"))
result = next((r for r in results if r["status"] == "fixed-with-deviation"), None)
if not result:
    raise SystemExit("SMOKE FAILED: Demo 1 hard case: fixed-with-deviation result is missing")
first = next((a for a in result["attempts"] if not a["tests_ok"]), None)
second = next((a for a in reversed(result["attempts"]) if a["tests_ok"]), None)
if not first or not second:
    raise SystemExit("SMOKE FAILED: Demo 1 hard case: failed or passing attempt is missing")
evidence = hard_case_evidence(first, second)
print("Hard case: settlement-instruction-service")
print(f"  Attempt 1 FAIL: {first['strategy']}")
print(f"  {evidence['headline']}")
for test in evidence["tests"]:
    print(f"  {test['name']}: expected {test['expected']}; actual {test['actual']}")
print(f"  Attempt 2 PASS {evidence['passed']}: {evidence['attempt2_headline']}")
print(f"  Added test file: {evidence['test_file']}")
print(f"  Deviation recorded for human reviewer: {', '.join(result.get('owners', [])) or 'CODEOWNERS owner not found'}")
print(f"  Dashboard: {Path('demo/reports/demo1-burndown.html')}")
PY
