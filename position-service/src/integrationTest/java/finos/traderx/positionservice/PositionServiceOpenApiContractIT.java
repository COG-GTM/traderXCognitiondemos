package finos.traderx.positionservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;

/**
 * Provider contract test: live {@code GET /positions/{accountId}} and
 * {@code GET /trades/{accountId}} responses are validated against
 * {@code position-service/openapi.yaml}.
 */
class PositionServiceOpenApiContractIT extends AbstractPositionServiceIT {

    @Autowired
    private TestRestTemplate rest;

    private OpenApiInteractionValidator validator() throws Exception {
        String spec = Files.readString(Path.of("openapi.yaml"));
        return OpenApiInteractionValidator.createFor(spec).build();
    }

    @Test
    void positionsResponseMatchesOpenApiContract() throws Exception {
        String body = rest.getForObject("/positions/22214", String.class);

        SimpleResponse response = new SimpleResponse.Builder(200)
                .withContentType("application/json")
                .withBody(body)
                .build();

        ValidationReport report = validator()
                .validateResponse("/positions/{accountId}", Request.Method.GET, response);
        assertThat(report.hasErrors())
                .withFailMessage("OpenAPI validation errors: %s", report.getMessages())
                .isFalse();
    }

    @Test
    void tradesResponseMatchesOpenApiContract() throws Exception {
        String body = rest.getForObject("/trades/22214", String.class);

        SimpleResponse response = new SimpleResponse.Builder(200)
                .withContentType("application/json")
                .withBody(body)
                .build();

        ValidationReport report = validator()
                .validateResponse("/trades/{accountId}", Request.Method.GET, response);
        assertThat(report.hasErrors())
                .withFailMessage("OpenAPI validation errors: %s", report.getMessages())
                .isFalse();
    }
}
