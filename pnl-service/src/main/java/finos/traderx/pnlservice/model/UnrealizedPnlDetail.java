package finos.traderx.pnlservice.model;

import java.math.BigDecimal;

public class UnrealizedPnlDetail {

    private String security;
    private Integer quantity;
    private BigDecimal averageCost;
    private BigDecimal currentPrice;
    private BigDecimal costBasis;
    private BigDecimal marketValue;
    private BigDecimal unrealizedPnl;

    public UnrealizedPnlDetail() {
    }

    public UnrealizedPnlDetail(String security, Integer quantity, BigDecimal averageCost, BigDecimal currentPrice) {
        this.security = security;
        this.quantity = quantity;
        this.averageCost = averageCost;
        this.currentPrice = currentPrice;
        this.costBasis = averageCost.multiply(BigDecimal.valueOf(Math.abs(quantity)));
        this.marketValue = currentPrice.multiply(BigDecimal.valueOf(Math.abs(quantity)));
        this.unrealizedPnl = this.marketValue.subtract(this.costBasis);
        if (quantity < 0) {
            this.unrealizedPnl = this.unrealizedPnl.negate();
        }
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAverageCost() {
        return averageCost;
    }

    public void setAverageCost(BigDecimal averageCost) {
        this.averageCost = averageCost;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }
}
