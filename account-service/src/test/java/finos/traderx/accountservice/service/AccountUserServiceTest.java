package finos.traderx.accountservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.repository.AccountRepository;
import finos.traderx.accountservice.repository.AccountUserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountUserServiceTest {

	@Mock
	AccountUserRepository accountUserRepository;

	@Mock
	AccountRepository accountRepository;

	@InjectMocks
	AccountUserService accountUserService;

	private AccountUser accountUser(Integer accountId, String username) {
		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(accountId);
		accountUser.setUsername(username);
		return accountUser;
	}

	@Test
	@DisplayName("getAllAccountUsers returns every account user provided by the repository")
	void getAllAccountUsersReturnsAllUsers() {
		when(this.accountUserRepository.findAll())
				.thenReturn(Arrays.asList(accountUser(1, "trader1"), accountUser(1, "trader2")));

		List<AccountUser> accountUsers = this.accountUserService.getAllAccountUsers();

		assertEquals(2, accountUsers.size());
		assertEquals("trader1", accountUsers.get(0).getUsername());
	}

	@Test
	@DisplayName("getAllAccountUsers returns an empty list when no account users exist")
	void getAllAccountUsersReturnsEmptyList() {
		when(this.accountUserRepository.findAll()).thenReturn(Collections.emptyList());

		assertTrue(this.accountUserService.getAllAccountUsers().isEmpty());
	}

	@Test
	@DisplayName("getAccountUserById returns the matching account user")
	void getAccountUserByIdReturnsAccountUser() {
		when(this.accountUserRepository.findById(1)).thenReturn(Optional.of(accountUser(1, "trader1")));

		assertEquals("trader1", this.accountUserService.getAccountUserById(1).getUsername());
	}

	@Test
	@DisplayName("getAccountUserById throws ResourceNotFoundException for an unknown id")
	void getAccountUserByIdThrowsWhenMissing() {
		when(this.accountUserRepository.findById(99)).thenReturn(Optional.empty());

		ResourceNotFoundException exception =
				assertThrows(ResourceNotFoundException.class, () -> this.accountUserService.getAccountUserById(99));

		assertTrue(exception.getMessage().contains("99"));
	}

	@Test
	@DisplayName("upsertAccountUser saves the account user when the referenced account exists")
	void upsertAccountUserSavesWhenAccountExists() {
		AccountUser toSave = accountUser(1, "trader1");
		when(this.accountRepository.findById(1)).thenReturn(Optional.of(new Account()));
		when(this.accountUserRepository.save(toSave)).thenReturn(toSave);

		assertSame(toSave, this.accountUserService.upsertAccountUser(toSave));
		verify(this.accountUserRepository).save(toSave);
	}

	@Test
	@DisplayName("upsertAccountUser rejects an account user pointing at a missing account")
	void upsertAccountUserThrowsWhenAccountIsMissing() {
		AccountUser toSave = accountUser(42, "trader1");
		when(this.accountRepository.findById(42)).thenReturn(Optional.empty());

		ResourceNotFoundException exception =
				assertThrows(ResourceNotFoundException.class, () -> this.accountUserService.upsertAccountUser(toSave));

		assertTrue(exception.getMessage().contains("42"));
		verify(this.accountUserRepository, never()).save(any(AccountUser.class));
	}

	@Test
	@DisplayName("upsertAccountUser rejects an account user with a null account id")
	void upsertAccountUserThrowsWhenAccountIdIsNull() {
		AccountUser toSave = accountUser(null, "trader1");
		when(this.accountRepository.findById(null)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> this.accountUserService.upsertAccountUser(toSave));
		verify(this.accountUserRepository, never()).save(any(AccountUser.class));
	}

	@Test
	@DisplayName("upsertAccountUser saves an account user with an empty username when the account exists")
	void upsertAccountUserAcceptsEmptyUsername() {
		AccountUser toSave = accountUser(1, "");
		when(this.accountRepository.findById(1)).thenReturn(Optional.of(new Account()));
		when(this.accountUserRepository.save(toSave)).thenReturn(toSave);

		assertEquals("", this.accountUserService.upsertAccountUser(toSave).getUsername());
	}
}
