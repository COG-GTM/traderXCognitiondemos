package finos.traderx.pnlservice.messaging;

public class PubSubException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public PubSubException(String message) {
        super(message);
    }

    public PubSubException(String message, Throwable cause) {
        super(message, cause);
    }
}
