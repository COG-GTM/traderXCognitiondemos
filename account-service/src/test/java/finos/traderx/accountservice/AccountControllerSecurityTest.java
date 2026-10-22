package finos.traderx.accountservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import finos.traderx.accountservice.service.AccountService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = {
		"account.security.admin.username=test-admin",
		"account.security.admin.password=admin-pass",
		"account.security.reader.username=test-reader",
		"account.security.reader.password=reader-pass",
		"account.cors.allowed-origins=http://localhost:18093"
})
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:test-application.properties")
class AccountControllerSecurityTest {

	private static final RequestPostProcessor ADMIN = httpBasic("test-admin", "admin-pass");
	private static final RequestPostProcessor READER = httpBasic("test-reader", "reader-pass");

	@Autowired
	MockMvc mvc;

	@Autowired
	AccountService accountService;

	private final ObjectMapper mapper = new ObjectMapper();

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
		return builder.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	@Test
	void anonymousCallersAreRejected() throws Exception {
		int id = accountService.createAccount("victim").getId();

		mvc.perform(get("/account/")).andExpect(status().isUnauthorized());
		mvc.perform(get("/account/" + id)).andExpect(status().isUnauthorized());
		mvc.perform(get("/accountuser/")).andExpect(status().isUnauthorized());
		mvc.perform(json(post("/account/"), "{\"displayName\":\"attacker\"}")).andExpect(status().isUnauthorized());
		mvc.perform(json(put("/account/" + id), "{\"displayName\":\"attacker\"}")).andExpect(status().isUnauthorized());
		mvc.perform(json(post("/accountuser/"), "{\"accountId\":" + id + ",\"username\":\"user01\"}"))
				.andExpect(status().isUnauthorized());

		assertEquals("victim", accountService.getAccountById(id).getDisplayName());
	}

	@Test
	void wrongPasswordIsRejected() throws Exception {
		mvc.perform(get("/account/").with(httpBasic("test-admin", "wrong"))).andExpect(status().isUnauthorized());
	}

	@Test
	void readerCanReadButNotWrite() throws Exception {
		int id = accountService.createAccount("victim").getId();

		mvc.perform(get("/account/").with(READER)).andExpect(status().isOk());
		mvc.perform(get("/account/" + id).with(READER)).andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("victim"));
		mvc.perform(json(post("/account/"), "{\"displayName\":\"x\"}").with(READER)).andExpect(status().isForbidden());
		mvc.perform(json(put("/account/" + id), "{\"displayName\":\"x\"}").with(READER))
				.andExpect(status().isForbidden());
		mvc.perform(json(put("/accountuser/"), "{\"accountId\":" + id + ",\"username\":\"user01\"}").with(READER))
				.andExpect(status().isForbidden());

		assertEquals("victim", accountService.getAccountById(id).getDisplayName());
	}

	@Test
	void createIgnoresClientSuppliedId() throws Exception {
		int victimId = accountService.createAccount("victim").getId();

		String body = mvc.perform(json(post("/account/"), "{\"id\":" + victimId + ",\"displayName\":\"new account\"}")
				.with(ADMIN))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode created = mapper.readTree(body);

		assertNotEquals(victimId, created.get("id").asInt());
		assertEquals("new account", created.get("displayName").asText());
		assertEquals("victim", accountService.getAccountById(victimId).getDisplayName());
	}

	@Test
	void updateUsesPathIdAndRequiresExistingAccount() throws Exception {
		int id = accountService.createAccount("before").getId();
		int otherId = accountService.createAccount("other").getId();

		mvc.perform(json(put("/account/" + id), "{\"id\":" + otherId + ",\"displayName\":\"after\"}").with(ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.displayName").value("after"));
		assertEquals("after", accountService.getAccountById(id).getDisplayName());
		assertEquals("other", accountService.getAccountById(otherId).getDisplayName());

		mvc.perform(json(put("/account/987654"), "{\"displayName\":\"ghost\"}").with(ADMIN))
				.andExpect(status().isNotFound());
	}

	@Test
	void legacyBodyIdUpdateRouteIsGone() throws Exception {
		int id = accountService.createAccount("victim").getId();

		mvc.perform(json(put("/account/"), "{\"id\":" + id + ",\"displayName\":\"attacker\"}").with(ADMIN))
				.andExpect(status().isMethodNotAllowed());
		assertEquals("victim", accountService.getAccountById(id).getDisplayName());
	}

	@Test
	void invalidDisplayNameIsRejected() throws Exception {
		mvc.perform(json(post("/account/"), "{\"displayName\":\"  \"}").with(ADMIN)).andExpect(status().isBadRequest());
		mvc.perform(json(post("/account/"), "{\"displayName\":\"" + "x".repeat(51) + "\"}").with(ADMIN))
				.andExpect(status().isBadRequest());
	}

	@Test
	void corsOnlyAllowsConfiguredOrigins() throws Exception {
		mvc.perform(options("/account/")
				.header("Origin", "https://evil.example")
				.header("Access-Control-Request-Method", "PUT"))
				.andExpect(status().isForbidden());

		mvc.perform(options("/account/")
				.header("Origin", "http://localhost:18093")
				.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:18093"));
	}
}
