package finos.traderx.accountservice.repository;

import finos.traderx.accountservice.model.RiskLimit;

import org.springframework.data.repository.CrudRepository;

public interface RiskLimitRepository extends CrudRepository<RiskLimit, Integer> {
}
