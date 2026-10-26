#!/usr/bin/env bash
# demo_t1 presenter setup: prepare this machine, seed demo/t1-live-before, print READY + prompt 1 + fallback.
# Run from a checkout of the kit branch (devin/1792724600-demo-arden-live). Prints READY only if every step passed.
set -eEuo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
KIT=$(cd "$HERE/../../.." && pwd)
REPO="${DEMO_ARDEN_REPO:-COG-GTM/traderXCognitiondemos}"
T1_SEED="${DEMO_T1_SEED_BRANCH:-demo/t1-live-before}"
CURRENT_STEP="initializing"
trap 'status=$?; printf "SETUP FAILED: %s (exit %s). Not ready.\n" "$CURRENT_STEP" "$status" >&2; exit "$status"' ERR
step() { CURRENT_STEP="$1"; printf '==> %s\n' "$1"; }
fail() { CURRENT_STEP="$1"; false; }

step "Tools: git gh python3 java javac mvn"
for t in git gh python3 java javac; do command -v "$t" >/dev/null || fail "$t not installed"; done
if ! command -v mvn >/dev/null; then sudo apt-get install -y -q maven >/dev/null; fi
java -version 2>&1 | grep -Eq 'version "(17|2[0-9])' || fail "Java 17+ required"
python3 -c 'import yaml' 2>/dev/null || python3 -m pip install -q pyyaml || fail "pyyaml missing"

step "Maven mirror (Maven Central returns 429 on Devin VMs)"
mkdir -p ~/.m2
if ! grep -q maven-central.storage-download.googleapis.com ~/.m2/settings.xml 2>/dev/null; then
  [[ -f ~/.m2/settings.xml ]] && cp ~/.m2/settings.xml ~/.m2/settings.xml.bak
  cat > ~/.m2/settings.xml <<'XML'
<settings><mirrors><mirror><id>gcs-central</id><mirrorOf>central</mirrorOf><url>https://maven-central.storage-download.googleapis.com/maven2/</url></mirror></mirrors></settings>
XML
fi

step "Seed $T1_SEED"
"$HERE/seed-t1.sh"

step "Reset the estate workspace to BEFORE (~/finserv-workspace)"
FS_SOURCE_MODE=remote "$KIT/scripts/reset.sh"

step "Verify BEFORE state"
WS="${FS_WORKSPACE:-$HOME/finserv-workspace}"
[[ $(find "$WS" -mindepth 1 -maxdepth 1 -type d | wc -l) -ge 20 ]] || fail "workspace has fewer than 20 repos"
grep -q '<core-dates.version>1.4.0<' "$WS/common-java-bom/pom.xml" || fail "BOM is not pinned to core-dates 1.4.0"
ls ~/.m2/repository/com/ardencm/posttrade/core-dates/1.4.0/core-dates-1.4.0-cli.jar >/dev/null || fail "baseline CLI jar not installed"
git -C "$(git -C "$HERE" rev-parse --show-toplevel)" ls-remote --exit-code origin "refs/heads/$T1_SEED" >/dev/null || fail "seed branch missing on origin"

cat <<BANNER

READY: demo_t1 (T+1 settlement migration) seeded
  Seed branch: https://github.com/$REPO/tree/$T1_SEED
  Presenter sheet (pin it): https://github.com/$REPO/blob/devin/1792724600-demo-arden-live/arden/finserv-estate/demo/live/t1/PRESENTER.md
  Open Devin (normal mode), repos traderXCognitiondemos + Securities-Trade-Processing-System + Springboot-BankApp, paste prompt 1:
----------------------------------------------------------------------
$(sed -n '/^<!-- prompt1 -->$/,/^<!-- \/prompt1 -->$/p' "$HERE/PROMPTS.md" | sed '1,2d;$d' | sed '$d')
----------------------------------------------------------------------
  Talk track:  arden/finserv-estate/demo/live/t1/TALK-TRACK.md
  Your check:  python3 arden/finserv-estate/demo/t1/parity.py --migrated <jar from Devin's core-dates PR>
  Fallback:    arden/finserv-estate/demo/live/t1/fallback-t1.sh   (kit scripts, same numbers, ~2 min)
BANNER
