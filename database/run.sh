#!/bin/bash
set -euo pipefail

### Fetch variables
set -a
: ${DATABASE_TCP_PORT:=18082}
: ${DATABASE_PG_PORT:=18083}
: ${DATABASE_WEB_PORT:=18084}
: ${DATABASE_DBUSER:=traderx_app}
: ${DATABASE_DBPASS:=}
: ${DATABASE_ADMIN_USER:=traderx_admin}
: ${DATABASE_ADMIN_PASS:=}
: ${DATABASE_H2JAR:=./build/libs/database.jar}
: ${DATABASE_DATA_DIR:=./_data}
: ${DATABASE_DBNAME:=traderx}
: ${DATABASE_HOSTNAME:=${HOSTNAME:-localhost}}
: ${DATABASE_JDBC_URL:="jdbc:h2:tcp://$DATABASE_HOSTNAME:$DATABASE_TCP_PORT/$DATABASE_DBNAME"}
: ${DATABASE_PG_ENABLED:=false}
: ${DATABASE_WEB_CONSOLE_ENABLED:=false}
set +a

fail() { echo "ERROR: $*" >&2; exit 1; }
random_secret() { head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n'; }
sql_quote() { printf "'%s'" "${1//\'/\'\'}"; }

### Validate credentials: no well-known defaults, app user must not be the admin
for name in DATABASE_DBUSER DATABASE_ADMIN_USER; do
  [[ "${!name}" =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || fail "$name must match ^[A-Za-z][A-Za-z0-9_]{0,63}\$"
done
[[ "${DATABASE_DBUSER^^}" != "${DATABASE_ADMIN_USER^^}" ]] || fail "DATABASE_DBUSER must differ from DATABASE_ADMIN_USER"
[[ "${DATABASE_DBUSER^^}" != "SA" ]] || fail "DATABASE_DBUSER must not be 'sa'"
[[ -n "$DATABASE_DBPASS" ]] || fail "DATABASE_DBPASS must be set (application DB password; no default)"
[[ ${#DATABASE_DBPASS} -ge 12 ]] || fail "DATABASE_DBPASS must be at least 12 characters"
[[ "${DATABASE_DBPASS,,}" != "sa" && "${DATABASE_DBPASS,,}" != "password" ]] || fail "DATABASE_DBPASS must not be a well-known default"
if [[ -z "$DATABASE_ADMIN_PASS" ]]; then
  DATABASE_ADMIN_PASS=$(random_secret)
fi
[[ ${#DATABASE_ADMIN_PASS} -ge 12 ]] || fail "DATABASE_ADMIN_PASS must be at least 12 characters"
[[ "$DATABASE_ADMIN_PASS" != "$DATABASE_DBPASS" ]] || fail "DATABASE_ADMIN_PASS must differ from DATABASE_DBPASS"
DATABASE_TCP_MGMT_PASS=$(random_secret)

### Start the DB
# The schema is recreated on every start, so start from a fresh database file
# to (re)create the admin/app users with the passwords supplied for this run.
echo "Data will be located in $DATABASE_DATA_DIR"
echo "Database name is $DATABASE_DBNAME"
mkdir -p "$DATABASE_DATA_DIR"
rm -f "$DATABASE_DATA_DIR/$DATABASE_DBNAME.mv.db" "$DATABASE_DATA_DIR/$DATABASE_DBNAME.trace.db"

BOOTSTRAP_SQL=$(mktemp "$DATABASE_DATA_DIR/.bootstrap.XXXXXX.sql")
trap 'rm -f "$BOOTSTRAP_SQL"' EXIT
chmod 600 "$BOOTSTRAP_SQL"
{
  cat initialSchema.sql
  echo
  echo "CREATE USER $DATABASE_DBUSER PASSWORD $(sql_quote "$DATABASE_DBPASS");"
  # Least privilege: DML only. No ADMIN (blocks CREATE ALIAS / Java code execution) and no DDL.
  echo "GRANT SELECT, INSERT, UPDATE, DELETE ON SCHEMA PUBLIC TO $DATABASE_DBUSER;"
} > "$BOOTSTRAP_SQL"

echo 'Running schema setup script with log output to stdout below'
echo '---------------------------------------------------------------------------'
java -cp "$DATABASE_H2JAR" org.h2.tools.RunScript \
  -url "jdbc:h2:$DATABASE_DATA_DIR/$DATABASE_DBNAME;DATABASE_TO_UPPER=TRUE" \
  -user "$DATABASE_ADMIN_USER" -password "$DATABASE_ADMIN_PASS" -script "$BOOTSTRAP_SQL"
rm -f "$BOOTSTRAP_SQL"
trap - EXIT

# TCP is the only listener reachable from other hosts (the Java services connect to it).
# Remote database creation stays disabled (no -ifNotExists), so jdbc:h2:mem:/new files are refused.
SERVER_ARGS=(-baseDir "$DATABASE_DATA_DIR" -tcp -tcpPort "$DATABASE_TCP_PORT" -tcpAllowOthers -tcpPassword "$DATABASE_TCP_MGMT_PASS")
if [[ "$DATABASE_PG_ENABLED" == "true" ]]; then
  echo "PG server enabled on port $DATABASE_PG_PORT (localhost only)"
  SERVER_ARGS+=(-pg -pgPort "$DATABASE_PG_PORT")
fi
if [[ "$DATABASE_WEB_CONSOLE_ENABLED" == "true" ]]; then
  echo "H2 web console enabled on port $DATABASE_WEB_PORT (localhost only)"
  SERVER_ARGS+=(-web -webPort "$DATABASE_WEB_PORT")
fi

echo 'Starting Database Server - DB logs below'
echo '---------------------------------------------------------------------------'
exec java -cp "$DATABASE_H2JAR" org.h2.tools.Server "${SERVER_ARGS[@]}"
