#!/usr/bin/env bash
# Fails if a JDK is below the fixed build for Java4Shell (CVE-2026-114938).
# Usage:
#   check-jdk-version.sh                 # checks `java` on PATH
#   check-jdk-version.sh <version>       # checks a version string, e.g. 21.0.13.1+2-LTS
#   <cmd> | check-jdk-version.sh -       # checks `java -version` output read from stdin
#   check-jdk-version.sh --self-test
set -euo pipefail

# Minimum fixed version per feature line. Lines 8 and 11 are not affected.
declare -A MIN_FIXED=(
  [17]="17.0.21.1"
  [21]="21.0.13.1"
  [25]="25.0.5.1"
  [27]="27.0.1.1"
)

# Prints "feature.interim.update.patch" from a version string or `java -version` output.
normalize() {
  local raw ver
  raw="$1"
  ver=$(printf '%s\n' "$raw" | grep -oE 'version "[^"]+"' | head -n1 | sed -E 's/version "([^"]+)"/\1/' || true)
  [[ -z "$ver" ]] && ver=$(printf '%s\n' "$raw" | head -n1)
  ver=${ver%%[+_-]*}
  # Vendor builds such as Corretto 21.0.13.1.2 carry extra numeric fields; keep the first four.
  IFS=. read -r f i u p _ <<<"$ver"
  [[ "$f" == "1" ]] && { f="$i"; i=0; u=0; p=0; }   # legacy 1.8.0_xxx
  printf '%s.%s.%s.%s\n' "${f:-0}" "${i:-0}" "${u:-0}" "${p:-0}"
}

version_ge() {
  [[ "$(printf '%s\n%s\n' "$2" "$1" | sort -t. -k1,1n -k2,2n -k3,3n -k4,4n | head -n1)" == "$2" ]]
}

check() {
  local ver feature min
  ver=$(normalize "$1")
  feature=${ver%%.*}
  if [[ ! "$feature" =~ ^[0-9]+$ ]]; then
    echo "ERROR: could not parse a JDK version from: $1" >&2
    return 1
  fi
  if [[ "$feature" == 8 || "$feature" == 11 ]]; then
    echo "OK: JDK $ver (Java $feature is not affected by CVE-2026-114938)"
    return 0
  fi
  min=${MIN_FIXED[$feature]:-}
  if [[ -z "$min" ]]; then
    echo "FAIL: JDK $ver is on Java $feature, which has no fixed build for CVE-2026-114938; use 17.0.21.1+, 21.0.13.1+, 25.0.5.1+ or 27.0.1.1+" >&2
    return 1
  fi
  if version_ge "$ver" "$min"; then
    echo "OK: JDK $ver >= $min (fixed for CVE-2026-114938)"
  else
    echo "FAIL: JDK $ver is below $min and is vulnerable to Java4Shell (CVE-2026-114938)" >&2
    return 1
  fi
}

self_test() {
  local failures=0 v
  for v in 21.0.13.1+2-LTS 21.0.14+7 17.0.21.1 25.0.5.1 27.0.1.1 21.0.13.1.2 11.0.25 1.8.0_432 \
           'openjdk version "21.0.13.1" 2026-11-10 LTS'; do
    check "$v" >/dev/null 2>&1 || { echo "self-test: expected pass for '$v'" >&2; failures=$((failures + 1)); }
  done
  for v in 21.0.13+9 21.0.8 17.0.21 17.0.6 25.0.5 27.0.1 22.0.2 \
           'openjdk version "17.0.20.1" 2026-08-18'; do
    if check "$v" >/dev/null 2>&1; then echo "self-test: expected fail for '$v'" >&2; failures=$((failures + 1)); fi
  done
  [[ $failures -eq 0 ]] && echo "self-test passed"
  return $failures
}

case "${1:-}" in
  --self-test) self_test ;;
  -) check "$(cat)" ;;
  "") check "$(java -version 2>&1)" ;;
  *) check "$1" ;;
esac
