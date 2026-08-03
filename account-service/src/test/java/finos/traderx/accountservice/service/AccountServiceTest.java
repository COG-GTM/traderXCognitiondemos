package finos.traderx.accountservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import finos.traderx.accountservice.repository.AccountRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	@Mock
	AccountRepository accountRepository;

	@InjectMocks
	AccountService accountService;

	private Account account(int id, String displayName) {
		Account account = new Account();
		account.setId(id);
		account.setDisplayName(displayName);
		return account;
	}

	@Test
	@DisplayName("getAllAccount returns every account provided by the repository")
	void getAllAccountReturnsAllAccounts() {
		when(this.accountRepository.findAll())
				.thenReturn(Arrays.asList(account(1, "Trading Desk"), account(2, "Treasury")));

		List<Account> accounts = this.accountService.getAllAccount();

		assertEquals(2, accounts.size());
		assertEquals("Trading Desk", accounts.get(0).getDisplayName());
		assertEquals("Treasury", accounts.get(1).getDisplayName());
	}

	@Test
	@DisplayName("getAllAccount returns an empty list when no accounts exist")
	void getAllAccountReturnsEmptyListWhenRepositoryIsEmpty() {
		when(this.accountRepository.findAll()).thenReturn(Collections.emptyList());

		assertTrue(this.accountService.getAllAccount().isEmpty());
	}

	@Test
	@DisplayName("getAccountById returns the matching account")
	void getAccountByIdReturnsAccount() {
		when(this.accountRepository.findById(1)).thenReturn(Optional.of(account(1, "Trading Desk")));

		Account found = this.accountService.getAccountById(1);

		assertEquals(1, found.getId());
		assertEquals("Trading Desk", found.getDisplayName());
	}

	@Test
	@DisplayName("getAccountById throws ResourceNotFoundException for an unknown id")
	void getAccountByIdThrowsWhenAccountIsMissing() {
		when(this.accountRepository.findById(404)).thenReturn(Optional.empty());

		ResourceNotFoundException exception =
				assertThrows(ResourceNotFoundException.class, () -> this.accountService.getAccountById(404));

		assertTrue(exception.getMessage().contains("404"));
	}

	@Test
	@DisplayName("getAccountById throws ResourceNotFoundException for non-positive ids")
	void getAccountByIdThrowsForNonPositiveIds() {
		when(this.accountRepository.findById(0)).thenReturn(Optional.empty());
		when(this.accountRepository.findById(-1)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> this.accountService.getAccountById(0));
		assertThrows(ResourceNotFoundException.class, () -> this.accountService.getAccountById(-1));
		verify(this.accountRepository, never()).save(any(Account.class));
	}

	@Test
	@DisplayName("upsertAccount returns the persisted account")
	void upsertAccountReturnsSavedAccount() {
		Account toSave = account(0, "New Desk");
		Account saved = account(7, "New Desk");
		when(this.accountRepository.save(toSave)).thenReturn(saved);

		Account result = this.accountService.upsertAccount(toSave);

		assertSame(saved, result);
		verify(this.accountRepository).save(toSave);
	}

	@Test
	@DisplayName("upsertAccount passes an account with an empty display name through to the repository")
	void upsertAccountAcceptsEmptyDisplayName() {
		Account toSave = account(0, "");
		when(this.accountRepository.save(toSave)).thenReturn(toSave);

		assertEquals("", this.accountService.upsertAccount(toSave).getDisplayName());
	}

	@Test
	@DisplayName("upsertAccount passes an account with a null display name through to the repository")
	void upsertAccountAcceptsNullDisplayName() {
		Account toSave = account(0, null);
		when(this.accountRepository.save(toSave)).thenReturn(toSave);

		assertNull(this.accountService.upsertAccount(toSave).getDisplayName());
	}
}
