package finos.traderx.tradeservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.tradeservice.model.Account;

/**
 * Access to the account-service. Extracted from TradeOrderController so it can
 * be mocked and so trade-service can be tested without account-service up.
 */
@Service
public class AccountValidationService {

	private static final Logger log = LoggerFactory.getLogger(AccountValidationService.class);

	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${account.service.url}")
	private String accountServiceAddress;

	public boolean accountExists(Integer id) {
		String url = this.accountServiceAddress + "//account/" + id;
		try {
			ResponseEntity<Account> response = this.restTemplate.getForEntity(url, Account.class);
			log.info("Validate account {}", response.getBody());
			return true;
		} catch (HttpClientErrorException ex) {
			if (ex.getStatusCode().value() == 404) {
				log.info("Account {} not found in account service.", id);
			} else {
				log.error(ex.getMessage());
			}
			return false;
		}
	}
}
