package finos.traderx.positionservice.techfest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;

/** Legacy (v1) contract: GET /positions/{accountId} is one JSON array; shape unchanged except the added fields. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "/techfest-test.properties")
class PositionsV1ContractTest {

	@Autowired
	MockMvc mvc;

	@Test
	void v1MatchesApprovedFixtureRowForRow() throws Exception {
		JsonNode expected = Fixtures.load("v1-positions-77007.json");
		String body = mvc.perform(get("/positions/{accountId}", Fixtures.ACCOUNT))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode actual = Fixtures.MAPPER.readTree(body);

		assertTrue(actual.isArray(), "v1 stays a single JSON array");
		assertEquals(expected.size(), actual.size(), "same number of positions");

		Iterator<JsonNode> it = actual.elements();
		int index = 0;
		while (it.hasNext()) {
			JsonNode row = it.next();
			JsonNode exp = findBySecurity(expected, row.get("security").asText());
			assertEquals(exp.get("accountId").asInt(), row.get("accountId").asInt());
			assertEquals(exp.get("quantity").asInt(), row.get("quantity").asInt(), "quantity " + row.get("security"));
			assertEquals(exp.get("updated").asText(), row.get("updated").asText(), "updated (legacy timestamp) " + row.get("security"));
			assertEquals(exp.get("currency").asText(), row.get("currency").asText(), "currency " + row.get("security"));
			assertTrue(row.has("marketValue"), "marketValue key present even when unknown");
			if (exp.get("marketValue").isNull()) {
				assertTrue(row.get("marketValue").isNull(), "unknown market value stays null: " + row.get("security"));
			} else {
				assertEquals(0, new BigDecimal(exp.get("marketValue").asText())
						.compareTo(new BigDecimal(row.get("marketValue").asText())), "marketValue " + row.get("security"));
			}
			index++;
		}
		assertEquals(expected.size(), index);
	}

	@Test
	void v1StillServesTheOriginalSeedAccounts() throws Exception {
		String body = mvc.perform(get("/positions/{accountId}", 22214))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode actual = Fixtures.MAPPER.readTree(body);
		assertEquals(3, actual.size());
		for (JsonNode row : actual) {
			assertTrue(row.has("security") && row.has("quantity") && row.has("updated") && row.has("accountId"));
		}
	}

	private static JsonNode findBySecurity(JsonNode array, String security) {
		for (JsonNode n : array) {
			if (n.get("security").asText().equals(security)) {
				return n;
			}
		}
		throw new AssertionError("unexpected security in v1 response: " + security);
	}
}
