package finos.traderx.validationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.validationservice.model.Person;

@Service
public class PersonValidationService {

	private static final Logger log = LoggerFactory.getLogger(PersonValidationService.class);

	private RestTemplate restTemplate = new RestTemplate();

	@Value("${people.service.url}")
	private String peopleServiceAddress;

	public boolean validatePerson(String username) {
		String url = this.peopleServiceAddress + "/People/GetPerson" + "?LogonId=" + username;
		ResponseEntity<Person> response = null;

		try {
			response = this.restTemplate.getForEntity(url, Person.class);
			log.info("Validated person " + response.getBody().toString());
			return true;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getRawStatusCode() == 404) {
				log.info(username + " not found in People service.");
			}
			else {
				log.error(ex.getMessage());
			}
			return false;
		}
	}
}
