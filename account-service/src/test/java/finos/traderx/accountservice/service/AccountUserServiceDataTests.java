package finos.traderx.accountservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import finos.traderx.accountservice.exceptions.AccountUserConflictException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.repository.AccountRepository;
import finos.traderx.accountservice.repository.AccountUserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(AccountUserService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountUserServiceDataTests {

	@Autowired
	AccountUserService accountUserService;

	@Autowired
	AccountRepository accountRepository;

	@SpyBean
	AccountUserRepository accountUserRepository;

	@AfterEach
	void cleanUp() {
		this.accountUserRepository.deleteAll();
		this.accountRepository.deleteAll();
	}

	@Test
	void createConflictsWhenRowInsertedAfterExistenceCheck() {
		Account account = new Account();
		account.setDisplayName("Race");
		int accountId = this.accountRepository.save(account).getId();
		this.accountUserService.createAccountUser(accountId, "user01");

		// Simulate a concurrent request that inserted the row between the existence check and the insert.
		doReturn(false).when(this.accountUserRepository).existsByAccountIdAndUsername(accountId, "user01");

		assertThrows(AccountUserConflictException.class, () -> this.accountUserService.createAccountUser(accountId, "user01"));
		assertEquals(1, this.accountUserRepository.count());
	}

	@Test
	void upsertOfExistingRowSucceeds() {
		Account account = new Account();
		account.setDisplayName("Upsert");
		int accountId = this.accountRepository.save(account).getId();
		this.accountUserService.createAccountUser(accountId, "user01");

		AccountUser updated = this.accountUserService.upsertAccountUser(accountId, "user01");
		assertEquals("user01", updated.getUsername());
		assertEquals(1, this.accountUserRepository.count());
	}
}
