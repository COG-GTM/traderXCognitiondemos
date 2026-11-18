package finos.traderx.positionservice.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import com.fasterxml.jackson.databind.ObjectMapper;

import finos.traderx.positionservice.model.Trade;

@JsonTest
class UtcDateSerializerJsonTest {

	@Autowired
	private JacksonTester<Trade> json;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void bootObjectMapperUsesUtcDateSerializer() throws Exception {
		assertThat(objectMapper.getSerializerProviderInstance().findValueSerializer(Date.class))
				.isInstanceOf(UtcDateSerializer.class);
	}

	@Test
	void bootObjectMapperWritesTradeDatesInStdDateFormatShape() throws Exception {
		Trade trade = new Trade();
		trade.setId("TRADE-22214-AABBCC");
		trade.setAccountId(22214);
		trade.setCreated(new Timestamp(1_763_503_666_752L));
		trade.setUpdated(new Timestamp(1_763_503_666_005L));
		assertThat(json.write(trade)).extractingJsonPathStringValue("$.created").isEqualTo("2025-11-18T22:07:46.752+00:00");
		assertThat(json.write(trade)).extractingJsonPathStringValue("$.updated").isEqualTo("2025-11-18T22:07:46.005+00:00");
	}
}
