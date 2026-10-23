#!/usr/bin/env bash
# Demo 4 reset: clear demo/out/audit and put the platform-terraform source back at the estate "before" ref.
# Only touches the Springboot-BankApp clone in $FS_SOURCES (Demo 4 reads nothing else). Target: well under 2 minutes.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/../../scripts/lib.sh"
rm -rf "$HERE/../out/audit"
gh="$(repo_github platform-terraform)"; ref="$(repo_ref platform-terraform)"; dir="$(repo_clone_dir "$gh")"
if [[ -d "$_ESTATE_ROOT/repos/platform-terraform" ]]; then
  log "local-source mode: $_ESTATE_ROOT/repos/platform-terraform"
elif [[ ! -d "$dir/.git" ]]; then
  log "cloning https://github.com/$gh ($ref) -> $dir"
  mkdir -p "$FS_SOURCES"; git clone -q --depth 1 --branch "$ref" "https://github.com/$gh.git" "$dir"
else
  log "resetting $dir -> origin/$ref"
  git -C "$dir" fetch -q --depth 1 origin "$ref" && git -C "$dir" checkout -q -B "$ref" FETCH_HEAD && git -C "$dir" reset -q --hard FETCH_HEAD
fi
python3 -c "import yaml" 2>/dev/null || die "PyYAML missing: run demo/setup-demo4.sh first"
log "demo 4 reset complete"
