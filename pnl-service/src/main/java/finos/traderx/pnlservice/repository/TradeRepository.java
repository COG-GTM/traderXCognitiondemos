package finos.traderx.pnlservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import finos.traderx.pnlservice.model.Trade;

public interface TradeRepository extends JpaRepository<Trade, String> {
    
    List<Trade> findByAccountId(Integer id);
    
    List<Trade> findByAccountIdAndState(Integer accountId, String state);
    
    List<Trade> findByAccountIdAndSecurity(Integer accountId, String security);
    
    List<Trade> findByAccountIdAndSecurityAndState(Integer accountId, String security, String state);
}
