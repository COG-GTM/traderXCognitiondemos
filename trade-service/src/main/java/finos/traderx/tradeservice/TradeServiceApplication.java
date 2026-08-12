package finos.traderx.tradeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import finos.traderx.tradeservice.risk.RiskLimitProperties;

@SpringBootApplication
@EnableConfigurationProperties(RiskLimitProperties.class)
public class TradeServiceApplication {

	
	public static void main(String[] args) {
		SpringApplication.run(TradeServiceApplication.class, args);
	}

}
