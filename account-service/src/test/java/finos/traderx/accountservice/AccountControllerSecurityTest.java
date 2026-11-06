package finos.traderx.accountservice;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.repository.AccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:account-security;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"traderx.security.jwt.hmac-secret=" + AccountControllerSecurityTest.SECRET
})
class AccountControllerSecurityTest {

	static final String SECRET = "test-only-account-service-hmac-secret-0123456789";

	@Autowired
	MockMvc mvc;

	@Autowired
	AccountRepository accountRepository;

	private int aliceAccountId;

	@BeforeEach
	void seed() {
		accountRepository.deleteAll();
		Account alice = new Account();
		alice.setDisplayName("Alice Book");
		aliceAccountId = accountRepository.save(alice).getId();
	}

	@Test
	void anonymousUpdateIsRejectedAndAccountIsUnchanged() throws Exception {
		mvc.perform(json(put("/account/"), body(aliceAccountId, "Mallory")))
				.andExpect(status().isUnauthorized());

		assertEquals("Alice Book", accountRepository.findById(aliceAccountId).orElseThrow().getDisplayName());
	}

	@Test
	void anonymousCreateIsRejected() throws Exception {
		mvc.perform(json(post("/account/"), body(aliceAccountId, "Mallory")))
				.andExpect(status().isUnauthorized());

		assertEquals(1, accountRepository.count());
		assertEquals("Alice Book", accountRepository.findById(aliceAccountId).orElseThrow().getDisplayName());
	}

	@Test
	void forgedOrExpiredTokensAreRejected() throws Exception {
		String forged = token("some-other-secret-that-is-at-least-32-bytes-long", adminClaims(), Instant.now().plusSeconds(300));
		String expired = token(SECRET, adminClaims(), Instant.now().minusSeconds(600));

		for (String token : List.of(forged, expired)) {
			mvc.perform(bearer(json(put("/account/"), body(aliceAccountId, "Mallory")), token))
					.andExpect(status().isUnauthorized());
		}
		assertEquals("Alice Book", accountRepository.findById(aliceAccountId).orElseThrow().getDisplayName());
	}

	@Test
	void nonAdminCannotCreateOrUpdate() throws Exception {
		String user = token(SECRET, Map.of("preferred_username", "user01"), Instant.now().plusSeconds(300));

		mvc.perform(bearer(json(put("/account/"), body(aliceAccountId, "Mallory")), user))
				.andExpect(status().isForbidden());
		mvc.perform(bearer(json(post("/account/"), body(0, "New")), user))
				.andExpect(status().isForbidden());

		assertEquals(1, accountRepository.count());
		assertEquals("Alice Book", accountRepository.findById(aliceAccountId).orElseThrow().getDisplayName());
	}

	@Test
	void adminCreateIgnoresClientSuppliedId() throws Exception {
		mvc.perform(bearer(json(post("/account/"), body(aliceAccountId, "New Book")), adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", not(is(aliceAccountId))))
				.andExpect(jsonPath("$.displayName", is("New Book")));

		assertEquals(2, accountRepository.count());
		assertEquals("Alice Book", accountRepository.findById(aliceAccountId).orElseThrow().getDisplayName());
	}

	@Test
	void adminUpdateRenamesExistingAccount() throws Exception {
		mvc.perform(bearer(json(put("/account/"), body(aliceAccountId, "  Alice Renamed  ")), adminToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(aliceAccountId)))
				.andExpect(jsonPath("$.displayName", is("Alice Renamed")));

		assertEquals(1, accountRepository.count());
	}

	@Test
	void adminUpdateOfMissingAccountReturns404AndCreatesNothing() throws Exception {
		mvc.perform(bearer(json(put("/account/"), body(aliceAccountId + 1000, "Ghost")), adminToken()))
				.andExpect(status().isNotFound());

		assertEquals(1, accountRepository.count());
	}

	@Test
	void invalidDisplayNameIsRejected() throws Exception {
		mvc.perform(bearer(json(post("/account/"), body(0, "   ")), adminToken()))
				.andExpect(status().isBadRequest());
		mvc.perform(bearer(json(put("/account/"), body(aliceAccountId, "x".repeat(51))), adminToken()))
				.andExpect(status().isBadRequest());

		assertEquals(1, accountRepository.count());
		assertEquals("Alice Book", accountRepository.findById(aliceAccountId).orElseThrow().getDisplayName());
	}

	@Test
	void readsStayPublic() throws Exception {
		mvc.perform(get("/account/")).andExpect(status().isOk());
		mvc.perform(get("/account/" + aliceAccountId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName", is("Alice Book")));
	}

	private static String body(int id, String displayName) {
		return "{\"id\":" + id + ",\"displayName\":\"" + displayName + "\"}";
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String content) {
		return request.contentType(MediaType.APPLICATION_JSON).content(content);
	}

	private static MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

	private static Map<String, Object> adminClaims() {
		return Map.of("preferred_username", "admin01", "roles", List.of("traderx-admin"));
	}

	private static String adminToken() throws Exception {
		return token(SECRET, adminClaims(), Instant.now().plusSeconds(300));
	}

	private static String token(String secret, Map<String, Object> claims, Instant expiry) throws Exception {
		JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
				.subject("subject")
				.issueTime(Date.from(Instant.now().minusSeconds(60)))
				.expirationTime(Date.from(expiry));
		claims.forEach(builder::claim);
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
		jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
		return jwt.serialize();
	}
}
