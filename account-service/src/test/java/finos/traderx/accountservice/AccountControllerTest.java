package finos.traderx.accountservice;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.service.AccountService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "/test-application.properties")
class AccountControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	AccountService accountService;

	int accountId;

	@BeforeEach
	void seedAccount() {
		Account account = new Account();
		account.setDisplayName("Test Account");
		accountId = accountService.upsertAccount(account).getId();
	}

	@Test
	void getAccountById() throws Exception {
		mockMvc.perform(get("/account/{id}", accountId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(accountId))
			.andExpect(jsonPath("$.displayName").value("Test Account"));
	}

	@Test
	void getAllAccounts() throws Exception {
		mockMvc.perform(get("/account/"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.id == " + accountId + ")].displayName").value("Test Account"));
	}

	@Test
	void unknownAccountReturns404() throws Exception {
		mockMvc.perform(get("/account/{id}", 999999))
			.andExpect(status().isNotFound())
			.andExpect(content().string(containsString("999999")));
	}

	@Test
	void createAccount() throws Exception {
		mockMvc.perform(post("/account/")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"displayName\":\"Created Account\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("Created Account"))
			.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	void rootRedirectsToSwaggerUi() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("swagger-ui.html"));
	}

	@Test
	void traversalSegmentsAreNotRoutedToController() throws Exception {
		mockMvc.perform(get("/account/%2e%2e/account/{id}", accountId))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/account/{id}/.", accountId))
			.andExpect(status().isNotFound());
	}
}
