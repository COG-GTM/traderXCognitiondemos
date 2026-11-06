package finos.traderx.tradeprocessor.service;

import java.time.Duration;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.tradeprocessor.exceptions.InvalidTradeOrderException;
import finos.traderx.tradeprocessor.exceptions.TradeValidationUnavailableException;
import finos.traderx.tradeprocessor.model.TradeOrder;

/**
 * Validates every trade order before it is booked, whichever entry point it came from.
 * Checks the order shape locally, then confirms the security exists in reference-data
 * and the account exists in account-service. Fails closed if either service is unreachable.
 */
@Component
public class TradeOrderValidator {

	private static final Logger log = LoggerFactory.getLogger(TradeOrderValidator.class);

	static final Pattern TICKER = Pattern.compile("^[A-Za-z0-9]{1,10}([.-][A-Za-z0-9]{1,5})?$");

	private final RestTemplate restTemplate;
	private final String referenceDataServiceAddress;
	private final String accountServiceAddress;
	private final int maxOrderQuantity;

	@Autowired
	public TradeOrderValidator(RestTemplateBuilder restTemplateBuilder,
			@Value("${reference.data.service.url}") String referenceDataServiceAddress,
			@Value("${account.service.url}") String accountServiceAddress,
			@Value("${trade.processor.max-order-quantity:1000000}") int maxOrderQuantity) {
		this(restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(5))
				.build(),
				referenceDataServiceAddress, accountServiceAddress, maxOrderQuantity);
	}

	TradeOrderValidator(RestTemplate restTemplate, String referenceDataServiceAddress,
			String accountServiceAddress, int maxOrderQuantity) {
		this.restTemplate = restTemplate;
		this.referenceDataServiceAddress = stripTrailingSlash(referenceDataServiceAddress);
		this.accountServiceAddress = stripTrailingSlash(accountServiceAddress);
		this.maxOrderQuantity = maxOrderQuantity;
	}

	public void validate(TradeOrder order) {
		if (order == null) {
			throw new InvalidTradeOrderException("Trade order is required");
		}
		if (order.getAccountId() == null || order.getAccountId() <= 0) {
			throw new InvalidTradeOrderException("accountId must be a positive integer");
		}
		if (order.getSide() == null) {
			throw new InvalidTradeOrderException("side must be Buy or Sell");
		}
		if (order.getQuantity() == null || order.getQuantity() <= 0 || order.getQuantity() > maxOrderQuantity) {
			throw new InvalidTradeOrderException("quantity must be between 1 and " + maxOrderQuantity);
		}
		if (order.getSecurity() == null || !TICKER.matcher(order.getSecurity()).matches()) {
			throw new InvalidTradeOrderException("security is not a valid ticker");
		}
		requireExists(referenceDataServiceAddress + "/stocks/{ticker}", order.getSecurity(),
				"Security " + order.getSecurity() + " not found in reference data service");
		requireExists(accountServiceAddress + "/account/{id}", order.getAccountId(),
				"Account " + order.getAccountId() + " not found in account service");
	}

	private void requireExists(String urlTemplate, Object id, String notFoundMessage) {
		try {
			restTemplate.getForEntity(urlTemplate, String.class, id);
		} catch (HttpClientErrorException ex) {
			log.info("{} (HTTP {})", notFoundMessage, ex.getStatusCode().value());
			throw new InvalidTradeOrderException(notFoundMessage);
		} catch (RestClientException ex) {
			log.error("Unable to validate trade order against {}: {}", urlTemplate, ex.getMessage());
			throw new TradeValidationUnavailableException("Unable to validate trade order", ex);
		}
	}

	private static String stripTrailingSlash(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
