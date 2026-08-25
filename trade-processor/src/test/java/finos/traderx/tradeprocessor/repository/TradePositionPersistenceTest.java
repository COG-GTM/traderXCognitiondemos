package finos.traderx.tradeprocessor.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;

import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.PositionID;
import finos.traderx.tradeprocessor.model.Trade;
import finos.traderx.tradeprocessor.model.TradeSide;
import finos.traderx.tradeprocessor.model.TradeState;
import jakarta.persistence.EntityManager;

/**
 * Entity / repository level behaviour against an in-memory H2 database.
 */
@DataJpaTest
class TradePositionPersistenceTest {

	@Autowired
	TradeRepository tradeRepository;

	@Autowired
	PositionRepository positionRepository;

	@Autowired
	EntityManager entityManager;

	private Trade trade(String id, Integer accountId, String security, TradeSide side, Integer quantity,
			TradeState state) {
		Trade t = new Trade();
		t.setId(id);
		t.setAccountId(accountId);
		t.setSecurity(security);
		t.setSide(side);
		t.setQuantity(quantity);
		t.setState(state);
		t.setCreated(new Date());
		t.setUpdated(new Date());
		return t;
	}

	private Position position(Integer accountId, String security, Integer quantity) {
		Position p = new Position();
		p.setAccountId(accountId);
		p.setSecurity(security);
		p.setQuantity(quantity);
		p.setUpdated(new Date());
		return p;
	}

	/** DB-01 */
	@Test
	@DisplayName("DB-01 every TradeState and TradeSide round-trips through the STATE/SIDE columns")
	void allEnumValuesRoundTrip() {
		int i = 0;
		for (TradeState state : TradeState.values()) {
			for (TradeSide side : TradeSide.values()) {
				tradeRepository.saveAndFlush(trade("t-" + (i++), 1, "IBM", side, 10, state));
			}
		}
		entityManager.clear();
		List<Trade> all = tradeRepository.findByAccountId(1);
		assertEquals(TradeState.values().length * TradeSide.values().length, all.size());
		assertTrue(all.stream().anyMatch(t -> t.getState() == TradeState.Cancelled),
				"Cancelled persists fine even though no code path produces it");
	}

	/** DB-02 */
	@Test
	@DisplayName("DB-02 the position composite key (accountId, security) upserts instead of inserting a second row")
	void compositeKeyUpserts() {
		positionRepository.saveAndFlush(position(1, "IBM", 100));
		positionRepository.saveAndFlush(position(1, "IBM", 150));
		entityManager.clear();

		List<Position> positions = positionRepository.findByAccountId(1);
		assertEquals(1, positions.size(), "same key => single row");
		assertEquals(150, positions.get(0).getQuantity());
	}

	/** DB-03 */
	@Test
	@DisplayName("DB-03 the same security for two accounts produces two independent rows")
	void sameSecurityDifferentAccounts() {
		positionRepository.saveAndFlush(position(1, "IBM", 100));
		positionRepository.saveAndFlush(position(2, "IBM", -40));
		entityManager.clear();

		assertEquals(100, positionRepository.findByAccountIdAndSecurity(1, "IBM").getQuantity());
		assertEquals(-40, positionRepository.findByAccountIdAndSecurity(2, "IBM").getQuantity());
	}

	/** DB-04 */
	@Test
	@DisplayName("DB-04 a negative (short) position is persisted without complaint")
	void negativePositionPersists() {
		positionRepository.saveAndFlush(position(1, "IBM", -500));
		entityManager.clear();
		assertEquals(-500, positionRepository.findByAccountIdAndSecurity(1, "IBM").getQuantity());
	}

	/** DB-05 */
	@Test
	@DisplayName("DB-05 findByAccountIdAndSecurity returns null (not empty/Optional) for an unknown key")
	void unknownKeyReturnsNull() {
		assertNull(positionRepository.findByAccountIdAndSecurity(42, "NOPE"));
		assertTrue(positionRepository.findByAccountId(42).isEmpty());
	}

	/** DB-06 */
	@Test
	@DisplayName("DB-06 a security longer than the 50 character column is rejected by the database")
	void overlongSecurityIsRejectedByTheDatabase() {
		assertThrows(DataAccessException.class,
				() -> positionRepository.saveAndFlush(position(1, "X".repeat(200), 10)));
	}

	/** DB-07 */
	@Test
	@DisplayName("DB-07 a position with a null security key component cannot be persisted")
	void nullSecurityKeyIsRejected() {
		assertThrows(Exception.class, () -> positionRepository.saveAndFlush(position(1, null, 10)));
	}

	/** DB-08 */
	@Test
	@DisplayName("DB-08 a position with a null accountId key component cannot be persisted")
	void nullAccountIdKeyIsRejected() {
		assertThrows(Exception.class, () -> positionRepository.saveAndFlush(position(null, "IBM", 10)));
	}

	/** DB-09 */
	@Test
	@DisplayName("DB-09 an empty string security is a perfectly valid key")
	void emptySecurityIsAValidKey() {
		positionRepository.saveAndFlush(position(1, "", 10));
		entityManager.clear();
		assertNotNull(positionRepository.findByAccountIdAndSecurity(1, ""));
	}

	/** DB-10 */
	@Test
	@DisplayName("DB-10 PositionID declares no equals/hashCode, which JPA requires of an @IdClass")
	void positionIdHasNoEqualsOrHashCode() throws Exception {
		boolean declaresEquals = true;
		try {
			PositionID.class.getDeclaredMethod("equals", Object.class);
		} catch (NoSuchMethodException e) {
			declaresEquals = false;
		}
		boolean declaresHashCode = true;
		try {
			PositionID.class.getDeclaredMethod("hashCode");
		} catch (NoSuchMethodException e) {
			declaresHashCode = false;
		}
		assertFalse(declaresEquals, "PositionID must override equals for @IdClass usage");
		assertFalse(declaresHashCode, "PositionID must override hashCode for @IdClass usage");
		assertFalse(new PositionID(1, "IBM").equals(new PositionID(1, "IBM")),
				"two identical keys are not equal, so identity based lookups and caching misbehave");
	}

	/** DB-11 */
	@Test
	@DisplayName("DB-11 TradeRepository is declared with an Integer id although Trade.id is a String")
	void tradeRepositoryIdTypeIsWrong() throws Exception {
		tradeRepository.saveAndFlush(trade("t-1", 1, "IBM", TradeSide.Buy, 10, TradeState.Settled));
		entityManager.clear();

		assertEquals(1, tradeRepository.findByAccountId(1).size());
		// The inherited findById takes an Integer, which can never designate the
		// String primary key of a Trade.
		assertTrue(tradeRepository.findById(1).isEmpty());

		java.lang.reflect.ParameterizedType jpaRepository = null;
		for (java.lang.reflect.Type t : TradeRepository.class.getGenericInterfaces()) {
			jpaRepository = (java.lang.reflect.ParameterizedType) t;
		}
		assertNotNull(jpaRepository);
		assertEquals(Integer.class, jpaRepository.getActualTypeArguments()[1],
				"TradeRepository declares Integer as the id type");
		assertEquals(String.class, Trade.class.getDeclaredField("id").getType(),
				"but Trade.id is a String");
	}
}
