# FINOS | TraderX Sample Trading App | Position Service

![DEV Only Warning](https://badgen.net/badge/warning/not-for-production/red) ![Local Dev Machine Supported](http://badgen.net/badge/local-dev/supported/green)

The position service retrieves trades and aggregate positions from the database and returns them to pre-populate a blotter (trades/positions) for a specific accountID specified. Incremental updates are received in the GUI via the pub-sub trade-feed service.


## Prerequisites and Config

You will need a running instance of the database. This currently is configured to be a H2 Database running on tcp at `$DATABASE_TCP_HOST`:`$DATABASE_TCP_PORT` on the database `$DATABASE_NAME` 

Login details are `$DATABASE_DBUSER` and `$DATABASE_DBPASS`

Default `$POSITION_SERVICE_PORT` is `18090`

In this project, you can run the database by running a shell in the [database](../database/README.md) project and running

```bash
./gradlew build
./run.sh
``` 
## Running The Position Service`

A simple application that exposes lists of trades and positions by accountID to bootstrap the blotter. 

The API documentation is available via swagger:

`http://localhost:18090/api-docs`

And via UI:

`http://localhost:18090/swagger-ui.html`

It runs on port 18090 which can be changed via the

`server.port=18090`  system property or `$POSITION_SERVICE_PORT` environment variable.

How to run the application
Check out the source code from git

```bash
gradlew bootRun
```

Configuration can be found in `application.properties` and can be overridden with env vars or command line parameters

## Authentication and Authorization

`/positions/**` and `/trades/**` require a JWT bearer token (`Authorization: Bearer <jwt>`), typically forwarded by the OIDC proxy in front of the service.

* `GET /positions/{accountId}` and `GET /trades/{accountId}` are only allowed when the token's username claim is mapped to `accountId` in the `AccountUsers` table (managed by account-service). Other accounts, including unknown ones, return `403`.
* `GET /positions/` and `GET /trades/` (all accounts) require the admin role. Admins may also read any single account.
* Missing, forged, expired or unsigned tokens return `401`.
* Swagger UI and the API docs remain public.

| Env var | Default | Purpose |
|---|---|---|
| `POSITION_SERVICE_JWT_JWK_SET_URI` | _(none)_ | JWK set URI of the identity provider (preferred) |
| `POSITION_SERVICE_JWT_HMAC_SECRET` | _(none)_ | HS256 shared secret (at least 32 bytes), used when no JWK set URI is set |
| `POSITION_SERVICE_JWT_ISSUER` | _(none)_ | Required `iss` value, if set |
| `POSITION_SERVICE_JWT_AUDIENCE` | _(none)_ | Required `aud` value, if set |
| `POSITION_SERVICE_JWT_USERNAME_CLAIM` | `preferred_username` | Claim matched against `AccountUsers.Username` |
| `POSITION_SERVICE_JWT_ROLES_CLAIM` | `roles` | Claim (string or list) holding the caller's roles |
| `POSITION_SERVICE_ADMIN_ROLE` | `traderx-admin` | Role allowed to list all positions/trades |
| `POSITION_SERVICE_CORS_ALLOWED_ORIGINS` | _(none)_ | Comma-separated browser origins allowed cross-origin; `*` is ignored |

If neither a JWK set URI nor an HMAC secret is configured, every token is rejected.


## Simple Testing of Position Service`

You can run a mock of this service by installing @stoplight/prism 

This statically uses the example content in the OpenAPI spec to mock the service (you can specify `--dynamic` to let it be more creative)

```bash
# Only need to do this once for your machine
sudo npm install -g @stoplight/prism-cli
```

Run prism to mock your OpenAPI spec as follows (Specify `port` as you see fit)
```bash
prism --cors --port 18090 mock openapi.yaml
```

You can then try out your requests against the mock service as follows: (or from a browser)

```bash
curl -X GET "http://localhost:18090/trades/22214" -H "accept: application/json" -H "Authorization: Bearer $TOKEN"
curl -X GET "http://localhost:18090/positions/22214" -H "accept: application/json" -H "Authorization: Bearer $TOKEN"

```