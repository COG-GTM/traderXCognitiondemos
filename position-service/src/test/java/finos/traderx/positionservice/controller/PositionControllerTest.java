package finos.traderx.positionservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import finos.traderx.positionservice.model.Position;
import finos.traderx.positionservice.service.PositionService;

/** Edge and corner case coverage for the read-only /positions endpoints. */
@WebMvcTest(PositionController.class)
class PositionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private PositionService positionService;

	private static Position position(int accountId, String security, int quantity) {
		Position position = new Position();
		position.setAccountId(accountId);
		position.setSecurity(security);
		position.setQuantity(quantity);
		position.setUpdated(new Date(0));
		return position;
	}

	@Test
	@DisplayName("PS-25a: GET /positions/{accountId} for an unknown account returns 200 with an empty array")
	void unknownAccountReturnsEmptyArray() throws Exception {
		when(this.positionService.getPositionsByAccountID(987654)).thenReturn(Collections.emptyList());

		MvcResult result = this.mockMvc.perform(get("/positions/987654")).andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertEquals("[]", result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("PS-25b: GET /positions/-1 returns 200 with an empty array")
	void negativeAccountIdReturnsEmptyArray() throws Exception {
		when(this.positionService.getPositionsByAccountID(-1)).thenReturn(Collections.emptyList());

		MvcResult result = this.mockMvc.perform(get("/positions/-1")).andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertEquals("[]", result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("PS-25c: GET /positions/0 returns 200 with an empty array")
	void zeroAccountIdReturnsEmptyArray() throws Exception {
		when(this.positionService.getPositionsByAccountID(0)).thenReturn(Collections.emptyList());

		assertEquals(HttpStatus.OK.value(),
				this.mockMvc.perform(get("/positions/0")).andReturn().getResponse().getStatus());
	}

	@Test
	@DisplayName("PS-25d: GET /positions/{accountId} with a non-numeric id returns 500 (not 400)")
	void nonNumericAccountIdReturns500() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/positions/abc")).andReturn();

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
		assertTrue(result.getResponse().getContentAsString().contains("abc"),
				"body: " + result.getResponse().getContentAsString());
	}

	@Test
	@Disabled("LATENT BUG: PositionController's @ExceptionHandler(Exception.class) catches "
			+ "MethodArgumentTypeMismatchException, so a malformed accountId is a 500 instead of a 400")
	@DisplayName("PS-25e: GET /positions/{accountId} with a non-numeric id should return 400")
	void nonNumericAccountIdShouldReturn400() throws Exception {
		assertEquals(HttpStatus.BAD_REQUEST.value(),
				this.mockMvc.perform(get("/positions/abc")).andReturn().getResponse().getStatus());
	}

	@Test
	@DisplayName("PS-26a: GET /positions/ on an empty repository returns an empty array, not null")
	void emptyRepositoryReturnsEmptyArray() throws Exception {
		when(this.positionService.getAllPositions()).thenReturn(Collections.emptyList());

		MvcResult result = this.mockMvc.perform(get("/positions/")).andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertEquals("[]", result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("PS-26b: GET /positions (no trailing slash) does not match the '/' mapping")
	void listWithoutTrailingSlashIsNotMapped() throws Exception {
		assertEquals(HttpStatus.NOT_FOUND.value(),
				this.mockMvc.perform(get("/positions")).andReturn().getResponse().getStatus());
	}

	@Test
	@DisplayName("PS-27a: a repository failure returns a 500 whose body is the raw exception message")
	void repositoryFailureLeaksMessage() throws Exception {
		when(this.positionService.getPositionsByAccountID(anyInt()))
				.thenThrow(new RuntimeException("could not prepare statement [SELECT * FROM POSITIONS]"));

		MvcResult result = this.mockMvc.perform(get("/positions/1")).andReturn();

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getResponse().getStatus());
		assertEquals("could not prepare statement [SELECT * FROM POSITIONS]",
				result.getResponse().getContentAsString());
	}

	@Test
	@Disabled("LATENT BUG: PositionController.generalError returns e.getMessage() verbatim, leaking SQL "
			+ "and infrastructure detail to the caller (information disclosure)")
	@DisplayName("PS-27b: a repository failure should not echo the internal exception message")
	void repositoryFailureShouldNotLeakMessage() throws Exception {
		when(this.positionService.getPositionsByAccountID(anyInt()))
				.thenThrow(new RuntimeException("could not prepare statement [SELECT * FROM POSITIONS]"));

		MvcResult result = this.mockMvc.perform(get("/positions/1")).andReturn();

		assertTrue(!result.getResponse().getContentAsString().contains("SELECT"),
				"internal detail leaked: " + result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("PS-28a: a zero-quantity position is still returned")
	void zeroQuantityPositionIsReturned() throws Exception {
		when(this.positionService.getPositionsByAccountID(1)).thenReturn(List.of(position(1, "AAPL", 0)));

		MvcResult result = this.mockMvc.perform(get("/positions/1")).andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertTrue(result.getResponse().getContentAsString().contains("\"quantity\":0"),
				"body: " + result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("PS-28b: a negative (short) position is returned with its sign intact")
	void shortPositionIsReturned() throws Exception {
		when(this.positionService.getPositionsByAccountID(1)).thenReturn(List.of(position(1, "AAPL", -250)));

		MvcResult result = this.mockMvc.perform(get("/positions/1")).andReturn();

		assertTrue(result.getResponse().getContentAsString().contains("\"quantity\":-250"),
				"body: " + result.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("PS-28c: a null-quantity position serialises as null rather than failing")
	void nullQuantityPositionIsReturned() throws Exception {
		Position position = position(1, "AAPL", 0);
		position.setQuantity(null);
		when(this.positionService.getPositionsByAccountID(1)).thenReturn(List.of(position));

		MvcResult result = this.mockMvc.perform(get("/positions/1")).andReturn();

		assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
		assertTrue(result.getResponse().getContentAsString().contains("\"quantity\":null"),
				"body: " + result.getResponse().getContentAsString());
	}
}
