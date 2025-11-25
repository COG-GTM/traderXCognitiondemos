# TraderX Docker Development Workflow Demo Script

This guide demonstrates how to test and develop locally using Docker containers in the traderXCognitiondemos repository. The application is a multi-service microservices trading platform that can be fully orchestrated using Docker Compose.

## Prerequisites

Before starting, ensure you have the following installed on your machine:

- Docker Desktop (or Docker Engine with Docker Compose)
- Git
- At least 8GB of RAM available for Docker (recommended: 16GB for optimal performance)

## Architecture Overview

TraderX consists of 10 microservices that work together to provide a complete trading application:

| Service | Technology | Port | Description |
|---------|------------|------|-------------|
| database | Java/H2 | 18082-18084 | SQL database with TCP, PG, and Web Console ports |
| reference-data | Node.js/NestJS | 18085 | REST service for querying ticker symbols |
| trade-feed | Node.js/Socket.IO | 18086 | Message bus for trade flows and GUI streaming |
| people-service | .NET Core | 18089 | User lookup service for account management |
| account-service | Java/Spring Boot | 18088 | Account querying and validation |
| position-service | Java/Spring Boot | 18090 | Position and trade lookups for the blotter |
| trade-service | Java/Spring Boot | 18092 | Trade/order request submission |
| trade-processor | Java/Spring Boot | 18091 | Trade feed consumer for processing orders |
| web-front-end-angular | Angular | 18093 | Interactive UI for trading |
| ingress | Nginx | 8080 | API gateway routing external traffic |

## Part 1: Initial Setup

### Step 1: Clone the Repository

Open your terminal and clone the repository:

```bash
git clone https://github.com/COG-GTM/traderXCognitiondemos.git
cd traderXCognitiondemos
```

### Step 2: Start All Services

From the repository root directory, start all services using Docker Compose:

```bash
docker compose up
```

This command will build all container images from their respective Dockerfiles on first run and then start them in the correct dependency order. The Docker Compose configuration creates a shared virtual network called `localnet` that enables all services to communicate with each other.

You will see logs from all 10 services streaming to your terminal. Wait until you see all services have started successfully. Look for messages indicating each service is ready, such as Spring Boot's "Started Application" messages for Java services and "Listening on port" messages for Node.js services.

### Step 3: Verify the WebUI is Running

Once all services have started, open your web browser and navigate to:

```
http://localhost:8080
```

You should see the TraderX Angular web interface. The ingress service routes all traffic through port 8080 to the appropriate backend services.

To verify the application is fully functional, try the following:

1. Navigate to the Accounts section to view existing trading accounts
2. Select an account and view its positions in the blotter
3. Submit a test trade to verify the trade flow is working

## Part 2: Iterative Development Loop

The Docker Compose configuration includes volume mounts that map your local source code into the containers, enabling live code editing during development. Each service has a volume mount configured as:

```yaml
volumes:
  - .:/workspace:cached
```

This means changes to your local files are immediately visible inside the containers.

### Step 1: Make a Code Change

Let's walk through an example of modifying the reference-data service. Open the file in your preferred editor:

```bash
# Example: Modify the reference-data service
code reference-data/src/stocks/stocks.controller.ts
```

Make your desired changes to the code. For example, you might add a new endpoint, modify response formatting, or update business logic.

### Step 2: Rebuild the Changed Service

After making changes, you need to rebuild only the affected service. Use the following command to rebuild and restart a specific service without affecting others:

```bash
docker compose up -d --build reference-data
```

The flags used here are:

- `-d` (detached mode): Runs the container in the background so you can continue using your terminal
- `--build`: Forces a rebuild of the container image before starting

For Java/Spring Boot services that use Gradle, the rebuild will recompile the application:

```bash
# Rebuild the account-service
docker compose up -d --build account-service

# Rebuild the trade-service
docker compose up -d --build trade-service

# Rebuild the position-service
docker compose up -d --build position-service

# Rebuild the trade-processor
docker compose up -d --build trade-processor
```

For Node.js services:

```bash
# Rebuild the reference-data service
docker compose up -d --build reference-data

# Rebuild the trade-feed service
docker compose up -d --build trade-feed
```

For the .NET Core people-service:

```bash
docker compose up -d --build people-service
```

For the Angular frontend:

```bash
docker compose up -d --build web-front-end-angular
```

### Step 3: Verify Your Changes

After the service restarts, verify your changes are reflected in the running application:

