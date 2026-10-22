# FINOS | TraderX Sample Trading App | Trade Processor

![DEV Only Warning](https://badgen.net/badge/warning/not-for-production/red) ![Local Dev Machine Supported](http://badgen.net/badge/local-dev/supported/green)

# trade-processor

A simple application that subscribes to trade-feed pubsub engine, and 'processes' trades. This initially stores them in the database as pending trades and marks them as processed, reporting each phase change on thei appropriate trade-feed as a notification. As trades are settled, it also recalculates any position changes and persists and broadcasts those changes as well.
 
# How to run the application (WIP)

- Check out the source code from git
- change any ports needed
- ``gradlew bootRun``

# Configuration

The easiest way to reconfigure the application is by editing properties in:

    `src/main/resources/application.properties`

Alternatively you can use environment variables to override certain values:

   `export TRADE_PROCESSOR_SERVICE_PORT=18091`

Or you can use command line arguments:
    
   $ gradlew bootRun --args='--server.port=18091'
   
The app by default runs on port 18091

## Trade Feed Location

You can either specify `TRADE_FEED_ADDRESS` as an environment variable (should be a URL in the current SocketIO implementation) or specify the app property per below

To specify the host and the port for the TradeFeed instance to subscribe to use the `trade.feed.address` property using one of the ways describe above, like

   $ gradlew bootRun --args='--trade.feed.address=http://localhost:18086

## Order validation

Every order is validated in `TradeService.processTrade` before anything is persisted, whether it arrives from the trade feed or over HTTP:

- `accountId` must be positive, `side` must be `Buy` or `Sell`, and `quantity` must be between 1 and `trade.processor.max-order-quantity` (default `1000000`, env `TRADE_PROCESSOR_MAX_ORDER_QUANTITY`).
- `security` must be a well-formed ticker that exists in reference-data (`reference.data.service.url`, env `REFERENCE_DATA_HOST` / `REFERENCE_DATA_SERVICE_URL`).
- The account must exist in account-service (`account.service.url`, env `ACCOUNT_SERVICE_HOST` / `ACCOUNT_SERVICE_URL`).
- Orders that would overflow the resulting position are rejected.

If reference-data or account-service is unreachable, the order is rejected (fail closed).

## Internal order endpoint

`POST /tradeservice/order` is an internal, service-to-service endpoint. It is disabled (returns 404) unless `trade.processor.order.token` (env `TRADE_PROCESSOR_ORDER_TOKEN`) is set, and callers must then send `Authorization: Bearer <token>`. It has no CORS policy and is not routed through the ingress. User-facing order entry goes through trade-service.

## Database settings

To specify the database properties, the following properties should be used

    spring.datasource.url=jdbc:h2:tcp://localhost:18082/traderx
    spring.datasource.username=sa
    spring.datasource.password=sa


You can see all configuration details in [src/main/resources/application.properties](application.properties)