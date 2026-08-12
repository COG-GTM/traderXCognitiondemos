package finos.traderx.positionservice.audit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuditQueryProperties.class)
public class AuditConfig {
}
