#!/usr/bin/env bash
# fill-brief.sh BEFORE_URL AFTER_URL SESSION_URL [LIVE_PR_URL] > PRESENTER-BRIEF.live.md
# Fills the presenter brief with this run's links. Rerun with LIVE_PR_URL once the live session opens its PR.
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
LIVE_PR="Devin's PR"
if [[ -n "${4:-}" ]]; then LIVE_PR="[Devin's PR #${4##*/}]($4)"; fi
sed -e "s|{{BEFORE_URL}}|$1|g" -e "s|{{AFTER_URL}}|$2|g" -e "s|{{SESSION_URL}}|$3|g" -e "s|{{LIVE_PR}}|$LIVE_PR|g" "$HERE/PRESENTER-BRIEF.md"
