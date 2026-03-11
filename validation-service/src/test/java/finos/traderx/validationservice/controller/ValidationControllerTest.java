package finos.traderx.validationservice.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import finos.traderx.validationservice.service.AccountValidationService;
import finos.traderx.validationservice.service.PersonValidationService;
import finos.traderx.validationservice.service.TickerValidationService;

@WebMvcTest(ValidationController.class)
class ValidationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private TickerValidationService tickerValidationService;

	@MockBean
	private AccountValidationService accountValidationService;

	@MockBean
	private PersonValidationService personValidationService;

	@Test
	void validateTicker_valid_returnsValid() throws Exception {
		when(tickerValidationService.validateTicker("AAPL")).thenReturn(true);

		mockMvc.perform(get("/validate/ticker/AAPL"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(true))
				.andExpect(jsonPath("$.errors").isEmpty());
	}

	@Test
	void validateTicker_invalid_returnsInvalid() throws Exception {
		when(tickerValidationService.validateTicker("INVALID")).thenReturn(false);

		mockMvc.perform(get("/validate/ticker/INVALID"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(false))
				.andExpect(jsonPath("$.errors[0]").value("INVALID not found in Reference data service."));
	}

	@Test
	void validateAccount_valid_returnsValid() throws Exception {
		when(accountValidationService.validateAccount(12345)).thenReturn(true);

		mockMvc.perform(get("/validate/account/12345"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(true))
				.andExpect(jsonPath("$.errors").isEmpty());
	}

	@Test
	void validateAccount_invalid_returnsInvalid() throws Exception {
		when(accountValidationService.validateAccount(99999)).thenReturn(false);

		mockMvc.perform(get("/validate/account/99999"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(false))
				.andExpect(jsonPath("$.errors[0]").value("99999 not found in Account service."));
	}

	@Test
	void validatePerson_valid_returnsValid() throws Exception {
		when(personValidationService.validatePerson("johndoe")).thenReturn(true);

		mockMvc.perform(get("/validate/person").param("username", "johndoe"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(true))
				.andExpect(jsonPath("$.errors").isEmpty());
	}

	@Test
	void validatePerson_invalid_returnsInvalid() throws Exception {
		when(personValidationService.validatePerson("unknown")).thenReturn(false);

		mockMvc.perform(get("/validate/person").param("username", "unknown"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(false))
				.andExpect(jsonPath("$.errors[0]").value("unknown not found in People service."));
	}

	@Test
	void validateTradeOrder_allValid_returnsValid() throws Exception {
		when(tickerValidationService.validateTicker("AAPL")).thenReturn(true);
		when(accountValidationService.validateAccount(12345)).thenReturn(true);

		mockMvc.perform(post("/validate/trade-order")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"security\":\"AAPL\",\"accountId\":12345}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(true))
				.andExpect(jsonPath("$.errors").isEmpty());
	}

	@Test
	void validateTradeOrder_invalidTicker_returnsErrors() throws Exception {
		when(tickerValidationService.validateTicker("INVALID")).thenReturn(false);
		when(accountValidationService.validateAccount(12345)).thenReturn(true);

		mockMvc.perform(post("/validate/trade-order")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"security\":\"INVALID\",\"accountId\":12345}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(false))
				.andExpect(jsonPath("$.errors[0]").value("INVALID not found in Reference data service."));
	}

	@Test
	void validateTradeOrder_bothInvalid_returnsAllErrors() throws Exception {
		when(tickerValidationService.validateTicker("INVALID")).thenReturn(false);
		when(accountValidationService.validateAccount(99999)).thenReturn(false);

		mockMvc.perform(post("/validate/trade-order")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"security\":\"INVALID\",\"accountId\":99999}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(false))
				.andExpect(jsonPath("$.errors.length()").value(2));
	}
}
