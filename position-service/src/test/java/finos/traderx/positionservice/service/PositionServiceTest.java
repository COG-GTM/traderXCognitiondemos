package finos.traderx.positionservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import finos.traderx.positionservice.model.Position;
import finos.traderx.positionservice.repository.PositionRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

	@Mock
	PositionRepository positionRepository;

	@InjectMocks
	PositionService positionService;

	private Position position(Integer accountId, String security, Integer quantity) {
		Position position = new Position();
		position.setAccountId(accountId);
		position.setSecurity(security);
		position.setQuantity(quantity);
		return position;
	}

	private int netQuantity(List<Position> positions, String security) {
		return positions.stream()
				.filter(position -> security.equals(position.getSecurity()))
				.mapToInt(Position::getQuantity)
				.sum();
	}

	@Test
	@DisplayName("getAllPositions returns every position provided by the repository")
	void getAllPositionsReturnsAllPositions() {
		when(this.positionRepository.findAll())
				.thenReturn(Arrays.asList(position(1, "MSFT", 100), position(2, "AAPL", 50)));

		List<Position> positions = this.positionService.getAllPositions();

		assertEquals(2, positions.size());
		assertEquals("MSFT", positions.get(0).getSecurity());
		assertEquals("AAPL", positions.get(1).getSecurity());
	}

	@Test
	@DisplayName("getAllPositions returns an empty list when no positions exist")
	void getAllPositionsReturnsEmptyList() {
		when(this.positionRepository.findAll()).thenReturn(Collections.emptyList());

		assertTrue(this.positionService.getAllPositions().isEmpty());
	}

	@Test
	@DisplayName("getPositionsByAccountID returns only the positions of the requested account")
	void getPositionsByAccountIdReturnsAccountPositions() {
		when(this.positionRepository.findByAccountId(1))
				.thenReturn(Arrays.asList(position(1, "MSFT", 100), position(1, "AAPL", 25)));

		List<Position> positions = this.positionService.getPositionsByAccountID(1);

		assertEquals(2, positions.size());
		assertTrue(positions.stream().allMatch(position -> position.getAccountId() == 1));
		verify(this.positionRepository).findByAccountId(1);
	}

	@Test
	@DisplayName("getPositionsByAccountID returns an empty list for an account without positions")
	void getPositionsByAccountIdReturnsEmptyListForUnknownAccount() {
		when(this.positionRepository.findByAccountId(404)).thenReturn(Collections.emptyList());

		assertTrue(this.positionService.getPositionsByAccountID(404).isEmpty());
	}

	@Test
	@DisplayName("Position quantities of an account aggregate to the expected net exposure per security")
	void positionQuantitiesAggregatePerSecurity() {
		when(this.positionRepository.findByAccountId(1)).thenReturn(Arrays.asList(
				position(1, "MSFT", 100),
				position(1, "AAPL", 40),
				position(1, "IBM", 10)));

		List<Position> positions = this.positionService.getPositionsByAccountID(1);

		assertEquals(150, positions.stream().mapToInt(Position::getQuantity).sum());
		assertEquals(100, netQuantity(positions, "MSFT"));
		assertEquals(40, netQuantity(positions, "AAPL"));
		assertEquals(10, netQuantity(positions, "IBM"));
	}

	@Test
	@DisplayName("Short positions offset long positions when aggregating a security")
	void shortPositionsOffsetLongPositions() {
		when(this.positionRepository.findByAccountId(1)).thenReturn(Arrays.asList(
				position(1, "MSFT", 100),
				position(1, "MSFT", -30),
				position(1, "AAPL", -20)));

		List<Position> positions = this.positionService.getPositionsByAccountID(1);

		assertEquals(70, netQuantity(positions, "MSFT"));
		assertEquals(-20, netQuantity(positions, "AAPL"));
		assertEquals(50, positions.stream().mapToInt(Position::getQuantity).sum());
	}

	@Test
	@DisplayName("A fully unwound position aggregates to a zero net quantity")
	void fullyUnwoundPositionAggregatesToZero() {
		when(this.positionRepository.findByAccountId(1)).thenReturn(Arrays.asList(
				position(1, "MSFT", 100),
				position(1, "MSFT", -100)));

		assertEquals(0, netQuantity(this.positionService.getPositionsByAccountID(1), "MSFT"));
	}

	@Test
	@DisplayName("Positions are kept separate per account when aggregating across all accounts")
	void positionsAggregateSeparatelyPerAccount() {
		when(this.positionRepository.findAll()).thenReturn(Arrays.asList(
				position(1, "MSFT", 100),
				position(2, "MSFT", 250),
				position(2, "AAPL", 75)));

		List<Position> positions = this.positionService.getAllPositions();

		assertEquals(100, positions.stream()
				.filter(position -> position.getAccountId() == 1)
				.mapToInt(Position::getQuantity)
				.sum());
		assertEquals(325, positions.stream()
				.filter(position -> position.getAccountId() == 2)
				.mapToInt(Position::getQuantity)
				.sum());
	}
}
