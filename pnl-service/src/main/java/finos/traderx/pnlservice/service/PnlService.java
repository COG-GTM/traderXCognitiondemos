package finos.traderx.pnlservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import finos.traderx.pnlservice.model.PnlSummary;
import finos.traderx.pnlservice.model.Position;
import finos.traderx.pnlservice.model.RealizedPnlDetail;
import finos.traderx.pnlservice.model.SecurityPnl;
import finos.traderx.pnlservice.model.Trade;
import finos.traderx.pnlservice.model.UnrealizedPnlDetail;
import finos.traderx.pnlservice.repository.PositionRepository;
import finos.traderx.pnlservice.repository.TradeRepository;

@Service
public class PnlService {

    private static final String SETTLED_STATE = "Settled";
    private static final String BUY_SIDE = "Buy";
    private static final String SELL_SIDE = "Sell";

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PriceService priceService;

    public PnlSummary getPnlSummary(Integer accountId) {
        List<Trade> settledTrades = tradeRepository.findByAccountIdAndState(accountId, SETTLED_STATE);
        List<Position> positions = positionRepository.findByAccountId(accountId);

        Map<String, List<Trade>> tradesBySecurity = groupTradesBySecurity(settledTrades);
        
        List<SecurityPnl> securityBreakdown = new ArrayList<>();
        BigDecimal totalRealizedPnl = BigDecimal.ZERO;
        BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;

        for (Position position : positions) {
            String security = position.getSecurity();
            List<Trade> securityTrades = tradesBySecurity.getOrDefault(security, new ArrayList<>());
            
            BigDecimal averageCost = calculateAverageCost(securityTrades);
            BigDecimal currentPrice = priceService.getCurrentPrice(security);
            BigDecimal realizedPnl = calculateRealizedPnl(securityTrades);
            BigDecimal unrealizedPnl = calculateUnrealizedPnl(position, averageCost, currentPrice);

            SecurityPnl securityPnl = new SecurityPnl(
                security,
                position.getQuantity(),
                averageCost,
                currentPrice,
                realizedPnl,
                unrealizedPnl
            );
            securityBreakdown.add(securityPnl);

            totalRealizedPnl = totalRealizedPnl.add(realizedPnl);
            totalUnrealizedPnl = totalUnrealizedPnl.add(unrealizedPnl);
        }

        return new PnlSummary(accountId, totalRealizedPnl, totalUnrealizedPnl, securityBreakdown);
    }

    public List<RealizedPnlDetail> getRealizedPnlDetails(Integer accountId) {
        List<Trade> settledTrades = tradeRepository.findByAccountIdAndState(accountId, SETTLED_STATE);
        Map<String, List<Trade>> tradesBySecurity = groupTradesBySecurity(settledTrades);
        
        List<RealizedPnlDetail> details = new ArrayList<>();

        for (Map.Entry<String, List<Trade>> entry : tradesBySecurity.entrySet()) {
            String security = entry.getKey();
            List<Trade> trades = entry.getValue();
            
            trades.sort(Comparator.comparing(Trade::getCreated));
            
            List<TradeWithPrice> buyQueue = new ArrayList<>();
            
            for (Trade trade : trades) {
                BigDecimal tradePrice = priceService.getHistoricalPrice(security);
                
                if (BUY_SIDE.equals(trade.getSide())) {
                    buyQueue.add(new TradeWithPrice(trade, tradePrice));
                } else if (SELL_SIDE.equals(trade.getSide())) {
                    BigDecimal realizedPnl = calculateRealizedPnlForSell(trade, tradePrice, buyQueue);
                    
                    RealizedPnlDetail detail = new RealizedPnlDetail(
                        trade.getId(),
                        security,
                        trade.getSide(),
                        trade.getQuantity(),
                        tradePrice,
                        realizedPnl,
                        trade.getCreated()
                    );
                    details.add(detail);
                }
            }
        }

        return details;
    }

    public List<UnrealizedPnlDetail> getUnrealizedPnlDetails(Integer accountId) {
        List<Position> positions = positionRepository.findByAccountId(accountId);
        List<Trade> settledTrades = tradeRepository.findByAccountIdAndState(accountId, SETTLED_STATE);
        Map<String, List<Trade>> tradesBySecurity = groupTradesBySecurity(settledTrades);
        
        List<UnrealizedPnlDetail> details = new ArrayList<>();

        for (Position position : positions) {
            if (position.getQuantity() == 0) {
                continue;
            }

            String security = position.getSecurity();
            List<Trade> securityTrades = tradesBySecurity.getOrDefault(security, new ArrayList<>());
            
            BigDecimal averageCost = calculateAverageCost(securityTrades);
            BigDecimal currentPrice = priceService.getCurrentPrice(security);

            UnrealizedPnlDetail detail = new UnrealizedPnlDetail(
                security,
                position.getQuantity(),
                averageCost,
                currentPrice
            );
            details.add(detail);
        }

        return details;
    }

