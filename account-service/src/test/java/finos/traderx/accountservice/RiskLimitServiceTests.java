package finos.traderx.accountservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.MissingLimitPolicy;
import finos.traderx.accountservice.model.RiskLimitChangeType;
import finos.traderx.accountservice.model.RiskLimitHistory;
import finos.traderx.accountservice.model.RiskLimitRequest;
import finos.traderx.accountservice.model.RiskLimitView;
import finos.traderx.accountservice.service.AccountService;
import finos.traderx.accountservice.service.RiskLimitService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "/test-application.properties")
class RiskLimitServiceTests {

    @Autowired
    AccountService accountService;

    @Autowired
    RiskLimitService riskLimitService;

    private int newAccount(String displayName) {
        Account account = new Account();
        account.setDisplayName(displayName);
        return accountService.upsertAccount(account).getId();
    }

    private RiskLimitRequest request(String notional, String setBy, String reason) {
        RiskLimitRequest request = new RiskLimitRequest();
        request.setMaxOrderNotional(new BigDecimal(notional));
        request.setCurrency("usd");
        request.setSetBy(setBy);
        request.setReason(reason);
        return request;
    }

    @Test
    void putThenGetReturnsTheLimitInForce() {
        int accountId = newAccount("limit account");

        riskLimitService.setRiskLimit(accountId, request("50000.00", "risk.control@traderx", "Initial limit"));

        RiskLimitView view = riskLimitService.getRiskLimit(accountId);
        assertTrue(view.isLimitPresent());
        assertEquals(0, new BigDecimal("50000.00").compareTo(view.getMaxOrderNotional()));
        assertEquals("USD", view.getCurrency());
        assertEquals("risk.control@traderx", view.getSetBy());
    }

    @Test
    void amendingALimitLeavesATrailOfThePreviousValue() {
        int accountId = newAccount("amend account");

        riskLimitService.setRiskLimit(accountId, request("50000.00", "risk.control@traderx", "Initial limit"));
        riskLimitService.setRiskLimit(accountId, request("75000.00", "head.of.risk@traderx", "Client uplift approved"));

        assertEquals(0, new BigDecimal("75000.00").compareTo(riskLimitService.getRiskLimit(accountId).getMaxOrderNotional()));

        List<RiskLimitHistory> history = riskLimitService.getRiskLimitHistory(accountId);
        assertEquals(2, history.size());

        RiskLimitHistory latest = history.get(0);
        assertEquals(RiskLimitChangeType.AMEND, latest.getChangeType());
        assertEquals("head.of.risk@traderx", latest.getChangedBy());
        assertEquals("Client uplift approved", latest.getReason());
        assertEquals(0, new BigDecimal("75000.00").compareTo(latest.getMaxOrderNotional()));

        RiskLimitHistory superseded = history.get(1);
        assertEquals(RiskLimitChangeType.CREATE, superseded.getChangeType());
        assertEquals("risk.control@traderx", superseded.getChangedBy());
        assertEquals(0, new BigDecimal("50000.00").compareTo(superseded.getMaxOrderNotional()));
    }

    @Test
    void accountWithNoLimitReportsTheConfiguredMissingLimitPolicy() {
        int accountId = newAccount("unlimited account");

        RiskLimitView view = riskLimitService.getRiskLimit(accountId);
        assertFalse(view.isLimitPresent());
        assertEquals(MissingLimitPolicy.UNLIMITED, view.getMissingLimitPolicy());
        assertNull(view.getMaxOrderNotional());
        assertTrue(riskLimitService.getRiskLimitHistory(accountId).isEmpty());
    }

    @Test
    void unknownAccountIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> riskLimitService.getRiskLimit(-1));
        assertThrows(ResourceNotFoundException.class,
                () -> riskLimitService.setRiskLimit(-1, request("1000.00", "risk.control@traderx", null)));
    }

    @Test
    void invalidPayloadIsRejected() {
        int accountId = newAccount("validation account");

        RiskLimitRequest negative = request("-1.00", "risk.control@traderx", null);
        assertThrows(IllegalArgumentException.class, () -> riskLimitService.setRiskLimit(accountId, negative));

        RiskLimitRequest noOwner = request("1000.00", "  ", null);
        assertThrows(IllegalArgumentException.class, () -> riskLimitService.setRiskLimit(accountId, noOwner));

        RiskLimitRequest badCurrency = request("1000.00", "risk.control@traderx", null);
        badCurrency.setCurrency("DOLLARS");
        assertThrows(IllegalArgumentException.class, () -> riskLimitService.setRiskLimit(accountId, badCurrency));

        RiskLimitRequest tooPrecise = request("100.999", "risk.control@traderx", null);
        assertThrows(IllegalArgumentException.class, () -> riskLimitService.setRiskLimit(accountId, tooPrecise));

        RiskLimitRequest longOwner = request("1000.00", "x".repeat(51), null);
        assertThrows(IllegalArgumentException.class, () -> riskLimitService.setRiskLimit(accountId, longOwner));

        RiskLimitRequest notACurrency = request("1000.00", "risk.control@traderx", null);
        notACurrency.setCurrency("XQZ");
        assertThrows(IllegalArgumentException.class, () -> riskLimitService.setRiskLimit(accountId, notACurrency));

        RiskLimitRequest futureDated = request("1000.00", "risk.control@traderx", null);
        futureDated.setEffectiveFrom(new Date(System.currentTimeMillis() + 86400000L));
        assertThrows(IllegalArgumentException.class, () -> riskLimitService.setRiskLimit(accountId, futureDated));

        assertFalse(riskLimitService.getRiskLimit(accountId).isLimitPresent());
    }
}
