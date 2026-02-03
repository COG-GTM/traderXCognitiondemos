package finos.traderx.pnlservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import finos.traderx.pnlservice.model.Position;
import finos.traderx.pnlservice.model.PositionID;

public interface PositionRepository extends JpaRepository<Position, PositionID> {

    List<Position> findByAccountId(Integer id);

}
