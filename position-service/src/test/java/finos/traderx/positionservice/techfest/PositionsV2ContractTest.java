package finos.traderx.positionservice.techfest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;

/** Approved (v2) contract: GET /v2/positions?accountId=&cursor=&limit= must equal the approved page fixtures byte-for-byte as JSON. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "/techfest-test.properties")
class PositionsV2ContractTest {

	@Autowired
	MockMvc mvc;

	@Test
	void everyApprovedPageIsReturnedExactly() throws Exception {
		JsonNode pages = Fixtures.load("v2-pages-77007.json");
		assertTrue(pages.size() >= 3, "fixture yields at least three pages");
		for (JsonNode page : pages) {
			JsonNode req = page.get("request");
			MockHttpServletRequestBuilder b = get("/v2/positions")
					.param("accountId", req.get("accountId").asText())
					.param("limit", req.get("limit").asText());
			if (!req.get("cursor").isNull()) {
				b.param("cursor", req.get("cursor").asText());
			}
			String body = mvc.perform(b).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
			assertEquals(page.get("response"), Fixtures.MAPPER.readTree(body), "page with cursor " + req.get("cursor"));
		}
	}

	@Test
	void unknownMarketValueIsExplicitNullAndNeverOnPageOne() throws Exception {
		JsonNode pages = Fixtures.load("v2-pages-77007.json");
		JsonNode first = pages.get(0).get("response").get("items");
		for (JsonNode item : first) {
			assertFalse(item.get("marketValue").isNull(), "page 1 must not carry the unknown case: " + item.get("security"));
		}
		boolean sawUnknown = false;
		for (int i = 1; i < pages.size(); i++) {
			for (JsonNode item : pages.get(i).get("response").get("items")) {
				assertTrue(item.has("marketValue"), "marketValue key always present");
				assertEquals(3, item.get("currency").asText().length(), "currency present even when value unknown");
				if (item.get("marketValue").isNull()) {
					sawUnknown = true;
				} else {
					assertTrue(item.get("marketValue").get("amount").isTextual(), "amount is a decimal string");
					assertEquals(item.get("currency").asText(), item.get("marketValue").get("currency").asText());
				}
			}
		}
		assertTrue(sawUnknown, "at least one unknown market value on a later page");
	}

	@Test
	void jpyAmountsHaveZeroDecimalsAndOthersTwo() throws Exception {
		String body = mvc.perform(get("/v2/positions").param("accountId", String.valueOf(Fixtures.ACCOUNT)).param("limit", "100"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		for (JsonNode item : Fixtures.MAPPER.readTree(body).get("items")) {
			JsonNode mv = item.get("marketValue");
			if (mv.isNull()) {
				continue;
			}
			String amount = mv.get("amount").asText();
			int decimals = amount.contains(".") ? amount.length() - amount.indexOf('.') - 1 : 0;
			assertEquals("JPY".equals(mv.get("currency").asText()) ? 0 : 2, decimals, item.get("security") + " " + amount);
			assertTrue(item.get("asOf").asText().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"), "asOf is ISO-8601 UTC");
		}
	}

	@Test
	void rejectsBadCursorAndBadLimit() throws Exception {
		mvc.perform(get("/v2/positions").param("accountId", "77007").param("cursor", "!!not-base64!!"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/v2/positions").param("accountId", "77007").param("limit", "0"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/v2/positions").param("accountId", "77007").param("limit", "101"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void emptyAccountReturnsEmptyItemsAndNullCursor() throws Exception {
		String body = mvc.perform(get("/v2/positions").param("accountId", "999999"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		JsonNode page = Fixtures.MAPPER.readTree(body);
		assertEquals(0, page.get("items").size());
		assertTrue(page.has("nextCursor") && page.get("nextCursor").isNull());
	}
}
