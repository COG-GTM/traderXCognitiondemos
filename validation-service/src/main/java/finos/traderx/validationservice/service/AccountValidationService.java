package finos.traderx.validationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.validationservice.model.Account;

@Service
public class AccountValidationService {

	private static final Logger log = LoggerFactory.getLogger(AccountValidationService.class);

	private RestTemplate restTemplate = new RestTemplate();

	@Value("${account.service.url}")
	private String accountServiceAddress;

	public boolean validateAccount(Integer id) {
		String url = this.accountServiceAddress + "//account/" + id;
		ResponseEntity<Account> response = null;

		try {
			response = this.restTemplate.getForEntity(url, Account.class);
			log.info("Validate account " + response.getBody().toString());
			return true;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getRawStatusCode() == 404) {
				log.info("Account" + id + " not found in account service.");
			}
			else {
				log.error(ex.getMessage());
			}
			return false;
		}
	}
}
