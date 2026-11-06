package finos.traderx.tradeservice.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import finos.traderx.tradeservice.repository.AccountUserRepository;

/**
 * Allows a caller to trade an account only when the AccountUsers table maps the
 * caller's username to that account.
 */
@Component
public class TradeAccountAuthorizer {

	private final AccountUserRepository accountUserRepository;
	private final SecurityProperties properties;

	public TradeAccountAuthorizer(AccountUserRepository accountUserRepository, SecurityProperties properties) {
		this.accountUserRepository = accountUserRepository;
		this.properties = properties;
	}

	public boolean canTrade(Jwt jwt, Integer accountId) {
		if (jwt == null || accountId == null) {
			return false;
		}
		String username = jwt.getClaimAsString(properties.getUsernameClaim());
		if (!StringUtils.hasText(username)) {
			return false;
		}
		return accountUserRepository.existsByAccountIdAndUsername(accountId, username);
	}
}
