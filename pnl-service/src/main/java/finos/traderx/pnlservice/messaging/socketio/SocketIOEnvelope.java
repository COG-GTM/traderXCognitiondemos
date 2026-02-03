package finos.traderx.pnlservice.messaging.socketio;

import finos.traderx.pnlservice.messaging.Envelope;

public class SocketIOEnvelope<T> implements Envelope<T> {
    
    private String topic;
    private T payload;
    private String type;

    public SocketIOEnvelope() {
    }

    public SocketIOEnvelope(String topic, T payload) {
        this.topic = topic;
        this.payload = payload;
        this.type = payload.getClass().getSimpleName();
    }

    @Override
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
