package finos.traderx.tradeprocessor.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import finos.traderx.tradeprocessor.model.TradeBookingResult;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.service.TradeService;

/**
 * Internal, service-authenticated order booking endpoint. Disabled (404) unless
 * {@code trade.processor.order.token} is set; callers must send it as a Bearer token.
 * Public order entry goes through trade-service, which publishes to the trade feed.
 */
@RestController
@RequestMapping("/tradeservice")
public class TradeServiceController {

	private static final Logger log = LoggerFactory.getLogger(TradeServiceController.class);

	private static final String BEARER_PREFIX = "Bearer ";

	@Autowired
	TradeService tradeService;

	@Value("${trade.processor.order.token:}")
	private String orderToken;

	@PostMapping("/order")
	public ResponseEntity<TradeBookingResult> processOrder(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody TradeOrder order) {
		if (orderToken == null || orderToken.isBlank()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		if (!isAuthorized(authorization)) {
			log.warn("Rejected unauthenticated call to /tradeservice/order");
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		TradeBookingResult result = tradeService.processTrade(order);
		return ResponseEntity.ok(result);
	}

	private boolean isAuthorized(String authorization) {
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			return false;
		}
		byte[] presented = authorization.substring(BEARER_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(presented, orderToken.getBytes(StandardCharsets.UTF_8));
	}
}
