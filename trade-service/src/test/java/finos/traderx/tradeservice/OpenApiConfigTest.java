package finos.traderx.tradeservice;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

class OpenApiConfigTest {

    @Test
    void config_ReturnsNonNullOpenAPI() {
        OpenApiConfig openApiConfig = new OpenApiConfig();
        ReflectionTestUtils.setField(openApiConfig, "port", 18092);

        OpenAPI api = openApiConfig.config();

        assertNotNull(api);
    }

    @Test
    void config_HasCorrectTitle() {
        OpenApiConfig openApiConfig = new OpenApiConfig();
        ReflectionTestUtils.setField(openApiConfig, "port", 18092);

        OpenAPI api = openApiConfig.config();
        Info info = api.getInfo();

        assertNotNull(info);
        assertEquals("FINOS TraderX Trading Service", info.getTitle());
    }

    @Test
    void config_HasCorrectVersion() {
        OpenApiConfig openApiConfig = new OpenApiConfig();
        ReflectionTestUtils.setField(openApiConfig, "port", 18092);

        OpenAPI api = openApiConfig.config();
        Info info = api.getInfo();

        assertNotNull(info);
        assertEquals("0.1.0", info.getVersion());
    }

    @Test
    void config_HasDescription() {
        OpenApiConfig openApiConfig = new OpenApiConfig();
        ReflectionTestUtils.setField(openApiConfig, "port", 18092);

        OpenAPI api = openApiConfig.config();
        Info info = api.getInfo();

        assertNotNull(info);
        assertNotNull(info.getDescription());
        assertTrue(info.getDescription().contains("trade"));
    }

    @Test
    void config_HasServers() {
        OpenApiConfig openApiConfig = new OpenApiConfig();
        ReflectionTestUtils.setField(openApiConfig, "port", 18092);

        OpenAPI api = openApiConfig.config();

        assertNotNull(api.getServers());
        assertFalse(api.getServers().isEmpty());
    }

    @Test
    void config_HasLocalDevServer() {
        OpenApiConfig openApiConfig = new OpenApiConfig();
        ReflectionTestUtils.setField(openApiConfig, "port", 18092);

        OpenAPI api = openApiConfig.config();

        boolean hasLocalServer = api.getServers().stream()
                .anyMatch(server -> server.getUrl().contains("localhost:18092"));

        assertTrue(hasLocalServer);
    }

    @Test
    void config_HasEmptyUrlServer() {
        OpenApiConfig openApiConfig = new OpenApiConfig();
        ReflectionTestUtils.setField(openApiConfig, "port", 18092);

        OpenAPI api = openApiConfig.config();

        boolean hasEmptyUrlServer = api.getServers().stream()
                .anyMatch(server -> server.getUrl().isEmpty());

        assertTrue(hasEmptyUrlServer);
    }

    @Test
    void config_WithDifferentPort_UsesCorrectPort() {
        OpenApiConfig openApiConfig = new OpenApiConfig();
        ReflectionTestUtils.setField(openApiConfig, "port", 9999);

        OpenAPI api = openApiConfig.config();

        boolean hasCorrectPort = api.getServers().stream()
                .anyMatch(server -> server.getUrl().contains("localhost:9999"));

        assertTrue(hasCorrectPort);
    }
}
