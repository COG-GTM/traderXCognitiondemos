package finos.traderx.pnlservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pnlServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FINOS TraderX P&L Service")
                        .description("Service for calculating and retrieving profit and loss data")
                        .version("0.1.0"));
    }
}