1. Check the service logs to ensure it started successfully:
   ```bash
   docker compose logs -f reference-data
   ```

2. Test the affected functionality through the WebUI at http://localhost:8080

3. For API changes, you can test directly using curl or a tool like Postman:
   ```bash
   # Example: Test the reference-data service directly
   curl http://localhost:18085/stocks
   
   # Example: Test the account-service
   curl http://localhost:18088/accounts
   
   # Example: Test the position-service
   curl http://localhost:18090/positions
   ```

## Part 3: Key Docker Compose Commands

Here is a reference of essential Docker Compose commands for the development workflow:

### Starting Services

```bash
# Start all services (foreground, shows logs)
docker compose up

# Start all services in detached mode (background)
docker compose up -d

# Start specific services only
docker compose up database reference-data trade-feed
```

### Rebuilding Services

```bash
# Rebuild and restart a specific service
docker compose up -d --build <service-name>

# Rebuild all services
docker compose up -d --build

# Force rebuild without cache
docker compose build --no-cache <service-name>
```

### Viewing Logs

```bash
# View logs for all services
docker compose logs

# Follow logs in real-time
docker compose logs -f

# View logs for a specific service
docker compose logs -f account-service

# View last 100 lines of logs
docker compose logs --tail=100 trade-service
```

### Stopping Services

```bash
# Stop all services but keep containers
docker compose stop

# Stop and remove all containers
docker compose down

# Stop and remove containers, networks, and volumes
docker compose down -v

# Stop a specific service
docker compose stop trade-processor
```

### Service Management

```bash
# Restart a specific service
docker compose restart account-service

# View running containers
docker compose ps

# Execute a command inside a running container
docker compose exec account-service bash

# View resource usage
docker compose top
```

### Troubleshooting Commands

```bash
# Check service health and status
docker compose ps -a

# View detailed container information
docker compose inspect <service-name>

# Remove all stopped containers and unused images
docker system prune

# View Docker Compose configuration
docker compose config
```

## Part 4: Development Tips

### Accessing the H2 Database Console

The database service exposes a web console for direct database access:

```
http://localhost:18084
```

Use this to inspect tables, run queries, and debug data issues during development.

### Service Dependencies

The Docker Compose file defines service dependencies to ensure proper startup order. Key dependencies include:

- `account-service` depends on `database` and `people-service`
- `position-service` depends on `database`
- `trade-service` depends on `database`, `people-service`, `account-service`, `reference-data`, and `trade-feed`
- `trade-processor` depends on `database` and `trade-feed`
- `ingress` depends on all other services

If you're experiencing issues with a service, ensure its dependencies are running correctly.

### Environment Variables

Services communicate using environment variables for host discovery. Key variables include:

- `DATABASE_TCP_HOST`: Database hostname (default: `database` in Docker, `localhost` for local dev)
- `PEOPLE_SERVICE_HOST`: People service hostname
- `ACCOUNT_SERVICE_HOST`: Account service hostname
- `REFERENCE_DATA_HOST`: Reference data service hostname
- `TRADE_FEED_HOST`: Trade feed hostname

### Running Individual Services Locally

If you prefer to run a specific service outside of Docker for faster iteration:

1. Stop the service in Docker:
   ```bash
   docker compose stop account-service
   ```

2. Set environment variables to point to Docker services:
   ```bash
   export DATABASE_TCP_HOST=localhost
   export PEOPLE_SERVICE_HOST=localhost
   ```

3. Run the service locally:
   ```bash
   cd account-service
   ./gradlew bootRun
   ```

### GitHub Codespaces Support

This repository supports GitHub Codespaces for cloud-based development. When using Codespaces:

1. Select an 8-core machine type with 32GB RAM for optimal performance
2. Run `docker compose up` from the repository root
3. The localhost URLs will be automatically mapped through to your local browser

## Summary

This demo script covered the complete Docker development workflow for TraderX:

1. **Initial Setup**: Clone the repository and start all services with `docker compose up`
2. **Verify**: Access the WebUI at http://localhost:8080 to confirm the application is running
3. **Develop**: Make code changes to any service using your preferred editor
4. **Rebuild**: Use `docker compose up -d --build <service-name>` to rebuild only the changed service
5. **Verify**: Check logs and test the application to confirm your changes work correctly

The volume mounts and Docker Compose orchestration enable a smooth development experience where you can iterate quickly on any of the 10 microservices while maintaining the full application context.
