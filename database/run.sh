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
# The TCP listener is what the other services connect to, so it has to accept
# connections from other containers/pods. PG and the web console are opt-in.
: ${DATABASE_TCP_ALLOW_OTHERS:=true}
: ${DATABASE_ENABLE_PG:=false}
: ${DATABASE_PG_ALLOW_OTHERS:=false}
: ${DATABASE_ENABLE_WEB_CONSOLE:=false}
: ${DATABASE_WEB_ALLOW_OTHERS:=false}
set +a

DATABASE_MIN_PASSWORD_LENGTH=16
DATABASE_MIN_DISTINCT_CHARS=8

if [ -z "$DATABASE_DBUSER" ]; then
  echo "ERROR: DATABASE_DBUSER must not be empty." >&2
  exit 1
fi
if [ -z "$DATABASE_DBPASS" ]; then
  echo "ERROR: DATABASE_DBPASS is not set. Generate one (e.g. 'openssl rand -hex 24') and pass the same value to the database and every service." >&2
  exit 1
fi
if [ "${DATABASE_DBPASS,,}" = "sa" ] || [ "$DATABASE_DBPASS" = "$DATABASE_DBUSER" ]; then
  echo "ERROR: DATABASE_DBPASS must not be the default 'sa' or equal to DATABASE_DBUSER." >&2
  exit 1
fi
if [ ${#DATABASE_DBPASS} -lt $DATABASE_MIN_PASSWORD_LENGTH ]; then
  echo "ERROR: DATABASE_DBPASS must be at least $DATABASE_MIN_PASSWORD_LENGTH characters." >&2
  exit 1
fi
if [ $(printf '%s' "$DATABASE_DBPASS" | fold -w1 | sort -u | wc -l) -lt $DATABASE_MIN_DISTINCT_CHARS ]; then
  echo "ERROR: DATABASE_DBPASS must contain at least $DATABASE_MIN_DISTINCT_CHARS different characters; use a randomly generated value." >&2
  exit 1
fi

# -ifExists: remote clients may only open the existing traderx database, never create new ones.
SERVER_ARGS=(-baseDir "$DATABASE_DATA_DIR" -ifExists -tcp -tcpPort "$DATABASE_TCP_PORT")
[ "$DATABASE_TCP_ALLOW_OTHERS" = "true" ] && SERVER_ARGS+=(-tcpAllowOthers)
if [ "$DATABASE_ENABLE_PG" = "true" ]; then
  SERVER_ARGS+=(-pg -pgPort "$DATABASE_PG_PORT")
  [ "$DATABASE_PG_ALLOW_OTHERS" = "true" ] && SERVER_ARGS+=(-pgAllowOthers)
fi
if [ "$DATABASE_ENABLE_WEB_CONSOLE" = "true" ]; then
  # The H2 console lets anyone who reaches it open an in-memory database as admin,
  # so keep it on loopback unless remote access is explicitly requested.
  SERVER_ARGS+=(-web -webPort "$DATABASE_WEB_PORT" -webExternalNames "$DATABASE_WEB_HOSTNAMES")
  [ "$DATABASE_WEB_ALLOW_OTHERS" = "true" ] && SERVER_ARGS+=(-webAllowOthers)
fi

### Start the DB
echo "Data will be located in $DATABASE_DATA_DIR"
echo "Database name is $DATABASE_DBNAME"
echo 'Running schema setup script with log output to stdout below'
echo '---------------------------------------------------------------------------'
DATABASE_LOCAL_URL="jdbc:h2:$DATABASE_DATA_DIR/$DATABASE_DBNAME;IFEXISTS=TRUE"
h2_login() { java -cp "$DATABASE_H2JAR" org.h2.tools.Shell -url "$DATABASE_LOCAL_URL" -user "$DATABASE_DBUSER" -password "$1" -sql "$2" >/dev/null 2>&1; }
# Databases created by earlier versions use the old default password 'sa': move them to DATABASE_DBPASS.
if [ -f "$DATABASE_DATA_DIR/$DATABASE_DBNAME.mv.db" ] && ! h2_login "$DATABASE_DBPASS" "SELECT 1"; then
  SQ="'"
  if h2_login sa "ALTER USER $DATABASE_DBUSER SET PASSWORD '${DATABASE_DBPASS//$SQ/$SQ$SQ}'"; then
    echo "Changed the password of existing database user $DATABASE_DBUSER from the old default to DATABASE_DBPASS"
  else
    echo "ERROR: cannot log in to the existing database $DATABASE_DATA_DIR/$DATABASE_DBNAME with DATABASE_DBPASS. Use the password it was created with, or delete $DATABASE_DATA_DIR to recreate it." >&2
    exit 1
  fi
fi
java -cp "$DATABASE_H2JAR" org.h2.tools.RunScript -url "jdbc:h2:$DATABASE_DATA_DIR/$DATABASE_DBNAME;DATABASE_TO_UPPER=TRUE;TRACE_LEVEL_SYSTEM_OUT=3" -user "$DATABASE_DBUSER" -password "$DATABASE_DBPASS" -script initialSchema.sql || exit 1
echo 'Starting Database Server - DB logs below'
echo '---------------------------------------------------------------------------'
exec java -cp "$DATABASE_H2JAR" org.h2.tools.Server "${SERVER_ARGS[@]}"
