package finos.traderx.positionservice.controller;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import finos.traderx.positionservice.model.audit.AuditPage;
import finos.traderx.positionservice.model.audit.DecisionOutcome;
import finos.traderx.positionservice.service.AuditQueryService;

/**
 * Read-only query API over the best-execution audit trail written by trade-service (TRX-104),
 * retained under MiFID II Art. 16(6) and RTS 27/28.
 *
 * There is deliberately no POST, PUT, PATCH or DELETE here, and there never should be. A record
 * that can be amended after the fact is not evidence.
 */
@CrossOrigin("*")
@RestController
@RequestMapping(value = "/audit", produces = "application/json")
public class AuditController {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/decisions")
    public ResponseEntity<AuditPage> getDecisions(
            @RequestParam(required = false) Integer accountId,
            @RequestParam(required = false) String security,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        if (!auditQueryService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity
                .ok(auditQueryService.search(accountId, security, parseDecision(decision), from, to, page, size));
    }

    /**
     * An unrecognised decision value is rejected rather than ignored. Silently dropping the
     * filter would answer a narrow question with a wider result set, and a reviewer reading
     * "rejected only" has no way to tell.
     */
    private DecisionOutcome parseDecision(String decision) {
        if (decision == null || decision.isBlank()) {
            return null;
        }
        try {
            return DecisionOutcome.valueOf(decision.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown decision '" + decision + "'. Expected one of " + Arrays.toString(DecisionOutcome.values()));
        }
    }

    /**
     * A filter the caller mistyped is a bad request, not an outage. Without this the controller's
     * catch-all would turn Spring's binding failure into a 500 and the Compliance tab would
     * report the audit trail as unavailable.
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<String> typeMismatch(TypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    /**
     * The failure is logged, not returned. A persistence error's message carries schema and query
     * detail, and this endpoint answers anyone who can reach the service.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> generalError(Exception e) {
        log.error("Failed to query the order decision audit trail", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not query the audit trail.");
    }
}
