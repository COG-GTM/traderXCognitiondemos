# FINOS | TraderX Sample Trading App | Trade Feed

![DEV Only Warning](https://badgen.net/badge/warning/not-for-production/red) ![Local Dev Machine Supported](http://badgen.net/badge/local-dev/supported/green)

## About

This is a simple pub-sub server, meant to be swapped out by your preferred message bus. This simple example allows for simple subscription/unsubscription activities and events to be dispatched into recipient groups, known in [socket.IO](https://socket.io) as 'rooms'. This particular solution was chosen for this demo because it can be expressed simply in a single line of code and natively supports the UI via websockets as well as the 'server side' components of this demo application.

Publish command sends in a JSON object with {topic:'string', type:'string', payload: object}. Subscribe/Unsubscribe comes with a string topic and an optional acknowledgement callback that receives `{ok, message}`.

## Security model

Every connection must authenticate in the Socket.IO handshake (`auth: { token }`), and browser connections must come from an allowed origin.

| Identity | How it authenticates | Publish | Subscribe |
|---|---|---|---|
| `trade-service` | service token | `/trades` (type `TradeOrder`) | account topics |
| `trade-processor` | service token | `/accounts/<id>/trades` (type `Trade`), `/accounts/<id>/positions` (type `Position`) | `/trades`, account topics |
| user | HS256 JWT signed with `TRADE_FEED_USER_TOKEN_SECRET`, claims `{sub, aud: "trade-feed", accounts: [ids], exp}`; the socket is disconnected at `exp` | nothing | `/accounts/<id>/(trades\|positions)` for the ids in `accounts` |
| anonymous viewer (only when `TRADE_FEED_ALLOW_ANONYMOUS_VIEWERS=true`) | no token | nothing | `/accounts/<id>/(trades\|positions)` |

The envelope `from` field is set by the server to the authenticated identity name, so subscribers can trust it; trade-processor only books `TradeOrder` messages whose `from` is in `TRADE_FEED_TRUSTED_PUBLISHERS` (default `trade-service`). There is no `/*` wildcard room any more.

| Variable | Purpose |
|---|---|
| `TRADE_FEED_SERVICE_TOKENS` | Required. `trade-service=<token>,trade-processor=<token>` (32+ characters each). The Java services read their own token from `TRADE_FEED_SERVICE_TOKEN`. |
| `TRADE_FEED_ALLOWED_ORIGINS` | Comma-separated browser origins allowed for HTTP polling and WebSocket upgrades. Defaults to the local UI/ingress origins. |
| `TRADE_FEED_USER_TOKEN_SECRET` | Optional HMAC secret (32+ characters) for user JWTs scoped to accounts. |
| `TRADE_FEED_USER_TOKEN_AUDIENCE` | Required `aud` claim for user JWTs (default `trade-feed`). |
| `TRADE_FEED_ALLOW_ANONYMOUS_VIEWERS` | `true` lets unauthenticated clients read account topics. The demo UI has no login, so docker-compose, gitops and Radius enable it; leave it off anywhere real users or data are involved. |

## Developing Locally

Spin up a local environment to see your changes live (optional, but encouraged)

```shell
cd trade-feed (this directory)
# Default port is 18086 (Controlled with the `TRADE_FEED_PORT` environment variable)
npm install
export TRADE_FEED_SERVICE_TOKENS="trade-service=$(openssl rand -hex 32),trade-processor=$(openssl rand -hex 32)"
TRADE_FEED_ALLOW_ANONYMOUS_VIEWERS=true npm run start
npm test   # auth/authorization tests
```

Now you can edit the site, commit and merge as normal. If you performed the optional live environment step, you'll see a live-reloading copy of the site open in your browser at <http://localhost:18086/> (or whaterver port you are using)
