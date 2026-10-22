package finos.traderx.positionservice.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SecurityProperties {

	@Value("${traderx.security.jwt.jwk-set-uri:}")
	private String jwkSetUri;

	@Value("${traderx.security.jwt.hmac-secret:}")
	private String hmacSecret;

	@Value("${traderx.security.jwt.issuer:}")
	private String issuer;

	@Value("${traderx.security.jwt.audience:}")
	private String audience;

	@Value("${traderx.security.jwt.username-claim:preferred_username}")
	private String usernameClaim;

	@Value("${traderx.security.jwt.roles-claim:roles}")
	private String rolesClaim;

	@Value("${traderx.security.admin-role:traderx-admin}")
	private String adminRole;

	@Value("${traderx.cors.allowed-origins:}")
	private String allowedOrigins;

	public String getJwkSetUri() {
		return jwkSetUri;
	}

	public String getHmacSecret() {
		return hmacSecret;
	}

	public String getIssuer() {
		return issuer;
	}

	public String getAudience() {
		return audience;
	}

	public String getUsernameClaim() {
		return usernameClaim;
	}

	public String getRolesClaim() {
		return rolesClaim;
	}

	public String getAdminRole() {
		return adminRole;
	}

	public List<String> getAllowedOrigins() {
		return Arrays.stream(StringUtils.commaDelimitedListToStringArray(allowedOrigins))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.filter(origin -> !origin.contains("*"))
				.toList();
	}
}
