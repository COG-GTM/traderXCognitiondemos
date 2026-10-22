package finos.traderx.accountservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Value("${server.port}")
    private int port = 18088;

    @Bean
    public OpenAPI config() {
        Info info = new Info()
                .title("FINOS TraderX Account Service")
                .version("0.1.0")
                .description("Service for retrieving account data, and for managing accounts");

        OpenAPI api = new OpenAPI()
                .addServersItem(serverInfo("", "Empty URL to help proxied documentation work"))
                .addServersItem(serverInfo("http://localhost:" + port, "Local Dev URL"))
                .components(new Components().addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("GET requires ACCOUNT_READER or ACCOUNT_ADMIN; POST/PUT require ACCOUNT_ADMIN.")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));

        api.setInfo(info);
        return api;
    }

    private Server serverInfo(String url, String desc) {
        return new Server()
                .description(desc)
                .url(url);
    }
}