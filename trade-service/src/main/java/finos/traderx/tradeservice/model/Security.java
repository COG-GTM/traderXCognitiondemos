package finos.traderx.tradeservice.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Security {
    private String ticker;
    private String companyName;
    private BigDecimal lastPrice;

    public Security()
    {

    }

    public Security(String ticker, String companyName)
    {
        this(ticker, companyName, null);
    }

    public Security(String ticker, String companyName, BigDecimal lastPrice)
    {
        this.ticker = ticker;
        this.companyName = companyName;
        this.lastPrice = lastPrice;
    }

    public String getTicker()
    {
        return ticker;
    }

    public void setTicker(String ticker)
    {
        this.ticker = ticker;
    }

    public String getcompanyName()
    {
        return companyName; 
    }

    public void setcompanyName(String companyName)
    {
        this.companyName = companyName;
    }

    public BigDecimal getLastPrice()
    {
        return lastPrice;
    }

    public void setLastPrice(BigDecimal lastPrice)
    {
        this.lastPrice = lastPrice;
    }

    @Override
    public String toString()
    {
        return "Security{ticker=" + ticker + ", companyName=" + companyName + ", lastPrice=" + lastPrice + "}";
    }
}
