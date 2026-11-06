package finos.traderx.tradeservice.repository;

import org.springframework.data.repository.Repository;

import finos.traderx.tradeservice.model.AccountUser;
import finos.traderx.tradeservice.model.AccountUserID;

public interface AccountUserRepository extends Repository<AccountUser, AccountUserID> {

    boolean existsByAccountIdAndUsername(Integer accountId, String username);

}
