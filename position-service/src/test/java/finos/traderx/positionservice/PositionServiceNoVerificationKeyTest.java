package finos.traderx.positionservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:position-no-key;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"traderx.security.jwt.jwk-set-uri=",
		"traderx.security.jwt.hmac-secret="
})
class PositionServiceNoVerificationKeyTest {

	@Autowired
	MockMvc mvc;

	@Test
	void everyTokenIsRejectedWhenNoKeyIsConfigured() throws Exception {
		String token = PositionServiceSecurityTest.token(PositionServiceSecurityTest.SECRET,
				Map.of("preferred_username", "user01", "roles", List.of("traderx-admin")));

		mvc.perform(PositionServiceSecurityTest.withToken(get("/positions/"), token))
				.andExpect(status().isUnauthorized());
		mvc.perform(PositionServiceSecurityTest.withToken(get("/trades/22214"), token))
				.andExpect(status().isUnauthorized());
	}
}
