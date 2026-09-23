package finos.traderx.positionservice.techfest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Loads the approved fixtures shared with the React consumer tests. Tests run with cwd = position-service/. */
final class Fixtures {

	static final int ACCOUNT = 77007;
	static final Path DIR = Path.of("techfest-migration", "fixtures");
	static final ObjectMapper MAPPER = new ObjectMapper();

	private Fixtures() {
	}

	static JsonNode load(String name) throws IOException {
		return MAPPER.readTree(Files.readString(DIR.resolve(name)));
	}
}
