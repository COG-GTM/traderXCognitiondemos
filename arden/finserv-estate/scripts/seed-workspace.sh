#!/usr/bin/env bash
# Seed $FS_WORKSPACE (default ~/finserv-workspace) with one git repo per estate repo, each with a small
# deterministic history. The demos operate on the workspace, never on the pristine sources (repos/ locally,
# or the mapped COG-GTM repos — see lib.sh source_dir).
#
# Histories that matter to the demos:
#   client-portal               3 commits mirroring deploys/history.json; HEAD = the "false lead" perf deploy.
#   market-data-feed-contracts  v2.2 baseline → vendor notice (11 Sep) → auto-ingested v2.3 sample at 06:02Z on 25 Sep.
#   eod-pricing-batch           baseline → MD-1187 v2.3 fix (22 Sep) — proves the vendor change was known internally.
#   core-dates                  handled by seed-core-dates-releases.sh (tags v1.4.0 / v1.4.1 / v1.5.0).
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/lib.sh"
ESTATE="$(cd "$HERE/.." && pwd)"
WS="${FS_WORKSPACE:-$HOME/finserv-workspace}"
mkdir -p "$WS"

seed_plain() {  # seed_plain <repo> <date>
  local repo="$1" when="$2" dst="$WS/$1"
  rm -rf "$dst"; mkdir -p "$dst"
  cp -a "$(source_dir "$repo")/." "$dst/"
  rm -rf "$dst/target" "$dst/build" "$dst/dist" "$dst/node_modules" "$dst"/**/__pycache__ 2>/dev/null || true
  (cd "$dst" && git init -q -b main && git_commit "feature: import $repo" "$when")
}

for r in $(repo_list); do
  case "$r" in core-dates|client-portal|market-data-feed-contracts|eod-pricing-batch) continue;; esac
  seed_plain "$r" "2026-06-15T09:00:00Z"
done

# ---- client-portal: three deploys -------------------------------------------------------------------
dst="$WS/client-portal"; rm -rf "$dst"; mkdir -p "$dst"
cp -a "$(source_dir client-portal)/." "$dst/"
cd "$dst"
git init -q -b main
# 31.4.0 — no memoisation, no CHF fix
python3 - <<'PY'
import pathlib, re
p = pathlib.Path("src/valuation/valuation.js")
s = p.read_text()
s = s.replace("const rowCache = new Map();\n\n", "")
s = re.sub(r"// Memoised.*?\n", "", s)
s = s.replace("  const key = `${position.isin}|${quote.asof}`;\n  if (rowCache.has(key)) return rowCache.get(key);\n", "")
s = s.replace("  rowCache.set(key, row);\n  return row;", "  return row;")
s = s.replace("\nexport function clearCache() {\n  rowCache.clear();\n}\n", "\nexport function clearCache() {}\n")
p.write_text(s)
pathlib.Path("deploys/history.json").write_text('[\n  { "version": "31.4.0", "deployedAt": "2026-09-10T21:02:03Z", "commit": "41e9c7d", "ticket": "PORTAL-3270", "summary": "feature: settlement status widget", "deployer": "release-bot" }\n]\n')
PY
sed -i 's/"version": "31.4.2"/"version": "31.4.0"/' package.json
git_commit "feature: PORTAL-3270 settlement status widget" "2026-09-10T17:40:00Z"
# 31.4.1 — CHF formatting (touches server.js only)
sed -i 's/"version": "31.4.0"/"version": "31.4.1"/' package.json
sed -i "s|settle 2 business days after execution (T+2). US equities settle T+1.|settle 2 business days after execution (T+2). US equities settle T+1. Amounts shown in the account currency.|" src/server.js
git_commit "bug: PORTAL-3298 fix CHF number formatting in statements" "2026-09-17T16:12:00Z"
# 31.4.2 — memoise valuation rows (the false lead)
cp "$(source_dir client-portal)/src/valuation/valuation.js" src/valuation/valuation.js
cp "$(source_dir client-portal)/deploys/history.json" deploys/history.json
sed -i 's/"version": "31.4.1"/"version": "31.4.2"/' package.json
FS_GIT_NAME="Priya Natarajan" FS_GIT_EMAIL="priya.natarajan@ardencm.example" \
  git_commit "feature: PORTAL-3312 memoise valuation rows for positions table perf" "2026-09-24T18:31:00Z"
