package finos.traderx.positionservice.api.v2;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Cursor page on the v2 contract. nextCursor is null (present) on the terminal page. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PositionPageV2(List<PositionV2> items, String nextCursor) {
}
