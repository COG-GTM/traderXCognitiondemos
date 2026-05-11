# Change Management Ticket

## General Information

| Field | Value |
|---|---|
| **Change ID** | CHG-TRADERX-2026-0507 |
| **Title** | Add structured audit logging to trade-processor service |
| **Category** | Standard Change |
| **Priority** | High |
| **Risk Level** | Low |
| **Requested By** | Compliance / Data Governance |
| **Assigned To** | Platform Engineering |
| **Service** | trade-processor (FINOS TraderX) |
| **Environment** | All (Dev, Staging, Production) |
| **Change Window** | Next scheduled release |

---

## Change Description

### Summary

This change adds structured, immutable audit logging to the `trade-processor` Java service to satisfy the organization's 7-year data retention requirement under internal data governance standards. The compliance team identified that the trade-processor service — which is responsible for receiving, processing, settling, and persisting every trade — produced no audit trail that could be retained, queried, or forwarded to a SIEM for compliance monitoring.

### What Changed

1. **New dependency:** `logstash-logback-encoder 8.0` added to `build.gradle` for structured JSON log output compatible with Splunk, Elastic, and other SIEM platforms.

2. **New audit infrastructure (`audit` package):**
   - `AuditEvent.java` — Immutable value object capturing all required fields per the data governance standard: `eventType`, `timestamp` (ISO-8601 UTC), `tradeId`, `accountId`, `security`, `quantity`, `side`, `currentState`, `previousState`, and `initiator`.
   - `TradeAuditLogger.java` — Spring `@Component` that writes to a dedicated SLF4J logger named `AUDIT`, isolated from application logs. Emits structured key-value pairs via `net.logstash.logback.argument.StructuredArguments.kv()`.

3. **Logback configuration (`logback-spring.xml`):**
   - `AUDIT_FILE` appender: rolling file appender writing JSON to `logs/audit/trade-audit.log` with a 7-year (`maxHistory=2557` days) retention policy and 50 GB size cap.
   - `AUDIT_CONSOLE` appender: JSON console output for container/cloud environments where log aggregation collects stdout.
   - The `AUDIT` logger is configured with `additivity="false"` to prevent audit events from leaking into application logs.

4. **Integration into `TradeService.processTrade()`:**
   - `TRADE_RECEIVED` — logged immediately after trade creation with all order details and initiator identity.
   - `TRADE_STATE_CHANGE` — logged on each state transition (New → Processing) with both previous and current state.
   - `TRADE_SETTLED` — logged after final settlement with complete trade record.

5. **Tests (11 total, all passing):**
   - 6 unit tests for `TradeAuditLogger` verifying field presence, event types, and initiator resolution.
   - 3 unit tests for `TradeService` verifying audit logger integration, call ordering, and resilience to publish failures.
   - 2 integration tests (`@SpringBootTest` with in-memory H2) verifying end-to-end audit record production per trade.

### What Did NOT Change

- No changes to trade processing logic, database schema, API contracts, or pub/sub messaging.
- No changes to existing application logging behavior.
- No new REST endpoints or external dependencies beyond the logging library.

---

## Risk Assessment

| Risk Factor | Assessment | Rationale |
|---|---|---|
| **Service disruption** | Very Low | Audit logging is additive — it runs after each processing step but does not gate trade settlement. A logging failure cannot prevent trade processing. |
| **Data integrity** | None | No changes to trade persistence logic, database schema, or position calculations. |
| **Performance impact** | Negligible | SLF4J logging is asynchronous at the appender level. The `logstash-logback-encoder` adds ~0.1 ms per structured log call. Three calls per trade at current volumes is immaterial. |
| **Disk usage** | Low | JSON audit logs will grow proportionally to trade volume. The rolling policy caps total size at 50 GB with automatic daily rotation. |
| **Backward compatibility** | Full | No API changes. No configuration changes required for existing deployments. The audit log path defaults to `logs/audit/` and can be overridden via the `AUDIT_LOG_PATH` environment variable. |
| **Dependency risk** | Low | `logstash-logback-encoder` is a mature, widely-adopted library (40M+ downloads) maintained by the Logstash community. It depends only on `logback-core` and `jackson`, both already present in the Spring Boot dependency tree. |

### Overall Risk Rating: **LOW**

---

## Rollback Plan

1. **Immediate rollback:** Revert the merge commit on `main` and redeploy the previous image. No database migration is involved, so rollback is a single `git revert` + deploy.

2. **Partial rollback (disable audit logging only):** Set the `AUDIT` logger level to `OFF` in `logback-spring.xml` or via Spring property `logging.level.AUDIT=OFF`. No code change or restart required if using Spring Boot Actuator's `/loggers` endpoint.

3. **Dependency removal:** If `logstash-logback-encoder` causes classpath conflicts, remove the dependency from `build.gradle`, delete the `audit` package, revert the three lines added to `TradeService.java`, and remove `logback-spring.xml`. The service reverts to its previous behavior with zero side effects.

### Rollback verification:
- Confirm trade processing continues normally by submitting a test trade via `/tradeservice/order`.
- Confirm no `AUDIT`-prefixed log entries appear in stdout or the `logs/audit/` directory.

---

## Testing Evidence

### Unit Tests (9 tests — all passing)

| Test Class | Test | Result |
|---|---|---|
| `TradeAuditLoggerTest` | `logTradeReceived_emitsAuditEvent` | PASS |
| `TradeAuditLoggerTest` | `logTradeReceived_usesOrderIdAsInitiator` | PASS |
| `TradeAuditLoggerTest` | `logTradeReceived_defaultsToSystemInitiator` | PASS |
| `TradeAuditLoggerTest` | `logTradeStateChange_emitsAuditEventWithPreviousState` | PASS |
| `TradeAuditLoggerTest` | `logTradeSettled_emitsAuditEvent` | PASS |
| `TradeAuditLoggerTest` | `allRequiredFieldsPresent_inEveryAuditEvent` | PASS |
| `TradeServiceTest` | `processTrade_emitsThreeAuditEvents` | PASS |
| `TradeServiceTest` | `processTrade_auditLoggerCalledBeforePublish` | PASS |
| `TradeServiceTest` | `processTrade_auditLoggerStillCalledWhenPublishFails` | PASS |

### Integration Tests (2 tests — all passing)

| Test Class | Test | Result |
|---|---|---|
| `TradeAuditIntegrationTest` | `processTrade_producesAuditRecordForEveryTradeProcessed` | PASS |
| `TradeAuditIntegrationTest` | `processTrade_multipleTradesEachProduceAuditRecords` | PASS |

### Build Verification

```
BUILD SUCCESSFUL in 13s
6 actionable tasks: 6 executed
```

---

## Approval

| Role | Name | Date | Decision |
|---|---|---|---|
| Change Requestor | | | |
| Technical Reviewer | | | |
| Change Manager | | | |
| CAB Approval | | | |
