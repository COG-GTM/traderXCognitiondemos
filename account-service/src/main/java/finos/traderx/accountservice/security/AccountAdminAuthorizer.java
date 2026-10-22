package finos.traderx.accountservice.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import finos.traderx.accountservice.exceptions.AdminAccessDeniedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Guards administrative changes (e.g. account-user mappings). Callers must present
 * {@code Authorization: Bearer <token>} matching {@code traderx.account-admin.token}.
 * When no token is configured, administration is disabled and every change is refused.
 */
@Component
public class AccountAdminAuthorizer {

	private static final Logger logger = LoggerFactory.getLogger(AccountAdminAuthorizer.class);

	private static final String BEARER_PREFIX = "Bearer ";

	private final byte[] adminToken;

	public AccountAdminAuthorizer(@Value("${traderx.account-admin.token:}") String adminToken) {
		String token = adminToken == null ? "" : adminToken.trim();
		this.adminToken = token.getBytes(StandardCharsets.UTF_8);
		if (token.isEmpty()) {
			logger.warn("traderx.account-admin.token (ACCOUNT_ADMIN_TOKEN) is not set; account-user changes are disabled");
		}
	}

	public void requireAdmin(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			throw new AdminAccessDeniedException(HttpStatus.UNAUTHORIZED, "Admin bearer token required");
		}
		if (this.adminToken.length == 0) {
			throw new AdminAccessDeniedException(HttpStatus.FORBIDDEN, "Account administration is disabled");
		}
		byte[] presented = authorizationHeader.substring(BEARER_PREFIX.length()).trim().getBytes(StandardCharsets.UTF_8);
		if (!MessageDigest.isEqual(this.adminToken, presented)) {
			throw new AdminAccessDeniedException(HttpStatus.FORBIDDEN, "Admin role required");
		}
	}
}
