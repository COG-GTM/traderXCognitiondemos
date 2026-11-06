package finos.traderx.accountservice.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigUsersTest {

	private final SecurityConfig config = new SecurityConfig();
	private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	private static boolean hasRole(UserDetails user, String role) {
		return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
	}

	@Test
	void sharedUsernameWithSamePasswordGetsBothRoles() {
		UserDetailsService users = config.userDetailsService(encoder, "ops", "secret", "ops", "secret");
		UserDetails user = users.loadUserByUsername("ops");
		assertTrue(hasRole(user, SecurityConfig.ADMIN_ROLE));
		assertTrue(hasRole(user, SecurityConfig.READER_ROLE));
		assertTrue(encoder.matches("secret", user.getPassword()));
	}

	@Test
	void sharedUsernameWithDifferentPasswordsFailsWithClearError() {
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> config.userDetailsService(encoder, "ops", "admin-pass", "ops", "reader-pass"));
		assertTrue(e.getMessage().contains("ops"));
	}

	@Test
	void distinctUsersKeepSeparateRoles() {
		UserDetailsService users = config.userDetailsService(encoder, "admin", "a", "reader", "r");
		UserDetails reader = users.loadUserByUsername("reader");
		assertTrue(hasRole(users.loadUserByUsername("admin"), SecurityConfig.ADMIN_ROLE));
		assertTrue(hasRole(reader, SecurityConfig.READER_ROLE));
		assertFalse(hasRole(reader, SecurityConfig.ADMIN_ROLE));
	}
}
