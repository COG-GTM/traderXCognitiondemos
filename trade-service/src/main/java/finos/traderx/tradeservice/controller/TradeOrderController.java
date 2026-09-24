package finos.traderx.tradeservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.Account;
import finos.traderx.tradeservice.model.Security;
import finos.traderx.tradeservice.model.TradeOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@CrossOrigin("*")
@RestController
@RequestMapping(value = "/trade", produces = "application/json")
public class TradeOrderController {

	private static final Logger log = LoggerFactory.getLogger(TradeOrderController.class);

	private final Publisher<TradeOrder> tradePublisher;

	private final RestTemplate restTemplate;

	private final String referenceDataServiceAddress;

	private final String accountServiceAddress;

	public TradeOrderController(Publisher<TradeOrder> tradePublisher, RestTemplateBuilder restTemplateBuilder,
			@Value("${reference.data.service.url}") String referenceDataServiceAddress,
			@Value("${account.service.url}") String accountServiceAddress) {
		this.tradePublisher = tradePublisher;
		this.restTemplate = restTemplateBuilder.build();
		this.referenceDataServiceAddress = referenceDataServiceAddress;
		this.accountServiceAddress = accountServiceAddress;
	}

	@Operation(description = "Submit a new trade order")
	@PostMapping("/")
	public ResponseEntity<TradeOrder> createTradeOrder(
			@Parameter(description = "the intendeded trade order") @RequestBody TradeOrder tradeOrder) {
		log.info("Called createTradeOrder");

		if (!validateTicker(tradeOrder.security())) {
			throw new ResourceNotFoundException(tradeOrder.security() + " not found in Reference data service.");
		} else if (!validateAccount(tradeOrder.accountId())) {
			throw new ResourceNotFoundException(tradeOrder.accountId() + " not found in Account service.");
		} else {
			try {
				log.info("Trade is valid. Submitting {}", tradeOrder);
				tradePublisher.publish("/trades", tradeOrder);
				return ResponseEntity.ok(tradeOrder);
			} catch (PubSubException e) {
				throw new RuntimeException("Failed to publish trade order", e);
			}
		}
	}

	private boolean validateTicker(String ticker) {
		String url = this.referenceDataServiceAddress + "//stocks/" + ticker;

		try {
			ResponseEntity<Security> response = this.restTemplate.getForEntity(url, Security.class);
			log.info("Validate ticker {}", response.getBody());
			return true;
		} catch (HttpClientErrorException ex) {
			logLookupFailure(ex, "Ticker %s not found in reference data service.".formatted(ticker));
			return false;
		}
	}

	private boolean validateAccount(Integer id) {
		String url = this.accountServiceAddress + "//account/" + id;

		try {
			ResponseEntity<Account> response = this.restTemplate.getForEntity(url, Account.class);
			log.info("Validate account {}", response.getBody());
			return true;
		} catch (HttpClientErrorException ex) {
			logLookupFailure(ex, "Account %s not found in account service.".formatted(id));
			return false;
		}
	}

	private void logLookupFailure(HttpClientErrorException ex, String notFoundMessage) {
		if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
			log.info(notFoundMessage);
		} else {
			log.error(ex.getMessage());
		}
	}
}
