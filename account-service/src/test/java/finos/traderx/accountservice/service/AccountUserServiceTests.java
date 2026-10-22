package finos.traderx.accountservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import finos.traderx.accountservice.exceptions.AccountUserConflictException;
import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.repository.AccountRepository;
import finos.traderx.accountservice.repository.AccountUserRepository;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountUserServiceTests {

	@Mock
	AccountUserRepository accountUserRepository;

	@Mock
	AccountRepository accountRepository;

	@Mock
	EntityManager entityManager;

	@InjectMocks
	AccountUserService accountUserService;

	@BeforeEach
	void saveReturnsArgument() {
		org.mockito.Mockito.lenient().when(this.accountUserRepository.save(any(AccountUser.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void createRejectsExistingMapping() {
		when(this.accountRepository.findById(22214)).thenReturn(Optional.of(new Account()));
		when(this.accountUserRepository.existsByAccountIdAndUsername(22214, "user01")).thenReturn(true);

		assertThrows(AccountUserConflictException.class, () -> this.accountUserService.createAccountUser(22214, "user01"));
		verify(this.entityManager, never()).persist(any());
	}

	@Test
	void createTranslatesConcurrentInsertToConflict() {
		when(this.accountRepository.findById(22214)).thenReturn(Optional.of(new Account()));
		when(this.accountUserRepository.existsByAccountIdAndUsername(22214, "user01")).thenReturn(false);
		doThrow(new ConstraintViolationException("duplicate key", new SQLException("duplicate key"), "PRIMARY_KEY"))
				.when(this.entityManager).flush();

		assertThrows(AccountUserConflictException.class, () -> this.accountUserService.createAccountUser(22214, "user01"));
	}

	@Test
	void createSavesFreshEntity() {
		when(this.accountRepository.findById(22214)).thenReturn(Optional.of(new Account()));
		when(this.accountUserRepository.existsByAccountIdAndUsername(22214, "user01")).thenReturn(false);

		AccountUser saved = this.accountUserService.createAccountUser(22214, "user01");
		assertEquals(22214, saved.getAccountId());
		assertEquals("user01", saved.getUsername());
		verify(this.entityManager).persist(saved);
		verify(this.entityManager).flush();
	}

	@Test
	void unknownAccountIsNotFound() {
		when(this.accountRepository.findById(1)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> this.accountUserService.upsertAccountUser(1, "user01"));
		assertThrows(ResourceNotFoundException.class, () -> this.accountUserService.createAccountUser(1, "user01"));
		verify(this.accountUserRepository, never()).save(any());
		verify(this.entityManager, never()).persist(any());
	}
}
