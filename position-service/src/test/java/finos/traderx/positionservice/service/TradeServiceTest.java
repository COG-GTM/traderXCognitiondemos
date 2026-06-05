package finos.traderx.positionservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import finos.traderx.positionservice.model.ComplianceStatus;
import finos.traderx.positionservice.model.Trade;
import finos.traderx.positionservice.repository.TradeRepository;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    TradeRepository tradeRepository;

    @InjectMocks
    TradeService tradeService;

    private Trade tradeWithCompliance(String id, String compliance) {
        Trade t = new Trade();
        t.setId(id);
        t.setAccountId(22214);
        t.setComplianceStatus(compliance);
        return t;
    }

    @Test
    void onlyApprovedTradesAreCompliantForProcessing() {
        assertTrue(tradeService.isCompliantForProcessing(
                tradeWithCompliance("T1", ComplianceStatus.APPROVED.name())));
        assertFalse(tradeService.isCompliantForProcessing(
                tradeWithCompliance("T2", ComplianceStatus.PENDING_REVIEW.name())));
        assertFalse(tradeService.isCompliantForProcessing(
                tradeWithCompliance("T3", ComplianceStatus.FLAGGED.name())));
        assertFalse(tradeService.isCompliantForProcessing(
                tradeWithCompliance("T4", ComplianceStatus.REJECTED.name())));
        assertFalse(tradeService.isCompliantForProcessing(null));
    }

    @Test
    void getCompliantTradesFiltersNonApproved() {
        when(tradeRepository.findByAccountId(22214)).thenReturn(Arrays.asList(
                tradeWithCompliance("T1", ComplianceStatus.APPROVED.name()),
                tradeWithCompliance("T2", ComplianceStatus.REJECTED.name()),
                tradeWithCompliance("T3", ComplianceStatus.APPROVED.name())));

        assertEquals(2, tradeService.getCompliantTradesByAccountID(22214).size());
    }
}
