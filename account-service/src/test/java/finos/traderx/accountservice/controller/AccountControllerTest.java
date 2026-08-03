package finos.traderx.accountservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.service.AccountService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	AccountService accountService;

	private Account account(int id, String displayName) {
		Account account = new Account();
		account.setId(id);
		account.setDisplayName(displayName);
		return account;
	}

	@Test
	@DisplayName("GET /account/{id} returns the requested account")
	void getAccountByIdReturnsAccount() throws Exception {
		when(this.accountService.getAccountById(1)).thenReturn(account(1, "Trading Desk"));

		this.mockMvc.perform(get("/account/{id}", 1))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.displayName").value("Trading Desk"));
	}

	@Test
	@DisplayName("GET /account/{id} returns 404 when the account does not exist")
	void getAccountByIdReturnsNotFound() throws Exception {
		when(this.accountService.getAccountById(404))
				.thenThrow(new ResourceNotFoundException("Account with id 404 not found"));

		this.mockMvc.perform(get("/account/{id}", 404))
				.andExpect(status().isNotFound())
				.andExpect(content().string("Account with id 404 not found"));
	}

	@Test
	@DisplayName("GET /account/{id} with a non-numeric id is handled by the controller error handler")
	void getAccountByIdRejectsNonNumericId() throws Exception {
		this.mockMvc.perform(get("/account/{id}", "not-a-number"))
				.andExpect(status().isInternalServerError());

		verify(this.accountService, never()).getAccountById(anyInt());
	}

	@Test
	@DisplayName("GET /account/ returns all accounts")
	void getAllAccountsReturnsAccounts() throws Exception {
		when(this.accountService.getAllAccount())
				.thenReturn(Arrays.asList(account(1, "Trading Desk"), account(2, "Treasury")));

		this.mockMvc.perform(get("/account/"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[1].displayName").value("Treasury"));
	}

	@Test
	@DisplayName("GET /account/ returns an empty array when no accounts exist")
	void getAllAccountsReturnsEmptyArray() throws Exception {
		when(this.accountService.getAllAccount()).thenReturn(Collections.emptyList());

		this.mockMvc.perform(get("/account/"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("POST /account/ creates an account")
	void createAccountReturnsCreatedAccount() throws Exception {
		when(this.accountService.upsertAccount(any(Account.class))).thenReturn(account(5, "New Desk"));

		this.mockMvc.perform(post("/account/")
						.contentType(MediaType.APPLICATION_JSON)
						.content(this.objectMapper.writeValueAsString(account(0, "New Desk"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5));

		verify(this.accountService).upsertAccount(any(Account.class));
	}

	@Test
	@DisplayName("POST /account/ with a malformed body never reaches the service")
	void createAccountRejectsMalformedBody() throws Exception {
		this.mockMvc.perform(post("/account/")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":"))
				.andExpect(status().isInternalServerError());

		verify(this.accountService, never()).upsertAccount(any(Account.class));
	}

	@Test
	@DisplayName("PUT /account/ updates an account")
	void updateAccountReturnsUpdatedAccount() throws Exception {
		when(this.accountService.upsertAccount(any(Account.class))).thenReturn(account(1, "Renamed Desk"));

		this.mockMvc.perform(put("/account/")
						.contentType(MediaType.APPLICATION_JSON)
						.content(this.objectMapper.writeValueAsString(account(1, "Renamed Desk"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Renamed Desk"));
	}

	@Test
	@DisplayName("Unexpected service failures are mapped to 500")
	void unexpectedFailureIsMappedToInternalServerError() throws Exception {
		when(this.accountService.getAllAccount()).thenThrow(new IllegalStateException("database unavailable"));

		this.mockMvc.perform(get("/account/"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string("database unavailable"));
	}
}
