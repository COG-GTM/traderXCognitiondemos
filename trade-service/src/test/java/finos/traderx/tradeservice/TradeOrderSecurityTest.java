package finos.traderx.tradeservice;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.controller.TradeOrderController;
import finos.traderx.tradeservice.model.TradeOrder;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:trade-security;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"reference.data.service.url=http://reference-data",
		"account.service.url=http://account-service",
		"traderx.security.jwt.hmac-secret=" + TradeOrderSecurityTest.SECRET,
		"traderx.cors.allowed-origins=http://localhost:18093, *"
})
class TradeOrderSecurityTest {

	static final String SECRET = "test-only-trade-service-hmac-secret-0123456789";

	private static final int OWN_ACCOUNT = 22214;
	private static final int OTHER_ACCOUNT = 11413;

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	TradeOrderController controller;

	@MockBean
	Publisher<TradeOrder> tradePublisher;

	MockRestServiceServer downstream;

	@BeforeEach
	void setUp() {
		jdbc.update("DELETE FROM ACCOUNTUSERS");
		jdbc.update("INSERT INTO ACCOUNTUSERS (ACCOUNTID, USERNAME) VALUES (?, ?)", OWN_ACCOUNT, "user01");
		jdbc.update("INSERT INTO ACCOUNTUSERS (ACCOUNTID, USERNAME) VALUES (?, ?)", OTHER_ACCOUNT, "user03");
		RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(controller, "restTemplate");
		downstream = MockRestServiceServer.bindTo(restTemplate).build();
	}

	@Test
	void anonymousOrderIsRejected() throws Exception {
		mvc.perform(order(OTHER_ACCOUNT)).andExpect(status().isUnauthorized());

		verify(tradePublisher, never()).publish(any(), any());
		downstream.verify();
	}

	@Test
	void tokenSignedWithAnotherKeyIsRejected() throws Exception {
		String forged = token("some-other-secret-that-is-at-least-32-bytes", Map.of("preferred_username", "user03"));

		mvc.perform(withToken(order(OTHER_ACCOUNT), forged)).andExpect(status().isUnauthorized());

		verify(tradePublisher, never()).publish(any(), any());
	}

	@Test
	void userCannotTradeAnotherOrUnknownAccount() throws Exception {
		String token = token(SECRET, Map.of("preferred_username", "user01"));

		mvc.perform(withToken(order(OTHER_ACCOUNT), token)).andExpect(status().isForbidden());
		mvc.perform(withToken(order(99999), token)).andExpect(status().isForbidden());
		mvc.perform(withToken(post("/trade/").contentType(MediaType.APPLICATION_JSON)
				.content("{\"security\":\"IBM\",\"quantity\":10,\"side\":\"Buy\"}"), token))
				.andExpect(status().isForbidden());

		verify(tradePublisher, never()).publish(any(), any());
		downstream.verify();
	}

	@Test
	void subjectClaimIsNotUsedAsUsername() throws Exception {
		String token = token(SECRET, Map.of("sub", "user01"));

		mvc.perform(withToken(order(OWN_ACCOUNT), token)).andExpect(status().isForbidden());

		verify(tradePublisher, never()).publish(any(), any());
	}

	@Test
	void userCanTradeOwnAccount() throws Exception {
		downstream.expect(requestTo(endsWith("/stocks/IBM")))
				.andRespond(withSuccess("{\"ticker\":\"IBM\",\"companyName\":\"IBM\"}", MediaType.APPLICATION_JSON));
		downstream.expect(requestTo(endsWith("/account/" + OWN_ACCOUNT)))
				.andRespond(withSuccess("{\"id\":" + OWN_ACCOUNT + ",\"displayName\":\"Test\"}", MediaType.APPLICATION_JSON));
		String token = token(SECRET, Map.of("preferred_username", "user01"));

		mvc.perform(withToken(order(OWN_ACCOUNT), token)).andExpect(status().isOk());

		downstream.verify();
		verify(tradePublisher).publish(eq("/trades"), argThat(o -> o.getAccountId() == OWN_ACCOUNT));
	}

	@Test
	void corsAllowsOnlyConfiguredOrigins() throws Exception {
		mvc.perform(options("/trade/")
				.header(HttpHeaders.ORIGIN, "http://localhost:18093")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:18093"));
		mvc.perform(options("/trade/")
				.header(HttpHeaders.ORIGIN, "https://evil.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	private static MockHttpServletRequestBuilder order(int accountId) {
		return post("/trade/")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":" + accountId + ",\"security\":\"IBM\",\"quantity\":10,\"side\":\"Sell\"}");
	}

	private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

	static String token(String secret, Map<String, Object> claims) throws Exception {
		JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
				.issueTime(new Date())
				.expirationTime(Date.from(Instant.now().plusSeconds(300)));
		claims.forEach(builder::claim);
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
		jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
		return jwt.serialize();
	}
}
