# FINOS | TraderX Sample Trading App | Database

![DEV Only Warning](https://badgen.net/badge/warning/not-for-production/red) ![Local Dev Machine Supported](http://badgen.net/badge/local-dev/supported/green)

## Introduction

This is designed to play the role of a SQL database that runs standalone as part of an example environment. The other processes in this environment interact with this via SQL / JDBC drivers and therefore this component can be swapped out in other iterations of this environment and replaced with a robust and productionizable RDBMS.

This uses H2 Java-based database as a standalone server and initializes with an empty SQL schema every time it is started.

## Default Port Numbers
| Protocol | Port Number | Reachable from |
| :--- | :--- | :--- |
| TCP | 18082 | other hosts/containers (password required) |
| PG | 18083 | localhost only, disabled unless `DATABASE_PG_ENABLED=true` |
| HTTP (web console) | 18084 | localhost only, disabled unless `DATABASE_WEB_CONSOLE_ENABLED=true` |

None of these ports are published on the Docker host, and there is no `/db-web` ingress or nginx route.

## Credentials

`run.sh` refuses to start without a strong application password. There is no `sa`/`sa` default.

| Variable | Default | Purpose |
| :--- | :--- | :--- |
| `DATABASE_DBPASS` | *(none, required, min 12 chars)* | Password of the application user that the Java services use |
| `DATABASE_DBUSER` | `traderx_app` | Application user. It only has `SELECT, INSERT, UPDATE, DELETE` on schema `PUBLIC`: no `ADMIN` rights (so `CREATE ALIAS` / Java code execution is refused) and no DDL |
| `DATABASE_ADMIN_USER` | `traderx_admin` | Schema owner, used only to run `initialSchema.sql` |
| `DATABASE_ADMIN_PASS` | random per start | Schema owner password. Set it only if you need admin access (e.g. via the local web console) |

The account, position and trade-processor services read the same `DATABASE_DBUSER` / `DATABASE_DBPASS` variables (`spring.datasource.password` has no default either).

Remote database creation is disabled, so TCP clients cannot open new or in-memory (`jdbc:h2:mem:`) databases.

## Connecting to this database remotely
Use `$DATABASE_TCP_PORT` with the JDBC URL `jdbc:h2:tcp://<host>:18082/traderx` and the `$DATABASE_DBUSER` / `$DATABASE_DBPASS` credentials.

## Connecting to the web console
The web console is off by default. For local debugging, start the database with `DATABASE_WEB_CONSOLE_ENABLED=true` and, if you need schema-owner access, an explicit `DATABASE_ADMIN_PASS`. The console only accepts connections from localhost inside the database container/pod, for example:

```shell
kubectl -n traderx port-forward deploy/database-deployment 18084:18084
```

Then open http://localhost:18084 and use the JDBC URL `jdbc:h2:./traderx` (`-baseDir` is already set to `./_data`).

## Output Directory
Data is stored in the local `./_data` directory from where the script is run. This is .gitignore'd 

## Building

This builds in gradle to retrieve H2.

```shell
$> gradle build
```

## Running on Linux

This is desinged to run on Linux but can easily run on Windows as well. It launches a DB Script runner to pre-populate the database schema- delayed by 20 seconds, and then runs the database server in the foreground.

Currently the run script has environment variables defined for versions of H2 and common ports. These should be altered (or made configurable outside the script) as the environment matures.

To launch, set the application password and run:
```shell
$> export DATABASE_DBPASS='<strong-password>'
$> ./run.sh
```

## Running from your local Win10 Machine

You CAN run this on windows, with the help of Powershell.  All you need to do is have Java on your path, and then enter bash to run the script.  First launch a Terminal/Command Console in Windows.

```
>bash
>$ . run.sh
```

You will see the following output

```
TCP server running at tcp://[your IP]:18082 (others can connect)
```
