package finos.traderx.tradeprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import finos.traderx.messaging.Publisher;
import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeBookingResult;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.regulatory.UnreportableTradeException;
import finos.traderx.tradeprocessor.repository.PositionRepository;
import finos.traderx.tradeprocessor.repository.TradeRepository;

/**
 * The trade store is the reporting source of truth, so the UTI, LEI and regime must survive the
 * trade-service to trade-processor boundary, and an order that could not be reported must not book.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeReportingPropagationTest {

    private static final String LEI = "549300TRADERX0ACC128";
    private static final String UTI = LEI + "0F7C2C0C1B7A4C8B9E1D2F3A4B5C6D78";

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private Publisher<Trade> tradePublisher;

    @Mock
    private Publisher<Position> positionPublisher;

    @InjectMocks
    private TradeService tradeService;

    @Captor
    private ArgumentCaptor<Trade> savedTrade;

    @BeforeEach
    void setUp() {
        when(this.positionRepository.findByAccountIdAndSecurity(anyInt(), anyString())).thenReturn(null);
    }

    private TradeOrder reportableOrder() {
        TradeOrder order = new TradeOrder("order-1", 22214, "IBM", TradeSide.Buy, 100);
        order.setUti(UTI);
        order.setReportingCounterpartyLei(LEI);
        order.setReportingRegime("EMIR_REFIT");
        return order;
    }

    @Test
    void bookedTradeCarriesTheReportingFieldsFromTheOrder() {
        TradeBookingResult result = this.tradeService.processTrade(reportableOrder());

        assertEquals(UTI, result.getTrade().getUti());
        assertEquals(LEI, result.getTrade().getReportingCounterpartyLei());
        assertEquals("EMIR_REFIT", result.getTrade().getReportingRegime());

        verify(this.tradeRepository, org.mockito.Mockito.atLeastOnce()).save(this.savedTrade.capture());
        assertEquals(UTI, this.savedTrade.getValue().getUti());
        assertEquals(LEI, this.savedTrade.getValue().getReportingCounterpartyLei());
    }

    @Test
    void refusesToBookAnOrderWithoutAUti() {
        TradeOrder order = reportableOrder();
        order.setUti(null);

        UnreportableTradeException thrown = assertThrows(UnreportableTradeException.class,
                () -> this.tradeService.processTrade(order));

        assertEquals("REG-011", thrown.getCode());
        verify(this.tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void refusesToBookAnOrderWithoutAnLei() {
        TradeOrder order = reportableOrder();
        order.setReportingCounterpartyLei(null);

        UnreportableTradeException thrown = assertThrows(UnreportableTradeException.class,
                () -> this.tradeService.processTrade(order));

        assertEquals("REG-021", thrown.getCode());
        verify(this.tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void refusesToBookAnOrderWhoseUtiPrefixIsNotTheReportingLei() {
        TradeOrder order = reportableOrder();
        order.setUti("7LTWFZYICNSX8D621K860F7C2C0C1B7A4C8B9E1D2F3A4B5C6D78");

        UnreportableTradeException thrown = assertThrows(UnreportableTradeException.class,
                () -> this.tradeService.processTrade(order));

        assertEquals("REG-013", thrown.getCode());
        verify(this.tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void refusesToBookAnOrderForAnUnsupportedRegime() {
        TradeOrder order = reportableOrder();
        order.setReportingRegime("EMIR_PRE_REFIT");

        UnreportableTradeException thrown = assertThrows(UnreportableTradeException.class,
                () -> this.tradeService.processTrade(order));

        assertEquals("REG-031", thrown.getCode());
        verify(this.tradeRepository, never()).save(any(Trade.class));
    }
}