git tag -a v31.4.2 -m "deployed 2026-09-24T21:05:12Z by release-bot"

# ---- market-data-feed-contracts ----------------------------------------------------------------------
dst="$WS/market-data-feed-contracts"; rm -rf "$dst"; mkdir -p "$dst"
cp -a "$(source_dir market-data-feed-contracts)/." "$dst/"
cd "$dst"
git init -q -b main
mv ardenfeed/schema-v2.3.json /tmp/.fs-schema23 && mv ardenfeed/VENDOR-NOTICE-2026-09-11.md /tmp/.fs-notice
sed -i 's/schema: "2.3"/schema: "2.2"/; s/   # intraday valuation.*//' consumers.yaml
git_commit "feature: ArdenFeed v2.2 schema and consumer register" "2026-06-02T10:00:00Z"
mv /tmp/.fs-notice ardenfeed/VENDOR-NOTICE-2026-09-11.md
FS_GIT_NAME="Tom Okafor" FS_GIT_EMAIL="tom.okafor@ardencm.example" \
  git_commit "feature: file ArdenFeed v2.3 change notice (effective 25 Sep)" "2026-09-11T14:22:00Z"
cp "$(source_dir market-data-feed-contracts)/consumers.yaml" consumers.yaml
git_commit "feature: MD-1187 eod-pricing-batch moves to ArdenFeed 2.3" "2026-09-22T11:05:00Z"
mv /tmp/.fs-schema23 ardenfeed/schema-v2.3.json
mkdir -p samples
cp "$(source_dir client-portal)/fixtures/ardenfeed-v2.3.json" samples/latest-batch.json
FS_GIT_NAME="feed-ingest-bot" FS_GIT_EMAIL="feed-ingest-bot@ardencm.example" \
  git_commit "feature: auto-ingest ArdenFeed schema v2.3 + first live batch sample" "2026-09-25T06:02:14Z"

# ---- eod-pricing-batch --------------------------------------------------------------------------------
dst="$WS/eod-pricing-batch"; rm -rf "$dst"; mkdir -p "$dst"
cp -a "$(source_dir eod-pricing-batch)/." "$dst/"
rm -rf "$dst"/**/__pycache__ "$dst/.pytest_cache" 2>/dev/null || true
cd "$dst"
git init -q -b main
python3 - <<'PY'
import pathlib
p = pathlib.Path("pricing/snapshot.py"); s = p.read_text()
start = s.index("    px = q[\"px\"]"); end = s.index("    return Price(q[\"isin\"], float(px)")
p.write_text(s[:start] + s[end:].replace("float(px)", "float(q[\"px\"])"))
t = pathlib.Path("tests/test_snapshot.py"); ts = t.read_text()
ts = ts.replace('def test_parses_v22_and_v23_shapes', 'def test_parses_v22_shape')
ts = ts.replace('        {"isin": "CH0012032048", "px": {"v": 271.5, "ccy": "CHF"}, "asof": "2026-09-25T15:30:00Z"},\n', '')
ts = ts.replace('"schema": "2.3"', '"schema": "2.2"').replace("[12.34, 271.5]", "[12.34]").replace('    assert out[1].ccy == "CHF"\n', '')
t.write_text(ts)
PY
git_commit "feature: import eod-pricing-batch" "2026-06-15T09:00:00Z"
cp "$(source_dir eod-pricing-batch)/pricing/snapshot.py" pricing/snapshot.py
cp "$(source_dir eod-pricing-batch)/tests/test_snapshot.py" tests/test_snapshot.py
FS_GIT_NAME="Tom Okafor" FS_GIT_EMAIL="tom.okafor@ardencm.example" \
  git_commit "bug: MD-1187 handle ArdenFeed v2.3 px object" "2026-09-22T10:48:00Z"

# ---- core-dates ---------------------------------------------------------------------------------------
if [[ ! -d "$WS/core-dates/.git" || "${FS_RESEED_CORE_DATES:-0}" == "1" ]]; then
  "$HERE/seed-core-dates-releases.sh"
fi

log "workspace seeded at $WS ($(ls "$WS" | wc -l) repos)"
