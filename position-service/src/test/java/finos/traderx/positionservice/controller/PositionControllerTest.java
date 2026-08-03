package finos.traderx.positionservice.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;

import finos.traderx.positionservice.model.Position;
import finos.traderx.positionservice.service.PositionService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PositionController.class)
class PositionControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockBean
	PositionService positionService;

	private Position position(Integer accountId, String security, Integer quantity) {
		Position position = new Position();
		position.setAccountId(accountId);
		position.setSecurity(security);
		position.setQuantity(quantity);
		return position;
	}

	@Test
	@DisplayName("GET /positions/{accountId} returns the positions of the account")
	void getByAccountIdReturnsPositions() throws Exception {
		when(this.positionService.getPositionsByAccountID(1))
				.thenReturn(Arrays.asList(position(1, "MSFT", 100), position(1, "AAPL", 25)));

		this.mockMvc.perform(get("/positions/{accountId}", 1))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].security").value("MSFT"))
				.andExpect(jsonPath("$[0].quantity").value(100));
	}

	@Test
	@DisplayName("GET /positions/{accountId} returns an empty array for an account without positions")
	void getByAccountIdReturnsEmptyArray() throws Exception {
		when(this.positionService.getPositionsByAccountID(404)).thenReturn(Collections.emptyList());

		this.mockMvc.perform(get("/positions/{accountId}", 404))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("GET /positions/{accountId} serialises short positions as negative quantities")
	void getByAccountIdReturnsNegativeQuantities() throws Exception {
		when(this.positionService.getPositionsByAccountID(1))
				.thenReturn(Collections.singletonList(position(1, "MSFT", -30)));

		this.mockMvc.perform(get("/positions/{accountId}", 1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].quantity").value(-30));
	}

	@Test
	@DisplayName("GET /positions/{accountId} with a non-numeric account id never reaches the service")
	void getByAccountIdRejectsNonNumericAccountId() throws Exception {
		this.mockMvc.perform(get("/positions/{accountId}", "not-a-number"))
				.andExpect(status().isInternalServerError());

		verify(this.positionService, never()).getPositionsByAccountID(anyInt());
	}

	@Test
	@DisplayName("GET /positions/ returns all positions")
	void getAllPositionsReturnsPositions() throws Exception {
		when(this.positionService.getAllPositions())
				.thenReturn(Arrays.asList(position(1, "MSFT", 100), position(2, "AAPL", 50)));

		this.mockMvc.perform(get("/positions/"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[1].accountId").value(2));
	}

	@Test
	@DisplayName("GET /positions/ returns an empty array when no positions exist")
	void getAllPositionsReturnsEmptyArray() throws Exception {
		when(this.positionService.getAllPositions()).thenReturn(Collections.emptyList());

		this.mockMvc.perform(get("/positions/"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Unexpected service failures are mapped to 500")
	void unexpectedFailureIsMappedToInternalServerError() throws Exception {
		when(this.positionService.getAllPositions()).thenThrow(new IllegalStateException("database unavailable"));

		this.mockMvc.perform(get("/positions/"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().string("database unavailable"));
	}
}
