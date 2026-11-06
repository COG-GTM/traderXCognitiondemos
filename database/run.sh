#!/bin/bash

### Fetch variables
set -a
: ${DATABASE_TCP_PORT:=18082}
: ${DATABASE_WEB_PORT:=18084}
: ${DATABASE_DBUSER:=sa}
: ${DATABASE_H2JAR:=./build/libs/database.jar}
: ${DATABASE_DATA_DIR:=./_data}
: ${DATABASE_DBNAME:=traderx}
: ${DATABASE_HOSTNAME:=$HOSTNAME}
: ${DATABASE_JDBC_URL:="jdbc:h2:tcp://$HOSTNAME:$DATABASE_TCP_PORT/$DATABASE_DBNAME"}
: ${DATABASE_WEB_HOSTNAMES:=$HOSTNAME}

set +a

### Require a real password (from DATABASE_DBPASS or a secret file in DATABASE_DBPASS_FILE)
if [ -z "$DATABASE_DBPASS" ] && [ -n "$DATABASE_DBPASS_FILE" ]; then
  if [ ! -r "$DATABASE_DBPASS_FILE" ]; then
    echo "ERROR: DATABASE_DBPASS_FILE ($DATABASE_DBPASS_FILE) is not readable" >&2
    exit 1
  fi
  DATABASE_DBPASS="$(cat "$DATABASE_DBPASS_FILE")"
fi
if [ -z "$DATABASE_DBPASS" ]; then
  echo "ERROR: DATABASE_DBPASS is not set. Provide the database password from a secret (DATABASE_DBPASS or DATABASE_DBPASS_FILE)." >&2
  exit 1
fi
if [ "$(printf '%s' "$DATABASE_DBPASS" | tr '[:upper:]' '[:lower:]')" = "sa" ]; then
  echo "ERROR: DATABASE_DBPASS must not be the default 'sa' password." >&2
  exit 1
fi
if [ "${#DATABASE_DBPASS}" -lt 12 ]; then
  echo "ERROR: DATABASE_DBPASS must be at least 12 characters long." >&2
  exit 1
fi

### Start the DB
echo "Data will be located in $DATABASE_DATA_DIR"
echo "Database name is $DATABASE_DBNAME"
echo 'Running schema setup script with log output to stdout below'
echo '---------------------------------------------------------------------------'
java -cp "$DATABASE_H2JAR" org.h2.tools.RunScript -url "jdbc:h2:$DATABASE_DATA_DIR/$DATABASE_DBNAME;DATABASE_TO_UPPER=TRUE;TRACE_LEVEL_SYSTEM_OUT=3" -user "$DATABASE_DBUSER" -password "$DATABASE_DBPASS" -script initialSchema.sql
echo 'Starting Database Server - DB logs below'
echo '---------------------------------------------------------------------------'
# TCP is reachable by the other services on the internal network only (no host port mapping).
# The web console is local to the container: no -webAllowOthers and no ingress route.
exec java -jar "$DATABASE_H2JAR" -baseDir "$DATABASE_DATA_DIR" -tcp -tcpPort "$DATABASE_TCP_PORT" -tcpAllowOthers -web -webPort "$DATABASE_WEB_PORT" -webExternalNames "$DATABASE_WEB_HOSTNAMES"
