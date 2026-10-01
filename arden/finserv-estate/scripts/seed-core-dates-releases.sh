#!/usr/bin/env bash
# Builds the three seeded core-dates releases (1.4.0 / 1.4.1 / 1.5.0) into the local Maven repo
# and, if a workspace repo exists, recreates the git history (tags + release/1.4 branch).
# See core-dates/.seed/README.md (in its source tree) for what each version changes.
set -euo pipefail

ESTATE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ESTATE_ROOT/scripts/lib.sh"

SRC="$(source_dir core-dates)"
WORK="${FS_WORKSPACE:-$HOME/finserv-workspace}/core-dates"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

apply_141() {  # security-only patch release
  sed -i 's#<version>1.4.0</version>#<version>1.4.1</version>#' pom.xml
  sed -i 's#<snakeyaml.version>1.33</snakeyaml.version>#<snakeyaml.version>2.2</snakeyaml.version>#' pom.xml
  sed -i 's#core-dates-1.4.0-cli.jar#core-dates-1.4.1-cli.jar#' README.md
  python3 - <<'EOF'
p='CHANGELOG.md'; s=open(p).read()
s=s.replace('# Changelog\n', '# Changelog\n\n## 1.4.1 — 2026-09-03 (security)\n- snakeyaml 1.33 -> 2.2 (CVE-2022-1471). No API or wire-format changes.\n',1)
open(p,'w').write(s)
EOF
}

apply_150() {  # minor release with a wire-format change
  sed -i 's#<version>1.4.1</version>#<version>1.5.0</version>#' pom.xml
  sed -i 's#core-dates-1.4.1-cli.jar#core-dates-1.5.0-cli.jar#' README.md
  # tag64 now emits ISO-8601 to "align with ISO 20022 SttlmDt" — breaks FIX LocalMktDate consumers.
  sed -i 's#DateTimeFormatter.ofPattern("yyyyMMdd")#DateTimeFormatter.ISO_LOCAL_DATE#' \
    src/main/java/com/ardencm/posttrade/dates/FixDates.java
  sed -i 's#FIX LocalMktDate format used by tag 64 (SettlDate) and tag 75 (TradeDate).#Date format for tag 64/75. Since 1.5.0 emits ISO-8601 (yyyy-MM-dd) so FIX and ISO 20022 payloads share one representation.#' \
    src/main/java/com/ardencm/posttrade/dates/FixDates.java
  sed -i 's#assertEquals("20271013", FixDates.tag64#assertEquals("2027-10-13", FixDates.tag64#' \
    src/test/java/com/ardencm/posttrade/dates/SettlementDatesTest.java
  python3 - <<'EOF'
p='CHANGELOG.md'; s=open(p).read()
s=s.replace('# Changelog\n', '# Changelog\n\n## 1.5.0 — 2026-09-17\n- snakeyaml 2.2 (carried from 1.4.1).\n- **Breaking:** `FixDates.tag64`/`tag75` now emit ISO-8601 (`yyyy-MM-dd`) instead of FIX LocalMktDate (`yyyyMMdd`), aligning with ISO 20022 `SttlmDt`. FIX 4.4 consumers must convert.\n',1)
open(p,'w').write(s)
EOF
}

build_install() {
  (cd "$1" && mvn -q -DskipTests install)
}

log "seeding core-dates 1.4.0 (baseline)"
rm -rf "$TMP/v140" && cp -r "$SRC" "$TMP/v140" && rm -rf "$TMP/v140/target" && build_install "$TMP/v140"

log "seeding core-dates 1.4.1 (patch: snakeyaml only)"
cp -r "$TMP/v140" "$TMP/v141" && (cd "$TMP/v141" && apply_141) && build_install "$TMP/v141"

log "seeding core-dates 1.5.0 (minor: snakeyaml + tag64 format change)"
cp -r "$TMP/v141" "$TMP/v150" && (cd "$TMP/v150" && apply_150) && build_install "$TMP/v150"

if [[ "${FS_SEED_GIT:-1}" == "1" ]]; then
  log "recreating core-dates git history in $WORK"
  rm -rf "$WORK" && mkdir -p "$WORK"
  cp -r "$TMP/v140/." "$WORK/" && rm -rf "$WORK/target"
  (
    cd "$WORK"
    git_init_ws
    git_commit "feature: core-dates 1.4.0 — TARGET2 calendar, XSWX 2027 holidays" "2026-02-11T10:00:00"
    git tag v1.4.0
    git checkout -q -b release/1.4
    apply_141; rm -rf target
    git_commit "bug: security — bump snakeyaml to 2.2 (CVE-2022-1471), release 1.4.1" "2026-09-03T15:20:00"
    git tag v1.4.1
    git checkout -q main
    git merge -q --no-ff release/1.4 -m "feature: merge release/1.4 into main"
    apply_150; rm -rf target
    git_commit "feature: 1.5.0 — emit ISO-8601 dates from FixDates to align with ISO 20022" "2026-09-17T11:05:00"
    git tag v1.5.0
  )
fi

log "core-dates releases available in ~/.m2:"
ls "$HOME/.m2/repository/com/ardencm/posttrade/core-dates/" | grep -E '^1\.' | sed 's/^/  /'
