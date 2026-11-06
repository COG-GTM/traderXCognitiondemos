package finos.traderx.accountservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.repository.AccountRepository;
import finos.traderx.accountservice.repository.AccountUserRepository;
import finos.traderx.accountservice.service.AccountUserService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Runs the account-user endpoints against a stub People service that mimics
 * DirectoryService.GetPerson: LogonId wins when present, otherwise EmployeeId is used.
 */
@WebMvcTest(AccountUserController.class)
@Import(AccountUserService.class)
class AccountUserControllerTests {

	private static final int ACCOUNT_ID = 22214;
	private static final Map<String, String> EMPLOYEE_TO_LOGON = Map.of("E1", "user01", "E2", "user02");
	private static final List<Map<String, String>> peopleRequests = new CopyOnWriteArrayList<>();
	private static final HttpServer peopleService = startPeopleService();

	@Autowired
	MockMvc mockMvc;

	@MockBean
	AccountRepository accountRepository;

	@MockBean
	AccountUserRepository accountUserRepository;

	@DynamicPropertySource
	static void peopleServiceUrl(DynamicPropertyRegistry registry) {
		registry.add("people.service.url", () -> "http://localhost:" + peopleService.getAddress().getPort());
	}

	@AfterAll
	static void stopPeopleService() {
		peopleService.stop(0);
	}

	@BeforeEach
	void setUp() {
		peopleRequests.clear();
		when(this.accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(new Account()));
		when(this.accountUserRepository.save(any(AccountUser.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	@Test
	void putRejectsUsernameUnknownToPeopleService() throws Exception {
		this.mockMvc.perform(put("/accountuser/").contentType(MediaType.APPLICATION_JSON).content(body("mallory")))
				.andExpect(status().isNotFound());
		verify(this.accountUserRepository, never()).save(any());
	}

	@Test
	void putAcceptsKnownPerson() throws Exception {
		this.mockMvc.perform(put("/accountuser/").contentType(MediaType.APPLICATION_JSON).content(body("user02")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("user02"));
		verify(this.accountUserRepository).save(any());
	}

	@Test
	void postAcceptsKnownPerson() throws Exception {
		this.mockMvc.perform(post("/accountuser/").contentType(MediaType.APPLICATION_JSON).content(body("user01")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("user01"));
		assertEquals(List.of(Map.of("LogonId", "user01")), peopleRequests);
	}

	@Test
	void postCannotInjectEmployeeIdIntoPeopleQuery() throws Exception {
		this.mockMvc.perform(post("/accountuser/").contentType(MediaType.APPLICATION_JSON).content(body("x&EmployeeId=E1")))
				.andExpect(status().isNotFound());
		assertEquals(List.of(Map.of("LogonId", "x&EmployeeId=E1")), peopleRequests);
		verify(this.accountUserRepository, never()).save(any());
	}

	@Test
	void putCannotInjectEmployeeIdIntoPeopleQuery() throws Exception {
		this.mockMvc.perform(put("/accountuser/").contentType(MediaType.APPLICATION_JSON).content(body("x&EmployeeId=E1")))
				.andExpect(status().isNotFound());
		verify(this.accountUserRepository, never()).save(any());
	}

	@Test
	void postCannotTruncatePeopleQueryWithFragment() throws Exception {
		this.mockMvc.perform(post("/accountuser/").contentType(MediaType.APPLICATION_JSON).content(body("user01#x")))
				.andExpect(status().isNotFound());
		assertEquals(List.of(Map.of("LogonId", "user01#x")), peopleRequests);
		verify(this.accountUserRepository, never()).save(any());
	}

	private static String body(String username) {
		return "{\"accountId\":" + ACCOUNT_ID + ",\"username\":\"" + username + "\"}";
	}

	private static HttpServer startPeopleService() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/People/GetPerson", AccountUserControllerTests::getPerson);
			server.start();
			return server;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void getPerson(HttpExchange exchange) throws IOException {
		Map<String, String> params = new HashMap<>();
		String rawQuery = exchange.getRequestURI().getRawQuery();
		if (rawQuery != null) {
			for (String pair : rawQuery.split("&")) {
				String[] kv = pair.split("=", 2);
				params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
						kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
			}
		}
		peopleRequests.add(params);

		String logonId = params.getOrDefault("LogonId", "");
		String match = !logonId.isEmpty()
				? (EMPLOYEE_TO_LOGON.containsValue(logonId) ? logonId : null)
				: EMPLOYEE_TO_LOGON.get(params.get("EmployeeId"));

		if (match == null) {
			exchange.sendResponseHeaders(404, -1);
		}
		else {
			byte[] json = ("{\"logonId\":\"" + match + "\",\"fullName\":\"" + match + "\"}").getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, json.length);
			exchange.getResponseBody().write(json);
		}
		exchange.close();
	}
}
