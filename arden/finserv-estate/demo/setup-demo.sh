#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ESTATE="$(cd "$HERE/.." && pwd)"
ROOT="$(cd "$ESTATE/../.." && pwd)"
WS="${FS_WORKSPACE:-$HOME/finserv-workspace}"
export FS_SOURCES="${FS_SOURCES:-$HOME/fs-sources}"
export FS_SOURCE_MODE="${FS_SOURCE_MODE:-remote}"
export FS_WORKSPACE="$WS"
export PATH="$HOME/.local/bin:$PATH"
started="$(date +%s)"

trap 'status=$?; printf "\033[1;31mSETUP FAILED at: %s\033[0m\n" "$BASH_COMMAND" >&2; exit "$status"' ERR

as_root() {
  if [[ "$(id -u)" == "0" ]]; then
    "$@"
  else
    sudo -n "$@"
  fi
}

node_major() {
  node -p 'Number(process.versions.node.split(".")[0])' 2>/dev/null || printf '0'
}

java_major() {
  java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1
}

need_apt=0
for tool in git curl unzip mvn python3; do
  command -v "$tool" >/dev/null 2>&1 || need_apt=1
done
[[ "$(java_major || true)" =~ ^[0-9]+$ ]] && (( $(java_major) >= 17 )) || need_apt=1
[[ -x /usr/lib/jvm/java-17-openjdk-amd64/bin/java ]] || need_apt=1
if (( need_apt )); then
  as_root apt-get update
  packages=(git curl unzip python3 python3-pip openjdk-17-jdk maven python3-pytest python3-yaml psmisc)
  as_root apt-get install -y "${packages[@]}"
fi

if ! python3 - <<'PY'
import pytest
version = tuple(int(part) for part in pytest.__version__.split(".")[:2])
raise SystemExit(0 if version >= (8, 0) else 1)
PY
then
  python3 -m pip install --user --upgrade 'pytest>=8' PyYAML
fi

if (( $(node_major) < 20 )); then
  node_setup="$HOME/.cache/arden-nodesource-setup.sh"
  mkdir -p "$HOME/.cache"
  curl -fsSL https://deb.nodesource.com/setup_20.x -o "$node_setup"
  if [[ "$(id -u)" == "0" ]]; then
    bash "$node_setup"
  else
    sudo -n -E bash "$node_setup"
  fi
  as_root apt-get install -y nodejs
fi
(( $(node_major) >= 20 ))

python3 -c "import pytest, yaml" >/dev/null 2>&1 || {
  if command -v apt-get >/dev/null 2>&1; then
    as_root apt-get install -y python3-pytest python3-yaml
  fi
}
python3 -c "import pytest, yaml" >/dev/null 2>&1 || python3 -m pip install --user pytest PyYAML

if ! command -v terraform >/dev/null 2>&1 || [[ "$(terraform version -json 2>/dev/null | python3 -c 'import json,sys; print(json.load(sys.stdin).get("terraform_version", ""))' 2>/dev/null || true)" != "1.9.8" ]]; then
  mkdir -p "$HOME/.local/bin"
  terraform_zip="$(mktemp)"
  curl -fsSL https://releases.hashicorp.com/terraform/1.9.8/terraform_1.9.8_linux_amd64.zip -o "$terraform_zip"
  unzip -oq "$terraform_zip" -d "$HOME/.local/bin"
  rm -f "$terraform_zip"
fi
export PATH="$HOME/.local/bin:$PATH"
[[ "$(terraform version -json | python3 -c 'import json,sys; print(json.load(sys.stdin)["terraform_version"])')" == "1.9.8" ]]

settings="$HOME/.m2/settings.xml"
if [[ ! -f "$settings" ]] || ! python3 - "$settings" <<'PY'
import pathlib
import re
import sys
raise SystemExit(0 if re.search(r"<mirror(?:\s|>)", pathlib.Path(sys.argv[1]).read_text(), re.I) else 1)
PY
then
  mkdir -p "$(dirname "$settings")"
  python3 - "$settings" <<'PY'
import pathlib
import shutil
import sys

path = pathlib.Path(sys.argv[1])
mirror = """  <mirrors>
    <mirror>
      <id>google-maven-central</id>
      <name>Google Cloud Storage Maven Central mirror</name>
      <url>https://maven-central.storage-download.googleapis.com/maven2/</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
"""
if path.exists():
    text = path.read_text()
    backup = path.with_suffix(path.suffix + ".bak")
    shutil.copy2(path, backup)
    if "</settings>" in text:
        path.write_text(text.replace("</settings>", mirror + "</settings>", 1))
    else:
        path.write_text("<settings>\n" + mirror + "</settings>\n")
    print(f"Maven settings: inserted mirror, backup at {backup}")
else:
    path.write_text("<settings>\n" + mirror + "</settings>\n")
    print(f"Maven settings: wrote mirror to {path}")
PY
else
  printf 'Maven settings: existing mirror retained in %s\n' "$settings"
fi

export FS_SOURCE_MODE FS_SOURCES
scripts="$ESTATE/scripts"
"$scripts/reset.sh"

cd "$ESTATE"
DEMO_PACE=0 "$HERE/run-demo1.sh"
python3 - "$HERE/out/scanner/results.json" <<'PY'
import collections
import json
import sys
results = json.load(open(sys.argv[1]))
counts = collections.Counter(row["status"] for row in results)
actual = f"{counts['fixed']} fixed, {counts['fixed-with-deviation']} fixed-with-deviation, {counts['blocked']} blocked"
expected = "16 fixed, 1 fixed-with-deviation, 0 blocked"
if actual != expected:
    print(f"SMOKE FAILED: Demo 1: expected {expected}, got {actual}", file=sys.stderr)
    raise SystemExit(1)
