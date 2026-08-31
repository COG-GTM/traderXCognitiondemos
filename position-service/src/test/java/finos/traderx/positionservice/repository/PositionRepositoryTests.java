package finos.traderx.positionservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import finos.traderx.positionservice.model.Position;

@DataJpaTest
class PositionRepositoryTests {

    @Autowired
    PositionRepository positionRepository;

    private Position position(int accountId, String security, int quantity) {
        Position p = new Position();
        p.setAccountId(accountId);
        p.setSecurity(security);
        p.setQuantity(quantity);
        p.setUpdated(new Date());
        return p;
    }

    @Test
    void persistsCompositeKeyedPositionsAndQueriesByAccount() {
        positionRepository.save(position(1, "AAPL", 100));
        positionRepository.save(position(1, "MSFT", 50));
        positionRepository.save(position(2, "AAPL", 10));

        List<Position> forAccountOne = positionRepository.findByAccountId(1);

        assertEquals(2, forAccountOne.size());
        assertTrue(forAccountOne.stream().allMatch(p -> p.getAccountId() == 1));
    }

    @Test
    void updatesExistingPositionRatherThanInsertingDuplicate() {
        positionRepository.save(position(3, "IBM", 100));
        positionRepository.save(position(3, "IBM", 175));

        List<Position> positions = positionRepository.findByAccountId(3);

        assertEquals(1, positions.size());
        assertEquals(175, positions.get(0).getQuantity());
    }
}
