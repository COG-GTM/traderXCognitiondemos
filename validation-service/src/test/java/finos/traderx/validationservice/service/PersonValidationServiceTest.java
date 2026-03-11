package finos.traderx.validationservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class PersonValidationServiceTest {

	private PersonValidationService personValidationService;
	private MockRestServiceServer mockServer;
	private RestTemplate restTemplate;

	@BeforeEach
	void setUp() {
		personValidationService = new PersonValidationService();
		restTemplate = new RestTemplate();
		mockServer = MockRestServiceServer.createServer(restTemplate);
		ReflectionTestUtils.setField(personValidationService, "restTemplate", restTemplate);
		ReflectionTestUtils.setField(personValidationService, "peopleServiceAddress", "http://localhost:18089");
	}

	@Test
	void validatePerson_validPerson_returnsTrue() {
		mockServer.expect(requestTo("http://localhost:18089/People/GetPerson?LogonId=johndoe"))
				.andRespond(withSuccess("{\"logonId\":\"johndoe\",\"fullName\":\"John Doe\"}", MediaType.APPLICATION_JSON));

		boolean result = personValidationService.validatePerson("johndoe");

		assertTrue(result);
		mockServer.verify();
	}

	@Test
	void validatePerson_invalidPerson_returnsFalse() {
		mockServer.expect(requestTo("http://localhost:18089/People/GetPerson?LogonId=unknown"))
				.andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

		boolean result = personValidationService.validatePerson("unknown");

		assertFalse(result);
		mockServer.verify();
	}
}
