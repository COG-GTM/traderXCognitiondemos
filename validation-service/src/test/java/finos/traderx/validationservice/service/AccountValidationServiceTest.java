package finos.traderx.validationservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import finos.traderx.validationservice.model.Account;

@ExtendWith(MockitoExtension.class)
class AccountValidationServiceTest {

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private AccountValidationService accountValidationService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(accountValidationService, "accountServiceAddress", "http://localhost:18088");
	}

	@Test
	void validateAccount_validAccount_returnsTrue() {
		Account account = new Account(12345, "Test Account");
		ResponseEntity<Account> response = new ResponseEntity<>(account, HttpStatus.OK);
		when(restTemplate.getForEntity("http://localhost:18088//account/12345", Account.class))
				.thenReturn(response);

		boolean result = accountValidationService.validateAccount(12345);

		assertTrue(result);
	}

	@Test
	void validateAccount_invalidAccount_returnsFalse() {
		when(restTemplate.getForEntity("http://localhost:18088//account/99999", Account.class))
				.thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

		boolean result = accountValidationService.validateAccount(99999);

		assertFalse(result);
	}
}
