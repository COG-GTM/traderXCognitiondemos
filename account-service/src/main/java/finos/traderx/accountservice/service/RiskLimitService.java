package finos.traderx.accountservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import finos.traderx.accountservice.config.RiskLimitProperties;
import finos.traderx.accountservice.exceptions.RiskLimitsDisabledException;
import finos.traderx.accountservice.model.MissingLimitPolicy;
import finos.traderx.accountservice.model.RiskLimit;
import finos.traderx.accountservice.model.RiskLimitChangeType;
import finos.traderx.accountservice.model.RiskLimitHistory;
import finos.traderx.accountservice.model.RiskLimitRequest;
import finos.traderx.accountservice.model.RiskLimitView;
import finos.traderx.accountservice.repository.RiskLimitHistoryRepository;
import finos.traderx.accountservice.repository.RiskLimitRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns pre-trade risk limits. account-service is the authority that sets them; trade-service
 * only reads them at enforcement time, which is what keeps limit-setting independent of the
 * desk that trades against them (MiFID II RTS 6 Art. 15).
 */
@Service
public class RiskLimitService {

	@Autowired
	RiskLimitRepository riskLimitRepository;

	@Autowired
	RiskLimitHistoryRepository riskLimitHistoryRepository;

	@Autowired
	AccountService accountService;

	@Autowired
	RiskLimitProperties riskLimitProperties;

	/**
	 * Reads the limit in force for an account. Throws if the account itself is unknown;
	 * an account that exists but has no limit is a valid state, described by the returned view.
	 */
	public RiskLimitView getRiskLimit(int accountId) {
		this.accountService.getAccountById(accountId);

		if (!this.riskLimitProperties.isEnabled()) {
			return RiskLimitView.absent(accountId, MissingLimitPolicy.UNLIMITED);
		}

		Optional<RiskLimit> limit = this.riskLimitRepository.findById(accountId);
		return limit
				.map(l -> RiskLimitView.of(l, this.riskLimitProperties.getMissingLimitPolicy()))
				.orElseGet(() -> RiskLimitView.absent(accountId, this.riskLimitProperties.getMissingLimitPolicy()));
	}

	public List<RiskLimitHistory> getRiskLimitHistory(int accountId) {
		this.accountService.getAccountById(accountId);

		if (!this.riskLimitProperties.isEnabled()) {
			return List.of();
		}

		return this.riskLimitHistoryRepository.findByAccountIdOrderByChangedAtDescIdDesc(accountId);
	}

	/**
	 * Sets or amends the limit for an account. Every accepted change appends a history row
	 * holding the newly effective value together with who changed it and why; the superseded
	 * value survives as the preceding row, so the limit in force at any past point in time
	 * remains reconstructible.
	 */
	@Transactional
	public RiskLimitView setRiskLimit(int accountId, RiskLimitRequest request) {
		if (!this.riskLimitProperties.isEnabled()) {
			throw new RiskLimitsDisabledException("Risk limit administration is disabled (traderx.risk-limit.enabled=false)");
		}

		this.accountService.getAccountById(accountId);
		validate(request);

		Date now = new Date();
		Optional<RiskLimit> existing = this.riskLimitRepository.findById(accountId);
		RiskLimitChangeType changeType = existing.isPresent() ? RiskLimitChangeType.AMEND : RiskLimitChangeType.CREATE;

		RiskLimit limit = existing.orElseGet(RiskLimit::new);
		limit.setAccountId(accountId);
		limit.setMaxOrderNotional(request.getMaxOrderNotional().stripTrailingZeros().setScale(2, RoundingMode.UNNECESSARY));
		limit.setCurrency(request.getCurrency().toUpperCase(Locale.ROOT));
		limit.setEffectiveFrom(request.getEffectiveFrom() == null ? now : request.getEffectiveFrom());
		limit.setSetBy(request.getSetBy());
		limit.setUpdated(now);

		RiskLimit saved = this.riskLimitRepository.save(limit);
		this.riskLimitHistoryRepository.save(historyOf(saved, changeType, now, request.getReason()));

		return RiskLimitView.of(saved, this.riskLimitProperties.getMissingLimitPolicy());
	}

	private RiskLimitHistory historyOf(RiskLimit limit, RiskLimitChangeType changeType, Date changedAt, String reason) {
		RiskLimitHistory history = new RiskLimitHistory();
		history.setAccountId(limit.getAccountId());
		history.setMaxOrderNotional(limit.getMaxOrderNotional());
		history.setCurrency(limit.getCurrency());
		history.setEffectiveFrom(limit.getEffectiveFrom());
		history.setSetBy(limit.getSetBy());
		history.setChangeType(changeType);
		history.setChangedBy(limit.getSetBy());
		history.setChangedAt(changedAt);
		history.setReason(reason);
		return history;
	}

	private void validate(RiskLimitRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Risk limit payload is required");
		}
		if (request.getMaxOrderNotional() == null || request.getMaxOrderNotional().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("maxOrderNotional must be present and not negative");
		}
		if (request.getMaxOrderNotional().stripTrailingZeros().scale() > 2
				|| request.getMaxOrderNotional().precision() - request.getMaxOrderNotional().scale() > 17) {
			throw new IllegalArgumentException("maxOrderNotional must fit DECIMAL(19,2); it is stored and enforced to 2 decimal places");
		}
		if (request.getCurrency() == null || !isIsoCurrency(request.getCurrency())) {
			throw new IllegalArgumentException("currency must be a 3 letter ISO 4217 code");
		}
		if (request.getSetBy() == null || request.getSetBy().isBlank() || request.getSetBy().length() > 50) {
			throw new IllegalArgumentException("setBy must identify who set the limit and be at most 50 characters");
		}
		if (request.getReason() != null && request.getReason().length() > 255) {
			throw new IllegalArgumentException("reason must be at most 255 characters");
		}
		if (request.getEffectiveFrom() != null && request.getEffectiveFrom().after(new Date())) {
			throw new IllegalArgumentException("effectiveFrom cannot be in the future; a stored limit is always the limit in force");
		}
	}

	private boolean isIsoCurrency(String code) {
		try {
			Currency.getInstance(code.toUpperCase(Locale.ROOT));
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
