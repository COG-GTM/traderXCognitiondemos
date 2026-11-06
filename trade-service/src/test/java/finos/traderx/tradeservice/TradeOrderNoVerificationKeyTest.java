package finos.traderx.tradeservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeservice.model.TradeOrder;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:trade-no-key;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"traderx.security.jwt.jwk-set-uri=",
		"traderx.security.jwt.hmac-secret="
})
class TradeOrderNoVerificationKeyTest {

	@Autowired
	MockMvc mvc;

	@MockBean
	Publisher<TradeOrder> tradePublisher;

	@Test
	void everyTokenIsRejectedWhenNoKeyIsConfigured() throws Exception {
		String token = TradeOrderSecurityTest.token(TradeOrderSecurityTest.SECRET, Map.of("preferred_username", "user01"));

		mvc.perform(post("/trade/")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":22214,\"security\":\"IBM\",\"quantity\":10,\"side\":\"Buy\"}"))
				.andExpect(status().isUnauthorized());

		verify(tradePublisher, never()).publish(any(), any());
	}
}
