package finos.traderx.accountservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finos.traderx.accountservice.service.AccountService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "account.security.anonymous-reads=true")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:test-application.properties")
class AccountAnonymousReadsTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	AccountService accountService;

	@Test
	void anonymousReadsDoNotOpenWrites() throws Exception {
		int id = accountService.createAccount("victim").getId();

		mvc.perform(get("/account/")).andExpect(status().isOk());
		mvc.perform(get("/account/" + id)).andExpect(status().isOk());
		mvc.perform(post("/account/").contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"x\"}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(put("/account/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"x\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void noConfiguredUsersMeansNoWriteAccess() throws Exception {
		mvc.perform(post("/account/").with(
				org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
						.httpBasic("admin", "admin"))
				.contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"x\"}"))
				.andExpect(status().isUnauthorized());
	}
}
