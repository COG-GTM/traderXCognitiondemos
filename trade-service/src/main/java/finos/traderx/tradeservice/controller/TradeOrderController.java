package finos.traderx.tradeservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.exceptions.ResourceNotFoundException;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.service.ValidationServiceClient;
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
	private ValidationServiceClient validationServiceClient;

	@Operation(description = "Submit a new trade order")
	@PostMapping("/")
	public ResponseEntity<TradeOrder> createTradeOrder(@Parameter(description = "the intendeded trade order") @RequestBody TradeOrder tradeOrder) {
		log.info("Called createTradeOrder");
		
		ValidationServiceClient.ValidationResult validationResult = validationServiceClient.validate(tradeOrder.getSecurity(), tradeOrder.getAccountId());
		
		if (!validationResult.isValid()) {
			throw new ResourceNotFoundException(validationResult.getErrorMessage());
		}

		try{
			log.info("Trade is valid. Submitting {}", tradeOrder);
			tradePublisher.publish("/trades",tradeOrder);
			return  ResponseEntity.ok(tradeOrder);
		}  catch (PubSubException e){
			throw new RuntimeException("Failed to publish trade order", e);
		}
	}
}
