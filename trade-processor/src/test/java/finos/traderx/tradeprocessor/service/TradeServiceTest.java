package finos.traderx.tradeprocessor.service;

import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.audit.TradeAuditLogger;
import finos.traderx.tradeprocessor.model.*;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private TradeAuditLogger auditLogger;

    @Mock
    private Publisher<Trade> tradePublisher;

    @Mock
    private Publisher<Position> positionPublisher;

    @InjectMocks
    private TradeService tradeService;

    @Test
    void processTrade_emitsThreeAuditEvents() throws PubSubException {
        TradeOrder order = new TradeOrder("user-1", 42, "AAPL", TradeSide.Buy, 100);
        when(positionRepository.findByAccountIdAndSecurity(42, "AAPL")).thenReturn(null);

        tradeService.processTrade(order);

        verify(auditLogger).logTradeReceived(eq(order), anyString());
        verify(auditLogger).logTradeStateChange(any(Trade.class), eq(TradeState.New));
        verify(auditLogger).logTradeSettled(any(Trade.class));
        verifyNoMoreInteractions(auditLogger);
    }

    @Test
    void processTrade_auditLoggerCalledBeforePublish() throws PubSubException {
        TradeOrder order = new TradeOrder("user-2", 10, "MSFT", TradeSide.Sell, 50);
        Position existingPosition = new Position();
        existingPosition.setAccountId(10);
        existingPosition.setSecurity("MSFT");
        existingPosition.setQuantity(100);
        when(positionRepository.findByAccountIdAndSecurity(10, "MSFT")).thenReturn(existingPosition);

        tradeService.processTrade(order);

        // Audit events are logged
        verify(auditLogger).logTradeReceived(eq(order), anyString());
        verify(auditLogger).logTradeStateChange(any(Trade.class), eq(TradeState.New));
        verify(auditLogger).logTradeSettled(any(Trade.class));

        // Trade and position are published
        verify(tradePublisher).publish(eq("/accounts/10/trades"), any(Trade.class));
        verify(positionPublisher).publish(eq("/accounts/10/positions"), any(Position.class));
    }

    @Test
    void processTrade_auditLoggerStillCalledWhenPublishFails() throws PubSubException {
        TradeOrder order = new TradeOrder("user-3", 5, "GOOG", TradeSide.Buy, 25);
        when(positionRepository.findByAccountIdAndSecurity(5, "GOOG")).thenReturn(null);
        doThrow(new PubSubException("connection lost")).when(tradePublisher).publish(anyString(), any(Trade.class));

        tradeService.processTrade(order);

        // Audit events should still have been logged before the publish failure
        verify(auditLogger).logTradeReceived(eq(order), anyString());
        verify(auditLogger).logTradeStateChange(any(Trade.class), eq(TradeState.New));
        verify(auditLogger).logTradeSettled(any(Trade.class));
    }
}
