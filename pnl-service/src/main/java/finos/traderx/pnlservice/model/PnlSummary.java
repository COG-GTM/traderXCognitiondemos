package finos.traderx.pnlservice.model;

import java.math.BigDecimal;
import java.util.List;

public class PnlSummary {

    private Integer accountId;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;
    private BigDecimal totalPnl;
    private List<SecurityPnl> securityBreakdown;

    public PnlSummary() {
    }

    public PnlSummary(Integer accountId, BigDecimal realizedPnl, BigDecimal unrealizedPnl, List<SecurityPnl> securityBreakdown) {
        this.accountId = accountId;
        this.realizedPnl = realizedPnl;
        this.unrealizedPnl = unrealizedPnl;
        this.totalPnl = realizedPnl.add(unrealizedPnl);
        this.securityBreakdown = securityBreakdown;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }

    public BigDecimal getTotalPnl() {
        return totalPnl;
    }

    public void setTotalPnl(BigDecimal totalPnl) {
        this.totalPnl = totalPnl;
    }

    public List<SecurityPnl> getSecurityBreakdown() {
        return securityBreakdown;
    }

    public void setSecurityBreakdown(List<SecurityPnl> securityBreakdown) {
        this.securityBreakdown = securityBreakdown;
    }
}
