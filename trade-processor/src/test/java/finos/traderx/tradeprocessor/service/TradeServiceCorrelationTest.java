package finos.traderx.tradeprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

/**
 * The audit record written at decision time is only useful if it can be tied to what was
 * actually booked, so the correlation id has to survive the trip over the trade feed.
 */
class TradeServiceCorrelationTest {

    private TradeRepository tradeRepository;
    private PositionRepository positionRepository;
    private TradeService tradeService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        tradeRepository = mock(TradeRepository.class);
        positionRepository = mock(PositionRepository.class);
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(positionRepository.save(any(Position.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(positionRepository.findByAccountIdAndSecurity(anyInt(), anyString())).thenReturn(null);

        tradeService = new TradeService();
        ReflectionTestUtils.setField(tradeService, "tradeRepository", tradeRepository);
        ReflectionTestUtils.setField(tradeService, "positionRepository", positionRepository);
        ReflectionTestUtils.setField(tradeService, "tradePublisher", mock(Publisher.class));
        ReflectionTestUtils.setField(tradeService, "positionPublisher", mock(Publisher.class));
    }

    @Test
    void carriesTheDecisionCorrelationIdOntoTheBookedTrade() {
        TradeOrder order = new TradeOrder("ORDER-1", 22214, "IBM", TradeSide.Buy, 100);
        order.setCorrelationId("CORR-1");

        Trade booked = tradeService.processTrade(order).getTrade();

        assertEquals("CORR-1", booked.getCorrelationId());
        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertEquals("CORR-1", captor.getValue().getCorrelationId());
    }

    @Test
    void leavesTheCorrelationIdUnsetForLegacyOrdersWithoutOne() {
        TradeOrder order = new TradeOrder("ORDER-2", 22214, "MS", TradeSide.Sell, 50);

        var result = tradeService.processTrade(order);

        assertEquals(null, result.getTrade().getCorrelationId());
        assertEquals(-50, result.getPosition().getQuantity());
    }
}
