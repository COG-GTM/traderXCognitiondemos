# FINOS | TraderX Sample Trading App | Trade Feed

![DEV Only Warning](https://badgen.net/badge/warning/not-for-production/red) ![Local Dev Machine Supported](http://badgen.net/badge/local-dev/supported/green)

## About

This is a simple pub-sub server, meant to be swapped out by your preferred message bus. This simple example allows for simple subscription/unsubscription activities and events to be dispatched into recipient groups, known in [socket.IO](https://socket.io) as 'rooms'. This particular solution was chosen for this demo because it can be expressed simply in a single line of code and natively supports the UI via websockets as well as the 'server side' components of this demo application.

Publish command sends in a JSON object with {topic:'string', type:'string', payload: object} Subscribe/Unsubscribe comes with a string topic.

## Security

Every connection must authenticate in the Socket.IO handshake (`auth: { token }`) and is authorized per topic and message type:

| Identity | Token env var | May publish | May subscribe |
| --- | --- | --- | --- |
| `trade-service` | `TRADE_FEED_TRADE_SERVICE_TOKEN` | `TradeOrder` on `/trades` | — |
| `trade-processor` | `TRADE_FEED_TRADE_PROCESSOR_TOKEN` | `Trade` on `/accounts/{id}/trades`, `Position` on `/accounts/{id}/positions` | `/trades` |
| viewer (no token) | — (only if `TRADE_FEED_ALLOW_ANONYMOUS_VIEWERS=true`) | nothing | `/accounts/{id}/trades`, `/accounts/{id}/positions` |

Tokens must be distinct and at least 32 characters; the server refuses to start otherwise. The envelope `from` field is set by the server to the authenticated identity. Browser connections are only accepted from `TRADE_FEED_ALLOWED_ORIGINS` (comma separated). There is no wildcard (`/*`) topic.

## Developing Locally

Spin up a local environment to see your changes live (optional, but encouraged)

```shell
cd trade-feed (this directory)
# Default port is 18086 (Controlled with the `TRADE_FEED_PORT` environment variable)
npm ci
export TRADE_FEED_TRADE_SERVICE_TOKEN=$(openssl rand -hex 32)
export TRADE_FEED_TRADE_PROCESSOR_TOKEN=$(openssl rand -hex 32)
export TRADE_FEED_ALLOW_ANONYMOUS_VIEWERS=true
npm run start
npm test
```

Now you can edit the site, commit and merge as normal. If you performed the optional live environment step, you'll see a live-reloading copy of the site open in your browser at <http://localhost:18086/> (or whaterver port you are using)
