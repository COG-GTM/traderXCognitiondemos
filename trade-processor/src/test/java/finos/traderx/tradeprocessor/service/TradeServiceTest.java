package finos.traderx.tradeprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.model.ComplianceStatus;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    TradeRepository tradeRepository;

    @Mock
    PositionRepository positionRepository;

    @Mock
    Publisher<Trade> tradePublisher;

    @Mock
    Publisher<Position> positionPublisher;

    @Captor
    ArgumentCaptor<Trade> tradeCaptor;

    @InjectMocks
    TradeService tradeService;

    @Test
    void processTradePropagatesComplianceStatus() {
        TradeOrder order = new TradeOrder("TRADE-1", 22214, "AAPL", TradeSide.Buy, 100, ComplianceStatus.APPROVED);

        tradeService.processTrade(order);

        verify(tradeRepository, org.mockito.Mockito.atLeastOnce()).save(tradeCaptor.capture());
        assertEquals(ComplianceStatus.APPROVED, tradeCaptor.getValue().getComplianceStatus());
    }

    @Test
    void processTradeDefaultsComplianceStatusWhenMissing() {
        TradeOrder order = new TradeOrder("TRADE-2", 22214, "AAPL", TradeSide.Sell, 50);
        order.setComplianceStatus(null);

        tradeService.processTrade(order);

        verify(tradeRepository, org.mockito.Mockito.atLeastOnce()).save(tradeCaptor.capture());
        assertEquals(ComplianceStatus.PENDING_REVIEW, tradeCaptor.getValue().getComplianceStatus());
    }
}
