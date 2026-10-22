package finos.traderx.positionservice.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Requires a verified JWT bearer token for all blotter endpoints. Per-account
 * lookups are limited to users mapped to the account; list-all endpoints are
 * admin-only. With no verification key configured every token is rejected.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	static final int MIN_HMAC_SECRET_BYTES = 32;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AccountAccessAuthorizationManager accountAccess,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.cors(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/error", "/swagger-ui.html", "/swagger-ui/**",
						"/v3/api-docs", "/v3/api-docs/**").permitAll()
				.requestMatchers("/positions/", "/trades/").hasRole("ADMIN")
				.requestMatchers("/positions/{accountId}", "/trades/{accountId}").access(accountAccess)
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
		return http.build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecurityProperties properties) {
		NimbusJwtDecoder decoder;
		if (StringUtils.hasText(properties.getJwkSetUri())) {
			// An IdP key set signs tokens for every client; without an audience check,
			// tokens minted for other applications would be accepted here.
			if (!StringUtils.hasText(properties.getAudience())) {
				throw new IllegalStateException(
						"traderx.security.jwt.audience must be set when traderx.security.jwt.jwk-set-uri is used");
			}
			decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
		} else if (StringUtils.hasText(properties.getHmacSecret())) {
			byte[] secret = properties.getHmacSecret().getBytes(StandardCharsets.UTF_8);
			if (secret.length < MIN_HMAC_SECRET_BYTES) {
				throw new IllegalStateException("traderx.security.jwt.hmac-secret must be at least "
						+ MIN_HMAC_SECRET_BYTES + " bytes");
			}
			decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
					.macAlgorithm(MacAlgorithm.HS256)
					.build();
		} else {
			return token -> {
				throw new BadJwtException("No JWT verification key is configured");
			};
		}

		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(StringUtils.hasText(properties.getIssuer())
				? JwtValidators.createDefaultWithIssuer(properties.getIssuer())
				: JwtValidators.createDefault());
		if (StringUtils.hasText(properties.getAudience())) {
			String audience = properties.getAudience();
			validators.add(new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
					aud -> aud != null && aud.contains(audience)));
		}
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
		return decoder;
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter(SecurityProperties properties) {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setPrincipalClaimName(properties.getUsernameClaim());
		converter.setJwtGrantedAuthoritiesConverter(adminAuthorities(properties));
		return converter;
	}

	private static Converter<Jwt, Collection<GrantedAuthority>> adminAuthorities(SecurityProperties properties) {
		return jwt -> {
			Object claim = jwt.getClaims().get(properties.getRolesClaim());
			boolean admin = (claim instanceof Collection<?> roles && roles.contains(properties.getAdminRole()))
					|| (claim instanceof String role && role.equals(properties.getAdminRole()));
			return admin
					? List.of(new SimpleGrantedAuthority(AccountAccessAuthorizationManager.ADMIN_AUTHORITY))
					: List.of();
		};
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(properties.getAllowedOrigins());
		config.setAllowedMethods(List.of("GET", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		config.setAllowCredentials(false);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
