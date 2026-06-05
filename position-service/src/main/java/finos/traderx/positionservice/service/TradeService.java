package finos.traderx.positionservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import finos.traderx.positionservice.model.*;
import finos.traderx.positionservice.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TradeService {

	private static final Logger log = LoggerFactory.getLogger(TradeService.class);

	@Autowired
	TradeRepository tradeRepository;

	public List<Trade> getAllTrades() {
		List<Trade> trades = new ArrayList<Trade>();
		this.tradeRepository.findAll().forEach(trade -> trades.add(trade));
		return trades;
	}

	public List<Trade> getTradesByAccountID(int id) {
		return this.tradeRepository.findByAccountId(id);
	}

	/**
	 * A trade only contributes to position calculations once it has cleared
	 * compliance. Orders that are still PENDING_REVIEW, FLAGGED, or REJECTED must
	 * not move a position. This guards the position calculation against trades that
	 * have not been cleared by the compliance workflow.
	 */
	public boolean isCompliantForProcessing(Trade trade) {
		boolean compliant = trade != null
				&& ComplianceStatus.APPROVED.name().equalsIgnoreCase(trade.getComplianceStatus());
		if (!compliant && trade != null) {
			log.info("Skipping trade {} in position calculation - compliance status is {}",
					trade.getId(), trade.getComplianceStatus());
		}
		return compliant;
	}

	/**
	 * Returns only the trades for an account that have cleared compliance and may
	 * therefore be included in position calculations.
	 */
	public List<Trade> getCompliantTradesByAccountID(int id) {
		return this.tradeRepository.findByAccountId(id).stream()
				.filter(this::isCompliantForProcessing)
				.collect(Collectors.toList());
	}

}
