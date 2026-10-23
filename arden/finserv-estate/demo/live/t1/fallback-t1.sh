#!/usr/bin/env bash
# demo_t1 fallback: run the kit's deterministic inventory, tranche-1 migration and parity on this machine.
# Use only if the live Devin run stalls. These are the presenter's reference numbers, not Devin's output.
set -eEuo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
KIT=$(cd "$HERE/../../.." && pwd)
CURRENT_STEP="initializing"
trap 'status=$?; printf "FALLBACK FAILED: %s (exit %s)\n" "$CURRENT_STEP" "$status" >&2; exit "$status"' ERR
step() { CURRENT_STEP="$1"; printf '==> %s\n' "$1"; }
cd "$KIT"
step "Reset workspace";            FS_SOURCE_MODE=remote scripts/reset.sh
step "Inventory";                  python3 demo/t1/inventory.py
step "Migrate tranche 1";          python3 demo/t1/migrate.py --tranche 1
step "Parity vs golden files";     python3 demo/t1/parity.py
echo
echo "FALLBACK READY: open $KIT/demo/reports/demo2-t1-inventory.md, demo2-parity.html, demo2-migration-report.md"
