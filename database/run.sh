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
: ${DATABASE_WEB_CONSOLE:=false}
: ${DATABASE_PG_SERVER:=false}

set +a

### Require an injected, non-default database password
if [ -z "$DATABASE_DBPASS" ]; then
  echo 'ERROR: DATABASE_DBPASS must be set to a strong password; refusing to start with default credentials.' >&2
  exit 1
fi
if [ "$DATABASE_DBPASS" = "sa" ] || [ "$DATABASE_DBPASS" = "$DATABASE_DBUSER" ] || [ ${#DATABASE_DBPASS} -lt 12 ]; then
  echo 'ERROR: DATABASE_DBPASS is too weak (must be at least 12 characters and not a default such as "sa").' >&2
  exit 1
fi

### Build server arguments
# The TCP server accepts remote connections because the Java services connect to it over the network.
# The PG server and H2 web console are disabled by default and, when enabled, only listen on localhost.
SERVER_ARGS=(-baseDir "$DATABASE_DATA_DIR" -tcp -tcpPort "$DATABASE_TCP_PORT" -tcpAllowOthers)
if [ "$DATABASE_PG_SERVER" = "true" ]; then
  SERVER_ARGS+=(-pg -pgPort "$DATABASE_PG_PORT")
fi
if [ "$DATABASE_WEB_CONSOLE" = "true" ]; then
  SERVER_ARGS+=(-web -webPort "$DATABASE_WEB_PORT")
fi

### Start the DB
echo "Data will be located in $DATABASE_DATA_DIR"
echo "Database name is $DATABASE_DBNAME"
echo 'Running schema setup script with log output to stdout below'
echo '---------------------------------------------------------------------------'
java -cp "$DATABASE_H2JAR" org.h2.tools.RunScript -url "jdbc:h2:$DATABASE_DATA_DIR/$DATABASE_DBNAME;DATABASE_TO_UPPER=TRUE;TRACE_LEVEL_SYSTEM_OUT=3" -user "$DATABASE_DBUSER" -password "$DATABASE_DBPASS" -script initialSchema.sql || exit 1
echo 'Starting Database Server - DB logs below'
echo '---------------------------------------------------------------------------'
exec java -cp "$DATABASE_H2JAR" org.h2.tools.Server "${SERVER_ARGS[@]}"
