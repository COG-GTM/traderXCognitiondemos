package finos.traderx.pnlservice.messaging;

public interface Publisher<T> {
    void publish(T message) throws PubSubException;
    void publish(String topic, T message) throws PubSubException;
    void connect() throws PubSubException;
    void disconnect() throws PubSubException;
    boolean isConnected();
}
