package finos.traderx.tradeprocessor.repository;


import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import finos.traderx.tradeprocessor.model.Position;
import finos.traderx.tradeprocessor.model.PositionID;
import jakarta.persistence.LockModeType;

public interface PositionRepository extends JpaRepository<Position,PositionID> {

    List<Position> findByAccountId(Integer id);
    Position findByAccountIdAndSecurity(Integer id, String security);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Position findForUpdateByAccountIdAndSecurity(Integer id, String security);

    // Plain INSERT (not merge) so a concurrently created row fails with a key violation instead of being overwritten.
    @Modifying
    @Query(value = "INSERT INTO POSITIONS (ACCOUNTID, SECURITY, QUANTITY, UPDATED) VALUES (:accountId, :security, 0, :updated)", nativeQuery = true)
    int insertEmptyPosition(@Param("accountId") Integer accountId, @Param("security") String security, @Param("updated") Date updated);

}
