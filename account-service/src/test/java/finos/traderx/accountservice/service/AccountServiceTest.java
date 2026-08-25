package finos.traderx.accountservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.repository.AccountRepository;
import finos.traderx.accountservice.repository.AccountUserRepository;

/** Corner cases of the account services, with the JPA repositories mocked out. */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private AccountUserRepository accountUserRepository;

	@InjectMocks
	private AccountService accountService;

	@InjectMocks
	private AccountUserService accountUserService;

	private static Account account(int id, String displayName) {
		Account account = new Account();
		account.setId(id);
		account.setDisplayName(displayName);
		return account;
	}

	@Test
	@DisplayName("AS-16b: getAccountById throws ResourceNotFoundException when the row is absent")
	void missingAccountThrows() {
		when(this.accountRepository.findById(42)).thenReturn(Optional.empty());

		ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class,
				() -> this.accountService.getAccountById(42));

		// note the missing space: the message reads "...42not found"
		assertEquals("Account with id 42not found", thrown.getMessage());
	}

	@Test
	@DisplayName("AS-19c: upsertAccount performs no existence check, so an explicit id overwrites the row")
	void upsertOverwritesExistingId() {
		when(this.accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

		this.accountService.upsertAccount(account(1, "Hijacked"));

		ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
		verify(this.accountRepository).save(captor.capture());
		assertEquals(1, captor.getValue().getId());
		// findById is never called: the service cannot distinguish create from update
		verify(this.accountRepository, never()).findById(any());
	}

	@Test
	@Disabled("LATENT BUG: AccountService.upsertAccount (account-service/src/main/java/finos/traderx/"
			+ "accountservice/service/AccountService.java:34) calls save() blindly, so POST with an "
			+ "existing id overwrites another account with no ownership or existence check")
	@DisplayName("AS-19d: creating an account with an id that already exists should be refused")
	void upsertShouldNotOverwriteExistingId() {
		when(this.accountRepository.findById(1)).thenReturn(Optional.of(account(1, "Original")));

		assertThrows(IllegalStateException.class, () -> this.accountService.upsertAccount(account(1, "Hijacked")));
	}

	@Test
	@DisplayName("AS-21c: getAllAccount returns an empty (non-null) list for an empty repository")
	void getAllAccountOnEmptyRepository() {
		when(this.accountRepository.findAll()).thenReturn(List.of());

		List<Account> accounts = this.accountService.getAllAccount();

		assertNotNull(accounts);
		assertTrue(accounts.isEmpty());
	}

	@Test
	@DisplayName("AS-21d: getAllAccount copies every row returned by the repository")
	void getAllAccountCopiesRows() {
		when(this.accountRepository.findAll()).thenReturn(List.of(account(1, "One"), account(2, "Two")));

		assertEquals(2, this.accountService.getAllAccount().size());
	}

	@Test
	@DisplayName("AS-16c: getAccountById returns the row when present, including id 0 and negative ids")
	void getAccountByIdBoundaryIds() {
		when(this.accountRepository.findById(0)).thenReturn(Optional.of(account(0, "Zero")));
		when(this.accountRepository.findById(-1)).thenReturn(Optional.of(account(-1, "Negative")));

		assertEquals("Zero", this.accountService.getAccountById(0).getDisplayName());
		assertEquals("Negative", this.accountService.getAccountById(-1).getDisplayName());
	}

	@Test
	@DisplayName("AS-24i: upsertAccountUser rejects an unknown account id with ResourceNotFoundException")
	void upsertAccountUserRequiresAccount() {
		when(this.accountRepository.findById(55)).thenReturn(Optional.empty());
		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(55);
		accountUser.setUsername("bob");

		ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class,
				() -> this.accountUserService.upsertAccountUser(accountUser));

		assertEquals("Account with id 55not found", thrown.getMessage());
		verify(this.accountUserRepository, never()).save(any());
	}

	@Test
	@DisplayName("AS-24j: upsertAccountUser saves when the account exists")
	void upsertAccountUserSaves() {
		when(this.accountRepository.findById(1)).thenReturn(Optional.of(account(1, "One")));
		when(this.accountUserRepository.save(any(AccountUser.class))).thenAnswer(i -> i.getArgument(0));
		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(1);
		accountUser.setUsername("bob");

		assertSame(accountUser, this.accountUserService.upsertAccountUser(accountUser));
	}

	@Test
	@DisplayName("AS-24k: upsertAccountUser with a null accountId throws rather than saving an orphan row")
	void upsertAccountUserWithNullAccountId() {
		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(null);
		accountUser.setUsername("bob");

		assertThrows(Exception.class, () -> this.accountUserService.upsertAccountUser(accountUser));
		verify(this.accountUserRepository, never()).save(any());
	}
}
