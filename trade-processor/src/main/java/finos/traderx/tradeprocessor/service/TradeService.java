package finos.traderx.tradeprocessor.service;

import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.exception.InvalidTradeOrderException;
import finos.traderx.tradeprocessor.model.*;
import finos.traderx.tradeprocessor.repository.*;

@Service
public class TradeService {
	private static final int MAX_BOOKING_ATTEMPTS = 3;

	Logger log= LoggerFactory.getLogger(TradeService.class);
	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	PositionRepository positionRepository;

	@Autowired
	TransactionTemplate transactionTemplate;

	@Value("${trade.max-quantity:1000000}")
	int maxQuantity;

    @Autowired 
    private Publisher<Trade> tradePublisher;
    
    @Autowired
    private Publisher<Position> positionPublisher;
    
	public TradeBookingResult processTrade(TradeOrder order) {
		log.info("Trade order received : "+order);
		validate(order);

		TradeBookingResult result=bookWithRetry(order);
		log.info("Trade Processing complete : "+result);
		try{
			log.info("Publishing : "+result);
			tradePublisher.publish("/accounts/"+order.getAccountId()+"/trades", result.getTrade());
			positionPublisher.publish("/accounts/"+order.getAccountId()+"/positions", result.getPosition());
		} catch (PubSubException exc){
			log.error("Error publishing trade "+order,exc);
		}
		
		return result;	
	}

	void validate(TradeOrder order) {
		if(order==null) {
			throw new InvalidTradeOrderException("Trade order is required");
		}
		if(order.getAccountId()==null) {
			throw new InvalidTradeOrderException("Trade order accountId is required");
		}
		if(!StringUtils.hasText(order.getSecurity())) {
			throw new InvalidTradeOrderException("Trade order security is required");
		}
		if(order.getSide()==null) {
			throw new InvalidTradeOrderException("Trade order side is required");
		}
		Integer quantity=order.getQuantity();
		if(quantity==null || quantity<1 || quantity>maxQuantity) {
			throw new InvalidTradeOrderException("Trade order quantity must be between 1 and "+maxQuantity);
		}
	}

	private TradeBookingResult bookWithRetry(TradeOrder order) {
		for(int attempt=1;;attempt++) {
			try {
				return transactionTemplate.execute(status -> book(order));
			} catch (ConcurrencyFailureException | DataIntegrityViolationException exc) {
				// A concurrent order created the same position row first; retry so the existing row is locked and updated.
				if(attempt>=MAX_BOOKING_ATTEMPTS) {
					throw exc;
				}
				log.warn("Retrying booking of "+order+" after concurrent position update (attempt "+attempt+")",exc);
			}
		}
	}

	private TradeBookingResult book(TradeOrder order) {
        Trade t=new Trade();
        t.setAccountId(order.getAccountId());

		log.info("Setting a random TradeID");
		t.setId(UUID.randomUUID().toString());


        t.setCreated(new Date());
        t.setUpdated(new Date());
        t.setSecurity(order.getSecurity());
        t.setSide(order.getSide());
        t.setQuantity(order.getQuantity());
		t.setState(TradeState.New);
		Position position=positionRepository.findForUpdateByAccountIdAndSecurity(order.getAccountId(), order.getSecurity());
		log.info("Position for "+order.getAccountId()+" "+order.getSecurity()+" is "+position);
		if(position==null) {
			log.info("Creating new position for "+order.getAccountId()+" "+order.getSecurity());
			positionRepository.insertEmptyPosition(order.getAccountId(), order.getSecurity(), new Date());
			position=positionRepository.findForUpdateByAccountIdAndSecurity(order.getAccountId(), order.getSecurity());
		}
		int current=position.getQuantity()==null?0:position.getQuantity();
		int delta=(order.getSide()==TradeSide.Buy)?t.getQuantity():-t.getQuantity();
		int updatedQuantity;
		try {
			updatedQuantity=Math.addExact(current,delta);
		} catch (ArithmeticException exc) {
			throw new InvalidTradeOrderException("Trade order would overflow the position for account "+order.getAccountId()+" and security "+order.getSecurity());
		}
		position.setQuantity(updatedQuantity);
		position.setUpdated(new Date());
		log.info("Trade {}",t);
		tradeRepository.save(t);
		positionRepository.save(position);
		// Simulate the handling of this trade...
		// Now mark as processing
		t.setUpdated(new Date());
		t.setState(TradeState.Processing);
		// Now mark as settled
		t.setUpdated(new Date());
		t.setState(TradeState.Settled);
		tradeRepository.save(t);

		return new TradeBookingResult(t, position);
	}

}
