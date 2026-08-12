package finos.traderx.tradeservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.PreTradeRiskException;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.RiskDecision;
import finos.traderx.tradeservice.model.Security;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.risk.RiskLimitService;
import finos.traderx.tradeservice.service.AccountValidationService;
import finos.traderx.tradeservice.service.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/trade", produces = "application/json")
public class TradeOrderController {

	private static final Logger log = LoggerFactory.getLogger(TradeOrderController.class);

	@Autowired
	private Publisher<TradeOrder> tradePublisher;

	@Autowired
	private ReferenceDataService referenceDataService;

	@Autowired
	private AccountValidationService accountValidationService;

	@Autowired
	private RiskLimitService riskLimitService;

	@Operation(description = "Submit a new trade order")
	@PostMapping("/")
	public ResponseEntity<TradeOrder> createTradeOrder(@Parameter(description = "the intendeded trade order") @RequestBody TradeOrder tradeOrder) {
		log.info("Called createTradeOrder");

		Security security = this.referenceDataService.findSecurity(tradeOrder.getSecurity())
			.orElseThrow(() -> new ResourceNotFoundException(tradeOrder.getSecurity() + " not found in Reference data service."));

		if (!this.accountValidationService.accountExists(tradeOrder.getAccountId())) {
			throw new ResourceNotFoundException(tradeOrder.getAccountId() + " not found in Account service.");
		}

		RiskDecision decision = this.riskLimitService.evaluate(tradeOrder, security);
		if (decision.isRejected()) {
			throw new PreTradeRiskException(decision);
		}

		try {
			log.info("Trade is valid. Submitting {}", tradeOrder);
			tradePublisher.publish("/trades", tradeOrder);
			return ResponseEntity.ok(tradeOrder);
		} catch (PubSubException e) {
			throw new RuntimeException("Failed to publish trade order", e);
		}
	}

	@ExceptionHandler(PreTradeRiskException.class)
	public ResponseEntity<RiskDecision> handlePreTradeRisk(PreTradeRiskException ex) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getDecision());
	}
}
