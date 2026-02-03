package finos.traderx.pnlservice.model;

import java.math.BigDecimal;
import java.util.Date;

public class RealizedPnlDetail {

    private String tradeId;
    private String security;
    private String side;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal realizedPnl;
    private Date tradeDate;

    public RealizedPnlDetail() {
    }

    public RealizedPnlDetail(String tradeId, String security, String side, Integer quantity,
                             BigDecimal price, BigDecimal realizedPnl, Date tradeDate) {
        this.tradeId = tradeId;
        this.security = security;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.realizedPnl = realizedPnl;
        this.tradeDate = tradeDate;
    }

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public Date getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(Date tradeDate) {
        this.tradeDate = tradeDate;
    }
}
