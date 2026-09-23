package finos.traderx.positionservice.api.v2;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonInclude;

import finos.traderx.positionservice.model.Position;

/**
 * One position on the v2 contract. currency is the position's valuation currency and is always present;
 * marketValue is explicitly null when unknown, never omitted.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PositionV2(Integer accountId, String security, Integer quantity, String asOf, String currency,
		MoneyV2 marketValue) {

	public static PositionV2 from(Position p) {
		String asOf = p.getUpdated() == null ? null
				: DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(p.getUpdated().getTime()));
		return new PositionV2(p.getAccountId(), p.getSecurity(), p.getQuantity(), asOf, p.getCurrency(),
				MoneyV2.of(p.getMarketValue(), p.getCurrency()));
	}
}
