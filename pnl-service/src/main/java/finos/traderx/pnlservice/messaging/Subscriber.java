package finos.traderx.pnlservice.messaging;

public interface Subscriber<T> {
    void subscribe(String topic) throws PubSubException;
    void unsubscribe(String topic) throws PubSubException;
    void connect() throws PubSubException;
    void disconnect() throws PubSubException;
    boolean isConnected();
}
