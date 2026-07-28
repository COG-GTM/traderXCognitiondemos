# FINOS | TraderX Sample Trading App | Web Front End

![DEV Only Warning](https://badgen.net/badge/warning/not-for-production/red) ![Local Dev Machine Supported](http://badgen.net/badge/local-dev/supported/green)

The Web Front End provides a UI for users to select an account, view trades and positions, initiate new trades, and to administer the accounts themselves.

For the trade  and position blotters, it will need to query the position service, and subscribe to a given account on the trade feed for incremental updates.

For executing trades, it will need to query the account service to select an account, the security reference data service to resolve securities, and the trade service for submitting a trade to be executed.

For managing accounts it will need to connect to the account service to query and update accounts, and to the people service for resolving users to associate with accounts.

## Frontend conventions

`angular/` is the canonical UI (it is what `docker compose up` serves, and the only one with
account management); `react/` is a hack-day contribution kept for reference.

Before changing the Angular UI, read [`devin_context/frontend/`](../devin_context/README.md) — the
repo's normative frontend conventions: dependency policy and file layout, the Bootstrap 5 / AG Grid
design system, component and blotter patterns, service/RxJS/trade-feed rules, spec and selector
conventions, and the pre-PR checklist.
