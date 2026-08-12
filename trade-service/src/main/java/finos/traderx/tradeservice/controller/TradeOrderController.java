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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.exceptions.InvalidSubmissionException;
import finos.traderx.tradeservice.exceptions.ValidationUnavailableException;
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

	/** Length of the SUBMITTEDBY column; an over-long header must not fail the audit insert. */
	private static final int SUBMITTED_BY_MAX_LENGTH = 50;

	private final Publisher<TradeOrder> tradePublisher;

	private final OrderDecisionAuditService orderDecisionAuditService;

	private final RestTemplate restTemplate;

	@Autowired
	public TradeOrderController(Publisher<TradeOrder> tradePublisher,
			OrderDecisionAuditService orderDecisionAuditService, RestTemplate restTemplate) {
		this.tradePublisher = tradePublisher;
		this.orderDecisionAuditService = orderDecisionAuditService;
		this.restTemplate = restTemplate;
	}

	/** Outcome of a downstream existence check. "Not found" and "could not ask" are different facts. */
	enum LookupResult {
		FOUND,
		NOT_FOUND,
		/** The lookup service refused the question — the submission itself is malformed. */
		INVALID_SUBMISSION,
		UNAVAILABLE
	}

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
		String submittedBy = submittedBy(submittingUser);

		LookupResult tickerLookup = validateTicker(tradeOrder.getSecurity());
		if (tickerLookup == LookupResult.UNAVAILABLE)
		{
			orderDecisionAuditService.recordDecision(tradeOrder, correlationId, DecisionOutcome.REJECTED,
					DecisionReason.VALIDATION_UNAVAILABLE, submittedBy);
			throw new ValidationUnavailableException("Could not validate " + tradeOrder.getSecurity() + " against Reference data service.");
		}
		else if (tickerLookup == LookupResult.INVALID_SUBMISSION)
		{
			orderDecisionAuditService.recordDecision(tradeOrder, correlationId, DecisionOutcome.REJECTED,
					DecisionReason.SUBMISSION_INVALID, submittedBy);
			throw new InvalidSubmissionException("Reference data service rejected the lookup for " + tradeOrder.getSecurity() + ".");
		}
		else if (tickerLookup == LookupResult.NOT_FOUND) 
		{
			orderDecisionAuditService.recordDecision(tradeOrder, correlationId, DecisionOutcome.REJECTED,
					DecisionReason.SECURITY_NOT_FOUND, submittedBy);
			throw new ResourceNotFoundException(tradeOrder.getSecurity() + " not found in Reference data service.");
		}

		LookupResult accountLookup = validateAccount(tradeOrder.getAccountId());
		if (accountLookup == LookupResult.UNAVAILABLE)
		{
			orderDecisionAuditService.recordDecision(tradeOrder, correlationId, DecisionOutcome.REJECTED,
					DecisionReason.VALIDATION_UNAVAILABLE, submittedBy);
			throw new ValidationUnavailableException("Could not validate account " + tradeOrder.getAccountId() + " against Account service.");
		}
		else if (accountLookup == LookupResult.INVALID_SUBMISSION)
		{
			orderDecisionAuditService.recordDecision(tradeOrder, correlationId, DecisionOutcome.REJECTED,
					DecisionReason.SUBMISSION_INVALID, submittedBy);
			throw new InvalidSubmissionException("Account service rejected the lookup for account " + tradeOrder.getAccountId() + ".");
		}
		else if(accountLookup == LookupResult.NOT_FOUND)
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

	private String submittedBy(String submittingUser)
	{
		if (submittingUser == null || submittingUser.isBlank()) {
			return UNKNOWN_USER;
		}
		String trimmed = submittingUser.trim();
		return trimmed.length() > SUBMITTED_BY_MAX_LENGTH ? trimmed.substring(0, SUBMITTED_BY_MAX_LENGTH) : trimmed;
	}

	private LookupResult validateTicker(String ticker)
	{
		// Move whole method to a sperate class that handles all reference data 
		// so we can mock it and run without this service up.
		String url = this.referenceDataServiceAddress + "//stocks/" + ticker;
		ResponseEntity<Security> response = null;

		try {
			response = this.restTemplate.getForEntity(url, Security.class);
			log.info("Validate ticker " + String.valueOf(response.getBody()));
			return LookupResult.FOUND;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode().value() == 404) {
				log.info(ticker + " not found in reference data service.");
				return LookupResult.NOT_FOUND;
			}
			// A 4xx that is not a 404 means the service understood us and objected: that is a
			// bad submission, not an outage, and the audit record has to say so.
			log.error(ex.getMessage());
			return LookupResult.INVALID_SUBMISSION;
		}
		catch (RestClientException ex) {
			log.error("Reference data service unavailable while validating " + ticker, ex);
			return LookupResult.UNAVAILABLE;
		}
	}		
	
	private LookupResult validateAccount(Integer id)
	{
		// Move whole method to a sperate class that handles all accounts 
		// so we can mock it and run without this service up.

		String url = this.accountServiceAddress + "//account/" + id;
		ResponseEntity<Account> response = null;

		try 
		{
				response = this.restTemplate.getForEntity(url, Account.class);
				log.info("Validate account " + String.valueOf(response.getBody()));
				return LookupResult.FOUND;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode().value() == 404) {
				log.info("Account" + id + " not found in account service.");				
				return LookupResult.NOT_FOUND;
			}
			log.error(ex.getMessage());
			return LookupResult.INVALID_SUBMISSION;
		}
		catch (RestClientException ex) {
			log.error("Account service unavailable while validating account " + id, ex);
			return LookupResult.UNAVAILABLE;
		}
	}
}