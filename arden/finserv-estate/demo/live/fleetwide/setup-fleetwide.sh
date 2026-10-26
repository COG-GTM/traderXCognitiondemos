#!/usr/bin/env bash
# demo_fleetwide: set up the live fleet-wide CVE remediation situation a presenter hands to Devin.
#   1. Closes security/* PRs left by earlier LIVE fleetwide runs in the three host repos (never sec-2026-0912 PRs).
#   2. Prepares this machine: Maven + GCS mirror, FS_SOURCE_MODE=remote reset.sh (core-dates 1.4.0 / 1.4.1 / 1.5.0).
#   3. Checks core-dates 1.4.0 embeds snakeyaml 1.33 (1.4.1: 2.2), then proves the hard case locally: settlement-instruction-service is green at BEFORE, red on core-dates 1.5.0
#      (tag 64 contract), green on 1.4.1.
#   4. Re-creates demo/arden-live-before with the shared seed-situation.sh and checks the 5 stage findings fan out.
#   5. Checks the always-open example PRs (demo/fleetwide-example-* heads, never closed here) and reopens any that were closed.
#   6. Prints READY with the seed URL, prompt 1, the example PRs and the fallback.
#
#   arden/finserv-estate/demo/live/fleetwide/setup-fleetwide.sh
#   DEMO_FLEETWIDE_DRY_RUN=1 ...       # list PRs it would close, do not close or push anything
#   DEMO_FLEETWIDE_SKIP_MACHINE=1 ...  # skip Maven/reset/hard-case proof (seed only)
set -eEuo pipefail

REPO="${DEMO_ARDEN_REPO:-COG-GTM/traderXCognitiondemos}"
SEED_BRANCH="${DEMO_ARDEN_SEED_BRANCH:-demo/arden-live-before}"
ESTATE_BRANCH=devin/1790864747-arden-finserv-estate
HOST_REPOS="COG-GTM/Securities-Trade-Processing-System COG-GTM/traderXCognitiondemos COG-GTM/Springboot-BankApp"
MIRROR=https://maven-central.storage-download.googleapis.com/maven2/
STAGE_IDS="settlement-instruction-service:SNYK-JAVA-ORGYAML-3152153 custody-gateway:SNYK-JAVA-ORGAPACHECOMMONS-6254296 fx-funding-service:SNYK-SECRET-ARDEN-0418 platform-terraform:SNYK-CC-ARDEN-TOKENTTL risk-lib:SNYK-CODE-PY-YAMLLOAD-0091"
DRY_RUN="${DEMO_FLEETWIDE_DRY_RUN:-}"
# Historical SEC-2026-0912 / audit / swarm PRs (Achal and other demos): never closed, whatever their head branch.
PROTECTED=" COG-GTM/Securities-Trade-Processing-System#$(seq -s ' COG-GTM/Securities-Trade-Processing-System#' 5 36) \
COG-GTM/traderXCognitiondemos#115 COG-GTM/traderXCognitiondemos#116 COG-GTM/traderXCognitiondemos#117 \
COG-GTM/Springboot-BankApp#$(seq -s ' COG-GTM/Springboot-BankApp#' 316 320) COG-GTM/Springboot-BankApp#327 COG-GTM/Springboot-BankApp#333 "
# Always-open example PRs the presenter brief links per beat (same commits as the measured live run's PRs).
EXAMPLES="COG-GTM/Securities-Trade-Processing-System#77:settlement-instruction-service(hard-case,video) \
COG-GTM/Securities-Trade-Processing-System#78:fx-funding-service COG-GTM/Securities-Trade-Processing-System#79:custody-gateway \
COG-GTM/traderXCognitiondemos#141:risk-lib COG-GTM/Springboot-BankApp#339:platform-terraform"
HERE=$(cd "$(dirname "$0")" && pwd)
ESTATE=$(cd "$HERE/../../.." && pwd)
GIT_ROOT=$(git -C "$HERE" rev-parse --show-toplevel)
WS="${FS_WORKSPACE:-$HOME/finserv-workspace}"
CURRENT_STEP="initializing"
TMP=""

trap 'status=$?; printf "SETUP FAILED: %s (exit %s)\n" "$CURRENT_STEP" "$status" >&2; exit "$status"' ERR
trap '[[ -n "$TMP" ]] && rm -rf "$TMP"; true' EXIT
step() { CURRENT_STEP="$1"; printf '==> %s\n' "$1"; }
fail() { CURRENT_STEP="$1"; false; }

