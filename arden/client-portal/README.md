# client-portal

Institutional client portal backend (positions, valuations, settlement status). Zero runtime dependencies by
policy since the 2024 npm supply-chain review; UI assets live in `client-portal-ui`.

- **Owner:** `@ardencm/client-digital`
- **Run:** `npm start` (port 3200; `ARDENFEED_BATCH=fixtures/ardenfeed-v2.3.json` to point at another batch)
- **Test:** `npm test`
- **Deploys:** `deploys/history.json` mirrors the release-bot log; Datadog deploy markers are emitted from the same source.

Datadog monitor `client-portal / valuation 5xx rate` pages `@ardencm/client-digital` and posts to `#inc-client-portal`.
