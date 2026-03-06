package finos.traderx.tradeservice.controller;

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
import org.springframework.web.client.RestTemplate;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.ValidationRequest;
import finos.traderx.tradeservice.model.ValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/trade", produces = "application/json")
public class TradeOrderController {

	private static final Logger log = LoggerFactory.getLogger(TradeOrderController.class);

	@Autowired
	private Publisher<TradeOrder> tradePublisher;
	
	private RestTemplate restTemplate = new RestTemplate();

	@Value("${validation.service.url}")
	private String validationServiceUrl;

	@Operation(description = "Submit a new trade order")
	@PostMapping("/")
	public ResponseEntity<TradeOrder> createTradeOrder(@Parameter(description = "the intendeded trade order") @RequestBody TradeOrder tradeOrder) {
		log.info("Called createTradeOrder");

		ValidationResult result = restTemplate.postForObject(
			validationServiceUrl + "/validate/",
			new ValidationRequest(tradeOrder.getAccountId(), tradeOrder.getSecurity()),
			ValidationResult.class
		);

		if (!result.isValid()) {
			throw new ResourceNotFoundException(result.getReason());
		}

		try {
			log.info("Trade is valid. Submitting {}", tradeOrder);
			tradePublisher.publish("/trades", tradeOrder);
			return ResponseEntity.ok(tradeOrder);
		} catch (PubSubException e) {
			throw new RuntimeException("Failed to publish trade order", e);
		}
	}
}
