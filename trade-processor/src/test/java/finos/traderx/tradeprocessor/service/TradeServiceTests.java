package finos.traderx.tradeprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeBookingResult;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.model.TradeState;
import finos.traderx.tradeprocessor.repository.PositionRepository;

@DataJpaTest
@Import(TradeService.class)
class TradeServiceTests {

    @Autowired
    TradeService tradeService;

    @Autowired
    PositionRepository positionRepository;

    @MockitoBean
    Publisher<Trade> tradePublisher;

    @MockitoBean
    Publisher<Position> positionPublisher;

    @Test
    void bookingABuyOpensAPositionAndSettlesTheTrade() throws Exception {
        TradeBookingResult result = tradeService.processTrade(new TradeOrder(null, 1, "AAPL", TradeSide.Buy, 100));

        assertNotNull(result.getTrade().getId());
        assertEquals(TradeState.Settled, result.getTrade().getState());
        assertEquals(100, result.getPosition().getQuantity());
        assertEquals(100, positionRepository.findByAccountIdAndSecurity(1, "AAPL").getQuantity());

        verify(tradePublisher).publish("/accounts/1/trades", result.getTrade());
        verify(positionPublisher).publish("/accounts/1/positions", result.getPosition());
    }

    @Test
    void sellReducesTheExistingPositionInPlace() throws Exception {
        tradeService.processTrade(new TradeOrder(null, 2, "MSFT", TradeSide.Buy, 100));
        TradeBookingResult sell = tradeService.processTrade(new TradeOrder(null, 2, "MSFT", TradeSide.Sell, 40));

        assertEquals(60, sell.getPosition().getQuantity());
        assertEquals(60, positionRepository.findByAccountIdAndSecurity(2, "MSFT").getQuantity());
        verify(positionPublisher, org.mockito.Mockito.times(2)).publish(any(String.class), any(Position.class));
    }
}
