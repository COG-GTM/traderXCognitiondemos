package finos.traderx.tradeprocessor.service;

import java.util.Date;
import java.util.Objects;
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
import finos.traderx.tradeprocessor.model.*;
import finos.traderx.tradeprocessor.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class TradeService {
	static final int MAX_BOOKING_ATTEMPTS = 10;
	static final int MAX_TRADE_ID_LENGTH = 50;
	static final int MAX_SECURITY_LENGTH = 15;

	Logger log= LoggerFactory.getLogger(TradeService.class);
	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	PositionRepository positionRepository;

	@Autowired
	TransactionTemplate transactionTemplate;

	@PersistenceContext
	EntityManager entityManager;

	@Value("${trade.max-order-quantity:1000000}")
	int maxOrderQuantity;

	
    @Autowired 
    private Publisher<Trade> tradePublisher;
    
    @Autowired
    private Publisher<Position> positionPublisher;

	private record Booking(TradeBookingResult result, boolean applied) {}
    
	public TradeBookingResult processTrade(TradeOrder order) {
		log.info("Trade order received : "+order);
		validate(order);
		String tradeId=StringUtils.hasText(order.getId()) ? order.getId() : UUID.randomUUID().toString();

		Booking booking=bookWithRetry(order, tradeId);
		TradeBookingResult result=booking.result();
		if(!booking.applied()) {
			log.info("Trade {} was already booked; ignoring duplicate delivery", tradeId);
			return result;
		}

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

	private Booking bookWithRetry(TradeOrder order, String tradeId) {
		for (int attempt=1; ; attempt++) {
			try {
				return transactionTemplate.execute(status -> book(order, tradeId));
			} catch (DataIntegrityViolationException | ConcurrencyFailureException x) {
				// A concurrent booking created the same position row or trade id first; retry against the committed state.
				if (attempt >= MAX_BOOKING_ATTEMPTS) {
					throw x;
				}
				log.warn("Concurrent update booking trade {} (attempt {}): {}", tradeId, attempt, x.getMessage());
			}
		}
	}

	private Booking book(TradeOrder order, String tradeId) {
		Position position=positionRepository.lockByAccountIdAndSecurity(order.getAccountId(), order.getSecurity());
		log.info("Position for "+order.getAccountId()+" "+order.getSecurity()+" is "+position);

		Trade existing=tradeRepository.findById(tradeId).orElse(null);
		if(existing!=null) {
			if(!sameOrder(existing, order)) {
				throw new DuplicateTradeIdException("Trade id "+tradeId+" is already used by a different trade");
			}
			if(position==null) {
				position=positionRepository.findByAccountIdAndSecurity(order.getAccountId(), order.getSecurity());
			}
			return new Booking(new TradeBookingResult(existing, position), false);
		}

		boolean newPosition=position==null;
		if(newPosition) {
			log.info("Creating new position for "+order.getAccountId()+" "+order.getSecurity());
			position=new Position();
			position.setAccountId(order.getAccountId());
			position.setSecurity(order.getSecurity());
			position.setQuantity(0);
		}
		int signedQuantity=((order.getSide()==TradeSide.Buy)?1:-1)*order.getQuantity();
		int current=position.getQuantity()==null ? 0 : position.getQuantity();
		try {
			position.setQuantity(Math.addExact(current, signedQuantity));
		} catch (ArithmeticException x) {
			throw new InvalidTradeOrderException("Trade would overflow the position for account "+order.getAccountId()+" and security "+order.getSecurity());
		}
		Date now=new Date();
		position.setUpdated(now);

        Trade t=new Trade();
        t.setId(tradeId);
        t.setAccountId(order.getAccountId());
        t.setCreated(now);
        t.setUpdated(now);
        t.setSecurity(order.getSecurity());
        t.setSide(order.getSide());
        t.setQuantity(order.getQuantity());
		// Simulate the handling of this trade: New -> Processing -> Settled, committed atomically with the position.
		t.setState(TradeState.Settled);
		log.info("Trade {}",t);
		// persist (not merge) so a row committed concurrently since our read fails with a key violation instead of being overwritten.
		entityManager.persist(t);
		if(newPosition) {
			entityManager.persist(position);
		}
		return new Booking(new TradeBookingResult(t, position), true);
	}

	private void validate(TradeOrder order) {
		if(order==null) {
			throw new InvalidTradeOrderException("Trade order is required");
		}
		if(order.getAccountId()==null) {
			throw new InvalidTradeOrderException("accountId is required");
		}
		if(!StringUtils.hasText(order.getSecurity()) || order.getSecurity().length()>MAX_SECURITY_LENGTH) {
			throw new InvalidTradeOrderException("security is required and must be at most "+MAX_SECURITY_LENGTH+" characters");
		}
		if(order.getSide()==null) {
			throw new InvalidTradeOrderException("side is required");
		}
		if(order.getQuantity()==null || order.getQuantity()<=0 || order.getQuantity()>maxOrderQuantity) {
			throw new InvalidTradeOrderException("quantity must be between 1 and "+maxOrderQuantity);
		}
		if(order.getId()!=null && order.getId().length()>MAX_TRADE_ID_LENGTH) {
			throw new InvalidTradeOrderException("id must be at most "+MAX_TRADE_ID_LENGTH+" characters");
		}
	}

	private static boolean sameOrder(Trade trade, TradeOrder order) {
		return Objects.equals(trade.getAccountId(), order.getAccountId())
			&& Objects.equals(trade.getSecurity(), order.getSecurity())
			&& trade.getSide()==order.getSide()
			&& Objects.equals(trade.getQuantity(), order.getQuantity());
	}

}
