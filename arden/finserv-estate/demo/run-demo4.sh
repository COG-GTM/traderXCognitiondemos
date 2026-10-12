#!/usr/bin/env bash
# Demo 4: audit readiness and traceability. FINOS CCC control map -> one fix PR + one risk decision -> evidence pack -> change trail.
#
#   demo/run-demo4.sh                 # offline; Devin audit log from the schema-accurate fixture
#   FS_SKIP_RESET=1 demo/run-demo4.sh
#   DEVIN_AUDIT_TOKEN=... DEVIN_ORG_ID=org_... demo/run-demo4.sh --live   # real Devin audit log (needs ViewAccountAuditLogs)
#
# Controls come only from CCC.Core v2025.10 (stable). The v2026.06-rc* catalogs are pre-releases and are not used.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/../scripts/lib.sh"
cd "$HERE/.."
[[ -n "${FS_SKIP_RESET:-}" ]] || demo/audit/reset.sh
TF="$(source_dir platform-terraform)"; WF="$(source_dir ci-shared-workflows)"

log "1/3 map platform-terraform + ci-shared-workflows to CCC.Core v2025.10"
python3 demo/audit/ccc_map.py --terraform "$TF" --workflows "$WF"

log "2/3 change trail for the CN13 fix (who, why, which Devin session, who approved)"
python3 demo/audit/trail.py "$@"

log "3/3 artifacts"
echo "  control map     demo/out/audit/control-map.{md,html,json}"
echo "  evidence pack   demo/out/audit/evidence-pack/README.md"
echo "  risk decision   demo/out/audit/evidence-pack/risk-decision-CN05.md   (the one human decision)"
echo "  traceability    demo/out/audit/traceability.md + devin-audit-log.json"
