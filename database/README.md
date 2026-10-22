# FINOS | TraderX Sample Trading App | Database

![DEV Only Warning](https://badgen.net/badge/warning/not-for-production/red) ![Local Dev Machine Supported](http://badgen.net/badge/local-dev/supported/green)

## Introduction

This is designed to play the role of a SQL database that runs standalone as part of an example environment. The other processes in this environment interact with this via SQL / JDBC drivers and therefore this component can be swapped out in other iterations of this environment and replaced with a robust and productionizable RDBMS.

This uses H2 Java-based database as a standalone server and initializes with an empty SQL schema every time it is started.

## Security

- `DATABASE_DBPASS` is **required** and has no default. `run.sh` refuses to start if it is unset, shorter than 12 characters, or a well-known default such as `sa`. The same value must be supplied to `account-service`, `position-service` and `trade-processor`.
- Only the TCP server (`$DATABASE_TCP_PORT`) accepts connections from other hosts, because the Java services connect to it over the network. Do not publish it outside the internal network.
- The PG server and the H2 web console are **disabled by default**. Set `DATABASE_PG_SERVER=true` and/or `DATABASE_WEB_CONSOLE=true` to enable them for local debugging; they then only listen on `localhost` inside the database container/host and are not routed through the ingress.

## Default Port Numbers
| Protocol | Port Number |
| :--- | :--- |
| TCP | 18082 |
| PG | 18083 (disabled by default, localhost only) |
| HTTP | 18084 (disabled by default, localhost only) |
 
## Connecting to this database remotely
You can use the `$DATABASE_TCP_PORT`  or `$DATABASE_PG_PORT` and the database URL in JDBC is `jdbc:h2:./_data/traderx`

The default username is `sa`; the password is whatever you set in `$DATABASE_DBPASS`.

## Connecting to the web console
You can use the `$DATABASE_HTTP_PORT`  or `$DATABASE_PG_PORT` and the database URL in JDBC is `jdbc:h2:traderx` (This is because -baseDir is already set to ./_data) - NOTE you will have to change the default setting in the web console which often uses a home directory path. 

The web console is only available when `DATABASE_WEB_CONSOLE=true` and only from `localhost` on the machine running the database. Log in with `$DATABASE_DBUSER` / `$DATABASE_DBPASS`.

The database you want to use in the H2 GUI is `./traderx` (This may not be the default listed in the GUI)

## Using Web Console in K8S/containers
The web console is never exposed through the ingress. To use it against a running container, enable it with `DATABASE_WEB_CONSOLE=true` and use a local port-forward (e.g. `kubectl port-forward` or `docker exec`) so the connection still originates from `localhost`.

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

To launch, set a strong password and run:
```shell
$> export DATABASE_DBPASS='<strong password>'
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

TCP server running at tcp://[your IP]:18082 (others can connect)
```

On my windows PC, it actually doesn't work with the public IP address. Just change the above URLs/Host Addresses to use `localhost`  and they work fine.


## Accessing the Web Console

1. Start the database with `DATABASE_WEB_CONSOLE=true` and visit http://localhost:18084 from the same machine
2. You should see a web console, prompting you for a username, password, and JDBC URL - Values might not be auto-populated correctly
3. Username is `$DATABASE_DBUSER` (default `sa`), password is `$DATABASE_DBPASS`, the JDBC URL should be `jdbc:h2:./traderx` and the Driver should be `Generic H2 Embedded`
4. Click `Connect`
