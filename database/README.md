# FINOS | TraderX Sample Trading App | Database

![DEV Only Warning](https://badgen.net/badge/warning/not-for-production/red) ![Local Dev Machine Supported](http://badgen.net/badge/local-dev/supported/green)

## Introduction

This is designed to play the role of a SQL database that runs standalone as part of an example environment. The other processes in this environment interact with this via SQL / JDBC drivers and therefore this component can be swapped out in other iterations of this environment and replaced with a robust and productionizable RDBMS.

This uses H2 Java-based database as a standalone server and initializes with an empty SQL schema every time it is started.

## Credentials
The admin user is `$DATABASE_DBUSER` (default `sa`) with password `$DATABASE_DBPASS`. There is no default password: `run.sh` exits if `DATABASE_DBPASS` is unset, shorter than 16 characters, has fewer than 8 distinct characters, is `sa`, or equals the user name. Pass the same value to every service that connects to the database.

An existing `_data` database created by an earlier version with the old default password `sa` is switched to `DATABASE_DBPASS` on the next start. If the existing database uses some other password, `run.sh` exits; start it with that password, or delete `_data` to recreate it.

```shell
export DATABASE_DBPASS=$(openssl rand -hex 24)
```

## Default Port Numbers
| Protocol | Port Number | Enabled by |
| :--- | :--- | :--- |
| TCP | 18082 | always (`DATABASE_TCP_ALLOW_OTHERS=true` by default so other containers can connect) |
| PG | 18083 | `DATABASE_ENABLE_PG=true` (loopback only unless `DATABASE_PG_ALLOW_OTHERS=true`) |
| HTTP | 18084 | `DATABASE_ENABLE_WEB_CONSOLE=true` (loopback only unless `DATABASE_WEB_ALLOW_OTHERS=true`) |

The server runs with `-ifExists`, so clients can only open the existing `traderx` database. Docker Compose and the Kubernetes manifests do not publish these ports outside the internal network.

## Connecting to this database
The database URL in JDBC is `jdbc:h2:tcp://localhost:$DATABASE_TCP_PORT/traderx`. From inside the Compose network:

```shell
docker compose exec database sh -c 'java -cp build/libs/database.jar org.h2.tools.Shell -url jdbc:h2:tcp://localhost:18082/traderx -user sa -password "$DATABASE_DBPASS"'
```

## Connecting to the web console
Only for local development: anyone who can reach the H2 web console can open an in-memory database as admin and run code, so it is disabled by default and bound to loopback when enabled. The database URL is `jdbc:h2:traderx` (This is because -baseDir is already set to ./_data) - NOTE you will have to change the default setting in the web console which often uses a home directory path.

The database you want to use in the H2 GUI is `./traderx` (This may not be the default listed in the GUI)

## Using Web Console behind proxy/K8S/Env
If you enable remote access to the console with `DATABASE_WEB_ALLOW_OTHERS=true`, by default the hostname, localhost, 127.0.0.1 are all valid host headers to access the database. If you wish to connect using another IP, or via some proxy/gateway that's set up through K8S or other environment, you will need to specify the hostname your browser is using to access the web console. This is done by setting the environment variable `$DATABASE_WEB_HOSTNAMES` to the hostname you are using to access the web console. This is a comma-delimited list of fully qualified hostnames.

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

To launch, all that should be needed is running:
```shell
$> run.sh
```

## Running from your local Win10 Machine

You CAN run this on windows, with the help of Powershell.  All you need to do is have Java on your path, and then enter bash to run the script.  First launch a Terminal/Command Console in Windows.

```
>bash
>$ . run.sh
```

You will see the following output

```

Runing startup script
TCP server running at tcp://[your IP]:18082 (others can connect)
```

On my windows PC, it actually doesn't work with the public IP address. Just change the above URLs/Host Addresses to use `localhost`  and they work fine.

Example: http://localhost:18084 


## Accessing the Web Console

1. Start the database with `DATABASE_ENABLE_WEB_CONSOLE=true`, then visit http://localhost:18084 or whatever port you run your service on
2. You should see a web console, prompting you for a username, password, and JDBC URL - Values might not be auto-populated correctly
3. Username is `$DATABASE_DBUSER` (default `sa`), password is `$DATABASE_DBPASS`, and the JDBC URL should be `jdbc:h2:./traderx` and the Driver should be `Generic H2 Embedded`
4. Click `Connect`
