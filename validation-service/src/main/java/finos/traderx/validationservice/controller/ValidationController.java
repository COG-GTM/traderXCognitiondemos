package finos.traderx.validationservice.controller;

import finos.traderx.validationservice.model.ValidationRequest;
import finos.traderx.validationservice.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@CrossOrigin("*")
@RestController
@RequestMapping(value = "/validate", produces = "application/json")
public class ValidationController {

    Logger log = LoggerFactory.getLogger(ValidationController.class);

    @Value("${account.service.url}")
    private String accountServiceAddress;

    @Value("${reference.data.service.url}")
    private String referenceDataServiceAddress;

    private RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/")
    public ResponseEntity<ValidationResult> validate(@RequestBody ValidationRequest request) {
        if (!validateTicker(request.getSecurity())) {
            return ResponseEntity.ok(ValidationResult.invalid(request.getSecurity() + " not found in reference data."));
        }
        if (!validateAccount(request.getAccountId())) {
            return ResponseEntity.ok(ValidationResult.invalid(request.getAccountId() + " not found in account service."));
        }
        return ResponseEntity.ok(ValidationResult.valid());
    }

    private boolean validateTicker(String ticker) {
        String url = this.referenceDataServiceAddress + "//stocks/" + ticker;
        try {
            restTemplate.getForEntity(url, Object.class);
            log.info("Ticker {} is valid.", ticker);
            return true;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                log.info(ticker + " not found in reference data service.");
            } else {
                log.error(ex.getMessage());
            }
            return false;
        }
    }

    private boolean validateAccount(Integer id) {
        String url = this.accountServiceAddress + "//account/" + id;
        try {
            restTemplate.getForEntity(url, Object.class);
            log.info("Account {} is valid.", id);
            return true;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                log.info("Account " + id + " not found in account service.");
            } else {
                log.error(ex.getMessage());
            }
            return false;
        }
    }
}
