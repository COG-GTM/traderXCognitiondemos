package finos.traderx.positionservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import finos.traderx.positionservice.model.Position;
import finos.traderx.positionservice.repository.PositionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "/test-application.properties")
class PositionControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	PositionRepository positionRepository;

	@BeforeEach
	void seedPositions() {
		positionRepository.deleteAll();
		positionRepository.save(position(22214, "MSFT", 100));
		positionRepository.save(position(22214, "IBM", -50));
		positionRepository.save(position(52355, "BAC", 10));
	}

	@Test
	void getPositionsByAccount() throws Exception {
		mockMvc.perform(get("/positions/{accountId}", 22214))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[?(@.security == 'MSFT')].quantity").value(100))
			.andExpect(jsonPath("$[?(@.security == 'IBM')].quantity").value(-50));
	}

	@Test
	void getAllPositions() throws Exception {
		mockMvc.perform(get("/positions/"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(3));
	}

	@Test
	void unknownAccountReturnsEmptyList() throws Exception {
		mockMvc.perform(get("/positions/{accountId}", 999999))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void tradesByAccount() throws Exception {
		mockMvc.perform(get("/trades/{accountId}", 22214))
			.andExpect(status().isOk());
	}

	@Test
	void rootRedirectsToSwaggerUi() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("swagger-ui.html"));
	}

	@Test
	void traversalSegmentsAreNotRoutedToController() throws Exception {
		mockMvc.perform(get("/positions/%2e%2e/positions/{accountId}", 22214))
			.andExpect(status().isNotFound());
	}

	private static Position position(int accountId, String security, int quantity) {
		Position p = new Position();
		p.setAccountId(accountId);
		p.setSecurity(security);
		p.setQuantity(quantity);
		p.setUpdated(new Date());
		return p;
	}
}
