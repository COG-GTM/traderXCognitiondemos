package finos.traderx.tradeprocessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.Test;

class PubSubConfigTest {

    @Test
    void trustedPublishersAreTrimmed() {
        assertEquals(Set.of("trade-service"), PubSubConfig.requireTrustedPublishers(Set.of(" trade-service ", "")));
    }

    @Test
    void emptyTrustedPublishersFailClosed() {
        assertThrows(IllegalStateException.class, () -> PubSubConfig.requireTrustedPublishers(null));
        assertThrows(IllegalStateException.class, () -> PubSubConfig.requireTrustedPublishers(Set.of()));
        assertThrows(IllegalStateException.class, () -> PubSubConfig.requireTrustedPublishers(Set.of(" ", "")));
    }
}
