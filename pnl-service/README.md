# P&L Service

The P&L (Profit and Loss) Service is a read-model microservice that calculates realized and unrealized profit/loss for trading accounts in the TraderX application.

## Overview

This service provides REST APIs to query P&L data and subscribes to the trade-feed message bus to receive real-time trade updates. When trades are settled, it recalculates P&L and publishes updates to interested subscribers.

## Key Features

- **Realized P&L Calculation**: Calculates profit/loss from closed positions using FIFO (First In, First Out) matching of buy and sell trades
- **Unrealized P&L Calculation**: Calculates mark-to-market gains/losses on open positions by comparing average cost basis against current market prices
- **Real-time Updates**: Subscribes to trade updates via Socket.IO and publishes P&L changes
- **REST API**: Provides endpoints to query P&L summaries and breakdowns by security

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /pnl/{accountId}` | Get P&L summary for an account |
| `GET /pnl/{accountId}/realized` | Get realized P&L breakdown by security |
| `GET /pnl/{accountId}/unrealized` | Get unrealized P&L breakdown by security |
| `GET /pnl/{accountId}/securities/{security}` | Get P&L for a specific security |

## P&L Calculations

### Realized P&L
Calculated by matching sell trades against buy trades using FIFO method:
```
Realized P&L = (Sell Price - Buy Price) × Quantity
```

### Unrealized P&L
Calculated by comparing average cost basis against current market price:
```
Unrealized P&L = (Current Market Price - Average Cost) × Current Quantity
```

### Total P&L
```
Total P&L = Realized P&L + Unrealized P&L
```

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `PNL_SERVICE_PORT` | 18095 | Service port |
| `DATABASE_TCP_HOST` | localhost | H2 database host |
| `DATABASE_TCP_PORT` | 18082 | H2 database port |
| `TRADE_FEED_HOST` | localhost | Trade feed Socket.IO server host |

## Running Locally

```bash
./gradlew bootRun
```

## Building

```bash
./gradlew build
```

## Docker

```bash
docker build -t pnl-service .
docker run -p 18095:18095 pnl-service
```

## Dependencies

- Spring Boot 3.3.13
- Spring Data JPA
- H2 Database (via JDBC)
- Socket.IO Client (for real-time messaging)
- SpringDoc OpenAPI (for API documentation)

## Message Bus Integration

The service subscribes to the `/trades` topic on the trade-feed to receive trade updates. When a trade is settled, it:
1. Recalculates P&L for the affected account and security
2. Publishes the updated P&L to `/accounts/{accountId}/pnl` topic

## API Documentation

When running, Swagger UI is available at:
- http://localhost:18095/swagger-ui.html
