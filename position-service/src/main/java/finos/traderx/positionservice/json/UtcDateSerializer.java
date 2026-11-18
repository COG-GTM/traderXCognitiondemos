package finos.traderx.positionservice.json;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Writes java.util.Date values in the exact form of Jackson's default StdDateFormat
 * ("yyyy-MM-dd'T'HH:mm:ss.SSS+00:00", UTC) without its per-value GregorianCalendar work.
 * Any other configuration (timestamps, custom DateFormat or time zone, @JsonFormat,
 * dates outside 1582-10-15..9999-12-31) is delegated to Jackson's own DateSerializer.
 */
@JsonComponent
public class UtcDateSerializer extends StdSerializer<Date> implements ContextualSerializer {

	private static final long serialVersionUID = 1L;

	// 1582-10-15T00:00:00Z: GregorianCalendar switches to the Julian calendar before this.
	static final long MIN_FAST_MILLIS = -12219292800000L;
	// 10000-01-01T00:00:00Z: StdDateFormat stops zero-padding the year to four digits after this.
	static final long MAX_FAST_MILLIS_EXCLUSIVE = 253402300800000L;

	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	public UtcDateSerializer() {
		super(Date.class);
	}

	@Override
	public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
			throws JsonMappingException {
		JsonFormat.Value format = findFormatOverrides(prov, property, handledType());
		if (format != null && (format.hasPattern() || format.hasShape() || format.hasLocale()
				|| format.hasTimeZone() || format.hasLenient())) {
			return DateSerializer.instance.createContextual(prov, property);
		}
		return this;
	}

	@Override
	public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		long millis = value.getTime();
		if (millis < MIN_FAST_MILLIS || millis >= MAX_FAST_MILLIS_EXCLUSIVE || !usesDefaultIsoFormat(provider)) {
			DateSerializer.instance.serialize(value, gen, provider);
			return;
		}
		gen.writeString(formatUtc(millis));
	}

	private static boolean usesDefaultIsoFormat(SerializerProvider provider) {
		if (provider.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)) {
			return false;
		}
		if (provider.getConfig().getDateFormat().getClass() != StdDateFormat.class) {
			return false;
		}
		StdDateFormat df = (StdDateFormat) provider.getConfig().getDateFormat();
		TimeZone tz = df.getTimeZone();
		return df.isColonIncludedInTimeZone()
				&& (tz == null || tz.hasSameRules(UTC))
				&& provider.getConfig().getTimeZone().hasSameRules(UTC);
	}

	static String formatUtc(long millis) {
		LocalDateTime t = LocalDateTime.ofEpochSecond(Math.floorDiv(millis, 1000L),
				(int) Math.floorMod(millis, 1000L) * 1_000_000, ZoneOffset.UTC);
		char[] c = new char[29];
		put(c, 0, t.getYear(), 4);
		c[4] = '-';
		put(c, 5, t.getMonthValue(), 2);
		c[7] = '-';
		put(c, 8, t.getDayOfMonth(), 2);
		c[10] = 'T';
		put(c, 11, t.getHour(), 2);
		c[13] = ':';
		put(c, 14, t.getMinute(), 2);
		c[16] = ':';
		put(c, 17, t.getSecond(), 2);
		c[19] = '.';
		put(c, 20, t.getNano() / 1_000_000, 3);
		c[23] = '+';
		c[24] = '0';
		c[25] = '0';
		c[26] = ':';
		c[27] = '0';
		c[28] = '0';
		return new String(c);
	}

	private static void put(char[] c, int offset, int value, int width) {
		for (int i = offset + width - 1; i >= offset; i--) {
			c[i] = (char) ('0' + value % 10);
			value /= 10;
		}
	}
}
