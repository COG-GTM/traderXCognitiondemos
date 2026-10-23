#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KIT_DIR="$(cd "$HERE/.." && pwd)"
source "$HERE/../scripts/lib.sh"

fail() {
  printf 'SETUP FAILED: %s: %s\n' "$1" "$2" >&2
  exit 1
}

missing=()
command -v git >/dev/null 2>&1 || missing+=(git)
if ! command -v python3 >/dev/null 2>&1; then
  missing+=(python3)
elif ! python3 -c 'import sys; sys.exit(0 if sys.version_info >= (3, 8) else 1)'; then
  missing+=("python3 >= 3.8")
fi
command -v curl >/dev/null 2>&1 || missing+=(curl)
if [[ ${#missing[@]} -gt 0 ]]; then
  command -v apt-get >/dev/null 2>&1 || fail "install prerequisites" "missing ${missing[*]} and apt-get is unavailable"
  command -v sudo >/dev/null 2>&1 || fail "install prerequisites" "missing ${missing[*]} and sudo is unavailable"
  sudo -n apt-get install -y git python3 python3-pip curl ||
    fail "install prerequisites" "sudo -n apt-get could not install git, python3, python3-pip, and curl"
fi
for command_name in git python3 curl; do
  command -v "$command_name" >/dev/null 2>&1 || fail "check prerequisites" "$command_name is still missing"
done
python_version="$(python3 -c 'import sys; print("%d.%d" % sys.version_info[:2])')" ||
  fail "check Python version" "python3 could not report its version"
python3 -c 'import sys; sys.exit(0 if sys.version_info >= (3, 8) else 1)' ||
  fail "check Python version" "Python 3.8 or newer is required, found $python_version"

if ! python3 -c "import yaml, pytest" >/dev/null 2>&1; then
  if ! python3 -m pip install -q --disable-pip-version-check PyYAML pytest; then
    python3 -m pip install -q --disable-pip-version-check --user PyYAML pytest ||
      fail "install Python packages" "PyYAML and pytest could not be installed"
  fi
fi
python3 -c "import yaml, pytest" >/dev/null 2>&1 ||
  fail "check Python packages" "PyYAML or pytest is still unavailable"

# Demo 4 uses Python and shell only, Maven, JDK, and Node are not used.
printf 'Maven, JDK, and Node are not used by Demo 4, so setup does not install them.\n'

if ! "$HERE/audit/reset.sh"; then
  fail "reset Demo 4 sources" "demo/audit/reset.sh failed"
fi
if ! python3 -m pytest -q -p no:cacheprovider "$HERE/audit/test_audit.py"; then
  fail "run audit tests" "pytest reported a failure"
fi
tmp_log="$(mktemp)"
trap 'rm -f "$tmp_log"' EXIT
if ! (cd "$KIT_DIR" && FS_SKIP_RESET=1 demo/run-demo4.sh >"$tmp_log" 2>&1); then
  tail -40 "$tmp_log" >&2
  fail "run Demo 4 smoke check" "demo/run-demo4.sh failed"
fi
if ! python3 - "$HERE/out/audit/control-map.json" "$HERE/out/audit/traceability.html" \
    "$HERE/out/audit/evidence-pack/risk-decision-CN05.md" <<'PY'
import json
import sys
from pathlib import Path

try:
    control_map = json.loads(Path(sys.argv[1]).read_text())
    traceability = Path(sys.argv[2]).read_text()
    risk_memo = Path(sys.argv[3]).read_text()
    findings = control_map["findings"]
    routes = {finding["control"]: finding["route"] for finding in findings}
    expected = {
        "CCC.Core.CN13": "FIX",
        "CCC.Core.CN05": "RISK",
        "CCC.Core.CN01": "PASS",
        "CCC.Core.CN04": "GAP",
        "CCC.Core.CN09": "GAP",
    }
    assert routes == expected, f"unexpected routes: {routes}"
    cn13 = next(f for f in findings if f["control"] == "CCC.Core.CN13")
    assert "12 tokens" in cn13["finding"], cn13["finding"]
    assert all(f["evidence"] for f in findings), "a finding has empty evidence"
    assert all(f.get("owner") and f.get("due") for f in findings if f["route"] == "GAP"), "a GAP is missing owner or due"
    assert "Recorded audit snapshot (fixture)" in traceability, "traceability snapshot label is missing"
    assert "PLAT-771" in risk_memo, "risk memo is missing PLAT-771"
    assert "FAIL" not in risk_memo, "risk memo contains FAIL"
except Exception as exc:
    print(f"SETUP FAILED: artifact assertions: {exc}", file=sys.stderr)
    sys.exit(1)
PY
then
  fail "check Demo 4 artifacts" "artifact assertions failed"
fi
if ! "$HERE/audit/reset.sh"; then
  fail "reset Demo 4 output" "demo/audit/reset.sh failed"
fi

printf 'READY: Demo 4, audit readiness and traceability (FINOS CCC.Core v2025.10)\n'
printf 'First command:  cd %s && demo/run-demo4.sh\n' "$KIT_DIR"
printf 'Talk track:     demo/TALK-TRACK-demo4.md\n'
printf 'First line:     "Your auditor just asked: show me every platform control, and who changed what, why, and who approved it. Devin answers with one run."\n'
