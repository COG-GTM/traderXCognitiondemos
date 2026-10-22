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
		List<UserDetails> users = new ArrayList<>();
		addUser(users, passwordEncoder, adminUsername, adminPassword, ADMIN_ROLE);
		addUser(users, passwordEncoder, readerUsername, readerPassword, READER_ROLE);
		return new InMemoryUserDetailsManager(users);
	}

	private static void addUser(List<UserDetails> users, PasswordEncoder passwordEncoder, String username,
			String password, String role) {
		// Unconfigured credentials leave the role without any user (fail closed).
		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			return;
		}
		users.add(User.withUsername(username).password(passwordEncoder.encode(password)).roles(role).build());
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
