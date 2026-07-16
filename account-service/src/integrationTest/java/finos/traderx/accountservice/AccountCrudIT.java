package finos.traderx.accountservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * S7: database -> {@code account-service}. Account CRUD round-trips against real H2, the
 * seeded data is served, unknown accounts yield 404, and the account-user upsert enforces
 * the account foreign-key relationship. None of these paths touch people-service.
 */
class AccountCrudIT extends AbstractAccountServiceIT {

    @Autowired
    private TestRestTemplate rest;

    private ResponseEntity<String> postJson(String path, String body) {
        return rest.exchange(RequestEntity.post(URI.create(path))
                .contentType(MediaType.APPLICATION_JSON).body(body), String.class);
    }

    private ResponseEntity<String> putJson(String path, String body) {
        return rest.exchange(RequestEntity.put(URI.create(path))
                .contentType(MediaType.APPLICATION_JSON).body(body), String.class);
    }

    @Test
    void createdAccountCanBeReadBack() {
        String payload = "{\"displayName\":\"IT Created Account\"}";

        JsonNode created = rest.exchange(RequestEntity.post(URI.create("/account/"))
                .contentType(MediaType.APPLICATION_JSON).body(payload), JsonNode.class).getBody();
        int newId = created.get("id").asInt();
        assertThat(newId).isPositive();

        JsonNode fetched = rest.getForObject("/account/" + newId, JsonNode.class);
        assertThat(fetched.get("id").asInt()).isEqualTo(newId);
        assertThat(fetched.get("displayName").asText()).isEqualTo("IT Created Account");
    }

    @Test
    void seededAccountsAreListed() {
        JsonNode all = rest.getForObject("/account/", JsonNode.class);
        assertThat(all.isArray()).isTrue();

        boolean found = false;
        for (JsonNode node : all) {
            if (node.get("id").asInt() == 22214) {
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void unknownAccountReturns404() {
        ResponseEntity<String> response = rest.getForEntity("/account/999999", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void accountUserUpsertRequiresExistingAccount() {
        // PUT does not validate the person, but the service enforces that the account exists.
        ResponseEntity<String> ok = putJson("/accountuser/",
                "{\"accountId\":22214,\"username\":\"crud_user\"}");
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode all = rest.getForObject("/accountuser/", JsonNode.class);
        boolean persisted = false;
        for (JsonNode node : all) {
            if (node.get("accountId").asInt() == 22214 && "crud_user".equals(node.get("username").asText())) {
                persisted = true;
            }
        }
        assertThat(persisted).isTrue();

        ResponseEntity<String> missing = putJson("/accountuser/",
                "{\"accountId\":999999,\"username\":\"orphan\"}");
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
