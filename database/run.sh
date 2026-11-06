#!/bin/bash

### Fetch variables
set -a
: ${DATABASE_TCP_PORT:=18082}
: ${DATABASE_PG_PORT:=18083}
: ${DATABASE_WEB_PORT:=18084}
: ${DATABASE_DBUSER:=sa}
: ${DATABASE_H2JAR:=./build/libs/database.jar}
: ${DATABASE_DATA_DIR:=./_data}
: ${DATABASE_DBNAME:=traderx}
: ${DATABASE_HOSTNAME:=$HOSTNAME}
: ${DATABASE_JDBC_URL:="jdbc:h2:tcp://$HOSTNAME:$DATABASE_TCP_PORT/$DATABASE_DBNAME"}
: ${DATABASE_WEB_HOSTNAMES:=$HOSTNAME}
: ${DATABASE_ENABLE_PG:=false}
: ${DATABASE_ENABLE_WEB_CONSOLE:=false}

set +a

### Refuse to start with a missing or well-known admin password
if [ -z "$DATABASE_DBPASS" ]; then
  echo 'ERROR: DATABASE_DBPASS is not set. Generate a strong password (e.g. `openssl rand -hex 24`) and pass it to the database and every service.' >&2
  exit 1
fi
if [ ${#DATABASE_DBPASS} -lt 16 ]; then
  echo "ERROR: DATABASE_DBPASS must be at least 16 characters long." >&2
  exit 1
fi
case "${DATABASE_DBPASS,,}" in
  *password*|*passw0rd*|*admin*|*changeme*|*secret*|*traderx*|*qwerty*|*letmein*|*123456*|*abcdef*)
    echo "ERROR: DATABASE_DBPASS contains a well-known/predictable value. Use a randomly generated password (e.g. \`openssl rand -hex 24\`)." >&2
    exit 1
    ;;
esac
if [ "$(printf '%s' "$DATABASE_DBPASS" | fold -w1 | sort -u | wc -l)" -lt 8 ]; then
  echo "ERROR: DATABASE_DBPASS is too repetitive. Use a randomly generated password." >&2
  exit 1
fi

### Start the DB
echo "Data will be located in $DATABASE_DATA_DIR"
echo "Database name is $DATABASE_DBNAME"
DB_URL="jdbc:h2:$DATABASE_DATA_DIR/$DATABASE_DBNAME;DATABASE_TO_UPPER=TRUE;IFEXISTS=TRUE"
if [ -f "$DATABASE_DATA_DIR/$DATABASE_DBNAME.mv.db" ] && \
   ! java -cp "$DATABASE_H2JAR" org.h2.tools.Shell -url "$DB_URL" -user "$DATABASE_DBUSER" -password "$DATABASE_DBPASS" -sql "SELECT 1" >/dev/null 2>&1; then
  # Existing database created by an older version of this script with the legacy sa/sa login:
  # rotate the admin password to DATABASE_DBPASS before continuing.
  echo "Existing database does not accept DATABASE_DBPASS; attempting to rotate the legacy default password"
  ESCAPED_PASS=${DATABASE_DBPASS//\'/\'\'}
  ESCAPED_USER=${DATABASE_DBUSER^^}
  ESCAPED_USER=${ESCAPED_USER//\"/\"\"}
  # Shell exits 0 on SQL errors, so verify the new password actually works afterwards.
  if ! java -cp "$DATABASE_H2JAR" org.h2.tools.Shell -url "$DB_URL" -user "$DATABASE_DBUSER" -password sa \
       -sql "ALTER USER \"$ESCAPED_USER\" SET PASSWORD '$ESCAPED_PASS'" >/dev/null 2>&1 || \
     ! java -cp "$DATABASE_H2JAR" org.h2.tools.Shell -url "$DB_URL" -user "$DATABASE_DBUSER" -password "$DATABASE_DBPASS" -sql "SELECT 1" >/dev/null 2>&1; then
    echo "ERROR: Existing database in $DATABASE_DATA_DIR rejects DATABASE_DBPASS and the legacy default. Use the password it was created with, or delete $DATABASE_DATA_DIR/$DATABASE_DBNAME.* (the schema is recreated on every start)." >&2
    exit 1
  fi
  echo "Admin password rotated"
fi
echo 'Running schema setup script with log output to stdout below'
echo '---------------------------------------------------------------------------'
java -cp "$DATABASE_H2JAR" org.h2.tools.RunScript -url "jdbc:h2:$DATABASE_DATA_DIR/$DATABASE_DBNAME;DATABASE_TO_UPPER=TRUE;TRACE_LEVEL_SYSTEM_OUT=3" -user "$DATABASE_DBUSER" -password "$DATABASE_DBPASS" -script initialSchema.sql || exit 1

# The TCP listener must accept connections from other hosts/containers (the Java services).
# -ifExists prevents remote clients from creating new databases.
SERVER_ARGS=(-baseDir "$DATABASE_DATA_DIR" -ifExists -tcp -tcpPort "$DATABASE_TCP_PORT" -tcpAllowOthers)

if [ "$DATABASE_ENABLE_PG" = "true" ]; then
  echo "WARNING: PostgreSQL wire listener enabled on port $DATABASE_PG_PORT"
  SERVER_ARGS+=(-pg -pgPort "$DATABASE_PG_PORT" -pgAllowOthers)
fi

if [ "$DATABASE_ENABLE_WEB_CONSOLE" = "true" ]; then
  echo "WARNING: H2 web console enabled on port $DATABASE_WEB_PORT - do not enable this outside local development"
  SERVER_ARGS+=(-web -webPort "$DATABASE_WEB_PORT" -webExternalNames "$DATABASE_WEB_HOSTNAMES" -webAllowOthers)
fi

echo 'Starting Database Server - DB logs below'
echo '---------------------------------------------------------------------------'
exec java -jar "$DATABASE_H2JAR" "${SERVER_ARGS[@]}"
