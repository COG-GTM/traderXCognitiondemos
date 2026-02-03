package finos.traderx.pnlservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import finos.traderx.pnlservice.model.MarketPrice;
import finos.traderx.pnlservice.model.PnlSummary;
import finos.traderx.pnlservice.model.Position;
import finos.traderx.pnlservice.model.SecurityPnl;
import finos.traderx.pnlservice.model.Trade;
import finos.traderx.pnlservice.repository.MarketPriceRepository;
import finos.traderx.pnlservice.repository.PositionRepository;
import finos.traderx.pnlservice.repository.TradeRepository;

@Service
public class PnlService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PnlService.class);

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private MarketPriceRepository marketPriceRepository;

    public PnlSummary getPnlSummary(Integer accountId) {
        List<Trade> settledTrades = tradeRepository.findByAccountIdAndState(accountId, "Settled");
        List<Position> positions = positionRepository.findByAccountId(accountId);

        Map<String, List<Trade>> tradesBySecurity = groupTradesBySecurity(settledTrades);
        Map<String, Position> positionsBySecurity = new HashMap<>();
        for (Position position : positions) {
            positionsBySecurity.put(position.getSecurity(), position);
        }

        List<SecurityPnl> securityPnls = new ArrayList<>();
        BigDecimal totalRealizedPnl = BigDecimal.ZERO;
        BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;

        for (String security : tradesBySecurity.keySet()) {
            SecurityPnl securityPnl = calculateSecurityPnl(accountId, security, 
                    tradesBySecurity.get(security), positionsBySecurity.get(security));
            securityPnls.add(securityPnl);
            totalRealizedPnl = totalRealizedPnl.add(securityPnl.getRealizedPnl());
            totalUnrealizedPnl = totalUnrealizedPnl.add(securityPnl.getUnrealizedPnl());
        }

        return new PnlSummary(accountId, totalRealizedPnl, totalUnrealizedPnl, securityPnls);
    }

    public List<SecurityPnl> getRealizedPnlBreakdown(Integer accountId) {
        List<Trade> settledTrades = tradeRepository.findByAccountIdAndState(accountId, "Settled");
        Map<String, List<Trade>> tradesBySecurity = groupTradesBySecurity(settledTrades);

        List<SecurityPnl> realizedPnls = new ArrayList<>();
        for (String security : tradesBySecurity.keySet()) {
            BigDecimal realizedPnl = calculateRealizedPnl(tradesBySecurity.get(security));
            SecurityPnl securityPnl = new SecurityPnl();
            securityPnl.setSecurity(security);
            securityPnl.setRealizedPnl(realizedPnl);
            securityPnl.setUnrealizedPnl(BigDecimal.ZERO);
            securityPnl.setTotalPnl(realizedPnl);
            realizedPnls.add(securityPnl);
        }

        return realizedPnls;
    }

    public List<SecurityPnl> getUnrealizedPnlBreakdown(Integer accountId) {
        List<Position> positions = positionRepository.findByAccountId(accountId);
        List<Trade> settledTrades = tradeRepository.findByAccountIdAndState(accountId, "Settled");
        Map<String, List<Trade>> tradesBySecurity = groupTradesBySecurity(settledTrades);

        List<SecurityPnl> unrealizedPnls = new ArrayList<>();
        for (Position position : positions) {
            String security = position.getSecurity();
            BigDecimal averageCost = calculateAverageCost(tradesBySecurity.getOrDefault(security, new ArrayList<>()));
            BigDecimal currentPrice = getCurrentMarketPrice(security);
            BigDecimal unrealizedPnl = calculateUnrealizedPnl(position.getQuantity(), averageCost, currentPrice);

            SecurityPnl securityPnl = new SecurityPnl();
            securityPnl.setSecurity(security);
            securityPnl.setQuantity(position.getQuantity());
            securityPnl.setAverageCost(averageCost);
            securityPnl.setCurrentPrice(currentPrice);
            securityPnl.setRealizedPnl(BigDecimal.ZERO);
            securityPnl.setUnrealizedPnl(unrealizedPnl);
            securityPnl.setTotalPnl(unrealizedPnl);
            unrealizedPnls.add(securityPnl);
        }

        return unrealizedPnls;
    }

    public SecurityPnl getSecurityPnl(Integer accountId, String security) {
        List<Trade> trades = tradeRepository.findByAccountIdAndSecurityAndState(accountId, security, "Settled");
        List<Position> positions = positionRepository.findByAccountId(accountId);
        Position position = positions.stream()
                .filter(p -> p.getSecurity().equals(security))
                .findFirst()
                .orElse(null);

        return calculateSecurityPnl(accountId, security, trades, position);
    }

    private SecurityPnl calculateSecurityPnl(Integer accountId, String security, List<Trade> trades, Position position) {
        BigDecimal realizedPnl = calculateRealizedPnl(trades);
        BigDecimal averageCost = calculateAverageCost(trades);
        BigDecimal currentPrice = getCurrentMarketPrice(security);
        
        Integer quantity = position != null ? position.getQuantity() : 0;
        BigDecimal unrealizedPnl = calculateUnrealizedPnl(quantity, averageCost, currentPrice);

        return new SecurityPnl(security, quantity, averageCost, currentPrice, realizedPnl, unrealizedPnl);
    }

    private BigDecimal calculateRealizedPnl(List<Trade> trades) {
        if (trades == null || trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<Trade> sortedTrades = new ArrayList<>(trades);
        sortedTrades.sort(Comparator.comparing(Trade::getCreated));

        Queue<TradeEntry> buyQueue = new LinkedList<>();
        BigDecimal realizedPnl = BigDecimal.ZERO;

        for (Trade trade : sortedTrades) {
            if (trade.getPrice() == null) {
                log.warn("Trade {} has no price, skipping P&L calculation", trade.getId());
                continue;
            }

            if ("Buy".equals(trade.getSide())) {
                buyQueue.add(new TradeEntry(trade.getQuantity(), trade.getPrice()));
            } else if ("Sell".equals(trade.getSide())) {
                int remainingToSell = trade.getQuantity();
                BigDecimal sellPrice = trade.getPrice();

                while (remainingToSell > 0 && !buyQueue.isEmpty()) {
                    TradeEntry buyEntry = buyQueue.peek();
                    int matchedQuantity = Math.min(remainingToSell, buyEntry.quantity);
                    
                    BigDecimal pnl = sellPrice.subtract(buyEntry.price).multiply(BigDecimal.valueOf(matchedQuantity));
                    realizedPnl = realizedPnl.add(pnl);

                    remainingToSell -= matchedQuantity;
                    buyEntry.quantity -= matchedQuantity;

                    if (buyEntry.quantity == 0) {
                        buyQueue.poll();
                    }
                }
            }
        }

        return realizedPnl.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageCost(List<Trade> trades) {
        if (trades == null || trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (Trade trade : trades) {
            if (trade.getPrice() == null) {
                continue;
            }
            if ("Buy".equals(trade.getSide())) {
                totalCost = totalCost.add(trade.getPrice().multiply(BigDecimal.valueOf(trade.getQuantity())));
                totalQuantity += trade.getQuantity();
            }
        }

        if (totalQuantity == 0) {
            return BigDecimal.ZERO;
        }

        return totalCost.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateUnrealizedPnl(Integer quantity, BigDecimal averageCost, BigDecimal currentPrice) {
        if (quantity == null || quantity == 0 || currentPrice == null) {
            return BigDecimal.ZERO;
        }

        return currentPrice.subtract(averageCost).multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getCurrentMarketPrice(String security) {
        return marketPriceRepository.findBySecurity(security)
                .map(MarketPrice::getPrice)
                .orElse(BigDecimal.ZERO);
    }

    private Map<String, List<Trade>> groupTradesBySecurity(List<Trade> trades) {
        Map<String, List<Trade>> tradesBySecurity = new HashMap<>();
        for (Trade trade : trades) {
            tradesBySecurity.computeIfAbsent(trade.getSecurity(), k -> new ArrayList<>()).add(trade);
        }
        return tradesBySecurity;
    }

    private static class TradeEntry {
        int quantity;
        BigDecimal price;

        TradeEntry(int quantity, BigDecimal price) {
            this.quantity = quantity;
            this.price = price;
        }
    }
}
