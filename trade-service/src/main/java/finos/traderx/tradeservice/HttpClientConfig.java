package finos.traderx.tradeservice;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    /**
     * Timeouts are explicit because a hung validation service would otherwise leave a
     * submission pending indefinitely, with no decision reached and so nothing to record.
     * A bounded wait turns that into a VALIDATION_UNAVAILABLE record and a 503.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder,
            @Value("${lookup.connect.timeout.ms:2000}") long connectTimeoutMs,
            @Value("${lookup.read.timeout.ms:5000}") long readTimeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
