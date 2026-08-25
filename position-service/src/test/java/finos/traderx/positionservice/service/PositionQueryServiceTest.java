package finos.traderx.positionservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import finos.traderx.positionservice.model.Position;
import finos.traderx.positionservice.model.Trade;
import finos.traderx.positionservice.repository.PositionRepository;
import finos.traderx.positionservice.repository.TradeRepository;

/** Corner cases of the position/trade query services, with the repositories mocked out. */
@ExtendWith(MockitoExtension.class)
class PositionQueryServiceTest {

	@Mock
	private PositionRepository positionRepository;

	@Mock
	private TradeRepository tradeRepository;

	@InjectMocks
	private PositionService positionService;

	@InjectMocks
	private TradeService tradeService;

	private static Position position(int accountId, String security, Integer quantity) {
		Position position = new Position();
		position.setAccountId(accountId);
		position.setSecurity(security);
		position.setQuantity(quantity);
		return position;
	}

	@Test
	@DisplayName("PS-26e: getAllPositions returns an empty, non-null list for an empty repository")
	void getAllPositionsOnEmptyRepository() {
		when(this.positionRepository.findAll()).thenReturn(Collections.emptyList());

		List<Position> positions = this.positionService.getAllPositions();

		assertNotNull(positions);
		assertTrue(positions.isEmpty());
	}

	@Test
	@DisplayName("PS-26f: getAllTrades returns an empty, non-null list for an empty repository")
	void getAllTradesOnEmptyRepository() {
		when(this.tradeRepository.findAll()).thenReturn(Collections.emptyList());

		assertTrue(this.tradeService.getAllTrades().isEmpty());
	}

	@Test
	@DisplayName("PS-25j: getPositionsByAccountID passes the id straight to the repository, including 0 and -1")
	void boundaryAccountIdsAreDelegated() {
		when(this.positionRepository.findByAccountId(0)).thenReturn(Collections.emptyList());
		when(this.positionRepository.findByAccountId(-1)).thenReturn(Collections.emptyList());

		assertTrue(this.positionService.getPositionsByAccountID(0).isEmpty());
		assertTrue(this.positionService.getPositionsByAccountID(-1).isEmpty());
	}

	@Test
	@DisplayName("PS-28g: zero and negative (short) quantities survive the service layer untouched")
	void quantitiesAreNotFiltered() {
		when(this.positionRepository.findByAccountId(1))
				.thenReturn(List.of(position(1, "AAPL", 0), position(1, "MSFT", -25)));

		List<Position> positions = this.positionService.getPositionsByAccountID(1);

		assertEquals(2, positions.size());
		assertEquals(0, positions.get(0).getQuantity());
		assertEquals(-25, positions.get(1).getQuantity());
	}

	@Test
	@DisplayName("PS-25k: a repository failure propagates out of the service (handled by the controller)")
	void repositoryFailurePropagates() {
		when(this.positionRepository.findByAccountId(1)).thenThrow(new RuntimeException("db down"));

		assertEquals("db down",
				assertThrows(RuntimeException.class, () -> this.positionService.getPositionsByAccountID(1))
						.getMessage());
	}

	@Test
	@DisplayName("PS-25l: getPositionsByAccountID returns whatever the repository returns, including null")
	void nullFromRepositoryIsPassedThrough() {
		when(this.positionRepository.findByAccountId(1)).thenReturn(null);

		assertNull(this.positionService.getPositionsByAccountID(1));
	}

	@Test
	@Disabled("LATENT BUG: PositionService.getPositionsByAccountID returns the repository result verbatim, "
			+ "so a null (rather than empty list) result would be serialised as a null JSON body")
	@DisplayName("PS-25m: getPositionsByAccountID should never return null")
	void shouldNeverReturnNull() {
		when(this.positionRepository.findByAccountId(1)).thenReturn(null);

		assertNotNull(this.positionService.getPositionsByAccountID(1));
	}

	@Test
	@DisplayName("PS-26g: getAllTrades copies every row returned by the repository")
	void getAllTradesCopiesRows() {
		Trade trade = new Trade();
		trade.setId("t1");
		when(this.tradeRepository.findAll()).thenReturn(List.of(trade));

		assertEquals(1, this.tradeService.getAllTrades().size());
	}
}
