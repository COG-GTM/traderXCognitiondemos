package finos.traderx.tradeservice.model;

public class Account {
    private Integer id;
    private String displayName;
    private Long creditLimit;

    public Account()
    {


    }

    public Account (Integer id,String displayName)
    {
        this.id = id;
        this.displayName = displayName;
    }

    public Account (Integer id,String displayName, Long creditLimit)
    {
        this.id = id;
        this.displayName = displayName;
        this.creditLimit = creditLimit;
    }

    public Integer getid()
    {
        return id;
    }
    public String getdisplayName()
    {
        return displayName;
    }

    public Long getCreditLimit()
    {
        return creditLimit;
    }

    public void setCreditLimit(Long creditLimit)
    {
        this.creditLimit = creditLimit;
    }
}
