# P&L Service

The P&L (Profit and Loss) Service calculates and provides profit/loss metrics for trading accounts in the TraderX platform.

## Overview

This service provides REST APIs to retrieve:
- **P&L Summary**: Overall realized and unrealized P&L for an account
- **Realized P&L**: Profit/loss from closed positions (matched buy/sell trades)
- **Unrealized P&L**: Mark-to-market P&L on open positions

## Technology Stack

- Java 21
- Spring Boot 3.3.13
- Spring Data JPA
- H2 Database (shared with other TraderX services)
- Socket.IO (for trade-feed integration)

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/pnl/{accountId}` | GET | Get P&L summary for an account |
| `/pnl/{accountId}/realized` | GET | Get realized P&L details |
| `/pnl/{accountId}/unrealized` | GET | Get unrealized P&L details |

## Configuration

The service uses the following environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `PNL_SERVICE_PORT` | 18095 | Service port |
| `DATABASE_TCP_HOST` | localhost | H2 database host |
| `DATABASE_TCP_PORT` | 18082 | H2 database port |
| `TRADE_FEED_HOST` | localhost | Trade feed service host |
| `TRADE_FEED_PORT` | 18086 | Trade feed service port |

## Running Locally

```bash
./gradlew bootRun
```

The service will be available at `http://localhost:18095`

## Running with Docker Compose

The service is included in the main `docker-compose.yml`:

```bash
docker-compose up pnl-service
```

## API Documentation

Swagger UI is available at:
- Direct: `http://localhost:18095/swagger-ui/index.html`
- Via Ingress: `http://localhost:8080/pnl-service/swagger-ui/index.html`

## P&L Calculations

### Realized P&L
Calculated using FIFO (First-In-First-Out) matching of buy and sell trades:
- For each sell trade, match against the oldest buy trades
- Realized P&L = (Sell Price - Buy Price) × Quantity

### Unrealized P&L
Calculated on open positions:
- Current Position Value = Current Market Price × Position Quantity
- Cost Basis = Average Purchase Price × Position Quantity
- Unrealized P&L = Current Position Value - Cost Basis

### Total P&L
- Total P&L = Realized P&L + Unrealized P&L

## Trade Feed Integration

The service subscribes to the trade-feed message bus to receive real-time trade updates. When trades are settled, it recalculates P&L metrics and publishes updates to `/accounts/{accountId}/pnl`.
