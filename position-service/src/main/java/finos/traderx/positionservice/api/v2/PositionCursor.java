package finos.traderx.positionservice.api.v2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Opaque cursor: URL-safe base64 of the last security on the previous page. Order is SECURITY ascending. */
public final class PositionCursor {

	private PositionCursor() {
	}

	public static String encode(String lastSecurity) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(lastSecurity.getBytes(StandardCharsets.UTF_8));
	}

	public static String decode(String cursor) {
		try {
			String s = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
			if (s.isEmpty()) {
				throw new IllegalArgumentException("empty cursor");
			}
			return s;
		} catch (IllegalArgumentException e) {
			throw new InvalidCursorException(cursor);
		}
	}

	public static class InvalidCursorException extends RuntimeException {
		public InvalidCursorException(String cursor) {
			super("Invalid cursor: " + cursor);
		}
	}
}
