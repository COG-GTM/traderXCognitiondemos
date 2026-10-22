package finos.traderx.accountservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import finos.traderx.accountservice.exceptions.AccountUserConflictException;
import finos.traderx.accountservice.exceptions.ResourceNotFoundException;
import finos.traderx.accountservice.model.Account;
import finos.traderx.accountservice.model.AccountUser;
import finos.traderx.accountservice.repository.AccountRepository;
import finos.traderx.accountservice.repository.AccountUserRepository;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountUserService {

	@Autowired
	AccountUserRepository accountUserRepository;

	@Autowired
	AccountRepository accountRepository;

	@PersistenceContext
	EntityManager entityManager;

	public List<AccountUser> getAllAccountUsers() {
		List<AccountUser> accountUsers = new ArrayList<AccountUser>();
		this.accountUserRepository.findAll().forEach(accountUser -> accountUsers.add(accountUser));
		return accountUsers;
	}

	public AccountUser getAccountUserById(int id) throws ResourceNotFoundException {
		Optional<AccountUser> accountUser = this.accountUserRepository.findById(Integer.valueOf(id));
		if (accountUser.isEmpty()) {
			throw new ResourceNotFoundException("AccountUser with id " + id + "not found");
		}
		return accountUser.get();
	}

	@Transactional
	public AccountUser createAccountUser(Integer accountId, String username) {
		requireAccount(accountId);
		if (this.accountUserRepository.existsByAccountIdAndUsername(accountId, username)) {
			throw alreadyMapped(accountId, username);
		}
		// persist + flush issues a plain INSERT (save() would merge into a row created concurrently),
		// so a concurrent duplicate surfaces here as a primary-key violation.
		AccountUser accountUser = newAccountUser(accountId, username);
		try {
			this.entityManager.persist(accountUser);
			this.entityManager.flush();
		}
		catch (EntityExistsException | ConstraintViolationException e) {
			throw alreadyMapped(accountId, username);
		}
		return accountUser;
	}

	public AccountUser upsertAccountUser(Integer accountId, String username) {
		requireAccount(accountId);
		return this.accountUserRepository.save(newAccountUser(accountId, username));
	}

	private void requireAccount(Integer accountId) {
		Optional<Account> account = this.accountRepository.findById(accountId);
		if (account.isEmpty()) {
			throw new ResourceNotFoundException("Account with id " + accountId + " not found");
		}
	}

	private static AccountUserConflictException alreadyMapped(Integer accountId, String username) {
		return new AccountUserConflictException(username + " is already mapped to account " + accountId);
	}

	private static AccountUser newAccountUser(Integer accountId, String username) {
		AccountUser accountUser = new AccountUser();
		accountUser.setAccountId(accountId);
		accountUser.setUsername(username);
		return accountUser;
	}

}
