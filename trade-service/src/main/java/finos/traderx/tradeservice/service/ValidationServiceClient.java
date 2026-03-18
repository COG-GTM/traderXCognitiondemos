package finos.traderx.tradeservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ValidationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ValidationServiceClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${validation.service.url}")
    private String validationServiceUrl;

    public ValidationResult validate(String security, Integer accountId) {
        String url = this.validationServiceUrl + "/validate/trade";
        ValidationRequest request = new ValidationRequest(security, accountId);

        ResponseEntity<ValidationResult> response = this.restTemplate.postForEntity(url, request, ValidationResult.class);
        log.info("Validation result: valid={}, errorMessage={}", response.getBody().isValid(), response.getBody().getErrorMessage());
        return response.getBody();
    }

    public static class ValidationRequest {
        private String security;
        private Integer accountId;

        public ValidationRequest() {
        }

        public ValidationRequest(String security, Integer accountId) {
            this.security = security;
            this.accountId = accountId;
        }

        public String getSecurity() {
            return security;
        }

        public void setSecurity(String security) {
            this.security = security;
        }

        public Integer getAccountId() {
            return accountId;
        }

        public void setAccountId(Integer accountId) {
            this.accountId = accountId;
        }
    }

    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;

        public ValidationResult() {
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
