package finos.traderx.positionservice.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import finos.traderx.positionservice.model.Trade;

class UtcDateSerializerTest {

	/** Jackson as Spring Boot configures it by default, without the serializer. */
	private static ObjectMapper stock() {
		return new ObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	private static ObjectMapper withSerializer(ObjectMapper mapper) {
		return mapper.registerModule(new SimpleModule().addSerializer(Date.class, new UtcDateSerializer()));
	}

	private static List<Date> sampleDates() {
		List<Date> dates = new ArrayList<>();
		long[] edges = { 0L, -1L, 1L, 999L, -999L, -1000L, -1001L, System.currentTimeMillis(),
				UtcDateSerializer.MIN_FAST_MILLIS - 1, UtcDateSerializer.MIN_FAST_MILLIS,
				UtcDateSerializer.MAX_FAST_MILLIS_EXCLUSIVE - 1, UtcDateSerializer.MAX_FAST_MILLIS_EXCLUSIVE,
				-62135596800000L, -62135596800001L, 951782400000L /* 2000-02-29 */, 4107542399999L /* 2100-02-28T23:59:59.999 */ };
		for (long e : edges) {
			dates.add(new Date(e));
		}
		Random random = new Random(55);
		for (int i = 0; i < 200_000; i++) {
			long millis = random.nextLong() % 300_000_000_000_000L;
			dates.add(i % 2 == 0 ? new Date(millis) : new Timestamp(millis));
		}
		return dates;
	}

	@Test
	void writesSameJsonAsJacksonDefaultForEveryDate() throws Exception {
		ObjectMapper expected = stock();
		ObjectMapper actual = withSerializer(stock());
		for (Date d : sampleDates()) {
			assertEquals(expected.writeValueAsString(d), actual.writeValueAsString(d), "millis=" + d.getTime());
		}
	}

	@Test
	void writesSameJsonForTradeList() throws Exception {
		List<Trade> trades = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			Trade t = new Trade();
			t.setId("TRADE-" + i);
			t.setAccountId(22214);
			t.setSecurity("IBM");
			t.setSide(i % 2 == 0 ? "Buy" : "Sell");
			t.setQuantity(i + 1);
			t.setState("Settled");
			t.setCreated(new Timestamp(1_700_000_000_000L + i * 1_234L));
			t.setUpdated(i % 3 == 0 ? null : new Date(1_700_000_000_000L + i * 4_321L));
			trades.add(t);
		}
		assertEquals(stock().writeValueAsString(trades), withSerializer(stock()).writeValueAsString(trades));
	}

	@Test
	void delegatesForNonDefaultMapperConfiguration() throws Exception {
		Date d = new Date(1_700_000_000_123L);
		ObjectMapper timestamps = new ObjectMapper();
		assertEquals(timestamps.writeValueAsString(d), withSerializer(new ObjectMapper()).writeValueAsString(d));

		ObjectMapper zoned = stock().setTimeZone(TimeZone.getTimeZone("America/Chicago"));
		assertEquals(zoned.writeValueAsString(d),
				withSerializer(stock().setTimeZone(TimeZone.getTimeZone("America/Chicago"))).writeValueAsString(d));

		ObjectMapper custom = stock().setDateFormat(new SimpleDateFormat("dd/MM/yyyy"));
		assertEquals(custom.writeValueAsString(d),
				withSerializer(stock().setDateFormat(new SimpleDateFormat("dd/MM/yyyy"))).writeValueAsString(d));
	}

	static class Annotated {
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm")
		public Date patterned = new Date(1_700_000_000_123L);
		@JsonFormat(shape = JsonFormat.Shape.NUMBER)
		public Date numeric = new Date(1_700_000_000_123L);
		public Date plain = new Date(1_700_000_000_123L);
	}

	@Test
	void honoursJsonFormatAnnotations() throws Exception {
		assertEquals(stock().writeValueAsString(new Annotated()), withSerializer(stock()).writeValueAsString(new Annotated()));
	}
}
