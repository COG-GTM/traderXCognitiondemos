package finos.traderx.tradeservice.controller;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping(value="/trade", produces = "application/json")
public class TradeOrderController {

	private static final Logger log = LoggerFactory.getLogger(TradeOrderController.class);

	@Autowired
	private Publisher<TradeOrder> tradePublisher;

	@Autowired
	private Tracer tracer;

	private RestTemplate restTemplate = new RestTemplate();

	@Value("${reference.data.service.url}")
	private String referenceDataServiceAddress;

	@Value("${account.service.url}")
	private String accountServiceAddress;

	@Operation(description = "Submit a new trade order")
	@PostMapping("/")
	@WithSpan("trade.submit")
	public ResponseEntity<TradeOrder> createTradeOrder(@Parameter(description = "the intendeded trade order") @RequestBody TradeOrder tradeOrder) {
		Span parentSpan = Span.current();
		parentSpan.setAttribute("trade.security", tradeOrder.getSecurity());
		if (tradeOrder.getAccountId() != null) {
			parentSpan.setAttribute("trade.accountId", (long) tradeOrder.getAccountId());
		}
		if (tradeOrder.getQuantity() != null) {
			parentSpan.setAttribute("trade.quantity", (long) tradeOrder.getQuantity());
		}
		parentSpan.setAttribute("trade.side", tradeOrder.getSide() != null ? tradeOrder.getSide().name() : "unknown");
		log.info("Called createTradeOrder: security={}, accountId={}, side={}, quantity={}",
				tradeOrder.getSecurity(), tradeOrder.getAccountId(), tradeOrder.getSide(), tradeOrder.getQuantity());

		if (!validateTicker(tradeOrder.getSecurity()))
		{
			parentSpan.setStatus(StatusCode.ERROR, "Invalid ticker");
			parentSpan.setAttribute("trade.validation.result", "ticker_not_found");
			throw new ResourceNotFoundException(tradeOrder.getSecurity() + " not found in Reference data service.");
		}
		else if(!validateAccount(tradeOrder.getAccountId()))
		{
			parentSpan.setStatus(StatusCode.ERROR, "Invalid account");
			parentSpan.setAttribute("trade.validation.result", "account_not_found");
			throw new ResourceNotFoundException(tradeOrder.getAccountId() + " not found in Account service.");
		}
		else
		{
			try{
				parentSpan.setAttribute("trade.validation.result", "passed");
				parentSpan.addEvent("Trade validated successfully");
				log.info("Trade is valid. Submitting {}", tradeOrder);
				publishTrade(tradeOrder);
				parentSpan.setStatus(StatusCode.OK);
				parentSpan.addEvent("Trade published to feed");
				return  ResponseEntity.ok(tradeOrder);
			}  catch (PubSubException e){
				parentSpan.recordException(e);
				parentSpan.setStatus(StatusCode.ERROR, "Failed to publish trade order");
				throw new RuntimeException("Failed to publish trade order", e);
			}
		}
	}

	private boolean validateTicker(String ticker)
	{
		Span span = tracer.spanBuilder("trade.validateTicker")
				.setParent(Context.current())
				.startSpan();
		try (Scope scope = span.makeCurrent()) {
			span.setAttribute("trade.ticker", ticker);
			String url = this.referenceDataServiceAddress + "//stocks/" + ticker;
			ResponseEntity<Security> response = null;

			try {
				response = this.restTemplate.getForEntity(url, Security.class);
				log.info("Validate ticker {}", response.getBody().toString());
				span.setStatus(StatusCode.OK);
				span.setAttribute("trade.ticker.valid", true);
				return true;
			}
			catch (HttpClientErrorException ex) {
				if (ex.getRawStatusCode() == 404) {
					log.info("{} not found in reference data service.", ticker);
					span.setAttribute("trade.ticker.valid", false);
					span.setStatus(StatusCode.ERROR, "Ticker not found");
				}
				else {
					log.error(ex.getMessage());
					span.recordException(ex);
					span.setStatus(StatusCode.ERROR, ex.getMessage());
				}
				return false;
			}
		} finally {
			span.end();
		}
	}

	private boolean validateAccount(Integer id)
	{
		Span span = tracer.spanBuilder("trade.validateAccount")
				.setParent(Context.current())
				.startSpan();
		try (Scope scope = span.makeCurrent()) {
			span.setAttribute("trade.accountId", id);
			String url = this.accountServiceAddress + "//account/" + id;
			ResponseEntity<Account> response = null;

			try
			{
					response = this.restTemplate.getForEntity(url, Account.class);
					log.info("Validate account {}", response.getBody().toString());
					span.setStatus(StatusCode.OK);
					span.setAttribute("trade.account.valid", true);
					return true;
			}
			catch (HttpClientErrorException ex) {
				if (ex.getRawStatusCode() == 404) {
					log.info("Account {} not found in account service.", id);
					span.setAttribute("trade.account.valid", false);
					span.setStatus(StatusCode.ERROR, "Account not found");
				}
				else {
					log.error(ex.getMessage());
					span.recordException(ex);
					span.setStatus(StatusCode.ERROR, ex.getMessage());
				}
				return false;
			}
		} finally {
			span.end();
		}
	}

	private void publishTrade(TradeOrder tradeOrder) throws PubSubException {
		Span span = tracer.spanBuilder("trade.publish")
				.setParent(Context.current())
				.startSpan();
		try (Scope scope = span.makeCurrent()) {
			span.setAttribute("trade.security", tradeOrder.getSecurity());
			span.setAttribute("trade.accountId", tradeOrder.getAccountId());
			span.setAttribute("messaging.destination", "/trades");
			span.setAttribute("messaging.system", "socketio");
			tradePublisher.publish("/trades", tradeOrder);
			span.setStatus(StatusCode.OK);
			log.info("Trade published successfully for account={}, security={}",
					tradeOrder.getAccountId(), tradeOrder.getSecurity());
		} catch (PubSubException e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR, "Failed to publish trade");
			throw e;
		} finally {
			span.end();
		}
	}
}
