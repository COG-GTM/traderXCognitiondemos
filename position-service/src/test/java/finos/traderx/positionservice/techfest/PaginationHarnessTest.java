package finos.traderx.positionservice.techfest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;

/** Walks every cursor at several page sizes: each position exactly once, stable order, terminal null cursor. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "/techfest-test.properties")
class PaginationHarnessTest {

	@Autowired
	MockMvc mvc;

	@ParameterizedTest(name = "limit={0}")
	@ValueSource(ints = { 1, 7, 10, 25, 26, 100 })
	void walkingAllCursorsVisitsEveryPositionExactlyOnce(int limit) throws Exception {
		JsonNode v1 = Fixtures.load("v1-positions-77007.json");
		Set<String> expected = new LinkedHashSet<>();
		for (JsonNode n : v1) {
			expected.add(n.get("security").asText());
		}

		List<String> seen = new ArrayList<>();
		String cursor = null;
		int pages = 0;
		do {
			MockHttpServletRequestBuilder b = get("/v2/positions")
					.param("accountId", String.valueOf(Fixtures.ACCOUNT)).param("limit", String.valueOf(limit));
			if (cursor != null) {
				b.param("cursor", cursor);
			}
			JsonNode page = Fixtures.MAPPER.readTree(
					mvc.perform(b).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
			assertTrue(page.get("items").size() <= limit, "page never exceeds limit");
			for (JsonNode item : page.get("items")) {
				seen.add(item.get("security").asText());
			}
			cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asText();
			pages++;
			assertTrue(pages <= expected.size() + 1, "cursor walk must terminate");
		} while (cursor != null);

		assertEquals(expected.size(), seen.size(), "no duplicates and no drops at limit=" + limit);
		assertEquals(new LinkedHashSet<>(seen).size(), seen.size(), "no duplicates");
		assertEquals(new ArrayList<>(new java.util.TreeSet<>(expected)), seen, "stable ascending SECURITY order");
		assertEquals((int) Math.ceil(expected.size() / (double) limit), pages, "page count for limit=" + limit);
	}
}
