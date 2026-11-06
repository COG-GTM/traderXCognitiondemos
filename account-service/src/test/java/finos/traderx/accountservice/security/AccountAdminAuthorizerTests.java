package finos.traderx.accountservice.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import finos.traderx.accountservice.exceptions.AdminAccessDeniedException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AccountAdminAuthorizerTests {

	private static HttpStatus statusFor(AccountAdminAuthorizer authorizer, String header) {
		return assertThrows(AdminAccessDeniedException.class, () -> authorizer.requireAdmin(header)).getStatus();
	}

	@Test
	void acceptsMatchingBearerToken() {
		AccountAdminAuthorizer authorizer = new AccountAdminAuthorizer("s3cret-admin-token");
		assertDoesNotThrow(() -> authorizer.requireAdmin("Bearer s3cret-admin-token"));
		assertDoesNotThrow(() -> authorizer.requireAdmin("bearer s3cret-admin-token"));
	}

	@Test
	void missingOrNonBearerHeaderIsUnauthorized() {
		AccountAdminAuthorizer authorizer = new AccountAdminAuthorizer("s3cret-admin-token");
		assertEquals(HttpStatus.UNAUTHORIZED, statusFor(authorizer, null));
		assertEquals(HttpStatus.UNAUTHORIZED, statusFor(authorizer, "Basic czNjcmV0"));
	}

	@Test
	void wrongTokenIsForbidden() {
		AccountAdminAuthorizer authorizer = new AccountAdminAuthorizer("s3cret-admin-token");
		assertEquals(HttpStatus.FORBIDDEN, statusFor(authorizer, "Bearer s3cret-admin-tokenX"));
		assertEquals(HttpStatus.FORBIDDEN, statusFor(authorizer, "Bearer "));
	}

	@Test
	void unconfiguredTokenDisablesAdministration() {
		AccountAdminAuthorizer authorizer = new AccountAdminAuthorizer("");
		assertEquals(HttpStatus.FORBIDDEN, statusFor(authorizer, "Bearer "));
		assertEquals(HttpStatus.FORBIDDEN, statusFor(authorizer, "Bearer anything"));
	}
}
