package finos.traderx.tradeservice.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import finos.traderx.tradeservice.exceptions.CreditLimitExceededException;
import finos.traderx.tradeservice.model.Account;
import finos.traderx.tradeservice.model.Position;
import finos.traderx.tradeservice.model.TradeOrder;
import finos.traderx.tradeservice.model.TradeSide;

/**
 * Enforces per-account credit limits before a trade is accepted.
 *
 * <p>Exposure is measured in net share units: the account's post-trade gross
 * exposure is the sum of the absolute net position quantity across every
 * security once the incoming order has been applied. A trade is rejected when
 * that post-trade exposure would exceed the account's configured credit limit.
 */
@Service
public class CreditCheckService {

	private static final Logger log = LoggerFactory.getLogger(CreditCheckService.class);

	private final RestTemplate restTemplate;
	private final String accountServiceAddress;
	private final String positionServiceAddress;

	@Autowired
	public CreditCheckService(
			@Value("${account.service.url}") String accountServiceAddress,
			@Value("${position.service.url}") String positionServiceAddress) {
		this(new RestTemplate(), accountServiceAddress, positionServiceAddress);
	}

	CreditCheckService(RestTemplate restTemplate, String accountServiceAddress, String positionServiceAddress) {
		this.restTemplate = restTemplate;
		this.accountServiceAddress = accountServiceAddress;
		this.positionServiceAddress = positionServiceAddress;
	}

	/**
	 * Validate the given order against the account's credit limit.
	 *
	 * @throws CreditLimitExceededException if accepting the order would push the
	 *         account's post-trade exposure beyond its credit limit.
	 */
	public void assertWithinCreditLimit(TradeOrder tradeOrder) {
		Account account = fetchAccount(tradeOrder.getAccountId());
		Long creditLimit = account == null ? null : account.getCreditLimit();
		if (creditLimit == null) {
			log.info("No credit limit configured for account {}; skipping credit check", tradeOrder.getAccountId());
			return;
		}

		long postTradeExposure = computePostTradeExposure(tradeOrder);
		log.info("Account {} post-trade exposure {} against credit limit {}",
				tradeOrder.getAccountId(), postTradeExposure, creditLimit);

		if (postTradeExposure > creditLimit) {
			throw new CreditLimitExceededException(String.format(
					"Trade rejected: account %d post-trade exposure %d exceeds credit limit %d",
					tradeOrder.getAccountId(), postTradeExposure, creditLimit));
		}
	}

	/**
	 * Compute the account's gross exposure (sum of absolute net position
	 * quantities across all securities) after applying the incoming order.
	 */
	public long computePostTradeExposure(TradeOrder tradeOrder) {
		Map<String, Long> quantitiesBySecurity = new HashMap<>();
		for (Position position : fetchPositions(tradeOrder.getAccountId())) {
			if (position.getSecurity() == null || position.getQuantity() == null) {
				continue;
			}
			quantitiesBySecurity.merge(position.getSecurity(), position.getQuantity().longValue(), Long::sum);
		}

		long signedQuantity = tradeOrder.getSide() == TradeSide.Buy
				? tradeOrder.getQuantity()
				: -tradeOrder.getQuantity();
		quantitiesBySecurity.merge(tradeOrder.getSecurity(), signedQuantity, Long::sum);

		long grossExposure = 0L;
		for (Long netQuantity : quantitiesBySecurity.values()) {
			grossExposure += Math.abs(netQuantity);
		}
		return grossExposure;
	}

	private Account fetchAccount(Integer accountId) {
		String url = this.accountServiceAddress + "//account/" + accountId;
		ResponseEntity<Account> response = this.restTemplate.getForEntity(url, Account.class);
		return response.getBody();
	}

	private Position[] fetchPositions(Integer accountId) {
		String url = this.positionServiceAddress + "/positions/" + accountId;
		ResponseEntity<Position[]> response = this.restTemplate.getForEntity(url, Position[].class);
		Position[] positions = response.getBody();
		return positions == null ? new Position[0] : positions;
	}
}
