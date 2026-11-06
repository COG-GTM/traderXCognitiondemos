package finos.traderx.tradeservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "MAX_HTTP_REQUEST_HEADER_SIZE=64KB")
class RequestHeaderSizeLimitTest {

	@LocalServerPort
	int port;

	private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

	private int statusWithHeaderOfSize(int bytes) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/"))
				.header("X-Padding", "a".repeat(bytes)).GET().build();
		return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
	}

	@Test
	void rejectsHeadersLargerThanLimit() throws Exception {
		assertEquals(400, statusWithHeaderOfSize(100_000));
	}

	@Test
	void acceptsOidcSizedHeaders() throws Exception {
		assertNotEquals(400, statusWithHeaderOfSize(16_000));
	}
}
