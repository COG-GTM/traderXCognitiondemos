package finos.traderx.tradeservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class TradeServiceApplication {

	private static final Logger log = LoggerFactory.getLogger(TradeServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(TradeServiceApplication.class, args);
		log.info("TradeServiceApplication started with OpenTelemetry tracing enabled");
	}

}
