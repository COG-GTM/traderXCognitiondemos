package finos.traderx.accountservice;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.client.TestRestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;

/**
 * S6: {@code account-service} -> {@code people-service}. Creating an account-user validates
 * the person against people-service (stubbed with WireMock). Known people are persisted;
 * unknown people yield 404 and no row is written.
 */
class AccountUserPeopleValidationIT extends AbstractAccountServiceIT {

    @Autowired
    private TestRestTemplate rest;

    private void stubPerson(String logonId, int status, String body) {
        stubFor(get(urlPathEqualTo("/People/GetPerson"))
                .withQueryParam("LogonId", equalTo(logonId))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    private ResponseEntity<String> postAccountUser(int accountId, String username) {
        String payload = "{\"accountId\":" + accountId + ",\"username\":\"" + username + "\"}";
        RequestEntity<String> request = RequestEntity.post(URI.create("/accountuser/"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload);
        return rest.exchange(request, String.class);
    }

    @Test
    void knownPersonAllowsAccountUserCreationAndIsPersisted() {
        stubPerson("user02", 200,
                "{\"logonId\":\"user02\",\"fullName\":\"User Two\",\"email\":\"user2@traderx.test\","
                        + "\"department\":\"Trading\",\"photoUrl\":\"\"}");

        ResponseEntity<String> response = postAccountUser(22214, "user02");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(getRequestedFor(urlPathEqualTo("/People/GetPerson"))
                .withQueryParam("LogonId", equalTo("user02")));

        JsonNode all = rest.getForObject("/accountuser/", JsonNode.class);
        assertThat(containsUser(all, 22214, "user02")).isTrue();
    }

    @Test
    void unknownPersonReturns404AndPersistsNothing() {
        stubPerson("ghost", 404, "");

        ResponseEntity<String> response = postAccountUser(22214, "ghost");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode all = rest.getForObject("/accountuser/", JsonNode.class);
        assertThat(containsUser(all, 22214, "ghost")).isFalse();
    }

    private boolean containsUser(JsonNode array, int accountId, String username) {
        for (JsonNode node : array) {
            if (node.get("accountId").asInt() == accountId
                    && username.equals(node.get("username").asText())) {
                return true;
            }
        }
        return false;
    }
}
