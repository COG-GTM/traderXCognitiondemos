package finos.traderx.accountservice;

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
 * Provider contract test: live {@code GET /account/{id}} and {@code GET /accountuser/}
 * responses are validated against {@code account-service/openapi.yaml}.
 */
class AccountServiceOpenApiContractIT extends AbstractAccountServiceIT {

    @Autowired
    private TestRestTemplate rest;

    private OpenApiInteractionValidator validator() throws Exception {
        String spec = Files.readString(Path.of("openapi.yaml"));
        return OpenApiInteractionValidator.createFor(spec).build();
    }

    @Test
    void accountByIdResponseMatchesOpenApiContract() throws Exception {
        String body = rest.getForObject("/account/22214", String.class);

        SimpleResponse response = new SimpleResponse.Builder(200)
                .withContentType("application/json")
                .withBody(body)
                .build();

        ValidationReport report = validator()
                .validateResponse("/account/{id}", Request.Method.GET, response);
        assertThat(report.hasErrors())
                .withFailMessage("OpenAPI validation errors: %s", report.getMessages())
                .isFalse();
    }

    @Test
    void accountUserListResponseMatchesOpenApiContract() throws Exception {
        String body = rest.getForObject("/accountuser/", String.class);

        SimpleResponse response = new SimpleResponse.Builder(200)
                .withContentType("application/json")
                .withBody(body)
                .build();

        ValidationReport report = validator()
                .validateResponse("/accountuser/", Request.Method.GET, response);
        assertThat(report.hasErrors())
                .withFailMessage("OpenAPI validation errors: %s", report.getMessages())
                .isFalse();
    }
}