step "Tools"
for t in git gh python3 curl java unzip; do command -v "$t" >/dev/null || fail "$t not installed"; done
gh auth status >/dev/null 2>&1 || fail "gh not authenticated"

step "Close security/* PRs from earlier live fleetwide runs"
for host in $HOST_REPOS; do
  gh pr list -R "$host" --base "$ESTATE_BRANCH" --state open --limit 200 \
     --json number,headRefName --jq '.[] | "\(.number)\t\(.headRefName)"' |
  while IFS=$'\t' read -r num head; do
    [[ "$head" == security/* ]] || continue
    [[ "$head" == *sec-2026-0912* ]] && continue
    [[ "$PROTECTED" == *" $host#$num "* ]] && { echo "    kept $host#$num (protected)"; continue; }
    if [[ -n "$DRY_RUN" ]]; then echo "    would close $host#$num ($head)"; continue; fi
    gh pr close -R "$host" "$num" --delete-branch \
       --comment "Closed by demo/live/fleetwide/setup-fleetwide.sh: reseeding the demo_fleetwide situation for a new live run." >/dev/null
    echo "    closed $host#$num ($head)"
  done
done

step "Always-open example PRs"
EXAMPLE_LINES=""
for e in $EXAMPLES; do
  ref=${e%%:*}; host=${ref%#*}; num=${ref#*#}
  state=$(gh api "repos/$host/pulls/$num" --jq 'if .merged then "merged" else .state end') || fail "example PR $ref not found"
  [[ "$state" == merged ]] && fail "example PR $ref was merged"
  if [[ "$state" != open && -z "$DRY_RUN" ]]; then gh pr reopen -R "$host" "$num" >/dev/null; echo "    reopened $ref"; fi
  echo "    $ref open (${e#*:})"
  EXAMPLE_LINES+="               https://github.com/$host/pull/$num  ${e#*:}"$'\n'
done

if [[ -z "${DEMO_FLEETWIDE_SKIP_MACHINE:-}" ]]; then
  step "Maven with the GCS mirror (Maven Central returns 429 to these VMs)"
  command -v mvn >/dev/null || sudo apt-get install -y -q maven >/dev/null
  if ! grep -q "$MIRROR" "$HOME/.m2/settings.xml" 2>/dev/null; then
    mkdir -p "$HOME/.m2"
    [[ -f "$HOME/.m2/settings.xml" ]] && cp "$HOME/.m2/settings.xml" "$HOME/.m2/settings.xml.bak.$(date +%s)"
    cat > "$HOME/.m2/settings.xml" <<XML
<settings><mirrors><mirror><id>gcs-central</id><mirrorOf>central</mirrorOf><url>$MIRROR</url></mirror></mirrors></settings>
XML
  fi

  step "Reset the estate workspace (FS_SOURCE_MODE=remote reset.sh)"
  FS_SOURCE_MODE=remote "$ESTATE/scripts/reset.sh" >/dev/null
  for v in 1.4.0 1.4.1 1.5.0; do
    [[ -f "$HOME/.m2/repository/com/ardencm/posttrade/core-dates/$v/core-dates-$v.jar" ]] || fail "core-dates $v not installed"
  done
  embedded() { unzip -p "$HOME/.m2/repository/com/ardencm/posttrade/core-dates/$1/core-dates-$1.jar" \
      META-INF/maven/org.yaml/snakeyaml/pom.properties 2>/dev/null | sed -n "s/^version=//p" || true; }
  if [[ "$(embedded 1.4.0)" != "1.33" || "$(embedded 1.4.1)" != "2.2" ]]; then
    step "Reseed core-dates releases (~/.m2 predates the embedded snakeyaml)"
    FS_SOURCE_MODE=remote FS_RESEED_CORE_DATES=1 "$ESTATE/scripts/reset.sh" >/dev/null
  fi
  [[ "$(embedded 1.4.0)" == "1.33" ]] || fail "core-dates 1.4.0 does not embed snakeyaml 1.33 (the finding would be a false positive)"
  [[ "$(embedded 1.4.1)" == "2.2" ]] || fail "core-dates 1.4.1 does not embed snakeyaml 2.2"

  step "Hard case: tag 64 contract green at BEFORE, red on core-dates 1.5.0, green on 1.4.1"
  TMP=$(mktemp -d)
  cp -r "$WS/settlement-instruction-service" "$TMP/sis"
  pin() { python3 - "$TMP/sis/pom.xml" "$1" <<'PY'
import re, sys
p, v = sys.argv[1], sys.argv[2]
s = open(p).read()
s = re.sub(r"(<artifactId>core-dates</artifactId>)(\s*<version>[^<]*</version>)?", rf"\1<version>{v}</version>", s, count=1)
open(p, "w").write(s)
PY
  }
  ( cd "$TMP/sis" && mvn -q -B test >/dev/null 2>&1 ) || fail "settlement-instruction-service is not green at BEFORE"
  pin 1.5.0
  if ( cd "$TMP/sis" && mvn -q -B test >"$TMP/v150.log" 2>&1 ); then fail "core-dates 1.5.0 unexpectedly passes the tag 64 contract"; fi
  grep -q 'tag 64 must match' "$TMP/v150.log" || fail "core-dates 1.5.0 failed for a reason other than tag 64"
  pin 1.4.1
  ( cd "$TMP/sis" && mvn -q -B test >/dev/null 2>&1 ) || fail "core-dates 1.4.1 does not pass the tag 64 contract"
  echo "    BEFORE green, 1.5.0 red (tag 64 = ISO date), 1.4.1 green"
fi

if [[ -n "$DRY_RUN" ]]; then
  step "Dry run: skipping seed-situation.sh"
  SEED_LINE="(dry run, not reseeded)"
else
  FOREIGN=$(gh pr list -R "$REPO" --base "$SEED_BRANCH" --state open --json number,headRefName \
    --jq '.[] | "#\(.number) \(.headRefName)"')
  if [[ -n "$FOREIGN" ]]; then
    # seed-situation.sh would close these; they belong to other demos' live runs on the shared seed branch.
    step "Seed branch has other demos' open PRs, keeping it as is (fleetwide children target the estate branch)"
    printf '    %s\n' "$FOREIGN"
    SEED_LINE="(seed kept: other demos have open PRs on it)"
  else
    step "Re-create the seed branch"
    "$HERE/../seed-situation.sh" >/dev/null
    SEED_LINE=""
  fi
fi

step "Seed branch fans the 5 stage findings out with the live playbook"
TMP=${TMP:-$(mktemp -d)}
git -C "$GIT_ROOT" fetch -q origin "$SEED_BRANCH"
git -C "$GIT_ROOT" archive FETCH_HEAD arden/finserv-estate | tar -x -C "$TMP"
SEEDED="$TMP/arden/finserv-estate"
python3 "$SEEDED/demo/scanner/normalize.py" --out "$TMP/findings.json" >/dev/null
python3 - "$TMP/findings.json" "$STAGE_IDS" <<'PY' || fail "stage findings missing from the seeded fixture"
import json, sys
found = {f"{f['repo']}:{f['scanner_id']}" for f in json.load(open(sys.argv[1]))}
missing = [s for s in sys.argv[2].split() if s not in found]
if missing: sys.exit("missing: " + ", ".join(missing))
print(f"    {len(found)} findings in the fixture, all 5 stage findings present")
PY
grep -q 'FS_REMEDIATION_PLAYBOOK' "$SEEDED/demo/scanner/fanout.py" || fail "seeded fanout.py cannot take the playbook id"

PROMPT1=$(awk '/^## Prompt 1: stage run/{f=1} f&&/^```text/{p=1;next} p&&/^```/{exit} p' "$HERE/PROMPTS.md")
[[ -n "$PROMPT1" ]] || fail "prompt 1 not found in PROMPTS.md"

cat <<BANNER

READY: demo_fleetwide live situation set up $SEED_LINE
  Seed branch: https://github.com/$REPO/tree/$SEED_BRANCH
  Where:       the setup session starts the live Devin session with prompt 1 as its first message
               (repos traderXCognitiondemos, Securities-Trade-Processing-System, Springboot-BankApp)
  Brief:       arden/finserv-estate/demo/live/fleetwide/PRESENTER-BRIEF.md (goal, tabs, beats, which PR per beat)
  Prompt 1 (full 17 variant and prompt 2 in arden/finserv-estate/demo/live/fleetwide/PROMPTS.md):
----------------------------------------------------------------
$PROMPT1
----------------------------------------------------------------
  Example PRs (always open, use when the live PR is not up yet):
$EXAMPLE_LINES  Talk track:  arden/finserv-estate/demo/live/fleetwide/TALK-TRACK.md
  Fallback:    (1) open the pre-run live session in TALK-TRACK.md "Fallback" and walk its burn-down and PRs
               (2) deterministic replay: git checkout $ESTATE_BRANCH && arden/finserv-estate/demo/run-demo1.sh
BANNER
