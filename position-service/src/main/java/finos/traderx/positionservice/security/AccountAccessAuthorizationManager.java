package finos.traderx.positionservice.security;

import java.util.function.Supplier;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import finos.traderx.positionservice.repository.AccountUserRepository;

/**
 * Grants access to an account's blotter only to admins and to users mapped to
 * that account in the AccountUsers table.
 */
@Component
public class AccountAccessAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

	static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

	private static final AuthorizationDecision GRANTED = new AuthorizationDecision(true);
	private static final AuthorizationDecision DENIED = new AuthorizationDecision(false);

	private final AccountUserRepository accountUserRepository;
	private final SecurityProperties properties;

	public AccountAccessAuthorizationManager(AccountUserRepository accountUserRepository, SecurityProperties properties) {
		this.accountUserRepository = accountUserRepository;
		this.properties = properties;
	}

	@Override
	public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) {
		Authentication authentication = authenticationSupplier.get();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return DENIED;
		}
		if (authentication.getAuthorities().stream().anyMatch(a -> ADMIN_AUTHORITY.equals(a.getAuthority()))) {
			return GRANTED;
		}

		Integer accountId = parseAccountId(context.getVariables().get("accountId"));
		String username = username(authentication);
		if (accountId == null || username == null) {
			return DENIED;
		}
		return accountUserRepository.existsByAccountIdAndUsername(accountId, username) ? GRANTED : DENIED;
	}

	private String username(Authentication authentication) {
		if (authentication.getPrincipal() instanceof Jwt jwt) {
			String username = jwt.getClaimAsString(properties.getUsernameClaim());
			return StringUtils.hasText(username) ? username : null;
		}
		return null;
	}

	private static Integer parseAccountId(String raw) {
		if (raw == null) {
			return null;
		}
		try {
			return Integer.valueOf(raw);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
