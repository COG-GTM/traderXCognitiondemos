package finos.traderx.positionservice;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:position-security;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"traderx.security.jwt.hmac-secret=" + PositionServiceSecurityTest.SECRET,
		"traderx.cors.allowed-origins=http://localhost:18093, *"
})
class PositionServiceSecurityTest {

	static final String SECRET = "test-only-position-service-hmac-secret-0123456789";

	private static final int OWN_ACCOUNT = 22214;
	private static final int OTHER_ACCOUNT = 10031;

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@BeforeEach
	void seed() {
		jdbc.update("DELETE FROM ACCOUNTUSERS");
		jdbc.update("DELETE FROM POSITIONS");
		jdbc.update("DELETE FROM TRADES");
		jdbc.update("INSERT INTO ACCOUNTUSERS (ACCOUNTID, USERNAME) VALUES (?, ?)", OWN_ACCOUNT, "user01");
		jdbc.update("INSERT INTO ACCOUNTUSERS (ACCOUNTID, USERNAME) VALUES (?, ?)", OTHER_ACCOUNT, "user03");
		jdbc.update("INSERT INTO POSITIONS (ACCOUNTID, SECURITY, QUANTITY) VALUES (?, ?, ?)", OWN_ACCOUNT, "IBM", 100);
		jdbc.update("INSERT INTO POSITIONS (ACCOUNTID, SECURITY, QUANTITY) VALUES (?, ?, ?)", OTHER_ACCOUNT, "MSFT", 50);
		jdbc.update("INSERT INTO TRADES (ID, ACCOUNTID, SECURITY, SIDE, STATE, QUANTITY) VALUES (?, ?, ?, ?, ?, ?)",
				"T-OWN", OWN_ACCOUNT, "IBM", "Buy", "Settled", 100);
		jdbc.update("INSERT INTO TRADES (ID, ACCOUNTID, SECURITY, SIDE, STATE, QUANTITY) VALUES (?, ?, ?, ?, ?, ?)",
				"T-OTHER", OTHER_ACCOUNT, "MSFT", "Buy", "Settled", 50);
	}

	@Test
	void anonymousCallersAreRejected() throws Exception {
		for (String path : List.of("/positions/" + OWN_ACCOUNT, "/trades/" + OWN_ACCOUNT, "/positions/", "/trades/")) {
			mvc.perform(get(path)).andExpect(status().isUnauthorized());
		}
	}

	@Test
	void userCanReadOwnAccount() throws Exception {
		String token = token(SECRET, Map.of("preferred_username", "user01"));

		mvc.perform(withToken(get("/positions/" + OWN_ACCOUNT), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].accountId", is(OWN_ACCOUNT)));
		mvc.perform(withToken(get("/trades/" + OWN_ACCOUNT), token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is("T-OWN")));
	}

	@Test
	void userCannotReadAnotherOrUnknownAccount() throws Exception {
		String token = token(SECRET, Map.of("preferred_username", "user01"));

		for (String path : List.of("/positions/" + OTHER_ACCOUNT, "/trades/" + OTHER_ACCOUNT,
				"/positions/99999", "/trades/99999", "/positions/abc")) {
			mvc.perform(withToken(get(path), token)).andExpect(status().isForbidden());
		}
	}

	@Test
	void subjectClaimIsNotUsedAsUsername() throws Exception {
		String token = token(SECRET, Map.of("sub", "user01"));

		mvc.perform(withToken(get("/positions/" + OWN_ACCOUNT), token)).andExpect(status().isForbidden());
	}

	@Test
	void listAllEndpointsAreAdminOnly() throws Exception {
		String user = token(SECRET, Map.of("preferred_username", "user01", "roles", List.of("trader")));
		mvc.perform(withToken(get("/positions/"), user)).andExpect(status().isForbidden());
		mvc.perform(withToken(get("/trades/"), user)).andExpect(status().isForbidden());

		String admin = token(SECRET, Map.of("preferred_username", "ops", "roles", List.of("traderx-admin")));
		mvc.perform(withToken(get("/positions/"), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].accountId", containsInAnyOrder(OWN_ACCOUNT, OTHER_ACCOUNT)));
		mvc.perform(withToken(get("/trades/"), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", containsInAnyOrder("T-OWN", "T-OTHER")));
		mvc.perform(withToken(get("/trades/" + OTHER_ACCOUNT), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].accountId", everyItem(is(OTHER_ACCOUNT))));
	}

	@Test
	void forgedExpiredAndUnsignedTokensAreRejected() throws Exception {
		Map<String, Object> claims = Map.of("preferred_username", "user01", "roles", List.of("traderx-admin"));

		String forged = token("attacker-chosen-secret-that-is-long-enough!!", claims);
		mvc.perform(withToken(get("/positions/"), forged)).andExpect(status().isUnauthorized());

		String expired = token(SECRET, claims, Instant.now().minusSeconds(3600));
		mvc.perform(withToken(get("/positions/"), expired)).andExpect(status().isUnauthorized());

		String unsigned = b64("{\"alg\":\"none\"}") + "."
				+ b64("{\"preferred_username\":\"user01\",\"roles\":[\"traderx-admin\"]}") + ".";
		mvc.perform(withToken(get("/positions/"), unsigned)).andExpect(status().isUnauthorized());
	}

	@Test
	void corsIsLimitedToConfiguredOrigins() throws Exception {
		mvc.perform(options("/positions/" + OWN_ACCOUNT)
						.header(HttpHeaders.ORIGIN, "http://localhost:18093")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:18093"));

		mvc.perform(options("/positions/" + OWN_ACCOUNT)
						.header(HttpHeaders.ORIGIN, "https://evil.example")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void apiDocsStayPublic() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

	static String token(String secret, Map<String, Object> claims) throws Exception {
		return token(secret, claims, Instant.now().plusSeconds(600));
	}

	static String token(String secret, Map<String, Object> claims, Instant expiresAt) throws Exception {
		JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
				.issueTime(Date.from(expiresAt.minusSeconds(7200)))
				.expirationTime(Date.from(expiresAt));
		claims.forEach(builder::claim);
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
		jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
		return jwt.serialize();
	}

	private static String b64(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}
}
