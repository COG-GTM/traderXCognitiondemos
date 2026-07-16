package finos.traderx.tradeservice;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;

/**
 * Provider contract test: the live {@code POST /trade/} response is validated against
 * {@code trade-service/openapi.yaml} so the service can never drift from its own
 * published contract.
 */
class TradeServiceOpenApiContractIT extends AbstractTradeServiceIT {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void postTradeResponseMatchesOpenApiContract() throws Exception {
        stubFor(get(urlEqualTo("/stocks/IBM"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"ticker\":\"IBM\",\"companyName\":\"IBM\"}")));
        stubFor(get(urlEqualTo("/account/22214"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":22214,\"displayName\":\"Test Account 20\"}")));

        RequestEntity<String> request = RequestEntity
                .post(UriComponentsBuilder.fromPath("/trade/").build().toUri())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"id\":\"ORD-1\",\"security\":\"IBM\",\"quantity\":100,\"accountId\":22214,\"side\":\"Buy\"}");
        ResponseEntity<String> response = rest.exchange(request, String.class);

        String spec = Files.readString(Path.of("openapi.yaml"));
        OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(spec).build();

        SimpleResponse validatorResponse = new SimpleResponse.Builder(response.getStatusCode().value())
                .withContentType("application/json")
                .withBody(response.getBody())
                .build();

        ValidationReport report = validator.validateResponse("/trade/", Request.Method.POST, validatorResponse);
        assertThat(report.hasErrors())
                .withFailMessage("OpenAPI validation errors: %s", report.getMessages())
                .isFalse();
    }
}
