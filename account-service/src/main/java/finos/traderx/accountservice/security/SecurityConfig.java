package finos.traderx.accountservice.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	public static final String ADMIN_ROLE = "ACCOUNT_ADMIN";
	public static final String READER_ROLE = "ACCOUNT_READER";

	private static final String[] ACCOUNT_PATHS = { "/account/**", "/accountuser/**" };

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			@Value("${account.security.anonymous-reads:false}") boolean anonymousReads) throws Exception {
		http
				// Stateless HTTP Basic API: no session cookie, so no CSRF exposure.
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> {
					auth.requestMatchers("/", "/error", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
							"/api-docs/**").permitAll();
					if (anonymousReads) {
						auth.requestMatchers(HttpMethod.GET, ACCOUNT_PATHS).permitAll();
					} else {
						auth.requestMatchers(HttpMethod.GET, ACCOUNT_PATHS).hasAnyRole(READER_ROLE, ADMIN_ROLE);
					}
					auth.requestMatchers(HttpMethod.POST, ACCOUNT_PATHS).hasRole(ADMIN_ROLE);
					auth.requestMatchers(HttpMethod.PUT, ACCOUNT_PATHS).hasRole(ADMIN_ROLE);
					auth.anyRequest().denyAll();
				});
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder,
			@Value("${account.security.admin.username:}") String adminUsername,
			@Value("${account.security.admin.password:}") String adminPassword,
			@Value("${account.security.reader.username:}") String readerUsername,
			@Value("${account.security.reader.password:}") String readerPassword) {
		// Unconfigured credentials leave the role without any user (fail closed).
		boolean adminConfigured = isConfigured(adminUsername, adminPassword);
		boolean readerConfigured = isConfigured(readerUsername, readerPassword);
		List<UserDetails> users = new ArrayList<>();
		if (adminConfigured && readerConfigured && adminUsername.equals(readerUsername)) {
			if (!adminPassword.equals(readerPassword)) {
				throw new IllegalStateException(
						"Account service admin and reader share username '" + adminUsername
								+ "' but have different passwords; use distinct usernames or the same password");
			}
			users.add(buildUser(passwordEncoder, adminUsername, adminPassword, ADMIN_ROLE, READER_ROLE));
		} else {
			if (adminConfigured) {
				users.add(buildUser(passwordEncoder, adminUsername, adminPassword, ADMIN_ROLE));
			}
			if (readerConfigured) {
				users.add(buildUser(passwordEncoder, readerUsername, readerPassword, READER_ROLE));
			}
		}
		return new InMemoryUserDetailsManager(users);
	}

	private static boolean isConfigured(String username, String password) {
		return username != null && !username.isBlank() && password != null && !password.isBlank();
	}

	private static UserDetails buildUser(PasswordEncoder passwordEncoder, String username, String password,
			String... roles) {
		return User.withUsername(username).password(passwordEncoder.encode(password)).roles(roles).build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(
			@Value("${account.cors.allowed-origins:}") List<String> allowedOrigins) {
		CorsConfiguration config = new CorsConfiguration();
		allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isEmpty() && !origin.equals("*"))
				.forEach(config::addAllowedOrigin);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
		config.setAllowCredentials(false);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