print(f"SMOKE PASS: Demo 1: {actual}")
PY

DEMO_PACE=0 "$HERE/run-demo2.sh"
python3 - "$HERE/out/t1/inventory.json" "$HERE/out/t1/parity.json" <<'PY'
import json
import sys
inventory, parity = (json.load(open(path)) for path in sys.argv[1:])
summary = inventory["summary"]
counts = parity["summary"]
escalated = sorted(row["trade_id"].rsplit("-", 1)[-1] for row in parity["trades"] if row["verdict"] == "ESCALATE")
expected = "37 files, 62 citations, 16 repos; parity 16 expected, 10 unchanged, 4 escalated; IDs 0021, 0022, 0027, 0028"
actual = (f"{summary['files']} files, {summary['citations']} citations, {summary['repos_affected']} repos; "
          f"parity {counts['expected']} expected, {counts['unchanged']} unchanged, {counts['escalated']} escalated; "
          f"IDs {', '.join(escalated)}")
if actual != expected:
    print(f"SMOKE FAILED: Demo 2: expected {expected}, got {actual}", file=sys.stderr)
    raise SystemExit(1)
print(f"SMOKE PASS: Demo 2: {actual}")
PY

DEMO_PACE=0 "$HERE/run-demo3.sh"
python3 - "$HERE/out/incident/investigation.json" "$WS" <<'PY'
import json
import pathlib
import sys
investigation = json.load(open(sys.argv[1]))
expected = "deploy 31.4.2 ruled out, root cause ArdenFeed schema 2.3, fix tests pass, fix branch exists"
steps = {step["id"]: step for step in investigation["steps"]}
deploy_ruled_out = steps[2]["ruled_out"] and "RULED OUT" in steps[2]["verdict"]
root_cause = "ArdenFeed schema 2.3" in steps[3]["verdict"]
fix_passed = steps[4]["ok"]
portal = pathlib.Path(sys.argv[2]) / "client-portal"
branch_exists = investigation["branch"] in __import__("subprocess").check_output(
    ["git", "-C", str(portal), "branch", "--list", investigation["branch"]], text=True
)
actual = (f"deploy 31.4.2 {'ruled out' if deploy_ruled_out else 'not ruled out'}, "
          f"root cause ArdenFeed schema {'2.3' if root_cause else 'unknown'}, "
          f"fix tests {'pass' if fix_passed else 'fail'}, fix branch {'exists' if branch_exists else 'missing'}")
if actual != expected:
    print(f"SMOKE FAILED: Demo 3: expected {expected}, got {actual}", file=sys.stderr)
    raise SystemExit(1)
print(f"SMOKE PASS: Demo 3: {actual}")
PY

PORTAL="$WS/client-portal"
"$HERE/portal.sh" before
trap '"$HERE/portal.sh" stop' EXIT
page_status="$(curl -sS -o "$HOME/.cache/arden-portal-main.html" -w '%{http_code}' http://127.0.0.1:3210/)"
api_status="$(curl -sS -o "$HOME/.cache/arden-portal-main-api.json" -w '%{http_code}' http://127.0.0.1:3210/api/portfolio/valuation)"
"$HERE/portal.sh" stop
python3 - "$page_status" "$api_status" "$HOME/.cache/arden-portal-main.html" "$HOME/.cache/arden-portal-main-api.json" <<'PY'
import pathlib
import sys
page_status, api_status = sys.argv[1:3]
html, api = pathlib.Path(sys.argv[3]).read_text(), pathlib.Path(sys.argv[4]).read_text()
actual = f"GET / {page_status} brand={'Arden Capital Markets' in html}; API {api_status} valuation_failed={'valuation_failed' in api}"
expected = "GET / 200 brand=True; API 500 valuation_failed=True"
if actual != expected:
    print(f"SMOKE FAILED: portal main: expected {expected}, got {actual}", file=sys.stderr)
    raise SystemExit(1)
print(f"SMOKE PASS: portal main: {actual}")
PY

"$HERE/portal.sh" after
fix_api_status="$(curl -sS -o "$HOME/.cache/arden-portal-fix-api.json" -w '%{http_code}' http://127.0.0.1:3210/api/portfolio/valuation)"
"$HERE/portal.sh" stop
python3 - "$fix_api_status" "$HOME/.cache/arden-portal-fix-api.json" <<'PY'
import json
import pathlib
import sys
status = sys.argv[1]
body = json.loads(pathlib.Path(sys.argv[2]).read_text())
actual = f"API {status}, {len(body.get('rows', []))} rows"
expected = "API 200, 3 rows"
if actual != expected:
    print(f"SMOKE FAILED: portal fix: expected {expected}, got {actual}", file=sys.stderr)
    raise SystemExit(1)
print(f"SMOKE PASS: portal fix: {actual}")
PY

"$scripts/reset.sh"
trap - EXIT
[[ "$(git -C "$PORTAL" branch --show-current)" == "main" ]]
[[ -z "$(git -C "$PORTAL" status --porcelain)" ]]
[[ -z "$(find "$ESTATE/demo/out" -type f -print -quit)" ]]
for repo in $(bash -c 'source "$1/lib.sh"; repo_list' _ "$scripts"); do
  [[ -z "$(git -C "$WS/$repo" status --porcelain)" ]]
done
printf '\nREADY: Arden Capital Markets FS demos (Demo 1 CVE remediation, Demo 2 T+1, Demo 3 incident)\n'
printf 'First command: demo/run-demo1.sh\n'
printf 'First talk-track line: "The scanner has already done its job: 17 findings across 10 repositories, including the Artifactory token blast radius."\n'
finished="$(date +%s)"
printf 'Setup elapsed: %s seconds\n' "$((finished - started))"
