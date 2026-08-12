package finos.traderx.accountservice.repository;

import java.util.List;

import finos.traderx.accountservice.model.RiskLimitHistory;

import org.springframework.data.repository.CrudRepository;

public interface RiskLimitHistoryRepository extends CrudRepository<RiskLimitHistory, Long> {

	List<RiskLimitHistory> findByAccountIdOrderByChangedAtDescIdDesc(int accountId);
}
