package finos.traderx.tradeservice.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.tradeservice.model.Security;

/**
 * Access to the reference-data service. Extracted from TradeOrderController so
 * it can be mocked and so trade-service can be tested without reference-data up.
 */
@Service
public class ReferenceDataService {

	private static final Logger log = LoggerFactory.getLogger(ReferenceDataService.class);

	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${reference.data.service.url}")
	private String referenceDataServiceAddress;

	public Optional<Security> findSecurity(String ticker) {
		String url = this.referenceDataServiceAddress + "//stocks/" + ticker;
		try {
			ResponseEntity<Security> response = this.restTemplate.getForEntity(url, Security.class);
			log.info("Validate ticker {}", response.getBody());
			return Optional.ofNullable(response.getBody());
		} catch (HttpClientErrorException ex) {
			if (ex.getStatusCode().value() == 404) {
				log.info("{} not found in reference data service.", ticker);
			} else {
				log.error(ex.getMessage());
			}
			return Optional.empty();
		}
	}
}
