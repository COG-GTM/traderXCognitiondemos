#!/bin/bash
# Regression tests for the H2 database hardening:
#  - run.sh refuses to start without a password or with the default 'sa' password
#  - run.sh no longer starts the PG listener or a remotely reachable web console
#  - docker-compose does not publish the database ports on the host
#  - the /db-web H2 console route is not exposed through the ingress
set -u
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
FAILS=0
pass() { echo "PASS: $1"; }
fail() { echo "FAIL: $1"; FAILS=$((FAILS+1)); }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/bin"
cat > "$WORK/bin/java" <<'STUB'
#!/bin/bash
echo "$*" >> "$JAVA_CALLS"
STUB
chmod +x "$WORK/bin/java"

run_db() {
  # usage: run_db VAR=value ... ; runs run.sh with a stub java, sets RC and CALLS
  export JAVA_CALLS="$WORK/calls"; : > "$JAVA_CALLS"
  (cd "$WORK" && env -u DATABASE_DBPASS -u DATABASE_DBPASS_FILE PATH="$WORK/bin:$PATH" JAVA_CALLS="$JAVA_CALLS" "$@" bash "$REPO/database/run.sh" > "$WORK/out" 2>&1)
  RC=$?
  CALLS="$(cat "$JAVA_CALLS")"
}

run_db
[ $RC -ne 0 ] && [ -z "$CALLS" ] && pass "refuses to start when DATABASE_DBPASS is unset" || fail "starts without DATABASE_DBPASS (rc=$RC)"

run_db DATABASE_DBPASS=sa
[ $RC -ne 0 ] && [ -z "$CALLS" ] && pass "refuses default password 'sa'" || fail "accepts default password 'sa' (rc=$RC)"

run_db DATABASE_DBPASS=SA
[ $RC -ne 0 ] && [ -z "$CALLS" ] && pass "refuses default password 'SA'" || fail "accepts default password 'SA' (rc=$RC)"

echo -n "file-Secret-123" > "$WORK/dbpass"
run_db DATABASE_DBPASS_FILE="$WORK/dbpass"
[ $RC -eq 0 ] && echo "$CALLS" | grep -q -- "-password file-Secret-123" && pass "reads password from DATABASE_DBPASS_FILE" || fail "DATABASE_DBPASS_FILE not honoured (rc=$RC)"

run_db DATABASE_DBPASS=Str0ng-Test-Pass
SERVER="$(echo "$CALLS" | grep -- "-jar")"
if [ $RC -eq 0 ] && [ -n "$SERVER" ]; then
  pass "starts with a strong password"
  echo "$SERVER" | grep -q -- "-pg" && fail "PG listener still started: $SERVER" || pass "PG listener not started"
  echo "$SERVER" | grep -q -- "-webAllowOthers" && fail "web console accepts remote clients: $SERVER" || pass "web console is local-only"
  echo "$SERVER" | grep -q -- "-tcpAllowOthers" && pass "TCP listener still available to internal services" || fail "TCP listener not reachable by services"
else
  fail "does not start with a strong password (rc=$RC): $(cat "$WORK/out")"
fi

if grep -q "db-web" "$REPO/gitops/base/ingress/ingress.yaml"; then
  fail "/db-web route still present in the k8s ingress"
elif grep -E "db-web" "$REPO/ingress/nginx.traderx.conf.template" | grep -q "proxy_pass"; then
  fail "/db-web still proxied to the database in nginx"
else
  pass "/db-web route not exposed through the ingress"
fi

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  if (cd "$REPO" && env -u DATABASE_DBPASS docker compose --env-file /dev/null config >/dev/null 2>&1); then
    fail "docker compose config succeeds without DATABASE_DBPASS"
  else
    pass "docker compose requires DATABASE_DBPASS"
  fi
  PORTS="$(cd "$REPO" && DATABASE_DBPASS=Str0ng-Test-Pass docker compose --env-file /dev/null config --format json | python3 -c 'import json,sys; print(json.load(sys.stdin)["services"]["database"].get("ports", []))')"
  [ "$PORTS" = "[]" ] && pass "database ports not published on the host" || fail "database ports published on the host: $PORTS"
else
  echo "SKIP: docker compose not available"
fi

[ $FAILS -eq 0 ] && echo "All database security tests passed" || echo "$FAILS test(s) failed"
exit $FAILS
