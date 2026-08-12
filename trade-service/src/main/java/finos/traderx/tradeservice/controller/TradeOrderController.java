package finos.traderx.tradeservice.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.Account;
import finos.traderx.tradeservice.model.Security;
import finos.traderx.tradeservice.audit.OrderDecisionAuditService;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.audit.DecisionOutcome;
import finos.traderx.tradeservice.model.audit.DecisionReason;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/trade", produces = "application/json")
public class TradeOrderController {

	private static final Logger log = LoggerFactory.getLogger(TradeOrderController.class);

	private static final String SUBMITTING_USER_HEADER = "X-TraderX-User";
	private static final String UNKNOWN_USER = "UNKNOWN";

	@Autowired
	private Publisher<TradeOrder> tradePublisher;

	@Autowired
	private OrderDecisionAuditService orderDecisionAuditService;
	
	private RestTemplate restTemplate = new RestTemplate();

	@Value("${reference.data.service.url}")
	private String referenceDataServiceAddress;

	@Value("${account.service.url}")
	private String accountServiceAddress;

	@Operation(description = "Submit a new trade order")
	@PostMapping("/")
	public ResponseEntity<TradeOrder> createTradeOrder(@Parameter(description = "the intendeded trade order") @RequestBody TradeOrder tradeOrder,
			@Parameter(description = "user submitting the order, recorded in the audit trail") @RequestHeader(value = SUBMITTING_USER_HEADER, required = false) String submittingUser) {
		log.info("Called createTradeOrder");

		String correlationId = UUID.randomUUID().toString();
		tradeOrder.setCorrelationId(correlationId);
		String submittedBy = (submittingUser == null || submittingUser.isBlank()) ? UNKNOWN_USER : submittingUser;

		if (!validateTicker(tradeOrder.getSecurity())) 
		{
			orderDecisionAuditService.recordDecision(tradeOrder, correlationId, DecisionOutcome.REJECTED,
					DecisionReason.SECURITY_NOT_FOUND, submittedBy);
			throw new ResourceNotFoundException(tradeOrder.getSecurity() + " not found in Reference data service.");
		}
		else if(!validateAccount(tradeOrder.getAccountId()))
		{
			orderDecisionAuditService.recordDecision(tradeOrder, correlationId, DecisionOutcome.REJECTED,
					DecisionReason.ACCOUNT_NOT_FOUND, submittedBy);
			throw new ResourceNotFoundException(tradeOrder.getAccountId() + " not found in Account service.");
		}
		else
		{
			orderDecisionAuditService.recordDecision(tradeOrder, correlationId, DecisionOutcome.ACCEPTED,
					DecisionReason.VALIDATED, submittedBy);
			try{
				log.info("Trade is valid. Submitting {}", tradeOrder);
				tradePublisher.publish("/trades",tradeOrder);
				return  ResponseEntity.ok(tradeOrder);
			}  catch (PubSubException e){
				throw new RuntimeException("Failed to publish trade order", e);
			}
		}
	}

	private boolean validateTicker(String ticker)
	{
		// Move whole method to a sperate class that handles all reference data 
		// so we can mock it and run without this service up.
		String url = this.referenceDataServiceAddress + "//stocks/" + ticker;
		ResponseEntity<Security> response = null;

		try {
			response = this.restTemplate.getForEntity(url, Security.class);
			log.info("Validate ticker " + response.getBody().toString());
			return true;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getRawStatusCode() == 404) {
				log.info(ticker + " not found in reference data service.");
			}
			else {
				log.error(ex.getMessage());
			}
			return false;
		}
	}		
	
	private boolean validateAccount(Integer id)
	{
		// Move whole method to a sperate class that handles all accounts 
		// so we can mock it and run without this service up.

		String url = this.accountServiceAddress + "//account/" + id;
		ResponseEntity<Account> response = null;

		try 
		{
				response = this.restTemplate.getForEntity(url, Account.class);
				log.info("Validate account " + response.getBody().toString());
				return true;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getRawStatusCode() == 404) {
				log.info("Account" + id + " not found in account service.");				
			}
			else {
				log.error(ex.getMessage());
			}
			return false;
		}
	}
}