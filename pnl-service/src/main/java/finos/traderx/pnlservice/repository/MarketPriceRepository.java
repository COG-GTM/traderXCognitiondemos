package finos.traderx.pnlservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import finos.traderx.pnlservice.model.MarketPrice;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, String> {

    Optional<MarketPrice> findBySecurity(String security);

}
