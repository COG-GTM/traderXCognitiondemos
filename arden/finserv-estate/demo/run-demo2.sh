#!/usr/bin/env bash
# Demo 2 — regulatory-driven T+1 migration (EU/UK/CH, go-live 11 Oct 2027), end to end, deterministic.
#
#   demo/run-demo2.sh                # inventory → migration plan → tranche 1 on branches → golden-file parity → escalations
#   FS_SKIP_RESET=1 demo/run-demo2.sh          # keep the current workspace (re-run on top of an existing state)
#
# Nothing is merged. Human decisions (calendar authority, ex-date regime, dual listings) are reported, not made.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/../scripts/lib.sh"
cd "$HERE/.."
export PATH="$HOME/.local/bin:$PATH"

[[ -n "${FS_SKIP_RESET:-}" && -d "${FS_WORKSPACE:-$HOME/finserv-workspace}/core-dates/.git" ]] || scripts/reset.sh

log "1/4 inventory: every settlement-cycle assumption, cited, with CODEOWNERS and a tranche"
python3 demo/t1/inventory.py

log "2/4 tranche 1: core-dates → settlement-instruction-service → iso20022-fix-messages (branches + tests + PR artifacts)"
python3 demo/t1/migrate.py --tranche 1

log "3/4 parity: golden file through old and new core-dates; classify every difference"
python3 demo/t1/parity.py

log "4/4 artifacts"
echo "  inventory       demo/reports/demo2-t1-inventory.md        (file/line citations + owners)"
echo "  migration plan  demo/reports/demo2-t1-migration-plan.md   (tranches, decisions owed)"
echo "  PR artifacts    demo/out/t1/prs/<repo>/{PR.md,change.patch}  (+ attempt-1-from-main.patch for the deviation)"
echo "  parity          demo/reports/demo2-parity.html            (old vs new, escalations)"
echo "  report          demo/reports/demo2-migration-report.md    (leave-behind)"
echo "  branches        t1/tranche-1-* in ${FS_WORKSPACE:-$HOME/finserv-workspace}/{core-dates,settlement-instruction-service,iso20022-fix-messages}"
