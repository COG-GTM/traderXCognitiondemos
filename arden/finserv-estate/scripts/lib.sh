# Shared helpers for estate scripts. Source, do not execute.

# The estate targets Java 17. Prefer FS_JAVA_HOME, else a 17+ JDK if the ambient JAVA_HOME is older.
_java_major() { "$1/bin/java" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1; }
if [[ -n "${FS_JAVA_HOME:-}" ]]; then
  export JAVA_HOME="$FS_JAVA_HOME"
elif [[ -z "${JAVA_HOME:-}" || "$(_java_major "$JAVA_HOME")" -lt 17 ]]; then
  for cand in /usr/lib/jvm/java-17-openjdk-amd64 /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/temurin-17-jdk-amd64; do
    [[ -x "$cand/bin/java" ]] && { export JAVA_HOME="$cand"; break; }
  done
fi
export PATH="$JAVA_HOME/bin:$PATH"

log()  { printf '\033[1;36m[estate]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[estate]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[estate]\033[0m %s\n' "$*" >&2; exit 1; }

# git_commit "<message>" "<iso-datetime>"  — commit everything in cwd with a fixed author/date.
git_commit() {
  local msg="$1" when="${2:-}"
  git add -A
  if [[ -n "$when" ]]; then
    GIT_AUTHOR_DATE="$when" GIT_COMMITTER_DATE="$when" \
      git -c user.name="${FS_GIT_NAME:-Arden Platform Bot}" -c user.email="${FS_GIT_EMAIL:-platform-bot@ardencm.example}" \
      commit -q -m "$msg" --allow-empty
  else
    git -c user.name="${FS_GIT_NAME:-Arden Platform Bot}" -c user.email="${FS_GIT_EMAIL:-platform-bot@ardencm.example}" \
      commit -q -m "$msg" --allow-empty
  fi
}

# ---- Sources: where the pristine estate repos come from ---------------------------------------------
# Local mode:  $ESTATE/repos/<repo> exists (the tarball layout).
# Remote mode: $ESTATE/repos is absent (the kit as landed in COG-GTM/traderXCognitiondemos); each estate repo
#              is a subdirectory of an existing COG-GTM repo, per scripts/repo-map.tsv, cloned into $FS_SOURCES.
FS_SOURCES="${FS_SOURCES:-$HOME/finserv-sources}"
_ESTATE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_MAP="$_ESTATE_ROOT/scripts/repo-map.tsv"

# repo_map_field <repo> <2=github-repo|3=ref|4=subdir>
repo_map_field() {
  local v
  v="$(awk -F'\t' -v r="$1" -v c="$2" '$0 !~ /^#/ && $1 == r { print $c; exit }' "$REPO_MAP")"
  [[ -n "$v" ]] || die "$1 is not in $REPO_MAP"
  if [[ "$2" == 3 && -n "${FS_SOURCE_REF:-}" ]]; then v="$FS_SOURCE_REF"; fi
  printf '%s\n' "$v"
}
repo_github() { repo_map_field "$1" 2; }
repo_ref()    { repo_map_field "$1" 3; }
repo_subdir() { repo_map_field "$1" 4; }
repo_clone_dir() { printf '%s/%s\n' "$FS_SOURCES" "${1##*/}"; }   # <github-repo> -> local clone path

# source_dir <repo> — pristine source tree for an estate repo (local repos/ wins when present).
source_dir() {
  if [[ -d "$_ESTATE_ROOT/repos/$1" ]]; then printf '%s\n' "$_ESTATE_ROOT/repos/$1"; return; fi
  printf '%s/%s\n' "$(repo_clone_dir "$(repo_github "$1")")" "$(repo_subdir "$1")"
}

# sources_sync — clone/fast-forward every mapped COG-GTM repo at its ref (remote mode only).
sources_sync() {
  mkdir -p "$FS_SOURCES"
  awk -F'\t' '$0 !~ /^#/ && NF >= 4 { print $2 "\t" $3 }' "$REPO_MAP" | sort -u | while IFS=$'\t' read -r gh ref; do
    [[ -n "${FS_SOURCE_REF:-}" ]] && ref="$FS_SOURCE_REF"
    local dir; dir="$(repo_clone_dir "$gh")"
    if [[ ! -d "$dir/.git" ]]; then
      log "cloning https://github.com/$gh ($ref) -> $dir"
      git clone -q --branch "$ref" "https://github.com/$gh.git" "$dir"
    else
      log "updating $dir -> origin/$ref"
      git -C "$dir" fetch -q origin "$ref" && git -C "$dir" checkout -q "$ref" && git -C "$dir" merge -q --ff-only "origin/$ref"
    fi
  done
}

# repo_list — the 20 estate repos in dependency order.
repo_list() {
  cat <<'EOF'
common-java-bom
core-dates
iso20022-fix-messages
market-data-feed-contracts
allocation-service
confirmation-service
settlement-instruction-service
custody-gateway
fx-funding-service
stock-loan-recall-service
corporate-actions-service
risk-lib
recon-job
client-portal
eod-pricing-batch
legacy-stored-procs
batch-scheduler-config
legacy-position-keeper
platform-terraform
ci-shared-workflows
EOF
}
