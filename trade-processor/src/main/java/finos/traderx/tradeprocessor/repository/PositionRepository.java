package finos.traderx.tradeprocessor.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.PositionID;
import jakarta.persistence.LockModeType;

public interface PositionRepository extends JpaRepository<Position,PositionID> {

    List<Position> findByAccountId(Integer id);
    Position findByAccountIdAndSecurity(Integer id, String security);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Position p where p.accountId = :accountId and p.security = :security")
    Position lockByAccountIdAndSecurity(@Param("accountId") Integer accountId, @Param("security") String security);

}
