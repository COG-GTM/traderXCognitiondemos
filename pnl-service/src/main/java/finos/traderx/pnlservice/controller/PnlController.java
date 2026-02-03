package finos.traderx.pnlservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import finos.traderx.pnlservice.model.PnlSummary;
import finos.traderx.pnlservice.model.SecurityPnl;
import finos.traderx.pnlservice.service.PnlService;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/pnl", produces="application/json")
public class PnlController {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PnlController.class);

	@Autowired
	PnlService pnlService;

	@GetMapping("/{accountId}")
	public ResponseEntity<PnlSummary> getPnlSummary(@PathVariable int accountId) {
		log.info("Getting P&L summary for account {}", accountId);
		PnlSummary summary = pnlService.getPnlSummary(accountId);
		return ResponseEntity.ok(summary);
	}

	@GetMapping("/{accountId}/realized")
	public ResponseEntity<List<SecurityPnl>> getRealizedPnl(@PathVariable int accountId) {
		log.info("Getting realized P&L for account {}", accountId);
		List<SecurityPnl> realizedPnl = pnlService.getRealizedPnlBreakdown(accountId);
		return ResponseEntity.ok(realizedPnl);
	}

	@GetMapping("/{accountId}/unrealized")
	public ResponseEntity<List<SecurityPnl>> getUnrealizedPnl(@PathVariable int accountId) {
		log.info("Getting unrealized P&L for account {}", accountId);
		List<SecurityPnl> unrealizedPnl = pnlService.getUnrealizedPnlBreakdown(accountId);
		return ResponseEntity.ok(unrealizedPnl);
	}

	@GetMapping("/{accountId}/securities/{security}")
	public ResponseEntity<SecurityPnl> getSecurityPnl(@PathVariable int accountId, @PathVariable String security) {
		log.info("Getting P&L for account {} and security {}", accountId, security);
		SecurityPnl securityPnl = pnlService.getSecurityPnl(accountId, security);
		return ResponseEntity.ok(securityPnl);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> generalError(Exception e) {
		log.error("Error processing P&L request", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
	}
}