    private Map<String, List<Trade>> groupTradesBySecurity(List<Trade> trades) {
        Map<String, List<Trade>> tradesBySecurity = new HashMap<>();
        for (Trade trade : trades) {
            tradesBySecurity.computeIfAbsent(trade.getSecurity(), k -> new ArrayList<>()).add(trade);
        }
        return tradesBySecurity;
    }

    private BigDecimal calculateAverageCost(List<Trade> trades) {
        BigDecimal totalCost = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (Trade trade : trades) {
            if (BUY_SIDE.equals(trade.getSide())) {
                BigDecimal price = priceService.getBasePrice(trade.getSecurity());
                totalCost = totalCost.add(price.multiply(BigDecimal.valueOf(trade.getQuantity())));
                totalQuantity += trade.getQuantity();
            }
        }

        if (totalQuantity == 0) {
            return BigDecimal.ZERO;
        }

        return totalCost.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRealizedPnl(List<Trade> trades) {
        trades.sort(Comparator.comparing(Trade::getCreated));
        
        List<TradeWithPrice> buyQueue = new ArrayList<>();
        BigDecimal totalRealizedPnl = BigDecimal.ZERO;

        for (Trade trade : trades) {
            BigDecimal tradePrice = priceService.getBasePrice(trade.getSecurity());
            
            if (BUY_SIDE.equals(trade.getSide())) {
                buyQueue.add(new TradeWithPrice(trade, tradePrice));
            } else if (SELL_SIDE.equals(trade.getSide())) {
                BigDecimal realizedPnl = calculateRealizedPnlForSell(trade, tradePrice, buyQueue);
                totalRealizedPnl = totalRealizedPnl.add(realizedPnl);
            }
        }

        return totalRealizedPnl;
    }

    private BigDecimal calculateRealizedPnlForSell(Trade sellTrade, BigDecimal sellPrice, List<TradeWithPrice> buyQueue) {
        int remainingQuantity = sellTrade.getQuantity();
        BigDecimal realizedPnl = BigDecimal.ZERO;

        while (remainingQuantity > 0 && !buyQueue.isEmpty()) {
            TradeWithPrice buyTrade = buyQueue.get(0);
            int buyQuantity = buyTrade.getRemainingQuantity();
            
            int matchedQuantity = Math.min(remainingQuantity, buyQuantity);
            
            BigDecimal pnl = sellPrice.subtract(buyTrade.getPrice())
                .multiply(BigDecimal.valueOf(matchedQuantity));
            realizedPnl = realizedPnl.add(pnl);
            
            buyTrade.reduceQuantity(matchedQuantity);
            remainingQuantity -= matchedQuantity;
            
            if (buyTrade.getRemainingQuantity() == 0) {
                buyQueue.remove(0);
            }
        }

        if (remainingQuantity > 0) {
            BigDecimal shortPnl = sellPrice.multiply(BigDecimal.valueOf(remainingQuantity)).negate();
            realizedPnl = realizedPnl.add(shortPnl);
        }

        return realizedPnl;
    }

    private BigDecimal calculateUnrealizedPnl(Position position, BigDecimal averageCost, BigDecimal currentPrice) {
        if (position.getQuantity() == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal quantity = BigDecimal.valueOf(position.getQuantity());
        
        if (position.getQuantity() > 0) {
            return currentPrice.subtract(averageCost).multiply(quantity);
        } else {
            return averageCost.subtract(currentPrice).multiply(quantity.abs());
        }
    }

    private static class TradeWithPrice {
        private final Trade trade;
        private final BigDecimal price;
        private int remainingQuantity;

        public TradeWithPrice(Trade trade, BigDecimal price) {
            this.trade = trade;
            this.price = price;
            this.remainingQuantity = trade.getQuantity();
        }

        public BigDecimal getPrice() {
            return price;
        }

        public int getRemainingQuantity() {
            return remainingQuantity;
        }

        public void reduceQuantity(int quantity) {
            this.remainingQuantity -= quantity;
        }
    }
}
