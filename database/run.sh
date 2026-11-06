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
case "$DATABASE_DBPASS" in
  sa|SA|password|admin|changeme)
    echo "ERROR: DATABASE_DBPASS is set to a well-known default. Use a strong, randomly generated password." >&2
    exit 1
    ;;
esac
if [ ${#DATABASE_DBPASS} -lt 12 ]; then
  echo "ERROR: DATABASE_DBPASS must be at least 12 characters long." >&2
  exit 1
fi

### Start the DB
echo "Data will be located in $DATABASE_DATA_DIR"
echo "Database name is $DATABASE_DBNAME"
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
