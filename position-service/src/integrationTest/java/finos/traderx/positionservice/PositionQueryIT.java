package finos.traderx.positionservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * S3: H2 database -> {@code position-service}. Boots the real service against an H2 database
 * seeded from the canonical schema and asserts the REST read endpoints return the seeded
 * blotter data (and an empty array for unknown accounts).
 */
class PositionQueryIT extends AbstractPositionServiceIT {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void positionsForSeededAccountAreReturned() {
        JsonNode body = rest.getForObject("/positions/22214", JsonNode.class);

        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(3);
        assertThat(quantityFor(body, "MS")).isEqualTo(1000);
        assertThat(quantityFor(body, "IBM")).isEqualTo(-100);
        assertThat(quantityFor(body, "C")).isEqualTo(-2000);
    }

    @Test
    void tradesForSeededAccountAreReturned() {
        JsonNode body = rest.getForObject("/trades/22214", JsonNode.class);

        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(3);
        body.forEach(t -> {
            assertThat(t.get("accountId").asInt()).isEqualTo(22214);
            assertThat(t.get("state").asText()).isEqualTo("Settled");
        });
    }

    @Test
    void unknownAccountReturnsEmptyArrays() {
        JsonNode positions = rest.getForObject("/positions/99999", JsonNode.class);
        JsonNode trades = rest.getForObject("/trades/99999", JsonNode.class);

        assertThat(positions.isArray()).isTrue();
        assertThat(positions).isEmpty();
        assertThat(trades.isArray()).isTrue();
        assertThat(trades).isEmpty();
    }

    private int quantityFor(JsonNode array, String security) {
        for (JsonNode node : array) {
            if (security.equals(node.get("security").asText())) {
                return node.get("quantity").asInt();
            }
        }
        throw new AssertionError("No position for security " + security);
    }
}
