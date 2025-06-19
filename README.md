# TraderX Reference Data Service

A standalone microservice that provides reference data for financial securities, extracted from the TraderX monorepo. This service provides REST API access to S&P 500 company data including ticker symbols and company names.

![DEV Only Warning](https://badgen.net/badge/warning/not-for-production/red) ![Local Dev Machine Supported](http://badgen.net/badge/local-dev/supported/green)

## Overview

The Reference Data Service is a Node.js/NestJS application that:
- Loads S&P 500 company data from a CSV file on startup
- Exposes REST endpoints for retrieving security information
- Provides OpenAPI/Swagger documentation
- Includes health check endpoints
- Has no external database dependencies

## API Endpoints

### Get All Securities
```
GET /stocks
```
Returns an array of all securities with ticker and company name.

### Get Security by Ticker
```
GET /stocks/{ticker}
```
Returns a single security by ticker symbol, or 404 if not found.

### API Documentation
```
GET /api
```
OpenAPI/Swagger documentation interface.

### Health Check
```
GET /health
```
Service health status.

## Running the Service

### Using Docker
```bash
docker build -t reference-data-service .
docker run -p 18085:18085 reference-data-service
```

### Using Node.js directly
```bash
npm install
npm run start
```

The service will start on port 18085 by default.

## Configuration

The service can be configured using environment variables:
- `REFERENCE_DATA_SERVICE_PORT`: Port number (default: 18085)
- `REFERENCE_DATA_HOSTNAME`: Hostname to bind to (default: 0.0.0.0)

## Integration Points

### Trade Service Integration
The Trade Service connects to this service using the `REFERENCE_DATA_HOST` environment variable. Set this to the hostname/IP where this service is running.

Example:
```bash
REFERENCE_DATA_HOST=reference-data-service:18085
```

### Web Frontend Integration
The Web Frontend makes direct REST calls to this service. Configure the frontend to point to this service's URL.

## Data Source

The service reads S&P 500 company data from `/data/s-and-p-500-companies.csv`. This file contains:
- Symbol: Stock ticker symbol
- Security: Company name
- Additional metadata (sector, location, etc.)

The [CSV of S&P 500 companies](./data/s-and-p-500-companies.csv) was populated by copying the data from
[this table from Wikipedia](https://en.wikipedia.org/wiki/List_of_S%26P_500_companies#S&P_500_component_stocks).

## Development

### Available Scripts
- `npm run start`: Start the service
- `npm run start:dev`: Start in development mode with hot reload
- `npm run build`: Build the TypeScript code
- `npm run test`: Run tests
- `npm run lint`: Run ESLint

### Testing
```bash
# unit tests
$ npm run test

# e2e tests
$ npm run test:e2e

# test coverage
$ npm run test:cov
```

### Project Structure
```
├── src/
│   ├── app.module.ts          # Main application module
│   ├── main.ts                # Application entry point
│   ├── stocks/                # Stock-related endpoints and services
│   ├── health/                # Health check endpoints
│   └── data-loader/           # CSV data loading utilities
├── data/
│   └── s-and-p-500-companies.csv  # S&P 500 company data
├── openapi.yaml               # OpenAPI specification
└── package.json               # Dependencies and scripts
```

## Testing the API

You can test the service endpoints using curl:

```bash
# Get all stocks
curl -X GET "http://localhost:18085/stocks" -H "accept: application/json"

# Get specific stock by ticker
curl -X GET "http://localhost:18085/stocks/AAPL" -H "accept: application/json"

# Check service health
curl -X GET "http://localhost:18085/health" -H "accept: application/json"
```

## Mock Testing with Prism

You can also run a mock of this service using `@stoplight/prism-cli`:

```bash
# Install prism globally (one time)
sudo npm install -g @stoplight/prism-cli

# Run mock service
prism --cors --port 18085 mock openapi.yaml
```

## License

Apache-2.0
