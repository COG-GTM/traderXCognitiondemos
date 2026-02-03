package finos.traderx.pnlservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class PriceService {

    private final Map<String, BigDecimal> basePrices = new HashMap<>();
    private final Random random = new Random();

    public PriceService() {
        basePrices.put("IBM", new BigDecimal("145.50"));
        basePrices.put("MS", new BigDecimal("89.25"));
        basePrices.put("C", new BigDecimal("52.75"));
        basePrices.put("BAC", new BigDecimal("33.40"));
        basePrices.put("AAPL", new BigDecimal("178.50"));
        basePrices.put("GOOGL", new BigDecimal("141.25"));
        basePrices.put("MSFT", new BigDecimal("378.90"));
        basePrices.put("AMZN", new BigDecimal("178.25"));
        basePrices.put("META", new BigDecimal("505.75"));
        basePrices.put("NVDA", new BigDecimal("875.50"));
        basePrices.put("JPM", new BigDecimal("195.80"));
        basePrices.put("GS", new BigDecimal("385.25"));
        basePrices.put("WFC", new BigDecimal("55.90"));
        basePrices.put("V", new BigDecimal("275.40"));
        basePrices.put("MA", new BigDecimal("445.60"));
    }

    public BigDecimal getCurrentPrice(String security) {
        BigDecimal basePrice = basePrices.getOrDefault(security, new BigDecimal("100.00"));
        double variation = (random.nextDouble() - 0.5) * 0.02;
        return basePrice.multiply(BigDecimal.valueOf(1 + variation)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getHistoricalPrice(String security) {
        BigDecimal basePrice = basePrices.getOrDefault(security, new BigDecimal("100.00"));
        double variation = (random.nextDouble() - 0.5) * 0.10;
        return basePrice.multiply(BigDecimal.valueOf(1 + variation)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getBasePrice(String security) {
        return basePrices.getOrDefault(security, new BigDecimal("100.00"));
    }
}
