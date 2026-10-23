#!/usr/bin/env bash
# Reset the workspace and regenerate deterministic estate state.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/lib.sh"
ESTATE="$(cd "$HERE/.." && pwd)"
WS="${FS_WORKSPACE:-$HOME/finserv-workspace}"

trap 'status=$?; printf "\033[1;31mRESET FAILED at: %s\033[0m\n" "$BASH_COMMAND" >&2; exit "$status"' ERR

if ! command -v mvn >/dev/null 2>&1; then
  printf '\033[1;31mRESET FAILED at: mvn prerequisite\033[0m\n' >&2
  die "mvn is missing; run demo/setup-demo.sh first"
fi
fuser -k 3200/tcp 3210/tcp >/dev/null 2>&1 || true

log "clearing generated artifacts"
rm -rf "$ESTATE/demo/out"
mkdir -p "$ESTATE/demo/out"

if [[ ! -d "$ESTATE/repos" || "${FS_SOURCE_MODE:-}" == "remote" ]]; then
  log "remote-source mode: syncing COG-GTM repos per scripts/repo-map.tsv into $FS_SOURCES"
  sources_sync
fi

log "re-seeding workspace at $WS"
for r in $(repo_list); do
  rm -rf "$WS/$r"
done
core_dates_cached=1
for v in 1.4.0 1.4.1 1.5.0; do
  [[ -f "$HOME/.m2/repository/com/ardencm/posttrade/core-dates/$v/core-dates-$v.jar" ]] || core_dates_cached=0
done
[[ "$core_dates_cached" == "1" ]] && export FS_SKIP_CORE_DATES_BUILD=1
"$HERE/seed-workspace.sh"
unset FS_SKIP_CORE_DATES_BUILD

if [[ ! -f "$HOME/.m2/repository/com/ardencm/platform/common-java-bom/2026.3.0/common-java-bom-2026.3.0.pom" ]]; then
  (cd "$(source_dir common-java-bom)" && mvn -q install)
fi

# Demo 1 remediation targets that must be resolvable offline (the estate's Artifactory URL is a fixture).
if [[ ! -f "$HOME/.m2/repository/org/apache/commons/commons-compress/1.26.0/commons-compress-1.26.0.jar" ]]; then
  mvn -q dependency:get -Dartifact=org.apache.commons:commons-compress:1.26.0
fi
if ! command -v terraform >/dev/null && [[ ! -x "$HOME/.local/bin/terraform" ]]; then
  log "installing terraform 1.9.8 to ~/.local/bin (platform-terraform fmt check)"
  mkdir -p "$HOME/.local/bin"
  curl -sSfL -o /tmp/tf.zip https://releases.hashicorp.com/1.9.8/terraform_1.9.8_linux_amd64.zip
  unzip -o -q /tmp/tf.zip -d "$HOME/.local/bin"
fi
if ! python3 -c "import pytest, yaml" 2>/dev/null; then
  python3 -m pip install --user -q pytest PyYAML
fi

missing=()
for artifact in \
  "$HOME/.m2/repository/com/ardencm/posttrade/core-dates/1.4.0/core-dates-1.4.0.jar" \
  "$HOME/.m2/repository/com/ardencm/posttrade/core-dates/1.4.1/core-dates-1.4.1.jar" \
  "$HOME/.m2/repository/com/ardencm/posttrade/core-dates/1.5.0/core-dates-1.5.0.jar" \
  "$HOME/.m2/repository/com/ardencm/platform/common-java-bom/2026.3.0/common-java-bom-2026.3.0.pom" \
  "$HOME/.m2/repository/org/apache/commons/commons-compress/1.26.0/commons-compress-1.26.0.jar"; do
  [[ -f "$artifact" ]] || missing+=("$artifact")
done
command -v terraform >/dev/null 2>&1 || [[ -x "$HOME/.local/bin/terraform" ]] || missing+=("terraform")
python3 -c "import pytest, yaml" >/dev/null 2>&1 || missing+=("pytest and PyYAML")
node_major="$(node -p 'Number(process.versions.node.split(".")[0])' 2>/dev/null || printf '0')"
(( node_major >= 20 )) || missing+=("Node.js >=20")
if (( ${#missing[@]} != 0 )); then
  printf '\033[1;31mRESET FAILED at: post-seed prerequisite verification\033[0m\n' >&2
  die "reset prerequisites missing: ${missing[*]}"
fi

log "reset complete"
