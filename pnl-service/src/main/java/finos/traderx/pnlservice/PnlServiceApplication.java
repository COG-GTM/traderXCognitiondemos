package finos.traderx.pnlservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"finos.traderx.pnlservice", "finos.traderx.messaging"})
public class PnlServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PnlServiceApplication.class, args);
	}

}
