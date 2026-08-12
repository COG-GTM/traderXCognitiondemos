package finos.traderx.accountservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import finos.traderx.accountservice.exceptions.RiskLimitsDisabledException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.MissingLimitPolicy;
import finos.traderx.accountservice.model.RiskLimitRequest;
import finos.traderx.accountservice.model.RiskLimitView;
import finos.traderx.accountservice.service.AccountService;
import finos.traderx.accountservice.service.RiskLimitService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * With the feature switched off the service must look exactly as it did before TRX-102:
 * no limit is ever reported, and nothing can be written.
 */
@SpringBootTest(properties = { "traderx.risk-limit.enabled=false", "spring.datasource.url=jdbc:h2:mem:test-flag-off" })
@TestPropertySource(locations = "/test-application.properties")
class RiskLimitDisabledTests {

    @Autowired
    AccountService accountService;

    @Autowired
    RiskLimitService riskLimitService;

    @Test
    void featureFlagOffMeansNoLimitsAndNoWrites() {
        Account account = new Account();
        account.setDisplayName("flag off account");
        int accountId = accountService.upsertAccount(account).getId();

        RiskLimitView view = riskLimitService.getRiskLimit(accountId);
        assertFalse(view.isLimitPresent());
        assertEquals(MissingLimitPolicy.UNLIMITED, view.getMissingLimitPolicy());
        assertTrue(riskLimitService.getRiskLimitHistory(accountId).isEmpty());

        RiskLimitRequest request = new RiskLimitRequest();
        request.setMaxOrderNotional(new BigDecimal("1000.00"));
        request.setCurrency("USD");
        request.setSetBy("risk.control@traderx");
        assertThrows(RiskLimitsDisabledException.class, () -> riskLimitService.setRiskLimit(accountId, request));
    }
}
