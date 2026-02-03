package finos.traderx.pnlservice.messaging;

public interface Envelope<T> {
    String getTopic();
    T getPayload();
}
