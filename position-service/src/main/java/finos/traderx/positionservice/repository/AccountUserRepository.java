package finos.traderx.positionservice.repository;

import org.springframework.data.repository.Repository;

import finos.traderx.positionservice.model.AccountUser;
import finos.traderx.positionservice.model.AccountUserID;

public interface AccountUserRepository extends Repository<AccountUser, AccountUserID> {

    boolean existsByAccountIdAndUsername(Integer accountId, String username);

}
