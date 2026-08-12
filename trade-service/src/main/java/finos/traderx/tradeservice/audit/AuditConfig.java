package finos.traderx.tradeservice.audit;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BestExecutionAuditProperties.class)
public class AuditConfig {

    /** UTC, because the retained record has to be in UTC regardless of where the JVM runs. */
    @Bean
    public Clock auditClock() {
        return Clock.systemUTC();
    }
}
