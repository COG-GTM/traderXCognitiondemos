package finos.traderx.positionservice.controller;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> generalError(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
